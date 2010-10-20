/* Copyright (c) 2010 Park "segfault" Joon-Kyu
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
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

package org.planetmono.dcuploader;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class GenericProgressHandler {
	private Context context = null;
	private String progressTitle = null;
	private String progressMessage = null;
	
	private ProgressDialog dialog = null;
	
	private Handler starter = new Handler() {
		@Override
		public void handleMessage(Message m) {
			if (dialog != null) {
				dialog.dismiss();
				dialog = null;
			}
			
			dialog = ProgressDialog.show(context, progressTitle, progressMessage);
		}
	};
	
	private Handler messageSetter = new Handler() {
		@Override
		public void handleMessage(Message m) {
			if (dialog == null)
				return;
			
			progressMessage = m.getData().getString("message");
			dialog.setMessage(progressMessage);
		}
	};
	
	private Handler stopper = new Handler() {
		@Override
		public void handleMessage(Message m) {
			if (dialog != null) {
				dialog.dismiss();
				dialog = null;
			}
			
			/* what a fucking mess... */
			if (getLooper() != Looper.getMainLooper())
				getLooper().quit();
			
		}
	};
	
	private Handler errorDialogHandler = new Handler() {
		public void handleMessage(Message m) {
			Bundle b = m.getData();
			String msg;
			
			if (b.containsKey("why"))
				msg = b.getString("why");
			else
				msg = "오류 발생";
			
			new AlertDialog.Builder(context)
					.setNeutralButton("확인", new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							
							/* what a fucking mess... */
							if (getLooper() != Looper.getMainLooper())
								getLooper().quit();
						}
					})
					.setTitle("오류")
					.setMessage(msg).show();
		}
	};
	
	public GenericProgressHandler(Context ctx, String pt) {
		context = ctx;
		progressTitle = pt;
		progressMessage = "기다려 주십시오.";
	}
	
	public GenericProgressHandler(Context ctx, String pt, String pm) {
		context = ctx;
		progressTitle = pt;
		progressMessage = pm;
	}
	
	public void start() {
		starter.sendEmptyMessage(0);
	}
	
	public void stop() {
		stopper.sendEmptyMessage(0);
	}
	
	public void setMessage(String s) {
		Message m = messageSetter.obtainMessage();
		m.getData().putString("message", s);
		
		messageSetter.handleMessage(m);
	}
	
	public void error() {
		stop();
		
		errorDialogHandler.sendEmptyMessage(0);
	}
	
	public void error(String why) {
		stop();

		Message m = errorDialogHandler.obtainMessage();
		m.getData().putString("why", why);
		errorDialogHandler.handleMessage(m);
	}
}
