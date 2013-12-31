package edu.cmu.scs.azurite.expressions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.texteditor.AbstractTextEditor;

public class CodeSelectionPropertyTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		switch (property) {

		case "codeSelected": {
			IEditorPart editorPart = (IEditorPart) receiver;
			if (!(editorPart instanceof AbstractTextEditor)) {
				return false;
			}

			// check if there is text selection
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
				ITextSelection textSelection = (ITextSelection) iSelection;
				return !textSelection.isEmpty() && textSelection.getLength() > 0;
			}

			return false;
		}

		default:
			break;
		}

		return false;
	}

}
