package edu.cmu.scs.azurite.compare;

import org.eclipse.compare.ICompareInputLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

public class CodeHistoryCompareLabelProvider implements
		ICompareInputLabelProvider {

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public Image getImage(Object element) {
		return null;
	}

	@Override
	public String getText(Object element) {
		return "Partial Code History Compare";
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return true;
	}

	@Override
	public String getAncestorLabel(Object input) {
		return null;
	}

	@Override
	public Image getAncestorImage(Object input) {
		return null;
	}

	@Override
	public String getLeftLabel(Object input) {
		if (input instanceof CodeHistoryCompareInput) {
			return ((CodeHistoryCompareInput)input).getLeft().getName();
		}
		
		return null;
	}

	@Override
	public Image getLeftImage(Object input) {
		if (input instanceof CodeHistoryCompareInput) {
			return ((CodeHistoryCompareInput)input).getLeft().getImage();
		}
		
		return null;
	}

	@Override
	public String getRightLabel(Object input) {
		if (input instanceof CodeHistoryCompareInput) {
			return ((CodeHistoryCompareInput)input).getRight().getName();
		}
		
		return null;
	}

	@Override
	public Image getRightImage(Object input) {
		if (input instanceof CodeHistoryCompareInput) {
			return ((CodeHistoryCompareInput)input).getRight().getImage();
		}
		
		return null;
	}

}
