package edu.cmu.scs.azurite.model.grouper;

public interface IChangeInformation {
	
	ChangeType getChangeType();
	
	boolean shouldBeMerged(int level, IChangeInformation nextChange);
	
}
