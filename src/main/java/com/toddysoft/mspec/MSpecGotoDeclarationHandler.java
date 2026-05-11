package com.toddysoft.mspec;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.toddysoft.mspec.util.MSpecTypeIndex;
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
 *
 * Type lookups are delegated to {@link MSpecTypeIndex}, which caches per-file type-offset maps.
 */
public class MSpecGotoDeclarationHandler implements GotoDeclarationHandler {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_-]*");
    private static final Pattern TYPE_REF_CONTEXT_PATTERN = Pattern.compile("\\[\\s*(\\w+)\\s+$");

    private static final Set<String> PRIMITIVE_TYPES = new HashSet<>(Arrays.asList(
            "bit", "byte", "int", "uint", "vint", "vuint",
            "float", "ufloat", "string", "vstring",
            "time", "date", "dateTime"
    ));

    private static final Set<String> FIELD_TYPES_WITH_TYPE_REF = new HashSet<>(Arrays.asList(
            "abstract", "array", "assert", "const", "discriminator",
            "enum", "implicit", "manualArray", "manual", "optional",
            "peek", "simple", "virtual"
    ));

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement,
                                                              int offset,
                                                              Editor editor) {
        if (sourceElement == null) {
            return null;
        }

        PsiFile file = sourceElement.getContainingFile();
        if (file == null || !file.getName().endsWith(".mspec")) {
            return null;
        }

        String text = sourceElement.getText();
        if (text == null || text.isEmpty()) {
            return null;
        }

        if (!IDENTIFIER_PATTERN.matcher(text).matches()) {
            return null;
        }

        if (PRIMITIVE_TYPES.contains(text)) {
            return null;
        }

        if (!isTypeReference(sourceElement)) {
            return null;
        }

        MSpecTypeIndex.TypeLocation location = MSpecTypeIndex.findTypeDefinition(file, text);
        if (location == null) {
            return null;
        }
        PsiElement target = location.file.findElementAt(location.offset);
        return target != null ? new PsiElement[]{target} : null;
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

        int contextStart = Math.max(0, offset - 100);
        String beforeContext = fileText.substring(contextStart, offset);

        Matcher typeRefPattern = TYPE_REF_CONTEXT_PATTERN.matcher(beforeContext);
        if (typeRefPattern.find()) {
            return FIELD_TYPES_WITH_TYPE_REF.contains(typeRefPattern.group(1));
        }

        return false;
    }
}
