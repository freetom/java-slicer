package com.slicerplugin.slicer.dependencies;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;

import com.slicerplugin.generic.Pair;

public class RetCall {
	
	String call_name;
	ArrayList<Var> params=new ArrayList<Var>();
	
	public RetCall(){
		
	}
}
