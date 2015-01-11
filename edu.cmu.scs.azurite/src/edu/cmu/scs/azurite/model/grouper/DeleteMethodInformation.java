package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.scs.fluorite.commands.document.DocChange;
import edu.cmu.scs.fluorite.commands.document.Range;

public class DeleteMethodInformation extends BaseChangeInformation {
	
	private final ASTNode preMethodNode;
	private final Range preMethodRange;
	
	public DeleteMethodInformation(DocChange mergedChange, ASTNode preMethodNode) {
		super(mergedChange);
		
		this.preMethodNode = preMethodNode;
		this.preMethodRange = new Range(preMethodNode);
	}

	@Override
	public ChangeType getChangeType() {
		return ChangeType.DELETE_METHOD;
	}
	
	@Override
	public String getChangeSummary() {
		String methodName = getMethodName(getPreMethodNode());
		if (methodName != null) {
			return String.format("Deleted method '%s'", methodName);
		} else {
			return "Deleted a method";
		}
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		return false;
	}
	
	public ASTNode getPreMethodNode() {
		return this.preMethodNode;
	}
	
	public Range getPreMethodRange() {
		return this.preMethodRange;
	}

}
