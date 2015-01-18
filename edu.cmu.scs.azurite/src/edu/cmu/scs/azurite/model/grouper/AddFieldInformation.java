package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.FieldDeclaration;

import edu.cmu.scs.fluorite.commands.document.DocChange;
import edu.cmu.scs.fluorite.commands.document.Range;

public class AddFieldInformation extends BaseChangeInformation {
	
	private final FieldDeclaration postFieldNode;
	
	public AddFieldInformation(DocChange mergedChange, FieldDeclaration postFieldNode) {
		super(mergedChange);
		
		this.postFieldNode = postFieldNode;
	}
	
	@Override
	public ChangeKind getChangeKind() {
		return ChangeKind.ADD_FIELD;
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
		switch (level) {
		case OperationGrouper.LEVEL_METHOD:
			switch (nextChange.getChangeKind()) {
			case CHANGE_FIELD:
			case DELETE_FIELD:
				return getPostRange().equals(nextChange.getPreRange());
				
			default:
				return false;
			}
			
		case OperationGrouper.LEVEL_TYPE:
			switch (nextChange.getChangeKind()) {
			case ADD_FIELD:
			case CHANGE_FIELD:
			case DELETE_FIELD:
			case ADD_METHOD:
			case CHANGE_METHOD:
			case DELETE_METHOD:
			case CHANGE_TYPE:
			case DELETE_TYPE:
				return getPostTypeRange().equals(nextChange.getPreTypeRange());
				
			default:
				return false;
			}
		}
		
		return false;
	}
	
	@Override
	public FieldDeclaration getPostNode() {
		return this.postFieldNode;
	}
	
	@Override
	public Range getPreTypeRange() {
		if (getMergedChange() != null) {
			return getMergedChange().applyInverse(getPostTypeRange());
		} else {
			return getPostTypeRange();
		}
	}
	
	@Override
	public Range getPostTypeRange() {
		return new Range(getEnclosingType(getPostNode()));
	}

}
