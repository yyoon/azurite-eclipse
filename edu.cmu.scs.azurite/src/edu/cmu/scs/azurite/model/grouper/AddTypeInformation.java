package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;

import edu.cmu.scs.fluorite.commands.document.DocChange;
import edu.cmu.scs.fluorite.commands.document.Range;

public class AddTypeInformation extends BaseChangeInformation {
	
	private final AbstractTypeDeclaration postTypeNode;
	
	public AddTypeInformation(DocChange mergedChange, AbstractTypeDeclaration postTypeNode) {
		super(mergedChange);
		
		this.postTypeNode = postTypeNode;
	}

	@Override
	public ChangeKind getChangeKind() {
		return ChangeKind.ADD_TYPE;
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
	public AbstractTypeDeclaration getPostNode() {
		return this.postTypeNode;
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
