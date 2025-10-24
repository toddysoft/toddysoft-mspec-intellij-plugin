package com.toddysoft.mspec.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.toddysoft.mspec.MSpecFileType;
import com.toddysoft.mspec.MSpecLanguage;
import org.jetbrains.annotations.NotNull;

/**
 * PSI file implementation for MSpec files.
 */
public class MSpecFile extends PsiFileBase {
    public MSpecFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, MSpecLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return MSpecFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "MSpec File";
    }
}
