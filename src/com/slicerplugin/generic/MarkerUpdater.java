package com.slicerplugin.generic;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.texteditor.IMarkerUpdater;

import com.slicerplugin.popup.actions.Unmark;

public class MarkerUpdater implements IMarkerUpdater {
    /*
    *Returns the attributes for which this updater is responsible.
    *If the result is null, the updater assumes responsibility for any attributes.
    */
    @Override
    public String[] getAttribute() {
         return null;
    }

    @Override
    public String getMarkerType() {
          //returns the marker type that we are interested in updating
         return "slicerPlugin.myMarker";
    }
    
    
    /**
     * Called when an editor window save the file.
     * Remove all the markers of the current opened editor that is going to be saved (writing file on non volatile memory)
     */
    @Override
    public boolean updateMarker(IMarker marker, IDocument doc, Position position) {
          try {
        	  
        	  Unmark.removeAllMarkers(doc);
        	  
        	  return true;
        	  /*int start=doc.getLineOfOffset(position.getOffset()+1);
        	  int end=doc.getLineOfOffset(position.getOffset()+1+
        			  (int)marker.getAttribute(Constants.markerAttributeLength));
                marker.setAttribute(IMarker.LINE_NUMBER, start);
                marker.setAttribute(Constants.markerAttributeEndLine, end);
                return true;*/
          } catch (CoreException e) {
                return false;
		}
    }
}