package com.toddysoft.mspec.psi;

import com.intellij.lang.ASTNode;
import com.toddysoft.mspec.MSpecPsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * PSI element for data type references (primitive types).
 * Example: uint 8, int 16, byte, etc.
 */
public class MSpecDataTypeElement extends MSpecPsiElement {
    public MSpecDataTypeElement(@NotNull ASTNode node) {
        super(node);
    }
}
