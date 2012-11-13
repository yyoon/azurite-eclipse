package edu.cmu.scs.azurite.model.undo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Shell;

import edu.cmu.scs.azurite.commands.runtime.BaseRuntimeDocumentChange;
import edu.cmu.scs.azurite.commands.runtime.Segment;
import edu.cmu.scs.fluorite.model.EventRecorder;
import edu.cmu.scs.fluorite.util.Utilities;

/**
 * @author YoungSeok Yoon
 *
 */
public class SelectiveUndoEngine {

	// Singleton pattern.
	private static SelectiveUndoEngine instance = null;

	/**
	 * @return the singleton object.
	 */
	public static SelectiveUndoEngine getInstance() {
		if (instance == null) {
			instance = new SelectiveUndoEngine();
		}

		return instance;
	}

	public void doSelectiveUndo(
			List<BaseRuntimeDocumentChange> runtimeDocChanges) {
		if (runtimeDocChanges == null) {
			throw new IllegalArgumentException();
		}
		
		doSelectiveUndo(runtimeDocChanges
				.toArray(new BaseRuntimeDocumentChange[runtimeDocChanges.size()]));
	}
	
	/**
	 * @param runtimeDocChanges
	 */
	public void doSelectiveUndo(BaseRuntimeDocumentChange[] runtimeDocChanges) {
		if (runtimeDocChanges == null) {
			throw new IllegalArgumentException();
			
		}
		// Get all the segments.
		List<Segment> segments = getAllSegments(runtimeDocChanges);
		
		// Determine Chunks.
		List<Chunk> chunks = determineChunks(segments);
		
		// Reverse the chunks, so the last chunk comes at first.
		Collections.reverse(chunks);
		
		// Get the IDocument object for the current editor.
		IDocument document = null;
		try {
			document = Utilities.getIDocumentForEditor(EventRecorder
					.getInstance().getEditor());
		} catch (Exception e) {
			// We don't have an active editor,
			// or just failed to get the IDocument object.
			// Don't do anything further.
		}
		
		if (document == null)
			return;
		
		// For each chunk..
		for (Chunk chunk : chunks) {
			// Is there a conflict?
			if (chunk.hasConflictOutsideThisChunk()) {
				// Temporarily, just mark the chunks on the styled text!!
				StyledText styledText = Utilities.getStyledText(EventRecorder
						.getInstance().getEditor());
				Shell shell = styledText.getShell();
				Color foreground = shell.getDisplay().getSystemColor(SWT.COLOR_YELLOW);
				Color background = shell.getDisplay().getSystemColor(SWT.COLOR_BLACK);
				
				StyleRange styleRange = new StyleRange(chunk.getStartOffset(),
						chunk.getChunkLength(), foreground, background);
				styledText.setStyleRange(styleRange);
			}
			// No conflicts. just undo them backwards.
			else {
				try {
					int initialOffset = chunk.getStartOffset();
					String initialContent = document.get(initialOffset,
							chunk.getChunkLength());
					String resultingContent = doSelectiveUndoChunkWithoutConflicts(
							chunk, initialContent);

					document.replace(initialOffset, initialContent.length(),
							resultingContent);
				}
				catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
		}
	}
	
	private String doSelectiveUndoChunkWithoutConflicts(
			Chunk chunk, String initialContent) {
		
		int initialOffset = chunk.getStartOffset();
		
		// Copy the segments, and reverse them!
		List<Segment> chunkSegments = new ArrayList<Segment>(chunk);
		Collections.reverse(chunkSegments);

		StringBuffer buffer = new StringBuffer(initialContent);

		for (int i = 0; i < chunkSegments.size(); ++i) {
			Segment chunkSegment = chunkSegments.get(i);

			// TODO maybe let this algorithm be part of Segment.
			if (chunkSegment.isDeletion()) {
				// Insert the text back at the offset.
				buffer.replace(
						chunkSegment.getOffset() - initialOffset,
						chunkSegment.getOffset() - initialOffset,
						chunkSegment.getText());

				// re-adjust all the segment offsets closed by this segment.
				for (Segment closedSegment : chunkSegment
						.getSegmentsClosedByMe()) {
					closedSegment.reopen(chunkSegment.getOffset());
				}
			} else {
				// Find the last insert segment with the same owner.
				Segment lastInsertSegment = chunkSegment;
				for (int j = i + 1; j < chunkSegments.size(); ++j) {
					Segment temp = chunkSegments.get(j);
					if (!temp.isDeletion()
							&& temp.getOwner() == chunkSegment.getOwner()) {
						lastInsertSegment = temp;
						i = j; // This is important. Ignore everything
								// in-between.
					}
				}

				// Delete the whole insert range.
				buffer.replace(
						lastInsertSegment.getOffset() - initialOffset,
						chunkSegment.getEndOffset() - initialOffset,
						"");
			}
		}
		
		return buffer.toString();
	}

	private List<Segment> getAllSegments(
			BaseRuntimeDocumentChange[] runtimeDocChanges) {
		ArrayList<Segment> segments = new ArrayList<Segment>();
		
		for (BaseRuntimeDocumentChange runtimeDocChange : runtimeDocChanges) {
			segments.addAll(runtimeDocChange.getAllSegments());
		}
		return segments;
	}
	
	private List<Chunk> determineChunks(List<Segment> segments) {
		// Sort! Collections.sort is guaranteed to be *stable*.
		Collections.sort(segments, new Comparator<Segment>() {
			public int compare(Segment lhs, Segment rhs) {
				if (lhs.getOffset() < rhs.getOffset()) {
					return -1;
				}
				else if (lhs.getOffset() > rhs.getOffset()) {
					return 1;
				}
				else if (lhs.getEffectiveEndOffset() < rhs.getEffectiveEndOffset()) {
					return -1;
				}
				else if (lhs.getEffectiveEndOffset() > rhs.getEffectiveEndOffset()) {
					return 1;
				}
				
				return 0;
			}
		});
		
		// List of chunks.
		ArrayList<Chunk> chunks = new ArrayList<Chunk>();
		
		Chunk prevChunk = null;
		
		// Look from the beginning.
		for (int i = 0; i < segments.size(); ++i) {
			Chunk chunk = null;
			// If this is the first segment or
			// this segment is within the previous chunk range...
			if (prevChunk == null
					|| segments.get(i).getEffectiveEndOffset() < prevChunk
							.getStartOffset()
					|| segments.get(i).getOffset() > prevChunk.getEndOffset()) {
				chunk = new Chunk();
			} else {
				chunk = prevChunk;
			}
			
			// See if there's any following segment coming from the same runtime doc change.
			// Find the last occurence.
			int j = segments.size() - 1;
			for (; j > i; --j) {
				if (segments.get(i).getOwner() == segments.get(j).getOwner()) {
					break;
				}
			}
			
			// Now add [i, j] elements to the current chunk!
			for (int k = i; k <= j; ++k) {
				chunk.add(segments.get(k));
			}
			
			if (chunk != prevChunk)
				chunks.add(chunk);
			
			// advance the loop index.
			i = j;
			
			prevChunk = chunk;
		}
		
		return chunks;
	}
}
