package edu.cmu.scs.azurite.ui.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.handlers.IHandlerService;

import edu.cmu.scs.azurite.plugin.Activator;

public class SelectedCodeHandler extends AbstractHandler {
	
	private static final String DIALOG_TITLE = "Azurite - Commands for Selected Code";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		
		if (HandlerUtilities.isCodeSelected() == false) {
			MessageDialog.openInformation(shell, DIALOG_TITLE,
					"You must select some code first, in order to invoke these commands.");
			return null;
		}
		
		ListDialog dialog = new ListDialog(shell); 
		dialog.setTitle(DIALOG_TITLE);
		dialog.setMessage("Which of the following commands do you want to execute on the selected code?"); 
		dialog.setHelpAvailable(false);
		
		dialog.setContentProvider(new SelectedCodeOperationContentProvider());
		dialog.setLabelProvider(new SelectedCodeOperationLabelProvider());
		
		Object[][] input = {
				{ null, "Select Corresponding Timeline Rectangles", "edu.cmu.scs.azurite.ui.commands.selectCorrespondingTimelineRectanglesCommand" },
				{ "icons/undo_in_region.png", "Undo All Operations on the Selection", "edu.cmu.scs.azurite.ui.commands.undoInRegionCommand" },
				{ "icons/time_machine.png", "Launch Code History Diff View", "edu.cmu.scs.azurite.ui.commands.launchCodeHistoryDiff" },
		};
		
		dialog.setInput(input);
		dialog.setInitialSelections(new Object[] { input[0] });
		
		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				Object[] resultItem = (Object[]) result[0];
				String commandId = resultItem[2].toString();
				
				IHandlerService handlerService = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
				try {
					handlerService.executeCommand(commandId, null);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		return null;
	}
	
	private static class SelectedCodeOperationContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return (Object[]) inputElement;
		}
		
	}
	
	private static class SelectedCodeOperationLabelProvider extends LabelProvider {
		
		private List<Image> imagesToDispose = new ArrayList<Image>();

		@Override
		public void dispose() {
			super.dispose();
			
			for (Image image : imagesToDispose) {
				image.dispose();
			}
		}

		@Override
		public Image getImage(Object element) {
			if (element == null || ((Object[]) element)[0] == null) {
				return null;
			}
			
			Image image = Activator.getImageDescriptor(((Object[]) element)[0].toString()).createImage();
			imagesToDispose.add(image);
			
			return image;
		}

		@Override
		public String getText(Object element) {
			return ((Object[]) element)[1].toString();
		}
		
	}

}
