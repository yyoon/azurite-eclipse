package edu.cmu.scs.azurite.model.undo;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.junit.Before;
import org.junit.Test;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.model.OperationId;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.fluorite.commands.AbstractCommand;
import edu.cmu.scs.fluorite.commands.BaseDocumentChangeEvent;
import edu.cmu.scs.fluorite.commands.Delete;
import edu.cmu.scs.fluorite.commands.Insert;
import edu.cmu.scs.fluorite.commands.Replace;
import edu.cmu.scs.fluorite.model.EventRecorder;

public class SelectiveUndoTest {
	
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

	// This test came from the random test.
	@Test
	public void testInsertDeleteConflictWithinSelection() {
		document.set("JW7qZlnmyG");
		
		BaseDocumentChangeEvent[] operations = new BaseDocumentChangeEvent[] {
				new Insert(5, "SQ", null),
				new Replace(8, 2, 0, 0, 4, "nm", "kvmk", null),
				new Insert(3, "0a", null),
				new Delete(3, 4, 0, 0, "0aqZ", null),
		};
		
		applyOperations(operations);
		
		assertEquals("JW7SQlkvmkyG", document.get());
		
		undo(2, 3);
		
		assertEquals("JW7qZSQlkvmkyG", document.get());
	}
	
	// This test came from the random test.
	@Test
	public void testInsertDeleteReplace() {
		document.set("iRZdTMEc4W");
		
		BaseDocumentChangeEvent[] operations = new BaseDocumentChangeEvent[] {
				new Insert(9, "s0", null),
				new Delete(9, 2, 0, 0, "s0", null),
				new Replace(8, 2, 0, 0, 2, "4W", "lT", null),
		};
		
		applyOperations(operations);
		
		assertEquals("iRZdTMEclT", document.get());
		
		undo(0, 1, 2);
		
		assertEquals("iRZdTMEc4W", document.get());
	}
	
	// This test came from the random test.
	@Test
	public void testDeleteDeleteReplace() {
		document.set("pDarPrLtNF");
		
		BaseDocumentChangeEvent[] operations = new BaseDocumentChangeEvent[] {
				new Delete(3, 1, 0, 0, "r", null),
				new Delete(5, 4, 0, 0, "LtNF", null),
				new Replace(3, 2, 0, 0, 5, "Pr", "xIGSe", null),
		};
		
		applyOperations(operations);
		
		assertEquals("pDaxIGSe", document.get());
		
		undo(1, 2);
		
		assertEquals("pDaPrLtNF", document.get());
	}

	@Test
	public void testNoConflict() {
		BaseDocumentChangeEvent[] operations = new BaseDocumentChangeEvent[] {
				new Insert(0, "First", null),
				new Insert(5, "Second", null),
				new Insert(11, "Third", null),
				new Insert(16, "Fourth", null),
		};
		
		applyOperations(operations);
		
		assertEquals("FirstSecondThirdFourth", document.get());
		
		undo(1, 3);
		
		assertEquals("FirstThird", document.get());
	}
	
	@Test
	public void testSimpleConflict() {
		BaseDocumentChangeEvent[] operations = new BaseDocumentChangeEvent[] {
				new Insert(0, "System.out.println(\"Hello, world!\");\n", null),
				new Replace(27, 5, 0, 0, 3, "world", "Bob", null),
				new Insert(0, "System.out.println(\"Hello, Alice!\");\n", null),
				new Insert(72, "System.out.println(\"Hello, Charlie!\");\n", null),
				new Delete(20, 7, 0, 0, "Hello, ", null),
		};
		
		applyOperations(operations);
		
		undo(1);
		
		assertEquals("System.out.println(\"Alice!\");\n"
				+ "System.out.println(\"Hello, world!\");\n"
				+ "System.out.println(\"Hello, Charlie!\");\n", document.get());
		
		undo(4);
		
		assertEquals("System.out.println(\"Hello, Alice!\");\n"
				+ "System.out.println(\"Hello, world!\");\n"
				+ "System.out.println(\"Hello, Charlie!\");\n", document.get());
	}
	
	@Test
	public void testDeleteOrder01() {
		BaseDocumentChangeEvent[] operations = new BaseDocumentChangeEvent[] {
				new Insert(0, "AB", null),
				new Delete(0, 1, 0, 0, "A", null),
				new Delete(0, 1, 0, 0, "B", null),
		};
		
		applyOperations(operations);
		
		assertEquals("", document.get());
		
		undo(1, 2);
		
		assertEquals("AB", document.get());
	}
	
	@Test
	public void testDeleteOrder02() {
		BaseDocumentChangeEvent[] operations = new BaseDocumentChangeEvent[] {
				new Insert(0, "AB", null),
				new Delete(1, 1, 0, 0, "B", null),
				new Delete(0, 1, 0, 0, "A", null),
		};
		
		applyOperations(operations);
		
		assertEquals("", document.get());
		
		undo(1, 2);
		
		assertEquals("AB", document.get());
	}
	
	@Test
	public void testIDConflict01() {
		BaseDocumentChangeEvent[] operations = new BaseDocumentChangeEvent[] {
				new Insert(0, "AB", null),
				new Insert(2, "CD", null),
				new Insert(4, "EF", null),
				new Delete(3, 2, 0, 0, "DE", null),
		};
		
		applyOperations(operations);
		
		assertEquals("ABCF", document.get());
		
		undo(3);
		
		assertEquals("ABCDEF", document.get());
	}
	
	@Test
	public void testComplexConflict() {
		BaseDocumentChangeEvent[] operations = new BaseDocumentChangeEvent[] {
				new Insert(0, "ABCD", null),
				new Insert(2, "abcd", null),
				new Insert(8, "1234", null),
				new Replace(7, 3, 0, 0, 4, "D12", "!@#$", null),
		};
		
		applyOperations(operations);
		
		assertEquals("ABabcdC!@#$34", document.get());
		
		undo(0, 1, 3);
		
		assertEquals("1234", document.get());
	}
	
	private void undo(Integer ... indices) {
		long sessionId = EventRecorder.getInstance().getStartTimestamp();
		List<OperationId> oids = new ArrayList<OperationId>();
		for (Integer id : indices) {
			oids.add(new OperationId(sessionId, id));
		}
		
		List<RuntimeDC> runtimeDocChanges = manager
				.filterDocumentChangesByIds(oids);
		
		engine.doSelectiveUndo(runtimeDocChanges, document);
	}

	private void applyOperations(BaseDocumentChangeEvent[] operations) {
		for (BaseDocumentChangeEvent operation : operations) {
			operation.applyToDocument(document);
			manager.documentChangeFinalized(operation);
		}
	}

}
