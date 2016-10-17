package com.slicerplugin.slicer.parser;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.MethodInvocation;

import com.slicerplugin.generic.Pair;

public class GetSelectedStmt extends GenericVisit{
	
	public Pair<Integer,Integer> stmt_offset;
	
	private Pair<Integer,Integer> selection;
	
	private String code;
	
	public GetSelectedStmt(Pair<Integer,Integer> selection, String code){
		this.selection=selection;
		this.stmt_offset=new Pair<Integer,Integer>(null,null);
		this.code=code;
	}
	
	@Override public boolean do_work(ASTNode node){
		
		if(node.getStartPosition()<= selection.getFirst() && node.getStartPosition()+node.getLength()>=selection.getSecond()){
			
			Pair<Integer,Integer> se=getOffsets(node,code);
			
			stmt_offset.setFirst(se.getFirst());
			stmt_offset.setSecond(se.getSecond());
		}
		return true;
	}
	
	public static Pair<Integer,Integer> getOffsets(ASTNode node, String code){
		int start=node.getStartPosition(),end=start+node.getLength();
		switch(node.getNodeType()){
		case ASTNode.TYPE_DECLARATION:
			start=((TypeDeclaration)node).getName().getStartPosition();
			if(((TypeDeclaration)node).bodyDeclarations().size()>0)
				end=((BodyDeclaration)(((TypeDeclaration)node).bodyDeclarations().get(0))).getStartPosition();
			else
				end=start+((TypeDeclaration)node).getName().getLength();
			break;
		case ASTNode.FOR_STATEMENT:
			if(((ForStatement)node).getExpression()!=null){
				start=((ForStatement)node).getExpression().getStartPosition();
				end=start+((ForStatement)node).getExpression().getLength();
			}
			else{
				start=0;
				end=0;
			}
			break;
		case ASTNode.ENHANCED_FOR_STATEMENT:	
			start=((EnhancedForStatement)node).getExpression().getStartPosition();
			end=start+((EnhancedForStatement)node).getExpression().getLength();
			break;
		case ASTNode.DO_STATEMENT:	
			start=((DoStatement)node).getExpression().getStartPosition();
			end=start+((DoStatement)node).getExpression().getLength();
			break;
		case ASTNode.WHILE_STATEMENT:	
			start=((WhileStatement)node).getExpression().getStartPosition();
			end=start+((WhileStatement)node).getExpression().getLength();
			break;
		case ASTNode.SYNCHRONIZED_STATEMENT:
			start=((SynchronizedStatement)node).getExpression().getStartPosition();
			end=start+((SynchronizedStatement)node).getExpression().getLength();
			break;
		case ASTNode.IF_STATEMENT:
			IfStatement i=((IfStatement)node);
			start=i.getExpression().getStartPosition();
			end=i.getExpression().getLength()+start;
			break;
		case ASTNode.METHOD_DECLARATION:
			MethodDeclaration m=(MethodDeclaration)node;
			start=(m.getName().getStartPosition());
			//end=(m.getBody().getStartPosition());
			end=start+m.getName().getLength();
			break;
		case ASTNode.SWITCH_STATEMENT:
			if(((SwitchStatement)node).statements().size()>0)
				end=((Statement)((SwitchStatement)node).statements().get(0)).getStartPosition();
			break;
		case ASTNode.METHOD_INVOCATION:
			end=((MethodInvocation)node).getName().getLength()+((MethodInvocation)node).getName().getStartPosition();
		}
		
		do{
			end--;
		}
		while((end<code.length() && end>start) && (code.charAt(end)=='\n' || code.charAt(end)=='\t' || code.charAt(end)==' '));
		end++;
		
		return new Pair<Integer,Integer>(start,end);
	}
	
}
