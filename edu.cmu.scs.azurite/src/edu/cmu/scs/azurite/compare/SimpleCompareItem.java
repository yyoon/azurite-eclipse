package edu.cmu.scs.azurite.compare;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;

public class SimpleCompareItem extends BaseCompareItem {
	
	public SimpleCompareItem(String name, String contents, long time, boolean editable) {
		super(name, time, editable);
		
		this.contents = contents;
	}

	public SimpleCompareItem(String name, String contents, boolean editable) {
		this(name, contents, 0, editable);
	}
	
	private String contents;

	public InputStream getContents() throws CoreException {
		return new ByteArrayInputStream(contents.getBytes());
	}

}
