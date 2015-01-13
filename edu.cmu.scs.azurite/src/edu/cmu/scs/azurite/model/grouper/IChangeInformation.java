package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.cmu.scs.fluorite.commands.document.DocChange;
import edu.cmu.scs.fluorite.commands.document.Range;

public interface IChangeInformation {
	
	ChangeKind getChangeKind();
	
	String getChangeTypeString();
	
	String getChangeSummary();
	
	boolean shouldBeMerged(int level, IChangeInformation nextChange);
	
	/**
	 * NOTE: this may return null, in case the merged changes cancel out.
	 * @return the merged change object.
	 */
	DocChange getMergedChange();
	
	ASTNode getPreNode();
	
	ASTNode getPostNode();
	
	Range getPreRange();
	
	Range getPostRange();
	
}
