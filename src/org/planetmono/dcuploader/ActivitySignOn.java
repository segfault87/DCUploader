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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class ActivitySignOn extends Activity {
	private GenericProgressHandler progressDialog = new GenericProgressHandler(this, "로그인 중");
	
	private Handler signOnResultHandler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			Bundle b = m.getData();
			Application app = (Application)getApplication();
			
			app.setSignedOn(b.getBoolean("result"), b.getInt("method"));
			
			if (b.getBoolean("result")) {
				progressDialog.stop();
				
				boolean save = ((CheckBox)findViewById(R.id.signon_saveidpw)).isChecked();
				
				SharedPreferences.Editor e = getSharedPreferences(Application.APP, Activity.MODE_PRIVATE).edit();
							
				if (save) {
					e.putString("id", ((TextView)findViewById(R.id.signon_id)).getText().toString());
					e.putString("pw", ((TextView)findViewById(R.id.signon_pw)).getText().toString());
				} else {
					e.putString("id", "");
					e.putString("pw", "");
				}
				
				e.putBoolean(ActivityPreferences.KEY_SAVE_ID_PASSWORD, save);
				e.commit();
				
				setResult(RESULT_OK);
				finishActivity(Application.ACTION_SIGN_ON);
				
				finish();
			} else {
				progressDialog.error(b.getString("resultString"));
			}
		}
	};
	
	public void signOn(SignOnBase method, Bundle params) {
		new Thread(method.getMethodSignOn((Application)getApplication(), params, signOnResultHandler)).start();
	}
	
	@Override
	public void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		
		setContentView(R.layout.login);
		
		((Button)findViewById(R.id.signon_login)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EditText id = (EditText)findViewById(R.id.signon_id);
				EditText pw = (EditText)findViewById(R.id.signon_pw);
				
				if (id.getText().length() == 0 || pw.getText().length() == 0) {
					new AlertDialog.Builder(ActivitySignOn.this)
							.setTitle("오류")
							.setMessage("내용을 제대로 입력해 주십시오.")
							.setNeutralButton("확인", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							}).show();
					
					return;
				}
				
				progressDialog.start();
				
				Bundle b = new Bundle();
				SignOnBase method;
				if (true) {
					/* Gallog authentication */
					b.putString("id", id.getText().toString());
					b.putString("password", pw.getText().toString());
					method = new SignOnGallog();
				}
				
				signOn(method, b);
			}
	
		});
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		SharedPreferences pref = getSharedPreferences(Application.APP, Activity.MODE_PRIVATE);
		String id = pref.getString("id", "");
		String pw = pref.getString("pw", "");
		boolean saveIdPw = pref.getBoolean(ActivityPreferences.KEY_SAVE_ID_PASSWORD, false);
		boolean autoSignOn = pref.getBoolean(ActivityPreferences.KEY_AUTO_SIGN_ON, false);
		
		((EditText)findViewById(R.id.signon_id)).setText(id);
		((EditText)findViewById(R.id.signon_pw)).setText(pw);
		((CheckBox)findViewById(R.id.signon_saveidpw)).setChecked(saveIdPw);
		
		boolean auto = getIntent().getBooleanExtra("auto", true);
		if (saveIdPw && autoSignOn && auto && id.length() > 0 && pw.length() > 0) {
			progressDialog.start();
			
			Bundle b = new Bundle();
			SignOnBase method;
			if (true) {
				/* Gallog authentication */
				b.putString("id", id);
				b.putString("password", pw);
				method = new SignOnGallog();
			}
			
			signOn(method, b);
		}
	}
}
