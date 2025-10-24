package com.toddysoft.mspec;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * File type definition for MSpec files.
 */
public class MSpecFileType extends LanguageFileType {
    public static final MSpecFileType INSTANCE = new MSpecFileType();

    private MSpecFileType() {
        super(MSpecLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "MSpec File";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Apache PLC4X MSpec file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "mspec";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        try {
            return IconLoader.getIcon("/icons/mspec.svg", MSpecFileType.class);
        } catch (Exception e) {
            // Fallback to default text file icon if SVG not available
            return null;
        }
    }
}
