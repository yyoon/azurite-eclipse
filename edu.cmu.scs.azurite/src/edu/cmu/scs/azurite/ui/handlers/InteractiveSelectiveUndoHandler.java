package edu.cmu.scs.azurite.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import edu.cmu.scs.azurite.jface.dialogs.InteractiveSelectiveUndoDialog;

public class InteractiveSelectiveUndoHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Shell parentShell = Display.getDefault().getActiveShell();
		InteractiveSelectiveUndoDialog isuDialog = new InteractiveSelectiveUndoDialog(parentShell);
		isuDialog.create();
		isuDialog.open();
		
		return null;
	}

}
