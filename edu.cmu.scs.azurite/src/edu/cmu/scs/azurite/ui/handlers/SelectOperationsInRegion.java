package edu.cmu.scs.azurite.ui.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.model.OperationId;
import edu.cmu.scs.azurite.views.TimelineViewPart;
import edu.cmu.scs.fluorite.commands.BaseDocumentChangeEvent;

public class SelectOperationsInRegion extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RuntimeDC> dcs = HandlerUtilities.getOperationsInSelectedRegion();
		if (dcs == null) {
			return null;
		}
		
		// Extract the ids.
		List<OperationId> ids = new ArrayList<OperationId>();
		for (RuntimeDC dc : dcs) {
			BaseDocumentChangeEvent original = dc.getOriginal();
			ids.add(new OperationId(original.getSessionId(), original.getCommandIndex()));
		}
		
		if (ids.isEmpty()) {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			MessageDialog.openInformation(shell,
					"Azurite - Select Corresponding Timeline Rectangles",
					"There are no rectangles corresponding to the selected code.");
			
			return null;
		}
		
		// Send this to the timeline view, if it's available.
		TimelineViewPart timelineViewPart = TimelineViewPart.getInstance();
		if (timelineViewPart != null) {
			timelineViewPart.addSelection(ids, true);
		}

		return null;
	}

}
