package edu.cmu.scs.azurite.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.azurite.model.undo.SelectiveUndoEngine;

public class UndoCurrentFileToThisPointHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String timestampString = event
				.getParameter("edu.cmu.scs.azurite.ui.commands.undoCurrentFileToThisPoint.absTimestamp");
		
		long absTimestamp = Long.parseLong(timestampString);
		
		RuntimeHistoryManager history = RuntimeHistoryManager.getInstance();
		SelectiveUndoEngine.getInstance().doSelectiveUndo(
				history.filterDocumentChangesLaterThanTimestamp(absTimestamp));
		
		return null;
	}

}
