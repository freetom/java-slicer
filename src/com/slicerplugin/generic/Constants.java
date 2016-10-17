package com.slicerplugin.generic;

/**
 * Just a few useful Constants
 * @author Tomas
 *
 */
public class Constants {
	
	/**
	 * Attribute name for end line in marker objects
	 */
	public final static String markerAttributeEndLine="endline";
	
	/**
	 * Attribute name to identify the file in marker objects
	 */
	public final static String markerAttributeFile="file";
	
	/**
	 * Attribute name to store marker length in marker objects
	 */
	public final static String markerAttributeLength="length";
	
	/**
	 * Name of the plugin, used in message boxes
	 */
	public final static String pluginName="Slicer Plugin";
	
	/**
	 * Text used to fill out gaps in sliced mode
	 */
	public final static String gapText="//@slice{...}";
	
	
	/**
	 * Property used in AST nodes to indicate that the node has been visited
	 */
	public final static String inSlice="inSlice";
	
	/**
	 * Message to show to the user when a unit has compile problems
	 */
	public final static String compileProblemsMessage="One of your modules has compile problems. Not allowed to slice";
	
	
}
