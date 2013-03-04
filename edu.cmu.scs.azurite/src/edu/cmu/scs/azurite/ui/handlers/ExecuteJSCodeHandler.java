package edu.cmu.scs.azurite.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import edu.cmu.scs.azurite.views.TimelineViewPart;

public class ExecuteJSCodeHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String codeToExecute = event
				.getParameter("edu.cmu.scs.azurite.ui.commands.executeJSCode.codeToExecute");
		
		TimelineViewPart timeline = TimelineViewPart.getInstance();
		timeline.executeJSCode(codeToExecute);
		
		return null;
	}

}
