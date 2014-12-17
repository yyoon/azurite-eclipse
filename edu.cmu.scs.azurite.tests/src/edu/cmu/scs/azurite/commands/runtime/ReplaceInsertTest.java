package edu.cmu.scs.azurite.commands.runtime;

import static org.junit.Assert.*;
import static edu.cmu.scs.azurite.commands.runtime.TestHelper.*;

import org.junit.Test;

import edu.cmu.scs.fluorite.commands.document.Insert;
import edu.cmu.scs.fluorite.commands.document.Replace;

public class ReplaceInsertTest {

	private static final String OLD_TEXT = "abcdefgh";
	private static final String NEW_TEXT = "ABCD";

	private static final int OLDLEN = OLD_TEXT.length();
	private static final int NEWLEN = NEW_TEXT.length();
	
	private static final String INS_TEXT = "Hello";
	private static final int INSLEN = INS_TEXT.length();

	//                   | existing replace |
	// | new insert |
	@Test
	public void testRI01_01() {
		RuntimeReplace r = new RuntimeReplace(new Replace(20, OLDLEN, 0, 0,
				NEWLEN, OLD_TEXT, NEW_TEXT, null));
		RuntimeInsert i = new RuntimeInsert(new Insert(10, INS_TEXT, null));
		i.applyTo(r);
		
		assertTrue(checkSegmentEquals(r.getDeleteSegment(), 20 + INSLEN,
				OLDLEN, OLD_TEXT));
		assertEquals(1, r.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(0),
				20 + INSLEN, NEWLEN, NEW_TEXT));
		
		assertEquals(1, i.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(0), 10, INSLEN,
				INS_TEXT));
	}

	// |      existing replace      |
	//        | new insert |
	@Test
	public void testRI02_01() {
		RuntimeReplace r = new RuntimeReplace(new Replace(20, OLDLEN, 0, 0,
				NEWLEN, OLD_TEXT, NEW_TEXT, null));
		RuntimeInsert i = new RuntimeInsert(new Insert(22, INS_TEXT, null));
		i.applyTo(r);
		
		assertTrue(checkSegmentEquals(r.getDeleteSegment(), 20,
				OLDLEN, OLD_TEXT));
		assertEquals(2, r.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(0),
				20, 2, "AB"));
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(1),
				20 + 2 + INSLEN, 2, "CD"));
		
		assertEquals(1, i.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(0), 22, INSLEN,
				INS_TEXT));
		
		assertEquals(1, r.getConflicts().size());
		assertEquals(i, r.getConflicts().get(0));
	}

	// | existing replace |
	//                       | new insert |
	@Test
	public void testRI03_01() {
		RuntimeReplace r = new RuntimeReplace(new Replace(20, OLDLEN, 0, 0,
				NEWLEN, OLD_TEXT, NEW_TEXT, null));
		RuntimeInsert i = new RuntimeInsert(new Insert(30, INS_TEXT, null));
		i.applyTo(r);
		
		assertTrue(checkSegmentEquals(r.getDeleteSegment(), 20,
				OLDLEN, OLD_TEXT));
		assertEquals(1, r.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(0),
				20, NEWLEN, NEW_TEXT));
		
		assertEquals(1, i.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(0), 30, INSLEN,
				INS_TEXT));
	}
	
}
