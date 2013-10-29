package edu.cmu.scs.azurite.model.undo;

import static org.junit.Assert.*;

import java.util.List;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.junit.Before;
import org.junit.Test;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.commands.runtime.RuntimeDelete;
import edu.cmu.scs.azurite.commands.runtime.RuntimeInsert;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.fluorite.commands.AbstractCommand;
import edu.cmu.scs.fluorite.commands.BaseDocumentChangeEvent;
import edu.cmu.scs.fluorite.commands.Delete;
import edu.cmu.scs.fluorite.commands.Insert;
import edu.cmu.scs.fluorite.commands.Replace;

public class DetermineChunkTest {
	
	private RuntimeHistoryManager manager;
	private SelectiveUndoEngine engine;
	private IDocument document;
	
	@Before
	public void setUp() {
		manager = new RuntimeHistoryManager();
		manager.activeFileChanged("dummyProject", "dummyFile", null);
		
		engine = SelectiveUndoEngine.getInstance();
		
		document = new Document();
		
		AbstractCommand.resetCommandID();
	}

	@Test
	public void testAdjacentDeleteInsert() {
		Insert i = new Insert(10, "Insert", null);
		RuntimeInsert ri = new RuntimeInsert(i);
		
		Delete d = new Delete(5, 5, 0, 0, "Delet", null);
		RuntimeDelete rd = new RuntimeDelete(d);
		
		rd.applyTo(ri);
		
		List<Chunk> chunks = engine.determineChunks(engine
				.getAllSegments(new RuntimeDC[] { ri, rd }));
		
		assertEquals(1, chunks.size());
	}
	
	@Test
	public void testAdjacentInsertReplace() {
		BaseDocumentChangeEvent[] operations = new BaseDocumentChangeEvent[] {
				new Insert(0, "ABCD", null),
				new Insert(2, "abcd", null),
				new Insert(8, "1234", null),
				new Replace(7, 3, 0, 0, 4, "D12", "!@#$", null),
		};
		
		applyOperations(operations);
		
		assertEquals("ABabcdC!@#$34", document.get());
		
		List<Chunk> chunks = engine.determineChunks(engine
				.getAllSegments(new RuntimeDC[] { manager
						.getRuntimeDocumentChanges().get(0) }));
		
		assertEquals(1, chunks.size());
	}

	private void applyOperations(BaseDocumentChangeEvent[] operations) {
		for (BaseDocumentChangeEvent operation : operations) {
			operation.applyToDocument(document);
			manager.documentChangeFinalized(operation);
		}
	}

}
