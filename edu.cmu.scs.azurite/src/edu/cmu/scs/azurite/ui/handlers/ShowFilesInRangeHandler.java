package edu.cmu.scs.azurite.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import edu.cmu.scs.azurite.views.TimelineViewPart;

public class ShowFilesInRangeHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		TimelineViewPart timeline = TimelineViewPart.getInstance();
		if (timeline != null) {
			timeline.executeJSCode("showAllFilesEditedInRange();");
		}
		return null;
	}

}
