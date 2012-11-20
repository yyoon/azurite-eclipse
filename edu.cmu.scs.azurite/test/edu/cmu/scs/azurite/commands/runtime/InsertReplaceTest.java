package edu.cmu.scs.azurite.commands.runtime;

import static org.junit.Assert.*;

import static edu.cmu.scs.azurite.commands.runtime.TestHelper.*;

import org.junit.Test;

import edu.cmu.scs.fluorite.commands.Insert;
import edu.cmu.scs.fluorite.commands.Replace;

public class InsertReplaceTest {

	private static final String OLD_TEXT = "Old Text";
	private static final String NEW_TEXT = "New Text";

	private static final int OLDLEN = OLD_TEXT.length();
	private static final int NEWLEN = NEW_TEXT.length();

	//                     | existing insert |
	// | new replace |
	@Test
	public void testIR01_01() {
		RuntimeInsert i = new RuntimeInsert(new Insert(10, OLD_TEXT, null));
		RuntimeReplace r = new RuntimeReplace(new Replace(0, 5, 0, 0, NEWLEN,
				".....", NEW_TEXT, null));
		r.applyTo(i);

		assertEquals(1, i.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(0),
				10 + NEWLEN - 5, OLDLEN, OLD_TEXT));

		assertTrue(checkSegmentEquals(r.getDeleteSegment(), 0, 5, "....."));
		assertEquals(1, r.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(0),
				0, NEWLEN, NEW_TEXT));
	}

	//                     | existing insert |
	// | new replace |
	@Test
	public void testIR01_02() {
		RuntimeInsert i = new RuntimeInsert(new Insert(10, OLD_TEXT, null));
		RuntimeReplace r = new RuntimeReplace(new Replace(0, 10, 0, 0, NEWLEN,
				"..........", NEW_TEXT, null));
		r.applyTo(i);

		assertEquals(1, i.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(0),
				NEWLEN, OLDLEN, OLD_TEXT));

		assertTrue(checkSegmentEquals(r.getDeleteSegment(), 0, 10, ".........."));
		assertEquals(1, r.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(0),
				0, NEWLEN, NEW_TEXT));
	}

	// | existing insert |
	//   | new replace |
	@Test
	public void testIR02_01() {
		RuntimeInsert i = new RuntimeInsert(new Insert(10, OLD_TEXT, null));
		RuntimeReplace r = new RuntimeReplace(new Replace(12, 5, 0, 0, NEWLEN,
				".....", NEW_TEXT, null));
		r.applyTo(i);

		assertEquals(2, i.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(0),
				10, 2, "Ol"));
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(1),
				12 + NEWLEN, 1, "t"));

		assertTrue(checkSegmentEquals(r.getDeleteSegment(), 12, 5, "....."));
		assertEquals(1, r.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(0),
				12, NEWLEN, NEW_TEXT));
		
		assertEquals(1, i.getConflicts().size());
		assertEquals(r, i.getConflicts().get(0));
	}
	
	// | existing insert |
	//                       | new replace |
	@Test
	public void testIR03_01() {
		RuntimeInsert i = new RuntimeInsert(new Insert(10, OLD_TEXT, null));
		RuntimeReplace r = new RuntimeReplace(new Replace(20, 5, 0, 0, NEWLEN,
				".....", NEW_TEXT, null));
		r.applyTo(i);

		assertEquals(1, i.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(0),
				10, OLDLEN, OLD_TEXT));

		assertTrue(checkSegmentEquals(r.getDeleteSegment(), 20, 5, "....."));
		assertEquals(1, r.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(0),
				20, NEWLEN, NEW_TEXT));
		
		assertEquals(0, i.getConflicts().size());
	}

}
