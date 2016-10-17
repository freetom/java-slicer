package com.slicerplugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;

import com.slicerplugin.slicer_wrapper.SlicerWrapper;




/**
 * Class of the slice button.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class SliceHandler extends AbstractHandler {
	
	
	/**
	 * The constructor.
	 */
	public SliceHandler() {
	}

	/**
	 * the slice button has been pressed, compute a slice, save the result and mark the relevant rows.
	 * 
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		try {
			SlicerWrapper.slice();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

}



