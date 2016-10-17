package com.slicerplugin.generic;

public class Timer {
	long startTime,stopTime;
	public Timer(){
		
	}
	public void start(){
		startTime = System.nanoTime();
	}
	public void stopAndPrint(String msg){
		stopTime = System.nanoTime();
		System.out.println(msg+((double)(stopTime - startTime)/1000000d)+"ms");
	}
}
