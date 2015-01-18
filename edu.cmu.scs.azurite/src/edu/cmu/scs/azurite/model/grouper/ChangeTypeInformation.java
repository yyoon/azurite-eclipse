package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;

import edu.cmu.scs.fluorite.commands.document.DocChange;
import edu.cmu.scs.fluorite.commands.document.Range;

public class ChangeTypeInformation extends BaseChangeInformation {
	
	private final AbstractTypeDeclaration preTypeNode;
	private final AbstractTypeDeclaration postTypeNode;
	
	public ChangeTypeInformation(DocChange mergedChange, AbstractTypeDeclaration preTypeNode, Range preTypeRange, AbstractTypeDeclaration postTypeNode) {
		super(mergedChange);
		
		this.preTypeNode = preTypeNode;
		this.postTypeNode = postTypeNode;
		
		setPreRange(preTypeRange);
	}

	@Override
	public ChangeKind getChangeKind() {
		return ChangeKind.CHANGE_TYPE;
	}
	
	@Override
	public String getChangeSummary() {
		String typeName = getTypeName(getPostNode());
		String typeKind = getTypeKind(getPostNode());
		if (typeName != null && typeKind != null) {
			return String.format("Changed %s '%s'", typeKind, typeName);
		} else if (typeKind != null) {
			return String.format("Changed an unknown %s", typeKind);
		} else if (typeName != null) {
			return String.format("Changed %s", typeName);
		} else {
			return "Changed an unknown type";
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
	public AbstractTypeDeclaration getPreNode() {
		return this.preTypeNode;
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
