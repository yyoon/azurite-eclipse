package edu.cmu.scs.azurite.commands.runtime;

import static org.junit.Assert.*;
import static edu.cmu.scs.azurite.commands.runtime.TestHelper.*;

import org.junit.Test;

import edu.cmu.scs.fluorite.commands.document.Replace;

public class ReplaceReplaceTest {
	
	private static final String OLD_TEXT1 = "abcdefghijkl";
	private static final String NEW_TEXT1 = "ABCDEFGH";
	
	private static final int OLDLEN1 = OLD_TEXT1.length();
	private static final int NEWLEN1 = NEW_TEXT1.length();
	
	private static final String OLD_TEXT2 = "..........";
	private static final String NEW_TEXT2 = "hello";
	
	private static final int OLDLEN2 = OLD_TEXT2.length();
	private static final int NEWLEN2 = NEW_TEXT2.length();
	private static final int LENDIFF2 = NEWLEN2 - OLDLEN2;

	//                  | existing replace |
	// | new replace |
	@Test
	public void testRR01_01() {
		RuntimeReplace r1 = new RuntimeReplace(new Replace(20, OLDLEN1, 0, 0,
				NEWLEN1, OLD_TEXT1, NEW_TEXT1, null));
		RuntimeReplace r2 = new RuntimeReplace(new Replace(5, OLDLEN2, 0, 0,
				NEWLEN2, OLD_TEXT2, NEW_TEXT2, null));
		r2.applyTo(r1);
		
		assertTrue(checkSegmentEquals(r1.getDeleteSegment(), 20 + LENDIFF2,
				OLDLEN1, OLD_TEXT1));
		assertEquals(1, r1.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r1.getInsertSegments().get(0),
				20 + LENDIFF2, NEWLEN1, NEW_TEXT1));

		assertTrue(checkSegmentEquals(r2.getDeleteSegment(), 5, OLDLEN2,
				OLD_TEXT2));
		assertEquals(1, r2.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r2.getInsertSegments().get(0), 5,
				NEWLEN2, NEW_TEXT2));
	}

	//                  | existing replace |
	// |      new replace      |
	@Test
	public void testRR02_01() {
		RuntimeReplace r1 = new RuntimeReplace(new Replace(20, OLDLEN1, 0, 0,
				NEWLEN1, OLD_TEXT1, NEW_TEXT1, null));
		RuntimeReplace r2 = new RuntimeReplace(new Replace(16, OLDLEN2, 0, 0,
				NEWLEN2, "....ABCDEF", NEW_TEXT2, null));
		r2.applyTo(r1);
		
		assertTrue(checkSegmentEquals(r1.getDeleteSegment(), 16, OLDLEN1,
				OLD_TEXT1));
		assertEquals(2, r1.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r1.getInsertSegments().get(0),
				16, 0, "ABCDEF"));
		assertTrue(checkSegmentEquals(r1.getInsertSegments().get(1),
				16 + NEWLEN2, 2, "GH"));

		assertTrue(checkSegmentEquals(r2.getDeleteSegment(), 16, OLDLEN2,
				"....ABCDEF"));
		assertEquals(1, r2.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r2.getInsertSegments().get(0), 16,
				NEWLEN2, NEW_TEXT2));
		
		assertEquals(1, r1.getConflicts().size());
		assertEquals(r2, r1.getConflicts().get(0));
		assertEquals(4, r1.getDeleteSegment().getRelativeOffset());
		
		assertEquals(4, r1.getInsertSegments().get(0).getRelativeOffset());
		assertEquals(6, r1.getInsertSegments().get(0).getOriginalLength());
		
		assertEquals(2, r2.getDeleteSegment().getSegmentsClosedByMe().size());
		assertEquals(r1.getDeleteSegment(), r2.getDeleteSegment().getSegmentsClosedByMe().get(0));
		assertEquals(r1.getInsertSegments().get(0), r2.getDeleteSegment().getSegmentsClosedByMe().get(1));
	}

	//        | existing replace |
	// |           new replace           |
	@Test
	public void testRR03_01() {
		RuntimeReplace r1 = new RuntimeReplace(new Replace(20, OLDLEN1, 0, 0,
				NEWLEN1, OLD_TEXT1, NEW_TEXT1, null));
		RuntimeReplace r2 = new RuntimeReplace(new Replace(15, 15, 0, 0,
				NEWLEN2, ".....ABCDEFGH..", NEW_TEXT2, null));
		r2.applyTo(r1);
		
		assertTrue(checkSegmentEquals(r1.getDeleteSegment(), 15, OLDLEN1,
				OLD_TEXT1));
		assertEquals(1, r1.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r1.getInsertSegments().get(0),
				15, 0, NEW_TEXT1));

		assertTrue(checkSegmentEquals(r2.getDeleteSegment(), 15, 15,
				".....ABCDEFGH.."));
		assertEquals(1, r2.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r2.getInsertSegments().get(0), 15,
				NEWLEN2, NEW_TEXT2));
		
		assertEquals(1, r1.getConflicts().size());
		assertEquals(r2, r1.getConflicts().get(0));
		assertEquals(5, r1.getDeleteSegment().getRelativeOffset());
		
		assertEquals(5, r1.getInsertSegments().get(0).getRelativeOffset());
		assertEquals(NEWLEN1, r1.getInsertSegments().get(0).getOriginalLength());
		
		assertEquals(2, r2.getDeleteSegment().getSegmentsClosedByMe().size());
		assertEquals(r1.getDeleteSegment(), r2.getDeleteSegment().getSegmentsClosedByMe().get(0));
		assertEquals(r1.getInsertSegments().get(0), r2.getDeleteSegment().getSegmentsClosedByMe().get(1));
	}

	// | existing  replace |
	//    | new replace |
	@Test
	public void testRR04_01() {
		RuntimeReplace r1 = new RuntimeReplace(new Replace(20, OLDLEN1, 0, 0,
				NEWLEN1, OLD_TEXT1, NEW_TEXT1, null));
		RuntimeReplace r2 = new RuntimeReplace(new Replace(23, 3, 0, 0,
				NEWLEN2, "DEF", NEW_TEXT2, null));
		r2.applyTo(r1);
		
		assertTrue(checkSegmentEquals(r1.getDeleteSegment(), 20, OLDLEN1,
				OLD_TEXT1));
		assertEquals(3, r1.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r1.getInsertSegments().get(0), 20, 3,
				"ABC"));
		assertTrue(checkSegmentEquals(r1.getInsertSegments().get(1),
				23, 0, "DEF"));
		assertTrue(checkSegmentEquals(r1.getInsertSegments().get(2),
				20 + 3 + NEWLEN2, 2, "GH"));

		assertTrue(checkSegmentEquals(r2.getDeleteSegment(), 23, 3, "DEF"));
		assertEquals(1, r2.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r2.getInsertSegments().get(0), 23,
				NEWLEN2, NEW_TEXT2));
		
		assertEquals(1, r1.getConflicts().size());
		assertEquals(r2, r1.getConflicts().get(0));
		
		assertEquals(0, r1.getInsertSegments().get(1).getRelativeOffset());
		assertEquals(3, r1.getInsertSegments().get(1).getOriginalLength());
		
		assertEquals(1, r2.getDeleteSegment().getSegmentsClosedByMe().size());
		assertEquals(r1.getInsertSegments().get(1), r2.getDeleteSegment().getSegmentsClosedByMe().get(0));
	}

	// | existing replace |
	//             | new replace |
	@Test
	public void testRR05_01() {
		RuntimeReplace r1 = new RuntimeReplace(new Replace(20, OLDLEN1, 0, 0,
				NEWLEN1, OLD_TEXT1, NEW_TEXT1, null));
		RuntimeReplace r2 = new RuntimeReplace(new Replace(26, OLDLEN2, 0, 0,
				NEWLEN2, "GH........", NEW_TEXT2, null));
		r2.applyTo(r1);
		
		assertTrue(checkSegmentEquals(r1.getDeleteSegment(), 20, OLDLEN1,
				OLD_TEXT1));
		assertEquals(2, r1.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r1.getInsertSegments().get(0), 20, 6,
				"ABCDEF"));
		assertTrue(checkSegmentEquals(r1.getInsertSegments().get(1), 
				26, 0, "GH"));

		assertTrue(checkSegmentEquals(r2.getDeleteSegment(), 26, OLDLEN2,
				"GH........"));
		assertEquals(1, r2.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r2.getInsertSegments().get(0), 26,
				NEWLEN2, NEW_TEXT2));
		
		assertEquals(1, r1.getConflicts().size());
		assertEquals(r2, r1.getConflicts().get(0));
		
		assertEquals(0, r1.getInsertSegments().get(1).getRelativeOffset());
		assertEquals(2, r1.getInsertSegments().get(1).getOriginalLength());
		
		assertEquals(1, r2.getDeleteSegment().getSegmentsClosedByMe().size());
		assertEquals(r1.getInsertSegments().get(1), r2.getDeleteSegment().getSegmentsClosedByMe().get(0));
	}

	// | existing replace |
	//                       | new replace |
	@Test
	public void testRR06_01() {
		RuntimeReplace r1 = new RuntimeReplace(new Replace(20, OLDLEN1, 0, 0,
				NEWLEN1, OLD_TEXT1, NEW_TEXT1, null));
		RuntimeReplace r2 = new RuntimeReplace(new Replace(30, OLDLEN2, 0, 0,
				NEWLEN2, OLD_TEXT2, NEW_TEXT2, null));
		r2.applyTo(r1);

		assertTrue(checkSegmentEquals(r1.getDeleteSegment(), 20, OLDLEN1,
				OLD_TEXT1));
		assertEquals(1, r1.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r1.getInsertSegments().get(0), 20,
				NEWLEN1, NEW_TEXT1));

		assertTrue(checkSegmentEquals(r2.getDeleteSegment(), 30, OLDLEN2,
				OLD_TEXT2));
		assertEquals(1, r2.getInsertSegments().size());
		assertTrue(checkSegmentEquals(r2.getInsertSegments().get(0), 30,
				NEWLEN2, NEW_TEXT2));

		assertEquals(0, r1.getConflicts().size());
	}

}
