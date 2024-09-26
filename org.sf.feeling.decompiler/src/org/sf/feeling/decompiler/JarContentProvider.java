package org.sf.feeling.decompiler;

import java.io.File;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;



public class JarContentProvider extends BaseWorkbenchContentProvider
{
	private static final String[] types = {"jar", "war"};
	public class JarNode
	{
		private final URL url;
		private final URL parent;
		private final JarEntry entry;

		public JarNode(URI jarfile, JarEntry entry) throws MalformedURLException {
			this.url = new URL(String.format("jar:%s!/%s", jarfile, entry));
			this.parent = new URL(String.format("jar:%s!/%s", jarfile,
					entry.getName().substring(0, entry.getName()
							.replaceAll("/$","").lastIndexOf("/")+1)));
			this.entry = entry;
		}
		public JarNode(URL url, JarEntry entry) throws MalformedURLException {
			this.url = url;
			this.parent = new URL(url.toString( ).substring(0,
					url.toString( ).replaceAll("/$","").lastIndexOf("/")+1));
			this.entry = entry;
		}

		public URL toURL() {
			return url;
		}

		public URL getParent() {
			return parent;
		}

		public JarEntry getEntry() {
			return entry;
		}
	}

	private final Map<URL,List<JarNode>> nodes = new HashMap<>();
	private final Map<String,IFile> files = new HashMap<>( );

	private boolean isJavaArchive(IFile file) {
//		return Arrays.binarySearch( types, file.getFileExtension() ) >= 0;
		return true;
	}

	@Override
	public Object[] getChildren( Object parentElement )
	{
		if ( parentElement instanceof IFile && isJavaArchive( (IFile) parentElement ) ) {
			File f = ((IFile) parentElement).getLocation( ).toFile( );
			try ( JarFile jf = new JarFile( f ) ) {
				files.put( f.toURI( )+"!/", (IFile) parentElement );
				for ( JarEntry je:Collections.list( jf.entries( ) ) ) {
					JarNode node = new JarNode(f.toURI( ), je);
					if (!nodes.containsKey( node.getParent( ) ))
						nodes.put( node.getParent( ), new ArrayList<JarNode>( ) );
					nodes.get( node.getParent( ) ).add( node );
				}
				return nodes.get( new URL( String.format( "jar:%s!/", f.toURI( ) ) ) ).toArray( );
			} catch (Throwable t) {};
		}
		if (parentElement instanceof JarNode && ((JarNode) parentElement).getEntry( ).isDirectory( ))
			return nodes.get( ((JarNode) parentElement).toURL( ) ).toArray( );
		return new Object[0];
	}

	@Override
	public Object getParent( Object element )
	{
		if (element instanceof JarNode) {
			JarNode node = (JarNode) element;
			try
			{
				return new JarNode(node.getParent( ),
						((JarURLConnection) node.getParent( ).openConnection( )).getJarEntry( ));
			} catch ( Throwable t ) {
				try
				{
					return files.get( node.getParent( ).toURI( ).getSchemeSpecificPart( ) );
				} catch ( URISyntaxException e ) {}
			}
		}
		return super.getParent( element );
	}

	@Override
	public boolean hasChildren( Object element )
	{
		if (element instanceof JarNode)
			return nodes.containsKey( ((JarNode) element).toURL( ) );
		return super.hasChildren( element );
	}

}
