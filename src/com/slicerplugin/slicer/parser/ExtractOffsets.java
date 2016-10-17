package com.slicerplugin.slicer.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import com.slicerplugin.generic.Constants;
import com.slicerplugin.generic.Pair;

/**
 * Extract the offsets to remove from the code to make the slice.
 * This class simply visit a compilation unit (or a subtree) and
 * extract the offsets of the node that are not marked to be in
 * the slice.
 * Some computations are made to prevent too much '\t' to be present.
 * (We need some hci  )
 * 
 * @author Tomas Bortoli
 *
 */
public class ExtractOffsets extends ASTVisitor{
	
	private Stack<Pair<Integer,Integer>> toMark=new Stack<Pair<Integer,Integer>>();
	private ArrayList<Pair<Integer,Integer>> toMark_list=new ArrayList<Pair<Integer,Integer>>();
	
	/**
	 * The original code of the current AST
	 */
	String code;
	
	/**
	 * To extract corrected offsets of the unwanted blocks we need the original code 
	 * @param code
	 */
	public ExtractOffsets(String code){
		this.code=code;
	}
	
	private void removeUnwantedMarked(){
		for(int i=0;i<toMark.size();i++){
			Pair<Integer,Integer> p = toMark.get(i);
			int j=p.getFirst();
			char c;
			while(j<=p.getSecond()){
				if((c=code.charAt(j++))!=' ' && c!='\t' && c!='\r' && c!='\n' && c!=',' && c!='(' && c!=')')
					break;
				//else
				//	toMark.get(i).setFirst(j);
			}
			if(j>p.getSecond())
				toMark.remove(i--);
			/*else{
				j=p.getSecond();
				while(j>p.getFirst()){
					if((c=code.charAt(j--))!=' ' && c!='\t' && c!='\r' && c!='\n' && c!=',' && c!='(' && c!=')')
						break;
					else
						toMark.get(i).setSecond(j+1);
				}
			}*/
		}
	}
	
	public ArrayList<Pair<Integer,Integer>> getMarked(){
		removeUnwantedMarked();
		
		toMark_list=new ArrayList<Pair<Integer,Integer>>(toMark);
		Collections.sort(toMark_list);
		return toMark_list;
	}
	/**
	 * Correct the bad offset for a "normal" node of the AST
	 * This procedure is aimed to not keep additional unwanted characters like ' ' and '\t'
	 * @param node
	 * @return
	 */
	private boolean visit_node(ASTNode node){
		RefStmt ref=InitSlicing.stmt_table.get(node);
		if(ref==null)
			return false;
		return helper_visit(node);
	}
	
	private boolean special_visit(ASTNode node){
		return helper_visit(node);
	}
	
	private boolean helper_visit(ASTNode node){
		Collections.sort(toMark);
		if(node.getProperty(Constants.inSlice)==null){
			if(toMark.size()>0){
				Pair<Integer,Integer> toRemove=GetSelectedStmt.getOffsets(node, code);
				Pair<Integer,Integer> last=toMark.pop();
				
				if(contained_segment(last,toRemove)){
					Pair<Integer,Integer> new_segment=new Pair<Integer,Integer>(last.getFirst(), toRemove.getFirst());
					last=new Pair<Integer,Integer>(toRemove.getSecond(),last.getSecond());
					toMark.push(new_segment);
					toMark.push(last);
				}
				else
					toMark.push(last);
			}
			return false;
		}
		else{
			Pair<Integer,Integer> toAdd=GetSelectedStmt.getOffsets(node, code);
			if(toMark.size()>0){
				Pair<Integer,Integer> last=toMark.pop();
				toMark.push(last);
				if(!contained_segment(last,toAdd))
					toMark.push(toAdd);
			}
			else{
				toMark.push(toAdd);
			}
			return true;
		}
	}
	/**
	 * 
	 * @param x
	 * @param y
	 * @return true when x contains y
	 */
	private boolean contained_segment(Pair<Integer,Integer> x, Pair<Integer,Integer> y){
		return x.getFirst()<=y.getFirst() && x.getSecond()>=y.getSecond();
	}
	
	/**
	 * Function called from VariableDeclarationFragment to correct the offset of the bad block.
	 * In this case, ',' symbol must be removed before and if not before after the appearance
	 * of this statement.
	 * @param node
	 * @return False anyway. No need to visit subnodes of a VariableDeclarationFragment
	 */
	/*private boolean visit_fragment_node(ASTNode node){
		if(node.getProperty(Constants.inSlice)==null){
			int start=node.getStartPosition();
			int end=node.getStartPosition()+node.getLength();
			
			int tmp_start=start;
			while(true){
				tmp_start--;
				char c=code.charAt(tmp_start);
				if(c==' ' || c=='\t')
					continue;
				else break;
			}
			if(code.charAt(tmp_start)==',')
				start=tmp_start;
			else{
				int tmp_end=end;
				while(true){
					tmp_end++;
					char c=code.charAt(tmp_end);
					if(c==' ' || c=='\t')
						continue;
					else break;
				}
				if(code.charAt(tmp_end)==',')
					end=tmp_end;
			}
			badOffsets.add(new Pair<Integer,Integer>(start,end));
		}
		return false;
	}*/
	
	@Override public boolean visit(TypeDeclaration node){
		return visit_node(node);
	}
	
	@Override public boolean visit(MethodDeclaration node){
		if(helper_visit(node)){
			List<SingleVariableDeclaration> l=node.parameters();
			for(SingleVariableDeclaration e : l)
				e.accept(this);
			node.getBody().accept(this);
		}
		return false;
	}
	
	
	
	@Override public boolean visit(EnumDeclaration node){
		return visit_node(node);
	}
	
	@Override public boolean visit(SingleVariableDeclaration node){
		visit_node(node);
		return false;
	}
	
	@Override public boolean visit(FieldDeclaration node){
		visit_node(node);
		return true;
	}
	
	
	@Override public boolean visit(VariableDeclarationStatement node){
		visit_node(node);
		return true;
	}
	
	
	@Override public boolean visit(ExpressionStatement node){
		visit_node(node);
		return true;
	}
	
	@Override public boolean visit(Assignment node){
		if(visit_node(node)){
			node.getLeftHandSide().setProperty(Constants.inSlice,true);
			if(node.getRightHandSide().getProperty(Constants.inSlice)==null)
					node.getRightHandSide().setProperty(Constants.inSlice, true);
		}
		return true;
	}
	
	@Override public boolean visit(EmptyStatement node){
		return special_visit(node);
	}
	
	
	
	
	
	/**
	 * if ( Expression ) Statement [ else Statement]
	 */
	@Override public boolean visit(IfStatement node){
		visit_node(node);
		return true;
	}
	

	/**
	 * AssertStatement:
     * assert Expression [ : Expression ] ;
	 */
	@Override public boolean visit(AssertStatement node){
		visit_node(node);
		return true;
	}
	
	/**
	 * DoStatement:
     * 	do Statement while ( Expression ) ;
	 */
	@Override public boolean visit(DoStatement node){
		special_visit(node);
		return true;
	}
	
	/**
	 * EnhancedForStatement:
     * 	for ( FormalParameter : Expression )
     *  	Statement
	 */
	@Override public boolean visit(EnhancedForStatement node){
		visit_node(node);
		return true;
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
		visit_node(node);
		return true;
	}
	
	
	/**
	 *  SwitchStatement:
	 *  	switch ( Expression )
     *       	{ { SwitchCase | Statement } } }
	 */
	@Override public boolean visit(SwitchStatement node){
		return visit_node(node);
	}
	
	@Override public boolean visit(SwitchCase node){
		return visit_node(node);
	}
	
	/**
	 *  WhileStatement:
     *  	while ( Expression ) Statement
	 */
	@Override public boolean visit(WhileStatement node){
		visit_node(node);
		return true;
	}
	
	/**
	 * SynchronizedStatement:
     * 	synchronized ( Expression ) Block
	 */
	@Override public boolean visit(SynchronizedStatement node){
		visit_node(node);
		return true;
	}
	
	@Override public boolean visit(ReturnStatement node){
		helper_visit(node);
		return true;
	}
	
	@Override public boolean visit(BreakStatement node){
		special_visit(node);
		return false;
	}
	
	@Override public boolean visit(ContinueStatement node){
		special_visit(node);
		return false;
	}
	
	@Override public boolean visit(VariableDeclarationFragment node){
		visit_node(node);//visit_fragment_node(node);
		return true;
	}
	
	
	@Override public boolean visit(MethodInvocation node){
		special_visit(node);
		List<Expression> l=node.arguments();
		for(Expression e : l){
			special_visit(e);
			e.accept(this);
		}
		
		return false;
	}
	
	@Override public boolean visit(ClassInstanceCreation node){
		special_visit(node);
		List<Expression> l=node.arguments();
		for(Expression e : l){
			special_visit(e);
			e.accept(this);
		}
		
		return false;
	}
	
	/*visitForInvocation vfi=new visitForInvocation(this);
	class visitForInvocation extends ExtractOffsets{
		ExtractOffsets eo;
		public visitForInvocation(ExtractOffsets eo){
			this.eo=eo;
		}
		@Override public boolean visit(MethodInvocation node){
			visit_node(node);
			List<Expression> l=node.arguments();
			for(Expression e : l){
				special_visit(e);
				e.accept(this);
			}
			
			return false;
		}
		
	}
	
	/*@Override public boolean visit(ConditionalExpression node){
		return false;
	}*/
	@Override public boolean visit(PrefixExpression node){
		visit_node(node);
		return false;
	}
	/*@Override public boolean visit(CastExpression node){
		return false;
	}*/
	@Override public boolean visit(PostfixExpression node){
		visit_node(node);
		return false;
	}
	/*@Override public boolean visit(InfixExpression node){
		return false;
	}
	@Override public boolean visit(ParenthesizedExpression node){
		return false;
	}*/
	@Override public boolean visit(QualifiedName node){
		visit_node(node);
		return false;
	}
	@Override public boolean visit(SimpleName node){
		visit_node(node);
		return false;
	}
	@Override public boolean visit(FieldAccess node){
		visit_node(node);
		return false;
	}
}
