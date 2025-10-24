package com.toddysoft.mspec;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Element type for MSpec language constructs.
 */
public class MSpecElementType extends IElementType {
    public MSpecElementType(@NotNull @NonNls String debugName) {
        super(debugName, MSpecLanguage.INSTANCE);
    }
}
