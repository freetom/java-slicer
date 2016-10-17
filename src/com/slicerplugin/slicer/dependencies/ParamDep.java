package com.slicerplugin.slicer.dependencies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;

import com.slicerplugin.generic.Pair;
import com.slicerplugin.slicer.parser.InitSlicing;
import com.slicerplugin.slicer.parser.RefID;
import com.slicerplugin.slicer.parser.RefStmt;

/**
 * Compute parameters' dependencies (who influence who) for each function
 * Also store the calls and which parameter they use to resolve nested modifications at runtime in @DependenciesAnalyzer
 * 
 * @author Tomas Bortoli
 *
 */
public class ParamDep extends Dep {
	
	/**
	 * Map each parameter with the list of var that modify it
	 * Example schema:
	 * param n ->param m|...|external var k
	 * param n+1 -> EMPTY
	 * ...
	 * 
	 */
	HashMap<Integer,ArrayList<Var>> subparams_deps=new HashMap<Integer,ArrayList<Var>>();
	
	/**
	 * Calls that could modify parameters
	 */
	ArrayList<Call> calls=new ArrayList<Call>();
	
	public ParamDep(HashSet<String> local_vars, ArrayList<String> params){
		this.local_vars=local_vars;
		this.params=params;
	}
	
	
	private static ASTNode getParamFromInvocation(String invok, int param_n){
		RefID r=InitSlicing.ids_table.get(invok);
		return (ASTNode) ((MethodInvocation)r.invocation).arguments().get(param_n);
	}
	
	private static String getMethodFromInvokStr(String invok){
		String method="";
		Character ch;
		int index=invok.lastIndexOf(" ");
		while(--index>=0 && (ch=invok.charAt(index))!=' '){
			method+=ch;
		}
		return (new StringBuilder(method).reverse().toString());
	}
	
	/*
	public static void _resolve_param_deps(){
		for(RefID id : InitSlicing.ids_table.values()){
			if(id.param!=null){
				for(Pair<String,Integer> combo : id.param){
					String method=getMethodFromInvokStr(combo.getFirst());
					MethodDeclaration declaration=(MethodDeclaration)InitSlicing.methods_declarations.get(method);
					if(declaration!=null){
						SingleVariableDeclaration ref=(SingleVariableDeclaration) declaration.parameters().get(combo.getSecond());
						String name=InitSlicing.getKey(ref.getName());
						
						RefID refID=InitSlicing.ids_table.get(name);
						gg(refID,combo,id.key);
						
					}
				}
			}
		}
	}*/
	
	
	private static boolean gg(RefID refID,Pair<String,Integer> combo, String base) {
		boolean ret=false;
		if(refID.fields!=null){
			boolean flag=false;
			for(RefID field:refID.fields){
				gg(field,combo,base+field.key.substring(field.key.indexOf(" ")));
				
				if(field.write!=null){
					ret=flag=true;
				}
				if(field.param!=null){
					for(Pair<String,Integer> param: field.param){
						
						MethodDeclaration declaration=(MethodDeclaration)InitSlicing.methods_declarations.get(getMethodFromInvokStr(param.getFirst()));
						SingleVariableDeclaration ref=(SingleVariableDeclaration) declaration.parameters().get(param.getSecond());
						String name=InitSlicing.getKey(ref.getName());
						flag=gg(InitSlicing.ids_table.get(name),param,base+field.key.substring(field.key.indexOf(" ")));
					}
				
				}
				
				if(flag){
					String str=base+field.key.substring(field.key.indexOf(' '));
					RefID original=InitSlicing.ids_table.get(str);
					if(original!=null)
						original.refs.add(new Pair<String,ASTNode>(field.key,getParamFromInvocation(combo.getFirst(),combo.getSecond())));
					//if(id.param==null)
					//	id.param=new ArrayList<Pair<String,Integer>>();
					//id.param.add(combo);
				}
				flag=false;
			}
		}
		return ret;
	}


	public void resolve_param_deps(){
		int i=0;
		for(String param : params){
			ArrayList<Var> param_deps=new ArrayList<Var>();
			subparams_deps.put(i, param_deps);
			
			if(InitSlicing.ids_table.get(param)!=null && InitSlicing.ids_table.get(param).is_object){
				HashSet<String> deps1=new HashSet<String>();
				deps.clear();
				HashSet<String> current = new HashSet<String>();
				//current.add(param);
				
				RefID ref=InitSlicing.ids_table.get(param);
				
				if(ref.write!=null)
					for(ASTNode node: ref.write)
						visit(node);
				if(ref.param!=null)
					for(Pair<String,Integer> p:ref.param){
						addCall(p.getFirst(),params.indexOf(param),p.getSecond());
					}
				/*for(RefID field : ref.fields){
					if(field.write!=null)
						for(ASTNode x: field.write)
							visit(x);
				}*/
				current.addAll(deps);
				
				while(!current.isEmpty()){
					for(String s : current){
						ref=InitSlicing.ids_table.get(s);
						//if(ref.is_call)
						//	addCall(ref.invocation);
						
						//
						
						
						if(params.contains(s))
							if(ref.param!=null)
								for(Pair<String,Integer> p:ref.param){
									addCall(p.getFirst(),params.indexOf(s),p.getSecond());
								}
						//if(ref.read!=null)
						//	for(ASTNode node : ref.read)
						//		visit(node);
						if(ref.write!=null)
							for(ASTNode node : ref.write)
								visit(node);
					}
					current.addAll(deps);
					current.removeAll(deps1);
					deps1.addAll(current);
				}
				
				HashSet<String> local_vars_except_params=new HashSet<String>();
				local_vars_except_params.addAll(local_vars);
				local_vars_except_params.removeAll(params);
				
				deps1.removeAll(local_vars_except_params);
				
				//divide params and external by static and non static
				for(String var: deps1){
					Var v=new Var();
					
					//it is a parameter
					int index;
					if((index=params.indexOf(var))!=-1){
						v.param_index=index;
					}
					//it is an external variable
					else{
						v.param_index=-1;
						ref=InitSlicing.ids_table.get(var);
						v.is_static=ref.is_static;
						v.key=var;
						if(!v.is_static )//&& !var.contains(" "))
							v.is_local_field=true;
					}
					param_deps.add(v);
				}
			}
		}
	}
	
	private void addCall(String param, int originalIndex, int newIndex){
		ASTNode call;
		try{
			call=((MethodInvocation)InitSlicing.ids_table.get(param).invocation);
		}
		catch(Exception e){
			call=((ClassInstanceCreation)InitSlicing.ids_table.get(param).invocation);
		}
		while(call.getNodeType()!=ASTNode.METHOD_INVOCATION &&
				call.getNodeType()!=ASTNode.CLASS_INSTANCE_CREATION) call=call.getParent();
		String name;
		if(call.getNodeType()==ASTNode.METHOD_INVOCATION)	name=InitSlicing.getKey(((MethodInvocation)call).getName());
		else	name=((ClassInstanceCreation)call).resolveConstructorBinding().getMethodDeclaration().getKey();
		calls.add(new Call(param/*getObjectFromInvocation(call)*/, name,originalIndex,newIndex,call));
	}
	
	private String getObjectFromInvocation(ASTNode call){
		solveExpression se=new solveExpression();
		if(((MethodInvocation)call).getExpression()!=null)
			((MethodInvocation)call).getExpression().accept(se);
		return se.key;
	}
	
	
	
	
	private void visit(ASTNode node){
		if(node==null || node.getNodeType()==ASTNode.BLOCK || node.getNodeType()==ASTNode.METHOD_DECLARATION)
			return;
		
		RefStmt ref=InitSlicing.stmt_table.get(node);
		if(ref!=null){
			if(ref.read!=null)
				for(String id : ref.read)
					deps.add(id);
			if(ref.write!=null)
				for(String id : ref.write)
					deps.add(id);
		}
		visit(node.getParent());
		
	}
	
	
}
