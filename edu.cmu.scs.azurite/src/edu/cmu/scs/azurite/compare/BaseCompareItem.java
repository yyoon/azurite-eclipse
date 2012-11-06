package edu.cmu.scs.azurite.compare;

import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IModificationDate;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.swt.graphics.Image;

public abstract class BaseCompareItem implements IStreamContentAccessor, ITypedElement,
		IModificationDate, IEditableContent {
	private String name;
	private long time;
	private boolean editable;

	BaseCompareItem(String name, boolean editable) {
		this(name, 0, editable);
	}

	BaseCompareItem(String name, long time, boolean editable) {
		this.name = name;
		this.time = time;
		this.editable = editable;
	}

	public Image getImage() {
		return null;
	}

	public long getModificationDate() {
		return time;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return "timelinedJava";
	}

	public boolean isEditable() {
		return editable;
	}

	public void setContent(byte[] newContent) {
		// TODO Auto-generated method stub
		
	}

	public ITypedElement replace(ITypedElement dest, ITypedElement src) {
		// TODO Auto-generated method stub
		return null;
	}
}
