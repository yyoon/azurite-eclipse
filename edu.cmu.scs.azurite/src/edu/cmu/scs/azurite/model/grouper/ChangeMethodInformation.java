package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.scs.fluorite.commands.document.DocChange;
import edu.cmu.scs.fluorite.commands.document.Range;

public class ChangeMethodInformation extends BaseChangeInformation {
	
	private final MethodDeclaration preMethodNode;
	private final MethodDeclaration postMethodNode;
	
	public ChangeMethodInformation(DocChange mergedChange, MethodDeclaration preMethodNode, Range preMethodRange, MethodDeclaration postMethodNode) {
		super(mergedChange);
		
		this.preMethodNode = preMethodNode;
		this.postMethodNode = postMethodNode;
		
		setPreRange(preMethodRange);
	}

	@Override
	public ChangeType getChangeType() {
		return ChangeType.CHANGE_METHOD;
	}
	
	@Override
	public String getChangeSummary() {
		String methodName = getMethodName(getPostNode());
		if (methodName != null) {
			return String.format("Changed method '%s'", methodName);
		} else {
			return "Changed a method";
		}
	}

	@Override
	public boolean shouldBeMerged(int level, IChangeInformation nextChange) {
		if (level == OperationGrouper.LEVEL_METHOD) {
			if (nextChange.getChangeType() == ChangeType.CHANGE_METHOD) {
				return getPostRange().equals(nextChange.getPreRange());
			}
			
			if (nextChange.getChangeType() == ChangeType.DELETE_METHOD) {
				return getPostRange().equals(nextChange.getPreRange());
			}
		}
		
		return false;
	}
	
	@Override
	public MethodDeclaration getPreNode() {
		return this.preMethodNode;
	}
	
	@Override
	public MethodDeclaration getPostNode() {
		return this.postMethodNode;
	}

}
