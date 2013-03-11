package edu.cmu.scs.azurite.views;

import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.compare.PartialCodeCompareLabelProvider;
import edu.cmu.scs.azurite.jface.widgets.PartialCodeHistoryViewer;

public class PartialCodeHistoryViewPart extends ViewPart {
	
	private CTabFolder folder;
	private int viewerCount;
	
	private CompareConfiguration mConfiguration;
	
	private static PartialCodeHistoryViewPart me = null;
	
	public static PartialCodeHistoryViewPart getInstance() {
		return me;
	}

	@Override
	public void createPartControl(Composite parent) {
		me = this;
		
		folder = new CTabFolder(parent, SWT.NONE);
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
		
		super.dispose();
	}

	private CompareConfiguration createConfiguration() {
		CompareConfiguration configuration = new CompareConfiguration();
		configuration
				.setDefaultLabelProvider(new PartialCodeCompareLabelProvider());
		return configuration;
	}
	
	public void addPartialHistoryViewer(String fileName, String fileContent,
			int selectionStart, int selectionEnd, List<RuntimeDC> involvedDCs) {
		addPartialHistoryViewer(fileName, fileContent, selectionStart,
				selectionEnd, involvedDCs, -1, -1);
	}

	public void addPartialHistoryViewer(String fileName, String fileContent,
			int selectionStart, int selectionEnd, List<RuntimeDC> involvedDCs,
			int startLine, int endLine) {
		CTabItem tabItem = new CTabItem(folder, SWT.CLOSE);
		
		String title = "[" + (++viewerCount) + "] " + fileName;
		if (startLine != -1 && endLine != -1) {
			title += ":" + startLine + "-" + endLine;
		}
		
		tabItem.setText(title);
		
		PartialCodeHistoryViewer viewer =
				new PartialCodeHistoryViewer(folder, SWT.NONE);
		viewer.setParameters(mConfiguration, fileContent, selectionStart, selectionEnd, involvedDCs);
		viewer.create();
		
		tabItem.setControl(viewer);
		folder.setSelection(tabItem);
		folder.setFocus();
	}

}
