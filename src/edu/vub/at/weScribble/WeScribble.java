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

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Random;
import java.util.Vector;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.apis.graphics.ColorPickerDialog;

import edu.vub.at.IAT;
import edu.vub.at.android.util.IATAndroid;
import edu.vub.at.weScribble.interfaces.ATWeScribble;
import edu.vub.at.weScribble.interfaces.JWeScribble;
import edu.vub.at.weScribble.interfaces.JWeScribblePath;

/**
 * Main activity of the weScribble application. Starts an iat and it is
 * populated by a weScribbleView.
 */
public class WeScribble extends Activity implements
		ColorPickerDialog.OnColorChangedListener, JWeScribble {

	public class StartIATTask extends AsyncTask<Void, String, Void> {

		private ProgressDialog pd;

		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			pd.setMessage(values[0]);
		}

		protected void onPreExecute() {
			super.onPreExecute();
			pd = ProgressDialog.show(WeScribble.this, "weScribble", "Starting AmbientTalk");
		}
		
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			updateTitle();
			pd.dismiss();
			
			int lucky_color = Math.abs(new Random().nextInt()) % COLORS.length;
			colorChanged(COLORS[lucky_color]);
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			try {
				iat = IATAndroid.create(WeScribble.this);
				
				this.publishProgress("Loading weScribble code");
				iat.evalAndPrint("import /.demo.weScribble.weScribble.makeWeScribble()", System.err);
			} catch (Exception e) {
				Log.e("AmbientTalk", "Could not start IAT", e);
			}
			return null;
		}
	}

	private static final int _ASSET_INSTALLER_ = 0;
	private static IAT iat;
	private boolean activate = true;

	public static final int _MSG_TOUCH_START_ = 0;
	public static final int _MSG_TOUCH_MOVE_ = 1;
	public static final int _MSG_TOUCH_END_ = 2;
	public static final int _MSG_RESET_ = 3;

	private static ATWeScribble atws;
	private static int my_color = Color.RED;
	private static int numOthers = 0;
	
	public WeScribbleView myView;
	
	// Handler to communicate UI <-> AT threads.
	public static Handler mHandler;

	// Random starting colors
	private static final int[] COLORS = new int[] {
		Color.RED,  Color.GREEN, Color.YELLOW,
		Color.BLUE, Color.CYAN,  Color.MAGENTA, Color.WHITE
	};
	
	private final Runnable UPDATE_TITLE_ASYNC = new Runnable() {
		public void run() {
			updateTitle();
		}
	};

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		myView = new WeScribbleView(this);
		setContentView(myView);

		if (iat == null) {
			Intent i = new Intent(this, WeScribbleAssetInstaller.class);
			startActivityForResult(i, _ASSET_INSTALLER_);
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v("WeScribble", "Return of Asset Installer activity");
		switch (requestCode) {
		case (_ASSET_INSTALLER_):
			if (resultCode == Activity.RESULT_OK) {
				LooperThread lt = new LooperThread();
				lt.start();
				mHandler = lt.mHandler;
				new StartIATTask().execute((Void)null);
			}
			break;
		}
	}

	// The Options menu displayed in this activity are inflated from the
	// res/menu/menu.xml
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}
	
	protected void onStart() {
		super.onStart();
		if (atws != null)
			atws.handshake(this);
		myView.getPath().setColor(my_color);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Paint paint = myView.getPath().getPaint();

		switch (item.getItemId()) {
		case R.id.menu_color:
			new ColorPickerDialog(this, this, paint.getColor()).show();
			return true;
		case R.id.menu_reset:
			// we are already in the UI thread so we can call invalidate directly.
			myView.erase();
			myView.invalidate();
			mHandler.sendMessage(Message.obtain(mHandler, _MSG_RESET_));
			return true;
		case R.id.menu_save:
			try {
				File uri = newImagePath();
				FileOutputStream out = new FileOutputStream(uri);
				myView.getBitmap().compress(CompressFormat.PNG, 100, out);
				out.close();
				Toast.makeText(this, "Wrote image to " + uri + ".", Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				Toast.makeText(this, "Could not save image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
				Log.e("weScribble", "Could not write image", e);
			}
			return true;
		  case R.id.menu_network:
			//UNCOMMENT THE FOLLOWiNG LINES WHEN YOU DO SECTION C).
			  if (activate) {
				  atws.disconnect();
				  activate = false;
			  } else{
				  atws.reconnect();
				  activate = true;
			  }
			  return true;
		case R.id.menu_exit:
			System.exit(1);
			return true;
		case R.id.menu_rectangle:
			WeScribbleView.PREFERENCE_OBJECT = 1;
			return true;
		case R.id.menu_line:
			WeScribbleView.PREFERENCE_OBJECT = 2;
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// Generate a new path to save an image
	private File newImagePath() {
		File base = new File(Environment.getExternalStorageDirectory(), "weScribble");
		base.mkdir();
		String filename = DateFormat.format("'weScribble-'yyyyMMdd-kkmmss'.png'", new Date()).toString();
		return new File(base, filename);
	}

	public void colorChanged(final int color) {
		myView.getPath().setColor(color);
		my_color = color;
		runOnUiThread(new Runnable() {
			public void run() {
				Toast t = new Toast(WeScribble.this);
				View layout = getLayoutInflater().inflate(R.layout.lucky_color, null);
				
				ImageView iv = (ImageView) layout.findViewById(R.id.lucky_color_view);
				iv.setBackgroundColor(color);

				t.setView(layout);
				t.setDuration(Toast.LENGTH_SHORT);
				t.show();
			}
		});
	}

	// Methods implementing the JWeScribble interface
	public JWeScribble registerATApp(ATWeScribble atws) {
		this.atws = atws;
		return this;
	}

	public void resetCanvas() {
		myView.erase();
		myView.postInvalidate();
	}

	public void redrawCanvas() {
		Log.v("WeScribble", "redrawCanvas");
		myView.postInvalidate();
	}

	public int getColor() {
		return myView.getPath().getColor();
	}

	public JWeScribblePath createForeignPath(int color) {
		return new WeScribblePath(myView.getBitmap(), color, true);
	}

	public void addForeignPath(JWeScribblePath path) {
		myView.addForeignPath(path.asWeScribblePath());
	}

	public void endForeignPath(JWeScribblePath path) {
		myView.removeForeignPath(path.asWeScribblePath());
	}

	public JWeScribblePath getMyPath() {
		return myView.getPath();
	}

	public void grayOut(JWeScribblePath path) {
		myView.grayOut(path.asWeScribblePath());
	}

	public void recolor(JWeScribblePath path) {
		myView.recolor(path.asWeScribblePath());
	}

	// Indicate how many other people are drawing with us
	private void updateTitle() {
		if (numOthers == 0) {
			setTitle("WeScribble -- drawing alone");			
		} else {
			String suffix = numOthers == 1 ? "person" : "people";
			setTitle("WeScribble -- drawing with " + numOthers + " " + suffix);
		}
	}
	
	public void personAdded() {
		numOthers++;
		runOnUiThread(UPDATE_TITLE_ASYNC);
	}
	
	public void personRemoved() {
		numOthers--;
		runOnUiThread(UPDATE_TITLE_ASYNC);
	}

	// Call the AmbientTalk methods in a separate thread to avoid blocking the UI.
	class LooperThread extends Thread {

		public Handler mHandler = new Handler() {
			public void handleMessage(Message msg) {
				if (null == atws)
					return;
				switch (msg.what) {
				case _MSG_TOUCH_START_: {
					float[] startPoint = (float[]) msg.obj;
					atws.touchStart(startPoint[0], startPoint[1]);
					break;
				}
				case _MSG_TOUCH_MOVE_:
					atws.touchMove((Vector<Float>) msg.obj);
					break;
				case _MSG_TOUCH_END_: {
					float[] endPoint = (float[]) msg.obj;
					atws.touchEnd(endPoint[0], endPoint[1]);
					break;
				}
				case _MSG_RESET_:
					atws.reset();
					break;
				}
			}
		};

		public void run() {
			Looper.prepare();
			Looper.loop();
		}
	}
}
