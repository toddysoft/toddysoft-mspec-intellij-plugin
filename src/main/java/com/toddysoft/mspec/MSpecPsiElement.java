package com.toddysoft.mspec;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * Base PSI element for MSpec language elements.
 */
public class MSpecPsiElement extends ASTWrapperPsiElement {
    public MSpecPsiElement(@NotNull ASTNode node) {
        super(node);
    }
}
