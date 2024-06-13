/*******************************************************************************
 * Copyright (c) 2017 Chen Chao and other ECD project contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sf.feeling.decompiler.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.sf.feeling.decompiler.JavaDecompilerPlugin;

public final class CommentUtil {

	public static final String LINE_NUMBER_COMMENT = "/\\*\\s*\\d+\\s*\\*/";

	private CommentUtil() {
	}

	public static String clearComments(String input) {
		ASTParser parser = ASTParser.newParser(DecompilerOutputUtil.getMaxJSLLevel()); // AST.JLS3
		CompilerOptions option = new CompilerOptions();
		Map<String, String> options = option.getMap();
		options.put(CompilerOptions.OPTION_Compliance, DecompilerOutputUtil.getMaxDecompileLevel()); // $NON-NLS-1$
		options.put(CompilerOptions.OPTION_Source, DecompilerOutputUtil.getMaxDecompileLevel()); // $NON-NLS-1$
		parser.setCompilerOptions(options);
		parser.setSource(input.toCharArray());
		CompilationUnit unit = (CompilationUnit) parser.createAST(null);
		AST ast = unit.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		Document document = new Document(input);
		TextEdit edits = rewriter.rewriteAST(document, null);
		List<TextEdit> textEdits = new ArrayList<>();
		List<Comment> commentList = unit.getCommentList();
		for (Comment comment : commentList) {
			if (comment.isBlockComment()) {
				BlockComment blockComment = (BlockComment) comment;
				int commentStart = blockComment.getStartPosition();
				int commentLength = blockComment.getLength();
				int commentEnd = commentStart + commentLength;
				String commentText = input.substring(commentStart, commentEnd);
				if (!commentText.trim().matches(LINE_NUMBER_COMMENT)) {
					textEdits.add(new DeleteEdit(commentStart, commentLength));
				}
			}
		}
		edits.addChildren(textEdits.toArray(new TextEdit[textEdits.size()]));
		try {
			edits.apply(document);
		} catch (MalformedTreeException | BadLocationException e) {
			JavaDecompilerPlugin.logError(e, e.getMessage());
		}
		return document.get();
	}
}
