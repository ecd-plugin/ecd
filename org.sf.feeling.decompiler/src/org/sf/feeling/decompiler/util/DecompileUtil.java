/*******************************************************************************
 * Copyright (c) 2017 Chen Chao and other ECD project contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sf.feeling.decompiler.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.core.ClassFile;
import org.eclipse.jdt.internal.core.SourceMapper;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.sf.feeling.decompiler.editor.DecompilerSourceMapper;
import org.sf.feeling.decompiler.editor.SourceMapperFactory;

public class DecompileUtil {

	public static String decompile(IClassFile cf, String type, boolean always, boolean reuseBuf, boolean force)
			throws CoreException {
		String decompilerType = type;
		String origSrc = cf.getSource();
		// have to check our mark since all line comments are stripped
		// in debug align mode
		if (origSrc == null || always || !reuseBuf || force) {
			DecompilerSourceMapper sourceMapper = SourceMapperFactory.getSourceMapper(decompilerType);
			char[] src = sourceMapper.findSource(cf.getType());
			if (src == null) {
				return origSrc;
			}
			return new String(src);
		}

		return origSrc;
	}

	public static String decompileInput(FileStoreEditorInput input, String decompilerType) {
		DecompilerSourceMapper sourceMapper = SourceMapperFactory.getSourceMapper(decompilerType);
		File file = new File(input.getURI());
		return sourceMapper.decompileFile(decompilerType, file);
	}

	public static String getPackageName(String source) {
		Pattern p = Pattern.compile("(?i)package\\s+\\S+"); //$NON-NLS-1$

		Matcher m = p.matcher(source);
		if (m.find()) {
			return m.group().replace("package", "") //$NON-NLS-1$ //$NON-NLS-2$
					.replace(";", "") //$NON-NLS-1$ //$NON-NLS-2$
					.trim();
		}
		return null;
	}

	public static String updateBuffer(IClassFile cf, String origSrc) throws JavaModelException {
		updateSourceRanges(cf, origSrc);
		return origSrc;
	}

	public static void updateSourceRanges(IClassFile cf, String contents) throws JavaModelException {
		if (cf instanceof ClassFile) {
			ClassFile classFile = (ClassFile) cf;
			Object info = EclipseCompatibilityHelper.javaElementGetElementInfo(classFile);
			IBinaryType typeInfo = info instanceof IBinaryType ? (IBinaryType) info : null;
			SourceMapper mapper = classFile.getSourceMapper();
			// private org.eclipse.jdt.internal.core.ClassFile.getOuterMostEnclosingType()
			IType type = (IType) ReflectionUtils.invokeMethod(classFile, "getOuterMostEnclosingType");

			HashMap sourceRange = (HashMap) ReflectionUtils.getFieldValue(mapper, "sourceRanges"); //$NON-NLS-1$
			sourceRange.remove(type);
			SourceMapperUtil.mapSource(mapper, type, contents.toCharArray(), typeInfo);
		}
	}

	public static String deleteOneEmptyLine(String origSrc) {
		int index = origSrc.indexOf("{"); //$NON-NLS-1$
		if (index == -1) {
			return origSrc;
		}

		String prefix = origSrc.substring(0, index + 1);
		String suffix = origSrc.substring(index + 1);

		List<String> splits = new ArrayList(Arrays.asList(prefix.split("\n"))); //$NON-NLS-1$
		boolean flag = false;
		for (int i = 0; i < splits.size(); i++) {
			String split = splits.get(i);
			if (split.trim().length() == 0) {
				splits.remove(i);
				flag = true;
				break;
			}
		}

		if (flag) {
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < splits.size(); i++) {
				String split = splits.get(i);
				buffer.append(split);
				if (i < splits.size() - 1) {
					buffer.append("\n"); //$NON-NLS-1$
				}
			}
			buffer.append(suffix);
			return buffer.toString();
		}
		return null;
	}
}
