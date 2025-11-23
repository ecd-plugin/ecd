/*******************************************************************************
 * Copyright (c) 2017 Chen Chao and other ECD project contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sf.feeling.decompiler.editor;

import java.util.List;

/**
 * Generic Java Decompiler Interface. Decompilers work in file system terms, so
 * methods take file path-like parameters as opposed to Java style package and
 * class names. For instance, if a class com.acme.Foo that is be found under
 * directory /bar needs to be decompiled then the following should be passed to
 * a <code>IDecompiler</code> instance: root = /bar packege = com/acme className
 * = Foo.class
 */
public interface IDecompiler {

	/**
	 * Decompiles a class file located in file system (not in archive)
	 * 
	 * @param root      path to the directory which is root for this class package
	 *                  hierarchy
	 * @param packege   file path like package name
	 * @param className file name of the class file
	 */
	void decompile(String root, String packege, String className);

	/**
	 * Decompiles a class file located in archive.
	 * 
	 * @param archivePath path to archive that contains the class to be decompiled
	 * @param packege     file path like package name
	 * @param className   file name of the class file
	 */
	void decompileFromArchive(String archivePath, String packege, String className);

	/**
	 * @return time taken by decompilation
	 */
	long getDecompilationTime();

	/**
	 * If any exceptions occurred during decompilation they should be included into
	 * the <code>List</code> returned by this method.
	 * 
	 * @return non-<code>null</code> value which is a list containing
	 *         <code>java.lang.Exception</code>'s
	 */
	List<Exception> getExceptions();

	void clearExceptions();

	/**
	 * @return decompilation log specific to physical decompiler
	 */
	String getLog();

	/**
	 * @return Class file source which is the result of decompilation.
	 */
	String getSource();

	String getDecompilerType();

	/**
	 * Get the way the decompiler uses for printing source code line numbers in the
	 * decompiled source code. Required for post-formatting the source code
	 * according to the printed line numbers to make the source code suitable for
	 * debugging.
	 * 
	 * @return line number output type or <code>null</code> if reformatting is not
	 *         needed or supported
	 */
	LineNumberOutputType getLineNumberOutputType();

	String removeComment(String source);

	boolean supportDebugLevel(int level);

	boolean supportLevel(int level);

	String getDecompilerName();

	String getDecompilerVersion();

}