package edu.cmu.scs.azurite.commands.runtime;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import edu.cmu.scs.fluorite.commands.ICommand;
import edu.cmu.scs.fluorite.commands.document.Delete;
import edu.cmu.scs.fluorite.commands.document.DocChange;
import edu.cmu.scs.fluorite.commands.document.Insert;
import edu.cmu.scs.fluorite.commands.document.Range;
import edu.cmu.scs.fluorite.commands.document.Replace;

public class BaseRuntimeDocumentChangeTest {
	
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void testCreateInsert() {
		Insert original = new Insert(0, "", null);
		RuntimeDC runtime = RuntimeDC
				.createRuntimeDocumentChange(original);
		
		assertEquals(
				RuntimeInsert.class.getSimpleName(),
				runtime.getClass().getSimpleName());
		
		assertEquals(original, runtime.getOriginal());
	}

	@Test
	public void testCreateDelete() {
		Delete original = new Delete(0, 0, 0, 0, "", null);
		RuntimeDC runtime = RuntimeDC
				.createRuntimeDocumentChange(original);
		
		assertEquals(
				RuntimeDelete.class.getSimpleName(),
				runtime.getClass().getSimpleName());
		
		assertEquals(original, runtime.getOriginal());
	}

	@Test
	public void testCreateReplace() {
		Replace original = new Replace(0, 0, 0, 0, 0, "", "", null);
		RuntimeDC runtime = RuntimeDC
				.createRuntimeDocumentChange(original);
		
		assertEquals(
				RuntimeReplace.class.getSimpleName(),
				runtime.getClass().getSimpleName());
		
		assertEquals(original, runtime.getOriginal());
	}
	
	@Test
	public void testCreateInvalid() {
		DocChange event = new DocChange() {
			public boolean execute(IEditorPart target) { return false; }
			public void dump() {}
			public Map<String, String> getAttributesMap() { return null; }
			public Map<String, String> getDataMap() { return null; }
			public String getCommandType() { return null; }
			public String getName() { return null; }
			public String getDescription() { return null; }
			public String getCategory() { return null; }
			public String getCategoryID() { return null; }
			public boolean combine(ICommand anotherCommand) { return false; }
			public void apply(IDocument doc) {}
			public String apply(String original) { return original; }
			public void apply(StringBuilder builder) {}
			public void applyInverse(IDocument doc) {}
			public String applyInverse(String original) { return original; }
			public void applyInverse(StringBuilder builder) {}
			public double getY1() { return 0; }
			public double getY2() { return 0; }
			public Range getDeletionRange() { return null; }
			public String getDeletedText() { return null; }
			public Range getInsertionRange() { return null; }
			public String getInsertedText() { return null; }
		};
		
		exception.expect(IllegalArgumentException.class);
		RuntimeDC.createRuntimeDocumentChange(event);
	}
}
