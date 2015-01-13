package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;

import edu.cmu.scs.fluorite.commands.document.DocChange;

public class DeleteTypeInformation extends BaseChangeInformation {
	
	private final AbstractTypeDeclaration preTypeNode;
	
	public DeleteTypeInformation(DocChange mergedChange, AbstractTypeDeclaration preTypeNode) {
		super(mergedChange);
		
		this.preTypeNode = preTypeNode;
	}

	@Override
	public ChangeType getChangeType() {
		return ChangeType.DELETE_TYPE;
	}
	
	@Override
	public String getChangeSummary() {
		String typeName = getTypeName(getPreNode());
		String typeKind = getTypeKind(getPreNode());
		if (typeName != null && typeKind != null) {
			return String.format("Deleted %s '%s'", typeKind, typeName);
		} else if (typeKind != null) {
			return String.format("Deleted an unknown %s", typeKind);
		} else if (typeName != null) {
			return String.format("Deleted %s", typeName);
		} else {
			return "Deleted an unknown type";
		}
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public AbstractTypeDeclaration getPreNode() {
		return this.preTypeNode;
	}

}
