package com.slicerplugin.slicer_wrapper;

/**
 * Class that represent an association between a piece of code in the original file and in the sliced file.
 * This object is used to keep track of the changes and in the end put the rows of code in the correct position.
 * @author Tomas
 *
 */
public class Block {
	
	public int originalStartRow,originalEndRow,slicedStartRow,slicedEndRow;
	boolean block; //this flag show when the object is a block or a gap
	
	public Block(int originalStartRow, int originalEndRow, int slicedStartRow, int slicedEndRow, boolean block){
		this.originalStartRow=originalStartRow;
		this.originalEndRow=originalEndRow;
		this.slicedStartRow=slicedStartRow;
		this.slicedEndRow=slicedEndRow;
		this.block=block;
	}
}
