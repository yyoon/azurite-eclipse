package edu.cmu.scs.azurite.commands.runtime;

import static org.junit.Assert.*;
import static edu.cmu.scs.azurite.commands.runtime.TestHelper.*;

import org.junit.Test;

import edu.cmu.scs.fluorite.commands.document.Insert;

public class InsertInsertTest {
	
	private static final String OLD_TEXT = "Old Text";
	private static final String NEW_TEXT = "New Text";
	
	private static int OLDLEN = OLD_TEXT.length();
	private static int NEWLEN = NEW_TEXT.length();
	
	//                   | existing insert |
	// | new insert |
	@Test
	public void testII01_01() {
		RuntimeInsert i1 = new RuntimeInsert(new Insert(0, OLD_TEXT, null));
		RuntimeInsert i2 = new RuntimeInsert(new Insert(0, NEW_TEXT, null));
		i2.applyTo(i1);
		
		assertEquals(1, i1.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i1.getInsertSegments().get(0),
				NEWLEN, OLDLEN, OLD_TEXT));
		
		assertEquals(1, i2.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i2.getInsertSegments().get(0),
				0, NEWLEN, NEW_TEXT));
	}
	
	//                   | existing insert |
	// | new insert |
	@Test
	public void testII01_02() {
		RuntimeInsert i1 = new RuntimeInsert(new Insert(10, OLD_TEXT, null));
		RuntimeInsert i2 = new RuntimeInsert(new Insert(0, NEW_TEXT, null));
		i2.applyTo(i1);
		
		assertEquals(1, i1.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i1.getInsertSegments().get(0),
				10 + NEWLEN, OLDLEN, OLD_TEXT));
		
		assertEquals(1, i2.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i2.getInsertSegments().get(0),
				0, NEWLEN, NEW_TEXT));
	}
	
	//                   | existing insert |
	// | new insert |
	@Test
	public void testII01_03() {
		RuntimeInsert i1 = new RuntimeInsert(new Insert(100, OLD_TEXT, null));
		RuntimeInsert i2 = new RuntimeInsert(new Insert(5, NEW_TEXT, null));
		i2.applyTo(i1);
		
		assertEquals(1, i1.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i1.getInsertSegments().get(0),
				100 + NEWLEN, OLDLEN, OLD_TEXT));
		
		assertEquals(1, i2.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i2.getInsertSegments().get(0),
				5, NEWLEN, NEW_TEXT));
	}
	
	// | ----- existing insert ----- |
	//        | new insert |
	@Test
	public void testII02_01() {
		RuntimeInsert i1 = new RuntimeInsert(new Insert(0, OLD_TEXT, null));
		RuntimeInsert i2 = new RuntimeInsert(new Insert(4, NEW_TEXT, null));
		i2.applyTo(i1);
		
		assertEquals(2, i1.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i1.getInsertSegments().get(0),
				0, 4, "Old "));
		assertTrue(checkSegmentEquals(i1.getInsertSegments().get(1),
				4 + NEWLEN, 4, "Text"));
		
		assertEquals(1, i2.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i2.getInsertSegments().get(0),
				4, NEWLEN, NEW_TEXT));
		
		RuntimeInsert i3 = new RuntimeInsert(new Insert(0, NEW_TEXT, null));
		i3.applyTo(i1);
		i3.applyTo(i2);
		
		assertEquals(2, i1.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i1.getInsertSegments().get(0),
				NEWLEN, 4, "Old "));
		assertTrue(checkSegmentEquals(i1.getInsertSegments().get(1),
				NEWLEN + 4 + NEWLEN, 4, "Text"));
		
		assertEquals(1, i2.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i2.getInsertSegments().get(0),
				4 + NEWLEN, NEWLEN, NEW_TEXT));
		
		RuntimeInsert i4 = new RuntimeInsert(new Insert(NEWLEN + 2, NEW_TEXT, null));
		i4.applyTo(i1);
		i4.applyTo(i2);
		i4.applyTo(i3);
		
		assertEquals(3, i1.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i1.getInsertSegments().get(0),
				NEWLEN, 2, "Ol"));
		assertTrue(checkSegmentEquals(i1.getInsertSegments().get(1),
				NEWLEN + 2 + NEWLEN, 2, "d "));
		assertTrue(checkSegmentEquals(i1.getInsertSegments().get(2),
				NEWLEN + 4 + NEWLEN + NEWLEN, 4, "Text"));
		
		assertEquals(1, i2.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i2.getInsertSegments().get(0),
				NEWLEN + 4 + NEWLEN, NEWLEN, NEW_TEXT));
	}
	
	// | existing insert |
	//                        | new insert |
	@Test
	public void testII03_01() {
		RuntimeInsert i1 = new RuntimeInsert(new Insert(0, OLD_TEXT, null));
		RuntimeInsert i2 = new RuntimeInsert(new Insert(OLDLEN, NEW_TEXT, null));
		i2.applyTo(i1);
		
		assertEquals(1, i1.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i1.getInsertSegments().get(0),
				0, OLDLEN, OLD_TEXT));
		
		assertEquals(1, i2.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i2.getInsertSegments().get(0),
				OLDLEN, NEWLEN, NEW_TEXT));
	}
	
	// | existing insert |
	//                        | new insert |
	@Test
	public void testII03_02() {
		RuntimeInsert i1 = new RuntimeInsert(new Insert(0, OLD_TEXT, null));
		RuntimeInsert i2 = new RuntimeInsert(new Insert(OLDLEN + 10, NEW_TEXT, null));
		i2.applyTo(i1);
		
		assertEquals(1, i1.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i1.getInsertSegments().get(0),
				0, OLDLEN, OLD_TEXT));
		
		assertEquals(1, i2.getInsertSegments().size());
		assertTrue(checkSegmentEquals(i2.getInsertSegments().get(0),
				OLDLEN + 10, NEWLEN, NEW_TEXT));
	}
	
}
