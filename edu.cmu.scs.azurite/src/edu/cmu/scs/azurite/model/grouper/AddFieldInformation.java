package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.FieldDeclaration;

import edu.cmu.scs.fluorite.commands.document.DocChange;

public class AddFieldInformation extends BaseChangeInformation {
	
	private final FieldDeclaration postFieldNode;
	
	public AddFieldInformation(DocChange mergedChange, FieldDeclaration postFieldNode) {
		super(mergedChange);
		
		this.postFieldNode = postFieldNode;
	}
	
	@Override
	public ChangeType getChangeType() {
		return ChangeType.ADD_FIELD;
	}
	
	@Override
	public String getChangeSummary() {
		String fieldName = getFieldName(getPostNode());
		if (fieldName != null) {
			return String.format("Added field '%s'", fieldName);
		} else {
			return "Added a field";
		}
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		if (level == OperationGrouper.LEVEL_METHOD) {
			if (nextChange.getChangeType() == ChangeType.CHANGE_FIELD) {
				return getPostRange().equals(nextChange.getPreRange());
			}
			
			if (nextChange.getChangeType() == ChangeType.DELETE_FIELD) {
				return getPostRange().equals(nextChange.getPreRange());
			}
		}
		
		return false;
	}
	
	@Override
	public FieldDeclaration getPostNode() {
		return this.postFieldNode;
	}

}
