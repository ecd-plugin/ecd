/*******************************************************************************
 * Copyright (c) 2017 Chen Chao and other ECD project contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sf.feeling.decompiler.fernflower.decompiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.sf.feeling.decompiler.JavaDecompilerConstants;
import org.sf.feeling.decompiler.JavaDecompilerPlugin;
import org.sf.feeling.decompiler.editor.BaseDecompiler;
import org.sf.feeling.decompiler.editor.IDecompiler;
import org.sf.feeling.decompiler.editor.LineNumberOutputType;
import org.sf.feeling.decompiler.util.CommentUtil;
import org.sf.feeling.decompiler.util.FileUtil;
import org.sf.feeling.decompiler.util.JarClassExtractor;
import org.sf.feeling.decompiler.util.UIUtil;
import org.sf.feeling.decompiler.util.UnicodeUtil;

public class FernFlowerDecompiler extends BaseDecompiler {

	public static final String decompilerType = "FernFlower"; //$NON-NLS-1$ \r\n"
	public static final String decompilerVersion = "232.10203.10"; //$NON-NLS-1$ \r\n"

	private long time = 0;
	private String source = ""; // $NON-NLS-1$ //$NON-NLS-1$
	private String log = ""; //$NON-NLS-1$

	ByteArrayOutputStream loggerStream;

	/**
	 * Performs a <code>Runtime.exec()</code> on FernFlower executable with selected
	 * options.
	 * 
	 * @see IDecompiler#decompile(String, String, String)
	 */
	@Override
	public void decompile(String root, String packege, final String className) {
		if (root == null || packege == null || className == null) {
			return;
		}

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		exceptions.clear();
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

		if (UIUtil.isDebug()) {
			mapOptions.put(IFernflowerPreferences.DUMP_ORIGINAL_LINES, "1"); //$NON-NLS-1$
			mapOptions.put(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1"); //$NON-NLS-1$
		}

		final File tmpDir = new File(System.getProperty("java.io.tmpdir"), //$NON-NLS-1$
				String.valueOf(System.currentTimeMillis()));

		if (!tmpDir.exists()) {
			tmpDir.mkdirs();
		}

		String classNameFilterTmp = className.toLowerCase();
		if (classNameFilterTmp.endsWith(".class")) {
			classNameFilterTmp = classNameFilterTmp.substring(0, classNameFilterTmp.length() - 6);
		}
		final String classNameFilter = classNameFilterTmp;

		// Work around to get access to protected constructor
		class EmbeddedConsoleDecompiler extends ConsoleDecompiler {

			protected EmbeddedConsoleDecompiler() {
				super(tmpDir, mapOptions, new PrintStreamLogger(new PrintStream(loggerStream)));
			}

		}
		ConsoleDecompiler decompiler = new EmbeddedConsoleDecompiler();

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

		for (File f : files) {
			decompiler.addSource(f);
		}

		decompiler.decompileContext();

		File classFile = new File(tmpDir, className.replaceAll("(?i)\\.class", ".java")); //$NON-NLS-1$ //$NON-NLS-2$

		source = UnicodeUtil.decode(FileUtil.getContent(classFile));

		classFile.delete();

		FileUtil.deltree(tmpDir);

		source = CommentUtil.clearComments(source);

		time = stopWatch.getTime();
	}

	/**
	 * Jad doesn't support decompilation from archives. This methods extracts
	 * request class file from the specified archive into temp directory and then
	 * calls <code>decompile</code>.
	 * 
	 * @see IDecompiler#decompileFromArchive(String, String, String)
	 */
	@Override
	public void decompileFromArchive(String archivePath, String packege, String className) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
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
		time = stopWatch.getTime();
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
		return decompilerType;
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
		return FernFlowerDecompiler.decompilerType;
	}

	@Override
	public String getDecompilerVersion() {
		return FernFlowerDecompiler.decompilerVersion;
	}

}