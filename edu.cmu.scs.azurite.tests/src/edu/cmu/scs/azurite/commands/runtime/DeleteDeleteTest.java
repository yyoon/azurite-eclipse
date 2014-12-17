package edu.cmu.scs.azurite.commands.runtime;

import static org.junit.Assert.*;
import static edu.cmu.scs.azurite.commands.runtime.TestHelper.*;

import org.junit.Test;

import edu.cmu.scs.fluorite.commands.document.Delete;

public class DeleteDeleteTest {
	
	private static final String TEXT = "..........";
	private static int TEXTLEN = TEXT.length();

	//                     | existing delete |
	// | new delete |
	@Test
	public void testDD01_01() {
		RuntimeDelete d1 = new RuntimeDelete(new Delete(20, TEXTLEN, 0, 0,
				TEXT, null));
		RuntimeDelete d2 = new RuntimeDelete(new Delete(5, TEXTLEN, 0, 0, TEXT,
				null));
		d2.applyTo(d1);

		assertTrue(checkSegmentEquals(d1.getDeleteSegment(), 20 - TEXTLEN,
				TEXTLEN, TEXT));
		assertTrue(checkSegmentEquals(d2.getDeleteSegment(), 5, TEXTLEN, TEXT));
	}
	
	//      | existing delete |
	// |         new delete        |
	@Test
	public void testDD02_01() {
		RuntimeDelete d1 = new RuntimeDelete(new Delete(20, TEXTLEN, 0, 0,
				TEXT, null));
		RuntimeDelete d2 = new RuntimeDelete(new Delete(15, TEXTLEN, 0, 0,
				TEXT, null));
		d2.applyTo(d1);

		assertTrue(checkSegmentEquals(d1.getDeleteSegment(), 15, TEXTLEN, TEXT));
		assertTrue(checkSegmentEquals(d2.getDeleteSegment(), 15, TEXTLEN, TEXT));

		assertEquals(1, d1.getConflicts().size());
		assertEquals(d1.getConflicts().get(0), d2);
		assertEquals(5, d1.getDeleteSegment().getRelativeOffset());
	}
	
	//              | existing delete |
	// | new delete |
	@Test
	public void testDD02_02() {
		RuntimeDelete d1 = new RuntimeDelete(new Delete(20, TEXTLEN, 0, 0,
				TEXT, null));
		RuntimeDelete d2 = new RuntimeDelete(new Delete(10, TEXTLEN, 0, 0,
				TEXT, null));
		d2.applyTo(d1);

		assertTrue(checkSegmentEquals(d1.getDeleteSegment(), 10, TEXTLEN, TEXT));
		assertTrue(checkSegmentEquals(d2.getDeleteSegment(), 10, TEXTLEN, TEXT));

/*		assertEquals(1, d1.getConflicts().size());
		assertEquals(d1.getConflicts().get(0), d2);
		assertEquals(10, d1.getDeleteSegment().getRelativeOffset());*/
	}
	
	// | existing delete |
	//                   | new delete |
	@Test
	public void testDD02_03() {
		RuntimeDelete d1 = new RuntimeDelete(new Delete(20, TEXTLEN, 0, 0,
				TEXT, null));
		RuntimeDelete d2 = new RuntimeDelete(new Delete(20, TEXTLEN, 0, 0,
				TEXT, null));
		d2.applyTo(d1);

		assertTrue(checkSegmentEquals(d1.getDeleteSegment(), 20, TEXTLEN, TEXT));
		assertTrue(checkSegmentEquals(d2.getDeleteSegment(), 20, TEXTLEN, TEXT));

/*		assertEquals(1, d1.getConflicts().size());
		assertEquals(d1.getConflicts().get(0), d2);
		assertEquals(0, d1.getDeleteSegment().getRelativeOffset());*/
	}
	
	// | existing delete |
	//                        | new delete |
	@Test
	public void testDD03_01() {
		RuntimeDelete d1 = new RuntimeDelete(new Delete(20, TEXTLEN, 0, 0,
				TEXT, null));
		RuntimeDelete d2 = new RuntimeDelete(new Delete(25, TEXTLEN, 0, 0,
				TEXT, null));
		d2.applyTo(d1);

		assertTrue(checkSegmentEquals(d1.getDeleteSegment(), 20, TEXTLEN, TEXT));
		assertTrue(checkSegmentEquals(d2.getDeleteSegment(), 25, TEXTLEN, TEXT));

		assertEquals(0, d1.getConflicts().size());
		assertEquals(-1, d1.getDeleteSegment().getRelativeOffset());
	}

}
