package edu.cmu.scs.azurite.ui.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.jface.dialogs.PartialCodeHistoryDialog;
import edu.cmu.scs.fluorite.model.EventRecorder;
import edu.cmu.scs.fluorite.util.Utilities;

public class LaunchPartialCodeHistoryHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Shell parentShell = Display.getDefault().getActiveShell();
		
		List<RuntimeDC> dcs = HandlerUtilities.getOperationsInSelectedRegion();
		if (dcs == null || dcs.isEmpty()) {
			return null;
		}
		
		IEditorPart editor = EventRecorder.getInstance().getEditor();
		if (editor == null) {
			return null;
		}
		
		IDocument doc = Utilities.getDocument(editor);
		if (doc == null) { 
			return null;
		}
		
		String fileContent = doc.get();
		
		ITextSelection selection = HandlerUtilities.getSelectedRegion();
		
		PartialCodeHistoryDialog historyDialog = new PartialCodeHistoryDialog(
				parentShell, dcs, fileContent, selection.getOffset(),
				selection.getOffset() + selection.getLength());
		historyDialog.create();
		historyDialog.open();
		
		return null;
	}

}
