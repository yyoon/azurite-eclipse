package edu.cmu.scs.azurite.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class CompareInput extends CompareEditorInput {
	public CompareInput(BaseCompareItem left, BaseCompareItem right) {
		super(createConfiguration());
		
		mLeft = left;
		mRight = right;
	}
	
	private BaseCompareItem mLeft, mRight;
	
	private static CompareInput mLastInput = null;
	
	private static CompareConfiguration createConfiguration() {
		CompareConfiguration configuration = new CompareConfiguration();
		configuration.setLeftEditable(true);
		configuration.setRightEditable(false);
		
		return configuration;
	}
	
	public static void setLastCompareInput(CompareInput input) {
		mLastInput = input;
	}
	
	public static CompareInput getLastCompareInput() {
		return mLastInput;
	}

	protected Object prepareInput(IProgressMonitor pm) {
		return new DiffNode(Differencer.CHANGE, null, mLeft, mRight);
	}

	@Override
	public void saveChanges(IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		super.saveChanges(monitor);
	}
	
	public BaseCompareItem getLeftItem() {
		return mLeft;
	}
	
	public BaseCompareItem getRightItem() {
		return mRight;
	}
}
