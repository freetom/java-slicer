package com.slicerplugin.slicer.parser;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jdt.core.dom.ASTNode;

import com.slicerplugin.generic.Pair;

/**
 * RefID show which statements use a specific identifier and in which mode it use it.
 * Identifiers are class names, package names, methods name, variable name (object, primitive, attribute or call)
 * 
 * @author Tomas Bortoli
 *
 */
public class RefID {
	
	/**
	 * A key that uniquely identify the identifier
	 */
	public String key;
	
	public ArrayList<Pair<String,ASTNode>> refs=new ArrayList<Pair<String,ASTNode>>();
	
	/**
	 * List of the statements that use this id and how they use it
	 */
	public ArrayList<ASTNode> write,read,def;
	//public ArrayList<ASTNode> calls;
	public ArrayList<Pair<String,Integer>> param;
	
	public ArrayList<String>[] params_inline;
	
	public boolean is_object;
	public boolean is_static;
	public boolean is_call;
	public boolean is_type;
	public boolean is_of_known_type;
	
	public String type;
	
	public ASTNode method_declaration;
	public ASTNode invocation;
	
	public String call_name;
	
	/**
	 * Reference to the father identifier (in the case we have qualified names, so object.attribute)
	 */
	public RefID father;
	
	public ArrayList<RefID> fields;
	public ArrayList<RefID> invocations;
	
	@Override public String toString(){
		String ret="key: "+key+"\n";
		ret+="is_object="+is_object+"\n";
		
		if(write!=null){
			ret+="write:\n";
			for(ASTNode s : write) ret+=s+" ";
			ret+="\n";
		}
		if(read!=null){
			ret+="read:\n";
			for(ASTNode s : read) ret+=s+" ";
			ret+="\n";
		}
		
		return ret;
	}
	
	@Override public int hashCode(){
		return key.hashCode();
	}
	
	@Override public Object clone(){
		RefID ret=new RefID();
		ret.call_name=this.call_name;
		//ret.calls=this.calls;
		ret.def=this.def;
		ret.father=this.father;
		ret.fields=this.fields;
		ret.invocation=this.invocation;
		ret.invocations=this.invocations;
		ret.is_call=this.is_call;
		ret.is_object=this.is_object;
		ret.is_of_known_type=ret.is_of_known_type;
		ret.is_static=this.is_static;
		ret.is_type=this.is_type;
		ret.key=this.key;
		ret.method_declaration=this.method_declaration;
		ret.param=this.param;
		ret.read=this.read;
		ret.type=this.type;
		ret.write=this.write;
		
		
		return ret;
		
	}
}
