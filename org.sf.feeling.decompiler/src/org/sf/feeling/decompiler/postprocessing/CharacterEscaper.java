package org.sf.feeling.decompiler.postprocessing;

public class CharacterEscaper {

	public static String process(String code) {

		// Replace null character, see https://github.com/ecd-plugin/ecd/issues/122
		// Hopefully null characters only occur in string constants, otherwise this
		// replacement would create invalid Java code.
		code = code.replace("\u0000", "\\u0000");

		return code;
	}
}
