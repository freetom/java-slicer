package com.slicerplugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.jface.dialogs.MessageDialog;

/**
 * Our handler extends AbstractHandler, an IHandler base class.
 * This class handles the command that run when a user press on the Info button
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class InfoHandler extends AbstractHandler {
	/**
	 * The constructor.
	 */
	public InfoHandler() {
	}

	/**
	 * the Info command has been executed, so extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageDialog.openInformation(
				window.getShell(),
				com.slicerplugin.generic.Constants.pluginName,
				"To facilitate bug fixing on java code.\n"+
				"To use the plugin, just mark one or more rows in one of your java projects.\n"+
				"You can mark by: first select something, then click the right button of the mouse and click on the mark menu.\n"+
				"Then click the slice button on the toolbar. You will see all the intrested rows.\n"+
				"To pass to the Sliced Mode press the second button.\n"+
				"Edit what you want and when you've done click the slice button again to return in normal mode with the patched code.\n"+
				"If you need to restore your code when you are in sliced mode to avoid changes, press the blue arrow.\n"+
				"If you need to get the code how was before the previous slice, press the green button, you can press the green button n times, where n is the number of the previous states.");
		return null;
	}
}
