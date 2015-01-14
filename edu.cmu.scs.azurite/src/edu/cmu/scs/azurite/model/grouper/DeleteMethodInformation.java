package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.cmu.scs.fluorite.commands.document.DocChange;
import edu.cmu.scs.fluorite.commands.document.Range;

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
	public MethodDeclaration getPreNode() {
		return this.preMethodNode;
	}
	
	@Override
	public Range getPreTypeRange() {
		return new Range(getEnclosingType(getPreNode()));
	}
	
	@Override
	public Range getPostTypeRange() {
		if (getMergedChange() != null) {
			return getMergedChange().apply(getPreTypeRange());
		} else {
			return getPreTypeRange();
		}
	}

}
