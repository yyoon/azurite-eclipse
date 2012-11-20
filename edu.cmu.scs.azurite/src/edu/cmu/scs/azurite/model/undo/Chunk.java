package edu.cmu.scs.azurite.model.undo;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
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
		List<RuntimeDC> involvedDocChanges = getInvolvedChanges();
		
		// Iterate through the docChanges,
		// and see if there are any conflicts outside of this chunk.
		for (RuntimeDC docChange : involvedDocChanges) {
			List<RuntimeDC> conflicts =
					docChange.getConflicts();
			
			for (RuntimeDC conflict : conflicts) {
				if (!involvedDocChanges.contains(conflict)) {
					return true;
				}
			}
		}
		
		// If nothing is found.
		return false;
	}
	
	public List<RuntimeDC> getInvolvedChanges() {
		TreeSet<RuntimeDC> set = new TreeSet<RuntimeDC>(
				RuntimeDC.getCommandIDComparator());

		for (Segment segment : this) {
			set.add(segment.getOwner());
		}

		return new ArrayList<RuntimeDC>(set);
	}
}
