package edu.cmu.scs.azurite.model.undo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.commands.runtime.Segment;
import edu.cmu.scs.azurite.model.FileKey;

/**
 * @author yyoon1
 *
 */
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
	
	/**
	 * Gets a new chunk which includes all the conflicting operations within
	 * the same region.
	 * @return the expanded chunk object.
	 */
	public Chunk getExpandedChunkInRange() {
		Chunk result = new Chunk();
		
		// Flood fill.
		Queue<RuntimeDC> queue = new LinkedList<RuntimeDC>(getInvolvedChanges());
		Set<RuntimeDC> set = new HashSet<RuntimeDC>();
		
		while (!queue.isEmpty()) {
			RuntimeDC dc = queue.remove();
			
			if (!isInRange(dc, getStartOffset(), getEndOffset())) {
				continue;
			}
			
			if (set.contains(dc)) {
				continue;
			}
			
			set.add(dc);
			result.addAll(dc.getAllSegments());
			
			for (RuntimeDC conflict : dc.getConflicts()) {
				if (!set.contains(conflict)) {
					queue.add(conflict);
				}
			}
		}
		
		Collections.sort(result, Segment.getLocationComparator());
		return result;
	}
	
	/**
	 * Gets a new chunk which includes the conflicting operations up to the
	 * specified depth.
	 * 
	 * @param depth
	 *            maximum depth to include conflicting operations.
	 *            -1 if unlimited.
	 * @return the expanded chunk object.
	 */
	public Chunk getExpandedChunkWithDepth(int depth) {
		Chunk result = new Chunk();
		
		// Flood fill.
		Queue<Pair<RuntimeDC, Integer>> queue = new LinkedList<Pair<RuntimeDC, Integer>>();
		for (RuntimeDC dc : getInvolvedChanges())
			queue.add(new Pair<RuntimeDC, Integer>(dc, 0));
		Set<RuntimeDC> set = new HashSet<RuntimeDC>();
		
		while (!queue.isEmpty()) {
			Pair<RuntimeDC, Integer> pair = queue.remove();
			RuntimeDC dc = pair.getFirst();
			
			if (set.contains(dc)) {
				continue;
			}

			set.add(dc);
			result.addAll(dc.getAllSegments());
			
			if (depth != -1 && pair.getSecond().compareTo(depth) >= 0) {
				continue;
			}

			for (RuntimeDC conflict : dc.getConflicts()) {
				if (!set.contains(conflict)) {
					queue.add(new Pair<RuntimeDC, Integer>(conflict,
							pair.getSecond() + 1));
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
			
			for (Segment right : originalSegment.getRight()) {
				int rightIndex = this.indexOf(right);
				if (rightIndex != -1) {
					copySegment.addRight(copyChunk.get(rightIndex));
				}
			}
		}
		
		return copyChunk;
	}

	// Assume that all the segments within this chunk came from a same file.
	public FileKey getBelongsTo() {
		if (isEmpty()) {
			throw new IllegalStateException();
		}
		
		return this.get(0).getOwner().getBelongsTo();
	}
	
	private static Comparator<Chunk> locationComparator;
	
	public static Comparator<Chunk> getLocationComparator() {
		if (locationComparator == null) {
			locationComparator = new Comparator<Chunk>() {
				
				@Override
				public int compare(Chunk lhs, Chunk rhs) {
					if (lhs.getStartOffset() < rhs.getStartOffset()) {
						return -1;
					} else if (lhs.getStartOffset() > rhs.getStartOffset()) {
						return 1;
					} else if (lhs.getEndOffset() < rhs.getEndOffset()) {
						return -1;
					} else if (lhs.getEndOffset() > rhs.getEndOffset()) {
						return 1;
					}
					
					// TODO maybe sompare the session ids, command indices?
					
					return 0;
				}
			};
		}
		
		return locationComparator;
	}
}
