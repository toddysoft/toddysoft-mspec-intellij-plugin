package com.toddysoft.mspec;

import com.intellij.lang.Language;

/**
 * Language definition for MSpec files.
 */
public class MSpecLanguage extends Language {
    public static final MSpecLanguage INSTANCE = new MSpecLanguage();

    private MSpecLanguage() {
        super("MSpec");
    }
}
