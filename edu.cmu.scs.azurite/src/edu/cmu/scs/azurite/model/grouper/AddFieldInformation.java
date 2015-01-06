package edu.cmu.scs.azurite.model.grouper;

import edu.cmu.scs.fluorite.commands.document.Range;

public class AddFieldInformation implements IChangeInformation {
	
	private final Range postFieldRange;
	
	public AddFieldInformation(Range postFieldRange) {
		this.postFieldRange = postFieldRange;
	}
	
	@Override
	public ChangeType getChangeType() {
		return ChangeType.ADD_FIELD;
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		if (level == OperationGrouper.LEVEL_METHOD) {
			if (nextChange.getChangeType() == ChangeType.CHANGE_FIELD) {
				return getPostFieldRange().equals(((ChangeFieldInformation) nextChange).getPreFieldRange());
			}
			
			if (nextChange.getChangeType() == ChangeType.DELETE_FIELD) {
				return getPostFieldRange().equals(((DeleteFieldInformation) nextChange).getPreFieldRange());
			}
		}
		
		return false;
	}
	
	public Range getPostFieldRange() {
		return this.postFieldRange;
	}

}
