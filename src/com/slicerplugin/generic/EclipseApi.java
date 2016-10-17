package com.slicerplugin.generic;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public class EclipseApi {
	
	public static String getProjectPath(){
		
		IEditorPart part =
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
				
		if(part instanceof ITextEditor){
			IEditorInput input=part.getEditorInput();
			
			IResource resource=(IResource)input.getAdapter(IFile.class);
			if (resource == null) {
				resource=(IResource)input.getAdapter(IResource.class);
			}
			
			return resource.getFullPath().toString().split("/")[1];
		}
		return null;
	}
}
