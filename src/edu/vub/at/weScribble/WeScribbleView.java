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

import java.util.HashSet;
import java.util.Random;
import java.util.Vector;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
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
    	super.onDraw(canvas);
    	switch(PREFERENCE_OBJECT) {
	    	case 1:
	    		// background bitmap to cover all area
	    		//mBitmapPaint.setColor(Color.BLUE);
	    		//canvas.drawBitmap(mBitmap, null, mMeasuredRect, null);
	            for (CircleArea circle : mCircles) {
	            	canvas.drawCircle(circle.centerX, circle.centerY, circle.radius, mBitmapPaint);
	            }
	    	case 2: 
	    		canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
	            synchronized(mForeignPaths) {
	            	for (WeScribblePath path : mForeignPaths) {
	            		Log.v("WeScribble", "path" + path + " color " + path.getColor());
	    				canvas.drawPath(path, path.getPaint());
	    			}
	            }
	           	canvas.drawPath(myPath, myPath.getPaint());
    	}
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
    	switch(PREFERENCE_OBJECT) {
    		case 1:
    			boolean handled = false;

    	        CircleArea touchedCircle;
    	        int xTouch;
    	        int yTouch;
    	        int pointerId;
    	        int actionIndex = event.getActionIndex();

    	        // get touch event coordinates and make transparent circle from it
    	        switch (event.getAction()) {
    	            case MotionEvent.ACTION_DOWN:
    	                // it's the first pointer, so clear all existing pointers data
    	                clearCirclePointer();

    	                xTouch = (int) event.getX(0);
    	                yTouch = (int) event.getY(0);

    	                // check if we've touched inside some circle
    	                touchedCircle = obtainTouchedCircle(xTouch, yTouch);
    	                touchedCircle.centerX = xTouch;
    	                touchedCircle.centerY = yTouch;
    	                mCirclePointer.put(event.getPointerId(0), touchedCircle);

    	                invalidate();
    	                handled = true;
    	                break;

    	            case MotionEvent.ACTION_POINTER_DOWN:
    	                Log.w("WeScribble", "Pointer down");
    	                // It secondary pointers, so obtain their ids and check circles
    	                pointerId = event.getPointerId(actionIndex);

    	                xTouch = (int) event.getX(actionIndex);
    	                yTouch = (int) event.getY(actionIndex);

    	                // check if we've touched inside some circle
    	                touchedCircle = obtainTouchedCircle(xTouch, yTouch);

    	                mCirclePointer.put(pointerId, touchedCircle);
    	                touchedCircle.centerX = xTouch;
    	                touchedCircle.centerY = yTouch;
    	                invalidate();
    	                handled = true;
    	                break;

    	            case MotionEvent.ACTION_MOVE:
    	                final int pointerCount = event.getPointerCount();

    	                Log.w("WeScribble", "Move");

    	                for (actionIndex = 0; actionIndex < pointerCount; actionIndex++) {
    	                    // Some pointer has moved, search it by pointer id
    	                    pointerId = event.getPointerId(actionIndex);

    	                    xTouch = (int) event.getX(actionIndex);
    	                    yTouch = (int) event.getY(actionIndex);

    	                    touchedCircle = mCirclePointer.get(pointerId);

    	                    if (null != touchedCircle) {
    	                        touchedCircle.centerX = xTouch;
    	                        touchedCircle.centerY = yTouch;
    	                    }
    	                }
    	                invalidate();
    	                handled = true;
    	                break;

    	            case MotionEvent.ACTION_UP:
    	                clearCirclePointer();
    	                invalidate();
    	                handled = true;
    	                break;

    	            case MotionEvent.ACTION_POINTER_UP:
    	                // not general pointer was up
    	                pointerId = event.getPointerId(actionIndex);

    	                mCirclePointer.remove(pointerId);
    	                invalidate();
    	                handled = true;
    	                break;

    	            case MotionEvent.ACTION_CANCEL:
    	                handled = true;
    	                break;

    	            default:
    	                // do nothing
    	                break;
    	        }

    	        return super.onTouchEvent(event) || handled;
    		case 2:
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
	
	private Rect mMeasuredRect;
	
	private static class CircleArea	 {
        int radius;
        int centerX;
        int centerY;

        CircleArea(int centerX, int centerY, int radius) {
            this.radius = radius;
            this.centerX = centerX;
            this.centerY = centerY;
        }

        @Override
        public String toString() {
            return "Circle[" + centerX + ", " + centerY + ", " + radius + "]";
        }
    }
	/** Object to draw 1: circle, 2: line */
	static int PREFERENCE_OBJECT = 2; 
	/** Paint to draw circles */
    private final Random mRadiusGenerator = new Random();
    // Radius limit in pixels
    private final static int RADIUS_LIMIT = 100;

    private static final int CIRCLES_LIMIT = 100;

    /** All available circles */
    private HashSet<CircleArea> mCircles = new HashSet<CircleArea>(CIRCLES_LIMIT);
    private SparseArray<CircleArea> mCirclePointer = new SparseArray<CircleArea>(CIRCLES_LIMIT);
    
    /**
     * Clears all CircleArea - pointer id relations
     */
    private void clearCirclePointer() {
        Log.w("WeScribble", "clearCirclePointer");

        mCirclePointer.clear();
    }
    /**
     * Search and creates new (if needed) circle based on touch area
     *
     * @param xTouch int x of touch
     * @param yTouch int y of touch
     *
     * @return obtained {@link CircleArea}
     */
    private CircleArea obtainTouchedCircle(final int xTouch, final int yTouch) {
        CircleArea touchedCircle = getTouchedCircle(xTouch, yTouch);

        if (null == touchedCircle) {
            touchedCircle = new CircleArea(xTouch, yTouch, mRadiusGenerator.nextInt(RADIUS_LIMIT) + RADIUS_LIMIT);

            if (mCircles.size() == CIRCLES_LIMIT) {
                Log.w("WeScribble", "Clear all circles, size is " + mCircles.size());
                // remove first circle
                mCircles.clear();
            }

            Log.w("WeScribble", "Added circle " + touchedCircle);
            mCircles.add(touchedCircle);
        }

        return touchedCircle;
    }
    /**
     * Determines touched circle
     *
     * @param xTouch int x touch coordinate
     * @param yTouch int y touch coordinate
     *
     * @return {@link CircleArea} touched circle or null if no circle has been touched
     */
    private CircleArea getTouchedCircle(final int xTouch, final int yTouch) {
        CircleArea touched = null;

        for (CircleArea circle : mCircles) {
            if ((circle.centerX - xTouch) * (circle.centerX - xTouch) + (circle.centerY - yTouch) * (circle.centerY - yTouch) <= circle.radius * circle.radius) {
                touched = circle;
                break;
            }
        }

        return touched;
    }
//    private void init(final Context ct) {
//        // Generate bitmap used for background
//        mBitmap = BitmapFactory.decodeResource(ct.getResources(), R.drawable.up_image);
//
//        mBitmapPaint = new Paint();
//
//        mBitmapPaint.setColor(Color.BLUE);
//        mBitmapPaint.setStrokeWidth(40);
//        mBitmapPaint.setStyle(Paint.Style.FILL);
//    }
}