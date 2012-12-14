package edu.cmu.scs.azurite.actions;

import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.azurite.model.undo.SelectiveUndoEngine;
import edu.cmu.scs.fluorite.util.Utilities;

public class UndoInRegionAction implements IObjectActionDelegate {

	@Override
	public void run(IAction action) {
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
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// TODO Auto-generated method stub

	}

}
