/**
 * AmbientTalk/2 Project
 * (c) Software Languages Lab, 2006 - 2011
 * Authors: Software Languages Lab - Ambient Group
 * 
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package edu.vub.at.weScribble.interfaces;

/** 
 * Interface that the Java object implements with the methods that AmbientTalk objects call on it. 
 */
public interface JWeScribble {
	
	/**
	 * Registers an AmbientTalk application to listen for GUI events
	 * which trigger the method calls declared in ATWeScribble.
	 */
	public JWeScribble registerATApp(ATWeScribble weScribble);
	
	// Methods to manipulate canvas.
	public void resetCanvas();
	public void redrawCanvas();
	public int getColor();
	
	public JWeScribblePath getMyPath();
	
	// Methods to manipulate paths not belonging to remote users
	/**
	 * @param color denotes the color of the foreign WeScribblePath
	 * @return a new WeScribblePath for a remote peer initialized at (0,0).
	 */
	public JWeScribblePath createForeignPath(int color);
	
	/**
	 * Adds the given path to the canvas.
	 */
	public void addForeignPath(JWeScribblePath path);
	
	/**
	 * Signals the end of the given path, and displays it in the canvas.
	 */
	public void endForeignPath(JWeScribblePath path);
	
	// Methods to manipulate colors of remote users.
	public void grayOut(JWeScribblePath path);
	public void recolor(JWeScribblePath path);
}
