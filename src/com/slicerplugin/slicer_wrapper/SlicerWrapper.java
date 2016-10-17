package com.slicerplugin.slicer_wrapper;

import java.util.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.slicerplugin.generic.EclipseApi;
import com.slicerplugin.generic.Pair;
import com.slicerplugin.handlers.BackwardsHandler;
import com.slicerplugin.popup.actions.Mark;
import com.slicerplugin.popup.actions.Unmark;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;

/**
 * The slicer class; all statics methods inside. No need for instancing.
 * This class handles the slicing, the entering in sliced mode and the exiting from it.
 * 
 * @author Tomas
 *
 */
public class SlicerWrapper {
	
	/**
	 * Indicates where we are in sliced mode
	 */
	private static volatile boolean slicedMode=false;
	
	/**
	 * Original text before slice happened
	 */
	static String originalText;
	
	/**
	 * List that represent the blocks of code in the sliced file.
	 * A gap is a single line in the sliced but represent a more or less big block in the original one.
	 * Instead, a portion of code that will go in the sliced file represent another region in the original file.
	 * This is maybe the most important object of the plugin. Is used to track the changes and apply the patches back.
	 */
	volatile static ArrayList<Block> blocks;
	
	
	
	/**
	 * Listener to track changes into the document. If in the future we'll have more documents this will be an array list
	 */
	static DocumentTrackerListener listener=new DocumentTrackerListener();
	
	/**
	 * List of the lines to hold when going in sliced mode
	 */
	static ArrayList<Pair<Integer,Integer>> toHoldList=null;
	
	/**
	 * Boolean flag to show when the thread is busy in computing a slice
	 */
	static boolean slicing=false;
	
	/**
	 * The full path of the file that is currently being sliced or in sliced mode
	 */
	static String fullPath;
	
	
	/**
	 * Count the number of tabs inside a string
	 * @param source
	 * @return
	 */
	private static int countTabs(String source){
		int count=0;
		int i=0;
		while(i<source.length()){
			if(source.charAt(i)=='\t')
				count++;
			i++;
		}
		return count;
	}
	
	/**
	 * 
	 * @param part
	 * @return true if there are dirty editors or markers
	 * @throws CoreException
	 */
	public static boolean checkDirtyEditorsAndUserMarkers(IEditorPart part) throws CoreException{
		//if exist markers in dirty editors
		if(Unmark.removeAllMarkersOfDirtyEditors()){
			MessageDialog.openInformation(
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					com.slicerplugin.generic.Constants.pluginName,
					"Dirty editors cannot have markers");
			Mark.cleanUserMarkers();
			return true;
		}
		
		//if the user has selected no line
		if(!Mark.existMarkers()){
			
			IDocumentProvider idp = ((ITextEditor)part).getDocumentProvider();
			IDocument document = idp.getDocument(part.getEditorInput());
			
			Unmark.removeAllMarkers(document);
			
			
			MessageDialog.openInformation(
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					com.slicerplugin.generic.Constants.pluginName,
					"No user marker found. What are you slicing?");
			return true;
		}
		
		return false;
	}
	
	
	/**
	 * Method called to slice
	 * @throws CoreException
	 */
	public static void slice() throws CoreException{
		IEditorPart part;

		part =
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		
		
		
		if(!Mark.projectPath.equals(EclipseApi.getProjectPath())){
			MessageDialog.openInformation(
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					com.slicerplugin.generic.Constants.pluginName,
					"You can't slice this file while you have markers in another project!");
			return;
		}
		
		
		
		
		 
		if(part instanceof ITextEditor){

			
			if(checkDirtyEditorsAndUserMarkers(part)) return;
			  
			  
			//if not in sliced mode, compute a slice
			if(!getSlicedMode()){
			  
				//if a previous job is already slicing, does not schedule another
				if(slicing)
					return;
			  
				Job job = new Job("Slicing") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						slicing=true;
						
						//put markers for the lines to be selected
						try {
							/*ITextEditor editor = (ITextEditor)part;
							IDocumentProvider provider = editor.getDocumentProvider();
							IDocument doc = provider.getDocument(editor.getEditorInput());
							IEditorInput input=part.getEditorInput();
							
							
							IResource resource=(IResource)input.getAdapter(IFile.class);
							if (resource == null) {
								resource=(IResource)input.getAdapter(IResource.class);
							}
							//get the full path
							//fullPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
							//fullPath += resource.getFullPath().toString();
							
							IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
						    IResource res = root.findMember(resource.getFullPath());
						    fullPath=res.getLocation().toString();
						    
							//get the lines marked from the user
							Set<Integer> linesMarked = Mark.getAllUserSingleLinesMarkedNormalized(part);
						  
							//compute the slice
							LanguageFactory.init("java", fullPath);
							Slice slice = new Slice(linesMarked, LanguageFactory.getRoot());
							*/
							/*Get the complementary set of lines to hold from the lines to delete*/
							/*HashSet<Integer> toHold=new HashSet<Integer>();
							List<Integer> toDelete=new ArrayList<Integer>();
							toDelete.addAll(slice.getLinesToDelete());
							Collections.sort(toDelete);
							for(int i=0;i<toDelete.size();i++) toDelete.set(i,toDelete.get(i)-1);
							int j=0,nLines=doc.getNumberOfLines();
							for(int i=0;i<nLines;i++){
								if(j<toDelete.size() && i==toDelete.get(j))
									j++;
								else
									toHold.add(i);
							}
							//save the lines to hold, that will be used when going in slicing mode
							toHoldList=Mark.getBlocksFromLines(toHold, true);
							Mark.addSetOfLines(slice.getSelectedLines(), false,part);
							*/
							
							com.slicerplugin.generic.Timer t=new com.slicerplugin.generic.Timer(); t.start();
							com.slicerplugin.slicer.parser.Slice sl=new  com.slicerplugin.slicer.parser.Slice(Mark.getMarkedOffsets(),Mark.projectPath);
							t.stopAndPrint("Time to all slicing: ");
							
							Display.getDefault().asyncExec(new Runnable() {
								public void run(){
									try {
										Unmark.removeAllMarkers();
										HashMap<String,ArrayList<Pair<Integer,Integer>>> slice=sl.getMarkedClone();//sl.getSliceClone();
										Mark.markSlice(slice);
									} catch (Exception e){
										MessageDialog.openInformation(
												PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
								        		"Error in marking slice",
								        		e.getMessage());
									}
								}
							});
							
							//toHoldList=Mark.getBlocksFromLines(sl.getSlice(), false);
							//Mark.addSetOfLines(sl.getSlice(), false, 
							//		(IEditorPart)part);
							
							  
						} catch (Exception e){
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									MessageDialog.openInformation(
											PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
							        		"Error in slicing",
							        		e.getMessage());
									e.printStackTrace();
							      }
							    });
						}
						finally{
							  slicing=false;
						}
				      
				    return Status.OK_STATUS;
					}
				};
				job.setPriority(Job.SHORT);
				// Start the Job
				job.schedule(); 
		  }
		  else{
			  MessageDialog.openInformation(
	  					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
	  					"Error",
	  					"You can't slice again if you are in sliced mode!");

		  }	
		}
	}
	
	
	/**
	 * Method not actually used
	 * @param userMarkedLines
	 */
	@SuppressWarnings({ "unused", "unchecked" })
	private static void addUserMarkersToLinesToHold(Set<Integer> userMarkedLines){
		ArrayList<Pair<Integer,Integer>> userBlocks=Mark.getBlocksFromLines(userMarkedLines, true);
		
		
		for(int i=0;i<userBlocks.size();i++){
			for(int j=0;j<toHoldList.size();j++){
				if(userBlocks.get(i).getFirst()>=toHoldList.get(j).getFirst() &&
						userBlocks.get(i).getSecond()<=toHoldList.get(j).getSecond())
					break;
				if(toHoldList.get(j).getFirst()>userBlocks.get(i).getFirst()){
					toHoldList.add(new Pair<Integer,Integer>(userBlocks.get(i).getFirst(),userBlocks.get(i).getSecond()));
				}
			}
		}
		
		Collections.sort(toHoldList);
	}
	
	/**
	 * 
	 * Function called to go in sliced mode.
	 * A DocumentListenerTracker is added to track the changes and maintain associations between row.
	 * A support from the selection listener is needed to avoid changes that involved more that a marker per time
	 * (no way to handle a substitution of more than a marker in an instant for logic reasons)
	 * 
	 * @param doc the doc to slice
	 * @throws CoreException thrown if fatal internal error occurred
	 * @throws BadLocationException thrown if a location that is not in the document is referenced
	 */
	public static void goSliceMode(IDocument doc) throws CoreException, BadLocationException{
		
		
		IWorkbenchPage iwbp=PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorPart part=iwbp.getActiveEditor();
		IEditorInput input=part.getEditorInput();
		IResource resource=(IResource)input.getAdapter(IFile.class);
		if (resource == null) {
			resource=(IResource)input.getAdapter(IResource.class);
		}
		
		//if not in sliced mode, go in sliced mode, otherwise, come back from it.
		if(!slicedMode){
			
			//backup original text
			originalText=new String(doc.get());
			
			
			//WORK IN PROGRESS
			//add the user marked lines to the lines to hold. This to allow a user to manually mark a line and have in the slice
			//addUserMarkersToLinesToHold(Mark.getAllUserSingleLinesMarked(part));
			
			if(checkDirtyEditorsAndUserMarkers(part)) return;
			
			//the toHoldList is already ordered
			if(toHoldList==null){
				MessageDialog.openInformation(
      					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
      					"No slice found",
      					"You can't go in sliced mode if you don't run the slicer before");
            	return;
			}
			
			//push the current state into the stack of the history of states of the code
			BackwardsHandler.addState(originalText);
			
			//get the slice of the code
			String text=sliceCode(doc);
			
			
			//remove all markers
			IMarker[] markers=Mark.getAllMarkers2(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor());
			for(int i=0;i<markers.length;i++)
				Unmark.removeMarkerAndAnnotation(markers[i],PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor());
			
			
			
			iwbp.closeEditor(part, false);
			IEditorPart newEditor=iwbp.openEditor(input, "com.slicerplugin.editors.slicedModeEditor");
			
			ITextEditor editor=(ITextEditor)newEditor;
			IDocumentProvider provider = editor.getDocumentProvider();
		    IDocument document = provider.getDocument(editor.getEditorInput());
		    
			//set the slice as document content
			document.set(text);
			
			//add the listener to prevent invalid code alterations
			document.addDocumentListener(listener);
			DocumentTrackerListener.stop=false;
			
			
		    //adding a listener to the workbench page to catch events like editors opened and closed.
			//in this way, I can catch if a user close the sliced mode editor and I can handle that case.
			IPartListener2 pl = new IPartListener2() {
			        // ... Other methods
			        public void partClosed(IWorkbenchPartReference partRef)
			        {
			            if(partRef.getId().equals("com.slicerplugin.editors.slicedModeEditor")){
			            	slicedMode=false;
			            	blocks=null;
			    			toHoldList=null;
		            	}
			        }

					@Override
					public void partActivated(IWorkbenchPartReference partRef) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void partBroughtToTop(IWorkbenchPartReference partRef) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void partDeactivated(IWorkbenchPartReference partRef) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void partOpened(IWorkbenchPartReference partRef) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void partHidden(IWorkbenchPartReference partRef) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void partVisible(IWorkbenchPartReference partRef) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void partInputChanged(IWorkbenchPartReference partRef) {
						// TODO Auto-generated method stub
						
					}

			};
			iwbp.addPartListener(pl);
			
			
			slicedMode=true;
		}
		else{
			
			
			doc.removeDocumentListener(listener);
			DocumentTrackerListener.stop=true;
			
			String patchedCode=unslice();
			
			doc.set(patchedCode);
			
			iwbp.closeEditor(part, false);
			
			IEditorDescriptor desc = PlatformUI.getWorkbench().
			        getEditorRegistry().getDefaultEditor(resource.getName());
			
			iwbp.openEditor(input, desc.getId());
			part=iwbp.getActiveEditor();
			
			IDocumentProvider provider = ((ITextEditor) part).getDocumentProvider();
		    IDocument document = provider.getDocument(part.getEditorInput());
		    
			document.set(patchedCode);
			
			blocks=null;
			toHoldList=null;
			
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().doSave(null);
			
			slicedMode=false;
		}
		
		
	}
	
	/**
	 * Function that extract a slice from a document. It is used inside the main slice method.
	 * In brief this function create the new sliced document, and create blocks structure to track the changes
	 * @param doc
	 * @return
	 * @throws BadLocationException
	 */
	private static String sliceCode(IDocument doc) throws BadLocationException {
		
		String text="";
		
		int row=0, tabsIndex=0;
		
		blocks=new ArrayList<Block>();
		
		//check if we are on windows or linux/unix
		boolean onWindows=true;
		String filler="\r\n";
		String[] docText=doc.get().split(filler);
		if(docText.length==1){
			onWindows=false;
			filler="\n";
			docText=doc.get().split(filler);
		}
		
		
		/*The code below gather the needed lines and build the blocks that represent the structure of the slice*/
		
		//if the first marker starts not from the beginning, there's a gap before
		if(toHoldList.get(0).getFirst()!=0){
			
			//before to put a gap, compute which is the offset, in terms of indentation in the first line marked. just to have a nicer layout
			int nTabs=countTabs(docText[toHoldList.get(0).getFirst()]);
			for(int i=0;i<nTabs;i++)
				text+="\t";
			
			//put a block also for the gaps
			blocks.add(
					new Block(
					(int)0,
					(int)toHoldList.get(0).getFirst()-1,
					row,
					row,
					false));
			
			text+=com.slicerplugin.generic.Constants.gapText+filler;
			row++;
			
			
		}
		
		int docLength=doc.getNumberOfLines();
		//for each block of code that I have to take, 
		//take the code and put it in the new slice, and build a proper block in blocks list
		//also gaps have their own block. 
		for(int i=0;i<toHoldList.size();i++){
			
			for(int j=toHoldList.get(i).getFirst();j<=toHoldList.get(i).getSecond() && j<docText.length;j++){
				text+=docText[j]+filler;
			}
			
			
			int nextRow=(row+toHoldList.get(i).getSecond()-toHoldList.get(i).getFirst());
			
			blocks.add(
					new Block(
					(int)toHoldList.get(i).getFirst(),
					(int)toHoldList.get(i).getSecond(),
					row,
					nextRow,
					true));
			
			row=nextRow+1;
			
			if(docLength-1!=(int)toHoldList.get(i).getSecond()){
				//count the nuber of tabs(indentation) in the first row of the gap
				tabsIndex=countTabs(docText[toHoldList.get(i).getSecond()+1]);
				
				for(int j=0;j<tabsIndex;j++) text+="\t";
				
				//Add the indented preferred text
				text+=com.slicerplugin.generic.Constants.gapText+filler;
				
				if(i+1<toHoldList.size()){
					blocks.add(
							new Block(
							toHoldList.get(i).getSecond()+1,
							toHoldList.get(i+1).getFirst()-1,
							row,
							row,
							false));
				}
				else{
					blocks.add(
							new Block(
							toHoldList.get(i).getSecond()+1,
							doc.getNumberOfLines()-1,
							row,
							row,
							false));
				}
				
				row++;
				
				
			}
			
			//text+=associations.get(i).getFirst()+" "+associations.get(i).getSecond()+"\n";
		}
		//remove the last line
		if(onWindows && text.charAt(text.length()-1)=='\n' && text.charAt(text.length()-2)=='\r')
			text=text.substring(0, text.length()-2);
		else if(!onWindows && text.charAt(text.length()-1)=='\n')
			text=text.substring(0,text.length()-1);
		
		return text;
		
	}

	/**
	 * Unslice is called to return back from the sliced mode to the normal mode
	 * @return the orignal code patched with the modifications applied in sliced mode.
	 * @throws BadLocationException 
	 */
	private static String unslice() throws BadLocationException{
		
		Mark.cleanUserMarkers();
		
		ITextEditor editor = (ITextEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
	    IDocumentProvider provider = editor.getDocumentProvider();
	    IDocument document = provider.getDocument(editor.getEditorInput());
	    
	    
	    //Are we on windows or linux/unix?
	    String filling="\r\n";
	    
		String newText=document.get();
		String[] oldPieces=originalText.split(filling);
		if(oldPieces.length==1) {
			filling="\n";
			oldPieces=originalText.split(filling); //fix for non windows environments
		}
		String[] newPieces=newText.split(filling);
		newText="";
		
		
		//cycle through all the blocks and get the correct pieces of code
		int i,j=0;
		while(j<blocks.size()){
			
			//if the current block is a block of code, take the new code from the document and fill the patched result
			if(blocks.get(j).block){
				i=blocks.get(j).slicedStartRow;
				while(i<newPieces.length && i<=blocks.get(j).slicedEndRow){
					newText+=newPieces[i++]+filling;
				}
			}
			else{
				//else, we are in a code gap
				i=blocks.get(j).originalStartRow;
				//get offset and length of the gap to get the content
				int startOffset=document.getLineOffset(blocks.get(j).slicedStartRow),length=document.getLineOffset(blocks.get(j).slicedEndRow);
				length+=document.getLineLength(blocks.get(j).slicedEndRow); length-=startOffset;
				String content=document.get(startOffset, length );
				
				//get the index of the symbolic code gap
				int index=content.indexOf(com.slicerplugin.generic.Constants.gapText);
				int endIndex=index+com.slicerplugin.generic.Constants.gapText.length();
				
				//if the content of the code gap contains a valid symbol of code gap, we can fill with the old code.
				if(index!=-1){
					
					while(index-1>=0 && content.charAt(index-1)=='\t')
						index--;
					if(index>=0)
						newText+=content.substring(0, index);
					
					//FILL WITH THE OLD CODE. the code back and ahead is just there to handle the rest of the content of the code gap in the sliced file
					while(i<oldPieces.length && i<=blocks.get(j).originalEndRow){
						newText+=oldPieces[i++]+filling;
					}
					
					if(content.charAt(endIndex)=='\n')
						endIndex++;
					else if(endIndex+1<content.length() && content.charAt(endIndex)=='\r' && content.charAt(endIndex+1)=='\n')
						endIndex+=2;
					String secondPart=content.substring(endIndex);
					if(newText.length()>0){
						newText+=secondPart;
						if(j==blocks.size()-1)
							newText+=filling;
					}
				}
				else{
					//else, take the new content
					newText+=content;
				}
			}
			j++;
			
		}
		if(filling.equals("\n"))
			newText=newText.substring(0, newText.length()-1);
		else
			newText=newText.substring(0, newText.length()-2);
		return newText;
	}
	

	/**
	 * 
	 * @return True if you are in sliced mode, false otherwise
	 */
	public static boolean getSlicedMode(){
		return slicedMode;
	}
	
	
	//public static void setSlicedMode(boolean slicedMode_){
	//	slicedMode=slicedMode_;
	//}
	
	/**
	 * The function of the reverse button
	 * @throws PartInitException 
	 */
	public static void reverse() throws PartInitException{
		if(!slicedMode){
			MessageDialog.openInformation(
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					com.slicerplugin.generic.Constants.pluginName,
					"You cannot restore if you are not in sliced mode!");
			return;
		}
		
		
		IWorkbenchPage iwbp=PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorPart part=iwbp.getActiveEditor();
		ITextEditor editor = (ITextEditor)part;
	    IDocumentProvider provider = editor.getDocumentProvider();
	    IDocument doc = provider.getDocument(editor.getEditorInput());
	    IEditorInput input=part.getEditorInput();
		
	    IResource resource=(IResource)input.getAdapter(IFile.class);
		if (resource == null) {
			resource=(IResource)input.getAdapter(IResource.class);
		}
		
	    DocumentTrackerListener.stop=true;
	    doc.removeDocumentListener(listener);
	    
	    iwbp.closeEditor(part, false);
		
		IEditorDescriptor desc = PlatformUI.getWorkbench().
		        getEditorRegistry().getDefaultEditor(resource.getName());
		
		
		iwbp.openEditor(input, desc.getId());
		
		part=iwbp.getActiveEditor();
		
		provider = ((ITextEditor) part).getDocumentProvider();
	    IDocument document = provider.getDocument(part.getEditorInput());
	    
		document.set(originalText);
		
	    
	    blocks=null;
		toHoldList=null;
		
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().doSave(null);
		
		
		slicedMode=false;
		
		
		//pop the previous state saved because modifications haven't got success
		BackwardsHandler.popState();
		
		//clean the user markers, to reset the markers to the initial state
		Mark.cleanUserMarkers();
	}
	
	
	/**
	 * Function that check if the selection is contained in a marker
	 * @param startLine start line of the selection
	 * @param endLine end line of the selection
	 * @return return -1 if is contained, else return a row to which set the selection
	 */
	public static int containedInMarker(int startLine, int endLine){
		
		if(blocks==null)
			return -1;
		
		//if contained in marker return -1
		for(int i=0;i<blocks.size();i++){
			if(startLine>=blocks.get(i).slicedStartRow &&
					endLine<=blocks.get(i).slicedEndRow){
				return -1;
			}
		}
		
		//if the start line is greater than the end of the marker, return the last row of the marker
		for(int i=0;i<blocks.size();i++){
			if(startLine>blocks.get(i).slicedEndRow)
				return blocks.get(i).slicedEndRow;
		}
		
		//else, return the start line of the last marker
		return blocks.get(
				blocks.size()-1
				).slicedStartRow;
	}
	
}

