package com.slicerplugin.editors;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;

import com.slicerplugin.slicer_wrapper.SlicerWrapper;

/**
 * Sliced mode editor; Editor used for file in sliced mode.
 * 
 * @author Tomas
 *
 */
public class slicedModeEditor extends CompilationUnitEditor{
	
	/**
	 * Method called from eclipse system to check if the editor is dirty 
	 * (means if it needs a save operation to syncronize the current content with the content on disk)
	 */
	@Override
	public boolean isDirty(){
		return false;
	}
	
	@Override
	public boolean isSaveAsAllowed(){
		return false;
	}
	
	/**
	 * Method called from eclipse to get the string to put in the tab of the editor
	 */
	@Override
	public String getPartName(){
		return super.getPartName()+"@Sliced";
	}
	
}
