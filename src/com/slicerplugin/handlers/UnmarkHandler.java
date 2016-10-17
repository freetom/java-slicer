package com.slicerplugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;

import com.slicerplugin.popup.actions.Unmark;

/**
 * Class for the unmark button on the context menu
 * 
 * @author Tomas
 *
 */
public class UnmarkHandler extends AbstractHandler {
	
	/**
	 * Called when the unmark button is pressed
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		try {
			Unmark.unmark();
		} catch (BadLocationException e) {
			System.out.println("error in unmarking");
		}
		return null;
	}

}
