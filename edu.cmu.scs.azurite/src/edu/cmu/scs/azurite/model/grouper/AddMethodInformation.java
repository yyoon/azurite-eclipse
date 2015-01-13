package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.scs.fluorite.commands.document.DocChange;

public class AddMethodInformation extends BaseChangeInformation {
	
	private final MethodDeclaration postMethodNode;
	
	public AddMethodInformation(DocChange mergedChange, MethodDeclaration postMethodNode) {
		super(mergedChange);
		
		this.postMethodNode = postMethodNode;
	}

	@Override
	public ChangeType getChangeType() {
		return ChangeType.ADD_METHOD;
	}
	
	@Override
	public String getChangeSummary() {
		String methodName = getMethodName(getPostNode());
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
				return getPostRange().equals(nextChange.getPreRange());
			}
			
			if (nextChange.getChangeType() == ChangeType.DELETE_METHOD) {
				return getPostRange().equals(nextChange.getPreRange());
			}
		}
		
		return false;
	}
	
	@Override
	public MethodDeclaration getPostNode() {
		return this.postMethodNode;
	}

}
