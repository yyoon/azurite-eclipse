package edu.cmu.scs.azurite.views;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.compare.AzuriteCompareLabelProvider;
import edu.cmu.scs.azurite.jface.viewers.CodeHistoryDiffViewer;
import edu.cmu.scs.azurite.model.FileKey;

public class CodeHistoryDiffViewPart extends ViewPart {
	
	// NOTE: Whenever changing this max value,
	// also make sure that all the placeholders for the subviews are in place within plugin.xml.
	// See org.eclipse.ui.perspectiveExtensions extension point.
	private static final int MAX_VIEWS = 10;
	
	private static final Deque<Integer> availableViewerIds;
	
	static {
		availableViewerIds = new LinkedList<Integer>();
		for (int i = 0; i < MAX_VIEWS; ++i) {
			availableViewerIds.addLast(i);
		}
	}
	
	private static Map<Integer, CodeHistoryDiffViewPart> mes =
			new HashMap<Integer, CodeHistoryDiffViewPart>();
	
	private int viewerId = -1;
	
	private CodeHistoryDiffViewer viewer;
	
	private CompareConfiguration mConfiguration;
	
	public static Collection<CodeHistoryDiffViewPart> getInstances() {
		return Collections.unmodifiableCollection(mes.values());
	}
	
	public static Integer getNextViewerId() {
		return availableViewerIds.peekFirst();
	}

	@Override
	public void createPartControl(Composite parent) {
		this.viewerId = getNextViewerId();
		mes.put(this.viewerId, this);
		
		availableViewerIds.removeFirst();
		
		viewer = new CodeHistoryDiffViewer(parent, SWT.NONE);
		
		mConfiguration = createConfiguration();
	}

	@Override
	public void setFocus() {
		viewer.setFocus();
	}

	@Override
	public void dispose() {
		mes.remove(this.viewerId);
		
		availableViewerIds.addLast(this.viewerId);
		
		super.dispose();
	}

	private CompareConfiguration createConfiguration() {
		CompareConfiguration configuration = new CompareConfiguration();
		configuration
				.setDefaultLabelProvider(new AzuriteCompareLabelProvider());
		return configuration;
	}
	
	public void addCodeHistoryDiffViewer(String fileName, String fileContent,
			int selectionStart, int selectionEnd, List<RuntimeDC> involvedDCs, FileKey key) {
		addCodeHistoryDiffViewer(fileName, fileContent, selectionStart,
				selectionEnd, involvedDCs, -1, -1, key);
	}

	public void addCodeHistoryDiffViewer(String fileName, String fileContent,
			int selectionStart, int selectionEnd, List<RuntimeDC> involvedDCs,
			int startLine, int endLine, FileKey key) {
		String title = fileName;
		if (startLine != -1 && endLine != -1) {
			title += ":" + startLine + "-" + endLine;
		}
		
		viewer.setParameters(this, mConfiguration, title, fileContent, selectionStart, selectionEnd, involvedDCs, key);
		viewer.create();
		viewer.setFocus();
	}
	
	public void selectVersionWithAbsTimestamp(long absTimestamp) {
		viewer.selectVersionWithAbsTimestamp(absTimestamp);
	}

}
