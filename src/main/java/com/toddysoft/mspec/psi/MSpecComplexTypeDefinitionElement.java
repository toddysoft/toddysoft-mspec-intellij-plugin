package com.toddysoft.mspec.psi;

import com.intellij.lang.ASTNode;
import com.toddysoft.mspec.MSpecPsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * PSI element for complex type definitions (type, discriminatedType, enum, dataIo).
 */
public class MSpecComplexTypeDefinitionElement extends MSpecPsiElement {
    public MSpecComplexTypeDefinitionElement(@NotNull ASTNode node) {
        super(node);
    }
}
