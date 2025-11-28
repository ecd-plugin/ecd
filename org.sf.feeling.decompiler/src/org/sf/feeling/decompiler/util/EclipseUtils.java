package org.sf.feeling.decompiler.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;

public class EclipseUtils {

	private static String maxDecompileLevel = null;

	/**
	 * @return the maximum Java version supported by the current Eclipse JDT version
	 */
	public static String getMaxDecompileLevel() {
		if (maxDecompileLevel != null) {
			return maxDecompileLevel;
		}

		// replacement for newer Eclipse versions
		// return JavaCore.latestSupportedJavaVersion();
		Object obj = ReflectionUtils.invokeMethod(JavaCore.class, "latestSupportedJavaVersion");
		if (obj != null) {
			maxDecompileLevel = (String) obj;
			return maxDecompileLevel;
		}

		// filter oot all versions that are not a simple integers e.g. "9" "10" ...
		Pattern p = Pattern.compile("^\\d+$");
		List<String> allVersions = new LinkedList<>(JavaCore.getAllVersions());
		Iterator<String> it = allVersions.iterator();
		while (it.hasNext()) {
			String v = it.next();
			if (!p.matcher(v).matches()) {
				it.remove();
			}
		}
		if (allVersions.isEmpty()) {
			maxDecompileLevel = "1.8"; //$NON-NLS-1$
			return maxDecompileLevel;
		}

		List<Integer> allVersionsInt = new ArrayList<>();
		for (String v : allVersions) {
			allVersionsInt.add(Integer.parseInt(v));
		}
		maxDecompileLevel = Integer.toString(Collections.max(allVersionsInt));
		return maxDecompileLevel;
	}

	private static int maxJsLevel = -1;

	public static int getMaxJSLLevel() {
		if (maxJsLevel != -1) {
			return maxJsLevel;
		}

		// replacement for newer Eclipse versions
		// return AST.getJLSLatest();
		Object obj = ReflectionUtils.invokeMethod(AST.class, "getJLSLatest");
		if (obj != null) {
			maxJsLevel = (Integer) obj;
			return maxJsLevel;
		}

		Pattern p = Pattern.compile("^JLS\\d+$");
		int maxFieldValue = 8; // Java 8 is supported by all Eclipse versions ECD targets
		for (Field f : AST.class.getFields()) {
			if (f.getType() != int.class || !p.matcher(f.getName()).matches()) {
				continue;
			}
			try {
				int value = f.getInt(AST.class);
				if (value > maxFieldValue) {
					maxFieldValue = value;
				}
			} catch (Exception e) {

			}
		}
		maxJsLevel = maxFieldValue;
		return maxJsLevel;
	}

}
