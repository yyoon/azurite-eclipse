package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;

import edu.cmu.scs.fluorite.commands.document.DocChange;

public class AddTypeInformation extends BaseChangeInformation {
	
	private final AbstractTypeDeclaration postTypeNode;
	
	public AddTypeInformation(DocChange mergedChange, AbstractTypeDeclaration postTypeNode) {
		super(mergedChange);
		
		this.postTypeNode = postTypeNode;
	}

	@Override
	public ChangeType getChangeType() {
		return ChangeType.ADD_TYPE;
	}
	
	@Override
	public String getChangeSummary() {
		String typeName = getTypeName(getPostNode());
		String typeKind = getTypeKind(getPostNode());
		if (typeName != null && typeKind != null) {
			return String.format("Added %s '%s'", typeKind, typeName);
		} else if (typeKind != null) {
			return String.format("Added an unknown %s", typeKind);
		} else if (typeName != null) {
			return String.format("Added %s", typeName);
		} else {
			return "Added an unknown type";
		}
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public AbstractTypeDeclaration getPostNode() {
		return this.postTypeNode;
	}

}
