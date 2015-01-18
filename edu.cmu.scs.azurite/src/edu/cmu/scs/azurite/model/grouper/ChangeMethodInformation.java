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
	public ChangeKind getChangeKind() {
		return ChangeKind.CHANGE_METHOD;
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
		switch (level) {
		case OperationGrouper.LEVEL_METHOD:
			switch (nextChange.getChangeKind()) {
			case CHANGE_METHOD:
			case DELETE_METHOD:
				return getPostRange().equals(nextChange.getPreRange());
				
			default:
				return false;
			}
			
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
	public MethodDeclaration getPostNode() {
		return this.postMethodNode;
	}
	
	@Override
	public Range getPreTypeRange() {
		if (getMergedChange() != null) {
			return getMergedChange().applyInverse(getPostTypeRange());
		} else {
			return getPostTypeRange();
		}
	}
	
	@Override
	public Range getPostTypeRange() {
		return new Range(getEnclosingType(getPostNode()));
	}

}
