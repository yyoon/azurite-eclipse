package edu.cmu.scs.azurite.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.fluorite.commands.document.DocChange;
import edu.cmu.scs.fluorite.commands.document.Range;
import edu.cmu.scs.fluorite.model.Events;

public class OperationGrouper implements RuntimeDCListener {
	
	private static final int MERGE_TIME_THRESHOLD = 10000;	// in milliseconds
	
	public static final int LEVEL_PARSABLE = 0;
	public static final int LEVEL_METHOD = 1;
	public static final int LEVEL_CLASS = 2;
	public static final int NUM_LEVELS = 3;
	
	private Map<FileKey, Document>[] knownSnapshots;
	private FileKey currentFile;
	
	private List<RuntimeDC>[] pendingChangesList;
	private DocChange[] mergedPendingChanges;
	
	@SuppressWarnings("unchecked")
	public OperationGrouper() {
		this.knownSnapshots = new Map[NUM_LEVELS];
		this.pendingChangesList = new ArrayList[NUM_LEVELS];
		for (int i = 0; i < NUM_LEVELS; ++i) {
			this.knownSnapshots[i] = new HashMap<FileKey, Document>();
			this.pendingChangesList[i] = new ArrayList<RuntimeDC>();
		}
		
		this.currentFile = null;
		
		this.mergedPendingChanges = new DocChange[NUM_LEVELS];
		Arrays.fill(this.mergedPendingChanges, null);
	}

	@Override
	public void activeFileChanged(FileKey fileKey, String snapshot) {
		// Flush all the pending changes.
		for (int i = 0; i < NUM_LEVELS; ++i) {
			flushPendingChanges(i);
		}
		
		this.currentFile = fileKey;
		
		// The "initial" snapshot should be kept here, but not the subsequent ones.
		// Because, if a later snapshot is different from the previous one,
		// DiffDCs will be added by the RuntimeHistoryManager.
		if (snapshot != null) {
			for (int i = 0; i < NUM_LEVELS; ++i) {
				if (!this.knownSnapshots[i].containsKey(fileKey)) {
					this.knownSnapshots[i].put(fileKey, new Document(snapshot));
				}
			}
		}
	}

	@Override
	public void runtimeDCAdded(RuntimeDC dc) {
		Document currentSnapshot = getCurrentSnapshot(0);
		if (currentSnapshot == null) { return; }
		
		processRuntimeDC(0, Collections.singletonList(dc));
	}

	@Override
	public void documentChangeAdded(DocChange docChange) {
	}

	@Override
	public void documentChangeUpdated(DocChange docChange) {
	}

	@Override
	public void documentChangeAmended(DocChange oldDocChange, DocChange newDocChange) {
		// TODO Do something with this as well.
	}

	@Override
	public void pastLogsRead(List<Events> listEvents) {
		// TODO Do something with this as well.
	}
	
	private Document getCurrentSnapshot(int level) {
		return this.knownSnapshots[level].containsKey(currentFile)
				? this.knownSnapshots[level].get(this.currentFile)
				: null;
	}
	
	// c.f. LogProcessor#addPendingChange of fluorite-grouper.
	private void processRuntimeDC(int level, List<RuntimeDC> dcs) {
		DocChange mergedChange = RuntimeDC.mergeChanges(dcs);
		
		if (this.pendingChangesList[level].isEmpty() || shouldBeMerged(level, dcs, mergedChange)) {
			addPendingChanges(level, dcs, mergedChange);
		} else {
			flushPendingChanges(level);
			addPendingChanges(level, dcs, mergedChange);
		}
	}
	
	// c.f. LogProcessor#setPendingChange of fluorite-grouper.
	private void addPendingChanges(int level, List<RuntimeDC> dcs, DocChange mergedChange) {
		this.pendingChangesList[level].addAll(dcs);
		this.mergedPendingChanges[level] = DocChange.mergeChanges(this.mergedPendingChanges[level], mergedChange);
		
		int firstId = this.pendingChangesList[level].get(0).getOriginal().getCommandIndex();
		for (RuntimeDC dc : dcs) {
			for (int i = level; i < NUM_LEVELS; ++i) {
				dc.setCollapseID(i, firstId);
			}
		}
	}
	
	private boolean shouldBeMerged(int level, List<RuntimeDC> dcs, DocChange mergedChange) {
		switch (level) {
		case 0:
			return shouldBeMergedLevel0(dcs, mergedChange);
			
		case 1:
			return shouldBeMergedLevel1(dcs, mergedChange);
			
		case 2:
			return shouldBeMergedLevel2(dcs, mergedChange);
			
		default:
			throw new UnsupportedOperationException();
		}
	}
	
	/**
	 * Decide whether to really merge these two changes or not.
	 * 
	 * 1. If the two edits were performed in different lines, return false;
	 * 2. If the previous state is NOT parsable, return true.
	 * 3. If the time differs within MERGE_TIME_THRESHOLD, return true.
	 * 4. Return false, otherwise.
	 * 
	 * @return true if the two edits should be merged; false otherwise.
	 */
	private boolean shouldBeMergedLevel0(List<RuntimeDC> dcs, DocChange mergedChange) {
		if (dcs == null || dcs.size() != 1 || dcs.get(0) == null || dcs.get(0).getOriginal() == null) {
			throw new IllegalArgumentException();
		}
		
		// If the previous pending changes were all cancelled out themselves,
		// Do not merge the new ones with them.
		if (this.mergedPendingChanges[0] == null) { return false; }
		
		Document docBefore = getCurrentSnapshot(0);
		DocChange oldEvent = this.mergedPendingChanges[0];
		DocChange newEvent = dcs.get(0).getOriginal();
		
		Document docIntermediate = new Document(docBefore.get());
		oldEvent.apply(docIntermediate);
		
		if (docBefore != null) {
			try {
				Document doc = new Document(docBefore.get());
				oldEvent.apply(doc);
				if (!DocChange.overlap(oldEvent, newEvent) &&
					docIntermediate.getLineOfOffset(oldEvent.getInsertionRange().getEndOffset()) !=
					docIntermediate.getLineOfOffset(newEvent.getDeletionRange().getOffset()) &&
					docIntermediate.getLineOfOffset(oldEvent.getInsertionRange().getOffset()) !=
					docIntermediate.getLineOfOffset(newEvent.getDeletionRange().getEndOffset())) {
					return false;
				}
			} catch (BadLocationException e) {
				e.printStackTrace();
				if (!DocChange.overlap(oldEvent, newEvent)) {
					return false;
				}
			}
		} else {
			if (!DocChange.overlap(oldEvent, newEvent)) {
				return false;
			}
		}
		
		if (!isLocallyParsable(docIntermediate.get(), oldEvent)) {
			return true;
		}
		
		if (newEvent.getTimestamp() - oldEvent.getTimestamp2() < MERGE_TIME_THRESHOLD) {
			return true;
		}
		
		return false;
	}

	private boolean shouldBeMergedLevel1(List<RuntimeDC> dcs, DocChange mergedChange) {
		// TODO implement this properly.
		return false;
	}
	
	private boolean shouldBeMergedLevel2(List<RuntimeDC> dcs, DocChange mergedChange) {
		// TODO implement this properly.
		return false;
	}
	
	private boolean isLocallyParsable(String snapshot, DocChange lastChange) {
		ASTNode rootNode = parseSnapshot(snapshot);
		if (!(rootNode instanceof CompilationUnit)) {
			return false;
		}
		
		// Find the innermost node.
		Range insertionRange = lastChange.getInsertionRange();
		ASTNode node = NodeFinder.perform(rootNode, insertionRange.getEndOffset(), 0);
		
		// Walk all the descendents and see if any of them are malformed.
		MalformedNodeFinder visitor = new MalformedNodeFinder();
		node.accept(visitor);
		if (visitor.isMalformed()) {
			return false;
		}
		
		// Walk up the ancestor nodes and see if any of them are malformed.
		while (node != null) {
			if (isNodeMalformed(node)) {
				return false;
			}
			
			node = node.getParent();
		}
		
		return true;
	}

	private static ASTNode parseSnapshot(String snapshot) {
		// First, parse the file.
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(snapshot.toCharArray());
		
		@SuppressWarnings("rawtypes")
		Map options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
		parser.setCompilerOptions(options);
		
		// Do the parsing.
		ASTNode rootNode = parser.createAST(null);
		return rootNode;
	}
	
	private boolean isNodeMalformed(ASTNode node) {
		return (node.getFlags() & ASTNode.MALFORMED) == ASTNode.MALFORMED; 
	}
	
	private void flushPendingChanges(int level) {
		List<RuntimeDC> dcs = this.pendingChangesList[level];
		if (dcs.isEmpty()) { return; }
		
		int collapseID = dcs.get(0).getOriginal().getCommandIndex();
		for (RuntimeDC dc : dcs) {
			dc.setCollapseID(level, collapseID);
		}
		
		// TODO notify to the timeline view from here somehow?
		
		Document snapshot = getCurrentSnapshot(level);
		if (this.mergedPendingChanges[level] != null) {
			this.mergedPendingChanges[level].apply(snapshot);
		}

		// Go up one level
		if (level + 1 < NUM_LEVELS) {
			processRuntimeDC(level + 1, dcs);
		}
		
		// Clear the pending changes.
		this.pendingChangesList[level].clear();
		this.mergedPendingChanges[level] = null;
	}
	
	private class MalformedNodeFinder extends ASTVisitor {
		
		private boolean malformed = false;
		
		@Override
		public void preVisit(ASTNode node) {
			if (isMalformed()) {
				return;
			}
			
			if (isNodeMalformed(node)) {
				this.malformed = true;
			}
		}
		
		public boolean isMalformed() {
			return this.malformed;
		}
		
	}

}
