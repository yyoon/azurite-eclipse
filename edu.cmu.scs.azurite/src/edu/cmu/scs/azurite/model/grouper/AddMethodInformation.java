package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.scs.fluorite.commands.document.DocChange;
import edu.cmu.scs.fluorite.commands.document.Range;

public class AddMethodInformation extends BaseChangeInformation {
	
	private final ASTNode postMethodNode;
	private final Range postMethodRange;
	
	public AddMethodInformation(DocChange mergedChange, ASTNode postMethodNode) {
		super(mergedChange);
		
		this.postMethodNode = postMethodNode;
		this.postMethodRange = new Range(postMethodNode);
	}

	@Override
	public ChangeType getChangeType() {
		return ChangeType.ADD_METHOD;
	}
	
	@Override
	public String getChangeSummary() {
		String methodName = getMethodName(getPostMethodNode());
		if (methodName != null) {
			return String.format("Added method '%s'", methodName);
		} else {
			return "Added a method";
		}
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		if (level == OperationGrouper.LEVEL_METHOD) {
			if (nextChange.getChangeType() == ChangeType.CHANGE_METHOD) {
				return getPostMethodRange().equals(((ChangeMethodInformation) nextChange).getPreMethodRange());
			}
			
			if (nextChange.getChangeType() == ChangeType.DELETE_METHOD) {
				return getPostMethodRange().equals(((DeleteMethodInformation) nextChange).getPreMethodRange());
			}
		}
		
		return false;
	}
	
	public ASTNode getPostMethodNode() {
		return this.postMethodNode;
	}
	
	public Range getPostMethodRange() {
		return this.postMethodRange;
	}

}
