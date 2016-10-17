package com.slicerplugin.slicer.dependencies;

import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;

import com.slicerplugin.slicer.parser.InitSlicing;

public class solveExpression extends ASTVisitor{
	String key="";
	@Override public boolean visit(SimpleName node){
		key=InitSlicing.getKey(node);
		return false;
	}
	
	@Override public boolean visit(QualifiedName node){
		Stack<Name> hierarchy=InitSlicing.getHierarchy(node);
		while(!hierarchy.isEmpty()){
			String name=InitSlicing.getKey(hierarchy.pop());
			key+=name;
			key+=" ";
		}
		key=key.substring(0,key.length()-1);
		return false;
	}
}
