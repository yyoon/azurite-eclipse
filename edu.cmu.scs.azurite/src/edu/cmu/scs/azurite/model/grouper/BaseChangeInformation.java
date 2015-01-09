package edu.cmu.scs.azurite.model.grouper;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import edu.cmu.scs.fluorite.commands.document.DocChange;

public abstract class BaseChangeInformation implements IChangeInformation {
	
	private DocChange mergedChange;
	
	public BaseChangeInformation(DocChange mergedChange) {
		this.mergedChange = mergedChange;
	}
	
	@Override
	public String getChangeTypeString() {
		return "type_" + getChangeType().toString().toLowerCase();
	}
	
	@Override
	public String getChangeSummary() {
		return "";
	}

	@Override
	public DocChange getMergedChange() {
		return this.mergedChange;
	}
	
	protected String getFieldName(ASTNode fieldDeclarationNode) {
		if (fieldDeclarationNode instanceof FieldDeclaration) {
			FieldDeclaration fd = (FieldDeclaration) fieldDeclarationNode;
			if (fd.fragments().size() > 0) {
				Object fragment = fd.fragments().get(0);
				if (fragment instanceof VariableDeclarationFragment) {
					VariableDeclarationFragment vdf = (VariableDeclarationFragment) fragment;
					if (vdf.getName() != null) {
						return vdf.getName().getIdentifier();
					}
				}
			}
		}
		
		return null;
	}
	
	protected String getMethodName(ASTNode methodDeclarationNode) {
		if (methodDeclarationNode instanceof MethodDeclaration) {
			MethodDeclaration md = (MethodDeclaration) methodDeclarationNode;
			if (md.getName() != null) {
				return md.getName().getIdentifier();
			}
		}
		
		return null;
	}

}
