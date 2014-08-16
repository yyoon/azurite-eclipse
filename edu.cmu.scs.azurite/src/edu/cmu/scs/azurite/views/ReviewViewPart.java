package edu.cmu.scs.azurite.views;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import edu.cmu.scs.azurite.compare.AzuriteCompareLabelProvider;
import edu.cmu.scs.azurite.jface.viewers.ReviewViewer;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;

public class ReviewViewPart extends ViewPart {
	
	private static ReviewViewPart me = null;
	
	private ReviewViewer mViewer;
	
	private CompareConfiguration mConfiguration;
	
	public static ReviewViewPart getInstance() {
		return me;
	}

	@Override
	public void createPartControl(Composite parent) {
		me = this;
		
		mViewer = new ReviewViewer(parent, SWT.NONE);
		mConfiguration = createConfiguration();
		mViewer.setParameters(this, mConfiguration);
		mViewer.create();
		
	}

	@Override
	public void setFocus() {
		mViewer.setFocus();
	}

	@Override
	public void dispose() {
		me = null;
		
		super.dispose();
	}

	private CompareConfiguration createConfiguration() {
		CompareConfiguration configuration = new CompareConfiguration();
		configuration.setDefaultLabelProvider(new AzuriteCompareLabelProvider());
		return configuration;
	}
	
	public void addReviewViewer() {
		int historySize = RuntimeHistoryManager.getInstance().getEntireHistory().size();
		mViewer.selectVersion(historySize - 1, historySize);
		
		mViewer.setFocus();
	}

}
