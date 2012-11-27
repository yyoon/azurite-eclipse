package edu.cmu.scs.azurite.model.undo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.jface.text.IDocument;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
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
			List<RuntimeDC> runtimeDocChanges) {
		if (runtimeDocChanges == null) {
			throw new IllegalArgumentException();
		}
		
		doSelectiveUndo(runtimeDocChanges
				.toArray(new RuntimeDC[runtimeDocChanges.size()]));
	}

	public void doSelectiveUndo(
			List<RuntimeDC> runtimeDocChanges, IDocument document) {
		if (runtimeDocChanges == null || document == null) {
			throw new IllegalArgumentException();
		}
		
		doSelectiveUndo(runtimeDocChanges
				.toArray(new RuntimeDC[runtimeDocChanges.size()]),
				document);
	}

	public void doSelectiveUndo(RuntimeDC[] runtimeDocChanges) {
		if (runtimeDocChanges == null) {
			throw new IllegalArgumentException();
		}
		
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
			throw new IllegalStateException("Failed to get the active document");
		
		doSelectiveUndo(runtimeDocChanges, document);
	}
		

	/**
	 * @param runtimeDocChanges
	 */
	public void doSelectiveUndo(RuntimeDC[] runtimeDocChanges, IDocument document) {
		if (runtimeDocChanges == null || document == null) {
			throw new IllegalArgumentException();
		}
		
		// Sort the runtimeDocChanges by their original command IDs.
		sortRuntimeDocumentChanges(runtimeDocChanges);
		
		// Get all the segments.
		List<Segment> segments = getAllSegments(runtimeDocChanges);
		
		// Determine Chunks.
		List<Chunk> chunks = determineChunks(segments);
		
		// Reverse the chunks, so the last chunk comes at first.
		Collections.reverse(chunks);
		
		// For each chunk..
		for (Chunk chunk : chunks) {
			try {
				int initialOffset = chunk.getStartOffset();
				String initialContent = document.get(initialOffset,
						chunk.getChunkLength());
				
				// Is there a conflict?
				if (chunk.hasConflictOutsideThisChunk()) {
	/*				// Temporarily, just mark the chunks on the styled text!!
					StyledText styledText = Utilities.getStyledText(EventRecorder
							.getInstance().getEditor());
					Shell shell = styledText.getShell();
					Color foreground = shell.getDisplay().getSystemColor(SWT.COLOR_YELLOW);
					Color background = shell.getDisplay().getSystemColor(SWT.COLOR_BLACK);
					
					StyleRange styleRange = new StyleRange(chunk.getStartOffset(),
							chunk.getChunkLength(), foreground, background);
					styledText.setStyleRange(styleRange);*/
					
					List<UndoAlternative> alternatives = doSelectiveUndoChunkWithConflicts(
							chunk, initialContent);
					
					// Just print to the console..
					System.out.println("====================");
					System.out.println(chunk);
					System.out.println("Initial Content:");
					System.out.println(initialContent);
					
					for (int i = 0; i < alternatives.size(); ++i) {
						UndoAlternative alternative = alternatives.get(i);
						System.out.println("Alternative #" + (i + 1) + ": "
								+ alternative.getDescription());
						System.out.println(alternative.getResultingCode());
					}
				}
				// No conflicts. just undo them backwards.
				else {
					String resultingContent = doSelectiveUndoChunkWithoutConflicts(
							chunk, initialContent);

					document.replace(initialOffset, initialContent.length(),
							resultingContent);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	}

	// Sort the runtimeDocChanges by their original command IDs.
	private void sortRuntimeDocumentChanges(RuntimeDC[] runtimeDocChanges) {
		Arrays.sort(runtimeDocChanges, RuntimeDC.getCommandIDComparator());
	}
	
	private List<UndoAlternative> doSelectiveUndoChunkWithConflicts(
			Chunk chunk, String initialContent) {
		List<UndoAlternative> result = new ArrayList<UndoAlternative>();
		
		// Revert this chunk to the way it was before all the selected operations were performed.
		result.add(new UndoAlternative(
				"Revert this code to the way it was right before all the selected operations were performed.",
				doSelectiveUndoChunkWithoutConflicts(
						chunk.getExpandedChunkInRange(), initialContent)));
		
		// Strictly selectively undo only the selected operations.
		result.add(new UndoAlternative(
				"Strictly selectively undo only the selected operations.",
				doSelectiveUndoChunkWithoutConflicts(chunk, initialContent)));
		
		// Add the trivial option at the end.
		result.add(new UndoAlternative(
				"Keep the code as it is. Do not perform selective undo for this chunk.", initialContent));
		
		// Return the result as an unmodifiable list.
		return Collections.unmodifiableList(result);
	}
	
	private String doSelectiveUndoChunkWithoutConflicts(
			Chunk chunk, String initialContent) {
		
		int initialOffset = chunk.getStartOffset();
		StringBuffer buffer = new StringBuffer(initialContent);
		
		// Copy the chunk.
		Chunk copyChunk = chunk.copyChunk();
		
		// Get all the involved changes and reverse them.
		List<RuntimeDC> involvedDCs = new ArrayList<RuntimeDC>(chunk.getInvolvedChanges());
		Collections.reverse(involvedDCs);
		
		// Undo from the end.
		for (RuntimeDC change : involvedDCs) {
			
			// Collect all copy segments associated with this change.
			List<Segment> segments = new ArrayList<Segment>();
			for (Segment copySegment : copyChunk) {
				if (copySegment.getOwner().equals(change)) {
					segments.add(copySegment);
				}
			}
			
			// Sort and then reverse.
			// It's different from sorting with the reverseOrder comparator.
			Collections.sort(segments, Segment.getLocationComparator());
			Collections.reverse(segments);
			
			// Undo each segment.
			ListIterator<Segment> it = segments.listIterator();
			while (it.hasNext()) {
				Segment segmentUnderUndo = it.next();
				
				if (segmentUnderUndo.isDeletion()) {
					// Insert the text back at the offset.
					buffer.replace(
							segmentUnderUndo.getOffset() - initialOffset,
							segmentUnderUndo.getOffset() - initialOffset,
							segmentUnderUndo.getText());
					
					// Re-adjust all the following segments' offsets.
					for (Segment chunkSegment : copyChunk) {
						if (chunkSegment.equals(segmentUnderUndo)) {
							continue;
						}
						if (Segment.getLocationComparator().compare(chunkSegment, segmentUnderUndo) < 0) {
							continue;
						}

						chunkSegment.incrementOffset(segmentUnderUndo.getLength());
					}
					
					// Re-open all the closed segments.
					for (Segment closedSegment : segmentUnderUndo.getSegmentsClosedByMe()) {
						if (copyChunk.contains(closedSegment)) {
							closedSegment.reopen(segmentUnderUndo.getOffset());
						}
					}
				}	// if(segmentUnderUndo.isDeletion())
				else {
					// Delete this segment.
					buffer.replace(
							segmentUnderUndo.getOffset() - initialOffset,
							segmentUnderUndo.getEndOffset() - initialOffset,
							"");
					
					// Re-adjust all the following segments' offsets.
					for (Segment chunkSegment : copyChunk) {
						if (chunkSegment.equals(segmentUnderUndo)) {
							continue;
						}
						if (Segment.getLocationComparator().compare(chunkSegment, segmentUnderUndo) < 0) {
							continue;
						}
						
						chunkSegment.decrementOffset(segmentUnderUndo.getLength());
					}
				}
				
				it.remove();
				copyChunk.remove(segmentUnderUndo);
			}
		}

		return buffer.toString();
	}

	List<Segment> getAllSegments(
			RuntimeDC[] runtimeDocChanges) {
		ArrayList<Segment> segments = new ArrayList<Segment>();
		
		for (RuntimeDC runtimeDocChange : runtimeDocChanges) {
			segments.addAll(runtimeDocChange.getAllSegments());
		}
		return segments;
	}
	
	List<Chunk> determineChunks(List<Segment> segments) {
		// Sort! Collections.sort is guaranteed to be *stable*.
		Collections.sort(segments, Segment.getLocationComparator());
		
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
			// Find the last occurrence.
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
