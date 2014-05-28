package edu.vub.at.weScribble.interfaces;

import edu.vub.at.weScribble.WeScribbleShape;

public interface JWeScribbleShape {
	public WeScribbleShape asWeScribbleShape();
	
	public int getColor();
	public void setColor(int color);
}
