/*******************************************************************************
 * Copyright (c) 2017 Chen Chao and other ECD project contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sf.feeling.decompiler.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.sf.feeling.decompiler.JavaDecompilerPlugin;
import org.sf.feeling.decompiler.editor.BaseDecompiler;
import org.sf.feeling.decompiler.editor.IDecompiler;
import org.sf.feeling.decompiler.editor.IDecompilerDescriptor;
import org.sf.feeling.decompiler.editor.NoStackTraceException;

public class ClassUtil {

	public static IDecompiler checkAvailableDecompiler(IDecompiler decompiler, File file) {
		try (FileInputStream fis = new FileInputStream(file)) {
			return checkAvailableDecompiler(decompiler, fis);
		} catch (IOException e) {
			Logger.error(e);
		}
		return decompiler;
	}

	public static IDecompiler checkAvailableDecompiler(IDecompiler decompiler, InputStream is) {
		int classLevel = getLevel(is);

		boolean debug = isDebug();
		IDecompiler defaultDecompiler = getDebugDecompiler(classLevel, debug);
		defaultDecompiler.clearExceptions();

		if (decompiler.supportLevel(classLevel)) {
			if (debug) {
				if (!decompiler.supportDebugLevel(classLevel)) {
					String recommendation = "";
					if (JavaDecompilerPlugin.getDefault().isDebug()) {
						recommendation += "Disable the 'Align code for debugging' option. ";
					}
					if (UIUtil.isDebugPerspective()) {
						recommendation += "Switch to the Java perspective. ";
					}
					if (JavaDecompilerPlugin.getDefault().isDebugMode()) {
						recommendation += "Disable the debug Mode. ";
					}
					String msg = "Could not use " + decompiler.getClass().getSimpleName()
							+ " for decompilation since the debug view is not supported. " + recommendation
							+ "Falling back to " + defaultDecompiler.getClass().getSimpleName() + ".";
					JavaDecompilerPlugin.logInfo(msg);
					((BaseDecompiler) decompiler).addException(new NoStackTraceException(msg));
					return defaultDecompiler;
				}
			}
		} else {
			String msg = "Could not use " + decompiler.getClass().getSimpleName()
					+ " for decompilation since the classLevel " + classLevel + " is not supported. "
					+ "Falling back to " + defaultDecompiler.getClass().getSimpleName() + ".";
			JavaDecompilerPlugin.logInfo(msg);
			((BaseDecompiler) decompiler).addException(new NoStackTraceException(msg));
			return defaultDecompiler;
		}
		return decompiler;
	}

	public static boolean isDebug() {
		return JavaDecompilerPlugin.getDefault().isDebug() || UIUtil.isDebugPerspective()
				|| JavaDecompilerPlugin.getDefault().isDebugMode();
	}

	public static int getLevel(InputStream is) {
		try (DataInputStream data = new DataInputStream(is)) {
			if (0xCAFEBABE != data.readInt()) {
				return -1;
			}
			data.readUnsignedShort();
			int major = data.readUnsignedShort();
			return major - 44;
		} catch (IOException e) {
			Logger.error("Failed to get Class file version", e);
		}
		return -1;
	}

	public static boolean isClassFile(byte[] classData) {
		try (DataInputStream data = new DataInputStream(new ByteArrayInputStream(classData))) {
			if (0xCAFEBABE != data.readInt()) {
				return false;
			}
			data.readUnsignedShort();
			data.readUnsignedShort();
			return true;
		} catch (IOException e) {
			Logger.error("Class file test failed", e);
		}
		return false;
	}

	/**
	 * Uses FernFlower library to read the class and extract it's qualified name
	 */
	public static String getClassQualifiedName(byte[] classData) throws IOException {
		ClassReader classReader = new ClassReader(classData);
		return classReader.getClassName();
	}

	public static IDecompiler getDebugDecompiler(int level, boolean debug) {
		Collection<IDecompilerDescriptor> descriptors = JavaDecompilerPlugin.getDefault().getDecompilerDescriptorMap()
				.values();
		if (descriptors != null) {
			Stream<IDecompilerDescriptor> stream = descriptors.stream();
			if (debug) {
				stream = stream.filter(d -> d.getDecompiler().supportDebugLevel(level));
			}
			Optional<IDecompilerDescriptor> defaultDecompiler = stream
					.sorted(new DefaultDecompilerDescriptorComparator()).findFirst();
			if (!defaultDecompiler.isPresent()) {
				return null;
			}
			IDecompiler decompiler = defaultDecompiler.get().getDecompiler();
			decompiler.clearExceptions();
			return decompiler;
		}
		return null;
	}
}
