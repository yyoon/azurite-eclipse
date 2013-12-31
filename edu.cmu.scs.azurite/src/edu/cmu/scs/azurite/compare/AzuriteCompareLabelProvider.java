package edu.cmu.scs.azurite.compare;

import org.eclipse.compare.ICompareInputLabelProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

public class AzuriteCompareLabelProvider implements
		ICompareInputLabelProvider {

	@Override
	public void addListener(ILabelProviderListener listener) {
		// Do nothing
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		// Do nothing
	}

	@Override
	public Image getImage(Object element) {
		return null;
	}

	@Override
	public String getText(Object element) {
		return "Azurite Compare";
	}

	@Override
	public void dispose() {
		// Do nothing, as this class doesn't allocate any resource.
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return true;
	}

	@Override
	public String getAncestorLabel(Object input) {
		if (input instanceof ICompareInput) {
			ITypedElement ancestor = ((ICompareInput)input).getAncestor();
			return ancestor == null ? null : ancestor.getName();
		}
		
		return null;
	}

	@Override
	public Image getAncestorImage(Object input) {
		if (input instanceof ICompareInput) {
			ITypedElement ancestor = ((ICompareInput)input).getAncestor();
			return ancestor == null ? null : ancestor.getImage();
		}
		
		return null;
	}

	@Override
	public String getLeftLabel(Object input) {
		if (input instanceof ICompareInput) {
			ITypedElement left = ((ICompareInput)input).getLeft();
			return left == null ? null : left.getName();
		}
		
		return null;
	}

	@Override
	public Image getLeftImage(Object input) {
		if (input instanceof ICompareInput) {
			ITypedElement left = ((ICompareInput)input).getLeft();
			return left == null ? null : left.getImage();
		}
		
		return null;
	}

	@Override
	public String getRightLabel(Object input) {
		if (input instanceof ICompareInput) {
			ITypedElement right = ((ICompareInput)input).getRight();
			return right == null ? null : right.getName();
		}
		
		return null;
	}

	@Override
	public Image getRightImage(Object input) {
		if (input instanceof ICompareInput) {
			ITypedElement right = ((ICompareInput)input).getRight();
			return right == null ? null : right.getImage();
		}
		
		return null;
	}

}
