package edu.cmu.scs.azurite.commands.runtime;

import static org.junit.Assert.*;

import java.util.Map;

import org.eclipse.ui.IEditorPart;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Element;

import edu.cmu.scs.fluorite.commands.BaseDocumentChangeEvent;
import edu.cmu.scs.fluorite.commands.Delete;
import edu.cmu.scs.fluorite.commands.ICommand;
import edu.cmu.scs.fluorite.commands.Insert;
import edu.cmu.scs.fluorite.commands.Replace;

public class BaseRuntimeDocumentChangeTest {
	
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void testCreateInsert() {
		Insert original = new Insert(0, "", null);
		BaseRuntimeDocumentChange runtime = BaseRuntimeDocumentChange
				.createRuntimeDocumentChange(original);
		
		assertEquals(
				RuntimeInsert.class.getSimpleName(),
				runtime.getClass().getSimpleName());
		
		assertEquals(original, runtime.getOriginal());
	}

	@Test
	public void testCreateDelete() {
		Delete original = new Delete(0, 0, 0, 0, "", null);
		BaseRuntimeDocumentChange runtime = BaseRuntimeDocumentChange
				.createRuntimeDocumentChange(original);
		
		assertEquals(
				RuntimeDelete.class.getSimpleName(),
				runtime.getClass().getSimpleName());
		
		assertEquals(original, runtime.getOriginal());
	}

	@Test
	public void testCreateReplace() {
		Replace original = new Replace(0, 0, 0, 0, 0, "", "", null);
		BaseRuntimeDocumentChange runtime = BaseRuntimeDocumentChange
				.createRuntimeDocumentChange(original);
		
		assertEquals(
				RuntimeReplace.class.getSimpleName(),
				runtime.getClass().getSimpleName());
		
		assertEquals(original, runtime.getOriginal());
	}
	
	@Test
	public void testCreateInvalid() {
		BaseDocumentChangeEvent event = new BaseDocumentChangeEvent() {
			public boolean execute(IEditorPart target) { return false; }
			public void dump() {}
			public Map<String, String> getAttributesMap() { return null; }
			public Map<String, String> getDataMap() { return null; }
			public String getCommandType() { return null; }
			public ICommand createFrom(Element commandElement) { return null; }
			public String getName() { return null; }
			public String getDescription() { return null; }
			public String getCategory() { return null; }
			public String getCategoryID() { return null; }
			public boolean combine(ICommand anotherCommand) { return false; }
		};
		
		exception.expect(IllegalArgumentException.class);
		BaseRuntimeDocumentChange.createRuntimeDocumentChange(event);
	}
}
