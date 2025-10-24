package com.toddysoft.mspec.psi;

import com.intellij.lang.ASTNode;
import com.toddysoft.mspec.MSpecPsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * PSI element for field definitions.
 * Contains a field and its attributes.
 */
public class MSpecFieldDefinitionElement extends MSpecPsiElement {
    public MSpecFieldDefinitionElement(@NotNull ASTNode node) {
        super(node);
    }
}
