package edu.cmu.scs.azurite.model.undo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.commands.runtime.Segment;
import edu.cmu.scs.azurite.jface.dialogs.ConflictResolutionDialog;
import edu.cmu.scs.azurite.model.FileKey;
import edu.cmu.scs.fluorite.model.EventRecorder;
import edu.cmu.scs.fluorite.util.Utilities;

/**
 * @author YoungSeok Yoon
 *
 */
public class SelectiveUndoEngine {
	
	public static final int MAX_EXPANSION_DEPTH = 2;

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
		
		if (document == null) {
			throw new IllegalStateException("Failed to get the active document");
		}
		
		doSelectiveUndo(runtimeDocChanges, document);
	}


	/**
	 * @param runtimeDocChanges
	 */
	public void doSelectiveUndo(RuntimeDC[] runtimeDocChanges, IDocument document) {
		if (runtimeDocChanges == null || document == null) {
			throw new IllegalArgumentException();
		}
		
		// Determine Chunks.
		List<Chunk> chunks = determineChunksWithRuntimeDCs(runtimeDocChanges);
		
		doSelectiveUndoWithChunks(chunks, document);
	}

	public void doSelectiveUndoWithChunks(List<Chunk> chunks, IDocument document) {
		doSelectiveUndoWithChunks(chunks, document, null);
	}
	
	public void doSelectiveUndoWithChunks(List<Chunk> chunks, IDocument document, Map<Chunk, UndoAlternative> alternativeChoices) {
		// Reverse the chunks, so the last chunk comes at first.
		Collections.reverse(chunks);
		
		// For each chunk..
		for (Chunk chunk : chunks) {
			try {
				Chunk expandedChunk = chunk.getExpandedChunkWithDepth(MAX_EXPANSION_DEPTH);
				int initialOffset = expandedChunk.getStartOffset();
				String initialContent = document.get(initialOffset,
						expandedChunk.getChunkLength());
				
				// Is there a conflict?
				if (chunk.hasConflictOutsideThisChunk()) {
					List<UndoAlternative> alternatives = doSelectiveUndoChunkWithConflicts(
							chunk, initialContent);
					
					// If the result is obvious, don't bother to ask the user.
					if (alternatives.size() <= 2) {
						document.replace(initialOffset, initialContent.length(),
								alternatives.get(0).getResultingCode());
					} else if (alternativeChoices != null && alternativeChoices.get(chunk) != null) {
						UndoAlternative chosenAlternative = alternativeChoices.get(chunk);
						document.replace(initialOffset, initialContent.length(), chosenAlternative.getResultingCode());
					} else {
						final Shell parentShell = Display.getDefault().getActiveShell();
						
						ConflictResolutionDialog conflictDialog = new ConflictResolutionDialog(
								parentShell, document, initialOffset,
								initialContent.length(), alternatives, chunk);
						conflictDialog.create();
						conflictDialog.open();
					}
				} else {
				// No conflicts. just undo them backwards.
					String resultingContent = doSelectiveUndoChunkWithoutConflicts(
							chunk, initialContent);

					document.replace(initialOffset, initialContent.length(),
							resultingContent);
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	}

	// Sort the runtimeDocChanges by their original command IDs.
	private void sortRuntimeDocumentChanges(RuntimeDC[] runtimeDocChanges) {
		Arrays.sort(runtimeDocChanges, RuntimeDC.getCommandIDComparator());
	}
	
	public List<UndoAlternative> doSelectiveUndoChunkWithConflicts(
			Chunk chunk, String initialContent) {
		List<UndoAlternative> result = new ArrayList<UndoAlternative>();
		
		// Revert this chunk to the way it was before all the selected operations were performed.
		result.add(new UndoAlternative(
				"Revert this code to the way it was right before all the selected operations were performed.",
				doSelectiveUndoChunkWithoutConflicts(
						chunk.getExpandedChunkInRange(), initialContent)));
		
		// Revert this chunk to the way it was before, including the unselected conflicting operations.
		for (int i = 1; i <= MAX_EXPANSION_DEPTH; ++i) {
			if (i == 0) {
				continue;
			}
			
			result.add(new UndoAlternative(
					"Revert this code including the non-selected conflicting operations with depth "
							+ i + ".",
					doSelectiveUndoChunkWithoutConflicts(chunk
							.getExpandedChunkWithDepth(MAX_EXPANSION_DEPTH),
							initialContent)));
		}
		
		// Strictly selectively undo only the selected operations.
		result.add(new UndoAlternative(
				"Strictly selectively undo only the selected operations.",
				doSelectiveUndoChunkWithoutConflicts(chunk, initialContent)));
		
		// Add the trivial option at the end.
		result.add(new UndoAlternative(
				"Keep the code as it is. Do not perform selective undo for this chunk.", initialContent));
		
		// Merge the same results.
		for (int i = 1; i < result.size(); ++i) {
			UndoAlternative cur = result.get(i);

			for (int j = 0; j < i; ++j) {
				UndoAlternative prev = result.get(j);
				// TODO maybe ignore the whitespaces / indentations in the future.
				if (prev.getResultingCode().equals(cur.getResultingCode())) {
					result.remove(i);
					--i;
					break;
				}
			}
		}
		
		// Return the result as an unmodifiable list.
		return Collections.unmodifiableList(result);
	}
	
	public String doSelectiveUndoChunkWithoutConflicts(
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
				
				undoSegment(segmentUnderUndo, copyChunk, initialOffset, buffer);
				
				it.remove();
				copyChunk.remove(segmentUnderUndo);
			}
		}

		return buffer.toString();
	}

	private void undoSegment(Segment segmentUnderUndo, Chunk copyChunk,
			int initialOffset, StringBuffer buffer) {
		if (segmentUnderUndo.isDeletion()) {
			undoDeleteSegment(segmentUnderUndo, copyChunk, initialOffset, buffer);
		} else {
			undoInsertSegment(segmentUnderUndo, copyChunk, initialOffset, buffer);
		}
	}

	private void undoInsertSegment(Segment segmentUnderUndo, Chunk copyChunk,
			int initialOffset, StringBuffer buffer) {
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

	private void undoDeleteSegment(Segment segmentUnderUndo, Chunk copyChunk,
			int initialOffset, StringBuffer buffer) {
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
		
		for (Segment right : segmentUnderUndo.getRight()) {
			if (copyChunk.contains(right)) {
				right.setOffset(segmentUnderUndo.getOffset() + segmentUnderUndo.getLength());
			}
		}
	}
	
	public void doSelectiveUndoWithParams(SelectiveUndoParams params) {
		if (params == null) {
			throw new IllegalArgumentException();
		}
		
		doSelectiveUndoWithChunks(params.getChunks(), params.getDocument(), params.getAlternativeChoices());
	}

	List<Segment> getAllSegments(
			RuntimeDC[] runtimeDocChanges) {
		ArrayList<Segment> segments = new ArrayList<Segment>();
		
		for (RuntimeDC runtimeDocChange : runtimeDocChanges) {
			segments.addAll(runtimeDocChange.getAllSegments());
		}
		return segments;
	}
	
	public List<Chunk> determineChunksWithRuntimeDCs(List<RuntimeDC> runtimeDCs) {
		return determineChunksWithRuntimeDCs(runtimeDCs.toArray(new RuntimeDC[runtimeDCs.size()]));
	}
	
	public List<Chunk> determineChunksWithRuntimeDCs(RuntimeDC[] runtimeDCs) {
		if (runtimeDCs == null) {
			throw new IllegalArgumentException();
		}
		
		// Sort the runtimeDocChanges by their original command IDs.
		sortRuntimeDocumentChanges(runtimeDCs);
		
		// Get all the segments.
		List<Segment> segments = getAllSegments(runtimeDCs);
		
		// Determine Chunks.
		return determineChunks(segments);
	}
	
	public List<Chunk> determineChunks(List<Segment> segments) {
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
			
			if (chunk != prevChunk) {
				chunks.add(chunk);
			}
			
			// advance the loop index.
			i = j;
			
			prevChunk = chunk;
		}
		
		return chunks;
	}
	
	public void doSelectiveUndoOnMultipleFiles(
			Map<FileKey, List<RuntimeDC>> params) {
		// Process per file.
		for (FileKey key : params.keySet()) {
			List<RuntimeDC> runtimeDCs = params.get(key);
			
			// 1. Open the file in the editor.
			File fileToOpen = new File(key.getFilePath());
			
			if (fileToOpen.exists() && fileToOpen.isFile()) {
			    IFileStore fileStore = EFS.getLocalFileSystem().getStore(fileToOpen.toURI());
			    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			 
			    try {
			        IDE.openEditorOnFileStore( page, fileStore );
			    	
					// 2. Perform selective undo.
			        doSelectiveUndo(runtimeDCs);
			    } catch ( PartInitException e ) {
			    	e.printStackTrace();
				}
			} else {
			    //Do something if the file does not exist
			}
		}
	}
	
	public void doSelectiveUndoOnMultipleFilesWithChoices(
			Map<FileKey, SelectiveUndoParams> params) {
		for (FileKey key : params.keySet()) {
			// 1. Open the file in the editor.
			File fileToOpen = new File(key.getFilePath());
			
			if (fileToOpen.exists() && fileToOpen.isFile()) {
			    IFileStore fileStore = EFS.getLocalFileSystem().getStore(fileToOpen.toURI());
			    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			 
			    try {
			        IDE.openEditorOnFileStore( page, fileStore );
			    	
					// 2. Perform selective undo.
			        doSelectiveUndoWithParams(params.get(key));
			    } catch ( PartInitException e ) {
			    	e.printStackTrace();
				}
			} else {
			    //Do something if the file does not exist
			}
		}
	}
	
}
