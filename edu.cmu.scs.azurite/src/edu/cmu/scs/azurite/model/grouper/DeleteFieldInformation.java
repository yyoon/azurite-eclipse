package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.FieldDeclaration;

import edu.cmu.scs.fluorite.commands.document.DocChange;

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
		return false;
	}
	
	@Override
	public FieldDeclaration getPreNode() {
		return this.preFieldNode;
	}

}
