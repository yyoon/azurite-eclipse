package edu.cmu.scs.azurite.model.grouper;

import edu.cmu.scs.fluorite.commands.document.Range;

public class DeleteFieldInformation implements IChangeInformation {
	
	private final Range preFieldRange;
	
	public DeleteFieldInformation(Range preFieldRange) {
		this.preFieldRange = preFieldRange;
	}

	@Override
	public ChangeType getChangeType() {
		return ChangeType.DELETE_FIELD;
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		return false;
	}
	
	public Range getPreFieldRange() {
		return this.preFieldRange;
	}

}
