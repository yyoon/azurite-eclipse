package edu.cmu.scs.azurite.model.grouper;

import edu.cmu.scs.fluorite.commands.document.DocChange;

public abstract class BaseChangeInformation implements IChangeInformation {
	
	private DocChange mergedChange;
	
	public BaseChangeInformation(DocChange mergedChange) {
		this.mergedChange = mergedChange;
	}
	
	@Override
	public String getChangeTypeString() {
		return "type_" + getChangeType().toString().toLowerCase();
	}

	@Override
	public DocChange getMergedChange() {
		return this.mergedChange;
	}

}
