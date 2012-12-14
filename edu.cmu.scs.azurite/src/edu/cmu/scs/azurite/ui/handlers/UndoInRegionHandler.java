package edu.cmu.scs.azurite.ui.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.azurite.model.undo.SelectiveUndoEngine;
import edu.cmu.scs.fluorite.util.Utilities;

public class UndoInRegionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			// get active editor
			IEditorPart editorPart = Utilities.getActiveEditor();

			if (editorPart instanceof AbstractTextEditor) {
				// check if there is text selection
				int offset = 0;
				int length = 0;
				
				IEditorSite iEditorSite = editorPart.getEditorSite();
				if (iEditorSite != null) {
					ISelectionProvider selectionProvider = iEditorSite
							.getSelectionProvider();
					if (selectionProvider != null) {
						ISelection iSelection = selectionProvider
								.getSelection();
						offset = ((ITextSelection) iSelection).getOffset();
						if (!iSelection.isEmpty()) {
							length = ((ITextSelection) iSelection).getLength();
						}
					}
				}

				ITextEditor editor = (ITextEditor) editorPart;
				IDocumentProvider dp = editor.getDocumentProvider();
				IDocument doc = dp.getDocument(editor.getEditorInput());
				
				List<RuntimeDC> dcs = RuntimeHistoryManager.getInstance()
						.filterDocumentChangesByRegion(offset, offset + length);
				
				SelectiveUndoEngine.getInstance().doSelectiveUndo(dcs, doc);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

}
