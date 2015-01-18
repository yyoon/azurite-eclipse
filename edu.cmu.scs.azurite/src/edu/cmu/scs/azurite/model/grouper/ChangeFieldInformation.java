package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.FieldDeclaration;

import edu.cmu.scs.fluorite.commands.document.DocChange;
import edu.cmu.scs.fluorite.commands.document.Range;

public class ChangeFieldInformation extends BaseChangeInformation {
	
	private final FieldDeclaration preFieldNode;
	private final FieldDeclaration postFieldNode;
	
	public ChangeFieldInformation(DocChange mergedChange, FieldDeclaration preFieldNode, Range preFieldRange, FieldDeclaration postFieldNode) {
		super(mergedChange);
		
		this.preFieldNode = preFieldNode;
		this.postFieldNode = postFieldNode;
		
		setPreRange(preFieldRange);
	}

	@Override
	public ChangeKind getChangeKind() {
		return ChangeKind.CHANGE_FIELD;
	}
	
	@Override
	public String getChangeSummary() {
		String fieldName = getFieldName(getPostNode());
		if (fieldName != null) {
			return String.format("Changed field '%s'", fieldName);
		} else {
			return "Changed a field";
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
	public FieldDeclaration getPreNode() {
		return this.preFieldNode;
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
