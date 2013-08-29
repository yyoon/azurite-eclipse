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
		// Check if there is already a dialog.
		InteractiveSelectiveUndoDialog isuDialog = InteractiveSelectiveUndoDialog.getInstance();
		if (isuDialog != null) {
			isuDialog.getShell().forceActive();
		}
		else {
			final Shell parentShell = Display.getDefault().getActiveShell();
			isuDialog = new InteractiveSelectiveUndoDialog(parentShell);
			isuDialog.create();
			isuDialog.open();
		}
		
		return null;
	}

}
