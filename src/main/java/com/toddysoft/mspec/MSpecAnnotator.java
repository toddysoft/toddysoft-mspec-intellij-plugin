package com.toddysoft.mspec;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.toddysoft.mspec.util.MSpecTypeIndex;
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

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_-]*");

    // Field types that expect a TYPE reference (not field reference).
    private static final Set<String> FIELD_TYPES_WITH_TYPE_REF = new HashSet<>(Arrays.asList(
            "abstract", "array", "assert", "const", "discriminator",
            "enum", "implicit", "manualArray", "manual", "optional",
            "peek", "simple", "virtual"
    ));

    private static final Pattern ARRAY_LOOP_CONTEXT_PATTERN =
            Pattern.compile("[\\s\\S]*\\[\\s*(?:array|manualArray)\\s+\\S+\\s+\\S+\\s+[\\s\\S]*");
    private static final Pattern SIZED_FIELD_NAME_CONTEXT_PATTERN =
            Pattern.compile(".*\\[\\s*\\w+\\s+\\w+\\s+\\d+\\s+$");
    private static final Pattern FIELD_AFTER_CUSTOM_TYPE_PATTERN =
            Pattern.compile("\\[\\s*(\\w+)\\s+(\\w+)\\s+$");
    private static final Pattern ARRAY_BYTE_FIELD_NAME_CONTEXT_PATTERN =
            Pattern.compile(".*\\[\\s*array\\s+byte\\s+$");
    private static final Pattern CASE_NAME_CONTEXT_PATTERN =
            Pattern.compile(".*\\[\\s*'[^']*'\\s+$");
    private static final Pattern TYPE_REF_CONTEXT_PATTERN =
            Pattern.compile("\\[\\s*(\\w+)\\s+$");

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        // Only process leaf elements that are identifier-like tokens. Filter by token type first
        // (cheap) so we skip whitespace, brackets, comments, strings, numbers, and operators
        // without ever calling getText() or running regex on them.
        if (element.getChildren().length > 0) {
            return;
        }
        IElementType type = element.getNode().getElementType();
        if (type != MSpecTokenTypes.IDENTIFIER && type != MSpecTokenTypes.KEYWORD) {
            return;
        }

        String text = element.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        // Sanity check: must look like an identifier.
        if (!IDENTIFIER_PATTERN.matcher(text).matches()) {
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
            if (ARRAY_LOOP_CONTEXT_PATTERN.matcher(beforeContext).matches()) {
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
        if (SIZED_FIELD_NAME_CONTEXT_PATTERN.matcher(beforeContext).matches()) {
            // This is a field name after a sized primitive type - don't validate
            return;
        }

        // Pattern: [fieldType customType fieldName
        // E.g., [simple Item items]
        // Match: word word word$ (where word$ is our element, and middle word is not a primitive keyword)
        Matcher fieldAfterCustomType = FIELD_AFTER_CUSTOM_TYPE_PATTERN.matcher(beforeContext);
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
        if (ARRAY_BYTE_FIELD_NAME_CONTEXT_PATTERN.matcher(beforeContext).matches()) {
            // This is a field name after array byte - don't validate
            return;
        }

        // Pattern: ['discriminatorValue' CASENAME (without asterisk)
        // E.g., ['INT' INT or ['0x01' BOOL
        // This is a plain typeSwitch case name - don't validate as a type reference
        // Note: ['discriminatorValue' *CASENAME won't match this pattern because of the asterisk,
        // so it will fall through to type validation (which is correct, as it defines ParentType+CaseName)
        if (CASE_NAME_CONTEXT_PATTERN.matcher(beforeContext).matches()) {
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
        Matcher typeRefPattern = TYPE_REF_CONTEXT_PATTERN.matcher(beforeContext);
        if (typeRefPattern.find()) {
            String fieldKeyword = typeRefPattern.group(1);

            // Special field types that take field references or other syntax (not type references)
            // - typeSwitch: takes discriminator field name(s)
            // - state: takes just a name
            // - checksum, padding, reserved, unknown: take data types, not custom type refs
            // - validation: takes expression

            if (FIELD_TYPES_WITH_TYPE_REF.contains(fieldKeyword)) {
                // This is a type reference - validate it exists
                // Skip primitive types (only lowercase variants are primitive types)
                if (!PRIMITIVE_TYPES.contains(text)) {
                    Set<String> localTypes = MSpecTypeIndex.getTypesInFile(file);
                    Set<String> typesInScope = MSpecTypeIndex.getTypesInScope(file);

                    if (!typesInScope.contains(text)) {
                        // Type not found anywhere - error
                        holder.newAnnotation(HighlightSeverity.ERROR,
                            "Undefined type '" + text + "'. Type must be defined with [type " + text + "], [enum " + text + "], or similar.")
                            .range(element.getTextRange())
                            .create();
                    } else if (!localTypes.contains(text)) {
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

}
