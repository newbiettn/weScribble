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
package edu.vub.at.weScribble;

import java.util.Vector;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import edu.vub.at.weScribble.interfaces.JWeScribblePath;

public class WeScribblePath extends Path implements JWeScribblePath{

	private Paint myPaint;
	
	// keeping color for grayOut/recolor methods.
	private int myColor;
	
	// keeping the latest (x,y) because Path doesn't provide getters for it.
	private float latestX;
	private float latestY;
	
	// We cannot access the points in a Path, so duplicate them here.
	private Vector<Float> myPoints;
	
	private double scaleX_;
	private double scaleY_;
	
	private boolean foreignPath; // TODO refactor remoteWeScribblePath / localWeScribblePath.
	private static float EOL = (float) -1;
	
	public WeScribblePath(Bitmap bitmap, int color) {
		super();
		myPaint = new Paint();
		myPaint.setAntiAlias(true);
		myPaint.setDither(true);
		myPaint.setColor(color);
		myPaint.setStyle(Paint.Style.STROKE);
		myPaint.setStrokeJoin(Paint.Join.ROUND);
		myPaint.setStrokeCap(Paint.Cap.ROUND);
		myPaint.setStrokeWidth(12);
		latestX = 0;
		latestY = 0;
        scaleX_ = 1200.0 / bitmap.getWidth();
		scaleY_ = 1200.0 / bitmap.getHeight();
		foreignPath = false;
		myPoints = new Vector<Float>();
		myColor = color;
	}
	
	public WeScribblePath( Bitmap bitmap, int color, boolean fp) {
		this(bitmap, color);
		foreignPath = fp;
	}
	
	private float scaleX(float in) { return (float) (in * scaleX_) ; }
	private float scaleY(float in) { return (float) (in * scaleY_); }
	private float unscaleX(float in ) { return (float) (in / scaleX_); }
	private float unscaleY(float in) { return (float) (in / scaleY_); }
	public float getScaledX() { return scaleX(latestX); }
	public float getScaledY() { return scaleY(latestY); }
	
	// helper getters and setter for color
	public int getColor() { return myPaint.getColor(); }
	public void setColor(int color) {
		myColor = color;
		myPaint.setColor(color);
	}
	
	public void grayOut() {
	  myPaint.setColor(Color.GRAY);
	}
	
	public void recolor() {
	  myPaint.setColor(myColor);
	}
	
	public Paint getPaint() { return myPaint; }
	public float getX() { return latestX; }
	public float getY() {return latestY; }
	public void setPoint(float x, float y) { 
		latestX = x; latestY = y;
		myPoints.add(x); myPoints.add(y);
	}
	
	public Vector<Float> getAllPoints() {
		if (myPoints.size() > 0) {
			Vector<Float> scaledPoints = new Vector<Float>(myPoints.size());
			int i = 0 ;
			while ( i < myPoints.size()) {
				float x = myPoints.get(i);
				if ( x == EOL) {
					if ( i!=0 ) {
						scaledPoints.add(EOL);
					}
					i = i + 1;
				} else{
					float y = myPoints.get(i+1);
					scaledPoints.add(scaleX(x));
					scaledPoints.add(scaleY(y));
					i = i + 2;
				}


			}
			return scaledPoints;
		}
		return null;
	}

	public void lineTo(float x, float y) {
		if (foreignPath) {
			x = unscaleX(x);
			y = unscaleY(y);
		}
		setPoint(x,y);
		super.lineTo(x, y);
	}

	public void	moveTo(float x, float y) {
		myPoints.add(EOL);
		if (foreignPath) {
			x = unscaleX(x);
			y = unscaleY(y);
		}
		setPoint(x,y);
		super.moveTo(x,y);
	}

	public void doOffset(Vector<Float> scaledPoints) {
		assert(foreignPath);
		float x = 0,y = 0;
		float oldX = getScaledX();
		float oldY = getScaledY();
		for (int i = 0 ; i < scaledPoints.size()/2; i++) {
			x = scaledPoints.get(i*2);
			y = scaledPoints.get(i*2+1);
			quadTo(unscaleX(oldX), unscaleY(oldY), unscaleX((x+oldX)/2), unscaleY((y+oldY)/2));
			oldX = x;
			oldY = y;
		}
		setPoint(x,y);
	}
		
	public void doPath(Vector<Float> scaledPoints){	
		if (scaledPoints.size() >= 2) {
			Vector<Float> offsetVector = new Vector<Float>();
			int i = 0;
			while( i < scaledPoints.size()) {
				float currentX = scaledPoints.get(i);
				float currentY = scaledPoints.get(i + 1);
				moveTo(currentX, currentY);
				// doOffset as long as not EOL
				i = i + 2;
				int j = i ; boolean found = false; float p = 0;
				while( !found && j < scaledPoints.size()) {
					p= scaledPoints.get(j);
					if (p == EOL) {
						found = true;
					} else {
						offsetVector.add(p);
					}
					i++; j++;
				}
				doOffset(offsetVector);
				// finish the line
				if (p == EOL || (p !=EOL && i == scaledPoints.size())) { 
					lineTo(offsetVector.get(offsetVector.size()-2), offsetVector.get(offsetVector.size()-1));
				}
				offsetVector.clear();
			}
		}
	}
    
	public void reset(){
	  myPoints.clear();
	  super.reset();
	}
	
	public WeScribblePath asWeScribblePath() {
		return this;
	}
}
