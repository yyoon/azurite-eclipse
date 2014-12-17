package edu.cmu.scs.azurite.commands.runtime;

import static org.junit.Assert.*;
import static edu.cmu.scs.azurite.commands.runtime.TestHelper.*;

import org.junit.Test;

import edu.cmu.scs.fluorite.commands.document.Delete;
import edu.cmu.scs.fluorite.commands.document.Insert;

public class InsertDeleteTest {
	
	private static final String TEXT = "SampleText";
	private static int TEXTLEN = TEXT.length();

	//                   | existing insert |
	// | new delete |
	@Test
	public void testID01_01() {
		RuntimeInsert i = new RuntimeInsert(new Insert(10, TEXT, null));
		RuntimeDelete d = new RuntimeDelete(new Delete(0, 5, 0, 0, ".....", null));
		d.applyTo(i);
		
		assertEquals(1, i.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(0),
				5, TEXTLEN, TEXT));
		
		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 0, 5, "....."));
	}
	
	//       | existing insert |
	// | new delete |
	@Test
	public void testID02_01() {
		RuntimeInsert i = new RuntimeInsert(new Insert(10, TEXT, null));
		RuntimeDelete d = new RuntimeDelete(new Delete(5, 10, 0, 0, "..........", null));
		d.applyTo(i);
		
		assertEquals(2, i.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(0),
				5, 0, "Sampl"));
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(1),
				5, 5, "eText"));
		
		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 5, 10, ".........."));
		
		assertEquals(1, i.getConflicts().size());
		assertEquals(d, i.getConflicts().get(0));
		
		assertEquals(5, i.getInsertSegments().get(0).getRelativeOffset());
		assertEquals(5, i.getInsertSegments().get(0).getOriginalLength());
		
		assertEquals(1, d.getDeleteSegment().getSegmentsClosedByMe().size());
		assertEquals(i.getInsertSegments().get(0), d.getDeleteSegment().getSegmentsClosedByMe().get(0));
	}
	
	//       | existing insert |
	// | ---------- new delete ---------- |
	@Test
	public void testID03_01() {
		RuntimeInsert i = new RuntimeInsert(new Insert(10, TEXT, null));
		RuntimeDelete d = new RuntimeDelete(new Delete(5, 20, 0, 0, "....................", null));
		d.applyTo(i);
		
		assertEquals(1, i.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(0), 5, 0, TEXT));
		
		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 5, 20, "...................."));
		
		assertEquals(1, i.getConflicts().size());
		assertEquals(d, i.getConflicts().get(0));
		
		assertEquals(5, i.getInsertSegments().get(0).getRelativeOffset());
		assertEquals(TEXTLEN, i.getInsertSegments().get(0).getOriginalLength());
		
		assertEquals(1, d.getDeleteSegment().getSegmentsClosedByMe().size());
		assertEquals(i.getInsertSegments().get(0), d.getDeleteSegment().getSegmentsClosedByMe().get(0));
	}
	
	// | existing insert |
	//   | new delete |
	@Test
	public void testID04_01() {
		RuntimeInsert i = new RuntimeInsert(new Insert(10, TEXT, null));
		RuntimeDelete d = new RuntimeDelete(new Delete(12, 6, 0, 0, "......", null));
		d.applyTo(i);
		
		assertEquals(3, i.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(0),
				10, 2, "Sa"));
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(1),
				12, 0, "mpleTe"));
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(2),
				12, 2, "xt"));
		
		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 12, 6, "......"));
		
		assertEquals(1, i.getConflicts().size());
		assertEquals(d, i.getConflicts().get(0));
		
		assertEquals(0, i.getInsertSegments().get(1).getRelativeOffset());
		assertEquals(6, i.getInsertSegments().get(1).getOriginalLength());
		
		assertEquals(1, d.getDeleteSegment().getSegmentsClosedByMe().size());
		assertEquals(i.getInsertSegments().get(1), d.getDeleteSegment().getSegmentsClosedByMe().get(0));
	}
	
	// | existing insert |
	//            | new delete |
	@Test
	public void testID05_01() {
		RuntimeInsert i = new RuntimeInsert(new Insert(10, TEXT, null));
		RuntimeDelete d = new RuntimeDelete(new Delete(15, 10, 0, 0, "..........", null));
		d.applyTo(i);
		
		assertEquals(2, i.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(0),
				10, 5, "Sampl"));
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(1),
				15, 0, "eText"));
		
		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 15, 10, ".........."));
		
		assertEquals(1, i.getConflicts().size());
		assertEquals(d, i.getConflicts().get(0));
		
		assertEquals(0, i.getInsertSegments().get(1).getRelativeOffset());
		assertEquals(5, i.getInsertSegments().get(1).getOriginalLength());
		
		assertEquals(1, d.getDeleteSegment().getSegmentsClosedByMe().size());
		assertEquals(i.getInsertSegments().get(1), d.getDeleteSegment().getSegmentsClosedByMe().get(0));
	}
	
	// | existing insert |
	//                        | new delete |
	@Test
	public void testID06_01() {
		RuntimeInsert i = new RuntimeInsert(new Insert(10, TEXT, null));
		RuntimeDelete d = new RuntimeDelete(new Delete(25, 10, 0, 0, "..........", null));
		d.applyTo(i);
		
		assertEquals(1, i.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i.getInsertSegments().get(0),
				10, TEXTLEN, TEXT));
		
		assertTrue(checkSegmentEquals(d.getDeleteSegment(), 25, 10, ".........."));
	}

}