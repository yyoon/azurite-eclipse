package edu.cmu.scs.azurite.model.grouper;

import edu.cmu.scs.fluorite.commands.document.DocChange;

public class UnknownInformation extends BaseChangeInformation {
	
	public UnknownInformation(DocChange mergedChange) {
		super(mergedChange);
	}

	@Override
	public ChangeKind getChangeKind() {
		return ChangeKind.UNKNOWN;
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		// TODO Auto-generated method stub
		return false;
	}

}
