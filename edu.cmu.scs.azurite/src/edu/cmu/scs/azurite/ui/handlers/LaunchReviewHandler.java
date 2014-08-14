package edu.cmu.scs.azurite.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.azurite.views.ReviewViewPart;

public class LaunchReviewHandler extends AbstractHandler {
	
	private static final String REVIEW_VIEW_ID =
			"edu.cmu.scs.azurite.views.ReviewViewPart";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (RuntimeHistoryManager.getInstance().getEntireHistory().isEmpty()) {
			MessageDialog.openInformation(
					Display.getDefault().getActiveShell(),
					"Azurite - Review",
					"There are no chagnes to be reviewed.");
			return null;
		}
		
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				try {
					IViewPart viewPart = page.showView(REVIEW_VIEW_ID);
					
					if (viewPart instanceof ReviewViewPart) {
						ReviewViewPart reviewView = (ReviewViewPart) viewPart;
						reviewView.addReviewViewer();
					}
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			}
		}

		return null;
	}

}
