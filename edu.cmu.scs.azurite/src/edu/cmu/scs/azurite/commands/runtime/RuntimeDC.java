package edu.cmu.scs.azurite.commands.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import edu.cmu.scs.azurite.model.FileKey;
import edu.cmu.scs.fluorite.commands.BaseDocumentChangeEvent;
import edu.cmu.scs.fluorite.commands.Delete;
import edu.cmu.scs.fluorite.commands.Insert;
import edu.cmu.scs.fluorite.commands.Replace;

/**
 * @author YoungSeok Yoon
 *
 */
public abstract class RuntimeDC {

	private BaseDocumentChangeEvent mOriginal;
	
	private List<RuntimeDC> mConflicts;
	
	private FileKey mBelongsTo;
	
	public static RuntimeDC createRuntimeDocumentChange(BaseDocumentChangeEvent original) {
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
	
	protected RuntimeDC(BaseDocumentChangeEvent original) {
		mOriginal = original;
		
		mConflicts = new ArrayList<RuntimeDC>();
	}
	
	public BaseDocumentChangeEvent getOriginal() {
		return mOriginal;
	}
	
	public abstract void applyInsert(RuntimeInsert insert);
	
	public abstract void applyDelete(RuntimeDelete delete);
	
	public abstract void applyReplace(RuntimeReplace replace);
	
	public abstract void applyTo(RuntimeDC docChange);
	
	public List<RuntimeDC> getConflicts() {
		return mConflicts;
	}
	
	protected void addConflict(RuntimeDC docChange) {
		mConflicts.add(docChange);
	}
	
	public void setBelongsTo(FileKey belongsTo) {
		mBelongsTo = belongsTo;
	}
	
	public FileKey getBelongsTo() {
		return mBelongsTo;
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
	
	private static Comparator<RuntimeDC> commandIDComparator;
	
	/**
	 * Returns the singleton comparator objects which compares the runtime
	 * document changes based on the command IDs of their original events.
	 * @return comparator object.
	 */
	public static Comparator<RuntimeDC> getCommandIDComparator() {
		if (commandIDComparator == null) {
			commandIDComparator = new Comparator<RuntimeDC>() {

				@Override
				public int compare(RuntimeDC lhs,
						RuntimeDC rhs) {
					int lindex = lhs.getOriginal().getCommandIndex();
					int rindex = rhs.getOriginal().getCommandIndex();
					return new Integer(lindex).compareTo(rindex);
				}
				
			};
		}
		
		return commandIDComparator;
	}
}
