package com.slicerplugin.slicer.dependencies;

import java.util.ArrayList;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;

import com.slicerplugin.slicer.dependencies.DependenciesAnalyzer.addNames;
import com.slicerplugin.slicer.parser.InitSlicing;

/**
 * Used to extract the keys of the identifier in a subtree of a CompilationUnit that represent 
 * a parameter passing
 * 
 * @author Tomas Bortoli
 *
 */
public class getParams extends ASTVisitor{
	ArrayList<String> keys=new ArrayList<String>();
	
	@Override public boolean visit(MethodInvocation node){
		String call=InitSlicing.call_table.get(node);
		if(call!=null){
			keys.add(call);
			return false;
		}
		//else	InitSlicing.fatal("Orphan call");
		return false;
	}
	
	@Override public boolean visit(SimpleName node){
		keys.add(InitSlicing.getKey(node));
		return false;
	}
	@Override public boolean visit(QualifiedName node){
		if((node.resolveBinding().getModifiers()&0x8)!=0 ||
				node.resolveBinding().getKind()==IBinding.TYPE || node.resolveBinding().getKind()==IBinding.PACKAGE){
			node.getName().accept(this);
		}
		else{
			Stack<Name> hierarchy=InitSlicing.getHierarchy(node);
			String key="";
			while(!hierarchy.isEmpty()){
				String name=InitSlicing.getKey(hierarchy.pop());
				key+=name;
				//keys.add(key);
				key+=" ";
			}
			key=key.substring(0,key.length()-1);
			keys.add(key);
		}
		return false;
	}
	
}
