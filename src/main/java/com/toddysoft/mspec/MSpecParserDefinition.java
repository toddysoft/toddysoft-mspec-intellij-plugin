package com.toddysoft.mspec;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import com.toddysoft.mspec.parser.MSpecLexer;
import com.toddysoft.mspec.psi.MSpecFile;
import org.jetbrains.annotations.NotNull;

/**
 * Parser definition for MSpec language.
 */
public class MSpecParserDefinition implements ParserDefinition {
    public static final IFileElementType FILE = new IFileElementType(MSpecLanguage.INSTANCE);

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new MSpecLexerAdapter();
    }

    @Override
    public @NotNull PsiParser createParser(Project project) {
        return new MSpecParser();
    }

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return FILE;
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return MSpecTokenSets.COMMENTS;
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return MSpecTokenSets.STRINGS;
    }

    @NotNull
    @Override
    public PsiElement createElement(ASTNode node) {
        return new MSpecPsiElement(node);
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new MSpecFile(viewProvider);
    }
}
