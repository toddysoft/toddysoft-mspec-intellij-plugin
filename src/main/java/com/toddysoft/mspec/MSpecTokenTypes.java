package com.toddysoft.mspec;

import com.intellij.psi.tree.IElementType;
import com.toddysoft.mspec.parser.MSpecLexer;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapping between ANTLR token types and IntelliJ token types.
 */
public class MSpecTokenTypes {
    private static final Map<Integer, IElementType> tokenTypeMap = new HashMap<>();

    // Keywords
    public static final IElementType KEYWORD = new MSpecElementType("KEYWORD");
    public static final IElementType IDENTIFIER = new MSpecElementType("IDENTIFIER");
    public static final IElementType COMMENT = new MSpecElementType("COMMENT");
    public static final IElementType STRING = new MSpecElementType("STRING");
    public static final IElementType NUMBER = new MSpecElementType("NUMBER");
    public static final IElementType OPERATOR = new MSpecElementType("OPERATOR");
    public static final IElementType BRACKET = new MSpecElementType("BRACKET");
    public static final IElementType UNKNOWN = new MSpecElementType("UNKNOWN");
    public static final IElementType WHITESPACE = new MSpecElementType("WHITESPACE");

    static {
        // Operators (T__0 through T__23 are literal operators from the grammar)
        // T__0='=', T__1=',', T__2='.', T__3='?', T__4=':', T__5='"', T__6='!',
        // T__7='+', T__8='-', T__9='/', T__10='^', T__11='==', T__12='!=',
        // T__13='>>', T__14='<<', T__15='>', T__16='<', T__17='>=', T__18='<=',
        // T__19='&&', T__20='||', T__21='&', T__22='|', T__23='%'
        for (int i = 1; i <= 24; i++) {
            tokenTypeMap.put(i, OPERATOR);
        }

        // Special tokens
        tokenTypeMap.put(MSpecLexer.TICK, OPERATOR);
        tokenTypeMap.put(MSpecLexer.ASTERISK, OPERATOR);

        // Map keywords
        tokenTypeMap.put(MSpecLexer.CONSTANTS, KEYWORD);
        tokenTypeMap.put(MSpecLexer.GLOBALS, KEYWORD);
        tokenTypeMap.put(MSpecLexer.CONTEXT, KEYWORD);
        tokenTypeMap.put(MSpecLexer.TYPE, KEYWORD);
        tokenTypeMap.put(MSpecLexer.DISCRIMINATEDTYPE, KEYWORD);
        tokenTypeMap.put(MSpecLexer.DATAIO, KEYWORD);
        tokenTypeMap.put(MSpecLexer.ENUM, KEYWORD);
        tokenTypeMap.put(MSpecLexer.BATCHSET, KEYWORD);

        // Field keywords
        tokenTypeMap.put(MSpecLexer.ABSTRACT, KEYWORD);
        tokenTypeMap.put(MSpecLexer.ARRAY, KEYWORD);
        tokenTypeMap.put(MSpecLexer.ASSERT, KEYWORD);
        tokenTypeMap.put(MSpecLexer.CHECKSUM, KEYWORD);
        tokenTypeMap.put(MSpecLexer.CONST, KEYWORD);
        tokenTypeMap.put(MSpecLexer.DISCRIMINATOR, KEYWORD);
        tokenTypeMap.put(MSpecLexer.IMPLICIT, KEYWORD);
        tokenTypeMap.put(MSpecLexer.MANUALARRAY, KEYWORD);
        tokenTypeMap.put(MSpecLexer.MANUAL, KEYWORD);
        tokenTypeMap.put(MSpecLexer.OPTIONAL, KEYWORD);
        tokenTypeMap.put(MSpecLexer.PADDING, KEYWORD);
        tokenTypeMap.put(MSpecLexer.PEEK, KEYWORD);
        tokenTypeMap.put(MSpecLexer.RESERVED, KEYWORD);
        tokenTypeMap.put(MSpecLexer.SIMPLE, KEYWORD);
        tokenTypeMap.put(MSpecLexer.STATE, KEYWORD);
        tokenTypeMap.put(MSpecLexer.TYPESWITCH, KEYWORD);
        tokenTypeMap.put(MSpecLexer.UNKNOWN, KEYWORD);
        tokenTypeMap.put(MSpecLexer.VALIDATION, KEYWORD);
        tokenTypeMap.put(MSpecLexer.VIRTUAL, KEYWORD);

        // Type keywords
        tokenTypeMap.put(MSpecLexer.BIT, KEYWORD);
        tokenTypeMap.put(MSpecLexer.BYTE, KEYWORD);
        tokenTypeMap.put(MSpecLexer.INT, KEYWORD);
        tokenTypeMap.put(MSpecLexer.VINT, KEYWORD);
        tokenTypeMap.put(MSpecLexer.UINT, KEYWORD);
        tokenTypeMap.put(MSpecLexer.VUINT, KEYWORD);
        tokenTypeMap.put(MSpecLexer.FLOAT, KEYWORD);
        tokenTypeMap.put(MSpecLexer.UFLOAT, KEYWORD);
        tokenTypeMap.put(MSpecLexer.STRING, KEYWORD);
        tokenTypeMap.put(MSpecLexer.VSTRING, KEYWORD);
        tokenTypeMap.put(MSpecLexer.TIME, KEYWORD);
        tokenTypeMap.put(MSpecLexer.DATE, KEYWORD);
        tokenTypeMap.put(MSpecLexer.DATETIME, KEYWORD);
        tokenTypeMap.put(MSpecLexer.SHOULD_FAIL, KEYWORD);
        tokenTypeMap.put(MSpecLexer.ARRAY_LOOP_TYPE, KEYWORD);

        // Comments
        tokenTypeMap.put(MSpecLexer.LINE_COMMENT, COMMENT);
        tokenTypeMap.put(MSpecLexer.BLOCK_COMMENT, COMMENT);

        // Literals
        tokenTypeMap.put(MSpecLexer.STRING_LITERAL, STRING);
        tokenTypeMap.put(MSpecLexer.INTEGER_LITERAL, NUMBER);
        tokenTypeMap.put(MSpecLexer.FLOAT_LITERAL, NUMBER);
        tokenTypeMap.put(MSpecLexer.HEX_LITERAL, NUMBER);
        tokenTypeMap.put(MSpecLexer.BOOLEAN_LITERAL, KEYWORD);

        // Identifiers
        tokenTypeMap.put(MSpecLexer.IDENTIFIER_LITERAL, IDENTIFIER);

        // Brackets
        tokenTypeMap.put(MSpecLexer.LBRACKET, BRACKET);
        tokenTypeMap.put(MSpecLexer.RBRACKET, BRACKET);
        tokenTypeMap.put(MSpecLexer.LRBRACKET, BRACKET);
        tokenTypeMap.put(MSpecLexer.RRBRACKET, BRACKET);
        tokenTypeMap.put(MSpecLexer.LCBRACKET, BRACKET);
        tokenTypeMap.put(MSpecLexer.RCBRACKET, BRACKET);

        // Whitespace
        tokenTypeMap.put(MSpecLexer.WS, WHITESPACE);
        tokenTypeMap.put(MSpecLexer.NEWLINE, WHITESPACE);
        tokenTypeMap.put(MSpecLexer.EmptyLine, WHITESPACE);
    }

    public static IElementType getTokenType(int antlrTokenType) {
        return tokenTypeMap.getOrDefault(antlrTokenType, UNKNOWN);
    }

    public static class MSpecElementType extends IElementType {
        public MSpecElementType(@org.jetbrains.annotations.NotNull String debugName) {
            super(debugName, MSpecLanguage.INSTANCE);
        }
    }
}
