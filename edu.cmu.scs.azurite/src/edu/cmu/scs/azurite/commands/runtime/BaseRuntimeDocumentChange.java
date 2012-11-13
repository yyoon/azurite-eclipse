package edu.cmu.scs.azurite.commands.runtime;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.scs.fluorite.commands.BaseDocumentChangeEvent;
import edu.cmu.scs.fluorite.commands.Delete;
import edu.cmu.scs.fluorite.commands.Insert;
import edu.cmu.scs.fluorite.commands.Replace;

/**
 * @author YoungSeok Yoon
 *
 */
public abstract class BaseRuntimeDocumentChange {

	private BaseDocumentChangeEvent mOriginal;
	
	private List<BaseRuntimeDocumentChange> mConflicts;
	
	public static BaseRuntimeDocumentChange createRuntimeDocumentChange(BaseDocumentChangeEvent original) {
		if (original instanceof Insert) {
			return new RuntimeInsert((Insert) original);
		}
		else if (original instanceof Delete) {
			return new RuntimeDelete((Delete) original);
		}
		else if (original instanceof Replace) {
			return new RuntimeReplace((Replace) original);
		}
		else {
			throw new IllegalArgumentException("argument should be one of Insert / Delete / Replace");
		}
	}
	
	protected BaseRuntimeDocumentChange(BaseDocumentChangeEvent original) {
		mOriginal = original;
		
		mConflicts = new ArrayList<BaseRuntimeDocumentChange>();
	}
	
	public BaseDocumentChangeEvent getOriginal() {
		return mOriginal;
	}
	
	public abstract void applyInsert(RuntimeInsert insert);
	
	public abstract void applyDelete(RuntimeDelete delete);
	
	public abstract void applyReplace(RuntimeReplace replace);
	
	public abstract void applyTo(BaseRuntimeDocumentChange docChange);
	
	public List<BaseRuntimeDocumentChange> getConflicts() {
		return mConflicts;
	}
	
	protected void addConflict(BaseRuntimeDocumentChange docChange) {
		mConflicts.add(docChange);
	}
	
	public abstract List<Segment> getAllSegments();
	
	/**
	 * This type index is used inside the timeline view.
	 * The timeline.js code defines:
	 * 
	 * // Constants
	 * var INSERTION = 0;
	 * var DELETION = 1;
	 * var REPLACEMENT = 2;
	 * 
	 * So that it can color those things differently.
	 * 
	 * @return 0 if insertion, 1 if deletion, and 2 if replacement.
	 */
	public abstract int getTypeIndex();
}
