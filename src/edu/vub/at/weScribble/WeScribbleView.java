/*
 * Based on FingerPaint.java distributed with the Android SDK.
 * It has the following license:
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.vub.at.weScribble;

import java.util.Vector;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;

public class WeScribbleView extends View {

	private final WeScribble weScribble;
	private static Bitmap  mBitmap;
    private Paint   mBitmapPaint;
	private Canvas  mCanvas;
	private WeScribblePath myPath;
    private int     mDelayUpdate;
    private Vector<Float> mPoints;
	private Vector<WeScribblePath> mForeignPaths;

    private static final float TOUCH_TOLERANCE = 4;
    private static final int MAXPOINTS = 100;
	
    public WeScribbleView(WeScribble weScribble) {
        super(weScribble);
		this.weScribble = weScribble;
        
        Display display = weScribble.getWindowManager().getDefaultDisplay(); 
        int mWidth  = display.getWidth();
        int mHeight = display.getHeight();
        if (mBitmap == null) {
	        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
	        Log.v("weScribble", "Created bitmap " + mHeight + "x" + mWidth);
        }

        mCanvas = new Canvas(mBitmap);
        myPath = new WeScribblePath(mBitmap, 0xFFFF0000);
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        mDelayUpdate = 0;
        mPoints = new Vector<Float>(MAXPOINTS * 2 + 2);
		mForeignPaths = new Vector<WeScribblePath>();
		setBackgroundColor(0xFF555555);
		setKeepScreenOn(true);
    }

    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        synchronized(mForeignPaths) {
        	for (WeScribblePath path : mForeignPaths) {
        		Log.v("WeScribble", "path" + path + " color " + path.getColor());
				canvas.drawPath(path, path.getPaint());
			}
        }
        
       	canvas.drawPath(myPath, myPath.getPaint());
    }
	
	public Vector<WeScribblePath> getAllPaths() { 
		Vector<WeScribblePath> allPaths = (Vector<WeScribblePath>) mForeignPaths.clone();
		allPaths.add(myPath);
		return allPaths;		
	};
	
    private void touch_start(float x, float y) {
        myPath.rewind();
        myPath.moveTo(x, y);
     // update mPoints vector
        float scaledX = myPath.getScaledX();
        float scaledY = myPath.getScaledY();
        mPoints.clear();
        mPoints.add(scaledX);
        mPoints.add(scaledY);
        mDelayUpdate = 1;
        // send start point to ambientTalk layer.
        float [] startPoint = {scaledX, scaledY};
        getFPHandler().sendMessage(Message.obtain(getFPHandler(), WeScribble._MSG_TOUCH_START_, startPoint));
    }
    
    private void touch_move(float x, float y) {
    	float mX = myPath.getX();
    	float mY = myPath.getY();
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            myPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            myPath.setPoint(x, y);
            //update mPoints vector
            mPoints.add(myPath.getScaledX());
            mPoints.add(myPath.getScaledY());
            if (mDelayUpdate++ >= MAXPOINTS) {
            	transmitPoints();
            }
        }
    }
    
    private void touch_end() {
    	if (mDelayUpdate > 0) {
    		transmitPoints();
    	}
        myPath.lineTo(myPath.getX(), myPath.getY());
        // commit the path to our offscreen
        mCanvas.drawPath(myPath, myPath.getPaint());
        // kill this so we don't double draw
        myPath.rewind();
        float [] endPoint = {myPath.getScaledX(), myPath.getScaledY()};
        getFPHandler().sendMessage(Message.obtain(getFPHandler(), WeScribble._MSG_TOUCH_END_, endPoint));
    }

    private void transmitPoints() {
    	getFPHandler().sendMessage(Message.obtain(getFPHandler(), WeScribble._MSG_TOUCH_MOVE_, mPoints));
    	mPoints = new Vector<Float>(42);
    	mDelayUpdate = 0;
	}

    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_end();
                invalidate();
                break;
        }
        return true;
    }
	
    public final Bitmap getBitmap() {
		return mBitmap;
	}
    
    public Canvas getCanvas() {
		return mCanvas;
	}
    
    public WeScribblePath getPath(){
    	return myPath;
    }
   
    private Handler getFPHandler() {
    	return weScribble.mHandler;
    }

	public void erase() {
		mForeignPaths.clear();
		myPath.reset();
		mBitmap.eraseColor(0x00000000);
	}
	
	public void addForeignPath(WeScribblePath path) {
		synchronized(mForeignPaths) {
			mForeignPaths.add(path);
		}
	}
	
	public void removeForeignPath(WeScribblePath path) {
		synchronized (mForeignPaths) {
			mForeignPaths.remove(path);
		}
		mCanvas.drawPath(path, path.getPaint());
	}
	
	public void grayOut(WeScribblePath path) {
		path.grayOut();
		synchronized (mForeignPaths) {
			if (!mForeignPaths.contains(path)){
				 mForeignPaths.add(path);
			}
		}
	}
	
	public void recolor(WeScribblePath path) {
		path.recolor();
		synchronized (mForeignPaths) {
			if (!mForeignPaths.contains(path)){
				Log.v("WeScribble", "recoloring path "+ path);
			  mForeignPaths.add(path);
			}
		}
	}

}