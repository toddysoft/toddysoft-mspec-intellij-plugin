package com.toddysoft.mspec.psi;

import com.intellij.lang.ASTNode;
import com.toddysoft.mspec.MSpecPsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * PSI element for simple field definitions.
 * Example: [simple uint 8 messageType]
 */
public class MSpecSimpleFieldElement extends MSpecPsiElement {
    public MSpecSimpleFieldElement(@NotNull ASTNode node) {
        super(node);
    }
}
