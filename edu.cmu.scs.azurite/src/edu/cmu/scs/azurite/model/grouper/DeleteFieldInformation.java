package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.FieldDeclaration;

import edu.cmu.scs.fluorite.commands.document.DocChange;
import edu.cmu.scs.fluorite.commands.document.Range;

public class DeleteFieldInformation extends BaseChangeInformation {
	
	private final FieldDeclaration preFieldNode;
	
	public DeleteFieldInformation(DocChange mergedChange, FieldDeclaration preFieldNode) {
		super(mergedChange);
		
		this.preFieldNode = preFieldNode;
	}

	@Override
	public ChangeKind getChangeKind() {
		return ChangeKind.DELETE_FIELD;
	}
	
	@Override
	public String getChangeSummary() {
		String fieldName = getFieldName(getPreNode());
		if (fieldName != null) {
			return String.format("Deleted field '%s'", fieldName);
		} else {
			return "Deleted a field";
		}
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		switch (level) {
		case OperationGrouper.LEVEL_METHOD:
			return false;
			
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
	public FieldDeclaration getPreNode() {
		return this.preFieldNode;
	}
	
	@Override
	public Range getPreTypeRange() {
		return new Range(getEnclosingType(getPreNode()));
	}
	
	@Override
	public Range getPostTypeRange() {
		if (getMergedChange() != null) {
			return getMergedChange().apply(getPreTypeRange());
		} else {
			return getPreTypeRange();
		}
	}

}
