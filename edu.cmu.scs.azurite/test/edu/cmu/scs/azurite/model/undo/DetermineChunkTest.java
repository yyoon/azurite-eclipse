package edu.cmu.scs.azurite.model.undo;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import edu.cmu.scs.azurite.commands.runtime.BaseRuntimeDocumentChange;
import edu.cmu.scs.azurite.commands.runtime.RuntimeDelete;
import edu.cmu.scs.azurite.commands.runtime.RuntimeInsert;
import edu.cmu.scs.fluorite.commands.Delete;
import edu.cmu.scs.fluorite.commands.Insert;

public class DetermineChunkTest {
	
	private SelectiveUndoEngine mEngine;
	
	@Before
	public void setUp() {
		mEngine = SelectiveUndoEngine.getInstance(); 
	}

	@Test
	public void adjacentDeleteInsert() {
		Insert i = new Insert(10, "Insert", null);
		RuntimeInsert ri = new RuntimeInsert(i);
		
		Delete d = new Delete(5, 5, 0, 0, "Delet", null);
		RuntimeDelete rd = new RuntimeDelete(d);
		
		rd.applyTo(ri);
		
		List<Chunk> chunks = mEngine.determineChunks(mEngine
				.getAllSegments(new BaseRuntimeDocumentChange[] { ri, rd }));
		
		assertEquals(1, chunks.size());
	}

}
