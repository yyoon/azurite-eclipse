package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.scs.fluorite.commands.document.DocChange;
import edu.cmu.scs.fluorite.commands.document.Range;

public class AddFieldInformation extends BaseChangeInformation {
	
	private final ASTNode postFieldNode;
	private final Range postFieldRange;
	
	public AddFieldInformation(DocChange mergedChange, ASTNode postFieldNode) {
		super(mergedChange);
		
		this.postFieldNode = postFieldNode;
		this.postFieldRange = new Range(postFieldNode);
	}
	
	@Override
	public ChangeType getChangeType() {
		return ChangeType.ADD_FIELD;
	}
	
	@Override
	public String getChangeSummary() {
		String fieldName = getFieldName(getPostFieldNode());
		if (fieldName != null) {
			return String.format("Added field '%s'", fieldName);
		} else {
			return "Added a field";
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
	
	public ASTNode getPostFieldNode() {
		return this.postFieldNode;
	}
	
	public Range getPostFieldRange() {
		return this.postFieldRange;
	}

}
