package edu.vub.at.weScribble;

import edu.vub.at.weScribble.interfaces.JWeScribbleShape;
import android.graphics.Paint;
import android.graphics.Rect;
public class WeScribbleShape implements JWeScribbleShape{
	
	// keeping color for grayOut/recolor methods.
	private int myColor;
	private Paint myPaint;
	
	private float latestX;
	private float latestY;
	
	private Rect myRect;

	@Override
	public WeScribbleShape asWeScribbleShape() {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public int getColor() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setColor(int color) {
		// TODO Auto-generated method stub
		
	}
	public Paint getPaint() { return myPaint; }
	public float getX() { return latestX; }
	public float getY() {return latestY; }
	
	public void setPoint(float x, float y) { 
		latestX = x; latestY = y;
	}
}
