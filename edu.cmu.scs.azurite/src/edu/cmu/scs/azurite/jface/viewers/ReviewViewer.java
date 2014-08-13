package edu.cmu.scs.azurite.jface.viewers;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import edu.cmu.scs.azurite.compare.AzuriteCompareInput;
import edu.cmu.scs.azurite.compare.SimpleCompareItem;
import edu.cmu.scs.azurite.plugin.Activator;

public class ReviewViewer extends Composite {
	
	private CompareViewerSwitchingPane mCompareViewerSwitchingPane;
	private CompareConfiguration mConfiguration;
	
	private String mTitle;
	
	private IViewPart mParentViewPart;
	
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
		
		SimpleCompareItem left = new SimpleCompareItem("Before", "Left", false);
		SimpleCompareItem right = new SimpleCompareItem("After", "Right", false);
		mCompareViewerSwitchingPane.setInput(new AzuriteCompareInput(left, right));
		
		mCompareViewerSwitchingPane.redraw();
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
							// TODO: Implement
						}
				});
		mPrevAction.setId("historyDiffPrev");
		mPrevAction.setMode(ActionContributionItem.MODE_FORCE_TEXT);
		
		mNextAction = new ActionContributionItem(
				new Action("Next", Activator.getImageDescriptor("icons/old_go_next.png")) {
						@Override
						public void run() {
							// TODO: Implement
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
