package edu.cmu.scs.azurite.model.undo;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import edu.cmu.scs.azurite.commands.runtime.BaseRuntimeDocumentChange;
import edu.cmu.scs.azurite.commands.runtime.Segment;

@SuppressWarnings("serial")
public class Chunk extends ArrayList<Segment> {
	
	public int getStartOffset() {
		if (isEmpty()) {
			throw new IllegalStateException();
		}
		
		return get(0).getOffset();
	}
	
	public int getEndOffset() {
		if (isEmpty()) {
			throw new IllegalStateException();
		}
		
		Segment last = get(size() - 1);
		return last.getEffectiveEndOffset();
	}
	
	public int getChunkLength() {
		return getEndOffset() - getStartOffset();
	}
	
	public boolean hasConflictOutsideThisChunk() {
		// Determine all the runtime docChanges.
		List<BaseRuntimeDocumentChange> involvedDocChanges = getInvolvedChanges();
		
		// Iterate through the docChanges,
		// and see if there are any conflicts outside of this chunk.
		for (BaseRuntimeDocumentChange docChange : involvedDocChanges) {
			List<BaseRuntimeDocumentChange> conflicts =
					docChange.getConflicts();
			
			for (BaseRuntimeDocumentChange conflict : conflicts) {
				if (!involvedDocChanges.contains(conflict)) {
					return true;
				}
			}
		}
		
		// If nothing is found.
		return false;
	}
	
	public List<BaseRuntimeDocumentChange> getInvolvedChanges() {
		TreeSet<BaseRuntimeDocumentChange> set = new TreeSet<BaseRuntimeDocumentChange>(
				BaseRuntimeDocumentChange.getCommandIDComparator());

		for (Segment segment : this) {
			set.add(segment.getOwner());
		}

		return new ArrayList<BaseRuntimeDocumentChange>(set);
	}
}
