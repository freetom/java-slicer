package com.slicerplugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.slicerplugin.slicer_wrapper.SlicerWrapper;



/**
 * This class is the go on sliced mode toolbar button. It just start call the slicer class to do all.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class goSlice extends AbstractHandler {
	
	
	/**
	 * The constructor.
	 */
	public goSlice() {
	}

	/**
	 * the slice button has been pressed, handle switching to and from sliced mode
	 * 
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		IEditorPart part;

		part =
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		
		
		 
		if(part instanceof ITextEditor){
		    ITextEditor editor = (ITextEditor)part;
		    IDocumentProvider provider = editor.getDocumentProvider();
		    IDocument document = provider.getDocument(editor.getEditorInput());
		    IEditorInput input=part.getEditorInput();
		    
			IResource resource=(IResource)input.getAdapter(IFile.class);
			if (resource == null) {
				resource=(IResource)input.getAdapter(IResource.class);
			}
			  
			try {
				SlicerWrapper.goSliceMode(document);
			} catch (CoreException e) {
				e.printStackTrace();
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
			
		    
		}
		
		
		return null;
	}

}



