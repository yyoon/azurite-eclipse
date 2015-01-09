package edu.cmu.scs.azurite.model.grouper;

import edu.cmu.scs.fluorite.commands.document.DocChange;

public interface IChangeInformation {
	
	ChangeType getChangeType();
	
	String getChangeTypeString();
	
	boolean shouldBeMerged(int level, IChangeInformation nextChange);
	
	/**
	 * NOTE: this may return null.
	 * @return the merged change object.
	 */
	DocChange getMergedChange();
	
}
