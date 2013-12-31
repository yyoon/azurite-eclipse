package edu.cmu.scs.azurite.ui.handlers;

import java.util.ArrayList;
import java.util.Arrays;
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
import edu.cmu.scs.azurite.views.TimelineViewPart;

public class SelectedRectsHandler extends AbstractHandler {
	
	private static final String DIALOG_TITLE = "Azurite - Commands for Selected Rectangles in Timeline";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		
		TimelineViewPart timeline = TimelineViewPart.getInstance();
		
		if (timeline == null) {
			MessageDialog.openInformation(shell, DIALOG_TITLE,
					"You must open the timeline first.");
			return null;
		}
		
		int numSelected = timeline.getSelectedRectsCount();
		if (numSelected == 0) {
			MessageDialog.openInformation(shell, DIALOG_TITLE,
					"You must select some rectangles first.");
			return null;
		}
		
		ListDialog dialog = new ListDialog(shell); 
		dialog.setTitle(DIALOG_TITLE);
		dialog.setMessage("Which of the following commands do you want to execute on the selected rectangles?"); 
		dialog.setHelpAvailable(false);
		
		dialog.setContentProvider(new SelectedRectsOperationContentProvider());
		dialog.setLabelProvider(new SelectedRectsOperationLabelProvider());
		
		Object[][] input = {
				{ null, "Selective Undo", "edu.cmu.scs.azurite.ui.commands.selectiveUndoCommand" },
				{ null, "Undo Everything After the Last Selected Rectangle", "edu.cmu.scs.azurite.ui.commands.undoEverythingAfterSelectionCommand" },
				{ null, "Jump to the Affected Code in the Editor", "edu.cmu.scs.azurite.ui.commands.jumpToTheAffectedCodeCommand" },
		};
		
		if (numSelected > 1) {
			input = Arrays.copyOfRange(input, 0, 2);
		}
		
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
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		return null;
	}
	
	private static class SelectedRectsOperationContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {
			// Do nothing
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// Do nothing
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return (Object[]) inputElement;
		}
		
	}
	
	private static class SelectedRectsOperationLabelProvider extends LabelProvider {
		
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
