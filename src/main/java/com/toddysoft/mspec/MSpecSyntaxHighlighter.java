package com.toddysoft.mspec;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

/**
 * Syntax highlighter for MSpec language.
 */
public class MSpecSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey KEYWORD =
            createTextAttributesKey("MSPEC_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey IDENTIFIER =
            createTextAttributesKey("MSPEC_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey COMMENT =
            createTextAttributesKey("MSPEC_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey STRING =
            createTextAttributesKey("MSPEC_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey NUMBER =
            createTextAttributesKey("MSPEC_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey OPERATOR =
            createTextAttributesKey("MSPEC_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey BRACKET =
            createTextAttributesKey("MSPEC_BRACKET", DefaultLanguageHighlighterColors.BRACKETS);

    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];
    private static final TextAttributesKey[] KEYWORD_KEYS = new TextAttributesKey[]{KEYWORD};
    private static final TextAttributesKey[] IDENTIFIER_KEYS = new TextAttributesKey[]{IDENTIFIER};
    private static final TextAttributesKey[] COMMENT_KEYS = new TextAttributesKey[]{COMMENT};
    private static final TextAttributesKey[] STRING_KEYS = new TextAttributesKey[]{STRING};
    private static final TextAttributesKey[] NUMBER_KEYS = new TextAttributesKey[]{NUMBER};
    private static final TextAttributesKey[] OPERATOR_KEYS = new TextAttributesKey[]{OPERATOR};
    private static final TextAttributesKey[] BRACKET_KEYS = new TextAttributesKey[]{BRACKET};

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new MSpecLexerAdapter();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType.equals(MSpecTokenTypes.KEYWORD)) {
            return KEYWORD_KEYS;
        } else if (tokenType.equals(MSpecTokenTypes.IDENTIFIER)) {
            return IDENTIFIER_KEYS;
        } else if (tokenType.equals(MSpecTokenTypes.COMMENT)) {
            return COMMENT_KEYS;
        } else if (tokenType.equals(MSpecTokenTypes.STRING)) {
            return STRING_KEYS;
        } else if (tokenType.equals(MSpecTokenTypes.NUMBER)) {
            return NUMBER_KEYS;
        } else if (tokenType.equals(MSpecTokenTypes.OPERATOR)) {
            return OPERATOR_KEYS;
        } else if (tokenType.equals(MSpecTokenTypes.BRACKET)) {
            return BRACKET_KEYS;
        }
        return EMPTY_KEYS;
    }
}
