package edu.cmu.scs.azurite.model.grouper;

import edu.cmu.scs.fluorite.commands.document.Range;

public class DeleteMethodInformation implements IChangeInformation {
	
	private final Range preMethodRange;
	
	public DeleteMethodInformation(Range preMethodRange) {
		this.preMethodRange = preMethodRange;
	}

	@Override
	public ChangeType getChangeType() {
		return ChangeType.DELETE_METHOD;
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		return false;
	}
	
	public Range getPreMethodRange() {
		return this.preMethodRange;
	}

}
