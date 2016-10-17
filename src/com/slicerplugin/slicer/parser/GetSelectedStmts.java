package com.slicerplugin.slicer.parser;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BreakStatement;
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
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.SwitchCase;

import com.slicerplugin.generic.Pair;

public class GetSelectedStmts extends GenericVisit{
	
	public ArrayList<Pair<Integer,Integer>> stmt_offset;
	
	private ArrayList<Pair<Integer,Integer>> selectioned;
	
	private String code;
	
	public GetSelectedStmts(ArrayList<Pair<Integer,Integer>> selectioned, String code){
		this.selectioned=selectioned;
		this.stmt_offset=new ArrayList<Pair<Integer,Integer>>();
		this.code=code;
	}
	
	@Override public boolean do_work(ASTNode node){
		boolean ret=true;
		if(node.getNodeType()==ASTNode.VARIABLE_DECLARATION_STATEMENT ||
				node.getNodeType()==ASTNode.FIELD_DECLARATION)
			ret=false;
		
		Pair<Integer,Integer> se=GetSelectedStmt.getOffsets(node, code);
		for(Pair<Integer,Integer> selection : selectioned){
			if(se.getFirst()>= selection.getFirst() && se.getSecond()<=selection.getSecond() ||
					(se.getFirst()>selection.getFirst() && se.getSecond()>selection.getSecond() && se.getFirst()<selection.getSecond())){
				
				stmt_offset.add(new Pair<Integer,Integer>(se.getFirst(),se.getSecond()));//Math.min(se.getSecond(),selection.getSecond())));
				ret=true;
			}
		}
		return ret;
	}
	
	@Override public boolean visit(FieldDeclaration node){
		if(!do_work(node))
			return true;
		else
			return false;
	}
	
	@Override public boolean visit(VariableDeclarationStatement node){
		if(!do_work(node))
			return true;
		else
			return false;
	}
	
	@Override public boolean visit(MethodInvocation node){
		do_work(node);
		return true;
	}
	
	@Override public boolean visit(SimpleName node){
		do_work(node);
		return false;
	}
	
	@Override public boolean visit(Assignment node){
		do_work(node);
		return true;
	}
}
