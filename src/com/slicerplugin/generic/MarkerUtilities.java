package com.slicerplugin.generic;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.slicerplugin.slicer.parser.GetSelectedStmt;
import com.slicerplugin.slicer.parser.Slice;
import com.slicerplugin.slicer_wrapper.SlicerWrapper;




/**
 * Utilities class to facilitate the managing of eclipse SDK api
 * 
 * @author Tomas
 *
 */
public class MarkerUtilities {
	
	
	
	
	public static Pair<Integer,Integer> getSelectionStatementOffset(Pair<Integer,Integer> s) throws JavaModelException{
		
		ITextEditor editor = (ITextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        ITextSelection sel  = (ITextSelection) editor.getSelectionProvider().getSelection();
        ITypeRoot typeRoot = JavaUI.getEditorInputTypeRoot(editor.getEditorInput());
        ICompilationUnit icu = (ICompilationUnit) typeRoot.getAdapter(ICompilationUnit.class);
        CompilationUnit cu = Slice.light_parse(icu);
        IDocumentProvider idp = editor.getDocumentProvider();
		IDocument document = idp.getDocument(editor.getEditorInput());
        GetSelectedStmt v=new GetSelectedStmt(s,document.get());
		cu.accept(v);
		return v.stmt_offset;
	}
	
	/**
	 * Get all the markers from all editors
	 * @return All markers
	 * @throws CoreException
	 */
	public static ArrayList<IMarker> getAllMarkers() throws CoreException{
		ArrayList<IMarker> ret=new ArrayList<IMarker>();
		IEditorReference[] editors=PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
		
		for(int i=0;i<editors.length;i++){
			addMarkers(editors[i].getEditor(false),ret);
		}
		return ret;
	}
	
	public static ArrayList<IMarker> getAllMarkersOfCurrentEditor() throws CoreException{
		IEditorPart editor=getCurrentEditor();
		ArrayList<IMarker> ret=new ArrayList<IMarker>();
		addMarkers(editor,ret);
		return ret;
	}
	
	private static void addMarkers(IEditorPart editor, ArrayList<IMarker> ret) throws CoreException{
		IResource resource=(IResource)editor.getEditorInput().getAdapter(IFile.class);
		IMarker[] ms=resource.findMarkers("slicerPlugin.myMarker", false, 0);
		for(int j=0;j<ms.length;j++)
			ret.add(ms[j]);
	}
	
	/**
	 * Return all markers of an editor
	 * @param editor
	 * @return All markers of editor
	 * @throws CoreException
	 */
	public static IMarker[] getAllMarkers(IEditorPart editor) throws CoreException{
		IResource resource=(IResource)editor.getEditorInput().getAdapter(IFile.class);
		IMarker[] ms=resource.findMarkers("slicerPlugin.myMarker", false, 0);
		return ms;
	}
	
	
	
	/**
	 * Make a print of an array list of markers
	 * @param markers
	 * @param shell current shell
	 * @throws CoreException
	 */
	public static void printMarkerInfo(ArrayList<IMarker> markers, Shell shell) throws CoreException{
		
	       String s="";
	       for(int j=0;j<markers.size();j++){
	    	   s+=markers.get(j).getAttribute(IMarker.LINE_NUMBER)+" "+markers.get(j).getAttribute(Constants.markerAttributeEndLine)+" "+
	    	   markers.get(j).getAttribute(Constants.markerAttributeFile)+"\n";
	       }
	       MessageDialog.openInformation(
					shell,
					Constants.pluginName,
					s);
	    
	}
	
	/**
	 * Get the position from a marker
	 * @param marker
	 * @param document
	 * @return the position of the marker
	 * @throws BadLocationException thrown if a reference to an undefined location is done
	 * @throws CoreException
	 */
	public static Position getPositionFromMarker(IMarker marker, IDocument document) throws BadLocationException, CoreException{
		
		int start=(int)marker.getAttribute(IMarker.CHAR_START);
		int length=((int)marker.getAttribute(IMarker.CHAR_END))-start;
		return new Position(start,length);
	}
	
	/**
	 * 
	 * @return the current text selection
	 */
	public static TextSelection getCurrentTextSelection(){
		IEditorPart part;

		part =
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		
		ITextEditor editor = (ITextEditor)part;
		
	    ISelection sel = editor.getSelectionProvider().getSelection();
	    
	    TextSelection ts=((TextSelection)sel);
	    
	    return ts;
	}
	
	/**
	 * 
	 * @return the current resource
	 */
	public static IResource getCurrentResource(){
		IEditorPart part;

		part =
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		IEditorInput input=part.getEditorInput();
		IResource resource=(IResource)input.getAdapter(IFile.class);
		
		return resource;
	}
	
	public static IEditorPart getCurrentEditor(){
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
	}
}
