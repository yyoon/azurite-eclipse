package edu.cmu.scs.azurite.commands.runtime;

import java.util.ArrayList;
import java.util.Collections;
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
	private BaseRuntimeDocumentChange mOwner;
	
	// Related to D->D conflict.
	private int mRelativeOffset;
	private List<Segment> mSegmentsClosedByMe;

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
	public Segment(int offset, int length, String text, String belongsTo, BaseRuntimeDocumentChange owner, boolean deletion) {
		mOffset = offset;
		mLength = length;
		mText = text;

		mBelongsTo = belongsTo;
		mOwner = owner;
		
		mDeletion = deletion;
		
		mRelativeOffset = -1;
		mSegmentsClosedByMe = new ArrayList<Segment>();
	}

	/**
	 * Create a new segment from an Insert object.
	 * 
	 * @param insert
	 *            The insert object.
	 * @return Newly created segment using the insert object.
	 */
	public static Segment createInitialSegmentFromInsert(Insert insert, BaseRuntimeDocumentChange owner) {
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
	public static Segment createInitialSegmentFromDelete(Delete delete, BaseRuntimeDocumentChange owner) {
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
	public static Segment createInitialDeleteSegmentFromReplace(Replace replace, BaseRuntimeDocumentChange owner) {
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
	public static Segment createInitialInsertSegmentFromReplace(Replace replace, BaseRuntimeDocumentChange owner) {
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
	public BaseRuntimeDocumentChange getOwner() {
		return mOwner;
	}
	
	/**
	 * Sets the runtime object which holds this segment.
	 * @param owner the runtime object.
	 */
	public void setOwner(BaseRuntimeDocumentChange owner) {
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
	 * Should be called when the closing segment is being undone.
	 * @param baseOffset offset of the closing segment.
	 */
	public void reopen(int baseOffset) {
		setOffset(baseOffset + mRelativeOffset);
		setRelativeOffset(-1);
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
}
