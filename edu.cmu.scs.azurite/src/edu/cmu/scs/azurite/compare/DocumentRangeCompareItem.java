package edu.cmu.scs.azurite.compare;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.compare.contentmergeviewer.IDocumentRange;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

public class DocumentRangeCompareItem extends BaseCompareItem implements IDocumentRange {
	
	private IDocument mDocument;
	private Position mRange;

	public DocumentRangeCompareItem(String name, IDocument doc, int start, int length, boolean editable) {
		this(name, 0, doc, start, length, editable);
	}

	public DocumentRangeCompareItem(String name, long time, IDocument doc, int start, int length, boolean editable) {
		super(name, time, editable);
		
		mDocument = doc;
		registerPositionUpdater(start, length);
	}

	private void registerPositionUpdater(int start, int length) {
		mDocument.addPositionCategory(RANGE_CATEGORY);
		mRange= new Position(start, length);
		try {
			mDocument.addPosition(RANGE_CATEGORY, mRange);
		} catch (BadLocationException | BadPositionCategoryException e) {
			e.printStackTrace();
		}
	}

	@Override
	public InputStream getContents() throws CoreException {
		String result = null;
		try {
			result = mDocument.get(mRange.getOffset(), mRange.getLength());
		} catch (BadLocationException ex) {
			result = "";
		}
		
		return new ByteArrayInputStream(result.getBytes());
	}

	@Override
	public IDocument getDocument() {
		return mDocument;
	}

	@Override
	public Position getRange() {
		return mRange;
	}

}
