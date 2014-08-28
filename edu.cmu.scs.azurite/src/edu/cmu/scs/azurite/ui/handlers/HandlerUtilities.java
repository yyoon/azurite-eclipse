package edu.cmu.scs.azurite.ui.handlers;

import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.model.IRuntimeDCFilter;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.fluorite.model.EventRecorder;
import edu.cmu.scs.fluorite.util.Utilities;

public class HandlerUtilities {
	
	public static boolean isCodeSelected() {
		try {
			// get active editor
			IEditorPart editorPart = EventRecorder.getInstance().getEditor();

			if (!(editorPart instanceof AbstractTextEditor)) {
				return false;
			}
			
			IEditorSite iEditorSite = editorPart.getEditorSite();
			if (iEditorSite == null) {
				return false;
			}
			
			ISelectionProvider selectionProvider = iEditorSite.getSelectionProvider();
			if (selectionProvider == null) {
				return false;
			}
			
			ISelection iSelection = selectionProvider.getSelection();
			if (iSelection instanceof ITextSelection) {
				ITextSelection iTextSelection = (ITextSelection) iSelection;
				if (!iTextSelection.isEmpty() && iTextSelection.getLength() > 0) {
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}

	public static List<RuntimeDC> getOperationsInSelectedRegion() {
		try {
			// get active editor
			IEditorPart editorPart = EventRecorder.getInstance().getEditor();
			
			if (!(editorPart instanceof AbstractTextEditor)) {
				return null;
			}

			// check if there is text selection
			int offset = 0;
			int length = 0;
			
			IEditorSite iEditorSite = editorPart.getEditorSite();
			if (iEditorSite != null) {
				ISelectionProvider selectionProvider = iEditorSite.getSelectionProvider();
				if (selectionProvider != null) {
					ISelection iSelection = selectionProvider.getSelection();
					if (iSelection instanceof ITextSelection) {
						ITextSelection textSelection = (ITextSelection) iSelection; 
						offset = textSelection.getOffset();
						length = textSelection.getLength();
						
						// Strip out the ending newline characters from the selection.
						String text = textSelection.getText();
						if (text.endsWith("\r\n")) {
							length -= 2;
						} else if (text.endsWith("\r") || text.endsWith("\n")) {
							length -= 1;
						}
					}
				}
			}
			
			// Check if the entire file is selected.
			IDocument doc = Utilities.getDocument(editorPart);
			if (offset == 0 && length == doc.getLength()) {
				return RuntimeHistoryManager.getInstance()
						.filterDocumentChanges(new IRuntimeDCFilter() {
							@Override
							public boolean filter(RuntimeDC runtimeDC) {
								return true;
							}
						});
			}
			
			// Check if the selected region ends with newline characters.

			// In all the other cases.
			List<RuntimeDC> dcs = RuntimeHistoryManager.getInstance()
					.filterDocumentChangesByRegion(offset, offset + length);
			
			return dcs;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static ITextSelection getSelectedRegion() {
		try {
			// get active editor
			IEditorPart editorPart = EventRecorder.getInstance().getEditor();
			if (!(editorPart instanceof AbstractTextEditor)) {
				return null;
			}

			// check if there is text selection
			IEditorSite iEditorSite = editorPart.getEditorSite();
			if (iEditorSite != null) {
				ISelectionProvider selectionProvider = iEditorSite
						.getSelectionProvider();
				if (selectionProvider != null) {
					ISelection iSelection = selectionProvider
							.getSelection();
					if (iSelection instanceof ITextSelection) {
						return (ITextSelection)iSelection;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
