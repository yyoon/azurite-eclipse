package edu.cmu.scs.azurite.model.undo;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.IDocument;

import edu.cmu.scs.azurite.model.FileKey;

public class SelectiveUndoParams {

	private List<Chunk> mChunks;
	private IDocument mDocument;
	private FileKey mFileKey;
	private Map<Chunk, UndoAlternative> mAlternativeChoices;
	
	public SelectiveUndoParams(List<Chunk> chunks, IDocument doc, Map<Chunk, UndoAlternative> alternativeChoices) {
		if (chunks == null || chunks.isEmpty() || doc == null) {
			throw new IllegalArgumentException();
		}
		
		mChunks = Collections.unmodifiableList(chunks);
		mDocument = doc;
		mFileKey = chunks.get(0).getBelongsTo();
		mAlternativeChoices = Collections.unmodifiableMap(alternativeChoices);
	}
	
	public List<Chunk> getChunks() {
		return mChunks;
	}
	
	public IDocument getDocument() {
		return mDocument;
	}
	
	public FileKey getFileKey() {
		return mFileKey;
	}
	
	public Map<Chunk, UndoAlternative> getAlternativeChoices() {
		return mAlternativeChoices;
	}
	
}
