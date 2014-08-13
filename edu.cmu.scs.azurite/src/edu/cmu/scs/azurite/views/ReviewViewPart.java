package edu.cmu.scs.azurite.views;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import edu.cmu.scs.azurite.compare.AzuriteCompareLabelProvider;
import edu.cmu.scs.azurite.jface.viewers.ReviewViewer;

public class ReviewViewPart extends ViewPart {
	
	private static ReviewViewPart me = null;
	
	private ReviewViewer viewer;
	
	private CompareConfiguration mConfiguration;
	
	public static ReviewViewPart getInstance() {
		return me;
	}

	@Override
	public void createPartControl(Composite parent) {
		me = this;
		
		viewer = new ReviewViewer(parent, SWT.NONE);
		
		mConfiguration = createConfiguration();
	}

	@Override
	public void setFocus() {
		viewer.setFocus();
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
		viewer.setParameters(this, mConfiguration);
		viewer.create();
		viewer.setFocus();
	}

}
