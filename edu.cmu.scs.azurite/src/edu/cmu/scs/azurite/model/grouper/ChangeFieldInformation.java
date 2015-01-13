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
		if (level == OperationGrouper.LEVEL_METHOD) {
			if (nextChange.getChangeKind() == ChangeKind.CHANGE_FIELD) {
				return getPostRange().equals(nextChange.getPreRange());
			}
			
			if (nextChange.getChangeKind() == ChangeKind.DELETE_FIELD) {
				return getPostRange().equals(nextChange.getPreRange());
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

}
