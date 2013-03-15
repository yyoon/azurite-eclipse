package edu.cmu.scs.azurite.jface.widgets;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.commands.runtime.Segment;
import edu.cmu.scs.azurite.compare.CodeHistoryCompareInput;
import edu.cmu.scs.azurite.compare.SimpleCompareItem;
import edu.cmu.scs.azurite.model.undo.Chunk;
import edu.cmu.scs.azurite.model.undo.SelectiveUndoEngine;
import edu.cmu.scs.azurite.plugin.Activator;
import edu.cmu.scs.azurite.views.TimelineViewPart;
import edu.cmu.scs.fluorite.commands.ICommand;

public class CodeHistoryDiffViewer extends Composite {
	
	private CompareViewerSwitchingPane mCompareViewerSwitchingPane;
	private CompareConfiguration mConfiguration;
	
	private List<RuntimeDC> mInvolvedDCs;
	
	private SimpleCompareItem mCurrentItem;
	private Map<Integer, SimpleCompareItem> mHistoryItems;
	
	private String mFileContent;
	private String mSelectionText;
	private int mSelectionStart;
	private int mSelectionEnd;
	private int mSelectionLength;
	
	private int mCurrentVersion;
	
	private IAction mPrevAction;
	private IAction mNextAction;

	public CodeHistoryDiffViewer(Composite parent, int style) {
		super(parent, style);
		
		setLayout(new GridLayout());
	}

	public void setParameters(CompareConfiguration configuration,
			String fileContent, int selectionStart, int selectionEnd,
			List<RuntimeDC> involvedDCs) {
		if (involvedDCs == null || involvedDCs.isEmpty()) {
			throw new IllegalArgumentException("No history!");
		}
		
		mConfiguration = configuration;
		
		mInvolvedDCs = new ArrayList<RuntimeDC>();
		mInvolvedDCs.addAll(involvedDCs);
		Collections.sort(mInvolvedDCs, RuntimeDC.getCommandIDComparator());
		
		mFileContent = fileContent;
		mSelectionStart = selectionStart;
		mSelectionEnd = selectionEnd;
		mSelectionLength = mSelectionEnd - mSelectionStart;
		
		mSelectionText = fileContent.substring(selectionStart, selectionEnd);
		
		mCurrentItem = new SimpleCompareItem("[" + mInvolvedDCs.size()
				+ "] Current Version", mSelectionText, false);
		mHistoryItems = new HashMap<Integer, SimpleCompareItem>();
	}
	
	public void create() {
		createActions();
		mCompareViewerSwitchingPane = createCompareView(this);
		
		// Set the current version to the recent one.
		selectVersion(mInvolvedDCs.size());
	}
	
	private void createActions() {
		mPrevAction = new Action("Previous Version", Activator.getImageDescriptor("icons/old_go_previous.png")) {
			@Override
			public void run() {
				selectVersion(Math.max(getCurrentVersion() - 1, 0));
			}
		};
		
		mNextAction = new Action("Next Version", Activator.getImageDescriptor("icons/old_go_next.png")) {
			@Override
			public void run() {
				selectVersion(Math.min(getCurrentVersion() + 1, mInvolvedDCs.size()));
			}
		};
	}

	private CompareViewerSwitchingPane createCompareView(Composite parent) {
		CompareViewerSwitchingPane compareView = new CompareViewerSwitchingPane(parent, SWT.BORDER | SWT.FLAT) {
			@Override
			protected Viewer getViewer(Viewer oldViewer, Object input) {
				Viewer v = CompareUI.findContentViewer(oldViewer, input, this, mConfiguration);
				v.getControl().setData(CompareUI.COMPARE_VIEWER_TITLE, "Code History Diff");
				
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
					if (items.length > 0) {
						tbm.insertBefore(items[0].getId(), new Separator(navGroupId));
					}
					else {
						tbm.add(new Separator(navGroupId));
					}
					
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
	
	private int getCurrentVersion() {
		return mCurrentVersion;
	}

	private void selectVersion(int version) {
		if (mCompareViewerSwitchingPane == null) {
			throw new IllegalStateException();
		}
		
		if (getCurrentVersion() == version) {
			return;
		}
		
		SimpleCompareItem leftItem = getCompareItemOfVersion(version);
		
		mCompareViewerSwitchingPane.setInput(new CodeHistoryCompareInput(
				leftItem, mCurrentItem));
		
		mCurrentVersion = version;
		
		if (TimelineViewPart.getInstance() != null) {
			if (version == mInvolvedDCs.size()) {
				RuntimeDC dc = mInvolvedDCs.get(mInvolvedDCs.size() - 1);
				TimelineViewPart.getInstance().showMarker(
						dc.getOriginal().getSessionId() + dc.getOriginal().getTimestamp2() + 1);
			}
			else {
				RuntimeDC dc = mInvolvedDCs.get(version);
				TimelineViewPart.getInstance().showMarker(
						dc.getOriginal().getSessionId() + dc.getOriginal().getTimestamp());
			}
		}
	}
	
	public void selectVersionWithAbsTimestamp(long absTimestamp) {
		for (int i = 0; i < mInvolvedDCs.size(); ++i) {
			long t = mInvolvedDCs.get(i).getOriginal().getSessionId()
					+ mInvolvedDCs.get(i).getOriginal().getTimestamp();
			
			if (absTimestamp <= t) {
				selectVersion(i);
				return;
			}
		}
		
		selectVersion(mInvolvedDCs.size());
	}
	
	private SimpleCompareItem getCompareItemOfVersion(int version) {
		if (version < 0 || version > mInvolvedDCs.size()) {
			throw new IllegalArgumentException("Version out of range!");
		}
		
		if (version == mInvolvedDCs.size()) {
			return mCurrentItem;
		}
		else if (mHistoryItems.containsKey(version)) {
			return mHistoryItems.get(version);
		}
		
		// Get the previous versions by performing undo.
		List<RuntimeDC> subList = mInvolvedDCs.subList(version, mInvolvedDCs.size());
		Chunk chunk = new Chunk();
		for (RuntimeDC dc : subList) {
			for (Segment segment : dc.getAllSegments()) {
				if (segment.isDeletion()) {
					if (mSelectionStart < segment.getOffset() && segment.getOffset() < mSelectionEnd) {
						chunk.add(segment);
					}
				}
				else {
					if (segment.getOffset() < mSelectionEnd && segment.getEndOffset() > mSelectionStart) {
						chunk.add(segment);
					}
				}
			}
		}
		Collections.sort(chunk, Segment.getLocationComparator());
		
		int startOffset = chunk.getStartOffset();
		int endOffset = chunk.getEndOffset();
		String initialContent = mFileContent.substring(startOffset, endOffset);
		
		String undoResult = SelectiveUndoEngine.getInstance()
				.doSelectiveUndoChunkWithoutConflicts(chunk, initialContent);
		
		StringBuffer historyContent = new StringBuffer(mSelectionText);
		historyContent.replace(
				Math.max(startOffset - mSelectionStart, 0),
				Math.min(endOffset - mSelectionStart, mSelectionLength),
				undoResult);
		
		ICommand originalDC = mInvolvedDCs.get(version).getOriginal();
		Date date = new Date(originalDC.getSessionId() + originalDC.getTimestamp());
		String dateString = DateFormat.getDateTimeInstance().format(date);
		SimpleCompareItem historyItem = new SimpleCompareItem("[" + version
				+ "/" + mInvolvedDCs.size() + "] " + dateString + " (id:"
				+ originalDC.getCommandIndex() + ")",
				historyContent.toString(), false);
		
		// Add to the cache.
		mHistoryItems.put(version, historyItem);
		
		return historyItem;
	}

}
