package com.slicerplugin.slicer.dependencies;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;

import com.slicerplugin.generic.Pair;
import com.slicerplugin.slicer.parser.InitSlicing;
import com.slicerplugin.slicer.parser.RefID;
import com.slicerplugin.slicer.parser.RefStmt;


/**
 *
 * Find dependencies of the return value of a function
 * 
 * @author Tomas Bortoli
 *
 */
public class RetDep extends Dep {
	
	/**
	 * return value -> param 1, ... ,param n , external var 1, ... ,external var n
	 */
	ArrayList<Var> return_dep=new ArrayList<Var>();
	
	/**
	 * To prevent functions like: f(){ return 3; } to escape from the marking of the return
	 */
	ArrayList<ASTNode> function_returns=new ArrayList<ASTNode>();
	
	HashSet<RetCall> ret_calls=new HashSet<RetCall>();
	
	/**
	 * If is a get method, contains the key of the variable returned by the get function, a.k.a an alias
	 */
	String get=null;
	
	/**
	 * Temporary
	 */
	private HashSet<String> ret_deps=new HashSet<String>();
	
	
	/**
	 * To find the return's deps we need local variable of the function (to know the environment)
	 * And the identifiers involved in the return
	 * 
	 * @param local_vars
	 * @param function_returns
	 * @param params
	 */
	public RetDep(HashSet<String> local_vars,ArrayList<ASTNode> function_returns, ArrayList<String> params){
		this.local_vars=local_vars;
		this.params=params;
		this.function_returns=function_returns;
	}
	
	
	/**
	 * Discover if the function is a "get" function, meaning that it returns a pointer to an object
	 * that is an attribute of some class. 
	 * If it is, it's like a "pointer copy", but with aliasing, calling the function instead of the
	 * attribute directly.
	 */
	private void solve_get(){
		//boolean return_object=false;
		ArrayList<String> deps=new ArrayList<String>();
		for(ASTNode node : function_returns){
			/*if( ((ReturnStatement)node).getExpression()!=null && (((ReturnStatement)node).getExpression().resolveTypeBinding().isClass()
					|| ((ReturnStatement)node).getExpression().getNodeType()==ASTNode.NULL_LITERAL) ){*/
				single_visit(node,deps);
			//	return_object=true;
			//}
		}
		
		//if return an object, could be a get function
		//if(return_object){
			ArrayList<String> final_=new ArrayList<String>();
			ArrayList<String> current=new ArrayList<String>();
			current.addAll(deps);
			deps.clear();
			
			
			while(!current.isEmpty()){
				for(String s : current){
					RefID ref=InitSlicing.ids_table.get(s);
					if(is_local(s)){
						if(ref.write!=null)
							for(ASTNode node : ref.write)
								visit_ret(node);
					}
				}
				current.addAll(deps);
				current.removeAll(final_);
				final_.addAll(current);
			}
			
			for(String key : final_)
				if(is_external(key)){
					if(get!=null)
						;//InitSlicing.fatal("wtf#1");
					else
						get=key;
				}
			
		//}
	}
	
	private void single_visit(ASTNode n, ArrayList<String> deps){
		RefStmt rs = InitSlicing.stmt_table.get(n);
		
		if(rs!=null){
			if(rs.read!=null)
				for(String var: rs.read)
					deps.add(var);
		}
	}
	
	/**
	 * This function resolve the return dependencies
	 */
	public void resolve_ret_deps(){
		
		solve_get();
		
		
		HashSet<String> current = new HashSet<String>();
		/**
		 * Visit return nodes and fetch out identifier used in it
		 */
		for(ASTNode node:function_returns){
			visit_ret(node);
		}
		
		current.addAll(deps);
		deps.clear();
		
		/**
		 * Cycle while saturation or convergence
		 */
		while(!current.isEmpty()){
			for(String s : current){
				RefID ref=InitSlicing.ids_table.get(s);
				if(ref.is_call)
					add_call(s,ref.invocation);
				else if(is_local(s)){
					//if(ref.read!=null)
					//	for(ASTNode node : ref.read)
					//		visit_ret(node);
					if(ref.write!=null)
						for(ASTNode node : ref.write)
							visit_ret(node);
				}
			}
			current.addAll(deps);
			current.removeAll(ret_deps);
			ret_deps.addAll(current);
		}
		
		//HashSet<String> local_vars_except_params=new HashSet<String>();
		//local_vars_except_params.addAll(local_vars);
		//local_vars_except_params.removeAll(params);
		
		//ret_deps.removeAll(local_vars_except_params);
		
		/**
		 * divide params and external by static and non static
		 */
		for(String var: ret_deps){
			Var v=new Var();
			/**
			 * it is a parameter
			 */
			int index;
			if((index=params.indexOf(var))!=-1){
				v.param_index=index;
				v.is_static=false;
			}
			/**
			 * it is an external variable
			 */
			else{
				v.param_index=-1;
				RefID ref=InitSlicing.ids_table.get(var);
				v.is_static=ref.is_static;
				v.key=var;
				if(!v.is_static )//&& !var.contains(" "))
					v.is_local_field=true;
			}
			return_dep.add(v);
		}
		
	}
	
	/**
	 * Create a new @RetCall object and add it to the method dependencies
	 * @param call_name
	 * @param invocation
	 */
	private void add_call(String call_name, ASTNode invocation){
		RetCall c=new RetCall();
		c.call_name=call_name;
		List<Expression> l;
		//throw exception when invocation isn't a methodinvocation but a classinstancecreation
		try{
			l=((MethodInvocation)invocation).arguments();
		}
		catch(Exception e){return;}
		
		for(Expression e : l){
			getParams gp=new getParams();
			e.accept(gp);
			
			for(String s : gp.keys){
				Var v=new Var();
				int index;
				
				//if(InitSlicing.ids_table.get(s).is_call)
				//	continue;
				
				if((index=params.indexOf(s))!=-1){
					v.param_index=index;
				}
				else{
					v.param_index=-1;
					v.key=s;
				}
				Var v1=new Var();
				c.params.add(v);
			}
		}
		
		ret_calls.add(c);
	}
	
	/**
	 * Visit node, add identifier used, visit the father recursively
	 * @param node
	 */
	private void visit_ret(ASTNode node){
		if(node.getNodeType()==ASTNode.BLOCK || node.getNodeType()==ASTNode.METHOD_DECLARATION)		//	reached method stub
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
		visit_ret(node.getParent());
		
	}
	
	
}
