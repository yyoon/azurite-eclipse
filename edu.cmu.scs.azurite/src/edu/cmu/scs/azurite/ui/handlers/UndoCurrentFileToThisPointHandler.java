package edu.cmu.scs.azurite.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import edu.cmu.scs.azurite.model.OperationId;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.azurite.model.undo.SelectiveUndoEngine;

public class UndoCurrentFileToThisPointHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String oidString = event
				.getParameter("edu.cmu.scs.azurite.ui.commands.undoCurrentFileToThisPoint.annotationId");
		
		String[] tokens = oidString.split("_");
		OperationId oid = new OperationId(Long.parseLong(tokens[0]), Long.parseLong(tokens[1]));
		
		RuntimeHistoryManager history = RuntimeHistoryManager.getInstance();
		SelectiveUndoEngine.getInstance().doSelectiveUndo(
				history.filterDocumentChangesGreaterThanId(oid));
		
		return null;
	}

}
