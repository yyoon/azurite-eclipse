package edu.cmu.scs.azurite.jface.dialogs;

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
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.commands.runtime.Segment;
import edu.cmu.scs.azurite.compare.PartialCodeCompareInput;
import edu.cmu.scs.azurite.compare.PartialCodeCompareLabelProvider;
import edu.cmu.scs.azurite.compare.SimpleCompareItem;
import edu.cmu.scs.azurite.model.undo.Chunk;
import edu.cmu.scs.azurite.model.undo.SelectiveUndoEngine;
import edu.cmu.scs.fluorite.commands.ICommand;

public class PartialCodeHistoryDialog extends TitleAreaDialog {
	
	private static final int MINIMUM_COMPARE_WIDTH = 700;
	private static final int MINIMUM_COMPARE_HEIGHT = 400;
	
	private static final int MARGIN_WIDTH = 10;
	private static final int MARGIN_HEIGHT = 10;
	
	private static final int SPACING = 10;

	private static final String TEXT = "Azurite - Partial Code History";
	private static final String TITLE = "Partial Code History View";
	private static final String DEFAULT_MESSAGE = "History of the selected code.";
	
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

	public PartialCodeHistoryDialog(Shell parent, List<RuntimeDC> involvedDCs,
			String fileContent, int selectionStart, int selectionEnd) {
		super(parent);
		
		if (involvedDCs == null || involvedDCs.isEmpty()) {
			throw new IllegalArgumentException("No history!");
		}
		
		mConfiguration = createConfiguration();
		
		mInvolvedDCs = new ArrayList<RuntimeDC>();
		mInvolvedDCs.addAll(involvedDCs);
		
		mFileContent = fileContent;
		mSelectionStart = selectionStart;
		mSelectionEnd = selectionEnd;
		mSelectionLength = mSelectionEnd - mSelectionStart;
		
		mSelectionText = fileContent.substring(selectionStart, selectionEnd);
		
		mCurrentItem = new SimpleCompareItem("[" + mInvolvedDCs.size()
				+ "] Current Version", mSelectionText, false);
		mHistoryItems = new HashMap<Integer, SimpleCompareItem>();
		
		setHelpAvailable(false);
	}

	private CompareConfiguration createConfiguration() {
		CompareConfiguration configuration = new CompareConfiguration();
		configuration
				.setDefaultLabelProvider(new PartialCodeCompareLabelProvider());
		return configuration;
	}

	@Override
	public void create() {
		super.create();
		
		getShell().setText(TEXT);
		
		setTitle(TITLE);
		setMessage(DEFAULT_MESSAGE);
		
		// Set the current version to the recent one.
		selectVersion(mInvolvedDCs.size());
	}

	private void selectVersion(int version) {
		if (mCompareViewerSwitchingPane == null) {
			throw new IllegalStateException();
		}
		
		SimpleCompareItem leftItem = getCompareItemOfVersion(version);
		
		mCompareViewerSwitchingPane.setInput(new PartialCodeCompareInput(
				leftItem, mCurrentItem));
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
			chunk.addAll(dc.getAllSegments());
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
				+ "] " + dateString + " (id:" + originalDC.getCommandIndex()
				+ ")", historyContent.toString(), false);
		
		// Add to the cache.
		mHistoryItems.put(version, historyItem);
		
		return historyItem;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = createMainArea(parent);
		
		mCompareViewerSwitchingPane = createCompareView(composite);
		
		createScale(composite);
		
		createTickLabels(composite);
		
		return composite;
	}

	private Control createScale(Composite parent) {
		final Scale scale = new Scale(parent, SWT.HORIZONTAL);
		scale.setMinimum(0);
		scale.setMaximum(mInvolvedDCs.size());
		scale.setIncrement(1);
		scale.setPageIncrement(1);
		scale.setSelection(mInvolvedDCs.size());
		scale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		scale.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectVersion(scale.getSelection());
			}
		});
		
		return scale;
	}

	private Control createTickLabels(Composite parent) {
		Composite labelArea = new Composite(parent, SWT.NONE);
		labelArea.setLayout(new GridLayout(3, false));
		labelArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Label leftLabel = new Label(labelArea, SWT.NONE);
		leftLabel.setText("Oldest");
		leftLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		
		Label rightLabel = new Label(labelArea, SWT.NONE);
		rightLabel.setText("Current");
		rightLabel.setAlignment(SWT.RIGHT);
		rightLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		return labelArea;
	}

	private CompareViewerSwitchingPane createCompareView(Composite parent) {
		CompareViewerSwitchingPane compareView = new CompareViewerSwitchingPane(parent, SWT.BORDER | SWT.FLAT) {
			@Override
			protected Viewer getViewer(Viewer oldViewer, Object input) {
				Viewer v = CompareUI.findContentViewer(oldViewer, input, this, mConfiguration);
				v.getControl().setData(CompareUI.COMPARE_VIEWER_TITLE, "Partial Code History Compare");
				return v;
			}
		};
		
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.minimumWidth = MINIMUM_COMPARE_WIDTH;
		gridData.minimumHeight = MINIMUM_COMPARE_HEIGHT;
		compareView.setLayoutData(gridData);
		
		return compareView;
	}

	private Composite createMainArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		
		// Use GridLayout.
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginWidth = MARGIN_WIDTH;
		gridLayout.marginHeight = 0;
		gridLayout.marginTop = MARGIN_HEIGHT;
		gridLayout.horizontalSpacing = SPACING;
		gridLayout.verticalSpacing = SPACING;
		composite.setLayout(gridLayout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return composite;
	}
	
	@Override
	protected void cancelPressed() {
		super.cancelPressed();
	}

	@Override
	protected void okPressed() {
		super.okPressed();
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

}
