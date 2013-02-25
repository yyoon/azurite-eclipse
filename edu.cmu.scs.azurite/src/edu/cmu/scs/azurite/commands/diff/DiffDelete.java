package edu.cmu.scs.azurite.commands.diff;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.text.IDocument;

import edu.cmu.scs.azurite.model.FileKey;
import edu.cmu.scs.fluorite.commands.Delete;

public class DiffDelete extends Delete implements IDiffDC {
	
	private FileKey mFileKey;
	
	public DiffDelete(FileKey fileKey) {
		super();
		mFileKey = fileKey;
	}
	
	public DiffDelete(FileKey fileKey, int offset, int length, int startLine,
			int endLine, String text, IDocument document) {
		super(offset, length, startLine, endLine, text, document);
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
