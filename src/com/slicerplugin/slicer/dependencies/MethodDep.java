package com.slicerplugin.slicer.dependencies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTNode;

import com.slicerplugin.generic.Pair;

public class MethodDep {
	
	String method;
	
	public RetDep rd;
	public ParamDep pd;
	public ExtDep ed;
	
	HashSet<String> local_vars=new HashSet<String>();
	ArrayList<String> params=new ArrayList<String>();
	ArrayList<ASTNode> function_returns=new ArrayList<ASTNode>();
	
	public MethodDep(HashSet<String> local_vars,ArrayList<ASTNode> function_returns, ArrayList<String> params,
			HashMap<String,ArrayList<ASTNode>> ext_write,HashMap<String,ArrayList<Pair<String,Integer>>> ext_as_param,
			String method){
		this.local_vars.addAll(local_vars);
		this.params.addAll(params);
		this.function_returns.addAll(function_returns);
		
		rd=new RetDep(this.local_vars,this.function_returns,this.params);
		pd=new ParamDep(this.local_vars,this.params);
		ed=new ExtDep(ext_write,ext_as_param,this.local_vars,this.params);
		
		this.method=method;
	}
	
}
