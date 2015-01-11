package edu.cmu.scs.azurite.model.grouper;

import edu.cmu.scs.fluorite.commands.document.DocChange;

public class DeleteImportStatementInformation extends BaseChangeInformation {
	
	public DeleteImportStatementInformation(DocChange mergedChange) {
		super(mergedChange);
	}

	@Override
	public ChangeType getChangeType() {
		return ChangeType.DELETE_IMPORT_STATEMENT;
	}
	
	@Override
	public String getChangeSummary() {
		return "Deleted import statement(s)";
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		return	level > 0 &&
				(nextChange.getChangeType() == ChangeType.ADD_IMPORT_STATEMENT ||
				 nextChange.getChangeType() == ChangeType.DELETE_IMPORT_STATEMENT);
	}

}
