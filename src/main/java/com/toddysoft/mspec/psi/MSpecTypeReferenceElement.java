package com.toddysoft.mspec.psi;

import com.intellij.lang.ASTNode;
import com.toddysoft.mspec.MSpecPsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * PSI element for type references.
 * Can be either a primitive type or custom type reference.
 */
public class MSpecTypeReferenceElement extends MSpecPsiElement {
    public MSpecTypeReferenceElement(@NotNull ASTNode node) {
        super(node);
    }
}
