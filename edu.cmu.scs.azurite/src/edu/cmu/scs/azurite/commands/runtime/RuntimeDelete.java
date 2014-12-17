package edu.cmu.scs.azurite.commands.runtime;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.scs.fluorite.commands.document.Delete;

/**
 * @author YoungSeok Yoon
 * 
 */
public class RuntimeDelete extends RuntimeDC {

	private DeleteComponent mDeleteComponent;
	
	public RuntimeDelete() {
		this(new Delete(0, 0, 0, 0, "", null));
	}

	public RuntimeDelete(Delete delete) {
		super(delete);
		
		mDeleteComponent = new DeleteComponent();
		
		getDeleteComponent().initialize(
				Segment.createInitialSegmentFromDelete(getOriginal(), this));
	}

	@Override
	public Delete getOriginal() {
		return (Delete) (super.getOriginal());
	}

	@Override
	public void applyInsert(RuntimeInsert insert) {
		boolean conflict = false;
		conflict |= getDeleteComponent().applyInsert(insert);

		if (conflict) {
			addConflict(insert);
		}
	}

	@Override
	public void applyDelete(RuntimeDelete delete) {
		boolean conflict = false;
		conflict |= getDeleteComponent().applyDelete(delete);

		if (conflict) {
			addConflict(delete);
		}
	}

	@Override
	public void applyReplace(RuntimeReplace replace) {
		boolean conflict = false;
		conflict |= getDeleteComponent().applyReplace(replace);

		if (conflict) {
			addConflict(replace);
		}
	}

	@Override
	public void applyTo(RuntimeDC docChange) {
		docChange.applyDelete(this);
	}

	public Segment getDeleteSegment() {
		return getDeleteComponent().getDeleteSegment();
	}

	private DeleteComponent getDeleteComponent() {
		return mDeleteComponent;
	}

	@Override
	public List<Segment> getAllSegments() {
		ArrayList<Segment> segments = new ArrayList<Segment>();
		segments.add(getDeleteSegment());
		return segments;
	}
	
	@Override
	public int getTypeIndex() {
		return 1;
	}

	@Override
	public String toString() {
		return "[Delete:" + getOriginal().getCommandIndex() + "] \""
				+ getOriginal().getText() + "\"";
	}

	@Override
	public String getHtmlInfo() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("----------\n");
		builder.append(transformToHtmlString(getOriginal().getText()));
		builder.append("\n----------");
		
		return builder.toString().replace("\n", "<br>");
	}

	@Override
	public String getTypeString() {
		return "delete";
	}
	
	@Override
	public String getMarkerMessage() {
		return "Deleted code: \"" + getOriginal().getText() + "\"";
	}
	
}
