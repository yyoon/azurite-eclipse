package edu.cmu.scs.azurite.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;

import edu.cmu.scs.azurite.jface.dialogs.AboutDialog;

public class AboutHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		AboutDialog dialog = new AboutDialog(Display.getDefault().getActiveShell());
		dialog.create();
		dialog.open();
		
		return null;
	}

}
