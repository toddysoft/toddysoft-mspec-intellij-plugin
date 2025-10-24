package com.toddysoft.mspec;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
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

    // Primitive types that require size parameters
    private static final Set<String> SIZED_TYPES = new HashSet<>(Arrays.asList(
        "int", "uint", "float", "ufloat", "string"
    ));

    // Pattern to find type definitions
    private static final Pattern TYPE_DEFINITION_PATTERN =
        Pattern.compile("\\[\\s*(?:type|dataIo|discriminatedType)\\s+([A-Za-z][A-Za-z0-9_-]*)");
    private static final Pattern ENUM_DEFINITION_PATTERN =
        Pattern.compile("\\[\\s*enum\\s+[A-Za-z]+\\s+\\d+\\s+([A-Za-z][A-Za-z0-9_-]*)");

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
            if (!PRIMITIVE_TYPES.contains(typeRef.toLowerCase())) {
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

        // Check if this is a sized type that needs validation (int, uint, float, ufloat, string)
        if (SIZED_TYPES.contains(text.toLowerCase())) {
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
            int spaceIdx = trimmed.indexOf(' ');
            String firstToken = spaceIdx > 0 ? trimmed.substring(0, spaceIdx) : trimmed;

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

            // Check if this is a field type keyword (simple, array, etc.)
            Set<String> fieldTypes = new HashSet<>(Arrays.asList(
                "abstract", "array", "assert", "checksum", "const", "discriminator",
                "enum", "implicit", "manualArray", "manual", "optional", "padding",
                "peek", "reserved", "simple", "state", "typeSwitch", "unknown",
                "validation", "virtual"
            ));

            if (fieldTypes.contains(fieldKeyword)) {
                // This is a type reference - validate it exists
                // Skip primitive types
                if (!PRIMITIVE_TYPES.contains(text.toLowerCase())) {
                    Set<String> customTypes = findCustomTypes(file);
                    if (!customTypes.contains(text)) {
                        holder.newAnnotation(HighlightSeverity.ERROR,
                            "Undefined type '" + text + "'. Type must be defined with [type " + text + "], [enum " + text + "], or similar.")
                            .range(element.getTextRange())
                            .create();
                    }
                }
            }
        }
    }

    /**
     * Finds all custom type definitions in the file
     */
    private Set<String> findCustomTypes(PsiFile file) {
        Set<String> types = new HashSet<>();
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

        return types;
    }
}
