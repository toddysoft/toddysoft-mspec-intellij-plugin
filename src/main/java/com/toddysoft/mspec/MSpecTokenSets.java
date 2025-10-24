package com.toddysoft.mspec;

import com.intellij.psi.tree.TokenSet;

/**
 * Token sets for MSpec language.
 */
public class MSpecTokenSets {
    public static final TokenSet COMMENTS = TokenSet.create(MSpecTokenTypes.COMMENT);
    public static final TokenSet STRINGS = TokenSet.create(MSpecTokenTypes.STRING);
    public static final TokenSet KEYWORDS = TokenSet.create(MSpecTokenTypes.KEYWORD);
    public static final TokenSet IDENTIFIERS = TokenSet.create(MSpecTokenTypes.IDENTIFIER);
    public static final TokenSet NUMBERS = TokenSet.create(MSpecTokenTypes.NUMBER);
}
