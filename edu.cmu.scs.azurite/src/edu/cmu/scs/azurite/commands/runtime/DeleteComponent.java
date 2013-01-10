package edu.cmu.scs.azurite.commands.runtime;

import java.util.List;

/**
 * @author YoungSeok Yoon
 *
 */
public class DeleteComponent implements EditComponent {
	
	private Segment mDeleteSegment;

	public void initialize(Segment initialSegment) {
		mDeleteSegment = initialSegment;
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
		Segment segment = getDeleteSegment();
		
		boolean conflict = false;
		boolean dummyDelete = deleteSegment.getLength() == 0;
		
		//                                         | existing segment |
		// |---------- replacement ----------|
		if (deleteSegment.getEndOffset() <= segment.getOffset()) {
			if (deleteSegment.getEndOffset() == segment.getOffset() && !dummyDelete) {
				deleteSegment.addRight(segment);
			}
			
			// adjust all the subsequent segments' offsets.
			segment.incrementOffset(lengthDiff);
			
		}
		//        | existing segment |
		// |---------- replacement ----------|
		else if (deleteSegment.getOffset() < segment.getOffset()
				&& segment.getOffset() < deleteSegment.getEndOffset()) {
			
			// In this case, the delete segment should not be a dummy.
			if (deleteSegment.getLength() == 0) {
				throw new RuntimeException("delete segment should not be a dummy.");
			}
			
			deleteSegment.closeSegment(segment);
			
			conflict = true;
		}
		// |     existing segment     |
		//                                |---------- replacement ----------|
		else if (segment.getOffset() <= deleteSegment.getOffset()) {
			// Do nothing
		}
		// Should not fall to this else clause
		else {
			throw new IllegalStateException();
		}
		
		return conflict;
	}
	
	public Segment getDeleteSegment() {
		return mDeleteSegment;
	}

}
