package com.slicerplugin.slicer.dependencies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;

import com.slicerplugin.generic.Pair;
import com.slicerplugin.slicer.parser.InitSlicing;
import com.slicerplugin.slicer.parser.RefID;
import com.slicerplugin.slicer.parser.RefStmt;

/**
 * Used to compute deps of external variables (in relation to a function)
 * 
 * @author Tomas Bortoli
 *
 */
public class ExtDep extends Dep{
	
	/**
	 * _Global
	 * EXT_VAR -> SET(CALLS)
	 */
	public static HashMap<String,HashSet<String>> ext_to_calls = new HashMap<String,HashSet<String>>();
	
	/**
	 * EXT_VAR -> SET(NODES)
	 * Nodes that write an external variable
	 */
	HashMap<String,ArrayList<ASTNode>> ext_write;
	
	/**
	 * EXT_VAR -> SET(FUNCTION_NODE,INDEX)
	 * When external variables are passed as parameters
	 */
	HashMap<String,ArrayList<Pair<String,Integer>>> ext_as_param;
	
	/**
	 * Variables that modify an external variable inside a function
	 */
	HashMap<String,HashSet<Var>> ext_deps=new HashMap<String,HashSet<Var>>();
	
	public ExtDep(HashMap<String,ArrayList<ASTNode>> ext_write,HashMap<String,ArrayList<Pair<String,Integer>>> ext_as_param
			,HashSet<String> local_vars, ArrayList<String> params){
		this.ext_write=(HashMap<String, ArrayList<ASTNode>>) ext_write.clone();
		this.ext_as_param=(HashMap<String, ArrayList<Pair<String,Integer>>>) ext_as_param.clone();
		
		this.local_vars=local_vars;
		this.params=params;
	}
	
	/**
	 * For each ext_var modified inside this function, find modificants factors and map them.
	 */
	public void resolve_ext_deps(){
		
		HashSet<String> local_vars_except_params=new HashSet<String>();
		local_vars_except_params.addAll(local_vars);
		local_vars_except_params.removeAll(params);
		
		HashSet<String> deps=new HashSet<String>(),total=new HashSet<String>(),deps1=new HashSet<String>();
		
		for(String ext : ext_write.keySet()){
			ArrayList<ASTNode> modificants=ext_write.get(ext);
			for(ASTNode node : modificants){
				visit(node,deps1);
			}
			deps.addAll(deps1);
			total.addAll(deps);
			while(!deps.isEmpty()){
				for(String current : deps){
					RefID ref=InitSlicing.ids_table.get(current);
					if(is_local(current)){
						if(ref.write!=null)
							for(ASTNode node : ref.write)
								visit(node,deps1);
					}
				}
				deps.addAll(deps1);
				deps.removeAll(total);
				total.addAll(deps);
				deps1.clear();
			}
			
			
			total.removeAll(local_vars_except_params);
			
			HashSet<Var> deps_=new HashSet<Var>();
			ext_deps.put(ext, deps_);
			for(String var: total){
				Var v=new Var();
				/**
				 * it is a parameter
				 */
				int index;
				if((index=params.indexOf(var))!=-1){
					v.param_index=index;
					v.is_static=false;
				}
				else if(local_vars.contains(var)){
					
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
				deps_.add(v);
			}
			total.clear();
		}
		
		
		for(String ext : ext_as_param.keySet()){
			HashSet<Var> v=ext_deps.get(ext);
			if(v==null)	{	v=new HashSet<Var>(); ext_deps.put(ext,v); }
			if(v.size()==0){
				boolean trash=true;
				ArrayList<Pair<String,Integer>> as_param=ext_as_param.get(ext);
				DependenciesAnalyzer da=new DependenciesAnalyzer();
				for(Pair<String,Integer> p : as_param){
					ArrayList<Var> v_;
					String x[]=p.getFirst().split(" ");
					if((v_=da.solve_param("",x[x.length-1]/*InitSlicing.getKey(((MethodInvocation)p.getFirst()).getName())*/,p,false)).size()>0){
						trash=false;
						List<Expression> l;
						try{
							l=((MethodInvocation)InitSlicing.ids_table.get(p.getFirst()).invocation).arguments();//((MethodInvocation)p.getFirst()).arguments();
						}
						catch(Exception e){
							l=((ClassInstanceCreation)InitSlicing.ids_table.get(p.getFirst()).invocation).arguments();
						}
						for(Var vv_ : v_){
							if(vv_.param_index!=-1){
								getParams gp=new getParams();
								l.get(vv_.param_index).accept(gp);
								int index;
								for(String s : gp.keys)
									if((index=params.indexOf(s))!=-1){
										Var dep=new Var();
										dep.param_index=index;
										v.add(dep);
									}
							}
						}
						
						break;
					}
				}
				if(trash)
					ext_deps.remove(ext);
			}
		}			
	}
		
	
	
	private void visit(ASTNode node, HashSet<String> deps){
		RefStmt ref=InitSlicing.stmt_table.get(node);
		if(ref!=null){
			if(ref.read!=null)
				deps.addAll(ref.read);
			if(ref.write!=null)
				deps.addAll(ref.write);
		}
	}
	
	/**
	 * Function that transorm:
	 * FUN -> EXTS MODIFIED
	 * to
	 * CALL N -> EXTS MODIFIED
	 * 
	 * In practice it solve the nested calls and the context of the calls
	 */
	public static void solve_context_and_nested_calls(){
		
		boolean changed=true;
		while(changed){
			changed=false;
			for(String method : InitSlicing.methods.keySet()){
				MethodDep md=InitSlicing.methods.get(method);
				HashSet<String> nested_calls=InitSlicing.method_nested_calls.get(method);
				if(nested_calls!=null)	for(String nested_call : nested_calls){
					boolean internal=md.ed.check_key(nested_call);
					//if(nested_call.split(" ").length-2>0){
					String method_name = nested_call.split(" ")[nested_call.split(" ").length-2];
					if(!internal){
						String obj=get_key(nested_call);
						
						if(InitSlicing.userDefinedTypesAndMethods.contains(method_name)){
							ExtDep ed=InitSlicing.methods.get(method_name).ed;
							
							for(String ext_ : ed.ext_deps.keySet()){
								String full=obj;
								if(full.length()>0)
									full+=" ";
								full+=ext_;
								
								if(!md.ed.ext_deps.containsKey(full)){
									HashSet<Var> deps = new HashSet<Var>();
									md.ed.ext_deps.put(full, deps);
									changed=true;
									
									RefID rid=InitSlicing.ids_table.get(nested_call);
									List<Expression> l;
									if(rid.invocation.getNodeType()==ASTNode.METHOD_INVOCATION)
										l=((MethodInvocation)rid.invocation).arguments();
									else
										l=((ClassInstanceCreation)rid.invocation).arguments();
									
									for(Var v : ed.ext_deps.get(ext_)){
										getParams gp=new getParams();
										if(v.param_index!=-1){
											Expression e=(Expression) l.get(v.param_index);
											e.accept(gp);
										}
										for(String k : gp.keys){
											int index;
											if((index=md.ed.params.indexOf(k))!=-1){
												Var v_=new Var();
												v_.param_index=index;
												v_.is_static=false;
												deps.add(v_);
											}
										}
									}
									
								}
							}
						}
						else{
							
							if(!md.ed.ext_deps.containsKey(obj) && !obj.equals("")){
								md.ed.ext_deps.put(obj, new HashSet<Var>());
								changed=true;
							}
						}
					//}
					}
				}
			}
		}
		
		
		/*HashMap<String,HashSet<String>> tmp = new HashMap<String,HashSet<String>>();
		for(String key:InitSlicing.methods_to_calls.keySet()){
			HashSet<String> deps=InitSlicing.methods_to_calls.get(key);
			String alias=DependenciesAnalyzer.solve_alias(key);
			if(!alias.equals(key) && !alias.equals("")){
				tmp.put(alias, deps);
			}	
		}
		InitSlicing.methods_to_calls.putAll(tmp);
		*/
		
		
		for(String method_ : InitSlicing.methods_to_calls.keySet()){
			String[] g=method_.split(" ");
			String method=g[g.length-2];
			HashSet<String> calls = InitSlicing.methods_to_calls.get(method_);
			MethodDep md = InitSlicing.methods.get(method);
			if(calls!=null){	for(String call : calls){
					String key=DependenciesAnalyzer.solve_alias(get_key(call));
					if(InitSlicing.userDefinedTypesAndMethods.contains(method)){
						for(String ext : md.ed.ext_deps.keySet()){
							String full=null;
							RefID ref = InitSlicing.ids_table.get(ext);
							/*if(ref==null){
								ref=new RefID();
								ref.key=ext;
								ref.is_static=false;
								InitSlicing.ids_table.put(ext, ref);
							}*/
							
							if(ref!=null && ref.is_static){
								full=ext;
							}
							else{
								full=key;
								if(full.length()>0)
									full+=" ";
								full+=ext;
								
								
								if(!key.equals("")){
									RefID r=InitSlicing.ids_table.get(key);
									if(r!=null){
										InitSlicing.add_ref(r,full,InitSlicing.ids_table.get(ext.split(" ")[ext.split(" ").length-1]).is_object,true,false,false);
									}
								}	
							}
							
							HashSet<String> ref_calls=ext_to_calls.get(full);
							if(ref_calls==null){
								ref_calls=new HashSet<String>();
								ext_to_calls.put(full, ref_calls);
							}
							ref_calls.add(call);
						}
					}
				}
			}
		}
	}
	
	private static String get_key(String call){
		String[] split=call.split(" ");
		String str="";
		for(int i=0;i<split.length-2;i++){
			str+=split[i]+" ";
		}
		if(str.length()>0)
			str=str.substring(0,str.length()-1);
		return str;
	}
	
	private boolean check_key(String call){
		boolean ret=false;
		String[] split=call.split(" ");
		String str="";
		for(int i=0;i<split.length-2;i++){
			str+=split[i];
			ret|=is_local(str);
			str+=" ";
		}
		
		return ret;
	}
	
}
