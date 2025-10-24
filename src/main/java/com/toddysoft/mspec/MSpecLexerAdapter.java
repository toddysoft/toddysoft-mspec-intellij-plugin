package com.toddysoft.mspec;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import com.toddysoft.mspec.parser.MSpecLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Adapter between IntelliJ Lexer interface and ANTLR lexer.
 */
public class MSpecLexerAdapter extends LexerBase {
    private MSpecLexer lexer;
    private List<? extends Token> tokens;
    private int currentTokenIndex;
    private CharSequence buffer;
    private int startOffset;
    private int endOffset;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.startOffset = startOffset;
        this.endOffset = endOffset;

        String text = buffer.subSequence(startOffset, endOffset).toString();
        lexer = new MSpecLexer(CharStreams.fromString(text));
        tokens = lexer.getAllTokens();
        currentTokenIndex = 0;
    }

    @Override
    public int getState() {
        return 0;
    }

    @Nullable
    @Override
    public IElementType getTokenType() {
        if (currentTokenIndex >= tokens.size()) {
            return null;
        }
        Token token = tokens.get(currentTokenIndex);
        return MSpecTokenTypes.getTokenType(token.getType());
    }

    @Override
    public int getTokenStart() {
        if (currentTokenIndex >= tokens.size()) {
            return endOffset;
        }
        return startOffset + tokens.get(currentTokenIndex).getStartIndex();
    }

    @Override
    public int getTokenEnd() {
        if (currentTokenIndex >= tokens.size()) {
            return endOffset;
        }
        return startOffset + tokens.get(currentTokenIndex).getStopIndex() + 1;
    }

    @Override
    public void advance() {
        if (currentTokenIndex < tokens.size()) {
            currentTokenIndex++;
        }
    }

    @NotNull
    @Override
    public CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return endOffset;
    }
}
