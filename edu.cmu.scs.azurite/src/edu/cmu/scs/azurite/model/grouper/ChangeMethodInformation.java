package edu.cmu.scs.azurite.model.grouper;

import edu.cmu.scs.fluorite.commands.document.Range;

public class ChangeMethodInformation implements IChangeInformation {
	
	private final Range preMethodRange;
	private final Range postMethodRange;
	
	public ChangeMethodInformation(Range preMethodRange, Range postMethodRange) {
		this.preMethodRange = preMethodRange;
		this.postMethodRange = postMethodRange;
	}

	@Override
	public ChangeType getChangeType() {
		return ChangeType.CHANGE_METHOD;
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		if (level == OperationGrouper.LEVEL_METHOD) {
			if (nextChange.getChangeType() == ChangeType.CHANGE_METHOD) {
				return getPostMethodRange().equals(((ChangeMethodInformation) nextChange).getPreMethodRange());
			}
			
			if (nextChange.getChangeType() == ChangeType.DELETE_METHOD) {
				return getPostMethodRange().equals(((DeleteMethodInformation) nextChange).getPreMethodRange());
			}
		}
		
		return false;
	}
	
	public Range getPreMethodRange() {
		return this.preMethodRange;
	}
	
	public Range getPostMethodRange() {
		return this.postMethodRange;
	}

}
