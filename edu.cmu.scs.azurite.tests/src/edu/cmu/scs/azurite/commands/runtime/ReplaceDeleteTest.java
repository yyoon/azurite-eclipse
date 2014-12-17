package edu.cmu.scs.azurite.commands.runtime;

import static org.junit.Assert.*;
import static edu.cmu.scs.azurite.commands.runtime.TestHelper.*;

import org.junit.Test;

import edu.cmu.scs.fluorite.commands.document.Delete;
import edu.cmu.scs.fluorite.commands.document.Replace;

public class ReplaceDeleteTest {

	private static final String OLD_TEXT = "abcdefgh";
	private static final String NEW_TEXT = "ABCD";

	private static final int OLDLEN = OLD_TEXT.length();
	private static final int NEWLEN = NEW_TEXT.length();

	//                 | existing replace |
	// | new delete |
	@Test
	public void testRD01_01() {
		RuntimeReplace r = new RuntimeReplace(new Replace(20, OLDLEN, 0, 0,
				NEWLEN, OLD_TEXT, NEW_TEXT, null));
		RuntimeDelete d = new RuntimeDelete(new Delete(10, 5, 0, 0, ".....",
				null));
		d.applyTo(r);

		assertTrue(checkSegmentEquals(r.getDeleteSegment(), 15, OLDLEN,
				OLD_TEXT));
		assertEquals(1, r.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(0), 15, NEWLEN,
				NEW_TEXT));

		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 10, 5, "....."));
		
		assertEquals(0, r.getConflicts().size());
	}

	//        | existing replace |
	// | new delete |
	@Test
	public void testRD02_01() {
		RuntimeReplace r = new RuntimeReplace(new Replace(20, OLDLEN, 0, 0,
				NEWLEN, OLD_TEXT, NEW_TEXT, null));
		RuntimeDelete d = new RuntimeDelete(new Delete(18, 5, 0, 0, ".....",
				null));
		d.applyTo(r);

		assertTrue(checkSegmentEquals(r.getDeleteSegment(), 18, OLDLEN,
				OLD_TEXT));
		assertEquals(2, r.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(0), 18, 0, "ABC"));
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(1), 18, 1, "D"));

		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 18, 5, "....."));
		
		assertEquals(1, r.getConflicts().size());
		assertEquals(d, r.getConflicts().get(0));
		assertEquals(2, r.getDeleteSegment().getRelativeOffset());
		
		assertEquals(2, r.getInsertSegments().get(0).getRelativeOffset());
		assertEquals(3, r.getInsertSegments().get(0).getOriginalLength());
		
		assertEquals(2, d.getDeleteSegment().getSegmentsClosedByMe().size());
		assertEquals(r.getDeleteSegment(), d.getDeleteSegment().getSegmentsClosedByMe().get(0));
		assertEquals(r.getInsertSegments().get(0), d.getDeleteSegment().getSegmentsClosedByMe().get(1));
	}

	//        | existing replace |
	// |           new delete           |
	@Test
	public void testRD03_01() {
		RuntimeReplace r = new RuntimeReplace(new Replace(20, OLDLEN, 0, 0,
				NEWLEN, OLD_TEXT, NEW_TEXT, null));
		RuntimeDelete d = new RuntimeDelete(new Delete(15, 10, 0, 0, ".....ABCD.",
				null));
		d.applyTo(r);

		assertTrue(checkSegmentEquals(r.getDeleteSegment(), 15, OLDLEN,
				OLD_TEXT));
		assertEquals(1, r.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(0),
				15, 0, NEW_TEXT));

		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 15, 10, ".....ABCD."));
		
		assertEquals(1, r.getConflicts().size());
		assertEquals(d, r.getConflicts().get(0));
		assertEquals(5, r.getDeleteSegment().getRelativeOffset());
		
		assertEquals(5, r.getInsertSegments().get(0).getRelativeOffset());
		assertEquals(NEWLEN, r.getInsertSegments().get(0).getOriginalLength());
		
		assertEquals(2, d.getDeleteSegment().getSegmentsClosedByMe().size());
		assertEquals(r.getDeleteSegment(), d.getDeleteSegment().getSegmentsClosedByMe().get(0));
		assertEquals(r.getInsertSegments().get(0), d.getDeleteSegment().getSegmentsClosedByMe().get(1));
	}

	// | existing replace |
	//    | new delete |
	@Test
	public void testRD04_01() {
		RuntimeReplace r = new RuntimeReplace(new Replace(20, OLDLEN, 0, 0,
				NEWLEN, OLD_TEXT, NEW_TEXT, null));
		RuntimeDelete d = new RuntimeDelete(new Delete(21, 2, 0, 0, "BC", null));
		d.applyTo(r);

		assertTrue(checkSegmentEquals(r.getDeleteSegment(), 20, OLDLEN,
				OLD_TEXT));
		assertEquals(3, r.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(0), 20, 1, "A"));
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(1), 21, 0, "BC"));
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(2), 21, 1, "D"));

		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 21, 2, "BC"));
		
		assertEquals(1, r.getConflicts().size());
		assertEquals(d, r.getConflicts().get(0));
		
		assertEquals(0, r.getInsertSegments().get(1).getRelativeOffset());
		assertEquals(2, r.getInsertSegments().get(1).getOriginalLength());
		
		assertEquals(1, d.getDeleteSegment().getSegmentsClosedByMe().size());
		assertEquals(r.getInsertSegments().get(1), d.getDeleteSegment().getSegmentsClosedByMe().get(0));
	}

	// | existing replace |
	//             | new delete |
	@Test
	public void testRD05_01() {
		RuntimeReplace r = new RuntimeReplace(new Replace(20, OLDLEN, 0, 0,
				NEWLEN, OLD_TEXT, NEW_TEXT, null));
		RuntimeDelete d = new RuntimeDelete(new Delete(22, 6, 0, 0, "CD....", null));
		d.applyTo(r);

		assertTrue(checkSegmentEquals(r.getDeleteSegment(), 20, OLDLEN,
				OLD_TEXT));
		assertEquals(2, r.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(0), 20, 2, "AB"));
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(1), 22, 0, "CD"));

		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 22, 6, "CD...."));
		
		assertEquals(1, r.getConflicts().size());
		assertEquals(d, r.getConflicts().get(0));
		
		assertEquals(0, r.getInsertSegments().get(1).getRelativeOffset());
		assertEquals(2, r.getInsertSegments().get(1).getOriginalLength());
		
		assertEquals(1, d.getDeleteSegment().getSegmentsClosedByMe().size());
		assertEquals(r.getInsertSegments().get(1), d.getDeleteSegment().getSegmentsClosedByMe().get(0));
	}

	// | existing replace |
	//                       | new delete |
	@Test
	public void testRD06_01() {
		RuntimeReplace r = new RuntimeReplace(new Replace(20, OLDLEN, 0, 0,
				NEWLEN, OLD_TEXT, NEW_TEXT, null));
		RuntimeDelete d = new RuntimeDelete(new Delete(30, 5, 0, 0, ".....",
				null));
		d.applyTo(r);

		assertTrue(checkSegmentEquals(r.getDeleteSegment(), 20, OLDLEN,
				OLD_TEXT));
		assertEquals(1, r.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r.getInsertSegments().get(0), 20, NEWLEN,
				NEW_TEXT));

		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 30, 5, "....."));

		assertEquals(0, r.getConflicts().size());
	}

}
