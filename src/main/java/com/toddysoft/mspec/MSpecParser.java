package com.toddysoft.mspec;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * Parser implementation for MSpec language.
 *
 * Note: This is a simplified parser that creates a flat PSI tree.
 * For this language, a flat tree with text-based validation in the annotator
 * is sufficient and more maintainable than trying to force ANTLR integration.
 *
 * Future improvement: Consider using Grammar-Kit (https://github.com/JetBrains/Grammar-Kit)
 * instead of ANTLR for proper IntelliJ PSI integration if hierarchical tree is needed.
 */
public class MSpecParser implements PsiParser {
    @NotNull
    @Override
    public ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
        PsiBuilder.Marker rootMarker = builder.mark();

        // Simple parse: consume all tokens
        // The annotator handles validation using text-based analysis
        while (!builder.eof()) {
            builder.advanceLexer();
        }

        rootMarker.done(root);
        return builder.getTreeBuilt();
    }
}
