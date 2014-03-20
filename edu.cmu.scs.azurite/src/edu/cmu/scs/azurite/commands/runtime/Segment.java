package edu.cmu.scs.azurite.commands.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.cmu.scs.fluorite.commands.Delete;
import edu.cmu.scs.fluorite.commands.Insert;
import edu.cmu.scs.fluorite.commands.Replace;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;

/**
 * @author YoungSeok Yoon
 * 
 */
public class Segment {

	private int mOffset;
	private int mLength;
	private String mText;
	
	private boolean mDeletion;

	private String mBelongsTo;
	private RuntimeDC mOwner;
	
	// Related to D->D & I->D conflict.
	private int mRelativeOffset;
	private int mOriginalLength;
	private List<Segment> mSegmentsClosedByMe;
	private List<Segment> mRight;

	/**
	 * Create a new segment.
	 * 
	 * @param offset
	 *            The offset of this segment.
	 * @param length
	 *            The length of this segment.
	 * @param text
	 *            The corresponding text for this segment.
	 * @param belongsTo
	 *            The file path to which this segment belongs.
	 * @param owner
	 *            The runtime object holding this segment.
	 * @param deletion
	 *            true if this segment represents deletion.
	 */
	public Segment(int offset, int length, String text, String belongsTo, RuntimeDC owner, boolean deletion) {
		mOffset = offset;
		mLength = length;
		mText = text;

		mBelongsTo = belongsTo;
		mOwner = owner;
		
		mDeletion = deletion;
		
		mRelativeOffset = -1;
		mOriginalLength = -1;
		mSegmentsClosedByMe = new ArrayList<Segment>();
		mRight = new ArrayList<Segment>();
	}

	/**
	 * Create a new segment from an Insert object.
	 * 
	 * @param insert
	 *            The insert object.
	 * @return Newly created segment using the insert object.
	 */
	public static Segment createInitialSegmentFromInsert(Insert insert, RuntimeDC owner) {
		return new Segment(insert.getOffset(), insert.getLength(),
				insert.getText(), RuntimeHistoryManager.getInstance()
						.getCurrentFile(), owner, false);
	}

	/**
	 * Create a new segment from a Delete object.
	 * 
	 * @param delete
	 *            The delete object.
	 * @return Newly created segment using the insert object.
	 */
	public static Segment createInitialSegmentFromDelete(Delete delete, RuntimeDC owner) {
		return new Segment(delete.getOffset(), delete.getLength(),
				delete.getText(), RuntimeHistoryManager.getInstance()
						.getCurrentFile(), owner, true);
	}

	/**
	 * Create a new segment from a Replace object.
	 * 
	 * @param replace
	 *            The replace object.
	 * @return Newly created segment using the insert object.
	 */
	public static Segment createInitialDeleteSegmentFromReplace(Replace replace, RuntimeDC owner) {
		return new Segment(replace.getOffset(), replace.getLength(),
				replace.getDeletedText(), RuntimeHistoryManager.getInstance()
						.getCurrentFile(), owner, true);
	}

	/**
	 * Create a new segment from a Replace object.
	 * 
	 * @param replace
	 *            The replace object.
	 * @return Newly created segment using the insert object.
	 */
	public static Segment createInitialInsertSegmentFromReplace(Replace replace, RuntimeDC owner) {
		return new Segment(replace.getOffset(), replace.getInsertionLength(),
				replace.getInsertedText(), RuntimeHistoryManager.getInstance()
						.getCurrentFile(), owner, false);
	}

	/**
	 * Getter for the offset value.
	 * 
	 * @return The offset value.
	 */
	public int getOffset() {
		return mOffset;
	}

	/**
	 * Increment the offset value by the given amount.
	 * 
	 * @param amount
	 *            The amount used when incrementing the offset.
	 */
	public void incrementOffset(int amount) {
		if (mOffset + amount < 0) {
			throw new IllegalArgumentException();
		}
		mOffset += amount;
	}

	/**
	 * Decrement the offset value by the given amount.
	 * 
	 * @param amount
	 *            The amount used when decrementing the offset.
	 */
	public void decrementOffset(int amount) {
		if (mOffset - amount < 0) {
			throw new IllegalArgumentException();
		}
		mOffset -= amount;
	}

	/**
	 * Set the offset as the given value.
	 * 
	 * @param offset
	 *            New offset to be used.
	 */
	public void setOffset(int offset) {
		if (offset < 0) {
			throw new IllegalArgumentException();
		}
		mOffset = offset;
	}

	/**
	 * Getter for the length value.
	 * 
	 * @return The length value.
	 */
	public int getLength() {
		return mLength;
	}

	private void setLength(int length) {
		mLength = length;
	}

	/**
	 * Returns the end offset of this segment (exclusive.)
	 * 
	 * @return offset + length value.
	 */
	public int getEndOffset() {
		return getOffset() + getLength();
	}
	
	/**
	 * Returns offset if deletion, end offset otherwise.
	 * @return offset if deletion, end offset otherwise.
	 */
	public int getEffectiveEndOffset() {
		return isDeletion() ? getOffset() : getEndOffset();
	}

	/**
	 * Getter for the text.
	 * 
	 * @return The corresponding text represented by this segment.
	 */
	public String getText() {
		return mText;
	}

	private void setText(String text) {
		if (text == null) {
			throw new IllegalArgumentException();
		}

		mText = text;
	}

	/**
	 * Getter for the belongsTo field.
	 * 
	 * @return The file path to which this segment belongs.
	 */
	public String getBelongsTo() {
		return mBelongsTo;
	}

	/**
	 * Merge with the following segment. The following segment should be removed
	 * from the segment list manually.
	 * 
	 * @param followingSegment
	 *            Another segment directly following this segment.
	 * @return true if success, false if fail.
	 */
	public boolean mergeWithSegment(Segment followingSegment) {
		if (followingSegment == null) {
			return false;
		}

		if (followingSegment.getOffset() != getEndOffset()) {
			return false;
		}

		setLength(getLength() + followingSegment.getLength());
		setText(getText() + followingSegment.getText());
		return true;
	}

	/**
	 * Return a new segment which represents a sub-section of this segment.
	 * 
	 * @param offset
	 *            The absolute offset relative to the beginning of the file.
	 * @param length
	 *            The length of the resulting sub segment.
	 * @return The new sub segment object.
	 */
	public Segment subSegment(int offset, int length) {
		if (offset < getOffset() || offset + length > getEndOffset()) {
			throw new IllegalArgumentException();
		}

		return new Segment(offset, length, getText().substring(
				offset - getOffset(), offset - getOffset() + length),
				getBelongsTo(), getOwner(), isDeletion());
	}

	/**
	 * Return a new segment which represents a sub-section of this segment
	 * starting from the given offset.
	 * 
	 * @param offset
	 *            The absolute offset relative to the beginning of the file.
	 * @return The new sub segment object.
	 */
	public Segment subSegment(int offset) {
		if (offset < getOffset() || offset > getEndOffset()) {
			throw new IllegalArgumentException();
		}

		return subSegment(offset, getEndOffset() - offset);
	}

	/**
	 * Cut down the given sub segment, and make this segment smaller. Note that
	 * this method does not change the offset.
	 * 
	 * @param offset
	 *            The absolute offset relative to the beginning of the file.
	 * @param length
	 *            The length of the resulting sub segment.
	 * @return true if succeeds, false if fails.
	 */
	public boolean cutDown(int offset, int length) {
		if (offset < getOffset() || offset + length > getEndOffset()) {
			throw new IllegalArgumentException();
		}

		setLength(getLength() - length);
		setText(getText().substring(0, offset - getOffset())
				+ getText().substring(offset - getOffset() + length));

		return true;
	}

	/**
	 * Cut down the given sub segment, and make this segment smaller. Note that
	 * this method does not change the offset.
	 * 
	 * @param offset
	 *            The absolute offset relative to the beginning of the file.
	 * @return true if succeeds, false if fails.
	 */
	public boolean cutDown(int offset) {
		if (offset < getOffset() || offset > getEndOffset()) {
			throw new IllegalArgumentException();
		}

		return cutDown(offset, getEndOffset() - offset);
	}
	
	/**
	 * Returns the runtime object which holds this segment.
	 * @return the runtime object.
	 */
	public RuntimeDC getOwner() {
		return mOwner;
	}
	
	/**
	 * Sets the runtime object which holds this segment.
	 * @param owner the runtime object.
	 */
	public void setOwner(RuntimeDC owner) {
		mOwner = owner;
	}
	
	/**
	 * Tells if this segment represents a deletion or not.
	 * @return true if this selection is deletion. false otherwise.
	 */
	public boolean isDeletion() {
		return mDeletion;
	}
	
	/**
	 * Set the relative offset.
	 * @param relativeOffset the relative offset value.
	 */
	public void setRelativeOffset(int relativeOffset) {
		mRelativeOffset = relativeOffset;
	}
	
	/**
	 * Get the relative offset.
	 * @return the relative offset.
	 */
	public int getRelativeOffset() {
		return mRelativeOffset;
	}
	
	/**
	 * Set the original length (only valid on a deleted insertion segment)
	 * @param originalLength the original length value
	 */
	public void setOriginalLength(int originalLength) {
		mOriginalLength = originalLength;
	}
	
	/**
	 * Get the original length of a deleted insertion segment.
	 * @return the original length;
	 */
	public int getOriginalLength() {
		return mOriginalLength;
	}
	
	public void close(int relativeOffset) {
		if (relativeOffset < 0) {
			throw new IllegalArgumentException("relative offset should be no less than zero!");
		}
		setRelativeOffset(relativeOffset);
		if (!isDeletion()) {
			setOriginalLength(getLength());
			setLength(0);
		}
	}
	
	/**
	 * Should be called when the closing segment is being undone.
	 * @param baseOffset offset of the closing segment.
	 */
	public void reopen(int baseOffset) {
		if (getRelativeOffset() == -1) {
			throw new IllegalStateException("Segment must be closed when reopen method is called");
		}
		
		setOffset(baseOffset + getRelativeOffset());
		setRelativeOffset(-1);
		if (!isDeletion()) {
			setLength(getOriginalLength());
			setOriginalLength(-1);
		}
	}
	
	/**
	 * Close another segment by me.
	 * @param toBeClosed the segment to be closed by me.
	 */
	public void closeSegment(Segment toBeClosed) {
		if (toBeClosed.getRelativeOffset() == -1) {
			toBeClosed.close(toBeClosed.getOffset() - getOffset());
			addSegmentClosedByMe(toBeClosed);
		}
		
		toBeClosed.setOffset(getOffset());
	}
	
	/**
	 * Add a (delete) segment closed by this segment.
	 * i.e. add a segment that is conflicted by this segment.
	 * @param closedSegment
	 */
	public void addSegmentClosedByMe(Segment closedSegment) {
		// Just for safety.
		if (!isDeletion()) {
			throw new RuntimeException("addSegmentClosedByMe method should only be called on a DELETE segment.");
		}
		mSegmentsClosedByMe.add(closedSegment);
	}
	
	/**
	 * @return read-only list of segments.
	 */
	public List<Segment> getSegmentsClosedByMe() {
		return Collections.unmodifiableList(mSegmentsClosedByMe);
	}
	
	public void addRight(Segment right) {
		mRight.add(right);
	}
	
	public List<Segment> getRight() {
		return Collections.unmodifiableList(mRight);
	}
	
	/**
	 * Make a copy of this segment and return it.
	 * Note that this method doesn't copy the mSegmentsClosedByMe contents.
	 * (Because this list should be reconstructed using the copy objects)
	 * @return copy of this segment.
	 */
	public Segment copySegment() {
		Segment copy = new Segment(mOffset, mLength, mText, mBelongsTo, mOwner, mDeletion);
		copy.mRelativeOffset = mRelativeOffset;
		copy.mOriginalLength = mOriginalLength;
		
		// Don't fill this out just yet...
		copy.mSegmentsClosedByMe = new ArrayList<Segment>();
		
		return copy;
	}
	
	/**
	 * Determines whether this segment is in the given selection range
	 * @param selectionStart the start offset of the selection range
	 * @param selectionEnd the end offset of the selection range
	 * @return true if this segment is within the selection range, false otherwise.
	 */
	public boolean inSelectionRange(int selectionStart, int selectionEnd) {
		if (isDeletion()) {
			return selectionStart < getOffset() && getOffset() < selectionEnd;
		} else {
			return getOffset() < selectionEnd && getEndOffset() > selectionStart;
		}
	}
	
	private static Comparator<Segment> locationComparator;
	
	/**
	 * Returns the singleton comparator objects which compares the location
	 * of the segment. Ties are broken by comparing the operation id.
	 * @return comparator object.
	 */
	public static Comparator<Segment> getLocationComparator() {
		if (locationComparator == null) {
			locationComparator = new Comparator<Segment>() {

				@Override
				public int compare(Segment lhs, Segment rhs) {
					if (lhs.getOffset() < rhs.getOffset()) {
						return -1;
					} else if (lhs.getOffset() > rhs.getOffset()) {
						return 1;
					} else if (lhs.getEffectiveEndOffset() < rhs.getEffectiveEndOffset()) {
						return -1;
					} else if (lhs.getEffectiveEndOffset() > rhs.getEffectiveEndOffset()) {
						return 1;
					} else if (lhs.getOwner().getOriginal().getSessionId() < rhs.getOwner().getOriginal().getSessionId()) {
						return -1;
					} else if (lhs.getOwner().getOriginal().getSessionId() > rhs.getOwner().getOriginal().getSessionId()) {
						return 1;
					} else if (lhs.getOwner().getOriginal().getCommandIndex() < rhs.getOwner().getOriginal().getCommandIndex()) {
						return -1;
					} else if (lhs.getOwner().getOriginal().getCommandIndex() > rhs.getOwner().getOriginal().getCommandIndex()) {
						return 1;
					}
					
					return 0;
				}
				
			};
		}
		
		return locationComparator;
	}

}
