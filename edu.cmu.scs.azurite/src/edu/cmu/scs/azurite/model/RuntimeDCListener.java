package edu.cmu.scs.azurite.model;

import java.util.List;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.fluorite.commands.BaseDocumentChangeEvent;
import edu.cmu.scs.fluorite.model.Events;


public interface RuntimeDCListener {
	
	/**
	 * Fired when a new file was opened and the active file was changed. 
	 */
	public void activeFileChanged(String projectName, String filePath);
	
	/**
	 * Fired when a new runtime document change was recorded.
	 */
	public void runtimeDCAdded(RuntimeDC docChange);
	
	/**
	 * Fired when a document change was first added.
	 */
	public void documentChangeAdded(BaseDocumentChangeEvent docChange);
	
	/**
	 * Fired when a document change was updated.
	 */
	public void documentChangeUpdated(BaseDocumentChangeEvent docChange);
	
	/**
	 * Fired when some past history files were read.
	 */
	public void pastLogsRead(List<Events> listEvents);

}
