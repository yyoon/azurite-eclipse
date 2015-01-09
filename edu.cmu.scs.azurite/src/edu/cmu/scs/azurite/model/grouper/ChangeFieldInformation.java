package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.scs.fluorite.commands.document.DocChange;
import edu.cmu.scs.fluorite.commands.document.Range;

public class ChangeFieldInformation extends BaseChangeInformation {
	
	private final ASTNode preFieldNode;
	private final ASTNode postFieldNode;
	
	private final Range preFieldRange;
	private final Range postFieldRange;
	
	public ChangeFieldInformation(DocChange mergedChange, ASTNode preFieldNode, Range preFieldRange, ASTNode postFieldNode) {
		super(mergedChange);
		
		this.preFieldNode = preFieldNode;
		this.postFieldNode = postFieldNode;
		
		this.preFieldRange = preFieldRange;
		this.postFieldRange = new Range(postFieldNode);
	}

	@Override
	public ChangeType getChangeType() {
		return ChangeType.CHANGE_FIELD;
	}
	
	@Override
	public String getChangeSummary() {
		String fieldName = getFieldName(getPostFieldNode());
		if (fieldName != null) {
			return String.format("Changed field '%s'", fieldName);
		} else {
			return "Changed a field";
		}
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		if (level == OperationGrouper.LEVEL_METHOD) {
			if (nextChange.getChangeType() == ChangeType.CHANGE_FIELD) {
				return getPostFieldRange().equals(((ChangeFieldInformation) nextChange).getPreFieldRange());
			}
			
			if (nextChange.getChangeType() == ChangeType.DELETE_FIELD) {
				return getPostFieldRange().equals(((DeleteFieldInformation) nextChange).getPreFieldRange());
			}
		}
		
		return false;
	}
	
	public ASTNode getPreFieldNode() {
		return this.preFieldNode;
	}
	
	public ASTNode getPostFieldNode() {
		return this.postFieldNode;
	}
	
	public Range getPreFieldRange() {
		return this.preFieldRange;
	}
	
	public Range getPostFieldRange() {
		return this.postFieldRange;
	}

}
