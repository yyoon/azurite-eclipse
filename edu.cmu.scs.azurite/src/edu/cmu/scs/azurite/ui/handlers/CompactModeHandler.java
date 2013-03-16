package edu.cmu.scs.azurite.ui.handlers;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

import edu.cmu.scs.azurite.views.TimelineViewPart;

public class CompactModeHandler extends AbstractHandler implements IElementUpdater {
	
	private boolean mChecked = true;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Command command = event.getCommand();
		boolean oldValue = HandlerUtil.toggleCommandState(command);
		// use the old value and perform the operation
		
		String newLayout = oldValue ? "REALTIME" : "COMPACT";
		TimelineViewPart timeline = TimelineViewPart.getInstance();
		timeline.executeJSCode("layout(LayoutEnum." + newLayout + ");");
		
		mChecked = !oldValue;
		
		ICommandService service = (ICommandService) PlatformUI.getWorkbench()
				.getService(ICommandService.class);
		service.refreshElements(event.getCommand().getId(), null);
		
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void updateElement(UIElement element, Map parameters) {
		element.setChecked(mChecked);
	}

}
