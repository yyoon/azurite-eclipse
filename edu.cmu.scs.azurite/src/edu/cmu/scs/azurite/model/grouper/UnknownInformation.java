package edu.cmu.scs.azurite.model.grouper;

public class UnknownInformation implements IChangeInformation {

	@Override
	public ChangeType getChangeType() {
		return ChangeType.UNKNOWN;
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		// TODO Auto-generated method stub
		return false;
	}

}
