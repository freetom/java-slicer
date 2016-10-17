package com.slicerplugin.slicer.dependencies;

public class Var {
	
	/**
	 * -1 if it is not a parameter
	 */
	int param_index;
	
	boolean is_static, is_local_field;
	String key;
	
	@Override public String toString(){
		String ret="";
		ret+=param_index+"\n";
		ret+=is_static+"\n";
		ret+=key+"\n";
		return ret;
	}
	
	@Override public boolean equals(Object o){
		if(o instanceof Var){
			Var v=(Var)o;
			if(this.param_index!=-1)
				return this.param_index==v.param_index;
			else
				return this.param_index==v.param_index && this.key.equals(v.key);
		}
		return false;
	}
}
