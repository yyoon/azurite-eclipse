package edu.cmu.scs.azurite.model.grouper;

import edu.cmu.scs.fluorite.commands.document.DocChange;

public class AddImportStatementInformation extends BaseChangeInformation {
	
	public AddImportStatementInformation(DocChange mergedChange) {
		super(mergedChange);
	}

	@Override
	public ChangeKind getChangeKind() {
		return ChangeKind.ADD_IMPORT_STATEMENT;
	}
	
	@Override
	public String getChangeSummary() {
		return "Added import statement(s)";
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		return	level > 0 &&
				(nextChange.getChangeKind() == ChangeKind.ADD_IMPORT_STATEMENT ||
				 nextChange.getChangeKind() == ChangeKind.DELETE_IMPORT_STATEMENT);
	}

}
