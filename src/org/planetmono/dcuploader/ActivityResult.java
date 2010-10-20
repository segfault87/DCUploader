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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityResult extends Activity {
	private boolean called;
	
	private abstract class ActionBase {
		protected String url;
		
		public ActionBase(String url) {
			this.url = url;
		}
	}
	
	/* Open browser */
	private class ActionOpenBrowser extends ActionBase implements OnClickListener {
		public ActionOpenBrowser(String url) {
			super(url);
		}

		public void onClick(View v) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
		}
	};
	
	/* Copy to clipboard */
	private class ActionCopyClipboard extends ActionBase implements OnClickListener {
		public ActionCopyClipboard(String url) {
			super(url);
		}

		public void onClick(View v) {
			ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
			cm.setText(url);
			
			Toast.makeText(ActivityResult.this, "클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show();
		}
	};
	
	/* Share link */
	private class ActionShareLink extends ActionBase implements OnClickListener {
		public ActionShareLink(String url) {
			super(url);
		}

		public void onClick(View v) {
			Intent i = new Intent(Intent.ACTION_SEND);
			i.putExtra(Intent.EXTRA_TEXT, url);
			i.setType("text/plain");
			
			startActivity(i);
		}
	};
	
	@Override
	public void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		
		setContentView(R.layout.result);
		
		Intent i = getIntent();
		
		String querymsg = "?id=" + i.getStringExtra("target") + "&no=" + i.getIntExtra("no", 0);
		int destination = i.getIntExtra("destination", 0);
		
		String urlDcinside = Application.URL_VIEW_DCINSIDE + querymsg;
		String urlBoxweb = Application.URL_VIEW_BOXWEB + querymsg;
		String urlMoolzo = Application.URL_VIEW_MOOLZO + querymsg;
		
		if (destination == Application.DESTINATION_BOXWEB) {
			finish();
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urlBoxweb)));
			return;
		} else if (destination == Application.DESTINATION_DCINSIDE) {
			finish();
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urlDcinside)));
			return;
		} else if (destination == Application.DESTINATION_MOOLZO) {
			finish();
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urlMoolzo)));
			return;
		}
		
		called = i.getBooleanExtra("called", false);
		
		((Button)findViewById(R.id.result_ok)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
				if (!called) {
					Intent i = new Intent(ActivityResult.this, ActivityUploader.class);
					i.setAction(Intent.ACTION_MAIN);
					
					startActivity(i);
				}
			}
		});
		
		SharedPreferences pref = getSharedPreferences(Application.APP, Activity.MODE_PRIVATE);
		String mobilePageProvider = pref.getString(ActivityPreferences.KEY_MOBILE_PAGE_PROVIDER, ActivityPreferences.DEFAULT_MOBILE_PAGE_PROVIDER);
		
		String urlMobile;
		if (mobilePageProvider.equals("moolzo")) {
			((TextView)findViewById(R.id.result_mobile_text)).setText("모바일 (물조)");
			urlMobile = urlMoolzo;
		} else {
			((TextView)findViewById(R.id.result_mobile_text)).setText("모바일 (박스웹)");
			urlMobile = urlBoxweb;
		}
		
		((Button)findViewById(R.id.result_pc_open_link)).setOnClickListener(new ActionOpenBrowser(urlDcinside));
		((Button)findViewById(R.id.result_mobile_open_link)).setOnClickListener(new ActionOpenBrowser(urlMobile));
		((Button)findViewById(R.id.result_pc_copy)).setOnClickListener(new ActionCopyClipboard(urlDcinside));
		((Button)findViewById(R.id.result_mobile_copy)).setOnClickListener(new ActionCopyClipboard(urlMobile));
		((Button)findViewById(R.id.result_pc_share)).setOnClickListener(new ActionShareLink(urlDcinside));
		((Button)findViewById(R.id.result_mobile_share)).setOnClickListener(new ActionShareLink(urlMobile));
	}
}
