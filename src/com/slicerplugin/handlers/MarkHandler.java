package com.slicerplugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.slicerplugin.popup.actions.Mark;

/**
 * Class of the mark button on the context menu
 * 
 * @author Tomas
 *
 */
public class MarkHandler extends AbstractHandler{
	
	/**
	 * Called when the mark button is pressed
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Mark.mark();
		return null;
	}

}
