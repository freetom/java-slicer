package com.slicerplugin.popup.actions;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;

import com.slicerplugin.generic.Constants;
import com.slicerplugin.generic.MarkerUtilities;
import com.slicerplugin.slicer_wrapper.SlicerWrapper;

/**
 * Class that represent the unmark context menu and his function in the plugin. It handles the elimination of markers and their related annotations.
 * 
 * @author Tomas
 *
 */
public class Unmark implements IEditorActionDelegate {
	
	/**
	 * Unmark button pressed, remove the selected markers!
	 */
	@Override
	public void run(IAction action) {
		
		try {
			unmark();
		} catch (BadLocationException e) {
			System.out.println("error in unmarking");
			
		}
		
		
	}
	
	
	/**
	 * Function called when the unmark command is fired
	 * @throws BadLocationException
	 */
	public static void unmark() throws BadLocationException{
		if(SlicerWrapper.getSlicedMode()){
			MessageDialog.openInformation(
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					Constants.pluginName,
					"Cannot unmark in sliced mode");
			return;
		}
		try {
			
			
			IEditorPart part=MarkerUtilities.getCurrentEditor();
			ITextEditor editor = (ITextEditor)part;
		    IDocumentProvider provider = editor.getDocumentProvider();
		    IDocument document = provider.getDocument(editor.getEditorInput());
		    ISelection sel = editor.getSelectionProvider().getSelection();
		    TextSelection ts=((TextSelection)sel);
		    IEditorInput input=part.getEditorInput();
			IResource resource=(IResource)input.getAdapter(IFile.class);
			if (resource == null) {
				resource=(IResource)input.getAdapter(IResource.class);
			}
			
			ArrayList<IMarker> markers=MarkerUtilities.getAllMarkersOfCurrentEditor();
			for( IMarker marker : markers){
				int start=(int)marker.getAttribute(IMarker.CHAR_START);
				int end=(int)marker.getAttribute(IMarker.CHAR_END);
				if((ts.getOffset()<=start && ts.getOffset()+ts.getLength()>=end) ||
						(start<=ts.getOffset() && end>=ts.getOffset()+ts.getLength())){
					
					removeMarkerAndAnnotation(marker,editor);
				}
			}
			
			/*IEditorPart part;
			part =
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			IMarker[] ms=MarkerUtilities.getAllMarkers(part);
			TextSelection ts=MarkerUtilities.getCurrentTextSelection();
			IResource resource=MarkerUtilities.getCurrentResource();
			
			int startRow=ts.getStartLine(),endRow=ts.getEndLine();
			
			for(int i=startRow; i<=endRow;i++)
				Mark.userMadeMarkers.remove(i);
			
			for(int i=0;i<ms.length;i++){
				if(ms[i].getAttribute(Constants.markerAttributeFile).equals(resource.getFullPath())){
					int markerStartLine=(int)ms[i].getAttribute(IMarker.LINE_NUMBER);
	        		int markerEndLine=(int)ms[i].getAttribute(Constants.markerAttributeEndLine);
		        	if(((markerStartLine<=startRow && markerEndLine>=endRow)
		    	        	||
		    	        	(startRow<=markerStartLine && endRow>=markerEndLine))
		    	        	||
		    	        	(markerEndLine>=startRow && markerStartLine<=endRow)
		    	        	||
		    	        	(markerStartLine>=startRow && markerStartLine<=endRow))
		    	        	{
		        		
		        		
		        		removeSelectedMarkerLinesAndAnnotationForUser(ms[i],part,startRow,endRow,markerStartLine,markerEndLine);
		        		
		        		((ITextEditor)part).resetHighlightRange();
		        	}
				}
			}*/
			
		} catch (CoreException e) {
			
			e.printStackTrace();
		}
	}
	
	
	
	/**
	 * Remove a marker and an annotation totally
	 * @param marker
	 * @param part
	 * @throws CoreException
	 */
	public static void removeMarkerAndAnnotation(IMarker marker,IEditorPart part) throws CoreException{
		
		//remove the annotation from the local reference
		SimpleMarkerAnnotation annotation=Unmark.removeAnnotationOfMarker(marker);
		
		//delete the marker
		marker.delete();
		
		//Remove the marker from the local lists of marker (my reference)
		Mark.marks.remove(marker);
		if(Mark.marks.isEmpty())
			Mark.projectPath=null;
		
		ITextEditor editor=((ITextEditor)part);
		IDocumentProvider idp = editor.getDocumentProvider();
		IAnnotationModel iamf = idp.getAnnotationModel(editor.getEditorInput());
		IDocument document = idp.getDocument(editor.getEditorInput());
		
		
		//Finally remove the annotation from the document
		iamf.connect(document);
		iamf.removeAnnotation(annotation);
		iamf.disconnect(document);
		
	}
	
	/**
	 * Overload, use more parameters to save time. Otherwise that parameters should be generated inside with eclipse API.
	 * @param marker
	 * @param part
	 * @param editor
	 * @param idp
	 * @param iamf
	 * @param document
	 * @throws CoreException
	 */
	public static void removeMarkerAndAnnotation(IMarker marker,IEditorPart part,ITextEditor editor,IDocumentProvider idp,IAnnotationModel iamf,IDocument document) throws CoreException{
		
		//remove the annotation from the local reference
		SimpleMarkerAnnotation annotation=Unmark.removeAnnotationOfMarker(marker);
		
		//delete the marker
		marker.delete();
		
		//Remove the marker from the local lists of marker (my reference)
		Mark.marks.remove(marker);
		if(Mark.marks.isEmpty())
			Mark.projectPath=null;
		
		//Finally remove the annotation from the document
		iamf.removeAnnotation(annotation);
		
	}
	
	
	/**
	 * Remove all the markers from the editors that has content not saved (dirty)
	 * @return true if something is removed, false otherwise
	 * @throws CoreException
	 */
	public static boolean removeAllMarkersOfDirtyEditors() throws CoreException{
		boolean ret=false;
		IEditorReference[] editors=PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences(); //get all the editors
		for(int i=0;i<editors.length;i++)
			if(editors[i].getEditor(false) instanceof ITextEditor && editors[i].getEditor(false).isDirty()){	//if text editor
				for(IMarker marker : Mark.marks.keySet()){
					IResource resource=(IResource)editors[i].getEditorInput().getAdapter(IFile.class);
					if(resource.getFullPath().equals(marker.getAttribute(Constants.markerAttributeFile))){ //if marker on the current file
						Unmark.removeMarkerAndAnnotation(marker, editors[i].getEditor(false)); //remove marker and annotation.
						ret=true;
					}
				}
			}
		return ret;
	}
	
	public static boolean removeAllMarkers() throws CoreException{
		boolean ret=false;
		IEditorReference[] editors=PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences(); //get all the editors
		for(int i=0;i<editors.length;i++)
			if(editors[i].getEditor(false) instanceof ITextEditor){	//if text editor
				for(IMarker marker : Mark.marks.keySet()){
					IResource resource=(IResource)editors[i].getEditorInput().getAdapter(IFile.class);
					if(resource.getFullPath().equals(marker.getAttribute(Constants.markerAttributeFile))){ //if marker on the current file
						Unmark.removeMarkerAndAnnotation(marker, editors[i].getEditor(false)); //remove marker and annotation.
						ret=true;
					}
				}
			}
		return ret;
	}
	
	
	/**
	 * Remove all the markers of a document
	 * @param doc
	 * @throws CoreException
	 */
	public static void removeAllMarkers(IDocument doc) throws CoreException{
		Mark.cleanUserMarkers();
		
		IEditorReference[] editors=PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences(); //get all the editors
		for(int i=0;i<editors.length;i++)
			if(editors[i].getEditor(false) instanceof ITextEditor){	//if text editor
				if(((ITextEditor)editors[i].getEditor(false)).getDocumentProvider().getDocument(editors[i].getEditor(false).getEditorInput())==doc){ //if current doc
					for(IMarker marker : Mark.marks.keySet()){
						IResource resource=(IResource)editors[i].getEditorInput().getAdapter(IFile.class);
						if(resource.getFullPath().equals(marker.getAttribute(Constants.markerAttributeFile))) //if marker on the current file
							Unmark.removeMarkerAndAnnotation(marker, editors[i].getEditor(false)); //remove marker and annotation.
					}
					return;
				}
			}
	}
	
	
	/**
	 * Remove the annotation of a given marker
	 * @param marker
	 * @return the annotation removed, false otherwise
	 */
	public static SimpleMarkerAnnotation removeAnnotationOfMarker(IMarker marker){
		
		SimpleMarkerAnnotation an=Mark.marks.get(marker);
		Mark.marks.put(marker, null);
		return an;
	}
	
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		
		
	}

	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		
		
	}

}
