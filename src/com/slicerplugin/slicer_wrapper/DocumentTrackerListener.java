package com.slicerplugin.slicer_wrapper;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * Keeps track of document change in sliced mode.
 * blocks structure is updated on each change.
 * @author Tomas
 *
 */
public class DocumentTrackerListener implements IDocumentListener {
	
	
	public static volatile boolean stop=false;
	
	/**
	 * Indicates whether a block is being deleted
	 */
	public static volatile boolean flag=false;
	
	/**
	 * Strings used to detect which block has been deleted
	 */
	//String lineBefore,lineAfter;
	
	/**
	 * Function that count the new line numbers in a string; recognize unix and windows based sources
	 * @param source
	 * @return
	 */
	private int countNewLine(String source){
		int count=0;
		int i=0;
		while(i<source.length()){
			if(source.charAt(i)=='\n')
				count++;
			i++;
		}
		return count;
	}
	
	
	
	
	/**
	 * Event that track user changes on a document. The event as parameter specify the selected part of the code and the input provide
	 * from the user. This function track the changes by updating the position of the blocks of code inside the SLICE.
	 * This event is called only when working in sliced mode. Otherwise we need no tracking.
	 * We have discovered that other plugin may interfere with the correctly launch of this event
	 * one of this plugins is (in this case the VIM plugin) that allow the user to interact
	 * with shortcut on the keyboard for functions like deleting a full row or similar, give problems in firing this event on the change.
	 * 
	 */
	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {
		
		//if doesn't have to run because locked by the selection listener (invalid selection?)
		if(stop)
			return;
		
		
		//get the line interested
		int beginOfLinesInterested,endOfLinesInterested;
		try {
			beginOfLinesInterested = event.getDocument().getLineOfOffset(event.fOffset);
			endOfLinesInterested = event.getDocument().getLineOfOffset(event.fOffset+event.fLength);
		} catch (BadLocationException e) {
			e.printStackTrace();
			return;
		}
		
		//Count the lines added and the lines subtracted
		int linesAdded=countNewLine(event.fText);

		ITextEditor editor =  (ITextEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
	    IDocumentProvider provider = editor.getDocumentProvider();	
	    IDocument document = provider.getDocument(editor.getEditorInput());
		int linesRemoved=countNewLine(document.get().substring(event.fOffset,event.fOffset+event.fLength));
		
		int linesOffset=linesAdded-linesRemoved,i; //compute the lines offset
		
		
		/*Here the code track the changes, first the algorithm find which is the block
		 *that contain the current selection that is being overwritten from the coming data
		 *If modifications are in a code gap, the case is handled below.
		 */
		for(i=0;i<SlicerWrapper.blocks.size();i++){
			if(SlicerWrapper.blocks.get(i).slicedStartRow<=beginOfLinesInterested
					&& SlicerWrapper.blocks.get(i).slicedEndRow>=endOfLinesInterested)
				break;
			if(SlicerWrapper.blocks.get(i).slicedStartRow<=beginOfLinesInterested
					&& SlicerWrapper.blocks.get(i).slicedEndRow+1>=endOfLinesInterested && 
					-linesOffset==SlicerWrapper.blocks.get(i).slicedEndRow-SlicerWrapper.blocks.get(i).slicedStartRow+1){
				/*lineBefore=event.fDocument.get().split("\n")[beginOfLinesInterested];
				try{
					lineAfter=event.fDocument.get().split("\n")[endOfLinesInterested];
				}
				catch(Exception e){lineAfter="";}*/
				flag=true;
				return;
			}
			if(SlicerWrapper.blocks.get(i).slicedStartRow-1<=beginOfLinesInterested
					&& SlicerWrapper.blocks.get(i).slicedEndRow>=endOfLinesInterested && 
					-linesOffset==SlicerWrapper.blocks.get(i).slicedEndRow-SlicerWrapper.blocks.get(i).slicedStartRow+1){
				/*lineBefore=event.fDocument.get().split("\n")[beginOfLinesInterested];
				try{
					lineAfter=event.fDocument.get().split("\n")[endOfLinesInterested];
				}
				catch(Exception e){lineAfter="";}*/
				flag=true;
				return;
			}
		}
		
		//if the line is contained, update the starting and end rows of the first marker
		//WARNING:
		//The first block of code affected can be totally deleted, 
		//or partially deleted and starting and end lines need a special procedure
		if(i<SlicerWrapper.blocks.size()){
			
			//in the case in which the block has been destroyed,
			if(-linesOffset==SlicerWrapper.blocks.get(i).slicedEndRow-SlicerWrapper.blocks.get(i).slicedStartRow+1){
				//Slicer.associations.get(i).deleted=true;
				SlicerWrapper.blocks.remove(i);
				//linesOffset--;
			}
			else{
				try {
					//if the user is adding a new line at the end of a code gap, the line is added to the block below
					if(linesOffset>0 && i+1<SlicerWrapper.blocks.size() && !SlicerWrapper.blocks.get(i).block
							&& document.getLineOffset(SlicerWrapper.blocks.get(i+1).slicedStartRow)-1==event.getOffset())
						i++;
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
				//augment the offset of the end line of the block that contain the modification
				SlicerWrapper.blocks.get(i).slicedEndRow=(SlicerWrapper.blocks.get(i).slicedEndRow+linesOffset);
				i++;
			}
		}
		//if in a code gap, don't increment the end line of the marker that stay behind, only after.
		//We need to restart the scan of markers because before I was searching for a modified inside a block of code, not in a gap
		else{
			for(i=0;i<SlicerWrapper.blocks.size();i++)
				if(SlicerWrapper.blocks.get(i).slicedStartRow>beginOfLinesInterested)
					break;
		}
		
		//cycle all the remaining markers, the position of each must be aligned with the new offset.
		//compute offsets for each marker till the end
		for(;i<SlicerWrapper.blocks.size();i++){
			SlicerWrapper.blocks.get(i).slicedStartRow=(SlicerWrapper.blocks.get(i).slicedStartRow+linesOffset);
			SlicerWrapper.blocks.get(i).slicedEndRow=(SlicerWrapper.blocks.get(i).slicedEndRow+linesOffset);
			
		}
		
	}

	/**
	 * Called after a change on a document. The flag is true only when a block has been deleted.
	 */
	@Override
	public void documentChanged(DocumentEvent event) {
		
		if(flag){
			flag=false;
			//int beginOfLinesInterested=-1;
			int endOfLinesInterested=-1;
			try{
				//get the lines interested. Lines will be the previous of the deleted and the deleted lines
				//or the lines deleted and the one after. This depends from the user input, if it is a CANC or a BACKSPACE
				//beginOfLinesInterested = event.getDocument().getLineOfOffset(event.fOffset);
				endOfLinesInterested = event.getDocument().getLineOfOffset(event.fOffset+event.fLength);
				
				int i;
				//Find the block that must be deleted
				for(i=0;i<SlicerWrapper.blocks.size();i++){
					if(SlicerWrapper.blocks.get(i).slicedStartRow<=endOfLinesInterested &&
							SlicerWrapper.blocks.get(i).slicedEndRow>=endOfLinesInterested){
						if(SlicerWrapper.blocks.get(i).slicedStartRow==endOfLinesInterested &&
							SlicerWrapper.blocks.get(i).slicedEndRow==endOfLinesInterested)
							SlicerWrapper.blocks.remove(i);
						else{
							SlicerWrapper.blocks.get(i++).slicedEndRow--;
						}
						break;
					}
				}
				//For all the other blocks, update their lines starting and ending.
				for(;i<SlicerWrapper.blocks.size();i++){
					SlicerWrapper.blocks.get(i).slicedStartRow=(SlicerWrapper.blocks.get(i).slicedStartRow-1);
					SlicerWrapper.blocks.get(i).slicedEndRow=(SlicerWrapper.blocks.get(i).slicedEndRow-1);
					
				}
				
			}
			catch(Exception e){
				//If an exception is thrown, the block deleted is the last one, update the list!
				SlicerWrapper.blocks.remove(SlicerWrapper.blocks.size()-1);
			}
			
			
		}
	}

}
