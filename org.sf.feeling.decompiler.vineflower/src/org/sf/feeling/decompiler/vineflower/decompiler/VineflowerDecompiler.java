/*******************************************************************************
 * Copyright (c) 2017 Chen Chao and other ECD project contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sf.feeling.decompiler.vineflower.decompiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.sf.feeling.decompiler.JavaDecompilerConstants;
import org.sf.feeling.decompiler.JavaDecompilerPlugin;
import org.sf.feeling.decompiler.editor.BaseDecompiler;
import org.sf.feeling.decompiler.editor.IDecompiler;
import org.sf.feeling.decompiler.editor.LineNumberOutputType;
import org.sf.feeling.decompiler.util.ClassUtil;
import org.sf.feeling.decompiler.util.CommentUtil;
import org.sf.feeling.decompiler.util.FileUtil;
import org.sf.feeling.decompiler.util.JarClassExtractor;
import org.sf.feeling.decompiler.util.UnicodeUtil;
import org.sf.feeling.decompiler.vineflower.VineflowerDecompilerPlugin;

public class VineflowerDecompiler extends BaseDecompiler {

	private String source = ""; // $NON-NLS-1$
	private long time, start;
	private String log = ""; //$NON-NLS-1$

	ByteArrayOutputStream loggerStream;

	/**
	 * Performs a <code>Runtime.exec()</code> on Vineflower executable with selected
	 * options.
	 * 
	 * @see IDecompiler#decompile(String, String, String)
	 */
	@Override
	public void decompile(String root, String packege, final String className) {
		if (root == null || packege == null || className == null) {
			return;
		}

		start = System.currentTimeMillis();
		log = ""; //$NON-NLS-1$
		source = ""; //$NON-NLS-1$

		loggerStream = new ByteArrayOutputStream();

		File workingDir = new File(root + "/" + packege); //$NON-NLS-1$

		final Map<String, Object> mapOptions = new HashMap<>();

		mapOptions.put(IFernflowerPreferences.REMOVE_SYNTHETIC, "1"); //$NON-NLS-1$
		mapOptions.put(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "1"); //$NON-NLS-1$
		mapOptions.put(IFernflowerPreferences.DECOMPILE_INNER, "1"); //$NON-NLS-1$
		mapOptions.put(IFernflowerPreferences.DECOMPILE_ENUM, "1"); //$NON-NLS-1$
		mapOptions.put(IFernflowerPreferences.LOG_LEVEL, IFernflowerLogger.Severity.ERROR.name());
		mapOptions.put(IFernflowerPreferences.ASCII_STRING_CHARACTERS, "1"); //$NON-NLS-1$
		if (ClassUtil.isDebug()) {
			mapOptions.put(IFernflowerPreferences.DUMP_ORIGINAL_LINES, "1"); //$NON-NLS-1$
			mapOptions.put(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1"); //$NON-NLS-1$
		}

		File tmpDir;
		try {
			tmpDir = Files.createTempDirectory("ecd_vineflower").toFile();
		} catch (IOException e) {
			throw new RuntimeException("Failed to create temporary working directory", e); //$NON-NLS-1$
		}

		// Work around protected constructor
		class EmbeddedConsoleDecompiler extends ConsoleDecompiler {

			protected EmbeddedConsoleDecompiler() {
				super(tmpDir, mapOptions, new PrintStreamLogger(new PrintStream(loggerStream)));
			}

		}

		String classNameFilterTmp = className.toLowerCase();
		if (classNameFilterTmp.endsWith(".class")) {
			classNameFilterTmp = classNameFilterTmp.substring(0, classNameFilterTmp.length() - 6);
		}
		final String classNameFilter = classNameFilterTmp;

		File[] files = workingDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				name = name.toLowerCase();
				if (name.startsWith(classNameFilter) && name.endsWith(".class")) {
					return true;
				}
				return false;
			}
		});

		try (ConsoleDecompiler decompiler = new EmbeddedConsoleDecompiler()) {
			for (File f : files) {
				decompiler.addSource(f);
			}
			decompiler.decompileContext();

			File classFile = new File(tmpDir, className.replaceAll("(?i)\\.class", ".java")); //$NON-NLS-1$ //$NON-NLS-2$

			source = UnicodeUtil.decode(FileUtil.getContent(classFile));

			classFile.delete();

		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			FileUtil.deltree(tmpDir);
		}

		source = CommentUtil.clearComments(source);

		time = System.currentTimeMillis() - start;
	}

	/**
	 * Vineflower doesn't support decompilation from archives. This method extracts
	 * request class file from the specified archive into temp directory and then
	 * calls <code>decompile</code>.
	 * 
	 * @see IDecompiler#decompileFromArchive(String, String, String)
	 */
	@Override
	public void decompileFromArchive(String archivePath, String packege, String className) {
		start = System.currentTimeMillis();
		File workingDir = new File(
				JavaDecompilerPlugin.getDefault().getPreferenceStore().getString(JavaDecompilerConstants.TEMP_DIR) + "/" //$NON-NLS-1$
						+ System.currentTimeMillis());

		try {
			workingDir.mkdirs();
			JarClassExtractor.extract(archivePath, packege, className, true, workingDir.getAbsolutePath());
			decompile(workingDir.getAbsolutePath(), "", className); //$NON-NLS-1$
		} catch (Exception e) {
			exceptions.add(e);
			JavaDecompilerPlugin.logError(e, e.getMessage());
			return;
		} finally {
			FileUtil.deltree(workingDir);
		}
	}

	@Override
	public long getDecompilationTime() {
		return time;
	}

	/**
	 * @see IDecompiler#getLog()
	 */
	@Override
	public String getLog() {
		if (loggerStream != null) {
			return log + loggerStream.toString();
		}
		return log;
	}

	/**
	 * @see IDecompiler#getSource()
	 */
	@Override
	public String getSource() {
		return source;
	}

	@Override
	public String getDecompilerType() {
		return VineflowerDecompilerPlugin.decompilerType;
	}

	@Override
	public String removeComment(String source) {
		return source;
	}

	@Override
	public boolean supportLevel(int level) {
		return true;
	}

	@Override
	public boolean supportDebugLevel(int level) {
		return true;
	}

	@Override
	public LineNumberOutputType getLineNumberOutputType() {
		return LineNumberOutputType.SINGLE_LINE_COMMENT_END_OF_LINE;
	}

	@Override
	public String getDecompilerName() {
		return VineflowerDecompilerPlugin.decompilerType;
	}

	@Override
	public String getDecompilerVersion() {
		return VineflowerDecompilerPlugin.decompilerVersion;
	}
}