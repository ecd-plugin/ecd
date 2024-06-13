package org.sf.feeling.decompiler.editor;

import java.util.LinkedList;
import java.util.List;

public abstract class BaseDecompiler implements IDecompiler {

	protected final List<Exception> exceptions = new LinkedList<>();

	public void addException(Exception ex) {
		exceptions.add(ex);
	}

	@Override
	public List<Exception> getExceptions() {
		return exceptions;
	}

	@Override
	public void clearExceptions() {
		exceptions.clear();
	}
}
