package com.slicerplugin.slicer.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;

import com.slicerplugin.generic.Constants;
import com.slicerplugin.generic.Pair;
import com.slicerplugin.slicer.dependencies.DependenciesAnalyzer;
import com.slicerplugin.slicer.dependencies.ExtDep;
import com.slicerplugin.slicer.dependencies.ParamDep;

/**
 * This class computes slices of java code using jdt.
 * Giving in input the path of the java project and 
 * informations needed to get the slicing criterion you can
 * get out fresh slices.
 * 
 * The algorithm to generate the slice is of more steps:
 * -Compute the ast for the modules using jdt
 * -Initialize the algorithm visiting asts and computing 
 * tables of reference between statement and identifiers
 * -Run the dependency algorithm to find all the dependencies
 * in the project based on the slicing criterion
 * -Run the Refine class on all the ast to add some useful
 * context's statement
 * -Run the pruner to prune the subtrees not in the slice
 * 
 * Done.
 * 
 * @author Tomas Bortoli
 *
 */
public class Slice {
	
	/**
	 * All the single java modules with the path
	 */
	ArrayList<Pair<String,CompilationUnit>> modulesAST=new ArrayList<Pair<String,CompilationUnit>>();
	
	/**
	 * The original sources [before slice]
	 */
	static ArrayList<String> sources=new ArrayList<String>();
	
	/**
	 * Marked offsets
	 */
	HashMap<String,ArrayList<Pair<Integer,Integer>>> project_offsets;
	
	/**
	 * The soon sliced project path
	 */
	String projectPath;
	
	/**
	 * Offsets to keep
	 */
	HashMap<String,ArrayList<Pair<Integer,Integer>>> toKeep;
	
	/**
	 * Offsets to mark
	 */
	HashMap<String,ArrayList<Pair<Integer,Integer>>> toMark;
	/**
	 * VALUE of a java project in eclipse that indicates the type
	 */
	private static final String JDT_NATURE = "org.eclipse.jdt.core.javanature";
	
	/**
	 * Initialize a slice, it's time to do that.
	 * -Keep needed infos
	 * -Create the ast and initialize the algorithm based on the data
	 * [Identify references to statements for each identifier who use it]
	 * [Identify for each statement which identifiers use it]
	 * [Creating graph relation between statement and variables [[Bidirectional relation]] ]
	 * 
	 * -Analyze dependencies with the GRAPH
	 * Check for statement that use identifier in the slicing criterion. Add them to the slice
	 * And recursiverly repeat the process until convergence
	 * 
	 * -Refine the slice by adding pure context statements not related to identifiers and 
	 * difficult to add before this moment [ continue; break; return; ]
	 * 
	 * 
	 * -Extract the inverse slice from the ast's unmarked nodes
	 * -reverse the slice + filter for clean background or not [OPTIONS]
	 * 
	 * 
	 * @param project_offsets
	 * @param projectPath
	 * @throws Exception
	 */
	public Slice(HashMap<String,ArrayList<Pair<Integer,Integer>>> project_offsets, String projectPath) throws Exception{
		this.project_offsets=project_offsets;
		this.projectPath=projectPath;
		
		InitSlicing.reset();
		
		sources.clear();
		toKeep=new HashMap<String,ArrayList<Pair<Integer,Integer>>>();
		toMark=new HashMap<String,ArrayList<Pair<Integer,Integer>>>();
		
		com.slicerplugin.generic.Timer t=new com.slicerplugin.generic.Timer(); t.start();
		//create ASTs and initialize the slicing algorithm
		updateAST();
		
		
		for(Pair<String,CompilationUnit> p : modulesAST){
			InitSlicing.initTypes(p.getSecond());
		}
		for(Pair<String,CompilationUnit> p : modulesAST){
			CompilationUnit cu = p.getSecond();
			InitSlicing visitor = new InitSlicing(cu,project_offsets.get(p.getFirst()));
	    	cu.accept(visitor); //synchronously run the init slicing upon the AST
		}
		InitSlicing.global_alias_solve();
		ExtDep.solve_context_and_nested_calls();
		//ParamDep._resolve_param_deps();
		
		t.stopAndPrint("Time to init: ");
		
		if(InitSlicing.V.size()==0)
			throw new Exception("Invalid slicing criterion. No identifier marked!!");
		t.start();
		//run the dependencies scan to **SLICE**
		DependenciesAnalyzer da=new DependenciesAnalyzer(InitSlicing.V);
		da.algorithm();
		
		boolean stop=false;
		while(!stop){
			Refine ref=new Refine();
			for(Pair<String,CompilationUnit> p : modulesAST){
				p.getSecond().accept(ref);
			}
			if(ref.V.size()>0 || ref.marked.size()>0){
				DependenciesAnalyzer da2=new DependenciesAnalyzer(ref.V);
				da2.add_marked_nodes(ref.marked);
				da2.algorithm();
			}
			else
				stop=true;
		}
		t.stopAndPrint("Time to compute slice: ");
		
		t.start();
		int z=0;
		for(Pair<String,CompilationUnit> p : modulesAST){
			String code=sources.get(z++),slice="";
			ExtractOffsets eo=new ExtractOffsets(code);
			p.getSecond().accept(eo);
			//slice=eo.getSlice();
			
			ArrayList<Pair<Integer,Integer>> goodOffsets=eo.getMarked();
			Collections.sort(goodOffsets);
			toMark.put(p.getFirst(), goodOffsets);
			//ArrayList<Pair<Integer,Integer>> goodOffsets=eo.getGoodOffsets();
			//toKeep.put(p.getFirst(),goodOffsets);
			//GetSelectedStmts gss=new GetSelectedStmts(goodOffsets,code);
			//p.getSecond().accept(gss);
			//toMark.put(p.getFirst(),gss.stmt_offset);
			
			//System.out.println(slice);
		}
		t.stopAndPrint("Time to extract slice: ");
		
		//toKeep,toMark
		
		
		
		/*Pruner p=new Pruner();
		for(CompilationUnit c : modulesAST){
			System.out.println(c+"\n\n");
			c.accept(p);
		}*/
		
	}
	
	public HashMap<String,ArrayList<Pair<Integer,Integer>>> getSliceClone(){
		return (HashMap<String,ArrayList<Pair<Integer,Integer>>>)toKeep.clone();
	}
	
	public HashMap<String,ArrayList<Pair<Integer,Integer>>> getMarkedClone(){
		return (HashMap<String,ArrayList<Pair<Integer,Integer>>>)toMark.clone();
	}
	
	/**
	 * Get a set of ast of the current working project.
	 * @throws Exception 
	 */
	public void updateAST() throws Exception{
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
	    IWorkspaceRoot root = workspace.getRoot();
	    
	    
	    // Get all projects in the workspace
	    IProject[] projects = root.getProjects();
	    // Loop over all projects
	    for (IProject project : projects) {
    	  //if the project is the one in which the user is currently working on..
    	  if(project.getFullPath().toString().substring(1).equals(projectPath))
    		  if (project.isNatureEnabled(JDT_NATURE)) {
    			  analysePackages(project);
    		  }
	    }
	}
	
	
	/**
	 * Go into each package of the project [to create ast]
	 * @param project
	 * @throws Exception
	 */
	private void analysePackages(IProject project) throws Exception {
		IPackageFragment[] packages = JavaCore.create(project).getPackageFragments();
	    // parse(JavaCore.create(project));
	    for (IPackageFragment mypackage : packages) {
	    	if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
	    		createAST(mypackage);
	    	}
	    }
	}
	
	/**
	 * Create an ast for each compilation unit in a package and visit each tree
	 *  to map statement->variables(read/written) and vice versa
	 *  */
	private void createAST(IPackageFragment mypackage)
			throws Exception {
  		for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
	    	// now create the AST for the ICompilationUnits
	    	CompilationUnit parse = parse(unit);
	    	haveProblems(parse);
	    	sources.add(unit.getSource());
	    	
	    	/*Add the parsed compilation unit to the local list for the project*/
	    	modulesAST.add(new Pair<String,CompilationUnit>(unit.getUnderlyingResource().getFullPath().toString(),parse));
	    	/******************/
	    	
	    	//for (MethodDeclaration method : visitor.getMethods()) {
	    	//	System.out.println("Method name: " + method.getName()
			//	+ " Return type: " + method.getReturnType2());
	    	//}
	    }
	}
	
	public void haveProblems(CompilationUnit parse) throws Exception{
		IProblem problems[] = parse.getProblems();
    	for(int i=0;i<problems.length;i++)
    		if(problems[i].isError()){
    			throw new Exception(Constants.compileProblemsMessage);
    		}
	}
	  
	/** * Reads a ICompilationUnit and creates the AST DOM for manipulating the * Java source file * * @param unit * @return 
	 * @throws JavaModelException */
	public static CompilationUnit parse(ICompilationUnit unit) throws JavaModelException {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit.getSource().toCharArray());
		parser.setSource(unit);
		
		/*IF YOU WANT TO USE THIS PIECE OF CODE OUTSIDE ECLIPSE, to have bindings resolved
		 * you must set parser.setProject e parser.setUnitName with project object and unit name*/
		
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}
	
	public static CompilationUnit light_parse(ICompilationUnit unit) throws JavaModelException {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit.getSource().toCharArray());
		parser.setSource(unit);
		return (CompilationUnit) parser.createAST(null); // parse
	}
}
