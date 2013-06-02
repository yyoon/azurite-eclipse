package edu.cmu.scs.azurite.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import edu.cmu.scs.azurite.views.TimelineViewPart;

public class UndoEverythingAfterSelectionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		TimelineViewPart timeline = TimelineViewPart.getInstance();
		timeline.executeJSCode("undoEverythingAfterSelection();");
		
		return null;
	}

}
