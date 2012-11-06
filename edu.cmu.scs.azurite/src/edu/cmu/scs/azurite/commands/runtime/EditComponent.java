package edu.cmu.scs.azurite.commands.runtime;

/**
 * @author YoungSeok Yoon
 *
 */
public interface EditComponent {
	public void initialize(Segment initialSegment);
	public boolean applyInsert(RuntimeInsert insert);
	public boolean applyDelete(RuntimeDelete delete);
	public boolean applyReplace(RuntimeReplace replace);
}
