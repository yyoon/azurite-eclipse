package edu.cmu.scs.azurite.ui.handlers;

import java.util.ArrayList;
import java.util.List;

import name.fraser.neil.plaintext.diff_match_patch;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.model.undo.SelectiveUndoEngine;
import edu.cmu.scs.fluorite.commands.Delete;
import edu.cmu.scs.fluorite.commands.Insert;
import edu.cmu.scs.fluorite.commands.Replace;
import edu.cmu.scs.fluorite.model.EventRecorder;
import edu.cmu.scs.fluorite.recorders.DocumentRecorder;
import edu.cmu.scs.fluorite.recorders.IDocumentRecorderInterceptor;
import edu.cmu.scs.fluorite.util.Utilities;

public class StepwiseUndoInRegionHandler extends AbstractHandler {
	
	private IDocument lastReferredActiveDocument;
	
	private ITextSelection lastKnownSelection;
	private String lastKnownSnapshot;
	private long lastModificationStamp;
	
	private List<String> snapshotsAfterEachStep;
	private ITextSelection originalSelection;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ITextSelection selection = HandlerUtilities.getSelectedRegion();

		// get active editor
		IEditorPart editorPart = Utilities.getActiveEditor();
		
		if (!(editorPart instanceof AbstractTextEditor)) {
			// Do nothing.
			return null;
		}
		
		ITextEditor editor = (ITextEditor) editorPart;
		IDocumentProvider dp = editor.getDocumentProvider();
		final IDocument doc = dp.getDocument(editor.getEditorInput());
		
		if (!(doc instanceof IDocumentExtension4)) {
			throw new RuntimeException("Document should be extending IDocumentExtension4!!");
		}
		
		IDocumentExtension4 ext4 = (IDocumentExtension4) doc;
		
		// Determine if this is the first invocation.
		boolean firstInvocation = determineFirstInvocation(doc, selection);
		
		if (firstInvocation) {
			List<RuntimeDC> dcs = HandlerUtilities.getOperationsInSelectedRegion();
			if (dcs == null) {
				return null;
			}
			
			this.lastReferredActiveDocument = doc;
			this.originalSelection = selection;
			
			StepwiseUndoState.clearStepCount();
			
			// Calculate the snapshots
			this.snapshotsAfterEachStep = new ArrayList<String>();
			this.snapshotsAfterEachStep.add(doc.get());
			
			for (int i = 1; i <= dcs.size(); ++i) {
				List<RuntimeDC> subDCs = dcs.subList(dcs.size() - i, dcs.size());
				IDocument copy = new Document(doc.get());
				SelectiveUndoEngine.getInstance().doSelectiveUndo(subDCs, copy);
				
				this.snapshotsAfterEachStep.add(copy.get());
			}
		}
		
		// See if there are remaining operations to be undone.
		if (this.snapshotsAfterEachStep.size() <= StepwiseUndoState.getStepCount() + 1) {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			MessageDialog.openInformation(shell,
					"Azurite - Stepwise Undo in Region",
					"No more operations to be undone.");
			
			return null;
		}
		
		// Perform the selective undo by replacing the document.
		StepwiseUndoState.incrementStepCount();
		final String originalSnapshot = this.snapshotsAfterEachStep.get(0);
		final String newSnapshot = this.snapshotsAfterEachStep.get(StepwiseUndoState.getStepCount());
		
		diff_match_patch dmp = new diff_match_patch();
		
		final int prefix = dmp.diff_commonPrefix(originalSnapshot, newSnapshot);
		final int suffix = dmp.diff_commonSuffix(originalSnapshot, newSnapshot);
		
		try {
			long stamp = ext4.getModificationStamp();
			long nextStamp = getNextStamp(stamp);
			
			// Intercept document change recorder with this stamp, only if it's not the first operation.
			if (!firstInvocation) {
				DocumentRecorder.getInstance().setIntercept(doc, stamp, nextStamp, new DocumentRecorderInterceptor(prefix, suffix, newSnapshot, originalSnapshot, doc));
			}
			
			// Replace the document using the stamp.
			int curPrefix = dmp.diff_commonPrefix(doc.get(), newSnapshot);
			int curSuffix = dmp.diff_commonSuffix(doc.get(), newSnapshot);
			
			ext4.replace(
					curPrefix,
					doc.getLength() - curPrefix - curSuffix,
					newSnapshot.substring(curPrefix, newSnapshot.length() - curSuffix),
					nextStamp);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		// Determine the resulting selection, and remember it.
		// First, determine the changed region. Use the prefix / suffix values.
		int startOffset = this.originalSelection.getOffset();
		int endOffset = startOffset + this.originalSelection.getLength() + newSnapshot.length() - originalSnapshot.length();
		
		if (prefix < startOffset) {
			startOffset = prefix;
		}
		
		if (newSnapshot.length() - suffix > endOffset) {
			endOffset = newSnapshot.length() - suffix;
		}
		
		editorPart.getEditorSite().getSelectionProvider().setSelection(new TextSelection(doc, startOffset, endOffset - startOffset));
		this.lastKnownSelection = new TextSelection(doc, startOffset, endOffset - startOffset);
		
		// Also, store the last known snapshot / modification stamp.
		this.lastKnownSnapshot = doc.get();
		this.lastModificationStamp = ext4.getModificationStamp();
		
		return null;
	}

	private boolean determineFirstInvocation(IDocument doc, ITextSelection selection) {
		IDocumentExtension4 ext4 = (IDocumentExtension4) doc;
		
		return StepwiseUndoState.getStepCount() == 0 ||
				this.lastReferredActiveDocument == null ||
				this.lastKnownSelection == null ||
				this.lastReferredActiveDocument != doc ||
				!this.lastKnownSnapshot.equals(doc.get()) ||
				this.lastModificationStamp != ext4.getModificationStamp() ||
				this.lastKnownSelection.getOffset() != selection.getOffset() ||
				this.lastKnownSelection.getLength() != selection.getLength();
	}
	
	private long getNextStamp(long stamp) {
		return (stamp == IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP || stamp == Long.MAX_VALUE)
				? 0
				: stamp + 1;
	}
	
	private static final class DocumentRecorderInterceptor implements IDocumentRecorderInterceptor {
		private final int prefix;
		private final int suffix;
		private final String newSnapshot;
		private final String originalSnapshot;
		private final IDocument doc;

		private DocumentRecorderInterceptor(int prefix, int suffix, String newSnapshot,
				String originalSnapshot, IDocument doc) {
			this.prefix = prefix;
			this.suffix = suffix;
			this.newSnapshot = newSnapshot;
			this.originalSnapshot = originalSnapshot;
			this.doc = doc;
		}

		@Override
		public void documentChanged(DocumentEvent event, EventRecorder recorder) {
			
			Document originalDoc = new Document(originalSnapshot);
			int startLine = -1;
			int endLine = -1;
			try {
				startLine = originalDoc.getLineOfOffset(prefix);
				endLine = originalDoc.getLineOfOffset(originalDoc.getLength() - suffix);
			} catch (BadLocationException e) {
			}
			
			if (prefix + suffix == originalSnapshot.length()) {
				Insert insert = new Insert(prefix, newSnapshot.substring(prefix, newSnapshot.length() - suffix), doc);
				recorder.amendLastDocumentChange(insert, true);
			} else if (prefix + suffix == newSnapshot.length()) {
				Delete delete = new Delete(
						prefix,
						originalSnapshot.length() - prefix - suffix,
						startLine,
						endLine,
						originalSnapshot.substring(prefix, originalSnapshot.length() - suffix),
						doc);
				
				recorder.amendLastDocumentChange(delete, true);
			} else {
				Replace replace = new Replace(
						prefix,
						originalSnapshot.length() - prefix - suffix,
						startLine,
						endLine,
						newSnapshot.length() - prefix - suffix,
						originalSnapshot.substring(prefix, originalSnapshot.length() - suffix),
						newSnapshot.substring(prefix, newSnapshot.length() - suffix),
						doc);
				
				recorder.amendLastDocumentChange(replace, true);
			}
		}
	}

	public static class StepwiseUndoState {
		
		private static int stepCount = 0;
		
		public static void clearStepCount() {
			stepCount = 0;
		}
		
		public static int getStepCount() {
			return stepCount;
		}
		
		public static void incrementStepCount() {
			++stepCount;
		}
		
	}

}
