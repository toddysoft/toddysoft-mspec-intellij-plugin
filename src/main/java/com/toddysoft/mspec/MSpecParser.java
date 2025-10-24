package com.toddysoft.mspec;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * Parser implementation for MSpec language.
 * This is a simplified parser that creates a flat PSI tree.
 */
public class MSpecParser implements PsiParser {
    @NotNull
    @Override
    public ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
        PsiBuilder.Marker rootMarker = builder.mark();

        // Simple parse all tokens approach
        while (!builder.eof()) {
            builder.advanceLexer();
        }

        rootMarker.done(root);
        return builder.getTreeBuilt();
    }
}
