package com.slicerplugin.popup.actions;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;

import com.slicerplugin.generic.*;
import com.slicerplugin.slicer_wrapper.DocumentTrackerListener;
import com.slicerplugin.slicer_wrapper.SlicerWrapper;

/**
 * This class represent the mark context menu and his function in the plugin. It takes track of all the marker and provide public methods to place
 * down marker and annotations. For the user and for the slicer. It tracks all the editors.
 * 
 * @author Tomas
 * 
 */
public class Mark implements IEditorActionDelegate  {

	private static Shell shell;
	
	/**
	 * 
	 */
	static HashMap<IMarker,SimpleMarkerAnnotation> marks=new HashMap<IMarker,SimpleMarkerAnnotation>();
	
	/**
	 * Set of the markers placed directly from the user
	 */
	static Set<Integer> userMadeMarkers=new HashSet<Integer>();
	
	/**
	 * The full path of the project that is currently being marked
	 */
	public static String projectPath=null;
	
	/**
	 * Constructor. Never used
	 */
	public Mark() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	/**
	 * Action launched when the Mark context menu button is pressed
	 */
	public void run(IAction action) {
		mark();
	}
	
	/**
	 * Method invoked when the mark command is fired
	 */
	public static void mark(){
		
		String projectPath_=EclipseApi.getProjectPath();
		if(projectPath==null || 
				(projectPath_).equals(projectPath)
							){
			if(SlicerWrapper.getSlicedMode()){
				MessageDialog.openInformation(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
						Constants.pluginName,
						"Cannot mark in sliced mode");
				return;
			}
		}
		else{
			MessageDialog.openInformation(
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					Constants.pluginName,
					"Cannot mark files that belongs to different projects! (remove all markers before)");
			return;
		}
		
		projectPath=projectPath_;
		
		IEditorPart part;

		part =
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		
		
		//text editor?
		if(part instanceof ITextEditor){
		    ITextEditor editor = (ITextEditor)part;
		    IDocumentProvider provider = editor.getDocumentProvider();
		    IDocument document = provider.getDocument(editor.getEditorInput());
		    

		    ISelection sel = editor.getSelectionProvider().getSelection();
		    
		    TextSelection ts=((TextSelection)sel);
		    
		    /*MessageDialog.openInformation(
					shell,
					editor.getTitle(),
					ts.getStartLine()+" "+ts.getEndLine());//+" "+ts.getOffset()+" "+ts.getText());*/
		    
		    IEditorInput input=part.getEditorInput();
			  IResource resource=(IResource)input.getAdapter(IFile.class);
			  if (resource == null) {
			    resource=(IResource)input.getAdapter(IResource.class);
			  }
			  
			  try{
				  addMarkerAndAnnotationByOffset(resource,ts,editor,document,shell); 
			  }
			  catch(Exception e){
				  MessageDialog.openInformation(
						  shell,
							Constants.pluginName,
							"You cannot mark a line in a file that does not belong to a project");
			  }
		}
		
	}
	
	private static void addMarkerAndAnnotationByOffset(IResource resource, ITextSelection ts, ITextEditor editor, IDocument document, Shell shell) throws CoreException, BadLocationException{
		editor.setHighlightRange(ts.getOffset(),ts.getLength(), false);
		  
		Pair<Integer,Integer> p=MarkerUtilities.getSelectionStatementOffset(new Pair<Integer,Integer>(ts.getOffset(),ts.getLength()+ts.getOffset()));
		
		if(p.getFirst()!=null && p.getSecond()!=null){
			if(!existMarker(p,resource)){
				//note: you use the id that is defined in your plugin.xml
				IMarker marker = resource.createMarker("slicerPlugin.myMarker");
				
				marker.setAttribute(IMarker.CHAR_START, p.getFirst());
				marker.setAttribute(IMarker.CHAR_END, p.getSecond());
				
				marker.setAttribute(IMarker.MESSAGE, "Line marked for slicing!");
				marker.setAttribute(Constants.markerAttributeFile,resource.getFullPath().toString());
				
				addAnnotation(marker,p.getFirst(),p.getSecond(),editor,document);
			}
		}
	}
	
	private static boolean existMarker(Pair<Integer,Integer> p , IResource resource) throws CoreException{
		boolean ret=false;
		for(IMarker marker : marks.keySet()){
			int start = (int)marker.getAttribute(IMarker.CHAR_START);
			int end = (int)marker.getAttribute(IMarker.CHAR_END);
			if(start<=p.getFirst() && end>=p.getSecond()){
				ret=true;
				break;
			}
		}
		return ret;
	}
	
	public static HashMap<String,ArrayList<Pair<Integer,Integer>>> getMarkedOffsets() throws CoreException{
		
		HashMap<String,ArrayList<Pair<Integer,Integer>>> ret=new HashMap<String,ArrayList<Pair<Integer,Integer>>>();
		
		for(IMarker m : marks.keySet()){
			String file=(String)m.getAttribute(Constants.markerAttributeFile);
			ArrayList<Pair<Integer,Integer>> list=ret.get(file);
			if(list==null){
				list=new ArrayList<Pair<Integer,Integer>>();
				ret.put(file, list);
			}
			list.add(new Pair<Integer,Integer>(
					(Integer)m.getAttribute(IMarker.CHAR_START),(Integer)m.getAttribute(IMarker.CHAR_END)));
			
		}
		
		return ret;
	}
	
	public static void markSlice(HashMap<String,ArrayList<Pair<Integer,Integer>>> slice) throws CoreException, BadLocationException{
		IEditorReference[] editors=PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
		
		for(IEditorReference e : editors){
			IEditorPart ep=e.getEditor(false);
			if(ep instanceof ITextEditor){
				ITextEditor editor = (ITextEditor)ep;
			    IEditorInput input=ep.getEditorInput();
			    IDocumentProvider provider = editor.getDocumentProvider();
			    IDocument document = provider.getDocument(editor.getEditorInput());
			    IResource resource=(IResource)input.getAdapter(IFile.class);
			    
			    ArrayList<Pair<Integer,Integer>> toMark=slice.get(resource.getFullPath().toString());
			    if(toMark!=null)
			    	addMarkers(toMark,editor,document,resource);
			    
			    slice.remove(resource.getFullPath().toString());
			    
			}
		}
		
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		for(String file : slice.keySet()){
			ArrayList<Pair<Integer,Integer>> toMark = slice.get(file);
			addMarkers(toMark,null,null,root.findMember(file));
		}
			
	}
	
	private static void addMarkers(ArrayList<Pair<Integer,Integer>> markers, ITextEditor editor, IDocument document, IResource resource) throws CoreException, BadLocationException{
		for(Pair<Integer,Integer> p : markers){
			IMarker marker = resource.createMarker("slicerPlugin.myMarker");
			
			marker.setAttribute(IMarker.CHAR_START, p.getFirst());
			marker.setAttribute(IMarker.CHAR_END, p.getSecond());
			
			marker.setAttribute(IMarker.MESSAGE, "Line marked for slicing!");
			marker.setAttribute(Constants.markerAttributeFile,resource.getFullPath().toString());
			
			if(editor!=null && document!=null)
				addAnnotation(marker,p.getFirst(),p.getSecond(),editor,document);
			else
				marks.put(marker,null);
		}
	}
	
	/**
	 * Return all markers of an editor, using the internal list of markers
	 * @param editor
	 * @return
	 * @throws CoreException
	 */
	public static IMarker[] getAllMarkers2(IEditorPart editor) throws CoreException{
		IResource resource=(IResource)editor.getEditorInput().getAdapter(IFile.class);
		
		int count=0;
		for(IMarker marker : marks.keySet()){
			if(marker.getAttribute(Constants.markerAttributeFile).equals(resource.getFullPath()))
				count++;
		}
		
		IMarker[] ret = new IMarker[count];
		count=0;
		for(IMarker marker : marks.keySet()){
			if(marker.getAttribute(Constants.markerAttributeFile).equals(resource.getFullPath()))
				ret[count++]=marker;
		}
		
		return ret;
	}
	
	
	
	/**
	 * Remove the markers pinned by the user from the internal structure
	 */
	public static void cleanUserMarkers(){
		userMadeMarkers.clear();
	}
	
	public static boolean existMarkers(){
		return (marks.size()!=0) ? true : false;
	}
	
	
	
	/**
	 * Method to convert a set of single lines of code into blocks of code merging adjacent lines
	 * @param lines
	 * @param normalized
	 * @return
	 */
	public static ArrayList<Pair<Integer,Integer>> getBlocksFromLines(Set<Integer> lines, boolean normalized){
		
		ArrayList<Integer> al=new ArrayList<Integer>(lines);
		Collections.sort(al);
		
		ArrayList<Pair<Integer,Integer>> mrks=new ArrayList<Pair<Integer,Integer>>();
		
		boolean newBlock=true;
		int start=-2,prev=-2,end=-2;
		for(int line : al){
			if(newBlock){
				start=line;
				prev=start;
				end=start;
				newBlock=false;
			}else{
				if(line==prev+1){
					end=line;
					prev=line;
				}
				else{
					//ret|=addMarkerNormalized(start,end);
					if(!normalized)	mrks.add(new Pair<Integer,Integer>(start-1,end-1));
					else	mrks.add(new Pair<Integer,Integer>(start,end));
					start=line;
					prev=start;
					end=start;
				}
			}
		}
		if(!normalized)	mrks.add(new Pair<Integer,Integer>(start-1,end-1));
		else	mrks.add(new Pair<Integer,Integer>(start,end));
		
		return mrks;
	}
	
	
	
	
	/**
	 * Check if a marker already exists
	 * @param resource current resource in which check
	 * @param startRow start row to forge the marker check
	 * @param endRow end row to forge the marker to check
	 * @return true if the marker exist, or if exist a marker that contain the marker forged with parameters, false otherwise 
	 * @throws CoreException
	 */
	private static boolean markerAlreadyExist(IResource resource, int startRow, int endRow) throws CoreException{
		boolean ret=false;
		
		IMarker[] ms=resource.findMarkers("slicerPlugin.myMarker", false, 0);
        for(int i=0;i<ms.length && !ret;i++)
        	if(ms[i].getAttribute(Constants.markerAttributeFile).equals(resource.getFullPath())){
        		int markerStartLine=(int)ms[i].getAttribute(IMarker.LINE_NUMBER);
        		int markerEndLine=(int)ms[i].getAttribute(Constants.markerAttributeEndLine);
        		
	        	if((markerStartLine<=startRow && markerEndLine>=endRow))
	        		ret=true;
	        	
        	}
	    return ret;
	}
	
	/**
	 * A logic function to check if two markers are intersecting or are neighbor of lines.
	 * @param startRow
	 * @param endRow
	 * @param markerStartLine
	 * @param markerEndLine
	 * @return
	 */
	private static boolean intersectionOrNeighbor(int startRow, int endRow, int markerStartLine, int markerEndLine){
		boolean ret=false;
		if((markerStartLine<=startRow && markerEndLine>=endRow)
    	||
    	(startRow<=markerStartLine && endRow>=markerEndLine))
    		ret=true;
    	else if(markerEndLine>=startRow && markerStartLine<=endRow)
    		ret=true;
    	else if(markerStartLine>=startRow && markerStartLine<=endRow)
    		ret=true;
    	else if(endRow==markerStartLine-1 || startRow==markerEndLine+1)
    		ret=true;
    	else
    		ret=false;
    	
    	return ret;
	}
	
	
	
	
	
	/**
	 * Add an annotation to a marker
	 * @param marker
	 * @param min the start row
	 * @param max the end row
	 * @param editor editor to which add
	 * @param d document to which add
	 * @throws BadLocationException
	 * @throws CoreException
	 */
	public static void addAnnotation(IMarker marker, int min, int max,
			ITextEditor editor, IDocument d) throws BadLocationException, CoreException {
		//The DocumentProvider enables to get the document currently loaded in the editor
		IDocumentProvider idp = editor.getDocumentProvider();
		
		//This is the document we want to connect to. This is taken from 
		//the current editor input.
		IDocument document = idp.getDocument(editor.getEditorInput());
		
		//The IannotationModel enables to add/remove/change annotation to a Document 
		//loaded in an Editor
		IAnnotationModel iamf = idp.getAnnotationModel(editor.getEditorInput());
		
		
		
		//Note: The annotation type id specify that you want to create one of your 
		//annotations
		SimpleMarkerAnnotation ma = new SimpleMarkerAnnotation(
		"slicerPlugin.myAnnotation",marker);
		
		
		marks.put(marker,ma);
		
		int startOffset=min;
		int length=max-min;//d.getLineOffset(endLine)+d.getLineLength(endLine)-startOffset;
		
		//marker.setAttribute(IMarker.CHAR_START, startOffset);
		//marker.setAttribute(IMarker.CHAR_END, endOffset);
		//marker.setAttribute(Constants.markerAttributeLength, length);
		
		//Finally add the new annotation to the model
		iamf.connect(document);
		iamf.addAnnotation(ma,new Position(startOffset,length));
		iamf.disconnect(document);
		
	}
	
	
	
	/**
	 * Eclipse normally provide an event of this type for each object that implement IEditorActionDelegate interface.
	 * Event launched when the user select something in an editor. Event used to prevent a user to select more blocks concurrently.
	 * 
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		
		if(!SlicerWrapper.getSlicedMode())
			return;
		
		
		IEditorPart part=PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		ITextEditor editor =  (ITextEditor)part;
		IEditorInput input=part.getEditorInput();
		  IResource resource=(IResource)input.getAdapter(IFile.class);
		  if (resource == null) {
		    resource=(IResource)input.getAdapter(IResource.class);
		    if(resource==null)
		    	return;
		  }
		  
		
		
		int s=((TextSelection)selection).getStartLine();
		int e=((TextSelection)selection).getEndLine();
		
		int res;
		if( ( res=SlicerWrapper.containedInMarker(s,e)) != -1 ){
			
			DocumentTrackerListener.stop=true;
			
			
			
			//If the selection is on more marker, avoid the selection
			//editor.selectAndReveal(((TextSelection)selection).getStartLine(), 0);
			
			IDocumentProvider idp = editor.getDocumentProvider();
			IDocument document = idp.getDocument(editor.getEditorInput());
			
			try {
				editor.selectAndReveal(document.getLineOffset(res),0);
			} catch (BadLocationException e1) {
				e1.printStackTrace();
			}
		}
		else
			DocumentTrackerListener.stop=false;
		
	}
	
	/**
	 * When the editor is changed, this event is fired to draw the correct annotations (red lines) associated to markers.
	 * Otherwise, when an editor is closed and reopened, the annotations would disappear
	 */
	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		
		/*IEditorPart part =
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
				
				 MessageDialog.openInformation(
							shell,
							"",
							part.getClass().toGenericString());*/
		
		
		
		//If the current editor is a text editor,
		if(targetEditor instanceof ITextEditor){
			ITextEditor editor=(ITextEditor)targetEditor;
			
			IDocumentProvider idp = editor.getDocumentProvider();
			IDocument document = idp.getDocument(editor.getEditorInput());
			IAnnotationModel iamf = idp.getAnnotationModel(editor.getEditorInput());
			IResource res=(IResource)targetEditor.getEditorInput().getAdapter(IFile.class);
			
			iamf.connect(document);
			//show the annotations that belongs to current editor
			for(IMarker marker: marks.keySet()){
				SimpleMarkerAnnotation a=marks.get(marker);
				try {
					//if the annotation belongs to the current file, recover the position and add it!!
					if(res.getFullPath().toString().equals(marker.getAttribute(Constants.markerAttributeFile))){
						if(a!=null)
							iamf.addAnnotation(a,
									MarkerUtilities.getPositionFromMarker(marker,document));
						else
							addAnnotation(marker,(int)marker.getAttribute(IMarker.CHAR_START),
									(int)marker.getAttribute(IMarker.CHAR_END),editor,document);
					}
				} catch (CoreException e) {
					e.printStackTrace();
				} catch (BadLocationException e){
					e.printStackTrace();
				}
			}
			
			iamf.disconnect(document);
		}
	}
}
