package edu.cmu.scs.azurite.model.undo;

/**
 * @author YoungSeok Yoon
 * 
 * UndoAlternative describes one of the options to choose from, as a result of
 * a selective undo with some conflicts.
 * 
 * A human-readable, high-level description is provided
 * and the resulting code(text) is included.
 *
 */
public class UndoAlternative {
	
	private String mDescription;
	private String mResultingCode;
	
	public UndoAlternative() {
		this(null, null);
	}
	
	public UndoAlternative(String description, String resultingCode) {
		mDescription = description;
		mResultingCode = resultingCode;
	}
	
	public String getDescription() {
		return mDescription;
	}
	
	void setDescription(String description) {
		mDescription = description;
	}
	
	public String getResultingCode() {
		return mResultingCode;
	}
	
	void setResultingCode(String resultingCode) {
		mResultingCode = resultingCode;
	}

}
