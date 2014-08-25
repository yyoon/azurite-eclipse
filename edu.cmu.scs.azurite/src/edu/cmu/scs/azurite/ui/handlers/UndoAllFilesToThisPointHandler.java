package edu.cmu.scs.azurite.ui.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.model.FileKey;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.azurite.model.undo.SelectiveUndoEngine;

public class UndoAllFilesToThisPointHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String timestampString = event
				.getParameter("edu.cmu.scs.azurite.ui.commands.undoAllFilesToThisPoint.absTimestamp");
		
		long absTimestamp = Long.parseLong(timestampString);
		
		Map<FileKey, List<RuntimeDC>> params = new HashMap<FileKey, List<RuntimeDC>>();
		RuntimeHistoryManager history = RuntimeHistoryManager.getInstance();
		
		for (FileKey key : history.getFileKeys()) {
			params.put(key, history.filterDocumentChangesLaterThanOrEqualToTimestamp(key, absTimestamp));
		}
		
		SelectiveUndoEngine.getInstance().doSelectiveUndoOnMultipleFiles(params);
		
		return null;
	}

}
