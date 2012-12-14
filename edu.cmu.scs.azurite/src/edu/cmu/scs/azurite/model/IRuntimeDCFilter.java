package edu.cmu.scs.azurite.model;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;

public interface IRuntimeDCFilter {

	boolean filter(RuntimeDC runtimeDC);
	
}
