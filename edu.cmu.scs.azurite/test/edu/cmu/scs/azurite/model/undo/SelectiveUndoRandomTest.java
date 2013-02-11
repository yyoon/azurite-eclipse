package edu.cmu.scs.azurite.model.undo;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.junit.Before;
import org.junit.Test;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager.FileKey;
import edu.cmu.scs.fluorite.commands.AbstractCommand;
import edu.cmu.scs.fluorite.commands.BaseDocumentChangeEvent;
import edu.cmu.scs.fluorite.commands.Delete;
import edu.cmu.scs.fluorite.commands.Insert;
import edu.cmu.scs.fluorite.commands.Replace;

public class SelectiveUndoRandomTest {
	
	private static final String RANDOM_TEXT = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

	private static final int LOOP_COUNT = 1;
	private static final int INITIAL_TEXT_LENGTH = 10;
	private static final int RANDOM_TEXT_MAX_LENGTH = 5;
	
	@Before
	public void setUp() {
		AbstractCommand.resetCommandID();
	}

	@Test
	public void testRandom10Operations() {
		testRandomOperations(10);
	}
	
	@Test
	public void estimateTimeToTake() {
		int numOperations = 1695;
//		int numOperations = 10000;
		
		long totalMillis = 0;
		
		for (int trial = 0; trial < 100; ++trial) {
			AbstractCommand.resetCommandID();
			RuntimeHistoryManager manager = new RuntimeHistoryManager();
			manager.activeFileChanged("dummyProject", "dummyFile", null);
			
			Document doc = new Document(
					randomStringOfLength(100));
			
			for (int i = 0; i < numOperations; ++i) {
				manager.documentChangeFinalized(applyRandomOperation(doc));
			}
			
			long start = System.currentTimeMillis();
			
			for (FileKey key : manager.getFileKeys()) {
				manager.calculateDynamicSegments(key);
			}
			
			long end = System.currentTimeMillis();
			
			System.out.println("Trial " + trial + "\t" + (end - start) + "ms");
			totalMillis += (end - start);
		}
		
		System.out.println("Mean: " + totalMillis / 100.0);
	}
	
	// This takes about 100 seconds to finish.
	// Only enable this when an excessive testing is required.
/*	@Test
	public void testRandom100Operations() {
		testRandomOperations(100);
	}*/

	public void testRandomOperations(int numOperations) {
		for (int trial = 0; trial < LOOP_COUNT; ++trial) {
			AbstractCommand.resetCommandID();

			RuntimeHistoryManager manager = new RuntimeHistoryManager();
			manager.activeFileChanged("dummyProject", "dummyFile", null);
			
			Document doc = new Document(
					randomStringOfLength(INITIAL_TEXT_LENGTH));
			
			List<IDocument> documentList = new ArrayList<IDocument>();
			List<BaseDocumentChangeEvent> docChanges = new ArrayList<BaseDocumentChangeEvent>();
			for (int i = 0; i < numOperations; ++i) {
				// Make a copy and keep it in the list.
				documentList.add(new Document(doc.get()));
				
				// get a random operation and add it to the manager.
				BaseDocumentChangeEvent operation = applyRandomOperation(doc);
				docChanges.add(operation);
				manager.documentChangeFinalized(operation);
				
				// Try backout!
				for (int j = 0; j <= i; ++j) {
					Document docCopy = new Document(doc.get());
					
					int count = manager.getRuntimeDocumentChanges().size();
					List<Integer> ids = new ArrayList<Integer>();
					for (int k = count - j - 1; k < count; ++k) {
						ids.add(k);
					}
					
					List<RuntimeDC> toBeUndone = 
							manager.filterDocumentChangesByIds(ids);
					
					// Do the selective undo.
					SelectiveUndoEngine.getInstance().doSelectiveUndo(
							toBeUndone, docCopy);
					
					// Compare the result with the one in the documentList.
					if (!documentList.get(i - j).get().equals(docCopy.get())) {
						printTrace(documentList.get(0).get(), docChanges, i, j);
					}
					assertEquals(documentList.get(i - j).get(), docCopy.get());
				}
			}
			
			printTrace(documentList.get(0).get(), docChanges, 0, 0);
		}
	}
	
	private void printTrace(String initialContent,
			List<BaseDocumentChangeEvent> docChanges, int i, int j) {
		System.out.println("i = " + i + "\tj = " + j);
		System.out.println("Initial Content:");
		System.out.println(initialContent);
		
		for (BaseDocumentChangeEvent docChange : docChanges) {
			if (docChange instanceof Insert) {
				Insert insert = (Insert)docChange;
				System.out.println("I[" + insert.getOffset() + ", "
						+ insert.getLength() + "]: \"" + insert.getText()
						+ "\"");
			}
			else if (docChange instanceof Delete) {
				Delete delete = (Delete)docChange;
				System.out.println("D[" + delete.getOffset() + ", "
						+ delete.getLength() + "]: \"" + delete.getText()
						+ "\"");
			}
			else if (docChange instanceof Replace) {
				Replace replace = (Replace)docChange;
				System.out.println("R[" + replace.getOffset() + ", "
						+ replace.getLength() + "]: \""
						+ replace.getDeletedText() + "\", \""
						+ replace.getInsertedText() + "\"");
			}
		}
	}
	
	private BaseDocumentChangeEvent applyRandomOperation(IDocument document) {
		while (true) {
			double randValue = Math.random() * 3.0;
			
			if (randValue < 1.0) {
				// Make an insertion here.
				Insert insert = new Insert(
						(int) (Math.random() * (document.getLength() + 1)),
						randomString(), null);
				
				try {
					document.replace(insert.getOffset(), 0, insert.getText());
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
				
				return insert;
			}
			else if (randValue < 2.0) {
				// Make a deletion here.
				if (document.getLength() == 0)
					continue;
				
				int startOffset = (int) (Math.random() * document.getLength());
				int length = (int) (Math.random() * Math.min(document.getLength() - startOffset, RANDOM_TEXT_MAX_LENGTH)) + 1;
				
				try {
					Delete delete = new Delete(startOffset, length,
							document.getLineOfOffset(startOffset),			// startLine
							document.getLineOfOffset(startOffset + length),	// endLine
							document.get(startOffset, length),				// text
							null);
					
					document.replace(startOffset, length, "");
					
					return delete;
				}
				catch (BadLocationException e) {
					e.printStackTrace();
				}
			}
			else {
				// Make a replacement here.
				if (document.getLength() == 0)
					continue;
				
				int startOffset = (int) (Math.random() * document.getLength());
				int length = (int) (Math.random() * Math.min(document.getLength() - startOffset, RANDOM_TEXT_MAX_LENGTH)) + 1;
				
				String insertedText = randomString();

				try {
					Replace replace = new Replace(startOffset, length,
							document.getLineOfOffset(startOffset),			// startLine
							document.getLineOfOffset(startOffset + length),	// endLine
							insertedText.length(),							// insertionLength
							document.get(startOffset, length),				// deletedText
							insertedText,
							null);
					
					document.replace(startOffset, length, insertedText);
					
					return replace;
				}
				catch (BadLocationException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private String randomString() {
		return randomStringOfLength(1, RANDOM_TEXT_MAX_LENGTH);
	}
	
	private String randomStringOfLength(int minLength, int maxLength) {
		return randomStringOfLength((int) (Math.random() * (maxLength
				- minLength + 1))
				+ minLength);
	}
	
	private String randomStringOfLength(int length) {
		char[] charArray = new char[length];
		for (int i = 0; i < length; ++i) {
			charArray[i] = RANDOM_TEXT.charAt((int)(Math.random() * RANDOM_TEXT.length()));
		}
		
		return new String(charArray);
	}

}
