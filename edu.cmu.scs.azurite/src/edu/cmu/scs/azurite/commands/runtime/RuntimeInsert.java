package edu.cmu.scs.azurite.commands.runtime;

import java.util.List;

import edu.cmu.scs.fluorite.commands.document.Insert;

/**
 * @author YoungSeok Yoon
 *
 */
public class RuntimeInsert extends RuntimeDC {
	
	InsertComponent mInsertComponent;
	
	public RuntimeInsert() {
		this(new Insert(0, "", null));
	}
	
	public RuntimeInsert(Insert insert) {
		super(insert);
		
		mInsertComponent = new InsertComponent();
		
		getInsertComponent().initialize(
				Segment.createInitialSegmentFromInsert(getOriginal(), this));
	}
	
	@Override
	public Insert getOriginal() {
		return (Insert) (super.getOriginal());
	}

	@Override
	public void applyInsert(RuntimeInsert insert) {
		boolean conflict = false;
		conflict |= getInsertComponent().applyInsert(insert);
		
		if (conflict) {
			addConflict(insert);
		}
	}

	@Override
	public void applyDelete(RuntimeDelete delete) {
		boolean conflict = false;
		conflict |= getInsertComponent().applyDelete(delete);
		
		if (conflict) {
			addConflict(delete);
		}
	}

	@Override
	public void applyReplace(RuntimeReplace replace) {
		boolean conflict = false;
		conflict |= getInsertComponent().applyReplace(replace);
		
		if (conflict) {
			addConflict(replace);
		}
	}

	@Override
	public void applyTo(RuntimeDC docChange) {
		docChange.applyInsert(this);
	}
	
	public List<Segment> getInsertSegments() {
		return getInsertComponent().getInsertSegments();
	}
	
	private InsertComponent getInsertComponent() {
		return mInsertComponent;
	}
	
	@Override
	public List<Segment> getAllSegments() {
		return getInsertComponent().getInsertSegments();
	}
	
	@Override
	public int getTypeIndex() {
		return 0;
	}

	@Override
	public String toString() {
		return "[Insert:" + getOriginal().getCommandIndex() + "] \""
				+ getOriginal().getText() + "\"";
	}

	@Override
	public String getHtmlInfo() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("++++++++++\n");
		builder.append(transformToHtmlString(getOriginal().getText()));
		builder.append("\n++++++++++");
		
		return builder.toString().replace("\n", "<br>");
	}

	@Override
	public String getTypeString() {
		return "insert";
	}
	
	@Override
	public String getMarkerMessage() {
		return "Inserted code: \"" + getOriginal().getText() + "\"";
	}
	
}
