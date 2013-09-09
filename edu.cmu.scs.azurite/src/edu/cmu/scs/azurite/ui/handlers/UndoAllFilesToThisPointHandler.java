package edu.cmu.scs.azurite.ui.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.model.FileKey;
import edu.cmu.scs.azurite.model.OperationId;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.azurite.model.undo.SelectiveUndoEngine;

public class UndoAllFilesToThisPointHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String oidString = event
				.getParameter("edu.cmu.scs.azurite.ui.commands.undoAllFilesToThisPoint.annotationId");
		
		String[] tokens = oidString.split("_");
		OperationId oid = new OperationId(Long.parseLong(tokens[0]), Long.parseLong(tokens[1]));
		
		Map<FileKey, List<RuntimeDC>> params = new HashMap<FileKey, List<RuntimeDC>>();
		RuntimeHistoryManager history = RuntimeHistoryManager.getInstance();
		
		for (FileKey key : history.getFileKeys()) {
			params.put(key, history.filterDocumentChangesGreaterThanId(key, oid));
		}
		
		SelectiveUndoEngine.getInstance().doSelectiveUndoOnMultipleFiles(params);
		
		return null;
	}

}
