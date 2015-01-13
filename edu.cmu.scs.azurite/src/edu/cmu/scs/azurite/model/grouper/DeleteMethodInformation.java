package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.scs.fluorite.commands.document.DocChange;

public class DeleteMethodInformation extends BaseChangeInformation {
	
	private final MethodDeclaration preMethodNode;
	
	public DeleteMethodInformation(DocChange mergedChange, MethodDeclaration preMethodNode) {
		super(mergedChange);
		
		this.preMethodNode = preMethodNode;
	}

	@Override
	public ChangeKind getChangeKind() {
		return ChangeKind.DELETE_METHOD;
	}
	
	@Override
	public String getChangeSummary() {
		String methodName = getMethodName(getPreNode());
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
	
	@Override
	public MethodDeclaration getPreNode() {
		return this.preMethodNode;
	}

}
