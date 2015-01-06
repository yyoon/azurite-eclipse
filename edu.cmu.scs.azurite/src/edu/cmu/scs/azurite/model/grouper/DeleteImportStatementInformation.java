package edu.cmu.scs.azurite.model.grouper;

public class DeleteImportStatementInformation implements IChangeInformation {
	
	private static DeleteImportStatementInformation _instance;
	
	public static DeleteImportStatementInformation getInstance() {
		if (_instance == null) {
			_instance = new DeleteImportStatementInformation();
		}
		
		return _instance;
	}

	@Override
	public ChangeType getChangeType() {
		return ChangeType.DELETE_IMPORT_STATEMENT;
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		return	level > 0 &&
				(nextChange.getChangeType() == ChangeType.ADD_IMPORT_STATEMENT ||
				 nextChange.getChangeType() == ChangeType.DELETE_IMPORT_STATEMENT);
	}

}
