package edu.cmu.scs.azurite.model.grouper;

import edu.cmu.scs.fluorite.commands.document.Range;

public class AddMethodInformation implements IChangeInformation {
	
	private final Range postMethodRange;
	
	public AddMethodInformation(Range postMethodRange) {
		this.postMethodRange = postMethodRange;
	}

	@Override
	public ChangeType getChangeType() {
		return ChangeType.ADD_METHOD;
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
	
	public Range getPostMethodRange() {
		return this.postMethodRange;
	}

}
