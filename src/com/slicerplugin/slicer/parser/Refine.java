package com.slicerplugin.slicer.parser;

import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.ThisExpression;

import com.slicerplugin.generic.Constants;

/**
 * This class refine the slice to add some of the context instructions that is good to add at 
 * this step of the algorithm. Interesting statement are continue, break, return and empty statements.
 * But not at this stage(in a next one) because after this step a dependencies analysis is run again.
 * Why now? For the "nondeterministic" nature of the algorithm is efficient to search at this
 * stage for this kind of context statement, because now all the dependencies are defined and
 * is possible to know all the subtrees included in the slice.
 * If this process would be done earlier, it would be very inefficient.
 * 
 * PRECONDITION:
 * 	Before making visits with Refine, the analysis of dependencies must be done at least one time.
 * 	Or you think you can predict context interesting statements just magically?
 * 
 * @author Tomas Bortoli
 *
 */
public class Refine extends ASTVisitor {
	
	/**
	 * The set V of identifier found in this refine search
	 */
	HashSet<String> V;
	
	HashSet<ASTNode> marked;
	
	/**
	 * Helper visitor to find SimpleNames inside ReturnStatements and IfStatements
	 */
	RefineHelper rh=new RefineHelper();
	
	public Refine(){
		V=new HashSet<String>();
		marked=new HashSet<ASTNode>();
	}
	
	/**
	 * Just register identifiers of new slice to prepare the further search
	 * @author Tomas Bortoli
	 *
	 */
	public class RefineHelper extends ASTVisitor{
		@Override public boolean visit(SimpleName node){
			V.add(node.resolveBinding().getKey());
			return true;
		}
		
		@Override public boolean visit(ThisExpression node){
			//???????'
			return true;
		}
	}
	
	
	/**
	 * Check if a node is son of a marked node
	 * @param node
	 * @return true->marked
	 */
	private boolean checkup(ASTNode node){
		if(node==null)
			return false;
		
		if(node.getNodeType()==ASTNode.BLOCK)
			if(checkup(node.getParent())){
				node.setProperty(Constants.inSlice, true);
				return true;
		}
		
		// Marked -> true
		if(node.getProperty(Constants.inSlice)!=null)
			return true;
		else
			return false;
	}
	
	/*private boolean checkup_till_method(ASTNode node){
		if(node.getNodeType()!=ASTNode.METHOD_DECLARATION){
			boolean ret=checkup_till_method(node.getParent());
			if(ret)
				marked.add(node);
				//node.setProperty(Constants.inSlice,true);
			return ret;
		}
		else
			return node.getProperty(Constants.inSlice)!=null;
	}*/
	
	@Override public boolean visit(SwitchCase node){
		if(/*node.isDefault() &&*/ checkup(node.getParent()) && node.getProperty(Constants.inSlice)==null){
			node.setProperty(Constants.inSlice, true);
			node.accept(rh);
			return true;
		}
		else
			return false;
	}
	/*@Override public boolean visit(MethodDeclaration node){
		if(node.getProperty(Constants.inSlice)==null && node.isConstructor()){
			if(checkup(node.getParent())){
				node.setProperty(Constants.inSlice, true);
				marked.add(node);
				//node.accept(rh);
			}
			return false;
		}
		return true;
		
	}*/
	
	/*@Override public boolean visit(ReturnStatement node){
		
		if(node.getProperty(Constants.inSlice)==null){
			if(checkup_till_method(node.getParent())){
				//node.setProperty(Constants.inSlice, true);
				//node.accept(rh);
				marked.add(node);
			}
		}
		
		return false;
	}*/
	
	@Override public boolean visit(BreakStatement node){
		if(checkup(node.getParent()))
			node.setProperty(Constants.inSlice, true);
		return false;
	}
	
	@Override public boolean visit(ContinueStatement node){
		if(checkup(node.getParent()))
			node.setProperty(Constants.inSlice, true);
		return false;
	}
	
	@Override public boolean visit(EmptyStatement node){
		if(checkup(node.getParent()))
			node.setProperty(Constants.inSlice, true);
		return false;
	}
	
	/**
	 * If is a little bit ugly. Check then and else of if with only one statement
	 */
	/*@Override public boolean visit(IfStatement node){
		if(node.getProperty(Constants.inSlice)!=null){
			if(node.getThenStatement().getNodeType()!=ASTNode.BLOCK){
				if(node.getThenStatement().getProperty(Constants.inSlice)==null){
					node.getThenStatement().setProperty(Constants.inSlice, true);
					node.getThenStatement().accept(rh);
				}
			}
			if(node.getElseStatement()!=null && node.getElseStatement().getNodeType()!=ASTNode.BLOCK){
				if(node.getElseStatement().getProperty(Constants.inSlice)==null){
					node.getElseStatement().setProperty(Constants.inSlice, true);
					node.getElseStatement().accept(rh);
				}
			}
		}
		return false;
	}*/
}
