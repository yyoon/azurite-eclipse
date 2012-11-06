package edu.cmu.scs.azurite.commands.runtime;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * @author YoungSeok Yoon
 *
 */
public class InsertComponent implements EditComponent {
	
	private LinkedList<Segment> mInsertSegments;
	
	public void initialize(Segment initialSegment) {
		mInsertSegments = new LinkedList<Segment>();
		mInsertSegments.addLast(initialSegment);
	}
	
	public boolean applyInsert(RuntimeInsert insert) {
		List<Segment> insertSegments = insert.getInsertSegments();
		if (insertSegments.size() != 1) {
			throw new IllegalArgumentException("A new insert object must have exactly one insertion segment.");
		}
		
		Segment insertSegment = insertSegments.get(0);
		Segment dummyDelete = new Segment(insertSegment.getOffset(), 0, "", insertSegment.getBelongsTo(), insertSegment.getOwner(), true);
		
		return applyHelper(dummyDelete, insertSegment, insert);
	}
	
	public boolean applyDelete(RuntimeDelete delete) {
		Segment deleteSegment = delete.getDeleteSegment();
		Segment dummyInsert = new Segment(deleteSegment.getOffset(), 0, "", deleteSegment.getBelongsTo(), deleteSegment.getOwner(), false);
		
		return applyHelper(deleteSegment, dummyInsert, delete);
	}
	
	public boolean applyReplace(RuntimeReplace replace) {
		List<Segment> insertSegments = replace.getInsertSegments();
		if (insertSegments.size() != 1) {
			throw new IllegalArgumentException("A new replace object must have exactly one insertion segment.");
		}
		
		return applyHelper(replace.getDeleteSegment(), insertSegments.get(0), replace);
	}
	
	private boolean applyHelper(Segment deleteSegment, Segment insertSegment, BaseRuntimeDocumentChange docChange) {
		int lengthDiff = insertSegment.getLength() - deleteSegment.getLength();
		
		ListIterator<Segment> it = mInsertSegments.listIterator();
		
		boolean conflict = false;
		
		while (it.hasNext()) {
			Segment segment = it.next();
			
			//                                         | existing segment |
			// |---------- replacement ----------|
			if (deleteSegment.getEndOffset() <= segment.getOffset()) {
				// adjust all the subsequent segments' offsets.
				segment.incrementOffset(lengthDiff);
				
				while (it.hasNext()) {
					it.next().incrementOffset(lengthDiff);
				}
			}
			//                          | existing segment |
			// |---------- replacement ----------|
			else if (deleteSegment.getOffset() <= segment.getOffset()
					&& segment.getOffset() < deleteSegment.getEndOffset()
					&& deleteSegment.getEndOffset() < segment.getEndOffset()) {
				// cut down the current segment, and adjust all the subsequent segments.
				segment.cutDown(segment.getOffset(), deleteSegment.getEndOffset() - segment.getOffset());
				segment.setOffset(deleteSegment.getOffset() + insertSegment.getLength());
				
				while (it.hasNext()) {
					it.next().incrementOffset(lengthDiff);
				}
				
				conflict = true;
			}
			//        | existing segment |
			// |---------- replacement ----------|
			else if (deleteSegment.getOffset() <= segment.getOffset()
					&& segment.getEndOffset() <= deleteSegment.getEndOffset()) {
				// simply remove the current segment and continue the iteration.
				it.remove();
				
				conflict = true;
			}
			// |       existing segment       |
			//      |--- replacement ---|
			else if (segment.getOffset() < deleteSegment.getOffset()
					&& deleteSegment.getEndOffset() < segment.getEndOffset()) {
				// split the current segment, and adjust all the subsequent segments.
				Segment segmentToBeAdded = segment.subSegment(deleteSegment.getEndOffset());
				segmentToBeAdded.incrementOffset(lengthDiff);
				it.add(segmentToBeAdded);
				
				segment.cutDown(deleteSegment.getOffset());
				
				while (it.hasNext()) {
					it.next().incrementOffset(lengthDiff);
				}
				
				conflict = true;
			}
			// |     existing segment     |
			//              |---------- replacement ----------|
			else if (segment.getOffset() < deleteSegment.getOffset()
					&& deleteSegment.getOffset() < segment.getEndOffset()
					&& segment.getEndOffset() <= deleteSegment.getEndOffset()) {
				// cut down the current segment, and continue to loop.
				segment.cutDown(deleteSegment.getOffset());
				
				conflict = true;
			}
			// |     existing segment     |
			//                                |---------- replacement ----------|
			else if (segment.getEndOffset() <= deleteSegment.getOffset()) {
				// Do nothing, and continue to loop.
			}
			// Should not fall to this else clause
			else {
				throw new IllegalStateException();
			}
		}// while (it.hasNext())
		
		// TODO cleanup the segments! merge subsequent segments, and close out when the array is empty.
		
		return conflict;
	}
	
	public List<Segment> getInsertSegments() {
		return mInsertSegments;
	}

}
