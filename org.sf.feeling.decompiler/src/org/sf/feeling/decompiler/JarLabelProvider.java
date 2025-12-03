package org.sf.feeling.decompiler;

import org.eclipse.jface.viewers.LabelProvider;


public class JarLabelProvider extends LabelProvider
{
	@Override
	public String getText(Object element) {
		if (null == element)
			return "";
		if (element instanceof JarContentProvider.JarNode) {
			String n = ((JarContentProvider.JarNode) element)
					.getEntry( ).getName( ).replaceAll( "/$", "" );
			return n.substring( n.lastIndexOf( '/' ), n.length( ) );
		}
		return element.toString();
	}

}
