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

public class SelectAllInsideHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		TimelineViewPart timelineViewPart = TimelineViewPart.getInstance();
		if (timelineViewPart == null) {
			// Do nothing if the timeline view is currently not available.
			return null;
		}
		
		long absTimestampStart = timelineViewPart.getTimeRangeStart();
		long absTimestampEnd = timelineViewPart.getTimeRangeEnd();
		
		if (absTimestampStart > absTimestampEnd) {
			long temp = absTimestampStart;
			absTimestampStart = absTimestampEnd;
			absTimestampEnd = temp;
		}
		
		List<RuntimeDC> dcs = new ArrayList<RuntimeDC>();
		RuntimeHistoryManager manager = RuntimeHistoryManager.getInstance();
		for (FileKey key : manager.getFileKeys()) {
			dcs.addAll(manager.filterDocumentChangesLaterThanOrEqualToAndEarlierThanTimestamps(key, absTimestampStart, absTimestampEnd));
		}
		
		// Extract the ids.
		List<OperationId> ids = OperationId.getOperationIdsFromRuntimeDCs(dcs);
		
		// Send this to the timeline view, if it's available.
		timelineViewPart.addSelection(ids, true);
		
		return null;
	}

}
