package edu.cmu.scs.azurite.commands.runtime;

import static org.junit.Assert.*;

import org.junit.Test;

public class TypeIndexTest {

	@Test
	public void test() {
		assertEquals(0, (new RuntimeInsert()).getTypeIndex());
		assertEquals(1, (new RuntimeDelete()).getTypeIndex());
		assertEquals(2, (new RuntimeReplace()).getTypeIndex());
	}

}
