package edu.cmu.scs.azurite.model.undo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.commands.runtime.Segment;

@SuppressWarnings("serial")
public class Chunk extends ArrayList<Segment> {
	
	public Chunk() {
		super();
	}
	
	public Chunk(Collection<? extends Segment> c) {
		super(c);
	}
	
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
		List<RuntimeDC> involvedDCs = getInvolvedChanges();
		
		// Iterate through the docChanges,
		// and see if there are any conflicts outside of this chunk.
		for (RuntimeDC dc : involvedDCs) {
			List<RuntimeDC> conflicts =
					dc.getConflicts();
			
			for (RuntimeDC conflict : conflicts) {
				if (!involvedDCs.contains(conflict)) {
					return true;
				}
			}
		}
		
		// If nothing is found.
		return false;
	}
	
	public Chunk getExpandedChunkInRange() {
		Chunk result = new Chunk();
		
		// Flood fill.
		Queue<RuntimeDC> queue = new LinkedList<RuntimeDC>(getInvolvedChanges());
		Set<RuntimeDC> set = new HashSet<RuntimeDC>();
		
		while (!queue.isEmpty()) {
			RuntimeDC dc = queue.remove();
			
			if (isInRange(dc, getStartOffset(), getEndOffset())) {
				set.add(dc);
				result.addAll(dc.getAllSegments());
				
				for (RuntimeDC conflict : dc.getConflicts()) {
					if (!set.contains(conflict)) {
						queue.add(conflict);
					}
				}
			}
		}
		
		Collections.sort(result, Segment.getLocationComparator());
		return result;
	}
	
	private boolean isInRange(RuntimeDC runtimeDC, int startOffset, int endOffset) {
		if (runtimeDC == null) {
			throw new IllegalArgumentException();
		}
		
		List<Segment> segments = runtimeDC.getAllSegments();
		
		// Consider it's not in range if the segments list is empty.
		if (segments.isEmpty()) {
			return false;
		}
		
		Segment first = segments.get(0);
		Segment last = segments.get(segments.size() - 1);
		
		return startOffset <= first.getOffset() && last.getEffectiveEndOffset() <= endOffset;
	}
	
	public List<RuntimeDC> getInvolvedChanges() {
		TreeSet<RuntimeDC> set = new TreeSet<RuntimeDC>(
				RuntimeDC.getCommandIDComparator());

		for (Segment segment : this) {
			set.add(segment.getOwner());
		}

		return Collections.unmodifiableList(new ArrayList<RuntimeDC>(set));
	}
	
	public Chunk copyChunk() {
		Chunk copyChunk = new Chunk();
		for (Segment originalSegment : this) {
			copyChunk.add(originalSegment.copySegment());
		}

		// Reconstruct the "segmentsClosedByMe" list
		for (int i = 0; i < this.size(); ++i) {
			Segment originalSegment = this.get(i);
			Segment copySegment = copyChunk.get(i);
			
			for (Segment closedSegment : originalSegment.getSegmentsClosedByMe()) {
				int closedSegmentIndex = this.indexOf(closedSegment);
				if (closedSegmentIndex != -1) {
					copySegment.addSegmentClosedByMe(copyChunk.get(closedSegmentIndex));
				}
			}
		}
		
		return copyChunk;
	}
}