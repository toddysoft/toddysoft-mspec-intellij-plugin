package com.toddysoft.mspec;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles "Go to Definition" for type references in MSpec files.
 * Uses GotoDeclarationHandler which works better with ANTLR's flat PSI structure.
 */
public class MSpecGotoDeclarationHandler implements GotoDeclarationHandler {

    private static final Pattern TYPE_DEFINITION_PATTERN =
            Pattern.compile("\\[\\s*(?:type|dataIo|discriminatedType)\\s+([A-Za-z][A-Za-z0-9_-]*)");
    private static final Pattern ENUM_DEFINITION_PATTERN =
            Pattern.compile("\\[\\s*enum\\s+(?:(?:(?:bit|byte|vint|vuint|time|date|dateTime|vstring)\\s+)|(?:(?:int|uint|float|ufloat|string)\\s+\\d+\\s+))?([A-Za-z][A-Za-z0-9_-]*)");

    private static final Set<String> PRIMITIVE_TYPES = new HashSet<>(Arrays.asList(
            "bit", "byte", "int", "uint", "vint", "vuint",
            "float", "ufloat", "string", "vstring",
            "time", "date", "dateTime"
    ));

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement,
                                                              int offset,
                                                              Editor editor) {
        if (sourceElement == null) {
            return null;
        }

        // Only handle MSpec files
        PsiFile file = sourceElement.getContainingFile();
        if (file == null || !file.getName().endsWith(".mspec")) {
            return null;
        }

        // Get the identifier text
        String text = sourceElement.getText();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        // Must be identifier-like
        if (!text.matches("[A-Za-z][A-Za-z0-9_-]*")) {
            return null;
        }

        // Skip primitive types
        if (PRIMITIVE_TYPES.contains(text)) {
            return null;
        }

        // Check if this looks like a type reference (not a definition or field name)
        if (!isTypeReference(sourceElement)) {
            return null;
        }

        // Search for the type definition
        PsiElement definition = findTypeDefinition(file, text);
        if (definition != null) {
            return new PsiElement[]{definition};
        }

        // Search in sibling .mspec files
        if (file.getParent() != null) {
            for (PsiElement child : file.getParent().getChildren()) {
                if (child instanceof PsiFile) {
                    PsiFile siblingFile = (PsiFile) child;
                    if (!siblingFile.equals(file) && siblingFile.getName().endsWith(".mspec")) {
                        definition = findTypeDefinition(siblingFile, text);
                        if (definition != null) {
                            return new PsiElement[]{definition};
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Checks if the element appears to be a type reference (not a definition or field name).
     */
    private boolean isTypeReference(PsiElement element) {
        PsiFile file = element.getContainingFile();
        if (file == null) {
            return false;
        }

        int offset = element.getTextRange().getStartOffset();
        String fileText = file.getText();

        // Get context before this element
        int contextStart = Math.max(0, offset - 100);
        String beforeContext = fileText.substring(contextStart, offset);

        // Pattern: [fieldType typeReference ... where typeReference is our element
        // E.g., [simple CustomType test]
        Matcher typeRefPattern = Pattern.compile("\\[\\s*(\\w+)\\s+$").matcher(beforeContext);
        if (typeRefPattern.find()) {
            String fieldKeyword = typeRefPattern.group(1);

            // Field types that expect a TYPE reference (not field reference)
            Set<String> fieldTypesWithTypeRef = new HashSet<>(Arrays.asList(
                    "abstract", "array", "assert", "const", "discriminator",
                    "enum", "implicit", "manualArray", "manual", "optional",
                    "peek", "simple", "virtual"
            ));

            return fieldTypesWithTypeRef.contains(fieldKeyword);
        }

        return false;
    }

    /**
     * Finds a type definition in the given file.
     */
    private @Nullable PsiElement findTypeDefinition(PsiFile file, String typeName) {
        String fileText = file.getText();

        // Search for type definitions (type, dataIo, discriminatedType)
        Matcher matcher = TYPE_DEFINITION_PATTERN.matcher(fileText);
        while (matcher.find()) {
            if (typeName.equals(matcher.group(1))) {
                int startOffset = matcher.start(1);
                return file.findElementAt(startOffset);
            }
        }

        // Search for enum definitions
        Matcher enumMatcher = ENUM_DEFINITION_PATTERN.matcher(fileText);
        while (enumMatcher.find()) {
            if (typeName.equals(enumMatcher.group(1))) {
                int startOffset = enumMatcher.start(1);
                return file.findElementAt(startOffset);
            }
        }

        return null;
    }
}
