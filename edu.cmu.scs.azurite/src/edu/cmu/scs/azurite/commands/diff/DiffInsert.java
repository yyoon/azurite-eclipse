package edu.cmu.scs.azurite.commands.diff;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.text.IDocument;

import edu.cmu.scs.azurite.model.FileKey;
import edu.cmu.scs.fluorite.commands.document.Insert;

public class DiffInsert extends Insert implements IDiffDC {
	
	private FileKey mFileKey;
	
	public DiffInsert(FileKey fileKey) {
		super();
		mFileKey = fileKey;
	}
	
	public DiffInsert(FileKey fileKey, int offset, String text, IDocument doc) {
		super(offset, text, doc);
		mFileKey = fileKey;
	}
	
	public void setDocLength(int docLength) {
		Map<String, Integer> numericalValues = new HashMap<String, Integer>();
		numericalValues.put("docLength", docLength);
		
		setNumericalValues(numericalValues);
	}

	@Override
	public FileKey getFileKey() {
		return mFileKey;
	}

}
