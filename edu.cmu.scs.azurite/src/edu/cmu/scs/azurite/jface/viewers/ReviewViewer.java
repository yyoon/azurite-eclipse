package edu.cmu.scs.azurite.jface.viewers;

import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.compare.AzuriteCompareInput;
import edu.cmu.scs.azurite.compare.SimpleCompareItem;
import edu.cmu.scs.azurite.model.FileKey;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.azurite.plugin.Activator;
import edu.cmu.scs.azurite.util.Utilities;
import edu.cmu.scs.fluorite.commands.BaseDocumentChangeEvent;

public class ReviewViewer extends Composite {
	
	private CompareViewerSwitchingPane mCompareViewerSwitchingPane;
	private CompareConfiguration mConfiguration;
	
	private String mTitle;
	
	private IViewPart mParentViewPart;
	
	private int mVersionBegin;
	private int mVersionEnd;
	
	private ActionContributionItem mRevertAction;
	private ActionContributionItem mPrevAction;
	private ActionContributionItem mNextAction;

	public ReviewViewer(Composite parent, int style) {
		super(parent, style);
		
		setLayout(new GridLayout());
	}
	
	public void setParameters(IViewPart parentViewPart, CompareConfiguration configuration) {
		mParentViewPart = parentViewPart;
		mConfiguration = configuration;
	}
	
	public void create() {
		createActions();
		
		mCompareViewerSwitchingPane = createCompareView(this);
	}
	
	private void createActions() {
		mRevertAction = new ActionContributionItem(
				new Action("Revert", Activator.getImageDescriptor("icons/old_edit_undo.png")) {
						@Override
						public void run() {
							// TODO: Implement
						}
				});
		mRevertAction.setId("historyDiffRevert");
		mRevertAction.setMode(ActionContributionItem.MODE_FORCE_TEXT);
		
		mPrevAction = new ActionContributionItem(
				new Action("Prev", Activator.getImageDescriptor("icons/old_go_previous.png")) {
						@Override
						public void run() {
							if (mVersionBegin > 0) {
								selectVersion(mVersionBegin - 1, mVersionBegin);
							} else {
								MessageDialog.openInformation(
										getShell(),
										"Azurite - Review",
										"No more changes to be reviewed");
							}
						}
				});
		mPrevAction.setId("historyDiffPrev");
		mPrevAction.setMode(ActionContributionItem.MODE_FORCE_TEXT);
		
		mNextAction = new ActionContributionItem(
				new Action("Next", Activator.getImageDescriptor("icons/old_go_next.png")) {
						@Override
						public void run() {
							int historySize = RuntimeHistoryManager.getInstance().getEntireHistory().size();
							if (mVersionEnd < historySize) {
								selectVersion(mVersionEnd, mVersionEnd + 1);
							} else {
								MessageDialog.openInformation(
										getShell(),
										"Azurite - Review",
										"No more changes to be reviewed");
							}
						}
				});
		mNextAction.setId("historyDiffNext");
		mNextAction.setMode(ActionContributionItem.MODE_FORCE_TEXT);
	}

	private CompareViewerSwitchingPane createCompareView(Composite parent) {
		CompareViewerSwitchingPane compareView = new CompareViewerSwitchingPane(parent, SWT.BORDER | SWT.FLAT) {
			@Override
			protected Viewer getViewer(Viewer oldViewer, Object input) {
				Viewer v = CompareUI.findContentViewer(oldViewer, input, this, mConfiguration);
				v.getControl().setData(CompareUI.COMPARE_VIEWER_TITLE, mTitle);
				
				ToolBarManager tbm = CompareViewerSwitchingPane.getToolBarManager(this);
				String navGroupId = "historyDiffNav";
				
				// Check if the actions are added or not.
				boolean added = false;
				IContributionItem[] items = tbm.getItems();
				
				for (IContributionItem item : items) {
					if (navGroupId.equals(item.getId())) {
						added = true;
						break;
					}
				}
				
				if (!added) {
					tbm.removeAll();
					
					tbm.add(mRevertAction);
					tbm.add(new Separator(navGroupId));
					
					tbm.appendToGroup(navGroupId, mPrevAction);
					tbm.appendToGroup(navGroupId, mNextAction);
					
					tbm.update(true);
				}

				return v;
			}
		};
		
		compareView.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return compareView;
	}
	
	private int getVersionBegin() {
		return mVersionBegin;
	}
	
	private int getVersionEnd() {
		return mVersionEnd;
	}
	
	public void selectVersion(int versionBegin, int versionEnd) {
		if (mCompareViewerSwitchingPane == null) {
			throw new IllegalStateException();
		}
		
		RuntimeHistoryManager manager = RuntimeHistoryManager.getInstance();
		
		List<RuntimeDC> entireHistory = manager.getEntireHistory();
		if (versionBegin < 0 || versionEnd > entireHistory.size() || versionBegin >= versionEnd) {
			throw new IllegalArgumentException();
		}
		
		if (getVersionBegin() == versionBegin && getVersionEnd() == versionEnd) {
			return;
		}
		
		// Extract involved DCs, and the file key.
		List<RuntimeDC> involvedDCs = entireHistory.subList(versionBegin, versionEnd);
		int size = versionEnd - versionBegin;
		
		FileKey key = involvedDCs.get(0).getBelongsTo();
		for (int i = 1; i < size; ++i) {
			if (!key.equals(involvedDCs.get(i).getBelongsTo())) {
				throw new IllegalArgumentException();
			}
		}
		
		List<RuntimeDC> historyForKey = manager.getRuntimeDocumentChanges(key);
		int versionEndForKey = historyForKey.indexOf(entireHistory.get(versionEnd - 1)) + 1;
		int versionBeginForKey = versionEndForKey - size;
		
		// Jump to the file
		Utilities.openEditorWithKey(key);
		
		// TODO Select these operations.
		
		// Get the IDocument object for the file key.
		IDocument doc = Utilities.findDocumentForKey(key);
		
		// TODO Optimize this process with caching.
		StringBuilder fileContent = new StringBuilder(doc.get());
		
		// Get the "After" code
		for (int i = historyForKey.size() - 1; i >= versionEndForKey; --i) {
			BaseDocumentChangeEvent originalDC = historyForKey.get(i).getOriginal();
			originalDC.applyInverse(fileContent);
		}
		
		String after = fileContent.toString();
		
		// Get the "Before code
		for (int i = versionEndForKey - 1; i >= versionBeginForKey; --i) {
			BaseDocumentChangeEvent originalDC = historyForKey.get(i).getOriginal();
			originalDC.applyInverse(fileContent);
		}
		
		String before = fileContent.toString();
		
		SimpleCompareItem left = new SimpleCompareItem("Before", before, false);
		SimpleCompareItem right = new SimpleCompareItem("After", after, false);
		AzuriteCompareInput input = new AzuriteCompareInput(left, right);
		
		mCompareViewerSwitchingPane.setInput(input);
		mCompareViewerSwitchingPane.redraw();
		
		mVersionBegin = versionBegin;
		mVersionEnd = versionEnd;
	}

	public void closeParentView() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				page.hideView(mParentViewPart);
			}
		}
	}

}
