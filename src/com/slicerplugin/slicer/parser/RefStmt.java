package com.slicerplugin.slicer.parser;

import java.util.ArrayList;

/**
 * RefStmt represent the references made by a statement. 
 * A reference could be for write purpose or read purposes.
 * 
 * @author Tomas Bortoli
 *
 */
public class RefStmt {
	public ArrayList<String> write,read;
	
	@Override public String toString(){
		String ret="";
		
		if(write!=null){
			ret+="write:\n";
			for(String s : write) ret+=s+" ";
			ret+="\n";
		}
		if(read!=null){
			ret+="read:\n";
			for(String s : read) ret+=s+" ";
			ret+="\n";
		}
		
		return ret;
	}
}
