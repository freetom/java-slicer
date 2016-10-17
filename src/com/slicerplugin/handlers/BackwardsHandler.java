package com.slicerplugin.handlers;

import java.util.Stack;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.slicerplugin.slicer_wrapper.SlicerWrapper;

/**
 * Class that handles the funcionality of the backwards button.
 * @author Tomas
 *
 */
public class BackwardsHandler extends AbstractHandler {
	
	/**
	 * The history of the previous states of the code in the form of a stack
	 */
	static Stack<String> states=new Stack<String>();
	
	/**
	 * Backwards button on click
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		if(SlicerWrapper.getSlicedMode()){
			IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
			MessageDialog.openInformation(
					window.getShell(),
					com.slicerplugin.generic.Constants.pluginName,
					"You can't use the backwards function when you are in Sliced Mode"
					);
			return null;
		}
		
		
		ITextEditor editor = (ITextEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
	    IDocumentProvider provider = editor.getDocumentProvider();
	    IDocument doc = provider.getDocument(editor.getEditorInput());
		
	    String s;
	    if( !states.empty() && (s = states.pop()) != null)
	    	doc.set(s);
	    else{
	    	IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
			MessageDialog.openInformation(
					window.getShell(),
					com.slicerplugin.generic.Constants.pluginName,
					"No more previous states in memory!"
					);
			return null;
	    }
	    
	    //save the file
	    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().doSave(null);
	    
		return null;
	}
	
	/**
	 * Add a state to the stack
	 * @param state
	 */
	public static void addState(String state){
		String s=new String(state);
		states.push(s);
	}
	
	/**
	 * Pop a state from the stack
	 */
	public static void popState(){
		states.pop();
	}

}
