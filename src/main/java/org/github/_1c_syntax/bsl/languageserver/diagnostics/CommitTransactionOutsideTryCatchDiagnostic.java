/*
 * This file is a part of BSL Language Server.
 *
 * Copyright © 2018-2019
 * Alexey Sosnoviy <labotamy@gmail.com>, Nikita Gryzlov <nixel2007@gmail.com> and contributors
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
package org.github._1c_syntax.bsl.languageserver.diagnostics;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.Trees;
import org.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticMetadata;
import org.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity;
import org.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import org.github._1c_syntax.bsl.parser.BSLParser;
import org.github._1c_syntax.bsl.parser.BSLParserRuleContext;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@DiagnosticMetadata(
	type = DiagnosticType.ERROR,
	severity = DiagnosticSeverity.MAJOR,
	minutesToFix = 10
)
public class CommitTransactionOutsideTryCatchDiagnostic extends AbstractVisitorDiagnostic {

	private Pattern endTransaction = Pattern.compile(
		"ЗафиксироватьТранзакцию|CommitTransaction",
		Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private BSLParserRuleContext nodeEndTransaction = null;
	private BSLParserRuleContext nodeEndFile = null;

	@Override
	public ParseTree visitStatement(BSLParser.StatementContext ctx) {
		int ctxType = ctx.getStart().getType();

		if(ctxType == BSLParser.TRY_KEYWORD || ctxType == BSLParser.EXCEPT_KEYWORD) {
			if(ctxType == BSLParser.TRY_KEYWORD && nodeEndTransaction != null) {
				diagnosticStorage.addDiagnostic(nodeEndTransaction);
			}
			nodeEndTransaction = null;
			return super.visitStatement(ctx);
		}

		// Это код после ЗафиксироватьТранзакцию
		if(nodeEndTransaction != null) {
			diagnosticStorage.addDiagnostic(nodeEndTransaction);
			nodeEndTransaction = null;
		}

		// Ищем только в идентификаторах
		if(ctxType == BSLParser.IDENTIFIER && endTransaction.matcher(ctx.getText()).find()) {
			nodeEndTransaction = ctx;
		}

		// Если это код в конце модуля, ЗафиксироватьТранзакию был/есть тогда фиксируем
		if(nodeEndFile != null && nodeEndTransaction != null && nodeEndFile == ctx) {
			diagnosticStorage.addDiagnostic(nodeEndTransaction);
			nodeEndTransaction = null;
		}
		return super.visitStatement(ctx);
	}

	@Override
	public ParseTree visitFileCodeBlock(BSLParser.FileCodeBlockContext ctx) {
		// Находим последний стейт в модуле и запоминаем его
		List<ParseTree> statements = Trees.findAllRuleNodes(ctx, BSLParser.RULE_statement).stream().collect(Collectors.toList());
		if(statements.size() > 0) {
			nodeEndFile = (BSLParserRuleContext) statements.get(statements.size() - 1);
		}
		return super.visitFileCodeBlock(ctx);
	}
}
