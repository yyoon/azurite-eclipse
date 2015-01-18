package edu.cmu.scs.azurite.model.grouper;

public enum ChangeKind {
	UNKNOWN,
	
	NON_CODE_CHANGE,
	
	ADD_IMPORT_STATEMENT,
	DELETE_IMPORT_STATEMENT,
	
	ADD_TYPE,
	CHANGE_TYPE,
	DELETE_TYPE,
	
	ADD_METHOD,
	CHANGE_METHOD,
	DELETE_METHOD,
	
	ADD_FIELD,
	CHANGE_FIELD,
	DELETE_FIELD,
	
	MIXED
}
