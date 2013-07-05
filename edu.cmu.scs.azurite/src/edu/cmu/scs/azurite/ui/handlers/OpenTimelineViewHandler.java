package edu.cmu.scs.azurite.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class OpenTimelineViewHandler extends AbstractHandler {
	
	private static final String TIMELINE_VIEW_ID =
			"edu.cmu.scs.azurite.views.TimelineViewPart";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				try {
					page.showView(TIMELINE_VIEW_ID);
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			}
		}
		
		return null;
	}

}
