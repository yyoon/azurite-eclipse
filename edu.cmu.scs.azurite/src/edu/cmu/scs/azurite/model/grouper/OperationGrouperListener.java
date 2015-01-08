package edu.cmu.scs.azurite.model.grouper;

import java.util.List;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;

public interface OperationGrouperListener {

	void collapseIDsUpdated(List<RuntimeDC> dcs, int level, int collapseID);
	
}
