/*******************************************************************************
 * Copyright (c) 2017 Chen Chao and other ECD project contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sf.feeling.decompiler.procyon.decompiler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.lang3.time.StopWatch;
import org.sf.feeling.decompiler.JavaDecompilerConstants;
import org.sf.feeling.decompiler.JavaDecompilerPlugin;
import org.sf.feeling.decompiler.editor.BaseDecompiler;
import org.sf.feeling.decompiler.editor.IDecompiler;
import org.sf.feeling.decompiler.editor.LineNumberOutputType;
import org.sf.feeling.decompiler.procyon.ProcyonDecompilerPlugin;
import org.sf.feeling.decompiler.procyon.decompiler.LineNumberFormatter.LineNumberOption;
import org.sf.feeling.decompiler.util.ClassUtil;
import org.sf.feeling.decompiler.util.CommentUtil;
import org.sf.feeling.decompiler.util.FileUtil;
import org.sf.feeling.decompiler.util.JarClassExtractor;
import org.sf.feeling.decompiler.util.Logger;
import org.sf.feeling.decompiler.util.UnicodeUtil;

import com.strobel.assembler.metadata.DeobfuscationUtilities;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.Language;
import com.strobel.decompiler.languages.LineNumberPosition;
import com.strobel.decompiler.languages.TypeDecompilationResults;

public class ProcyonDecompiler extends BaseDecompiler {

	private String source = ""; // $NON-NLS-1$ //$NON-NLS-1$
	private long time;
	private String log = ""; //$NON-NLS-1$

	/**
	 * @see IDecompiler#decompile(String, String, String)
	 */
	@Override
	public void decompile(String root, String packege, String className) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		log = ""; //$NON-NLS-1$
		source = ""; //$NON-NLS-1$
		File workingDir = new File(root + "/" + packege); //$NON-NLS-1$

		final String classPathStr = new File(workingDir, className).getAbsolutePath();

		boolean includeLineNumbers = false;
		boolean stretchLines = false;
		if (ClassUtil.isDebug()) {
			includeLineNumbers = true;
			stretchLines = true;
		}

		DecompilationOptions decompilationOptions = new DecompilationOptions();

		DecompilerSettings settings = DecompilerSettings.javaDefaults();
		settings.setTypeLoader(new com.strobel.assembler.InputTypeLoader());
		settings.setForceExplicitImports(true);

		decompilationOptions.setSettings(settings);
		decompilationOptions.setFullDecompilation(true);

		MetadataSystem metadataSystem = new NoRetryMetadataSystem(decompilationOptions.getSettings().getTypeLoader());
		metadataSystem.setEagerMethodLoadingEnabled(false);

		TypeReference type = metadataSystem.lookupType(classPathStr);

		TypeDefinition resolvedType;
		if ((type == null) || ((resolvedType = type.resolve()) == null)) {
			System.err.printf("!!! ERROR: Failed to load class %s.\n", //$NON-NLS-1$
					new Object[] { classPathStr });
			return;
		}

		DeobfuscationUtilities.processType(resolvedType);

		String property = "java.io.tmpdir"; //$NON-NLS-1$
		String tempDir = System.getProperty(property);
		File classFile = new File(tempDir, System.currentTimeMillis() + className);
		try {
			TypeDecompilationResults results;
			try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(classFile)))) {

				PlainTextOutput output = new PlainTextOutput(writer);

				output.setUnicodeOutputEnabled(decompilationOptions.getSettings().isUnicodeOutputEnabled());

				Language lang = decompilationOptions.getSettings().getLanguage();

				// perform the actual decompilation
				results = lang.decompileType(resolvedType, output, decompilationOptions);
			}

			List<LineNumberPosition> lineNumberPositions = results.getLineNumberPositions();

			if (includeLineNumbers || stretchLines) {
				EnumSet<LineNumberOption> lineNumberOptions = EnumSet.noneOf(LineNumberOption.class);

				if (includeLineNumbers) {
					lineNumberOptions.add(LineNumberFormatter.LineNumberOption.LEADING_COMMENTS);
				}

				if (stretchLines) {
					lineNumberOptions.add(LineNumberFormatter.LineNumberOption.STRETCHED);
				}

				LineNumberFormatter lineFormatter = new LineNumberFormatter(classFile, lineNumberPositions,
						lineNumberOptions);

				source = lineFormatter.reformatFile();
			} else {
				source = FileUtil.getContent(classFile);
			}

		} catch (IOException e) {
			exceptions.add(e);
			Logger.error(e);
		}

		source = UnicodeUtil.decode(source);

		classFile.delete();

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
			time = stopWatch.getTime();
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
		return ProcyonDecompilerPlugin.decompilerType;
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
	public String getDecompilerName() {
		return ProcyonDecompilerPlugin.decompilerType;
	}

	@Override
	public String getDecompilerVersion() {
		return ProcyonDecompilerPlugin.decompilerVersion;
	}

	@Override
	public LineNumberOutputType getLineNumberOutputType() {
		// Procyon generates line number comments at the beginning of the line but only
		// for those lines source line information is present. The other lines are just
		// indented by spaces.
		return LineNumberOutputType.BLOCK_COMMENT_BEGIN_OF_LINE;
	}

}