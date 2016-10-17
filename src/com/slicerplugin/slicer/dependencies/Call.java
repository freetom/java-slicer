package com.slicerplugin.slicer.dependencies;

import org.eclipse.jdt.core.dom.ASTNode;

public class Call {
	String father;
	String method;
	int originalIndex,newIndex;
	
	ASTNode invocation;
	
	public Call(String father, String method, int originalIndex, int newIndex, ASTNode invocation){
		this.father=father;
		this.method=method;
		this.originalIndex=originalIndex;
		this.newIndex=newIndex;
		this.invocation=invocation;
	}
	
	@Override public boolean equals(Object o){
		if(o instanceof Call){
			Call c=(Call)o;
			return this.method.equals(c.method) && this.originalIndex==c.originalIndex &&
					this.newIndex==c.newIndex && this.invocation.equals(c.invocation);
		}
		return false;
	}
}
