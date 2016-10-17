package com.slicerplugin.slicer.dependencies;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;

import com.slicerplugin.generic.Constants;
import com.slicerplugin.generic.Pair;
import com.slicerplugin.slicer.dependencies.ParamDep;
import com.slicerplugin.slicer.parser.InitSlicing;
import com.slicerplugin.slicer.parser.RefID;
import com.slicerplugin.slicer.parser.RefStmt;

/**
 * This class analyze a set of compilation units to find dependencies between the
 * variables in the V set and the rest of the program.
 * Its aim is to find statements that influence other statements of interest
 * 
 * In practice, it follows write references on identifiers, it resolve parameter
 * dependencies, return dependencies and external variable dependencies.
 * Further, object must be treated as a special case. Read the code
 * Unknown object and methods are even more special cases.
 * 
 * The algorithm run until saturation of the V set.
 * The nodes we want in the slice are the marked nodes (set Constants.inSlice Property)
 * 
 * PRECONDITION:
 * 	Before instantiating this class, @InitSlicing class must be run through all
 *	the compilation units contained in the project [a.k.a step #0]. This because 
 *  this class use static data present in InitSlicing, including the dependencies graph.
 * 
 * @author Tomas Bortoli
 *
 */
public class DependenciesAnalyzer {
	
	/**
	 * Current state, and next state of the dependencies' algorithm
	 */
	HashSet<String> V_current,V_next=new HashSet<String>();	
	
	/**
	 * Additional nodes to visit
	 */
	HashSet<ASTNode> markedNodes;
	
	public DependenciesAnalyzer(){}
	
	public DependenciesAnalyzer(HashSet<String> V){
		V_current=new HashSet<String>();
		V_current.addAll(V);
	}
	
	public void add_marked_nodes(HashSet<ASTNode> markedNodes){
		this.markedNodes=markedNodes;
	}
	
	private void add_var(String x){
		if(x!=null){
			V_next.add(x);
			//System.out.println(s+"->"+x);
		}
	}
	
	
	public static String solve_alias(String s){
		
		//****
		if(s==null)
			return "";
		
		String ret="";
		String[] pieces=s.split(" ");
		for(int i=0;i<pieces.length;i++){
			if(i+1<pieces.length && is_integer(pieces[i+1])){
				String get;
				if((InitSlicing.methods.get(pieces[i])!=null) && (get=InitSlicing.methods.get(pieces[i]).rd.get)!=null)
					ret+=get;
				else
					return "";
				i++;
			}
			else
				ret+=pieces[i];
			ret+=" ";
		}
		if(ret.length()>0)
			ret=ret.substring(0,ret.length()-1);
		
		return ret;
	}
	
	String s="";
	/**
	 * Run to build slices
	 */
	public void algorithm(){
		
		
		if(markedNodes!=null){
			for(ASTNode node:markedNodes){
				visit_node(node);
			}
		}
		V_current.addAll(V_next);
		
		do{
			V_next=new HashSet<String>();
			print_V();
			
			for(String s : V_current){
				//System.out.println(s);
				this.s=s;
				
				add_var(solve_alias(s));
				
				
				
				HashSet<String> calls=ExtDep.ext_to_calls.get(s);
				if(calls!=null){
					for(String call : calls)
						mark_call(call,s);
					
					//Recover internal calls because they could be aliased
					for(String call :calls){
						String[] pieces=call.split(" ");
						for(int i=0;i<pieces.length;i++){
							if(is_integer(pieces[i])){
								V_next.add(pieces[i-1]);
								//
								MethodDep md=InitSlicing.methods.get(pieces[i-1]);
								if(md!=null){
									RetDep rd=md.rd;
									/**
									 * Mark the return nodes of the function of interest
									 */
									for(ASTNode node : rd.function_returns){
										standalone_declaration(node);
									}
								}
							}
						}
					}
					
						
				}
				
				RefID rs;
				if((rs=InitSlicing.ids_table.get(s))!=null){
					if(rs.is_of_known_type)
						add_var(rs.type);
					if(rs.def!=null)
						for(ASTNode def : rs.def)
							standalone_declaration(def);
					
					for(int i=0;i<rs.refs.size();i++){
						Pair<String,ASTNode> p;
						add_var((p=rs.refs.get(i)).getFirst());
						visit_node(p.getSecond());
					}
					
					if(rs.is_call){
						visit_node(rs.invocation);
						solve_return(rs, new HashSet<String>());
					}
					if(rs.param!=null){
						for(Pair<String,Integer> p : rs.param){
							allCalls.clear();
							
							String[] pieces=p.getFirst().split(" ");
							String father="";
							for(int j=0;j<pieces.length-2;j++)	father+=pieces[j]+" ";
							if(father.length()>0)	father=father.substring(0,father.length()-1);
							solve_param(father,pieces[pieces.length-2],p,true);
						}
					}
					//If has one or more fathers, add them all
					//if(rs.father!=null){
					String ss=s;
					Pair<String,String> p;
					while((p=InitSlicing.getPrefixSuffix(ss)).getFirst()!=""){
						add_var(p.getSecond());
						add_var(p.getFirst());
						
						int index;
						if((index=p.getFirst().indexOf(' '))!=-1)
							add_var(p.getFirst().substring(index+1)+" "+p.getSecond());
						ss=p.getFirst();
					}
						
					//}
					if(rs.is_object && !rs.is_type){
						
						//V_next.add(rs.key.split(" ")[rs.key.split(" ").length-1]);
						
						/*if(rs.is_of_known_type && rs.father==null){
							HashSet<String> f=InitSlicing.objects_to_fields.get(rs.type);
							if(f!=null)
								for(String str : f){
									if(InitSlicing.V.contains(str) || V_next.contains(str))
										add_var(rs.key+" "+str);
								}
						}*/
						
						/**
						 * I could put this part as optional by user's decision.
						 * Because if an object is of an unknown structure the best thing is to
						 * follow everything, but also ignoring could make sense.
						 */
						if(!rs.is_of_known_type && rs.fields!=null)
							for(RefID id : rs.fields){
								add_var(id.key);
							}
						if(!rs.is_of_known_type && rs.invocations!=null){
							for(RefID call : rs.invocations)
								add_call(call);
						}
						
						/**
						 * If an object is read and on the same statement something else is written,
						 * with high probability we've a "pointer copy"
						 */
						if(rs.read!=null){
							for(ASTNode n : rs.read){
								RefStmt r=InitSlicing.stmt_table.get(n);
								if(r!=null && r.write!=null)
									visit_node(n);
							}
						}
					}
					
					//checkout writes to identifiers of interests
					if(rs.write!=null){
						for(ASTNode n : rs.write){
							visit_node(n);
						}
					}
				}
				//else{
				//	InitSlicing.fatal("Identifier \""+s+"\" does not exist (?!??)");
				//}
				
				
			}
			
			//remove the already seen elements. [ no need to process the same identifiers more times ]
			V_next.removeAll(InitSlicing.V);
			InitSlicing.V.addAll(V_next);
			//set up new current state
			V_current.clear();
			V_current.addAll(V_next);
			
			
		}while(V_next.size()!=0);
		
	}
	
	private void mark_call(String call, String var) {
		String[] tmp=call.split(" ");
		String var_=""; int i;
		for(i=0;i<tmp.length-2;i++)	;
		//i--;
		tmp=var.split(" ");
		for(;i<tmp.length;i++) var_+=tmp[i]+" ";
		if(var_.length()>0)	var_=var_.substring(0,var_.length()-1);
		
		String method=call.split(" ")[call.split(" ").length-2];
		MethodDep md=InitSlicing.methods.get(method);
		
		RefID ref=InitSlicing.ids_table.get(call);
		ASTNode call_node = ref.invocation;
		visit_node(call_node);
		ASTNode method_node= ref.method_declaration;
		
		List<Expression> exps;
		if(call_node.getNodeType()==ASTNode.METHOD_INVOCATION)	exps=((MethodInvocation)call_node).arguments();
		else	exps=((ClassInstanceCreation)call_node).arguments();
		
		List<Expression> args=((MethodDeclaration)method_node).parameters();
		
		HashSet<Var> vars=md.ed.ext_deps.get(var_);
		
		standalone_visit2(call_node);
		standalone_declaration(method_node);
		if(vars!=null)	for(Var v : vars){
			if(v.param_index==-1){
				add_var(v.key);
			}
			else{
				standalone_visit(exps.get(v.param_index));
				standalone_visit(args.get(v.param_index));
			}
		}
	}

	/**
	 * Add a call to the slice. Mark invocation and gather parameters' ids
	 * @param call
	 */
	private void add_call(RefID call){
		
		visit_node(call.invocation);
		
		
		List<Expression> l=((MethodInvocation)call.invocation).arguments();
		for(Expression e : l)
			standalone_visit(e);
	}
	
	
	private static boolean is_integer(String s){
		boolean ret=true;
		try{
			Integer.parseInt(s);
		}
		catch(Exception e){
			ret=false;
		}
		return ret;
	}
	
	/**
	 * Solve the dependencies of a  return.
	 * If the below code is executed, you are interested in a return value.
	 * So find out significant variables, functions, mark them and add them to the slice.
	 * 
	 * @param call the unique call to analyze
	 * @return
	 */
	private ArrayList<Var> solve_return(RefID call, HashSet<String> allCalls){
		
		/**
		 * Mark the invocation and the declaration nodes
		 */
		standalone_visit2(call.invocation);
		standalone_declaration(call.method_declaration);
		
		
		/**
		 * Return list of dependencies
		 */
		ArrayList<Var> ret=new ArrayList<Var>();
		
		if(allCalls.contains(call.key))
			return ret;
		allCalls.add(call.key);
		
		/**
		 * Pointer to the method dependencies built during the pre-computation to slice
		 */
		MethodDep md=InitSlicing.methods.get(call.call_name);
		
		List<Expression> l;
		if(call.invocation.getNodeType()==ASTNode.METHOD_INVOCATION)
			l=((MethodInvocation)call.invocation).arguments();
		else
			return ret;
		
		ArrayList<String>[] params=call.params_inline;
		
		if(md!=null){
			RetDep rd=md.rd;
			/**
			 * Mark the return nodes of the function of interest
			 */
			for(ASTNode node : rd.function_returns){
				standalone_declaration(node);
			}
			
			/**
			 * For each call that influence the return, recursively analyze it and eventually
			 * report new parameters or external variables of interest
			 */
			for(RetCall rc : rd.ret_calls){
				ArrayList<Var> res=solve_return(InitSlicing.ids_table.get(rc.call_name),allCalls);
				for(Var v : res){
					if(v.param_index!=-1 && v.param_index<rc.params.size())
						ret.add(rc.params.get(v.param_index));//getValuesByIndex(rc.params,v.param_index));
					else
						ret.add(v);
				}
			}
			
			
			
			//Extract the fathers string (complementary to the call string)
			String inheritance="";
			String[] fathers = call.key.split(" ");
			for(int i=0;i<fathers.length-2;i++){
				if(is_integer(fathers[i+1])){
					inheritance+=InitSlicing.methods.get(fathers[i]).rd.get+" ";
					i++;
				}
				else
					inheritance+=fathers[i]+" ";
			}
			if(inheritance.length()>0)
				inheritance=inheritance.substring(0,inheritance.length()-1);
			//
			
			/**
			 * Here we have all the deps ready to be added
			 */
			List<SingleVariableDeclaration> ll=((MethodDeclaration)call.method_declaration).parameters();
			ret.addAll(rd.return_dep);
			/**
			 * For each dep
			 */
			for( Var id : ret){
				/**
				 * It's a param, mark the param node in the invocation and in the declaration
				 */
				if(id.param_index!=-1 && id.param_index<l.size()){
					standalone_visit(l.get(id.param_index));
					if(params[id.param_index]!=null)
						for(String s : params[id.param_index])
							add_var(s);
					
					standalone_visit(ll.get(id.param_index));				
				}
				/**
				 * It's an external variable
				 */
				else{
					add_var(id.key);
					
					if(id.is_local_field && (!id.is_static)){
						
						add_var(inheritance+" "+id.key);
					}
				}
			}
			
			
		}
		else{
			/**
			 * Unknow call definition, take all the parameters as significant
			 */
			standalone_visit2(call.invocation);
			int i=0;
			for(Expression e : l){
				Var v=new Var(); v.param_index=i++;
				ret.add(v);
				standalone_visit(e);
				System.out.println(v.param_index);
				System.out.println(params);
				
				if(params!=null && params[v.param_index]!=null)
					for(String s : params[v.param_index])
						add_var(s);
			}
		}
		
		return ret;
	}
	
	/**
	 * The list of all the calls made in a visit to resolve a parameter's dependencies
	 */
	HashSet<Pair<String,Integer>> allCalls=new HashSet<Pair<String,Integer>>();
	
	/*private static void debug_(String call_key,List<Expression> l){
		System.out.println(call_key);
		for(int i=0;i<l.size();i++)
			if(l.get(i).getProperty(Constants.inSlice)!=null)
				System.out.print("1 ");
			else
				System.out.print("0 ");
		System.out.println();
	}*/
	
	
	/**
	 * Solve the dependencies of a param. The below code is executed only if we have an object passed as parameter
	 * and it's content could be modified within a function. So, we want to know which variable and methods modify it.
	 * And of course, if variables are parameters, they should be keeped also in the invocation further than in the declaration
	 * @param father
	 * @param keyOfCall
	 * @param p
	 * @return
	 */
	public ArrayList<Var> solve_param(String father, String keyOfCall, Pair<String,Integer> p, boolean mark){
		ArrayList<Var> deps = new ArrayList<Var>();
		
		MethodDep md=InitSlicing.methods.get(keyOfCall);
		
		ASTNode invocation=InitSlicing.ids_table.get(p.getFirst()).invocation;//.getParent();
		//ASSERTION #¹
		while(invocation.getNodeType()!=ASTNode.METHOD_INVOCATION &&
				invocation.getNodeType()!=ASTNode.CLASS_INSTANCE_CREATION) invocation=invocation.getParent();
		
		List<Expression> l=null;
		if(invocation.getNodeType()==ASTNode.METHOD_INVOCATION)	l=((MethodInvocation)invocation).arguments();
		else if(invocation.getNodeType()==ASTNode.CLASS_INSTANCE_CREATION)	l=((ClassInstanceCreation)invocation).arguments();
		
		ArrayList<String>[] params=InitSlicing.ids_table.get(p.getFirst()).params_inline;
		/**
		 * It's a user defined function
		 */
		if(md!=null){
			if(!allCalls.contains(p)){
				allCalls.add(p);
				
				MethodDeclaration method_declaration=((MethodDeclaration)InitSlicing.methods_declarations.get(keyOfCall));
				List<Expression> param_decl=method_declaration.parameters();
				
				/*String param=md.params.get(p.getSecond());
				System.out.println(param);
				RefID ref=InitSlicing.ids_table.get(param);
				if(ref.fields!=null){
					for(RefID field : ref.fields){
						String field_key=solve_alias(field.key);
					}
				}
				*/
				
				
				ParamDep pd=md.pd;
				ArrayList<Var> v_=pd.subparams_deps.size()>p.getSecond() ? pd.subparams_deps.get(p.getSecond()) : null;
				if(v_==null)	return new ArrayList<Var>();
				
				deps.addAll(v_);
				
				
				
				/**
				 * For each call in which the parameter (of the call) is passed to.
				 * This to find out nested writes and so on.
				 * Each record represent a precise parameter passed to another function
				 */
				for(Call c:pd.calls){
					/**
					 * If the parameter match the one we are computing for
					 */
					if(p.getSecond()==c.originalIndex){
						ArrayList<Var> inherited=new ArrayList<Var>();
						/**
						 * recursively solve..
						 */
						String father_=""; String[] pieces=c.father.split(" ");
						for(int i=0;i<pieces.length-2;i++)	father_+=pieces[i]+" ";
						if(father_.length()>0)	father_=father_.substring(0,father_.length()-1);
						
						inherited=solve_param(father_, c.method,new Pair<String,Integer>(c.father,c.newIndex),mark);
						List<Expression> lll = ((MethodInvocation)c.invocation).arguments();
						for(Var v : inherited){
							getParams gp=new getParams();
							if(v.param_index!=-1){
								lll.get(v.param_index).accept(gp);
								for(String s : gp.keys){
									Var vv=new Var();
									int index;
									vv.param_index=-1;
									if((index=pd.params.indexOf(s))!=-1)
										vv.param_index=index;
									else
										vv.key=s;
									deps.add(vv);
								}
							}
							else
								deps.add(v);
						}
						
					}
				}
				
				/**
				 * If we have at least a dependency, the param is modified in the function and we must mark its nodes to put it in the slice
				 */
				if(deps.size()>0){
					if(mark){
						visit_node(invocation);//standalone_visit2(invocation);
						standalone_declaration(method_declaration);
						standalone_visit(l.get(p.getSecond()));
						if(params[p.getSecond()]!=null)
							for(String s : params[p.getSecond()])
								add_var(s);
						
						standalone_visit(param_decl.get(p.getSecond()));
					}
				}
				/**
				 * Mark and add each dependency
				 */
				for(Var v : deps){
					if(v.param_index==-1){
						add_var(v.key);
						if(v.is_local_field && father.length()>0){
							add_var(father+" "+v.key);
						}
					}
					else{
						if(mark){
							standalone_visit(l.get(v.param_index));
							if(params[v.param_index]!=null)
								for(String s : params[v.param_index])
									add_var(s);
							
							standalone_visit(param_decl.get(v.param_index));
						}
					}
				}
			}
		}
		else{
			/**
			 * Unknow call definition, so consider all the parameters as importants
			 */
			if(mark)	standalone_visit2(invocation);
			int i=0;
			for(Expression e : l){
				Var v=new Var(); v.param_index=i++;
				deps.add(v);
				if(mark){
					standalone_visit(e);
					if(params[v.param_index]!=null)
						for(String s : params[v.param_index])
							add_var(s);
				}
			}
		}
		return deps;
	}
	
	/**
	 * Just mark the node and brothers
	 * @param node
	 */
	private void standalone_declaration(ASTNode node){
		if(node==null)	return;
		ASTNode parent=node;
		if(node.getNodeType()==ASTNode.METHOD_DECLARATION)
			((MethodDeclaration)node).getBody().setProperty(Constants.inSlice, true);
		if(node.getNodeType()==ASTNode.RETURN_STATEMENT)
			while((parent=parent.getParent()).getNodeType()!=ASTNode.METHOD_DECLARATION)
				parent.setProperty(Constants.inSlice, true);
		node.setProperty(Constants.inSlice, true);
	}
	
	/**
	 * Mark the node and the EXPRESSION_STATEMENT that contains it. If there is.
	 * @param node
	 */
	private void standalone_visit2(ASTNode node){
		//do{
		if( node.getNodeType()==ASTNode.METHOD_INVOCATION && ((MethodInvocation)node).getExpression()!=null)
			((MethodInvocation)node).getExpression().accept(an);
		node.setProperty(Constants.inSlice, true);
		node=node.getParent();
		if(node.getNodeType()==ASTNode.EXPRESSION_STATEMENT)
			node.setProperty(Constants.inSlice, true);
	}
	private void standalone_visit(ASTNode node){
		node.setProperty(Constants.inSlice, true);
		node.accept(an);
	}
	
	
	addNames an=new addNames();
	/**
	 * Class that add all the names found in a branch of the graph to the current slice
	 * 
	 * @author Tomas Bortoli
	 *
	 */
	class addNames extends ASTVisitor{
		@Override public boolean visit(MethodInvocation node){
			String call=InitSlicing.call_table.get(node);
			if(call!=null){
				add_var(call);
				return false;
			}
			//else	InitSlicing.fatal("Orphan call");
			return false;
		}
		
		@Override public boolean visit(SimpleName node){
			add_var(InitSlicing.getKey(node));
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
					add_var(key);
					key+=" ";
				}
			}
			return false;
		}
	}
	
	/**
	 * Visit a node and add his vars to V and visit recursively his father to preserve the context
	 * @param node
	 */
	private void visit_node(ASTNode node){
		
		//if the node has been already visited
		if(node.getProperty(Constants.inSlice)!=null)
			return;
		
		//visit the node
		node.setProperty(Constants.inSlice, true);
		
		
		//add the identifiers used in this node to V
		add_influents(node);
		
		//to preserve the context
		ASTNode parent=node.getParent();
		if(parent!=null){
			while(parent.getNodeType()==ASTNode.METHOD_INVOCATION || parent.getNodeType()==ASTNode.ASSIGNMENT || 
					parent.getNodeType()==ASTNode.EXPRESSION_STATEMENT || parent.getNodeType()==ASTNode.INFIX_EXPRESSION)
				parent=parent.getParent();
			visit_node(parent);
		}
	}
	
	private void add_influents(ASTNode node){
		RefStmt rv = InitSlicing.stmt_table.get(node);
		if(rv!=null){
			if(rv.read!=null){
				for(String s : rv.read)
					add_var(s);
			}
			if(rv.write!=null){
				for(String s : rv.write)
					add_var(s);
			}
		}
	}
	/**
	 * Print all the identifier contained in the slice set
	 */
	private void print_V(){
		System.out.print("\nV: ");
		for(String s : InitSlicing.V) System.out.print(s+" "); 
		System.out.println();
		
	}
}
