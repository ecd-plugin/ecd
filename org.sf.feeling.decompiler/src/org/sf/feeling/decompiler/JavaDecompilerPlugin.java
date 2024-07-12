/*******************************************************************************
 * Copyright (c) 2017 Chen Chao and other ECD project contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sf.feeling.decompiler;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.sf.feeling.decompiler.editor.IDecompilerDescriptor;
import org.sf.feeling.decompiler.editor.JavaDecompilerBufferManager;
import org.sf.feeling.decompiler.extension.DecompilerAdapterManager;
import org.sf.feeling.decompiler.source.attach.IAttachSourceHandler;
import org.sf.feeling.decompiler.util.DefaultDecompilerDescriptorComparator;
import org.sf.feeling.decompiler.util.FileUtil;
import org.sf.feeling.decompiler.util.Logger;
import org.sf.feeling.decompiler.util.SortMemberUtil;

public class JavaDecompilerPlugin extends AbstractUIPlugin implements IPropertyChangeListener {

	private static final String ECD_DECOMPILER_CLASS_NAME_PREFIX = "org.sf.feeling.decompiler";

	private static final List<String> ALLOWED_PLUGINS = Arrays.asList("cfr", "fernflower", "jd", "procyon",
			"vineflower");

	private static JavaDecompilerPlugin plugin;

	private IPreferenceStore preferenceStore;
	private TreeMap<String, IDecompilerDescriptor> decompilerDescriptorMap = new TreeMap<>();

	private boolean isDebugMode = false;

	private int sourceMode = 0;

	public Map<String, IDecompilerDescriptor> getDecompilerDescriptorMap() {
		return decompilerDescriptorMap;
	}

	public String[] getDecompilerDescriptorTypes() {
		return decompilerDescriptorMap.keySet().toArray(new String[0]);
	}

	public IDecompilerDescriptor getDecompilerDescriptor(String decompilerType) {
		return decompilerDescriptorMap.get(decompilerType);
	}

	public static JavaDecompilerPlugin getDefault() {
		return plugin;
	}

	public static void logError(Throwable t, String message) {
		JavaDecompilerPlugin.getDefault().getLog()
				.log(new Status(IStatus.ERROR, JavaDecompilerConstants.PLUGIN_ID, 0, message, t));
	}

	public static void logWarn(Throwable t, String message) {
		JavaDecompilerPlugin.getDefault().getLog()
				.log(new Status(IStatus.WARNING, JavaDecompilerConstants.PLUGIN_ID, 0, message, t));
	}

	public static void logInfo(String message) {
		JavaDecompilerPlugin.getDefault().getLog()
				.log(new Status(IStatus.INFO, JavaDecompilerConstants.PLUGIN_ID, 0, message, null));
	}

	/**
	 * 
	 * @param severity severity constant see {@link IStatus#INFO}
	 *                 {@link IStatus#WARNING} {@link IStatus#ERROR}
	 * @param t
	 * @param message
	 */
	public static void log(int severity, Throwable t, String message) {
		JavaDecompilerPlugin.getDefault().getLog()
				.log(new Status(severity, JavaDecompilerConstants.PLUGIN_ID, 0, message, t));
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		URL base = JavaDecompilerPlugin.getDefault().getBundle().getEntry("/"); //$NON-NLS-1$
		URL url = null;
		try {
			url = new URL(base, path); // $NON-NLS-1$
		} catch (MalformedURLException e) {
			Logger.debug(e);
		}
		ImageDescriptor actionIcon = null;
		if (url != null) {
			actionIcon = ImageDescriptor.createFromURL(url);
		}
		return actionIcon;
	}

	public JavaDecompilerPlugin() {
		plugin = this;
	}

	@Override
	protected void initializeDefaultPreferences(IPreferenceStore store) {
		store.setDefault(JavaDecompilerConstants.TEMP_DIR, System.getProperty("java.io.tmpdir") //$NON-NLS-1$
				+ File.separator + ".org.sf.feeling.decompiler" //$NON-NLS-1$
				+ System.currentTimeMillis());
		store.setDefault(JavaDecompilerConstants.REUSE_BUFFER, true);
		store.setDefault(JavaDecompilerConstants.IGNORE_EXISTING, false);
		store.setDefault(JavaDecompilerConstants.USE_ECLIPSE_FORMATTER, true);
		store.setDefault(JavaDecompilerConstants.USE_ECLIPSE_SORTER, false);
		store.setDefault(JavaDecompilerConstants.PREF_DISPLAY_METADATA, false);
		store.setDefault(JavaDecompilerConstants.DEFAULT_EDITOR, true);
		store.setDefault(JavaDecompilerConstants.ATTACH_SOURCE, false);
		store.setDefault(JavaDecompilerConstants.EXPORT_ENCODING, StandardCharsets.UTF_8.name());

		PreferenceConverter.setDefault(store, JavaDecompilerConstants.BYTECODE_MNEMONIC, new RGB(0, 0, 0));
		store.setDefault(JavaDecompilerConstants.BYTECODE_MNEMONIC_BOLD, true);
		store.setDefault(JavaDecompilerConstants.BYTECODE_MNEMONIC_ITALIC, false);
		store.setDefault(JavaDecompilerConstants.BYTECODE_MNEMONIC_STRIKETHROUGH, false);
		store.setDefault(JavaDecompilerConstants.BYTECODE_MNEMONIC_UNDERLINE, false);

		store.setDefault(JavaDecompilerConstants.CLASS_FILE_ATTR_SHOW_CONSTANT_POOL, false);
		store.setDefault(JavaDecompilerConstants.CLASS_FILE_ATTR_SHOW_LINE_NUMBER_TABLE, false);
		store.setDefault(JavaDecompilerConstants.CLASS_FILE_ATTR_SHOW_VARIABLE_TABLE, false);
		store.setDefault(JavaDecompilerConstants.CLASS_FILE_ATTR_SHOW_EXCEPTION_TABLE, false);
		store.setDefault(JavaDecompilerConstants.CLASS_FILE_ATTR_SHOW_MAXS, false);
		store.setDefault(JavaDecompilerConstants.BRANCH_TARGET_ADDRESS_RENDERING,
				JavaDecompilerConstants.BRANCH_TARGET_ADDRESS_RELATIVE);
		store.setDefault(JavaDecompilerConstants.CLASS_FILE_ATTR_RENDER_TRYCATCH_BLOCKS, true);
		store.setDefault(JavaDecompilerConstants.CLASS_FILE_ATTR_SHOW_SOURCE_LINE_NUMBERS, true);
		store.setDefault(JavaDecompilerConstants.CLASS_FILE_ATTR_SHOW_MAXS, false);
	}

	private void initializeDecompilerDescriptorMap(IPreferenceStore store) {
		List<IDecompilerDescriptor> decompilerAdapters = DecompilerAdapterManager.getAdapterList(this,
				IDecompilerDescriptor.class);

		if (decompilerAdapters != null) {

			for (IDecompilerDescriptor descriptor : decompilerAdapters) {
				// Filter out plugins/features of outdated ECD plugins (e.g. plugins that
				// belong to an older ECD version)
				String className = descriptor.getClass().getName();
				if (className.startsWith(ECD_DECOMPILER_CLASS_NAME_PREFIX)) {
					String name = className.substring(ECD_DECOMPILER_CLASS_NAME_PREFIX.length() + 1);
					int idx = name.indexOf('.');
					if (idx > 0) {
						name = name.substring(0, idx);
						boolean allowed = (ALLOWED_PLUGINS.indexOf(name) >= 0);
						if (!allowed) {
							Logger.error("Disabling unknown decompiler plugin: " + className);
							continue;
						}
					}
				}

				if (descriptor.isEnabled()) {
					decompilerDescriptorMap.put(descriptor.getDecompilerType(), descriptor);
				}
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(JavaDecompilerConstants.IGNORE_EXISTING)) {
			JavaDecompilerBufferManager.closeDecompilerBuffers(false);
		}
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		initializeDecompilerDescriptorMap(getPreferenceStore());
		initializeDefaultDecompilerType();
		getPreferenceStore().addPropertyChangeListener(this);
		SortMemberUtil.deleteDecompilerProject();
		Display.getDefault().asyncExec(new SetupRunnable());
	}

	@Override
	public IPreferenceStore getPreferenceStore() {
		if (preferenceStore == null) {
			preferenceStore = super.getPreferenceStore();

			String decompilerType = preferenceStore.getString(JavaDecompilerConstants.DECOMPILER_TYPE);
			IDecompilerDescriptor descriptor = getDecompilerDescriptor(decompilerType);
			if (descriptor == null) {
				String defaultDecompiler = getDefaultDecompilerType();
				if (defaultDecompiler != null) {
					preferenceStore.setDefault(JavaDecompilerConstants.DECOMPILER_TYPE, defaultDecompiler);
				}
			}
		}
		return preferenceStore;
	}

	public void initializeDefaultDecompilerType() {
		String decompilerType = preferenceStore.getString(JavaDecompilerConstants.DECOMPILER_TYPE);
		IDecompilerDescriptor descriptor = getDecompilerDescriptor(decompilerType);
		if (descriptor != null) {
			return;
		}
		Collection<IDecompilerDescriptor> descriptorColl = JavaDecompilerPlugin.getDefault()
				.getDecompilerDescriptorMap().values();
		if (!descriptorColl.isEmpty()) {
			String defaultDecompiler = getDefaultDecompilerType();
			if (defaultDecompiler != null) {
				preferenceStore.setDefault(JavaDecompilerConstants.DECOMPILER_TYPE, defaultDecompiler);
			}
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		FileUtil.deltree(new File(getPreferenceStore().getString(JavaDecompilerConstants.TEMP_DIR)));

		super.stop(context);

		getPreferenceStore().removePropertyChangeListener(this);

		plugin = null;
	}

	public Boolean isDisplayLineNumber() {
		return Boolean.valueOf(getPreferenceStore().getBoolean(JavaDecompilerConstants.PREF_DISPLAY_LINE_NUMBERS));
	}

	public Boolean isDebug() {
		return Boolean.valueOf(getPreferenceStore().getBoolean(JavaDecompilerConstants.ALIGN));
	}

	public void displayLineNumber(Boolean display) {
		getPreferenceStore().setValue(JavaDecompilerConstants.PREF_DISPLAY_LINE_NUMBERS, display.booleanValue());
	}

	public void setExportEncoding(String encoding) {
		getPreferenceStore().setValue(JavaDecompilerConstants.EXPORT_ENCODING, encoding);
	}

	public String getExportEncoding() {
		return getPreferenceStore().getString(JavaDecompilerConstants.EXPORT_ENCODING);
	}

	public boolean enableAttachSourceSetting() {
		Object attachSourceAdapter = DecompilerAdapterManager.getAdapter(this, IAttachSourceHandler.class);
		if (attachSourceAdapter instanceof IAttachSourceHandler) {
			return true;
		}

		return false;
	}

	private final Set<String> libraries = new ConcurrentSkipListSet<>();

	public void attachSource(IPackageFragmentRoot library, boolean force) {
		Object attachSourceAdapter = DecompilerAdapterManager.getAdapter(this, IAttachSourceHandler.class);
		if (attachSourceAdapter instanceof IAttachSourceHandler) {
			if (!libraries.contains(library.getPath().toOSString()) || force) {
				libraries.add(library.getPath().toOSString());
				((IAttachSourceHandler) attachSourceAdapter).execute(library, force);
			}
		}
	}

	public void syncLibrarySource(IPackageFragmentRoot library) {
		try {
			if (library.getPath() != null && library.getSourceAttachmentPath() != null
					&& !libraries.contains(library.getPath().toOSString())) {
				final IPreferenceStore prefs = JavaDecompilerPlugin.getDefault().getPreferenceStore();
				if (prefs.getBoolean(JavaDecompilerConstants.DEFAULT_EDITOR)) {
					final Object attachSourceAdapter = DecompilerAdapterManager
							.getAdapter(JavaDecompilerPlugin.getDefault(), IAttachSourceHandler.class);
					if (attachSourceAdapter instanceof IAttachSourceHandler) {
						libraries.add(library.getPath().toOSString());
						if (!((IAttachSourceHandler) attachSourceAdapter).syncAttachSource(library)) {
							libraries.remove(library.getPath().toOSString());
						}
					}
				}
			}
		} catch (JavaModelException e) {
			Logger.debug(e);
		}
	}

	public boolean isAutoAttachSource() {
		if (!enableAttachSourceSetting()) {
			return false;
		}

		return getPreferenceStore().getBoolean(JavaDecompilerConstants.ATTACH_SOURCE);
	}

	public String getDefaultDecompilerType() {
		Collection<IDecompilerDescriptor> descriptorColl = JavaDecompilerPlugin.getDefault()
				.getDecompilerDescriptorMap().values();
		if (!descriptorColl.isEmpty()) {
			IDecompilerDescriptor defaultDecompilerDescr = descriptorColl.stream()
					.sorted(new DefaultDecompilerDescriptorComparator()).findFirst().get();
			return defaultDecompilerDescr.getDecompilerType();
		}
		return null;
	}

	public boolean isDebugMode() {
		return isDebugMode;
	}

	public void setDebugMode(boolean isDebugMode) {
		this.isDebugMode = isDebugMode;
	}

	public int getSourceMode() {
		return sourceMode;
	}

	public void setSourceMode(int sourceMode) {
		this.sourceMode = sourceMode;
	}

	public String getDefaultExportEncoding() {
		return getPreferenceStore().getDefaultString(JavaDecompilerConstants.EXPORT_ENCODING);
	}

}