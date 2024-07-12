/*******************************************************************************
 * Copyright (c) 2017 Chen Chao and other ECD project contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sf.feeling.decompiler.util;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.ui.navigator.IExtensionStateConstants.Values;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonNavigatorManager;
import org.eclipse.ui.navigator.IExtensionStateModel;
import org.eclipse.ui.navigator.INavigatorContentService;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.sf.feeling.decompiler.JavaDecompilerConstants;
import org.sf.feeling.decompiler.JavaDecompilerPlugin;
import org.sf.feeling.decompiler.editor.JavaDecompilerClassFileEditor;

public class UIUtil {

	public static void openBrowser(String url) {
		try {
			final URL urlToOpen = new URL(url);
			IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
			if (browser != null) {
				browser.openURL(urlToOpen);
			}
		} catch (Exception e) {
			Logger.debug(e);
		}
	}

	public static JavaDecompilerClassFileEditor getActiveEditor() {
		final JavaDecompilerClassFileEditor[] editors = new JavaDecompilerClassFileEditor[1];
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				IWorkbenchPart editor = getActiveEditor(true);
				if (editor instanceof JavaDecompilerClassFileEditor) {
					editors[0] = (JavaDecompilerClassFileEditor) editor;
				} else {
					IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

					if (window != null) {
						IWorkbenchPage pg = window.getActivePage();

						if (pg != null) {
							IEditorPart editorPart = pg.getActiveEditor();
							if (editorPart instanceof JavaDecompilerClassFileEditor) {
								editors[0] = (JavaDecompilerClassFileEditor) editorPart;
							}
						}
					}
				}
			}
		});
		return editors[0];
	}

	public static JavaDecompilerClassFileEditor getActiveDecompilerEditor() {
		final AtomicReference<JavaDecompilerClassFileEditor> editors = new AtomicReference<>();
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				IWorkbenchPart editor = getActiveEditor(true);
				if (editor instanceof JavaDecompilerClassFileEditor) {
					editors.set((JavaDecompilerClassFileEditor) editor);
				}
			}
		});
		return editors.get();
	}

	public static List getActiveSelection() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		final List classes = getSelectedElements(window.getSelectionService(), IClassFile.class);
		if (classes != null && !classes.isEmpty()) {
			return classes;
		}
		return null;
	}

	public static String getActivePerspectiveId() {
		final AtomicReference<String> ids = new AtomicReference<>();
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				IWorkbench wb = PlatformUI.getWorkbench();
				if (wb == null) {
					return;
				}

				IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
				if (win == null) {
					return;
				}
				IWorkbenchPage page = win.getActivePage();
				if (page == null) {
					return;
				}

				IPerspectiveDescriptor perspective = page.getPerspective();
				if (perspective == null) {
					return;
				}
				ids.set(perspective.getId());
			}
		});
		return ids.get();
	}

	public static boolean isDebug() {
		IPreferenceStore prefs = JavaDecompilerPlugin.getDefault().getPreferenceStore();
		boolean showLineNumber = prefs.getBoolean(JavaDecompilerConstants.PREF_DISPLAY_LINE_NUMBERS);
		boolean align = prefs.getBoolean(JavaDecompilerConstants.ALIGN);
		boolean debug = (showLineNumber && align) || UIUtil.isDebugPerspective()
				|| JavaDecompilerPlugin.getDefault().isDebugMode();
		return debug;
	}

	public static boolean isDebugPerspective() {
		return "org.eclipse.debug.ui.DebugPerspective" //$NON-NLS-1$
				.equals(getActivePerspectiveId());
	}

	public static List getSelectedElements(ISelectionService selService, Class eleClass) {

		Iterator selections = getSelections(selService);
		List elements = new ArrayList();

		while ((selections != null) && selections.hasNext()) {
			Object select = selections.next();

			if (eleClass.isInstance(select)) {
				elements.add(select);
			}
		}

		return elements;
	}

	public static Iterator getSelections(ISelectionService selService) {
		ISelection selection = selService.getSelection();

		if (selection != null) {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection) selection;
				return structuredSelection.iterator();
			}
		}

		return null;
	}

	public static IWorkbenchPart getActiveEditor(boolean activePageOnly) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		if (window != null) {
			if (activePageOnly) {
				IWorkbenchPage pg = window.getActivePage();

				if (pg != null) {
					IWorkbenchPart activePart = pg.getActivePart();
					if (activePart instanceof ContentOutline) {
						ContentOutline outline = (ContentOutline) activePart;
						IWorkbenchPart part = (IWorkbenchPart) ReflectionUtils.invokeMethod(outline,
								"getCurrentContributingPart"); //$NON-NLS-1$
						if (part == null) {
							return (IWorkbenchPart) ReflectionUtils.getFieldValue(outline, "hiddenPart"); //$NON-NLS-1$
						}
					} else
						return activePart;
				}
			} else {
				for (IWorkbenchPage pg : window.getPages()) {
					if (pg != null) {
						IWorkbenchPart part = pg.getActivePart();
						if (part != null) {
							return part;
						}
					}
				}
			}
		}

		return null;
	}

	public static boolean isOpenClassEditor() {
		IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();

		if (windows != null) {
			for (IWorkbenchWindow window : windows) {
				for (IWorkbenchPage pg : window.getPages()) {
					if (pg == null) {
						continue;
					}
					// Deprecated since ?? use getEditorReferences() instead
					IEditorPart[] editorParts = pg.getEditors();
					if (editorParts == null) {
						continue;
					}
					for (IEditorPart part : editorParts) {
						if (part instanceof JavaDecompilerClassFileEditor) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public static List getExportSelections() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		final List selectedJars = getSelectedElements(window.getSelectionService(), IPackageFragmentRoot.class);
		if (selectedJars.size() == 1) {
			return selectedJars;
		}

		if (selectedJars.size() > 1) {
			return null;
		}

		final List selectedPackages = getSelectedElements(window.getSelectionService(), IPackageFragment.class);
		final List selectedClasses = getSelectedElements(window.getSelectionService(), IClassFile.class);
		selectedClasses.addAll(selectedPackages);
		if (!selectedClasses.isEmpty()) {
			return selectedClasses;
		}

		return null;
	}

	public static boolean isPackageFlat() {
		boolean isFlat = false;
		try {
			IWorkbenchPart view = getActiveEditor(true);
			if (view != null) {
				String id = view.getSite().getId();
				if (id.equals("org.eclipse.ui.navigator.ProjectExplorer")) //$NON-NLS-1$
				{
					CommonNavigator explorer = (CommonNavigator) view;
					Field field = CommonNavigator.class.getDeclaredField("commonManager"); //$NON-NLS-1$
					if (field != null) {
						field.setAccessible(true);
						CommonNavigatorManager manager = (CommonNavigatorManager) field.get(explorer);

						field = CommonNavigatorManager.class.getDeclaredField("contentService"); //$NON-NLS-1$
						if (field != null) {
							field.setAccessible(true);
							INavigatorContentService service = (INavigatorContentService) field.get(manager);
							IExtensionStateModel model = service.findStateModel("org.eclipse.jdt.java.ui.javaContent"); //$NON-NLS-1$
							isFlat = model.getBooleanProperty(Values.IS_LAYOUT_FLAT);
						}
					}
				} else if (id.equals("org.eclipse.jdt.ui.PackageExplorer")) //$NON-NLS-1$
				{
					PackageExplorerPart explorer = (PackageExplorerPart) view;
					isFlat = explorer.isFlatLayout();
				} else {
					JavaDecompilerPlugin.logWarn(null, "isPackageFlat: unsupported view: " + id);
				}
			}
		} catch (Exception e) {
		}
		return isFlat;
	}

	public static boolean isWin32() {
		return Platform.OS_WIN32.equalsIgnoreCase(Platform.getOS());
	}

	public static boolean isMacOS() {
		return Platform.OS_MACOSX.equalsIgnoreCase(Platform.getOS());
	}

	public static String getPathLocation(IPath path) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IResource resource = root.findMember(path);
		if (resource == null) {
			JavaDecompilerPlugin.logWarn(null, "Failed to find member of path " + path);
		} else {
			IPath location = resource.getLocation();
			if (location == null) {
				JavaDecompilerPlugin.logWarn(null, "Failed to get location of path " + resource);
			} else {
				return location.toOSString();
			}
		}
		return null;
	}

	public static boolean requestFromJavadocHover() {
		StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
		for (int i = 0; i < stacks.length && i < 12; i++) {
			if (stacks[i].getClassName().indexOf("BinaryType") != -1 //$NON-NLS-1$
					&& stacks[i].getMethodName().equals("getJavadocRange")) //$NON-NLS-1$
				return false;

			if (stacks[i].getClassName().indexOf("JavadocHover") != -1 //$NON-NLS-1$
					&& stacks[i].getMethodName().equals("getHoverInfo")) //$NON-NLS-1$
				return true;
		}
		return false;
	}

	public static boolean requestFromDisassemblerSelection() {
		StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
		for (int i = 0; i < stacks.length && i < 10; i++) {
			if (stacks[i].getMethodName().equals("doHandleCursorPositionChanged")) //$NON-NLS-1$
				return true;
		}
		return false;
	}

	public static boolean requestCreateBuffer() {
		StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
		for (int i = 0; i < stacks.length && i < 12; i++) {
			if (stacks[i].getClassName().indexOf("BinaryType") != -1 //$NON-NLS-1$
					&& stacks[i].getMethodName().equals("getJavadocRange")) //$NON-NLS-1$
				return false;

			if (stacks[i].getClassName().indexOf("JavadocHover") != -1 //$NON-NLS-1$
					&& stacks[i].getMethodName().equals("getHoverInfo2")) //$NON-NLS-1$
				return true;

			if (stacks[i].getClassName().indexOf("JavaSourceHover") != -1 //$NON-NLS-1$
					&& stacks[i].getMethodName().equals("getHoverInfo")) //$NON-NLS-1$
				return true;

			if (stacks[i].getClassName().indexOf("FindOccurrencesInFileAction") != -1 //$NON-NLS-1$
					&& stacks[i].getMethodName().equals("getMember")) //$NON-NLS-1$
				return true;

			// if ( stacks[i].getClassName( ).indexOf( "HyperlinkManager" ) !=
			// -1 //$NON-NLS-1$
			// && stacks[i].getMethodName( ).equals( "findHyperlinks" ) )
			// //$NON-NLS-1$
			// return true;

			// if ( stacks[i].getClassName( )
			// .indexOf( "DefaultJavaFoldingStructureProvider" ) != -1
			// //$NON-NLS-1$
			// && stacks[i].getMethodName( )
			// .equals( "computeProjectionRanges" ) ) //$NON-NLS-1$
			// return true;
		}
		return false;
	}

	public static boolean requestFromShowMatch() {
		StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
		for (int i = 0; i < stacks.length; i++) {
			if (stacks[i].getClassName().indexOf("JavaSearchResultPage") != -1 //$NON-NLS-1$
					&& stacks[i].getMethodName().equals("showMatch")) //$NON-NLS-1$
				return true;
		}
		return false;
	}

	public static boolean isDark(Control textWidget) {
		if (textWidget != null && !textWidget.isDisposed()) {
			Color color = textWidget.getBackground();
			int red = color.getRed();
			int blue = color.getBlue();
			int green = color.getGreen();
			return red < 127 || blue < 127 || green < 127;
		}
		return false;
	}

	public static boolean requestFromLinkToSelection() {
		StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
		for (int i = 0; i < stacks.length && i < 10; i++) {
			if (stacks[i].getMethodName().equals("linkToEditor") //$NON-NLS-1$
					|| stacks[i].getMethodName().equals("showSource")) //$NON-NLS-1$
				return true;
		}
		return false;
	}

	public static boolean requestFromCopyOperation() {
		StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
		for (int i = 0; i < stacks.length && i < 10; i++) {
			if (stacks[i].getMethodName().equals("doCutCopyWithImportsOperation")) //$NON-NLS-1$
				return true;
		}
		return false;
	}
}
