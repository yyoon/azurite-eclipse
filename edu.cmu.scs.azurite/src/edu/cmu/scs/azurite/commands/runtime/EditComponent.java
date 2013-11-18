package edu.cmu.scs.azurite.commands.runtime;

/**
 * @author YoungSeok Yoon
 *
 */
public interface EditComponent {
	void initialize(Segment initialSegment);
	boolean applyInsert(RuntimeInsert insert);
	boolean applyDelete(RuntimeDelete delete);
	boolean applyReplace(RuntimeReplace replace);
}
