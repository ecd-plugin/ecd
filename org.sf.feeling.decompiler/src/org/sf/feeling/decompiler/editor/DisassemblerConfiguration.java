/*******************************************************************************
 * Copyright (c) 2017 Chen Chao and other ECD project contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sf.feeling.decompiler.editor;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.AbstractJavaScanner;
import org.eclipse.jdt.internal.ui.text.JavaCommentScanner;
import org.eclipse.jdt.internal.ui.text.JavaPresentationReconciler;
import org.eclipse.jdt.internal.ui.text.SingleTokenJavaScanner;
import org.eclipse.jdt.internal.ui.text.java.JavaDoubleClickSelector;
import org.eclipse.jdt.internal.ui.text.java.hover.JavaEditorTextHoverDescriptor;
import org.eclipse.jdt.internal.ui.text.java.hover.JavaEditorTextHoverProxy;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.IJavaColorConstants;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sf.feeling.decompiler.util.ReflectionUtils;

import com.drgarbage.asm.render.intf.IFieldSection;
import com.drgarbage.asm.render.intf.IInstructionLine;
import com.drgarbage.asm.render.intf.IMethodSection;
import com.drgarbage.bytecode.ByteCodeConstants;
import com.drgarbage.bytecode.instructions.BranchInstruction;

public class DisassemblerConfiguration extends TextSourceViewerConfiguration {

	/**
	 * The Java source code scanner.
	 */
	private AbstractJavaScanner fCodeScanner;
	/**
	 * The color manager.
	 */
	private IColorManager fColorManager;
	/**
	 * The document partitioning.
	 */
	private String fDocumentPartitioning;
	/**
	 * The double click strategy.
	 * 
	 * @since 3.1
	 */
	private JavaDoubleClickSelector fJavaDoubleClickSelector;
	/**
	 * The Java multi-line comment scanner.
	 */
	private AbstractJavaScanner fMultilineCommentScanner;
	/**
	 * The Java single-line comment scanner.
	 */
	private AbstractJavaScanner fSinglelineCommentScanner;

	/**
	 * The Java string scanner.
	 */
	private AbstractJavaScanner fStringScanner;

	/**
	 * Editor reference
	 */
	private ITextEditor fTextEditor;

	public DisassemblerConfiguration(IColorManager colorManager, IPreferenceStore preferenceStore, ITextEditor editor,
			String partitioning) {
		super(preferenceStore);
		fColorManager = colorManager;
		fTextEditor = editor;
		fDocumentPartitioning = partitioning;
		initializeScanners();
	}

	public int[] getConfiguredTextHoverStateMasks(ISourceViewer sourceViewer, String contentType) {
		JavaEditorTextHoverDescriptor[] hoverDescs = JavaPlugin.getDefault().getJavaEditorTextHoverDescriptors();
		int stateMasks[] = new int[hoverDescs.length];
		int stateMasksLength = 0;
		for (int i = 0; i < hoverDescs.length; i++) {
			if (hoverDescs[i].isEnabled()) {
				int j = 0;
				int stateMask = hoverDescs[i].getStateMask();
				while (j < stateMasksLength) {
					if (stateMasks[j] == stateMask)
						break;
					j++;
				}
				if (j == stateMasksLength)
					stateMasks[stateMasksLength++] = stateMask;
			}
		}
		if (stateMasksLength == hoverDescs.length)
			return stateMasks;

		int[] shortenedStateMasks = new int[stateMasksLength];
		System.arraycopy(stateMasks, 0, shortenedStateMasks, 0, stateMasksLength);
		return shortenedStateMasks;
	}

	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
		JavaEditorTextHoverDescriptor[] hoverDescs = JavaPlugin.getDefault().getJavaEditorTextHoverDescriptors();
		int i = 0;
		while (i < hoverDescs.length) {
			if (hoverDescs[i].isEnabled() && hoverDescs[i].getStateMask() == stateMask)
				return new JavaEditorTextHoverProxy(hoverDescs[i], fTextEditor);
			i++;
		}

		return null;
	}

	/**
	 * Returns the Java source code scanner for this configuration.
	 *
	 * @return the Java source code scanner
	 */
	protected RuleBasedScanner getCodeScanner() {
		return fCodeScanner;
	}

	/**
	 * Returns the color manager for this configuration.
	 *
	 * @return the color manager
	 */
	protected IColorManager getColorManager() {
		return fColorManager;
	}

	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, IJavaPartitions.JAVA_MULTI_LINE_COMMENT,
				IJavaPartitions.JAVA_SINGLE_LINE_COMMENT, IJavaPartitions.JAVA_STRING, IJavaPartitions.JAVA_CHARACTER,
				IJavaPartitions.JAVA_DOC };
	}

	/*
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#
	 * getConfiguredDocumentPartitioning(org.eclipse.jface.text.source.
	 * ISourceViewer)
	 * 
	 * @since 3.0
	 */
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		if (fDocumentPartitioning != null) {
			return fDocumentPartitioning;
		}
		return super.getConfiguredDocumentPartitioning(sourceViewer);
	}

	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		/*
		 * FIX: bug#115 Nicolas F. Rouquette: Problems with the DrGarbage plugins with
		 * the DLTK 1.0.0M4 build (Galileo)
		 */
		// if (IJavaPartitions.JAVA_DOC.equals(contentType))
		// return new JavadocDoubleClickStrategy();
		if (IJavaPartitions.JAVA_MULTI_LINE_COMMENT.equals(contentType)
				|| IJavaPartitions.JAVA_SINGLE_LINE_COMMENT.equals(contentType))
			return new DefaultTextDoubleClickStrategy();
		// else if (IJavaPartitions.JAVA_STRING.equals(contentType) ||
		// IJavaPartitions.JAVA_CHARACTER.equals(contentType))
		// return new
		// JavaStringDoubleClickSelector(getConfiguredDocumentPartitioning(sourceViewer));
		if (fJavaDoubleClickSelector == null) {
			fJavaDoubleClickSelector = new JavaDoubleClickSelector();
			String sourceVersion = fPreferenceStore.getString(JavaCore.COMPILER_SOURCE);
			// JavaDoubleClickSelector.setSourceVersion(String) is only available on Eclipse
			// versions prior to Eclipse 2025-06
			ReflectionUtils.invokeMethod(JavaDoubleClickSelector.class, "setSourceVersion",
					new Class[] { String.class }, new Object[] { sourceVersion });
		}
		return fJavaDoubleClickSelector;
	}

	/*
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#
	 * getHyperlinkDetectors(org.eclipse.jface.text.source.ISourceViewer)
	 * 
	 * @since 3.1
	 */
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
		return new IHyperlinkDetector[] { new ClassFileHyperlinkDetector(), new URLHyperlinkDetector() };
	}

	/**
	 * Returns the Java multi-line comment scanner for this configuration.
	 *
	 * @return the Java multi-line comment scanner
	 * @since 2.0
	 */
	protected RuleBasedScanner getMultilineCommentScanner() {
		return fMultilineCommentScanner;
	}

	/*
	 * @see SourceViewerConfiguration#getPresentationReconciler(ISourceViewer)
	 */
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {

		PresentationReconciler reconciler = new JavaPresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr = new DefaultDamagerRepairer(getMultilineCommentScanner());
		reconciler.setDamager(dr, IJavaPartitions.JAVA_MULTI_LINE_COMMENT);
		reconciler.setRepairer(dr, IJavaPartitions.JAVA_MULTI_LINE_COMMENT);

		dr = new DefaultDamagerRepairer(getSinglelineCommentScanner());
		reconciler.setDamager(dr, IJavaPartitions.JAVA_SINGLE_LINE_COMMENT);
		reconciler.setRepairer(dr, IJavaPartitions.JAVA_SINGLE_LINE_COMMENT);

		dr = new DefaultDamagerRepairer(getStringScanner());
		reconciler.setDamager(dr, IJavaPartitions.JAVA_STRING);
		reconciler.setRepairer(dr, IJavaPartitions.JAVA_STRING);

		dr = new DefaultDamagerRepairer(getStringScanner());
		reconciler.setDamager(dr, IJavaPartitions.JAVA_CHARACTER);
		reconciler.setRepairer(dr, IJavaPartitions.JAVA_CHARACTER);

		dr = new DefaultDamagerRepairer(getStringScanner());
		reconciler.setDamager(dr, IJavaPartitions.JAVA_DOC);
		reconciler.setRepairer(dr, IJavaPartitions.JAVA_DOC);

		return reconciler;
	}

	/**
	 * Returns the Java single-line comment scanner for this configuration.
	 *
	 * @return the Java single-line comment scanner
	 * @since 2.0
	 */
	protected RuleBasedScanner getSinglelineCommentScanner() {
		return fSinglelineCommentScanner;
	}

	/**
	 * Returns the Java string scanner for this configuration.
	 *
	 * @return the Java string scanner
	 * @since 2.0
	 */
	protected RuleBasedScanner getStringScanner() {
		return fStringScanner;
	}

	/**
	 * Initializes the scanners.
	 *
	 */
	private void initializeScanners() {
		fCodeScanner = new RenderedBytecodeScanner(getColorManager(), fPreferenceStore);
		fMultilineCommentScanner = new JavaCommentScanner(getColorManager(), fPreferenceStore,
				IJavaColorConstants.JAVA_MULTI_LINE_COMMENT);
		fSinglelineCommentScanner = new JavaCommentScanner(getColorManager(), fPreferenceStore,
				IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT);
		fStringScanner = new SingleTokenJavaScanner(getColorManager(), fPreferenceStore,
				IJavaColorConstants.JAVA_STRING);
	}

	/**
	 * Adapts the behavior of the contained components to the change encoded in the
	 * given event.
	 */
	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		if (fCodeScanner.affectsBehavior(event))
			fCodeScanner.adaptToPreferenceChange(event);
		if (fMultilineCommentScanner.affectsBehavior(event))
			fMultilineCommentScanner.adaptToPreferenceChange(event);
		if (fSinglelineCommentScanner.affectsBehavior(event))
			fSinglelineCommentScanner.adaptToPreferenceChange(event);
		if (fStringScanner.affectsBehavior(event))
			fStringScanner.adaptToPreferenceChange(event);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.editors.text.TextSourceViewerConfiguration#getReconciler(
	 * org.eclipse.jface.text.source.ISourceViewer)
	 */
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		/* overwrite the implementation to deactivate spelling check */
		return null;
	}

	/**
	 * HyperLink Detector for BYtecode Visualizer Documnts.
	 */
	class ClassFileHyperlinkDetector implements IHyperlinkDetector {

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(
		 * org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion, boolean)
		 */
		public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
				boolean canShowMultipleHyperlinks) {

			IDocument document = fTextEditor.getDocumentProvider().getDocument(fTextEditor.getEditorInput());
			IRegion lineInfo = null;
			String lineText = null;
			int lineNumber = -1;
			try {
				lineInfo = document.getLineInformationOfOffset(region.getOffset());
				lineText = document.get(lineInfo.getOffset(), lineInfo.getLength());
				lineNumber = document.getLineOfOffset(region.getOffset());
			} catch (BadLocationException ex) {
				return null;
			}

			if (lineText.contains("goto") || lineText.contains("if")) //$NON-NLS-1$ //$NON-NLS-2$
			{
				/*
				 * mark a complete instruction as hyperlink "12 goto 23;" ->goto 23
				 */
				int begin = lineText.indexOf(" ", 7); //$NON-NLS-1$
				int end = lineText.indexOf(";"); //$NON-NLS-1$
				if (end < 0 || begin < 0 || end == begin + 1 || region.getOffset() < lineInfo.getOffset() + begin + 1)
					return null;

				String linkText = lineText.substring(begin + 1, end);

				IRegion r2 = new Region(lineInfo.getOffset() + begin + 1, linkText.length());
				return new IHyperlink[] { new ClassFileLocalHyperLink(r2, linkText, lineNumber) };
			} else if (lineText.contains("/* .")) //$NON-NLS-1$
			{
				int begin = lineText.indexOf("/* .") + 2; //$NON-NLS-1$
				int end = lineText.indexOf("*/") - 1; //$NON-NLS-1$
				if (end < 0 || begin < 0 || end == begin + 1 || region.getOffset() < lineInfo.getOffset() + begin + 1)
					return null;

				String linkText = lineText.substring(begin + 2, end);

				IRegion r2 = new Region(lineInfo.getOffset() + begin + 1, linkText.length());
				return new IHyperlink[] { new ClassFileFieldLocalHyperLink(r2, linkText, lineNumber) };

			}
			// else{
			// int begin= lineText.indexOf("/*");
			// int end = lineText.indexOf("*/");
			// if(end<0 || begin<0 || end==begin+1)
			// return null;
			//
			// String text = line.substring(begin+1,end+1);
			//
			// IRegion r2 = new Region(lineInfo.getOffset() + begin + 1,
			// text.length());
			// return new IHyperlink[] {new ClassFileLocalHyperLink(r2)};
			//
			// }

			return null;

		}

	}

	/**
	 * Local Hyperlink class for bytecode visualizer document.
	 */
	class ClassFileLocalHyperLink implements IHyperlink {

		/**
		 * LInk region.
		 */
		private IRegion region;

		/**
		 * Text of the marked link.
		 */
		private String text;

		/**
		 * The line number in which the link has been detected.
		 */
		private int lineNumber;

		/**
		 * Constructor.
		 * 
		 * @param region
		 * @param text
		 * @param lineNumber
		 */
		public ClassFileLocalHyperLink(IRegion region, String text, int lineNumber) {
			super();
			this.region = region;
			this.text = text;
			this.lineNumber = lineNumber;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkRegion()
		 */
		public IRegion getHyperlinkRegion() {
			return region;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkText()
		 */
		public String getHyperlinkText() {
			return text;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.text.hyperlink.IHyperlink#getTypeLabel()
		 */
		public String getTypeLabel() {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.text.hyperlink.IHyperlink#open()
		 */
		public void open() {

			DisassemblerDocumentProvider bdp = (DisassemblerDocumentProvider) fTextEditor.getDocumentProvider();
			IMethodSection method = bdp.getClassFileDocument().findMethodSection(lineNumber);
			List<IInstructionLine> instructions = method.getInstructionLines();

			/* find selected instruction */
			int targetOffset = ByteCodeConstants.INVALID_OFFSET;
			for (IInstructionLine i : instructions) {
				if (i.getLine() == lineNumber) {
					if (i.getInstruction() instanceof BranchInstruction) {
						BranchInstruction bi = (BranchInstruction) i.getInstruction();

						targetOffset = bi.getBranchOffset() + bi.getOffset();

						break;
					}
				}

			}

			/* find target instruction */
			for (IInstructionLine i : instructions) {
				if (i.getInstruction().getOffset() == targetOffset) {
					IDocument document = fTextEditor.getDocumentProvider().getDocument(fTextEditor.getEditorInput());
					int destOffset, destLength;
					try {
						destOffset = document.getLineOffset(i.getLine());
						destLength = document.getLineLength(i.getLine());
					} catch (BadLocationException e) {
						return;
					}

					fTextEditor.selectAndReveal(destOffset, destLength - 1);
					break;
				}
			}

		}

	}

	/**
	 * Local Hyperlink class for bytecode visualizer document. The field within a
	 * class file document is assigned to the link.
	 */
	class ClassFileFieldLocalHyperLink implements IHyperlink {

		/**
		 * LInk region.
		 */
		private IRegion region;

		/**
		 * Text of the marked link.
		 */
		private String text;

		/**
		 * The line number in which the link has been detected.
		 */
		@SuppressWarnings("unused")
		private int lineNumber;

		public ClassFileFieldLocalHyperLink(IRegion region, String text, int lineNumber) {
			super();
			this.region = region;
			this.text = text;
			this.lineNumber = lineNumber;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkRegion()
		 */
		public IRegion getHyperlinkRegion() {
			return region;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkText()
		 */
		public String getHyperlinkText() {
			return text;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.text.hyperlink.IHyperlink#getTypeLabel()
		 */
		public String getTypeLabel() {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.text.hyperlink.IHyperlink#open()
		 */
		public void open() {

			DisassemblerDocumentProvider bdp = (DisassemblerDocumentProvider) fTextEditor.getDocumentProvider();
			IFieldSection field = bdp.getClassFileDocument().findFieldSection(text);

			IDocument document = fTextEditor.getDocumentProvider().getDocument(fTextEditor.getEditorInput());

			int destOffset, destLength, elementIndex, elementLength;

			try {
				destOffset = document.getLineOffset(field.getBytecodeDocumentLine());
				destLength = document.getLineLength(field.getBytecodeDocumentLine());

				String lineString = document.get(destOffset, destLength);
				elementIndex = lineString.indexOf(" " + text + ";") + 1; //$NON-NLS-1$ //$NON-NLS-2$
				elementLength = text.length();

			} catch (BadLocationException e) {
				return;
			}

			fTextEditor.selectAndReveal(destOffset + elementIndex, elementLength);
		}
	}

	protected Map<String, IAdaptable> getHyperlinkDetectorTargets(ISourceViewer sourceViewer) {
		Map<String, IAdaptable> targets = super.getHyperlinkDetectorTargets(sourceViewer);
		targets.put("org.eclipse.jdt.ui.javaCode", fTextEditor); //$NON-NLS-1$
		return targets;
	}
}