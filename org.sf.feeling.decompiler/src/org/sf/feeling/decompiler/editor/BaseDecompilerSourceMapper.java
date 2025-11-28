/*******************************************************************************
 * Copyright (c) 2017 Chen Chao and other ECD project contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sf.feeling.decompiler.editor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.core.ClassFile;
import org.eclipse.jdt.internal.core.ExternalPackageFragmentRoot;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.internal.core.SourceMapper;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jface.preference.IPreferenceStore;
import org.sf.feeling.decompiler.JavaDecompilerConstants;
import org.sf.feeling.decompiler.JavaDecompilerPlugin;
import org.sf.feeling.decompiler.util.ClassUtil;
import org.sf.feeling.decompiler.util.DecompileUtil;
import org.sf.feeling.decompiler.util.DecompilerOutputUtil;
import org.sf.feeling.decompiler.util.Logger;
import org.sf.feeling.decompiler.util.ReflectionUtils;
import org.sf.feeling.decompiler.util.SortMemberUtil;
import org.sf.feeling.decompiler.util.SourceMapperUtil;
import org.sf.feeling.decompiler.util.UIUtil;

public abstract class BaseDecompilerSourceMapper extends DecompilerSourceMapper {

	private static Map<String, String> options = new HashMap<>();

	static {
		CompilerOptions option = new CompilerOptions();
		options = option.getMap();
		options.put(CompilerOptions.OPTION_Compliance, DecompilerOutputUtil.getMaxDecompileLevel()); // $NON-NLS-1$
		options.put(CompilerOptions.OPTION_Source, DecompilerOutputUtil.getMaxDecompileLevel()); // $NON-NLS-1$
	}

	protected IDecompiler originalDecompiler;
	private IDecompiler usedDecompiler;
	private String classLocation;

	public BaseDecompilerSourceMapper(IPath sourcePath, String rootPath) {
		this(sourcePath, rootPath, options);
	}

	public BaseDecompilerSourceMapper(IPath sourcePath, String rootPath, Map<String, String> options) {
		super(sourcePath, rootPath, options);
	}

	@Override
	public char[] findSource(IType type, IBinaryType info) {
		IPreferenceStore prefs = JavaDecompilerPlugin.getDefault().getPreferenceStore();
		boolean ignoreExistingSource = prefs.getBoolean(JavaDecompilerConstants.IGNORE_EXISTING);

		Collection<Exception> exceptions = new LinkedList<>();
		IPackageFragment pkgFrag = type.getPackageFragment();
		IPackageFragmentRoot root = (IPackageFragmentRoot) pkgFrag.getParent();

		JavaDecompilerPlugin.getDefault().syncLibrarySource(root);
		char[] attachedSource = null;

		if (UIUtil.requestFromJavadocHover() && !fromInput(type) && ignoreExistingSource) {
			sourceRanges.remove(type);
			attachedSource = originalSourceMapper.get(root).findSource(type, info);
			return attachedSource;
		}

		if (originalSourceMapper.containsKey(root)) {
			attachedSource = originalSourceMapper.get(root).findSource(type, info);

			if (attachedSource != null && !ignoreExistingSource) {
				updateSourceRanges(type, attachedSource);
				isAttachedSource = true;
				mapSourceSwitch(type, attachedSource, true);
				SourceMapperUtil.mapSource(((PackageFragmentRoot) root).getSourceMapper(), type, attachedSource, info);
				return attachedSource;
			}
		}

		if (info == null) {
			if (ignoreExistingSource) {
				return null;
			}
			return attachedSource;
		}

		try {
			if (root instanceof PackageFragmentRoot) {
				PackageFragmentRoot pfr = (PackageFragmentRoot) root;

				SourceMapper sourceMapper = pfr.getSourceMapper();

				if (!originalSourceMapper.containsKey(root)) {
					ReflectionUtils.setFieldValue(this, "options", //$NON-NLS-1$
							ReflectionUtils.getFieldValue(sourceMapper, "options")); //$NON-NLS-1$
					originalSourceMapper.put(root, sourceMapper);
				}

				if (sourceMapper != null && !ignoreExistingSource
						&& !(sourceMapper instanceof DecompilerSourceMapper)) {
					attachedSource = sourceMapper.findSource(type, info);
					if (attachedSource != null) {
						updateSourceRanges(type, attachedSource);
						isAttachedSource = true;
						mapSourceSwitch(type, attachedSource, true);
						SourceMapperUtil.mapSource(((PackageFragmentRoot) root).getSourceMapper(), type, attachedSource,
								info);
						return attachedSource;
					}
				}

				if (sourceMapper != this) {
					pfr.setSourceMapper(this);
				}
			}
		} catch (JavaModelException e) {
			JavaDecompilerPlugin.logError(e, "Could not set source mapper."); //$NON-NLS-1$
		}

		isAttachedSource = false;

		if (JavaDecompilerPlugin.getDefault().isAutoAttachSource()) {
			JavaDecompilerPlugin.getDefault().attachSource(root, false);
		}

		String className = new String(info.getName());
		String fullName = new String(info.getFileName());
		int classNameIndex = fullName.lastIndexOf(className);
		if (classNameIndex < 0) {
			String msg = String.format("Unable to find className \"%s\" in fullName \"%s\"", className, fullName); //$NON-NLS-1$
			JavaDecompilerPlugin.logError(null, msg);
			return messageWithExceptionsAsSourceCode(msg, exceptions);
		}
		className = fullName.substring(classNameIndex);

		int index = className.lastIndexOf('/');
		className = className.substring(index + 1);

		classLocation = ""; //$NON-NLS-1$

		usedDecompiler = decompile(type, exceptions, root, className);

		if (noDecompiledSourceCodeAvailable(usedDecompiler)) {
			if (usedDecompiler == null) {
				String msg = String.format("Failed to decompile using %s", getDecompilerName());
				exceptions.add(new RuntimeException(msg));
				return messageWithExceptionsAsSourceCode(msg, exceptions);
			}
		}

		String code = usedDecompiler.getSource();

		boolean showReport = prefs.getBoolean(JavaDecompilerConstants.PREF_DISPLAY_METADATA);
		if (!showReport) {
			code = usedDecompiler.removeComment(code);
		}

		boolean showLineNumber = prefs.getBoolean(JavaDecompilerConstants.PREF_DISPLAY_LINE_NUMBERS);
		boolean align = prefs.getBoolean(JavaDecompilerConstants.ALIGN);
		if ((showLineNumber && align) || UIUtil.isDebugPerspective()
				|| JavaDecompilerPlugin.getDefault().isDebugMode()) {
			if (showReport) {
				code = usedDecompiler.removeComment(code);
			}
			DecompilerOutputUtil decompilerOutputUtil = new DecompilerOutputUtil(usedDecompiler, code);
			code = decompilerOutputUtil.realign();
		}

		StringBuffer source = new StringBuffer();

		if (!(UIUtil.isDebugPerspective() || JavaDecompilerPlugin.getDefault().isDebugMode())) {
			boolean useSorter = prefs.getBoolean(JavaDecompilerConstants.USE_ECLIPSE_SORTER);
			if (useSorter) {
				className = new String(info.getName());
				fullName = new String(info.getFileName());
				if (fullName.lastIndexOf(className) != -1) {
					className = fullName.substring(fullName.lastIndexOf(className));
				}

				code = SortMemberUtil.sortMember(type.getPackageFragment().getElementName(), className, code);
			}

			source.append(formatSource(code));

			if (showReport) {
				printDecompileReport(source, classLocation, exceptions);
			}
		} else {
			source.append(code);
		}

		char[] sourceAsCharArray = source.toString().toCharArray();
		if (originalSourceMapper.containsKey(root)) {
			SourceMapper rootSourceMapper = originalSourceMapper.get(root);
			if (rootSourceMapper.findSource(type, info) == null) {
				SourceMapperUtil.mapSource(rootSourceMapper, type, sourceAsCharArray, info);
			}
		}

		updateSourceRanges(type, sourceAsCharArray);
		return sourceAsCharArray;
	}

	private boolean noDecompiledSourceCodeAvailable(IDecompiler decompiler) {
		return usedDecompiler == null || usedDecompiler.getSource() == null || usedDecompiler.getSource().isEmpty();
	}

	private char[] messageWithExceptionsAsSourceCode(String message, Collection<Exception> exceptions) {
		StringBuffer source = new StringBuffer(1024);
		source.append("/*"); //$NON-NLS-1$
		source.append(message.replaceAll("\\/\\*", "\\*"));
		if (exceptions != null && !exceptions.isEmpty()) {
			source.append("\n\n");
			logExceptions(exceptions, source);
		}
		source.append("*/"); //$NON-NLS-1$

		return source.toString().toCharArray();
	}

	private void updateSourceRanges(IType type, char[] attachedSource) {
		if (type.getParent() instanceof ClassFile) {
			try {
				DecompileUtil.updateSourceRanges(((ClassFile) type.getParent()), new String(attachedSource));
			} catch (JavaModelException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private boolean fromInput(IType type) {
		JavaDecompilerClassFileEditor editor = UIUtil.getActiveEditor();
		if (editor != null && editor.getEditorInput() instanceof IClassFileEditorInput) {
			IClassFile input = ((IClassFileEditorInput) editor.getEditorInput()).getClassFile();
			IType inputType = (IType) ReflectionUtils.invokeMethod(input, "getOuterMostEnclosingType", //$NON-NLS-1$
					new Class[0], new Object[0]);
			return type.equals(inputType);
		}
		return false;
	}

	private IDecompiler decompile(IType type, Collection<Exception> exceptions, IPackageFragmentRoot root,
			String className) {

		originalDecompiler.clearExceptions();

		IDecompiler result = null;

		String pkg = type.getPackageFragment().getElementName().replace('.', '/');

		Boolean displayNumber = null;
		if (UIUtil.isDebugPerspective() || JavaDecompilerPlugin.getDefault().isDebugMode()) {
			displayNumber = JavaDecompilerPlugin.getDefault().isDisplayLineNumber();
			JavaDecompilerPlugin.getDefault().displayLineNumber(Boolean.TRUE);
		}

		try {
			if (root.isArchive()) {
				String archivePath = getArchivePath(root);
				classLocation += archivePath;

				try (InputStream in = new ByteArrayInputStream(type.getClassFile().getBytes())) {
					result = ClassUtil.checkAvailableDecompiler(originalDecompiler, in);
				} catch (JavaModelException e) {
					Logger.error(e.toString());
					result = originalDecompiler;
				}
				result.decompileFromArchive(archivePath, pkg, className);
			} else {
				String rootLocation = null;
				try {
					if (root.getUnderlyingResource() != null) {
						rootLocation = root.getUnderlyingResource().getLocation().toOSString();
						classLocation += rootLocation + "/" //$NON-NLS-1$
								+ pkg + "/" //$NON-NLS-1$
								+ className;
					} else if (root instanceof ExternalPackageFragmentRoot) {
						rootLocation = ((ExternalPackageFragmentRoot) root).getPath().toOSString();
						classLocation += rootLocation + "/" //$NON-NLS-1$
								+ pkg + "/" //$NON-NLS-1$
								+ className;
					} else {
						rootLocation = root.getPath().toOSString();
						classLocation += rootLocation + "/" //$NON-NLS-1$
								+ pkg + "/" //$NON-NLS-1$
								+ className;
					}

					result = ClassUtil.checkAvailableDecompiler(originalDecompiler, new File(classLocation));
					result.decompile(rootLocation, pkg, className);
				} catch (JavaModelException e) {
					exceptions.add(e);
				}
			}
		} catch (Exception e) {
			exceptions.add(e);
		}

		if (displayNumber != null) {
			JavaDecompilerPlugin.getDefault().displayLineNumber(displayNumber);
		}
		return result;
	}

	@Override
	public String decompileFile(String decompilerType, File file) {
		IPreferenceStore prefs = JavaDecompilerPlugin.getDefault().getPreferenceStore();

		Boolean displayNumber = null;
		if (UIUtil.isDebugPerspective() || JavaDecompilerPlugin.getDefault().isDebugMode()) {
			displayNumber = JavaDecompilerPlugin.getDefault().isDisplayLineNumber();
			JavaDecompilerPlugin.getDefault().displayLineNumber(Boolean.TRUE);
		}

		IDecompiler currentDecompiler = ClassUtil.checkAvailableDecompiler(originalDecompiler, file);
		usedDecompiler = currentDecompiler;

		currentDecompiler.decompile(file.getParentFile().getAbsolutePath(), "", //$NON-NLS-1$
				file.getName());

		if (displayNumber != null) {
			JavaDecompilerPlugin.getDefault().displayLineNumber(displayNumber);
		}

		if (currentDecompiler.getSource() == null || currentDecompiler.getSource().length() == 0) {
			return null;
		}

		String code = currentDecompiler.getSource();

		boolean showReport = prefs.getBoolean(JavaDecompilerConstants.PREF_DISPLAY_METADATA);
		if (!showReport) {
			code = currentDecompiler.removeComment(code);
		}

		boolean showLineNumber = prefs.getBoolean(JavaDecompilerConstants.PREF_DISPLAY_LINE_NUMBERS);
		boolean align = prefs.getBoolean(JavaDecompilerConstants.ALIGN);
		if ((showLineNumber && align) || UIUtil.isDebugPerspective()
				|| JavaDecompilerPlugin.getDefault().isDebugMode()) {
			if (showReport) {
				code = currentDecompiler.removeComment(code);
			}
			DecompilerOutputUtil decompilerOutputUtil = new DecompilerOutputUtil(currentDecompiler, code);
			code = decompilerOutputUtil.realign();
		}

		StringBuffer source = new StringBuffer();

		if (!(UIUtil.isDebugPerspective() || JavaDecompilerPlugin.getDefault().isDebugMode())) {
			source.append(formatSource(code));

			if (showReport) {
				Collection<Exception> exceptions = new LinkedList<>();
				exceptions.addAll(currentDecompiler.getExceptions());
				printDecompileReport(source, file.getAbsolutePath(), exceptions);
			}
		} else {
			source.append(code);
		}
		return source.toString();
	}

	protected void logExceptions(Collection<Exception> exceptions, StringBuffer buffer) {
		if (exceptions.isEmpty()) {
			return;
		}
		buffer.append("\n\tCaught exceptions:"); //$NON-NLS-1$
		if (exceptions == null || exceptions.isEmpty()) {
			return; // nothing to do
		}
		buffer.append("\n"); //$NON-NLS-1$
		StringWriter stackTraces = new StringWriter();
		try (PrintWriter stackTracesP = new PrintWriter(stackTraces)) {

			Iterator<Exception> i = exceptions.iterator();
			while (i.hasNext()) {
				Exception ex = i.next();
				if (ex instanceof NoStackTraceException) {
					stackTracesP.append("\t");
					stackTracesP.println(ex.getMessage());
					continue;
				}
				ex.printStackTrace(stackTracesP);
				stackTracesP.println(""); //$NON-NLS-1$
			}
			stackTracesP.flush();
		}
		buffer.append(stackTraces.toString());
	}

	protected abstract String getDecompilerName();

	protected abstract String getDecompilerVersion();

	protected void printDecompileReport(StringBuffer source, String fileLocation, Collection<Exception> exceptions) {
		String log = usedDecompiler.getLog();
		String logMsg = log.replaceAll("\t", "") //$NON-NLS-1$ //$NON-NLS-2$
				.replaceAll("\n\\s*", "\n\t"); //$NON-NLS-1$ //$NON-NLS-2$

		source.append("\n\n/*"); //$NON-NLS-1$
		source.append("\n\tDECOMPILATION REPORT\n"); //$NON-NLS-1$
		if (usedDecompiler != originalDecompiler) {
			source.append("\n\tWARNING: used decompiler was changed from ");
			source.append(originalDecompiler.getDecompilerName());
			source.append(" to ");
			source.append(usedDecompiler.getDecompilerName());
			logExceptions(originalDecompiler.getExceptions(), source);
		}

		source.append("\n\tDecompiled from: "); //$NON-NLS-1$
		source.append(fileLocation);
		source.append("\n\tTotal time: "); //$NON-NLS-1$
		source.append(usedDecompiler.getDecompilationTime());
		source.append(" ms\n\t"); //$NON-NLS-1$
		source.append(logMsg);
		exceptions.addAll(usedDecompiler.getExceptions());
		logExceptions(exceptions, source);
		String decompiler = usedDecompiler.getDecompilerName();
		String ver = usedDecompiler.getDecompilerVersion();
		if (decompiler != null) {
			source.append("\n\tDecompiled with "); //$NON-NLS-1$
			source.append(decompiler);
			if (ver != null) {
				source.append(" version "); //$NON-NLS-1$
				source.append(ver);
			}
			source.append(".\n"); //$NON-NLS-1$
		}
		source.append("*/"); //$NON-NLS-1$
	}

}
