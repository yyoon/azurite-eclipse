package edu.cmu.scs.azurite.commands.runtime;

import static org.junit.Assert.*;
import static edu.cmu.scs.azurite.commands.runtime.TestHelper.*;

import org.junit.Test;

import edu.cmu.scs.fluorite.commands.document.Delete;
import edu.cmu.scs.fluorite.commands.document.Replace;

public class DeleteReplaceTest {

	private static final String OLD_TEXT = "abcdefgh";
	private static final String NEW_TEXT = "ABCD";

	private static final int OLDLEN = OLD_TEXT.length();
	private static final int NEWLEN = NEW_TEXT.length();
	private static final int LENDIFF = NEWLEN - OLDLEN;

	//                  | existing delete |
	// | new replace |
	@Test
	public void testDR01_01() {
		RuntimeDelete d = new RuntimeDelete(new Delete(20, 10, 0, 0,
				"..........", null));
		RuntimeReplace r = new RuntimeReplace(new Replace(5, OLDLEN, 0, 0,
				NEWLEN, OLD_TEXT, NEW_TEXT, null));
		r.applyTo(d);

		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 20 + LENDIFF, 10,
				".........."));
		assertTrue(checkSegmentEquals(r.getDeleteSegment(), 5, OLDLEN, OLD_TEXT));
		assertEquals(1, r.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(0), 5, NEWLEN,
				NEW_TEXT));
		
		assertEquals(0, d.getConflicts().size());
	}
	
	//    | existing delete |
	// |      new replace      |
	@Test
	public void testDR02_01() {
		RuntimeDelete d = new RuntimeDelete(new Delete(20, 10, 0, 0,
				"..........", null));
		RuntimeReplace r = new RuntimeReplace(new Replace(15, OLDLEN, 0, 0,
				NEWLEN, OLD_TEXT, NEW_TEXT, null));
		r.applyTo(d);

		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 15, 10,
				".........."));
		assertTrue(checkSegmentEquals(r.getDeleteSegment(), 15, OLDLEN, OLD_TEXT));
		assertEquals(1, r.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(0), 15, NEWLEN,
				NEW_TEXT));
		
		assertEquals(1, d.getConflicts().size());
		assertEquals(r, d.getConflicts().get(0));
		assertEquals(5, d.getDeleteSegment().getRelativeOffset());
	}
	
	//               | existing delete |
	// | new replace |
	@Test
	public void testDR02_02() {
		RuntimeDelete d = new RuntimeDelete(new Delete(20, 10, 0, 0,
				"..........", null));
		RuntimeReplace r = new RuntimeReplace(new Replace(20 - OLDLEN, OLDLEN, 0, 0,
				NEWLEN, OLD_TEXT, NEW_TEXT, null));
		r.applyTo(d);

		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 20 + LENDIFF, 10,
				".........."));
		assertTrue(checkSegmentEquals(r.getDeleteSegment(), 20 - OLDLEN, OLDLEN, OLD_TEXT));
		assertEquals(1, r.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(0), 20 - OLDLEN, NEWLEN,
				NEW_TEXT));
		
		assertEquals(0, d.getConflicts().size());
	}
	
	// | existing delete |
	//                   | new replace |
	@Test
	public void testDR02_03() {
		RuntimeDelete d = new RuntimeDelete(new Delete(20, 10, 0, 0,
				"..........", null));
		RuntimeReplace r = new RuntimeReplace(new Replace(20, OLDLEN, 0, 0,
				NEWLEN, OLD_TEXT, NEW_TEXT, null));
		r.applyTo(d);

		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 20, 10,
				".........."));
		assertTrue(checkSegmentEquals(r.getDeleteSegment(), 20, OLDLEN, OLD_TEXT));
		assertEquals(1, r.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(0), 20, NEWLEN,
				NEW_TEXT));
		
		assertEquals(0, d.getConflicts().size());
	}
	
	// | existing delete |
	//                     | new replace |
	@Test
	public void testDR03_01() {
		RuntimeDelete d = new RuntimeDelete(new Delete(20, 10, 0, 0,
				"..........", null));
		RuntimeReplace r = new RuntimeReplace(new Replace(25, OLDLEN, 0, 0,
				NEWLEN, OLD_TEXT, NEW_TEXT, null));
		r.applyTo(d);

		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 20, 10,
				".........."));
		assertTrue(checkSegmentEquals(r.getDeleteSegment(), 25, OLDLEN, OLD_TEXT));
		assertEquals(1, r.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(0), 25, NEWLEN,
				NEW_TEXT));
		
		assertEquals(0, d.getConflicts().size());
	}

}
