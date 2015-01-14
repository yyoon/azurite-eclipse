package edu.cmu.scs.azurite.model.grouper;

import edu.cmu.scs.fluorite.commands.document.DocChange;

public class NonCodeChangeInformation extends BaseChangeInformation {
	
	public NonCodeChangeInformation(DocChange mergedChange) {
		super(mergedChange);
	}

	@Override
	public ChangeKind getChangeKind() {
		return ChangeKind.NON_CODE_CHANGE;
	}

	@Override
	public String getChangeSummary() {
		return "Non-code change";
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		return nextChange.getChangeKind() == ChangeKind.NON_CODE_CHANGE;
	}

}
