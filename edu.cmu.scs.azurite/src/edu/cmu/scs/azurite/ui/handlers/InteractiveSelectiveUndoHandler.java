package edu.cmu.scs.azurite.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import edu.cmu.scs.azurite.jface.dialogs.InteractiveSelectiveUndoDialog;

public class InteractiveSelectiveUndoHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		InteractiveSelectiveUndoDialog.launch();
		
		return null;
	}

}
