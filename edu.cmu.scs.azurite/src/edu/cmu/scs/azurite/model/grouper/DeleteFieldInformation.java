package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.scs.fluorite.commands.document.DocChange;
import edu.cmu.scs.fluorite.commands.document.Range;

public class DeleteFieldInformation extends BaseChangeInformation {
	
	private final ASTNode preFieldNode;
	private final Range preFieldRange;
	
	public DeleteFieldInformation(DocChange mergedChange, ASTNode preFieldNode) {
		super(mergedChange);
		
		this.preFieldNode = preFieldNode;
		this.preFieldRange = new Range(preFieldNode);
	}

	@Override
	public ChangeType getChangeType() {
		return ChangeType.DELETE_FIELD;
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		return false;
	}
	
	public ASTNode getPreFieldNode() {
		return this.preFieldNode;
	}
	
	public Range getPreFieldRange() {
		return this.preFieldRange;
	}

}
