package com.slicerplugin.slicer.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.PrefixExpression.Operator;

import com.slicerplugin.generic.Pair;
import com.slicerplugin.slicer.dependencies.DependenciesAnalyzer;
import com.slicerplugin.slicer.dependencies.ExtDep;
import com.slicerplugin.slicer.dependencies.MethodDep;

/**
 * This class initialize the slicing.
 * It visits an AST that represent a java source file and most of the variables are shared
 * between all the instances to have a global view of the program structure.
 * 
 * It maps for each variable, the statement that use it and how.
 * For each statement it map which variables are used inside and how; [READ/WRITE]
 * Each method and class is mapped too.
 * It keep track of the user defined types and methods and of all the calls
 * and all the objects and the calls from the objects. 
 * Also method's dependencies are computed here. PARAMETERS, RETURN, EXTERNAL VARIABLES
 * 
 * PARAMETERS gets the dependencies of parameters of function/methods calls
 * RETURN gets the dependencies of return value of functions
 * EXTERNAL VARIABLES gets the dependencies of variables external to any function
 * 
 * Most of the data structures use are Maps, to bind directly identifiers together building
 * the dependencies graph
 * 
 * 		       ////////////////////\\\\\\\\\\\\\\\\\\\\\
 *			  /////////////////////\\\\\\\\\\\\\\\\\\\\\\ 			
 * 			 /CONTROL FLOW GRAPH ----> DEPENDENCIES GRAPH\
 * 			 \\\\\\\\\\\\\\\\\\\\\\///////////////////////
 * 			  \\\\\\\\\\\\\\\\\\\\\//////////////////////
 * 
 * A big part of the **Slicing** is done here.
 * This is a "preparative" part. But very heavy.
 * Here is spent most of the time for slicing, so this process should be 
 * put in background asynchronously respect to the slice computing.
 * For better user experience
 * 
 * 
 * 
 * @author Tomas Bortoli
 *
 */
public class InitSlicing extends ASTVisitor{
	
	/**
	 * The java modules
	 */
	//ArrayList<CompilationUnit> modulesAST=new ArrayList<CompilationUnit>();
	
	/**
	 * Current module in analysis
	 */
	CompilationUnit cu;
	
	/**
	 * Offsets marked from the user in this compilation unit
	 */
	ArrayList<Pair<Integer,Integer>> offsets;
	
	/**
	 * The V set contains all the variables in which we are interested in
	 */
	public static HashSet<String> V=new HashSet<String>();
	
	/**
	 * Hash table that contains for each identifier, each statement that use it
	 */
	public static HashMap<String,RefID> ids_table=new HashMap<String,RefID>();
	
	/**
	 * A map, for each node track read and written variables
	 */
	public static HashMap<ASTNode,RefStmt> stmt_table=new HashMap<ASTNode,RefStmt>();
	
	/**
	 * Reference counter for each call (to name each call differently)
	 */
	static HashMap<String,Integer> call_counter=new HashMap<String,Integer>();
	public static HashMap<ASTNode, String> call_table=new HashMap<ASTNode,String>();
	
	/**
	 * When there's a complex call like obj.fun().fun1()... or fun().fun2().... to track the previous calls
	 */
	static String complex_call="";
	
	/**
	 * Current node in analysis
	 */
	static ASTNode current;
	
	/**
	 * Is the current node a declaration?
	 */
	static boolean is_decl=false;
	
	static Stack<SimpleName> invokedMethod=new Stack<SimpleName>();
	static Stack<Integer> accessFieldCounter=new Stack<Integer>();
	
	/**
	 * Is the current node marked?
	 */
	static boolean marked=false;
	
	/**
	 * Delineate if the current statement is writing,reading an identifier
	 */
	static boolean write=false,read=false;
	
	/**
	 * Flags of the ast's visit
	 */
	static boolean is_call=false, is_param=false, is_param_decl=false;
	
	static boolean is_field=false;
	
	static boolean is_constructor=false;
	
	static String c_sig;
	
	/**
	 * stack of function parameter index, cause complex calls can happen
	 */
	static Stack<Integer> param_index=new Stack<Integer>();
	static Stack<ASTNode> current_call=new Stack<ASTNode>();
	
	static boolean more_params=false;
	
	static int n_params=0;
	static String last_call;
	
	/**
	 * Indicates how much deep is the current visit.
	 * In this way I can check when deepness == 1, need to register variable-statement association
	 */
	static short deepness=0;
	
	//static String current_params;
	
	/**
	 * For each method, associate its dependencies
	 */
	public static HashMap<String,MethodDep> methods=new HashMap<String,MethodDep>();
	
	/**
	 * Temporary structs used in method dependency analysis
	 */
	static HashSet<String> local_vars=new HashSet<String>();
	static ArrayList<String> params=new ArrayList<String>();
	static ArrayList<ASTNode> function_returns=new ArrayList<ASTNode>();
	
	private static HashMap<String,ArrayList<ASTNode>> ext_write=new HashMap<String,ArrayList<ASTNode>>();
	private static HashMap<String,ArrayList<Pair<String,Integer>>> ext_as_param= new HashMap<String,ArrayList<Pair<String,Integer>>>();
	
	/**
	 * Map method key with real ast node
	 */
	public static HashMap<String,ASTNode> methods_declarations=new HashMap<String,ASTNode>();
	
	public static HashMap<String,HashSet<String>> methods_to_calls = new HashMap<String,HashSet<String>>();
	public static HashMap<String,HashSet<String>> method_nested_calls = new HashMap<String,HashSet<String>>();
	
	private static Stack<String> current_method=new Stack<String>();
	
	/**
	 * All the calls from instantiated objects (needed to update their dependencies)
	 */
	public static HashSet<String> calls_from_objects=new HashSet<String>();
	
	public static HashMap<String,HashSet<String>> objects_to_fields=new HashMap<String,HashSet<String>>();
	
	/**
	 * Classes, methods, enums
	 */
	public static HashSet<String> userDefinedTypesAndMethods=new HashSet<String>();
	
	class FieldRef{
		String key;
		boolean write;
		Pair<String,Integer> param;
		ASTNode node;
		public FieldRef(String key, boolean write, ASTNode node, Pair<String, Integer> param){
			this.key=key;
			this.write=write;
			this.param=param;
			this.node=node;
		}
	}
	private static HashSet<FieldRef> with_alias=new HashSet<FieldRef>();
	
	public static void param_alias_solve(){
		
	}
	public static void global_alias_solve(){
		for(FieldRef ref : with_alias){
			String s=ref.key;
			
			String alias=DependenciesAnalyzer.solve_alias(s);
			RefID rid=ids_table.get(alias);
			
			/**
			 * **********IMPORTANT
			 * COULD NOT BE CORRECT IN ALL CASES
			 */
			if(rid==null){
				rid=new RefID();
				rid.key=alias;
				
				ids_table.put(alias, rid);
			}
			if(!alias.equals("")){
				if(ref.param!=null){
					if(rid.param==null)
						rid.param=new ArrayList<Pair<String,Integer>>();
					rid.param.add(ref.param);
				}else{
					if(ref.write){
						if(rid.write==null)
							rid.write=new ArrayList<ASTNode>();
						rid.write.add(ref.node);
					}
					else{
						if(rid.read==null)
							rid.read=new ArrayList<ASTNode>();
						rid.read.add(ref.node);
					}
				}
			}
		}
	}
	
	static userTypesInit uti=new userTypesInit();
	
	static class userTypesInit extends ASTVisitor{
		@Override public boolean visit(TypeDeclaration node){
			String object=getKey(node.getName());
			userDefinedTypesAndMethods.add(object);
			
			HashSet<String> attributes=objects_to_fields.get(object);
			if(attributes==null){
				attributes=new HashSet<String>();
				objects_to_fields.put(object, attributes);
			}
			FieldDeclaration[] fields=node.getFields();
			for(FieldDeclaration field : fields){
				List<VariableDeclarationFragment> fragments=field.fragments();
				for(VariableDeclarationFragment fragment : fragments){
					if((fragment.getName().resolveBinding().getModifiers()&0x8)==0)
						attributes.add(getKey(fragment.getName()));
				}
			}
			return true;
		}
		@Override public boolean visit(MethodDeclaration node){
			methods_declarations.put(getKey(node.getName()),node);
			userDefinedTypesAndMethods.add(getKey(node.getName()));
			return false;
		}
		@Override public boolean visit(EnumDeclaration node){
			userDefinedTypesAndMethods.add(getKey(node.getName()));
			return false;
		}
	}
	
	class countNames extends ASTVisitor{
		int counter=0;
		@Override public boolean visit(MethodInvocation node){
			counter++;
			return false;
		}
		
		@Override public boolean visit(SimpleName node){
			counter++;
			return false;
		}
		@Override public boolean visit(QualifiedName node){
			counter++;
			return false;
		}
	}
	
	public static void initTypes(CompilationUnit cu){
		cu.accept(uti);
	}
	/**
	 * Initialize all the structures needed for the initialization
	 * 
	 * @param cu the current compilation unit (java class)
	 * @param lines the lines marked in cu
	 */
	public InitSlicing(CompilationUnit cu, ArrayList<Pair<Integer,Integer>> offsets){
		this.cu=cu;
		
		//correct offset ( +1 )
		//Set<Integer> tmp=new HashSet<Integer>();
		//for(Integer i : lines)
		//	tmp.add(i+1);
		this.offsets=new ArrayList<Pair<Integer,Integer>>();
		if(offsets!=null)	this.offsets.addAll(offsets);
		
		if(V==null)
			V=new HashSet<String>();
		if(ids_table==null)
			ids_table=new HashMap<String,RefID>();
		if(stmt_table==null)
			stmt_table=new HashMap<ASTNode,RefStmt>();
		
		if(userDefinedTypesAndMethods==null)
			userDefinedTypesAndMethods=new HashSet<String>();
	}
	
	public static void reset(){
		V.clear();
		ids_table.clear();
		stmt_table.clear();
		userDefinedTypesAndMethods.clear();
		
		methods.clear();
		methods_declarations.clear();
		
		calls_from_objects.clear();
		
		call_counter.clear();
		call_table.clear();
		
		methods_to_calls.clear();
		method_nested_calls.clear();
		
		ExtDep.ext_to_calls.clear();
		
		invokedMethod.clear();
		accessFieldCounter.clear();
		param_index.clear();
		param_call.clear();
		
		deepness=0;
		
		current=null;
		marked=false;
		write=false; read=false;
		
		with_alias.clear();
		complex_call="";
	}
	
	
	public static String getKey(Name node){
		return node.resolveBinding().getKey();
	}
	
	public static void fatal(String err_msg){
		System.out.println("**"+err_msg+"**");
		System.exit(-1);
	}
	
	private boolean isMarked(ASTNode n){
		boolean ret=false;
		for(Pair<Integer,Integer> p : offsets){
			int start=n.getStartPosition();
			int end=n.getStartPosition()+n.getLength();
			if((start>=p.getFirst() &&
					end<=p.getSecond()) ||
					(start<=p.getFirst() &&
					end>=p.getSecond())){
				//System.out.println(n.toString());
				ret=true;
				break;
			}
		}
		return ret;
	}
	
	
	
	/**
	 * Given a key, return the prefix expr (path to reach the attribute or method)
	 * And postfix expr (attribute name or method name)
	 * @param key
	 * @return
	 */
	public static Pair<String,String> getPrefixSuffix(String key){
		String[] tmp=key.split(" ");
		String prefix="";
		int i;
		for(i=0;i<tmp.length-1;i++)
			prefix+=tmp[i]+" ";
		
		if(prefix.length()>0)
			prefix=prefix.substring(0,prefix.length()-1);
		
		String postfix=tmp[i];
		return new Pair<String,String>(prefix,postfix);
	}
	
	
	/**********************************************************************************/
	/***Types declarations, methods declarations and enumeration declarations nodes***/
	/********************************************************************************/
	
	@Override public boolean visit(TypeDeclaration node){
		
		if(current==null)	current=node;
		
		marked=isMarked(node);
		write=true;
		node.getName().accept(this);
		write=false;
		
		read=true;
		if(node.getSuperclassType()!=null)
			node.getSuperclassType().accept(this);
		if(node.superInterfaceTypes()!=null){
			List<Type> l=node.superInterfaceTypes();
			for(Type t : l)
				t.accept(this);
		}
		read=false;
		marked=false;
		
		if(current==node)	current=null;
		
		List<BodyDeclaration> l=node.bodyDeclarations();
		for(BodyDeclaration b: l)
			b.accept(this);
		
		
		
		return false;
	}
	
	@Override public boolean visit(MethodDeclaration node){
		if(current==null)	current=node; 
		current_method.push(getKey(node.getName()));
		marked=isMarked(node);
		
		
		is_decl=true;
		write=true;
		node.getName().accept(this);
		
		marked=false;
		
		current=null;
		
		is_param_decl=true;
		List<SingleVariableDeclaration> l=node.parameters();
		for(SingleVariableDeclaration s : l)
			s.accept(this);
		is_param_decl=false;
		
		write=false;
		is_decl=false;
		
		
		
		node.getBody().accept(this);
		
		/**
		 * Here we are after method's visit. Now it's time to solve the method's dependencies
		 */
		MethodDep md=new MethodDep(local_vars,function_returns,params,ext_write,ext_as_param,current_method.lastElement());
		methods.put(current_method.lastElement(),md);
		md.rd.resolve_ret_deps();
		md.pd.resolve_param_deps();
		md.ed.resolve_ext_deps();
		local_vars.clear(); params.clear(); function_returns.clear();
		ext_write.clear();	ext_as_param.clear();
		
		current_method.pop();
		
		if(current==node)	current=null;
		
		return false;
	}
	
	@Override public boolean visit(EnumDeclaration node){
		if(current==null)	current=node;
		marked=isMarked(node);
		
		write=true;
		
		node.getName().accept(this);
		List<EnumConstantDeclaration> l=node.enumConstants();
		for(EnumConstantDeclaration e : l)
			e.accept(this);
		
		write=false;
		marked=false;
		
		if(current==node)	current=null;
		
		return false;
	}
	
	
	/***************************************************************/
	/***Nodes that could be marked as current (so, "main" nodes)***/
	/*************************************************************/
	
	@Override public boolean visit(SingleVariableDeclaration node){
		current=node;
		marked=isMarked(node);
		
		node.getName().accept(this);
		
		if(node.getInitializer()!=null)
			node.getInitializer().accept(this);
		marked=false;
		current=null;
		return false;
	}
	
	@Override public boolean visit(FieldDeclaration node){
		
		marked=isMarked(node);
		List<VariableDeclarationFragment> l=node.fragments();
		for(VariableDeclarationFragment vds : l){
			vds.accept(this);
		}
		
		marked=false;
		
		return false;
	}
	
	
	@Override public boolean visit(VariableDeclarationStatement node){
		
		marked=isMarked(node);
		List<VariableDeclarationFragment> l=node.fragments();
		for(VariableDeclarationFragment vds : l)
			vds.accept(this);
		
		marked=false;
		
		return false;
	}
	
	
	@Override public boolean visit(ExpressionStatement node){
		if(current==null)	current=node;
		marked=isMarked(node);
		node.getExpression().accept(this);
		marked=false;
		if(current==node)	current=null;
		return false;
	}
	
	
	
	
	/**
	 * if ( Expression ) Statement [ else Statement]
	 */
	@Override public boolean visit(IfStatement node){
		if(current==null)	current=node;
		marked=isMarked(node);
		
		read=true;
		node.getExpression().accept(this);;
		read=false;
		marked=false;
		if(current==node)	current=null;
		
		node.getThenStatement().accept(this);
		if(node.getElseStatement()!=null)	node.getElseStatement().accept(this);
		
		return false;
	}
	

	/**
	 * AssertStatement:
     * assert Expression [ : Expression ] ;
	 */
	@Override public boolean visit(AssertStatement node){
		if(current==null)	current=node;
		marked=isMarked(node);
		read=true;
		node.getExpression().accept(this);
		read=false;
		read=true;
		if(node.getMessage()!=null)
			node.getMessage().accept(this);
		read=false;
		
		marked=false;
		
		if(current==node)	current=null;
		
		return false;
	}
	
	/**
	 * DoStatement:
     * 	do Statement while ( Expression ) ;
	 */
	@Override public boolean visit(DoStatement node){
		if(current==null)	current=node;
		marked=isMarked(node);
		
		
		read=true;
		node.getExpression().accept(this);
		read=false;
		marked=false;
		
		if(current==node)	current=null;
		
		node.getBody().accept(this);
		
		return false;
	}
	
	/**
	 * EnhancedForStatement:
     * 	for ( FormalParameter : Expression )
     *  	Statement
	 */
	@Override public boolean visit(EnhancedForStatement node){
		current=node;
		marked=isMarked(node);
		
		write=true;
		node.getParameter().accept(this);
		write=false;
		read=true;
		current=node;
		node.getExpression().accept(this);
		read=false;
		marked=false;
		
		current=null;
		
		node.getBody().accept(this);
		
		return false;
	}
	
	
	/**
	 * ForStatement:
     * 	for (
     *   	[ ForInit ];
     *      [ Expression ] ;
     *      [ ForUpdate ] )
     *			Statement
     *	ForInit:
     *		Expression { , Expression }
 	 *	ForUpdate:
     *		Expression { , Expression }
	 */
	@Override public boolean visit(ForStatement node){
		//current=node;
		marked=isMarked(node);
		
		List<Expression> l=node.initializers();
		for(Expression e : l){
			current=node;
			e.accept(this);
		}
		
		if(node.getExpression()!=null){
			current=node;
			read=true;
			node.getExpression().accept(this);
			read=false;
			current=null;
		}
		
		
		write=true;
		l=node.updaters();
		for(Expression e : l){
			//current=node;
			e.accept(this);
		}
		write=false;
		marked=false;
		
		//current=null;
		
		node.getBody().accept(this);
		
		return false;
	}
	
	@Override public boolean visit(CatchClause node){
		
		write=true;
		node.getException().accept(this);
		write=false;
		node.getBody().accept(this);
		
		return false;
		
	}
	
	/**
	 *  SwitchStatement:
	 *  	switch ( Expression )
     *       	{ { SwitchCase | Statement } } }
	 */
	@Override public boolean visit(SwitchStatement node){
		if(current==null)	current=node;
		marked=isMarked(node);
		
		read=true;
		node.getExpression().accept(this);
		read=false;
		marked=false;
		
		if(current==node)	current=null;
		
		List<Statement> sl = node.statements();
		for( Statement s : sl)
			s.accept(this);
		
		return false;
	}
	/**
	 *  WhileStatement:
     *  	while ( Expression ) Statement
	 */
	@Override public boolean visit(WhileStatement node){
		if(current==null)	current=node;
		marked=isMarked(node);
		
		read=true;
		node.getExpression().accept(this);
		read=false;
		marked=false;
		
		if(current==node)	current=null;
		
		node.getBody().accept(this);
		
		return false;
	}
	
	/**
	 * SynchronizedStatement:
     * 	synchronized ( Expression ) Block
	 */
	@Override public boolean visit(SynchronizedStatement node){
		if(current==null)	current=node;
		marked=isMarked(node);
		
		write=true;
		node.getExpression().accept(this);
		write=false;
		marked=false;
		
		if(current==node)	current=null;
		
		node.getBody().accept(this);
		
		return false;
	}
	
	@Override public boolean visit(ReturnStatement node){
		current=node;
		marked=isMarked(node);
		
		read=true;
		if(node.getExpression()!=null)
			node.getExpression().accept(this);
		read=false;
		marked=false;
		
		function_returns.add(node);
		
		current=null;
		
		return false;
	}
	
	/**
	 * VariableDeclarationFragment:
     * 	Identifier { [] } [ = Expression ]
	 */
	@Override public boolean visit(VariableDeclarationFragment node){
		current=node;
		//this check is to prevent successive fragment of being keeped
		marked=isMarked(node);
		write=true;
		is_decl=true;
		node.getName().accept(this);
		is_decl=false;
		write=false;
		if(node.getInitializer()!=null){
			read=true;
			node.getInitializer().accept(this);
			read=false;
		}
		current=null;
		marked=false;
		
		return false;
	}
	
	/**
	 *	SwitchCase:
     *      case Expression  :
     *      default :
	 */
	@Override public boolean visit(SwitchCase node){
		if(current==null)	current=node;
		marked=isMarked(node);
		if(node.getExpression()!=null){
			read=true;
			node.getExpression().accept(this);
			read=false;
		}
		marked=false;
		if(current==node)	current=null;
		return false;
	}
	
	@Override public boolean visit(FieldAccess node){
		
		deepness++;
		
		String key="",type = null;
		int kind=node.getName().resolveBinding().getKind();
		boolean is_static=is_static(node.getName());
		
		String father="";
		
		if(is_static)	complex_call="";
		
		boolean is_object=false;
		if(node.resolveTypeBinding()!=null){
			is_object=node.resolveTypeBinding().isClass();
			type=node.getName().resolveTypeBinding().getKey();
		}
		
		boolean is_of_known_type=false;
		if(is_object)
			is_of_known_type=userDefinedTypesAndMethods.contains(node.resolveTypeBinding().getKey());
		
		boolean is_type=kind==IBinding.TYPE;
		boolean _is_call=true;
		
		is_field=true;
		/**
		 * Case of field. . . someMethod()
		 */
		if(invokedMethod.size()>0){
			accessFieldCounter.push(accessFieldCounter.pop()+1);
			node.getExpression().accept(this);
			father=complex_call;
			
			/**
			 * Case of field.someMethod()
			 */
			if(accessFieldCounter.lastElement()==1){
				if(is_static(invokedMethod.lastElement())){
					complex_call="";
					is_static=true;
				}
				
				if(!complex_call.equals(""))
					key=complex_call+" ";
				key+=node.resolveFieldBinding().getKey();
				complex_call=key;
				
				
			}
			/**
			 * Case of field..someField.someMethod()
			 */
			else{
				_is_call=false;
				if(!complex_call.equals(""))
					complex_call+=" ";
				complex_call+=node.resolveFieldBinding().getKey();
				key=complex_call;
				accessFieldCounter.push(accessFieldCounter.pop()-1);
			}
		}
		/**
		 * Case of field access statement [READ/WRITE]
		 */
		else{
			accessFieldCounter.push(0);
			node.getExpression().accept(this);
			father=complex_call;
			
			_is_call=false;
			if(!complex_call.equals(""))
				complex_call+=" ";
			complex_call+=node.resolveFieldBinding().getKey();
			key=complex_call;
			
			// keep the reference because later we will solve aliasing from get function contained in the expression
			Pair<String,Integer> param=null;
			if(param_index.size()>0 && !more_params)
				param=new Pair<String,Integer>(param_call.lastElement(),param_index.lastElement());
			
			with_alias.add(new FieldRef(key,write,current,param));
			
			if(accessFieldCounter.size()==1)
				complex_call="";
			accessFieldCounter.pop();
			
			marked=isMarked(node);
			
		}
		
		is_field=false;
		
		
		boolean is_last=deepness==1;
		if(is_last){
			if(marked)
				V.add(key);
			
			add_stmt_to_var(key,type,is_object,is_static,_is_call,is_type,is_of_known_type);	
			
			add_var_to_stmt(key);
			
			add_var_to_method_and_viceversa(key,is_static,is_type);
		}
		
		if(!father.equals("")){
			RefID obj=ids_table.get(father);
			add_ref(obj,key,is_object,is_last,_is_call,is_static);
		}
		
		
		deepness--;
		
		return marked=false;
	}
	
	static Stack<String> param_call=new Stack<String>();
	
	@Override public boolean visit(MethodInvocation node){
		
		deepness++;
		
		
		if(current==null)	current=node;
		ASTNode backup_current=current;
		
		marked=isMarked(node);
		
		is_call=true;
		
		current_call.push(node);
		invokedMethod.push(node.getName());
		accessFieldCounter.push(0);
		if(node.getExpression()!=null){
			node.getExpression().accept(this);
		}
		
		List<Expression> l = node.arguments();
		
		String last_call_backup=last_call;
		n_params=l.size();
		node.getName().accept(this);
		n_params=0;
		
		is_call=false;
		
		//invokedMethod.pop();
		accessFieldCounter.pop();
		
		param_call.push(complex_call);
		if(invokedMethod.size()==0 && !is_field)
			complex_call="";
		
		
		deepness--;
		
		String last_call_=last_call;
		
		
		
		param_index.add(0);
		boolean backup_param=is_param;
		//current_params="";
		
		for(Expression e : l){
			is_param=true;
			
			current=e;
			//if(e.resolveTypeBinding().isClass())
			//	write=true;
			countNames cn=new countNames();
			e.accept(cn);
			
			more_params=cn.counter>1;
			e.accept(this);
			more_params=false;
			param_index.push(param_index.pop()+1);
			
			last_call=last_call_;
			//write=false;
			
		}
		current=backup_current;
		
		n_params=0;
		param_index.pop();
		param_call.pop();
		
		is_param=backup_param;
		
		last_call=last_call_backup;
		
		
		//read=false;
		//current_params="";
		
		marked=false;
		
		
		if(current==node)	current=null;
		
		current_call.pop();
		
		
		
		return false;
	}
	
	/**************************************************/
	/***Nodes that could *NOT* be marked as current***/
	/************************************************/
	
	
	/**
	 * Assignment:
     * 	Expression AssignmentOperator Expression
	 */
	@Override public boolean visit(Assignment node){
		current=node;
		write=true;
		node.getLeftHandSide().accept(this);
		write=false; 
		read=true;
		node.getRightHandSide().accept(this);
		read=false;
		current=null;
		return false;
	}
	
	
	
	/**
	 * PostfixExpression:
     * 	Expression PostfixOperator
	 */
	@Override public boolean visit(PostfixExpression node){
		if(current!=null)
			node.getOperand().accept(this);
		ASTNode backup=current;
		boolean backup_param=is_param;
		is_param=false;
		current=node;
		if(node.getOperator().equals(PostfixExpression.Operator.INCREMENT) || node.getOperator().equals(PostfixExpression.Operator.DECREMENT))
			write=true;
		node.getOperand().accept(this);
		if(node.getOperator().equals(PostfixExpression.Operator.INCREMENT) || node.getOperator().equals(PostfixExpression.Operator.DECREMENT))
			write=false;
		current=backup;
		is_param=backup_param;
		return false;
	}
	
	/**
	 * PrefixExpression:
     * 	PrefixOperator Expression
	 */
	@Override public boolean visit(PrefixExpression node){
		if(current!=null)
			node.getOperand().accept(this);
		ASTNode backup=current;
		boolean backup_param=is_param;
		is_param=false;
		current=node;
		if(node.getOperator().equals(PrefixExpression.Operator.INCREMENT) || node.getOperator().equals(PrefixExpression.Operator.DECREMENT))
			write=true;
		node.getOperand().accept(this);
		if(node.getOperator().equals(PrefixExpression.Operator.INCREMENT) || node.getOperator().equals(PrefixExpression.Operator.DECREMENT))
			write=false;
		current=backup;
		is_param=backup_param;
		return false;
	}
	
	/**
	 * ConditionalExpression:
     * 	Expression ? Expression : Expression
	 */
	@Override public boolean visit(ConditionalExpression node){
		read=true;
		node.getExpression().accept(this);
		read=false;
		read=true;
		node.getThenExpression().accept(this);
		node.getElseExpression().accept(this);
		read=false;
		
		return false;
	}
	
	
	
	/**
	 * InstanceofExpression:
     * 	Expression instanceof Type
	 */
	@Override public boolean visit(InstanceofExpression node){
		
		read=true;
		node.getLeftOperand().accept(this);
		node.getRightOperand().accept(this);
		read=false;
		
		return false;
	}
	
	/**
	 * ClassInstanceCreation:
     * 	[ Expression . ] new Name
     *  	( [ Expression { , Expression } ] )
     *      [ AnonymousClassDeclaration ]
	 */
	@Override public boolean visit(ClassInstanceCreation node){
		
		if(current==null)	current=node;
		ASTNode backup_current=current;
		
		marked=isMarked(node);
		
		is_call=true;
		c_sig = node.resolveConstructorBinding().getMethodDeclaration().getKey();
		
		current_call.push(node);
		
		
		List<Expression> l = node.arguments();
		
		String last_call_backup=last_call;
		n_params=l.size();
		//is_constructor=true;
//		node.getType().accept(this);
		String key=c_sig;
		key=update_call_key(key);
		key=complex_call;
		
		
		//last param may be wrong
		add_stmt_to_var(key,node.resolveTypeBinding().getKey(),false,false,true,false,true);
		
		add_var_to_stmt(key);
		
		add_var_to_method_and_viceversa(key,false,false);
		
		last_call=key;
		
		//is_constructor=false;
		n_params=0;
		
		is_call=false;
		
		//invokedMethod.pop();
		//accessFieldCounter.pop();
		
		param_call.push(complex_call);
		if(invokedMethod.size()==0 && !is_field)
			complex_call="";
		
		String last_call_=last_call;
		
		
		
		param_index.add(0);
		boolean backup_param=is_param;
		//current_params="";
		
		for(Expression e : l){
			is_param=true;
			
			current=e;
			//if(e.resolveTypeBinding().isClass())
			//	write=true;
			countNames cn=new countNames();
			e.accept(cn);
			
			more_params=cn.counter>1;
			e.accept(this);
			more_params=false;
			param_index.push(param_index.pop()+1);
			
			last_call=last_call_;
			//write=false;
			
		}
		current=backup_current;
		
		n_params=0;
		param_index.pop();
		param_call.pop();
		
		is_param=backup_param;
		
		last_call=last_call_backup;
		
		marked=false;
		
		if(current==node)	current=null;
		
		current_call.pop();
		
		
		return false;
		
	}
	
	@Override public boolean visit(SimpleType node){
		return userDefinedTypesAndMethods.contains(node.getName().resolveBinding().getKey());//return node.resolveBinding().isFromSource();
	}
	
	
	/*****************************************************/
	/*Below the part that handle names (identifier) *****/
	/***************************************************/
	
	@Override public boolean visit(QualifiedName node){
		/**
		 * First check if this qualified name point to something that is static. In that case we need no hierarchy.
		 * Static vars in java could be accessed in lots of ways but in the end they point to the same memory location
		 */
		if((node.resolveBinding().getModifiers()&0x8)!=0 ||
				node.resolveBinding().getKind()==IBinding.TYPE || node.resolveBinding().getKind()==IBinding.PACKAGE){
			node.getName().accept(this);
		}
		else{
			marked=isMarked(node);
			/**
			 * Compute hierarchy of names
			 */
			Stack<Name> hierarchy=getHierarchy(node);
			Name curr=hierarchy.pop();
			String key=getKey(curr);
			
			boolean is_static=(node.resolveBinding().getModifiers()&0x8)!=0;
			
			/**
			 * Prepare msg for debug
			 */
			String msg="";
			if(read && !is_param) msg+="R "; if(write) msg+="W ";
			msg+=": ";
			
			/**
			 * If the main object at the top of the hierarchy doesn't exist in table, put it
			 */
			RefID obj=ids_table.get(key);
			if(obj==null){
				obj=new RefID();
				ids_table.put(key, obj);
				obj.is_object=true;
				obj.key=key;
			}
			key+=" ";
			/**
			 * Until there are names, add them to the ids_table, binding them with the father
			 */
			while(!hierarchy.isEmpty()){
				curr=hierarchy.pop();
				key+=getKey(curr);
				add_ref(obj,key,curr.resolveTypeBinding().isClass(),(hierarchy.isEmpty())&(invokedMethod.size()==0),false, is_static);
				key+=" ";
			}
			/**
			 * If this qualified name gives a method in the end, add it
			 */
			if(invokedMethod.size()>0){
				key+=getKey(invokedMethod.lastElement());
				
				update_call_key(key);
				key=complex_call;
				//update_call_stack(key);
				
				add_ref(obj,key,invokedMethod.lastElement().resolveTypeBinding().isClass(),true,true,is_static);
			}
			
			System.out.println(msg+key+"\n");
			
			marked=false;
		}
		
		
		return false;
	}
	
	public static String keyFromHierarchy(Stack<Name> hierarchy){
		String key="";
		while(!hierarchy.isEmpty())
			key+=getKey(hierarchy.pop())+" ";
		key=key.substring(0, key.length()-1);
		return key;
	}
	
	/**
	 * Given a qualified name, compute the hierarchy as a stack
	 * @param node
	 * @return
	 */
	public static Stack<Name> getHierarchy(QualifiedName node){
		Stack<Name> hierarchy=new Stack<Name>();
		{
			QualifiedName name=node;
			while(name!=null){
				hierarchy.add(name.getName());
				if(name.getQualifier() instanceof QualifiedName)
					name=(QualifiedName) name.getQualifier();
				else{
					hierarchy.add(name.getQualifier());
					name=null;
				}
			}
		}
		return hierarchy;
	}
	
	
	private static void bind_call(String key){
		key=update_call_stack(key);
		//key=key.split(" ")[key.split(" ").length-2];
		HashSet<String> calls=methods_to_calls.get((key));
		if(calls==null){
			calls=new HashSet<String>();
			methods_to_calls.put(key, calls);
		}
		calls.add(key);
		
		if(!current_method.isEmpty()){
			HashSet<String> nested_calls=method_nested_calls.get(current_method.lastElement());
			if(nested_calls==null){
				nested_calls=new HashSet<String>();
				method_nested_calls.put(current_method.lastElement(), nested_calls);
			}
			nested_calls.add(key);
		}
	}
	/**
	 * Bind an object to an attribute or a method
	 * @param obj the father object
	 * @param key
	 * @param name
	 * @param is_last
	 * @param is_invocation
	 */
	public static void add_ref(RefID obj, String key, boolean is_object, boolean is_last, boolean is_invocation, boolean is_static){
		
		RefID inside_obj=ids_table.get(key);
		if(!is_invocation){
			if(inside_obj==null){
				inside_obj=new RefID();
				inside_obj.is_object=is_object;
				inside_obj.father=obj;
				inside_obj.key=key;
				ids_table.put(key, inside_obj);
			}
			if(obj.fields==null)
				obj.fields=new ArrayList<RefID>();
			obj.fields.add(inside_obj);
		}
		else{
			if(inside_obj==null){
				inside_obj=new RefID();
				inside_obj.is_object=is_object;
				inside_obj.father=obj;
				inside_obj.is_call=true;
				
				String call_name=key.split(" ")[key.split(" ").length-2];
				inside_obj.call_name=call_name;
				inside_obj.invocation=current_call.lastElement();
				inside_obj.method_declaration=methods_declarations.get(call_name);
				inside_obj.key=key;
				ids_table.put(inside_obj.key, inside_obj);
				
				call_table.put(current,key);
			}
			if(obj.invocations==null)
				obj.invocations=new ArrayList<RefID>();
			obj.invocations.add(inside_obj);
			
			
			
			calls_from_objects.add(key);
		}
		
		if(is_last){
			inside_obj.is_static=is_static;
			
			if(!is_invocation)
				add_var_to_method_and_viceversa(key,false,false);
			add_var_to_stmt(key);
			if(is_param && (inside_obj.is_object || is_call) && (!more_params || is_call)){
				//current_params+=key+" ";
				
				if(inside_obj.param==null)
					inside_obj.param=new ArrayList<Pair<String,Integer>>();
				inside_obj.param.add(new Pair<String,Integer>(param_call.lastElement(),param_index.lastElement()));
				//if(inside_obj.calls==null)
				//	inside_obj.calls=new ArrayList<ASTNode>();
				//inside_obj.calls.add(param_call.lastElement());
			}
			else {
				if(write){
					if(inside_obj.write==null)
						inside_obj.write=new ArrayList<ASTNode>();
					inside_obj.write.add(current);
				}
				else if(read && !is_param){
					if(inside_obj.read==null)
						inside_obj.read=new ArrayList<ASTNode>();
					inside_obj.read.add(current);
				}
			}
		}
		
		if(is_last && marked && !is_invocation)
			V.add(key);
		
		obj=inside_obj;
	}
	
	private static String update_call_stack(String key){
		if(!complex_call.equals(""))
			complex_call+=" ";
		complex_call+=key;
		key=complex_call;
		return key;
	}
	
	private static boolean is_static(Name node){
		return (node.resolveBinding().getModifiers()&0x8)!=0;
	}
	
	/**
	 * Visit a simple name track all the needed influences
	 */
	@Override public boolean visit(SimpleName node){
		
		marked=isMarked(node);
		
		String key=getKey(node);
		
		String type=null;
		
		int kind=node.resolveBinding().getKind();
		boolean is_static=is_static(node);
		
		boolean is_object=false;
		if(node.resolveTypeBinding()!=null){
			is_object=node.resolveTypeBinding().isClass();
			type=node.resolveTypeBinding().getKey();
		}
		
		boolean is_of_known_type=false;
		if(is_object)
			is_of_known_type=userDefinedTypesAndMethods.contains(node.resolveTypeBinding().getKey());
		
		boolean is_type=kind==IBinding.TYPE;
		
		/**
		 * Special case if we have an invocation with an expression of the type simple name
		 * It could be SomeClass.someMethod(); So the method is static and we don't need to consider SomeClass
		 * Or it could be someObject.someMethod(); In this case we need to bind the object with the method
		 */
		if(invokedMethod.size()>0){
			
			
			/*if(is_static(invokedMethod.lastElement()) && node!=invokedMethod.lastElement()){
				visit(invokedMethod.lastElement());
			}
			else*/ if(kind==IBinding.METHOD){
				
				is_static=is_static(invokedMethod.lastElement());
				is_of_known_type=userDefinedTypesAndMethods.contains(getKey(invokedMethod.lastElement()));
				
				RefID obj=null;
				
				if(!complex_call.equals("")){
					obj=ids_table.get(complex_call);
					if(obj==null){
						obj=new RefID();
						ids_table.put(complex_call, obj);
						obj.is_object=true;
						obj.key=key;
					}
					
				}
				
				
				key=getKey(invokedMethod.lastElement());
				//key+=" "+current_params;
				key=update_call_key(key);
				key=complex_call;
				//key=update_call_stack(key);
				
				invokedMethod.pop();
				
				
				boolean last=deepness==1;
				if(last){
					add_stmt_to_var(key,type,is_object,is_static,true,is_type,is_of_known_type);
					
					add_var_to_stmt(key);
					
					add_var_to_method_and_viceversa(key,is_static,is_type);
					
					Pair<String, Integer> param=null;
					if(param_index.size()>0 && !more_params)
						param=new Pair<String,Integer>(param_call.lastElement(),param_index.lastElement());
					
					with_alias.add(new FieldRef(key,write,current,param));
				}
				
				if(obj!=null)	
					add_ref(obj,key,false,last,true,is_static);
				
				
				last_call=key;
			}
			else if(kind!=IBinding.PACKAGE && !is_type){
				/*RefID obj=ids_table.get(key);
				
				if(obj==null){
					obj=new RefID();
					ids_table.put(key, obj);
					obj.is_object=true;
					obj.key=key;
				}*/
				
				/*int i=invokedMethod.size()-1;
				for(;i>=0;i--){
					key+=" "+getKey(invokedMethod.get(i));
					key=update_call_key(key);
					key=update_call_stack(key);
				}
				*/
				
				key=update_call_stack(key);
				
				//key+=" "+current_params;
				//add_ref(obj,key,node.resolveTypeBinding().isClass(),false,false,is_static);
				
				add_var_to_stmt(key);
				
				calls_from_objects.add(key);
			}
			
			if(marked)
				V.add(key);
			
			return marked=false;
		}
		
		if(marked)
			V.add(key);
		
		add_stmt_to_var(key,type,is_object,is_static,is_call,is_type,is_of_known_type);
		
		add_var_to_stmt(key);
		
		add_var_to_method_and_viceversa(key,is_static,is_type);
		
		
		return marked=false;
	}
	
	
	/**
	 * Associate a specific variable to a statement in one of the access modes
	 * @param key the key that identifies the var
	 */
	private static void add_var_to_stmt(String key){
		if(!is_param){
			System.out.println(current);
			RefStmt rv=stmt_table.get(current);
			if(rv==null){
				rv=new RefStmt();
				stmt_table.put(current, rv);
			}
			if(write){
				if(rv.write==null)
					rv.write=new ArrayList<String>();
				rv.write.add(key);
				System.out.println("W: "+key);			
			}
			else if(read){
				if(rv.read==null)
					rv.read=new ArrayList<String>();
				rv.read.add(key);
				System.out.println("R: "+key);
			}
			System.out.println("\n");
		}
		//else
		//	current_params+=key+" ";
	}
	
	private static String updateCallIndex(String key){
		String call_name=key.split(" ")[key.split(" ").length-1];
		Integer counter=call_counter.get(call_name);
		if(counter==null)
			counter=0;
		int newCounter=counter+1;
		call_counter.put(call_name, newCounter);
		key+=" "+counter;
		return key;
	}
	
	private String update_call_key(String key){
		key=updateCallIndex(key);
		
		bind_call(key);
		
		return key;
	}
	
	/**
	 * Bind the current statement to the var passed as parameter with a specific access mode
	 * @param key the identifier of the var
	 */
	private void add_stmt_to_var(String key, String type, boolean is_object, boolean is_static, boolean is_call, boolean is_type, boolean is_of_known_type){
		
		
		RefID rs=ids_table.get(key);
		if(rs==null){
			rs=new RefID();
			rs.is_object=is_object;
			rs.is_static=is_static;
			rs.is_call=is_call;
			rs.type=type;
			
			if(is_call){
				rs.call_name=key.split(" ")[key.split(" ").length-2];
				rs.method_declaration=methods_declarations.get(rs.call_name);
				rs.invocation=current_call.lastElement();
				
				rs.params_inline=new ArrayList[n_params];
				
				call_table.put(current,key);
			}
			rs.key=key;
			rs.is_type=is_type;
			rs.is_of_known_type=is_of_known_type;
			ids_table.put(key, rs);
		}
		
		if(is_param_decl){
			if(rs.def==null)
				rs.def=new ArrayList<ASTNode>();
			rs.def.add(current);
		}
		else{
			if(is_param && ((is_object && !more_params) /*|| is_call*/)){
				if(rs.param==null)
					rs.param=new ArrayList<Pair<String,Integer>>();
				rs.param.add(new Pair<String,Integer>(param_call.lastElement(),param_index.lastElement()));
//				if(rs.calls==null)
//					rs.calls=new ArrayList<ASTNode>();
//				rs.calls.add(param_call.lastElement());
			}
			else {
				
				//add the statement in the appropriate list
				if(write){
					if(rs.write==null)
						rs.write=new ArrayList<ASTNode>();
					rs.write.add(current);
				}
				else if(read  && !is_param){
					if(rs.read==null)
						rs.read=new ArrayList<ASTNode>();
					rs.read.add(current);
				}
			}
		}
	}
	
	
	
	/**
	 * Add information, for each method we'll have the "global" vars modified by it.
	 * A var could be STATIC or NON STATIC.
	 * For each var we'll have which methods modify it.
	 * Also we'll have a map for each method which other methods calls.
	 * 
	 * @param key
	 */
	private static void add_var_to_method_and_viceversa(String key, boolean is_static, boolean is_type){
		if(!current_method.isEmpty()){
			//is a param
			if(is_param_decl){
				params.add(key);
			}
			//is a local var
			if(is_decl){
				local_vars.add(key);
			}
			//is an external variable
			if(!is_decl && !local_vars.contains(key) && (write || is_param) && !is_call && !is_type){
				if(write){
					ArrayList<ASTNode> refs=ext_write.get(key);
					if(refs==null)
						refs=new ArrayList<ASTNode>();
					refs.add(current);
					ext_write.put(key,refs);
				}
				else if(is_param){
					ArrayList<Pair<String,Integer>> refs=ext_as_param.get(key);
					if(refs==null)
						refs=new ArrayList<Pair<String,Integer>>();
					refs.add((new Pair<String,Integer>(param_call.lastElement(),param_index.lastElement())));
					
					ext_as_param.put(key,refs);
				}
			}
			//link parameters of the calls to the call @RefID
			if(is_param){
				ArrayList<String> p=(ids_table.get(last_call)).params_inline[param_index.lastElement()];
				if(p==null){
					p=new ArrayList<String>();
					(ids_table.get(last_call)).params_inline[param_index.lastElement()]=p;
				}
				p.add(key);
				
			}
		}
		
	}
}
