package org.sf.feeling.decompiler.editor;

import java.util.regex.Pattern;

import org.sf.feeling.decompiler.postprocessing.LineReformatter;

/**
 * Several decompiler don't support generating decompiled code that is formatted
 * according to the line number information contained in the class file.
 * 
 * These decompilers only allow to include the source code line numbers as
 * comment in the generated source code. ECD allows to reformat decompiled
 * source code according to those line number comments.
 * 
 * There are multiple styles decompiler use for including the source code line
 * numbers into the decompiled source code. This enum class lists all supported
 * line number styles so that the source code reformatter in
 * {@link LineReformatter} can use this information to find and extract the
 * line numbers.
 */
public enum LineNumberOutputType {

	/**
	 * One or more source code line numbers are included as single line comment at
	 * the end of a source code line.
	 * 
	 * Capturing group returns the whole comment including the line numbers (for
	 * removal).
	 * 
	 * Capturing group 2 returns the source code line number(s) only
	 * 
	 * for example:
	 * 
	 * <pre>
	 * System.out.println(); // 13
	 * if (a.condition() && b.condition())) System.out.println(); // 13 14
	 * </pre>
	 */
	SINGLE_LINE_COMMENT_END_OF_LINE(//
			Pattern.compile("(//\\s+(\\d+(\\s+\\d+)*\\s*))$")// multiple line numbers supported
	),

	BLOCK_COMMENT_BEGIN_OF_LINE(//
			Pattern.compile("\\s*(/\\*\\s*(\\d*)\\s*\\*/)")// only one line number supported
	);

	private final Pattern pattern;

	/**
	 * @param pattern for extracting one or more line numbers. if it matches it has
	 *                to return at least one line number in matcher group(1).
	 */
	LineNumberOutputType(Pattern pattern) {
		this.pattern = pattern;
	}

	public Pattern getPattern() {
		return pattern;
	}

}
