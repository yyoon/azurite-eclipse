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
	
	private boolean applyHelper(Segment deleteSegment, Segment insertSegment, RuntimeDC docChange) {
		int lengthDiff = insertSegment.getLength() - deleteSegment.getLength();
		
		ListIterator<Segment> it = mInsertSegments.listIterator();
		
		boolean conflict = false;
		boolean dummyDelete = deleteSegment.getLength() == 0;

		while (it.hasNext()) {
			Segment segment = it.next();
			
			if (deleteSegment.getEndOffset() <= segment.getOffset()) {
			//                                         | existing segment |
			// |---------- replacement ----------|
				
				// adjust all the subsequent segments' offsets.
				segment.incrementOffset(lengthDiff);
				
				while (it.hasNext()) {
					it.next().incrementOffset(lengthDiff);
				}
			} else if (deleteSegment.getOffset() <= segment.getOffset()
					&& segment.getOffset() < deleteSegment.getEndOffset()
					&& deleteSegment.getEndOffset() < segment.getEndOffset()) {
			//                          | existing segment |
			// |---------- replacement ----------|
				
				// Split the segment into two and close the first segment.
				Segment segmentToBeAdded = segment.subSegment(
						deleteSegment.getEndOffset(), segment.getEndOffset()
								- deleteSegment.getEndOffset());
				segmentToBeAdded.incrementOffset(lengthDiff);
				it.add(segmentToBeAdded);
				
				// cut down the current segment, and adjust all the subsequent segments.
				segment.cutDown(deleteSegment.getEndOffset());
				deleteSegment.closeSegment(segment);
				
				while (it.hasNext()) {
					it.next().incrementOffset(lengthDiff);
				}
				
				conflict = true;
			} else if (deleteSegment.getOffset() <= segment.getOffset()
					&& segment.getEndOffset() <= deleteSegment.getEndOffset()) {
			//        | existing segment |
			// |---------- replacement ----------|
				
				deleteSegment.closeSegment(segment);
				
				conflict = true;
			} else if (segment.getOffset() < deleteSegment.getOffset()
					&& deleteSegment.getEndOffset() < segment.getEndOffset()) {
			// |       existing segment       |
			//      |--- replacement ---|
				
				// Split the current segment into three pieces.
				if (!dummyDelete) {
					Segment segmentInTheMiddle = segment.subSegment(
							deleteSegment.getOffset(), deleteSegment.getLength());
					deleteSegment.closeSegment(segmentInTheMiddle);
					it.add(segmentInTheMiddle);
				}
				
				Segment segmentAtTheEnd = segment.subSegment(deleteSegment.getEndOffset());
				segmentAtTheEnd.incrementOffset(lengthDiff);
				it.add(segmentAtTheEnd);
				
				// Cut down the current segment.
				segment.cutDown(deleteSegment.getOffset());
				
				while (it.hasNext()) {
					it.next().incrementOffset(lengthDiff);
				}
				
				conflict = true;
			} else if (segment.getOffset() < deleteSegment.getOffset()
					&& deleteSegment.getOffset() < segment.getEndOffset()
					&& segment.getEndOffset() <= deleteSegment.getEndOffset()) {
			// |     existing segment     |
			//              |---------- replacement ----------|
				
				Segment segmentToBeAdded = segment.subSegment(deleteSegment.getOffset());
				deleteSegment.closeSegment(segmentToBeAdded);
				it.add(segmentToBeAdded);
				
				// cut down the current segment, and continue to loop.
				segment.cutDown(deleteSegment.getOffset());
				
				conflict = true;
			} else if (segment.getEndOffset() <= deleteSegment.getOffset()) {
			// |     existing segment     |
			//                                |---------- replacement ----------|
				
				// Do nothing, and continue to loop.
			} else {
			// Should not fall to this else clause
				throw new IllegalStateException();
			}
		}// while (it.hasNext())
		
		return conflict;
	}
	
	public List<Segment> getInsertSegments() {
		return mInsertSegments;
	}

}
