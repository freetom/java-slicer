package com.slicerplugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PartInitException;

import com.slicerplugin.slicer_wrapper.SlicerWrapper;

/**
 * Handler used when restore button is pressed and released
 * @author Tomas
 *
 */
public class RestoreHandler extends AbstractHandler {
	
	/**
	 * Restore button pressed, restore the original code in the file and switch back to normal mode
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		try {
			SlicerWrapper.reverse();
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		return null;
	}

}
