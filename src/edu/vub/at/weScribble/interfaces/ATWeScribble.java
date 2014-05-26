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

import edu.vub.at.objects.coercion.Async;

/** 
 * Interface that the AmbientTalk object needs to implement so that Java objects talk to it. 
 */
public interface ATWeScribble {
	// Method called when the user touches down on the canvas.
	@Async
	void touchStart(float x, float y);
	// Method called when the user moves his finger on the canvas (after a touch start event).
	@Async
	void touchMove(Vector<Float>obj);
	// Method called when the user touches up on the canvas.
	@Async
	void touchEnd(float x, float y);
	// Method called when the user resets the canvas using the options menu.
	void reset();
	// Inform AT of our new GUI object.
	void handshake(JWeScribble gui);
	
	// UNCOMMENT THESE METHODS WHEN YOU DO SECTION C)
	void disconnect();
	void reconnect();
	
}
