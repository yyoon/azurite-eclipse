package edu.cmu.scs.azurite.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.compare.CodeHistoryCompareLabelProvider;
import edu.cmu.scs.azurite.jface.widgets.CodeHistoryDiffViewer;
import edu.cmu.scs.azurite.model.FileKey;

public class CodeHistoryDiffViewPart extends ViewPart {
	
	private static int viewerId = 0;
	
	private static List<CodeHistoryDiffViewPart> mes =
			new ArrayList<CodeHistoryDiffViewPart>();
	
	private CodeHistoryDiffViewer viewer;
	
	private CompareConfiguration mConfiguration;
	
	public static List<CodeHistoryDiffViewPart> getInstances() {
		return Collections.unmodifiableList(mes);
	}
	
	public static int getViewerId() {
		return viewerId;
	}

	@Override
	public void createPartControl(Composite parent) {
		mes.add(this);
		
		++viewerId;
		
		viewer = new CodeHistoryDiffViewer(parent, SWT.NONE);
		
		mConfiguration = createConfiguration();
	}

	@Override
	public void setFocus() {
		viewer.setFocus();
	}

	@Override
	public void dispose() {
		mes.remove(this);

		if (mes.isEmpty()) {
			hideMarker();
		}
		
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
