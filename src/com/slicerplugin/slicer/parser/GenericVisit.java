package com.slicerplugin.slicer.parser;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

/**
 * Generic visitor for AST trees. 
 * It provides a To Override method to make a custom visit
 * The function is repeated for seach node that could be a marked node
 * 
 * @author Tomas Bortoli
 *
 */
public class GenericVisit extends ASTVisitor{
	
	public GenericVisit(){
		
	}
	
	/**
	 * To override
	 * @param node
	 */
	protected boolean do_work(ASTNode node){
		return true;
	}
	
	
	@Override public boolean visit(TypeDeclaration node){
		return do_work(node);
	}
	
	
	@Override public boolean visit(MethodDeclaration node){
		return do_work(node);
	}
	
	@Override public boolean visit(EnumDeclaration node){
		return do_work(node);
	}
	
	@Override public boolean visit(SingleVariableDeclaration node){
		return do_work(node);
	}
	
	@Override public boolean visit(FieldDeclaration node){
		return do_work(node);
	}
	
	
	@Override public boolean visit(VariableDeclarationStatement node){
		return do_work(node);
	}
	
	
	@Override public boolean visit(ExpressionStatement node){
		return do_work(node);
	}
	
	
	@Override public boolean visit(IfStatement node){
		return do_work(node);
	}
	
	@Override public boolean visit(AssertStatement node){
		return do_work(node);
	}
	
	@Override public boolean visit(DoStatement node){
		return do_work(node);
	}
	
	@Override public boolean visit(EnhancedForStatement node){
		return do_work(node);
	}
	
	@Override public boolean visit(ForStatement node){
		return do_work(node);
	}
	
	@Override public boolean visit(SwitchStatement node){
		return do_work(node);
	}
	
	@Override public boolean visit(SwitchCase node){
		return do_work(node);
	}
	
	@Override public boolean visit(WhileStatement node){
		return do_work(node);
	}
	
	@Override public boolean visit(SynchronizedStatement node){
		return do_work(node);
	}
	
	@Override public boolean visit(ReturnStatement node){
		return do_work(node);
	}
	
	@Override public boolean visit(BreakStatement node){
		return do_work(node);
	}
	
	@Override public boolean visit(ContinueStatement node){
		return do_work(node);
	}
	
	@Override public boolean visit(EmptyStatement node){
		return do_work(node);
	}
	
	@Override public boolean visit(VariableDeclarationFragment node){
		return do_work(node);
	}
	
	
	//EXPRESSIONS
	/*@Override public boolean visit(ConditionalExpression node){
		return do_work(node);
	}*/
	@Override public boolean visit(PrefixExpression node){
		return do_work(node);
	}/*
	@Override public boolean visit(CastExpression node){
		return do_work(node);
	}*/
	@Override public boolean visit(PostfixExpression node){
		return do_work(node);
	}
	/*@Override public boolean visit(InfixExpression node){
		return do_work(node);
	}
	@Override public boolean visit(ParenthesizedExpression node){
		return do_work(node);
	}*/
	
}
