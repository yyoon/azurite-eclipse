package edu.cmu.scs.azurite.ui.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;

import edu.cmu.scs.azurite.commands.HistorySearchCommand;
import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.commands.runtime.Segment;
import edu.cmu.scs.azurite.jface.dialogs.HistorySearchDialog;
import edu.cmu.scs.azurite.model.IRuntimeDCFilter;
import edu.cmu.scs.azurite.model.OperationId;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.azurite.model.undo.Chunk;
import edu.cmu.scs.azurite.model.undo.SelectiveUndoEngine;
import edu.cmu.scs.azurite.views.TimelineViewPart;
import edu.cmu.scs.fluorite.model.EventRecorder;
import edu.cmu.scs.fluorite.util.Utilities;

public class HistorySearchHandler extends AbstractHandler {

	private static final String HISTORY_SEARCH = "History Search";
	private static final String NO_RESULTS_FOUND = "No results found.";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Shell parentShell = Display.getDefault().getActiveShell();
		
		HistorySearchDialog dialog = new HistorySearchDialog(parentShell, "");
		dialog.create();
		int result = dialog.open();
		
		System.out.println("Dialog Result: " + result);
		
		if (result == HistorySearchDialog.CLOSE) {
			return null;
		}
		
		// FIXME maybe move the logic to somewhere else so this can be reused.
		boolean caseSensitive = dialog.isCaseSensitive();
		boolean currentSession = dialog.isCurrentSession();
		boolean scopeSelectedCode = dialog.isScopeSelectedCode();
		String searchText = dialog.getSearchText();
		String searchTextLowerCase = searchText.toLowerCase();
		
		EventRecorder.getInstance().recordCommand(new HistorySearchCommand(
				searchText, caseSensitive, scopeSelectedCode, currentSession));
		
		IEditorPart editor = EventRecorder.getInstance().getEditor();
		if (editor == null) {
			return null;
		}
		
		IDocument doc = Utilities.getDocument(editor);
		if (doc == null) {
			return null;
		}
		
		List<RuntimeDC> dcs = scopeSelectedCode ? HandlerUtilities.getOperationsInSelectedRegion()
				: RuntimeHistoryManager.getInstance().filterDocumentChanges(new IRuntimeDCFilter() {
					@Override
					public boolean filter(RuntimeDC runtimeDC) {
						return true;
					}
				});
		if (dcs == null || dcs.isEmpty()) {
			MessageDialog.openInformation(parentShell, HISTORY_SEARCH, NO_RESULTS_FOUND);
			return null;
		}
		
		// Only current session?
		if (currentSession) {
			List<RuntimeDC> newDCs = new ArrayList<RuntimeDC>();
			for (RuntimeDC dc : dcs) {
				if (dc.getOriginal().getSessionId() == EventRecorder.getInstance().getStartTimestamp()) {
					newDCs.add(dc);
				}
			}
			
			dcs = newDCs;
		}
		
		// There was no document changes in the selected scope.
		if (dcs.isEmpty()) {
			MessageDialog.openInformation(parentShell, HISTORY_SEARCH, NO_RESULTS_FOUND);
			return null;
		}

		
		
		// FIXME This selective undo is ridiculously inefficient, but maybe OK for now.
		// TODO This is almost a cloned code from CodeHistoryDiffViewer.java Modularize.
		List<RuntimeDC> resultDCs = new ArrayList<RuntimeDC>();
		
		// determine the code scope.
		int selectionStart = 0;
		int selectionEnd = doc.getLength();
		
		if (scopeSelectedCode) {
			ITextSelection selection = HandlerUtilities.getSelectedRegion();
			selectionStart = selection.getOffset();
			selectionEnd = selection.getOffset() + selection.getLength();
		}
		
		int selectionLength = selectionEnd - selectionStart;
		String codeContent = doc.get();
		String selectedCode = codeContent.substring(selectionStart,  selectionEnd);
		
		for (int version = 1; version <= dcs.size(); ++version) {
			String resultingCode = null;
			
			if (version == dcs.size()) {
				resultingCode = codeContent;
			} else {
				// Get the previous versions by performing undo.
				List<RuntimeDC> subList = dcs.subList(version, dcs.size());
				Chunk chunk = getChunkFromSublist(subList, selectionStart, selectionEnd);
				Collections.sort(chunk, Segment.getLocationComparator());
				
				int startOffset = chunk.getStartOffset();
				int endOffset = chunk.getEndOffset();
				String initialContent = codeContent.substring(startOffset, endOffset);
				
				String undoResult = SelectiveUndoEngine.getInstance()
						.doSelectiveUndoChunkWithoutConflicts(chunk, initialContent);
				
				StringBuilder historyContent = new StringBuilder(selectedCode);
				historyContent.replace(
						Math.max(startOffset - selectionStart, 0),
						Math.min(endOffset - selectionStart, selectionLength),
						undoResult);
				
				resultingCode = historyContent.toString();
			}
			
			// resultingCode should not be null.
			if (resultingCode == null) { 
				throw new IllegalStateException();
			}
			
			// Check if the resulting code contains the provided search text.
			// There may be locale problem?
			// http://javapapers.com/core-java/javas-tolowercase-has-got-a-surprise-for-you/
			if (caseSensitive) {
				if (resultingCode.contains(searchText)) {
					resultDCs.add(dcs.get(version - 1));
				}
			} else {
				if (resultingCode.toLowerCase().contains(searchTextLowerCase)) {
					resultDCs.add(dcs.get(version - 1));
				}
			}
		}
		
		// Now the resultDCs contains all the searched operations.
		
		// Extract the ids.
		List<OperationId> ids = new ArrayList<OperationId>();
		for (RuntimeDC dc : resultDCs) {
			ids.add(dc.getOperationId());
		}
		
		if (ids.isEmpty()) {
			MessageDialog.openInformation(parentShell, HISTORY_SEARCH, NO_RESULTS_FOUND);
			return null;
		}
		
		// Send this to the timeline view, if it's available.
		// TODO open up the timeline if it's not.
		TimelineViewPart timelineViewPart = TimelineViewPart.getInstance();
		if (timelineViewPart != null) {
			timelineViewPart.addSelection(ids, true);
		}
		
		return null;
	}

	private Chunk getChunkFromSublist(List<RuntimeDC> subList,
			int selectionStart, int selectionEnd) {
		Chunk chunk = new Chunk();
		for (RuntimeDC dc : subList) {
			for (Segment segment : dc.getAllSegments()) {
				if (segment.inSelectionRange(selectionStart, selectionEnd)) {
					chunk.add(segment);
				}
			}
		}
		return chunk;
	}

}
