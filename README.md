# java-slicer
A slicer for the Java language - embedded as an eclipse plugin. PROJECT IS INCOMPLETE

## What is it?
This is a partial slicer for java, deployed as an eclipse plug-in.
It allows you to select one or more statements (slicing criterion) and run the slicer to see the statements in the program that modifies it/them.
It's a forward and backward slicer. Substantially all the influent variables are followed in modifications and control statements are preserved etc.. check documentation for more info

    Copyright (C) 2015-2016  Tomas Bortoli

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
    Additional permission under GNU GPL version 3 section 7
    
    If you modify this Program, or any covered work, by linking or combining 
    it with Java Development Tools (or a modified version of that library), 
    containing parts covered by the terms of Eclipse Public License 1.0, the 
    licensors of this Program grant you additional permission to convey the resulting work.



## Demo
To setup a demo, install a version of Eclipse RCP (tested successfully with Eclipse Luna, Mars and Neon).
Clone the repo and import it in Eclipse: File->Import->Plug-in Development-> Plug-in and Fragments
Good! Now run it (run in debug mode by pressing F11). If it tells you no running configuration, go on the root project folder, right click and run as -> Eclipse Application. 

When you successfully run the Plug-in you'll get another Eclipse instance. Create a new java project in it, create a new Main class and write the example code you want to slice (you can also import an already existing project).
To select the slicing criterion, navigate to the statement you want with the keyboard cursos (not the mouse cursor) and press CTRL+5 to put it in the slicing criterion (it will highlight in green). You can add as many statements as you like.
You can also mark them by pointing the keyboard cursor to the statement (also selecting the code works) and right click of the mouse and Mark. Remove selected statements with CTRL+4 or mouse right click->Unmark
To slice press CTRL+6, or use the right button on toolbar. After that you should see all the statements that affect the previously selected statements highlighted. That's the slice.

## Development
For the moment I'm not really working on it but I would be happy to see it finished.
There's a lot of work to do, also it would be nice to show the slice by setting **transparency (like 75-80%) to statements not in the slice**, instead of coloring the slice in green. Sadly, with eclipse Luna it was impossible to do this graphics tricks...not sure about the new Plug-in APIs in Neon..




Enjoy!
