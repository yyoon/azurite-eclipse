package edu.cmu.scs.azurite.commands.runtime;

import static org.junit.Assert.*;

import static edu.cmu.scs.azurite.commands.runtime.TestHelper.*;

import org.junit.Test;

import edu.cmu.scs.fluorite.commands.Delete;
import edu.cmu.scs.fluorite.commands.Insert;

public class DeleteInsertTest {
	
	private static final String TEXT = "SampleText";
	private static int TEXTLEN = TEXT.length();

	//                 | existing delete |
	// | new insert |
	@Test
	public void testDI01_01() {
		RuntimeDelete d = new RuntimeDelete(new Delete(10, 10, 0, 0,
				"..........", null));
		RuntimeInsert i = new RuntimeInsert(new Insert(5, TEXT, null));
		i.applyTo(d);

		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 10 + TEXTLEN, 10,
				".........."));
		assertEquals(1, i.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(0), 5, TEXTLEN,
				TEXT));
	}
	
	// | existing delete |
	// | new insert |
	// This case, the insert offset is the same as delete offset.
	// As of now, it doesn't count as a conflict.
	@Test
	public void testDI02_01() {
		RuntimeDelete d = new RuntimeDelete(new Delete(10, 10, 0, 0,
				"..........", null));
		RuntimeInsert i = new RuntimeInsert(new Insert(10, TEXT, null));
		i.applyTo(d);

		assertEquals(0, i.getConflicts().size());
		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 10 + TEXTLEN, 10,
				".........."));
		assertEquals(1, i.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(0), 10,
				TEXTLEN, TEXT));
	}
	
	@Test
	public void testDI03_01() {
		RuntimeDelete d = new RuntimeDelete(new Delete(10, 10, 0, 0,
				"..........", null));
		RuntimeInsert i = new RuntimeInsert(new Insert(15, TEXT, null));
		i.applyTo(d);

		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 10, 10,
				".........."));
		assertEquals(1, i.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(0), 15,
				TEXTLEN, TEXT));
	}
}
