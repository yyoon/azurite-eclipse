package edu.cmu.scs.azurite.commands.runtime;

public class TestHelper {

	static boolean checkSegmentEquals(Segment segment, int offset, int length,
			String text) {
		return segment != null && segment.getOffset() == offset
				&& segment.getLength() == length
				&& segment.getText().equals(text);
	}
	
}
