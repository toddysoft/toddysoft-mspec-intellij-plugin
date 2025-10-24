package com.toddysoft.mspec.psi;

import com.intellij.lang.ASTNode;
import com.toddysoft.mspec.MSpecPsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * PSI element for array field definitions.
 * Example: [array Item items count 'numberOfItems']
 */
public class MSpecArrayFieldElement extends MSpecPsiElement {
    public MSpecArrayFieldElement(@NotNull ASTNode node) {
        super(node);
    }
}
