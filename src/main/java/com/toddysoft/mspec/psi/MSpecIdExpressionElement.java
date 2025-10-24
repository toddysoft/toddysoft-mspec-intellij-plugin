package com.toddysoft.mspec.psi;

import com.intellij.lang.ASTNode;
import com.toddysoft.mspec.MSpecPsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * PSI element for identifier expressions.
 * Used for type names, field names, etc.
 */
public class MSpecIdExpressionElement extends MSpecPsiElement {
    public MSpecIdExpressionElement(@NotNull ASTNode node) {
        super(node);
    }
}
