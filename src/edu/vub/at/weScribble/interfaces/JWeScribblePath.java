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

import java.util.Vector;

import edu.vub.at.weScribble.WeScribblePath;

public interface JWeScribblePath {
    public WeScribblePath asWeScribblePath();
    
    //helper methods for color
	public int getColor();
	public void setColor(int color);
	/**
	 * Add a line from the last point to the specified point (x,y).	
	 * Note that this method update the latest point stored in the WeScribblePath receiving instance to the given (x,y).
	 */
	public void lineTo(float x, float y);
	
	/** 
	 * Set the beginning of the next contour to the point (x,y).
	 * Note that this method update the latest point stored in the WeScribblePath receiving instance to the given (x,y).
	 */
	public void	moveTo(float x, float y);
	
	/**
	 *  Draw an offset of an entire line from the last point of the receiver till the last point stored in the given vector.
	 *  Note that this method update the latest point stored in the WeScribblePath receiving instance to the given (x,y).
	 *  @param scaledPoints a collection (x,y) points to draw. It should contain at least one point (x,y).
	 */
	public void doOffset(Vector<Float> scaledPoints);
	
	/**
	 *  Draw an entire line(s) with points in the given vector. 
	 *  Note that this method update the latest point stored in the WeScribblePath receiving instance to the given (x,y).
	 *  @param scaledPoints a collection (x,y) points to draw. It should contain at least 2 points (x1, y1, x2, y2)
	 */
	public void doPath(Vector<Float> scaledPoints);
	
	/**
	 * Rewind the path: clears any lines and curves from the path but keeps the internal data structure for faster reuse.
	 */
	public void rewind();	
	
	/**
	 * Clear any lines and curves from the path, making it empty. This does NOT change the fill-type setting.
	 */
	public void reset();
}