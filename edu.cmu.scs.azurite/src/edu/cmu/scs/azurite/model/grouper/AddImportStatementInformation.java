package edu.cmu.scs.azurite.model.grouper;

public class AddImportStatementInformation implements IChangeInformation {
	
	private static AddImportStatementInformation _instance;
	
	public static AddImportStatementInformation getInstance() {
		if (_instance == null) {
			_instance = new AddImportStatementInformation();
		}
		
		return _instance;
	}

	@Override
	public ChangeType getChangeType() {
		return ChangeType.ADD_IMPORT_STATEMENT;
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		return	level > 0 &&
				(nextChange.getChangeType() == ChangeType.ADD_IMPORT_STATEMENT ||
				 nextChange.getChangeType() == ChangeType.DELETE_IMPORT_STATEMENT);
	}

}
