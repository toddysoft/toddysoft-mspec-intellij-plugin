package com.toddysoft.mspec;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Annotator for MSpec files that provides semantic validation and error highlighting.
 * Uses text-based context analysis since ANTLR PSI tree is flat (all elements are direct children of MSpecFile).
 */
public class MSpecAnnotator implements Annotator {

    // Valid primitive type keywords
    private static final Set<String> PRIMITIVE_TYPES = new HashSet<>(Arrays.asList(
        "bit", "byte", "int", "uint", "vint", "vuint",
        "float", "ufloat", "string", "vstring",
        "time", "date", "dateTime"
    ));

    // Array loop type keywords
    private static final Set<String> ARRAY_LOOP_TYPES = new HashSet<>(Arrays.asList(
        "count", "length", "terminated"
    ));

    // Primitive types that require size parameters
    private static final Set<String> SIZED_TYPES = new HashSet<>(Arrays.asList(
        "int", "uint", "float", "ufloat", "string"
    ));

    // Pattern to find type definitions
    private static final Pattern TYPE_DEFINITION_PATTERN =
        Pattern.compile("\\[\\s*(?:type|dataIo|discriminatedType)\\s+([A-Za-z][A-Za-z0-9_-]*)");
    // Pattern matches enum definitions with optional base type following ANTLR dataType grammar:
    // [enum Name] - no type
    // [enum bit Name], [enum byte Name], [enum vint Name], etc. - types without size
    // [enum int 8 Name], [enum uint 16 Name], etc. - types with required size
    private static final Pattern ENUM_DEFINITION_PATTERN =
        Pattern.compile("\\[\\s*enum\\s+(?:(?:(?:bit|byte|vint|vuint|time|date|dateTime|vstring)\\s+)|(?:(?:int|uint|float|ufloat|string)\\s+\\d+\\s+))?([A-Za-z][A-Za-z0-9_-]*)");

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        // Only process leaf elements that are identifiers
        if (element.getChildren().length > 0) {
            return;
        }

        String text = element.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        // Skip if it's not an identifier-like token
        if (!text.matches("[A-Za-z][A-Za-z0-9_-]*")) {
            return;
        }

        PsiFile file = element.getContainingFile();
        if (file == null) {
            return;
        }

        // Get context around this element
        int offset = element.getTextRange().getStartOffset();
        String fileText = file.getText();

        // Get context before and after
        int contextStart = Math.max(0, offset - 100);
        String beforeContext = fileText.substring(contextStart, offset);

        int contextEnd = Math.min(fileText.length(), offset + text.length() + 50);
        String afterContext = fileText.substring(offset + text.length(), contextEnd);

        // Validate based on context
        validateInContext(element, text, beforeContext, afterContext, file, holder);
    }

    /**
     * Validates an identifier based on its surrounding text context
     */
    private void validateInContext(PsiElement element, String text, String beforeContext,
                                   String afterContext, PsiFile file, AnnotationHolder holder) {

        // Check if this is an array loop type in the correct position
        // Pattern: [array typeRef fieldName <loopType>
        // or: [manualArray typeRef fieldName <loopType>
        if (ARRAY_LOOP_TYPES.contains(text.toLowerCase())) {
            // Check if it's in the loop type position
            // Match: [array/manualArray typeRef fieldName whitespace (but don't require end of string)
            // Use [\\s\\S]* instead of .* to match newlines as well
            if (beforeContext.matches("[\\s\\S]*\\[\\s*(?:array|manualArray)\\s+\\S+\\s+\\S+\\s+[\\s\\S]*")) {
                // This is a loop type keyword in the correct position
                // Get the keyword text attributes from the current color scheme
                TextAttributes keywordAttrs = EditorColorsManager.getInstance().getGlobalScheme()
                        .getAttributes(com.intellij.openapi.editor.DefaultLanguageHighlighterColors.KEYWORD);

                // Use enforcedTextAttributes to override base highlighting
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(element.getTextRange())
                        .enforcedTextAttributes(keywordAttrs)
                        .create();
                return;
            }
            // If it's not in the loop type position, it's just a regular identifier (field name)
            // Don't validate or highlight it
            return;
        }

        // Pattern: [fieldType primitiveType size fieldName
        // E.g., [simple uint 8 messageType]
        // Match: word word number word$ (where word$ is our element)
        if (beforeContext.matches(".*\\[\\s*\\w+\\s+\\w+\\s+\\d+\\s+$")) {
            // This is a field name after a sized primitive type - don't validate
            return;
        }

        // Pattern: [fieldType customType fieldName
        // E.g., [simple Item items]
        // Match: word word word$ (where word$ is our element, and middle word is not a primitive keyword)
        Matcher fieldAfterCustomType = Pattern.compile("\\[\\s*(\\w+)\\s+(\\w+)\\s+$").matcher(beforeContext);
        if (fieldAfterCustomType.find()) {
            String fieldType = fieldAfterCustomType.group(1);
            String typeRef = fieldAfterCustomType.group(2);

            // Check if typeRef is NOT a primitive type (it's a custom type)
            // and NOT a keyword like "byte" which doesn't need size
            // Only match lowercase primitive types; uppercase identifiers are type names
            if (!PRIMITIVE_TYPES.contains(typeRef)) {
                // This is a field name after a custom type reference - don't validate
                return;
            }
        }

        // Pattern: [array primitiveType fieldName
        // E.g., [array byte itemData
        // For array fields with non-sized types like byte
        if (beforeContext.matches(".*\\[\\s*array\\s+byte\\s+$")) {
            // This is a field name after array byte - don't validate
            return;
        }

        // Pattern: ['discriminatorValue' CASENAME (without asterisk)
        // E.g., ['INT' INT or ['0x01' BOOL
        // This is a plain typeSwitch case name - don't validate as a type reference
        // Note: ['discriminatorValue' *CASENAME won't match this pattern because of the asterisk,
        // so it will fall through to type validation (which is correct, as it defines ParentType+CaseName)
        if (beforeContext.matches(".*\\[\\s*'[^']*'\\s+$")) {
            // Plain case name without asterisk - don't validate
            return;
        }

        // Check if this is a sized type that needs validation (int, uint, float, ufloat, string)
        // Only match lowercase primitive types; uppercase identifiers (INT, UINT) are likely type/case names
        if (SIZED_TYPES.contains(text)) {
            // Check if followed by a valid number (size parameter)
            String trimmed = afterContext.trim();
            if (trimmed.isEmpty()) {
                holder.newAnnotation(HighlightSeverity.ERROR,
                    "Type '" + text + "' requires a size parameter (e.g., '" + text + " 8')")
                    .range(element.getTextRange())
                    .create();
                return;
            }

            // Extract the first token (should be the size number)
            // Split by any whitespace (space, tab, newline, etc.)
            String[] tokens = trimmed.split("\\s+");
            if (tokens.length == 0) {
                holder.newAnnotation(HighlightSeverity.ERROR,
                    "Type '" + text + "' requires a size parameter (e.g., '" + text + " 8')")
                    .range(element.getTextRange())
                    .create();
                return;
            }

            String firstToken = tokens[0];

            // Check if the first token is a valid number
            if (!firstToken.matches("\\d+")) {
                holder.newAnnotation(HighlightSeverity.ERROR,
                    "Type '" + text + "' requires a size parameter (e.g., '" + text + " 8')")
                    .range(element.getTextRange())
                    .create();
                return;
            }
            // If size parameter exists and is valid, this is a valid sized type - skip further validation
            return;
        }

        // Pattern: [fieldType typeReference ... where typeReference is our element
        // E.g., [simple Huiiii test]
        // Match: word word$ (where word$ is our element followed by more content)
        Matcher typeRefPattern = Pattern.compile("\\[\\s*(\\w+)\\s+$").matcher(beforeContext);
        if (typeRefPattern.find()) {
            String fieldKeyword = typeRefPattern.group(1);

            // Field types that expect a TYPE reference (not field reference)
            Set<String> fieldTypesWithTypeRef = new HashSet<>(Arrays.asList(
                "abstract", "array", "assert", "const", "discriminator",
                "enum", "implicit", "manualArray", "manual", "optional",
                "peek", "simple", "virtual"
            ));

            // Special field types that take field references or other syntax (not type references)
            // - typeSwitch: takes discriminator field name(s)
            // - state: takes just a name
            // - checksum, padding, reserved, unknown: take data types, not custom type refs
            // - validation: takes expression

            if (fieldTypesWithTypeRef.contains(fieldKeyword)) {
                // This is a type reference - validate it exists
                // Skip primitive types (only lowercase variants are primitive types)
                if (!PRIMITIVE_TYPES.contains(text)) {
                    Set<String> localTypes = findLocalTypes(file);
                    Set<String> externalTypes = findExternalTypes(file);

                    if (!localTypes.contains(text) && !externalTypes.contains(text)) {
                        // Type not found anywhere - error
                        holder.newAnnotation(HighlightSeverity.ERROR,
                            "Undefined type '" + text + "'. Type must be defined with [type " + text + "], [enum " + text + "], or similar.")
                            .range(element.getTextRange())
                            .create();
                    } else if (externalTypes.contains(text) && !localTypes.contains(text)) {
                        // Type is external - add subtle highlighting to indicate it's from another file
                        TextAttributes externalTypeAttrs = EditorColorsManager.getInstance().getGlobalScheme()
                                .getAttributes(com.intellij.openapi.editor.DefaultLanguageHighlighterColors.CLASS_REFERENCE);

                        // Create a copy and make it slightly different (e.g., italic or underlined)
                        TextAttributes customAttrs = externalTypeAttrs.clone();
                        customAttrs.setFontType(customAttrs.getFontType() | java.awt.Font.ITALIC);

                        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                                .range(element.getTextRange())
                                .enforcedTextAttributes(customAttrs)
                                .create();
                    }
                }
            }
        }
    }

    /**
     * Finds type definitions only in the current file
     */
    private Set<String> findLocalTypes(PsiFile file) {
        Set<String> types = new HashSet<>();
        extractTypesFromFile(file, types);
        return types;
    }

    /**
     * Finds type definitions in other .mspec files in the same directory (excluding current file)
     */
    private Set<String> findExternalTypes(PsiFile file) {
        Set<String> types = new HashSet<>();

        // Add types from other .mspec files in the same directory
        if (file.getParent() != null) {
            for (PsiElement child : file.getParent().getChildren()) {
                if (child instanceof PsiFile) {
                    PsiFile siblingFile = (PsiFile) child;
                    // Only process .mspec files, skip the current file
                    if (!siblingFile.equals(file) && siblingFile.getName().endsWith(".mspec")) {
                        extractTypesFromFile(siblingFile, types);
                    }
                }
            }
        }

        return types;
    }

    /**
     * Finds all custom type definitions in the file and other .mspec files in the same directory
     */
    private Set<String> findCustomTypes(PsiFile file) {
        Set<String> types = new HashSet<>();
        types.addAll(findLocalTypes(file));
        types.addAll(findExternalTypes(file));
        return types;
    }

    /**
     * Extracts type definitions from a single file
     */
    private void extractTypesFromFile(PsiFile file, Set<String> types) {
        String fileText = file.getText();

        // Find regular type definitions (type, dataIo, discriminatedType)
        Matcher matcher = TYPE_DEFINITION_PATTERN.matcher(fileText);
        while (matcher.find()) {
            types.add(matcher.group(1));
        }

        // Find enum definitions
        Matcher enumMatcher = ENUM_DEFINITION_PATTERN.matcher(fileText);
        while (enumMatcher.find()) {
            types.add(enumMatcher.group(1));
        }

        // Find typeSwitch case definitions with asterisk prefix
        // Pattern: ['discriminatorValue' *CaseName inside a parent type
        // These create subtypes like ParentType + CaseName = ParentTypeCaseName
        Pattern asteriskCasePattern = Pattern.compile("\\[\\s*'[^']*'\\s+\\*([A-Za-z][A-Za-z0-9_-]*)");
        Matcher asteriskMatcher = asteriskCasePattern.matcher(fileText);
        while (asteriskMatcher.find()) {
            String caseName = asteriskMatcher.group(1);
            int caseOffset = asteriskMatcher.start();

            // Find the parent type by searching backwards for the containing type definition
            String parentTypeName = findParentTypeName(fileText, caseOffset);
            if (parentTypeName != null) {
                // Combine parent type name + case name
                String fullTypeName = parentTypeName + caseName;
                types.add(fullTypeName);
            }
        }
    }

    /**
     * Finds the parent type name for a typeSwitch case at the given offset.
     * Searches backwards from the offset to find the containing [type Name] or [discriminatedType Name].
     */
    private String findParentTypeName(String fileText, int offset) {
        // Search backwards for the type definition
        String beforeText = fileText.substring(0, offset);

        // Pattern to find [type Name] or [discriminatedType Name]
        // We want the LAST match before our offset (closest parent)
        Pattern parentTypePattern = Pattern.compile("\\[\\s*(?:type|discriminatedType|dataIo)\\s+([A-Za-z][A-Za-z0-9_-]*)");
        Matcher matcher = parentTypePattern.matcher(beforeText);

        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group(1);
        }

        return lastMatch;
    }
}
