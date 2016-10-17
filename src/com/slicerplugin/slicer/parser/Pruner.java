package com.slicerplugin.slicer.parser;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import com.slicerplugin.generic.Constants;

/**
 * Class that prunes the subtrees that are not of interest.
 * This class just visit the nodes in which we know 
 * there are full statements and delete them if they are
 * not marked to be keeped.
 * This trivial algorithm runs after everything is done
 * and just find out the slice based on the AST.
 * 
 * @author Tomas Bortoli
 *
 */
public class Pruner extends ASTVisitor {
	
	private boolean haveToPrune(ASTNode node){
		if(node.getProperty(Constants.inSlice)!=null)
			return false;
		else
			return true;
	}
	
	private boolean doWork(ASTNode node){
		if(haveToPrune(node)){
			node.delete();
			return false;
		}
		
		return true;
	}
	
	@Override public boolean visit(TypeDeclaration node){
		return doWork(node);
	}
	
	@Override public void endVisit(TypeDeclaration node){
		System.out.println(node);
	}
	
	@Override public boolean visit(MethodDeclaration node){
		return doWork(node);
	}
	
	@Override public boolean visit(EnumDeclaration node){
		return doWork(node);
	}
	
	@Override public boolean visit(FieldDeclaration node){
		return doWork(node);
	}
	
	
	@Override public boolean visit(VariableDeclarationStatement node){
		return doWork(node);
	}
	
	
	@Override public boolean visit(ExpressionStatement node){
		return doWork(node);
	}
	
	
	
	
	/**
	 * if ( Expression ) Statement [ else Statement]
	 */
	@Override public boolean visit(IfStatement node){
		return doWork(node);
	}
	

	/**
	 * AssertStatement:
     * assert Expression [ : Expression ] ;
	 */
	@Override public boolean visit(AssertStatement node){
		return doWork(node);
	}
	
	/**
	 * DoStatement:
     * 	do Statement while ( Expression ) ;
	 */
	@Override public boolean visit(DoStatement node){
		return doWork(node);
	}
	
	/**
	 * EnhancedForStatement:
     * 	for ( FormalParameter : Expression )
     *  	Statement
	 */
	@Override public boolean visit(EnhancedForStatement node){
		return doWork(node);
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
		return doWork(node);
	}
	
	
	/**
	 *  SwitchStatement:
	 *  	switch ( Expression )
     *       	{ { SwitchCase | Statement } } }
	 */
	@Override public boolean visit(SwitchStatement node){
		return doWork(node);
	}
	/**
	 *  WhileStatement:
     *  	while ( Expression ) Statement
	 */
	@Override public boolean visit(WhileStatement node){
		return doWork(node);
	}
	
	/**
	 * SynchronizedStatement:
     * 	synchronized ( Expression ) Block
	 */
	@Override public boolean visit(SynchronizedStatement node){
		return doWork(node);
	}
	
	@Override public boolean visit(ReturnStatement node){
		return doWork(node);
	}
	
	
	/**
	 * VariableDeclarationFragment:
     * 	Identifier { [] } [ = Expression ]
	 */
	@Override public boolean visit(VariableDeclarationFragment node){
		return doWork(node);
	}
	
	
	
	
	
}
