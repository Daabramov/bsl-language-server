/*
 * This file is a part of BSL Language Server.
 *
 * Copyright © 2018-2019
 * Alexey Sosnoviy <labotamy@yandex.ru>, Nikita Gryzlov <nixel2007@gmail.com>
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * BSL Language Server is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * BSL Language Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BSL Language Server.
 */
package org.github._1c_syntax.intellij.bsl.lsp.server;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.github._1c_syntax.intellij.bsl.lsp.server.providers.DiagnosticProvider;
import org.github._1c_syntax.intellij.bsl.lsp.server.providers.HoverProvider;
import org.github._1c_syntax.parser.BSLLexer;
import org.github._1c_syntax.parser.BSLParser;
import org.github._1c_syntax.parser.BSLParser.FileContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class BSLTextDocumentService implements TextDocumentService, LanguageClientAware {

  private final Map<String, FileContext> documents = Collections.synchronizedMap(new HashMap<>());

  private BSLLexer lexer;
  private BSLParser parser;
  private LanguageClient client;

  public BSLTextDocumentService() {
    // no-op
  }

  @Override
  public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
    List<CompletionItem> completionItems = new ArrayList<>();
    completionItems.add(new CompletionItem("Hello World"));
    return CompletableFuture.completedFuture(Either.forLeft(completionItems));
  }

  @Override
  public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<Hover> hover(TextDocumentPositionParams position) {
    FileContext fileTree = documents.get(position.getTextDocument().getUri());
    Optional<Hover> hover = HoverProvider.getHover(position, fileTree);
    return CompletableFuture.completedFuture(hover.orElse(null));
  }

  @Override
  public CompletableFuture<SignatureHelp> signatureHelp(TextDocumentPositionParams position) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<List<? extends Location>> definition(TextDocumentPositionParams position) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(TextDocumentPositionParams position) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<List<? extends SymbolInformation>> documentSymbol(DocumentSymbolParams params) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<List<? extends Command>> codeAction(CodeActionParams params) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams params) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    TextDocumentItem textDocumentItem = params.getTextDocument();
    FileContext fileTree = getFileContext(textDocumentItem.getText());

    documents.put(textDocumentItem.getUri(), fileTree);
    validate(textDocumentItem.getUri(), fileTree);
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    // TODO: Place to optimize -> migrate to #TextDocumentSyncKind.INCREMENTAL and build changed parse tree
    TextDocumentIdentifier textDocumentItem = params.getTextDocument();
    FileContext fileTree = getFileContext(params.getContentChanges().get(0).getText());

    documents.put(textDocumentItem.getUri(), fileTree);
    validate(textDocumentItem.getUri(), fileTree);
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    this.documents.remove(params.getTextDocument().getUri());
  }

  @Override
  public void didSave(DidSaveTextDocumentParams params) {
    // no-op
  }

  @Override
  public void connect(LanguageClient client) {
    this.client = client;
  }

  public void reset() {
    documents.clear();
  }

  private FileContext getFileContext(String textDocumentContent) {
    CharStream input = CharStreams.fromString(textDocumentContent);
    if (lexer == null) {
      lexer = new BSLLexer(input);
    } else {
      lexer.setInputStream(input);
    }

    CommonTokenStream tokens = new CommonTokenStream(lexer);

    if (parser == null) {
      parser = new BSLParser(tokens);
    } else {
      parser.setInputStream(tokens);
    }

    return parser.file();
  }

  private void validate(String uri, FileContext fileTree) {
    DiagnosticProvider.computeAndPublishDiagnostics(client, uri, fileTree);
  }

}
