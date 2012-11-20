package edu.cmu.scs.azurite.model.undo;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.junit.Before;
import org.junit.Test;

import edu.cmu.scs.azurite.commands.runtime.BaseRuntimeDocumentChange;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.fluorite.commands.AbstractCommand;
import edu.cmu.scs.fluorite.commands.BaseDocumentChangeEvent;
import edu.cmu.scs.fluorite.commands.Delete;
import edu.cmu.scs.fluorite.commands.Insert;
import edu.cmu.scs.fluorite.commands.Replace;

public class SelectiveUndoTest {
	
	private RuntimeHistoryManager manager;
	private SelectiveUndoEngine engine;
	private IDocument document;
	
	@Before
	public void setUp() {
		manager = new RuntimeHistoryManager();
		manager.activeFileChanged("dummyProject", "dummyFile");
		
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
	
	private void undo(Integer ... indices) {
		List<BaseRuntimeDocumentChange> runtimeDocChanges = manager
				.filterDocumentChangesByIds(Arrays.asList(indices));
		
		engine.doSelectiveUndo(runtimeDocChanges, document);
	}

	private void applyOperations(BaseDocumentChangeEvent[] operations) {
		for (BaseDocumentChangeEvent operation : operations) {
			operation.applyToDocument(document);
			manager.documentChangeFinalized(operation);
		}
	}

}
