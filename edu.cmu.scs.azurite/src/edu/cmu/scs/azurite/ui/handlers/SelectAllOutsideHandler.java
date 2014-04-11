package edu.cmu.scs.azurite.ui.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.model.FileKey;
import edu.cmu.scs.azurite.model.OperationId;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.azurite.views.TimelineViewPart;

public class SelectAllOutsideHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String timestampStringStart = event.getParameter("edu.cmu.scs.azurite.ui.commands.absTimestampStart");
		String timestampStringEnd = event.getParameter("edu.cmu.scs.azurite.ui.commands.absTimestampEnd");
		long absTimestampStart = Long.parseLong(timestampStringStart);
		long absTimestampEnd = Long.parseLong(timestampStringEnd);
		
		if (absTimestampStart > absTimestampEnd) {
			long temp = absTimestampStart;
			absTimestampStart = absTimestampEnd;
			absTimestampEnd = temp;
		}
		
		List<RuntimeDC> dcs = new ArrayList<RuntimeDC>();
		RuntimeHistoryManager manager = RuntimeHistoryManager.getInstance();
		for (FileKey key : manager.getFileKeys()) {
			dcs.addAll(manager.filterDocumentChangesLaterThanOrEqualToTimestamp(key, absTimestampEnd));
			dcs.addAll(manager.filterDocumentChangesEarlierThanTimestamp(key, absTimestampStart));
		}
		
		// Extract the ids.
		List<OperationId> ids = OperationId.getOperationIdsFromRuntimeDCs(dcs);
		
		// Send this to the timeline view, if it's available.
		TimelineViewPart timelineViewPart = TimelineViewPart.getInstance();
		if (timelineViewPart != null) {
			timelineViewPart.addSelection(ids, true);
		}
		
		return null;
	}

}
