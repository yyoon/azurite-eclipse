package edu.cmu.scs.azurite.model;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;


public interface RuntimeDCListener {
	
	/**
	 * Fired when a new file was opened and the active file was changed. 
	 */
	public void activeFileChanged(String projectName, String filePath);
	
	/**
	 * Fired when a new runtime document change was recorded.
	 */
	public void runtimeDCAdded(RuntimeDC docChange);

}
