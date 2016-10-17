package com.slicerplugin.slicer.dependencies;

import java.util.ArrayList;
import java.util.HashSet;

public class Dep {
	
	/**
	 * Local variables of this function
	 */
	HashSet<String> local_vars=new HashSet<String>();
	
	ArrayList<String> params=new ArrayList<String>();
	
	HashSet<String> deps=new HashSet<String>();
	/**
	 * 
	 * @param key
	 * @return true when the id passed as parameter is local to the function
	 */
	protected boolean is_local(String key){
		return local_vars.contains(key);
	}
	
	protected boolean is_external(String key){
		return (!local_vars.contains(key)) && (!params.contains(key));
	}
}
