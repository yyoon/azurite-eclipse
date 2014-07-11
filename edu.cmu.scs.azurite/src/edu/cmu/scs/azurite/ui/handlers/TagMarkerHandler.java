package edu.cmu.scs.azurite.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import edu.cmu.scs.azurite.jface.dialogs.AddTagDialog;
import edu.cmu.scs.azurite.views.TimelineViewPart;
import edu.cmu.scs.fluorite.commands.AnnotateCommand;
import edu.cmu.scs.fluorite.model.EventRecorder;

public class TagMarkerHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// If the timeline view is off. don't do anything.
		TimelineViewPart timeline = TimelineViewPart.getInstance();
		if (timeline == null) {
			return null;
		}
		
		// Get the marker timestamp.
		long markerTimestamp = timeline.getMarkerTimestamp();
		if (markerTimestamp == -1) {
			return null;
		}
		
		Shell shell = Display.getDefault().getActiveShell();
		AddTagDialog dialog = new AddTagDialog(shell, "Tag the Marker Position");
		dialog.open();
		
		// Only log if the user chooses OK.
		if (dialog.getReturnCode() == AddTagDialog.OK) {
			EventRecorder.getInstance().recordCommand(
					new AnnotateCommand(AnnotateCommand.TAG, dialog.getComment(), markerTimestamp));
		}
		
		return null;
	}

}
