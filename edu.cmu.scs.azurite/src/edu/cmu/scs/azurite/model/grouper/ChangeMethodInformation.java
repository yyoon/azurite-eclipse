package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.scs.fluorite.commands.document.DocChange;
import edu.cmu.scs.fluorite.commands.document.Range;

public class ChangeMethodInformation extends BaseChangeInformation {
	
	private final ASTNode preMethodNode;
	private final ASTNode postMethodNode;
	
	private final Range preMethodRange;
	private final Range postMethodRange;
	
	public ChangeMethodInformation(DocChange mergedChange, ASTNode preMethodNode, Range preMethodRange, ASTNode postMethodNode) {
		super(mergedChange);
		
		this.preMethodNode = preMethodNode;
		this.postMethodNode = postMethodNode;
		
		this.preMethodRange = preMethodRange;
		this.postMethodRange = new Range(postMethodNode);
	}

	@Override
	public ChangeType getChangeType() {
		return ChangeType.CHANGE_METHOD;
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
	
	public ASTNode getPreMethodNode() {
		return this.preMethodNode;
	}
	
	public ASTNode getPostMethodNode() {
		return this.postMethodNode;
	}
	
	public Range getPreMethodRange() {
		return this.preMethodRange;
	}
	
	public Range getPostMethodRange() {
		return this.postMethodRange;
	}

}
