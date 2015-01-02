package edu.cmu.scs.azurite.model;

import java.util.List;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.fluorite.commands.document.DocChange;
import edu.cmu.scs.fluorite.model.Events;


public interface RuntimeDCListener {
	
	/**
	 * Fired when a new file was opened and the active file was changed. 
	 */
	void activeFileChanged(FileKey fileKey, String snapshot);
	
	/**
	 * Fired when a new runtime document change was recorded.
	 */
	void runtimeDCAdded(RuntimeDC docChange);
	
	/**
	 * Fired when a document change was first added.
	 */
	void documentChangeAdded(DocChange docChange);
	
	/**
	 * Fired when a document change was updated.
	 */
	void documentChangeUpdated(DocChange docChange);
	
	/**
	 * Fired when a document change was amended.
	 */
	void documentChangeAmended(DocChange oldDocChange, DocChange newDocChange);
	
	/**
	 * Fired when some past history files were read.
	 */
	void pastLogsRead(List<Events> listEvents);

}
