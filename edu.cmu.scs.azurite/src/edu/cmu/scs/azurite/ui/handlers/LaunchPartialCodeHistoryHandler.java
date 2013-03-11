package edu.cmu.scs.azurite.ui.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.jface.dialogs.PartialCodeHistoryDialog;
import edu.cmu.scs.azurite.views.PartialCodeHistoryViewPart;
import edu.cmu.scs.fluorite.model.EventRecorder;
import edu.cmu.scs.fluorite.util.Utilities;

public class LaunchPartialCodeHistoryHandler extends AbstractHandler {
	
	private static final String PARTIAL_CODE_HISTORY_VIEW_ID =
			"edu.cmu.scs.azurite.views.PartialCodeHistoryViewPart";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Shell parentShell = Display.getDefault().getActiveShell();
		
		List<RuntimeDC> dcs = HandlerUtilities.getOperationsInSelectedRegion();
		if (dcs == null || dcs.isEmpty()) {
			return null;
		}
		
		IEditorPart editor = EventRecorder.getInstance().getEditor();
		if (editor == null) {
			return null;
		}
		
		IDocument doc = Utilities.getDocument(editor);
		if (doc == null) { 
			return null;
		}
		
		String fileContent = doc.get();
		
		ITextSelection selection = HandlerUtilities.getSelectedRegion();
		
/*		PartialCodeHistoryDialog historyDialog = new PartialCodeHistoryDialog(
				parentShell, dcs, fileContent, selection.getOffset(),
				selection.getOffset() + selection.getLength());
		historyDialog.create();
		historyDialog.open();
*/		
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				try {
					IViewPart viewPart = page.showView(PARTIAL_CODE_HISTORY_VIEW_ID);
					if (viewPart instanceof PartialCodeHistoryViewPart) {
						PartialCodeHistoryViewPart pchView =
								(PartialCodeHistoryViewPart) viewPart;
						
						String fileName = editor.getEditorInput().getName();
						
						pchView.addPartialHistoryViewer(
								fileName,
								fileContent,
								selection.getOffset(),
								selection.getOffset() + selection.getLength(),
								dcs,
								selection.getStartLine(),
								selection.getEndLine());
					}
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			}
		}

		return null;
	}

}
