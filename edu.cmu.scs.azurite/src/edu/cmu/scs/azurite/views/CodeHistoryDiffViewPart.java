package edu.cmu.scs.azurite.views;

import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Listener;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.compare.CodeHistoryCompareLabelProvider;
import edu.cmu.scs.azurite.jface.widgets.CodeHistoryDiffViewer;

public class CodeHistoryDiffViewPart extends ViewPart {
	
	private CTabFolder folder;
	private int viewerCount;
	
	private CompareConfiguration mConfiguration;
	
	private static CodeHistoryDiffViewPart me = null;
	
	public static CodeHistoryDiffViewPart getInstance() {
		return me;
	}

	@Override
	public void createPartControl(Composite parent) {
		me = this;
		
		folder = new CTabFolder(parent, SWT.NONE);
		folder.setMinimizeVisible(false);
		folder.setMaximizeVisible(false);
		folder.setSimple(false);
		
		folder.addCTabFolder2Listener(new CTabFolder2Listener() {
			
			@Override
			public void showList(CTabFolderEvent event) {
			}
			
			@Override
			public void restore(CTabFolderEvent event) {
			}
			
			@Override
			public void minimize(CTabFolderEvent event) {
			}
			
			@Override
			public void maximize(CTabFolderEvent event) {
			}
			
			@Override
			public void close(CTabFolderEvent event) {
				if (folder.getItemCount() == 1) {
					hideMarker();
				}
			}
		});
		
		mConfiguration = createConfiguration();
		viewerCount = 0;
	}

	@Override
	public void setFocus() {
		folder.setFocus();
	}

	@Override
	public void dispose() {
		me = null;
		
		hideMarker();
		
		super.dispose();
	}

	private void hideMarker() {
		if (TimelineViewPart.getInstance() != null) {
			TimelineViewPart.getInstance().hideMarker();
		}
	}

	private CompareConfiguration createConfiguration() {
		CompareConfiguration configuration = new CompareConfiguration();
		configuration
				.setDefaultLabelProvider(new CodeHistoryCompareLabelProvider());
		return configuration;
	}
	
	public void addCodeHistoryDiffViewer(String fileName, String fileContent,
			int selectionStart, int selectionEnd, List<RuntimeDC> involvedDCs) {
		addCodeHistoryDiffViewer(fileName, fileContent, selectionStart,
				selectionEnd, involvedDCs, -1, -1);
	}

	public void addCodeHistoryDiffViewer(String fileName, String fileContent,
			int selectionStart, int selectionEnd, List<RuntimeDC> involvedDCs,
			int startLine, int endLine) {
		CTabItem tabItem = new CTabItem(folder, SWT.CLOSE);
		
		String title = "[" + (++viewerCount) + "] " + fileName;
		if (startLine != -1 && endLine != -1) {
			title += ":" + startLine + "-" + endLine;
		}
		
		tabItem.setText(title);
		
		CodeHistoryDiffViewer viewer =
				new CodeHistoryDiffViewer(folder, SWT.NONE);
		viewer.setParameters(mConfiguration, fileContent, selectionStart, selectionEnd, involvedDCs);
		viewer.create();
		
		tabItem.setControl(viewer);
		folder.setSelection(tabItem);
		folder.setFocus();
	}
	
	public void selectVersionWithAbsTimestamp(long absTimestamp) {
		for (CTabItem item : folder.getItems()) {
			CodeHistoryDiffViewer viewer = 
					(CodeHistoryDiffViewer)item.getControl();
			
			viewer.selectVersionWithAbsTimestamp(absTimestamp);
		}
	}

}
