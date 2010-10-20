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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.planetmono.dcuploader.DatabaseHelper.CursorAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class ActivityGalleryChooser extends Activity {
	private ArrayList<String> names = new ArrayList<String>();
	private ArrayList<String> ids = new ArrayList<String>();
	
	private DatabaseHelper db;
	
	private GenericProgressHandler progressHandler = new GenericProgressHandler(this, "갤러리 목록 수신중", "오래 걸립니다. 잠시만 기다려 주십시오.");
	
	private static String GALLERY_LISTS[] = {
		"http://gall.dcinside.com",
		"http://wstatic.dcinside.com/gallery/gallindex_iframe_new.html"
	};
	
	Runnable updaterThread = new Runnable() {
		public void run() {
			for (int i = 0; i < GALLERY_LISTS.length; ++i)
				fetchPage(GALLERY_LISTS[i]);
			
			progressHandler.stop();
			
			fetcherHandler.sendEmptyMessage(0);
		}
		
		public void fetchPage(String url) {
			Application app = (Application)ActivityGalleryChooser.this.getApplication();
			HttpGet get = new HttpGet(url);
			
			Log.d(Application.TAG, "fetching " + url);
			
			try {
				app.sendGetRequest(get);
			} catch (Exception e) {
				progressHandler.error(e.toString());
				
				return;
			}
			
			db.clearFields();
			
			HttpResponse response = null;
			try {
				response = app.sendGetRequest(get);
			} catch (Exception e) {
				progressHandler.error(e.toString());
				
				return;
			}
			
			HttpEntity entity = response.getEntity();
			BufferedReader r;
			
			String key, value;
			try {
				r = new BufferedReader(new InputStreamReader(entity.getContent(), "utf-8"));
				
				while (true) {
					String line = r.readLine();
					if (line == null) break;
					
					line = line.trim();
					
					Pattern p = Pattern.compile("list.php\\?id=([\\-a-zA-Z0-9_]+)");
					Matcher m = p.matcher(line);
					
					while (m.find()) {
						if (m.groupCount() > 0) {
							key = m.group(1).trim();
							
							Pattern ip = Pattern.compile(".*(<a .*?list.php\\?id=" + key + ".*?>.*?</a>).*");
							Matcher im = ip.matcher(line);
							
							if (im.find()) {
								value = im.group(1).replaceAll("\\<.*?>","").trim();
								if (value == null || value.equals(""))
									value = key;
							} else
								value = key;
							
							if (value.startsWith("-")) {
								int i;
								for (i = 0; i < value.length(); ++i)
									if (value.charAt(i) != '-')
										break;
								value = value.substring(i).trim();
							}
							
							db.getWritableDatabase();
							db.insert(key, value);
							db.close();
						}
					}
				}
			} catch (Exception e) {
				progressHandler.error(e.toString());
				
				return;
			}
		}
	};
	
	Handler fetcherHandler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			names.clear();
			ids.clear();
			
			CursorAdapter ca = new CursorAdapter() {
				public void handleCursor(final Cursor c) {
					String key = c.getString(0);
					String value = c.getString(1);
					
					ids.add(key);
					names.add(value);
				}
			};
			
			Bundle b = m.getData();
			if (b.containsKey("searchTerm"))
				db.fetchList(ca, b.getString("searchTerm"));
			else
				db.fetchList(ca);
			
			ArrayAdapter<String> aa = new ArrayAdapter<String>(
					ActivityGalleryChooser.this,
					android.R.layout.simple_list_item_multiple_choice,
					names);
			
			ListView lv = (ListView)findViewById(R.id.gallery_chooser_list);
			lv.setAdapter(aa);
			lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		}
	};
	
	public void refresh() {
		progressHandler.start();
		
		new Handler() {
			@Override
			public void handleMessage(Message m) {
				new LooperDelegate(updaterThread).start();
			}
		}.sendEmptyMessage(0);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		db = new DatabaseHelper(this);
		
		setContentView(R.layout.gallery_chooser);
		((Button)findViewById(R.id.gallery_chooser_search)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String query = ((EditText)findViewById(R.id.gallery_chooser_edit)).getText().toString();
				
				if (query.length() > 0) {
					Message m = fetcherHandler.obtainMessage();
					m.getData().putString("searchTerm", query);
					fetcherHandler.handleMessage(m);
				} else {
					fetcherHandler.sendEmptyMessage(0);
				}
			}
		});
		((Button)findViewById(R.id.gallery_chooser_ok)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ListView lv = (ListView)findViewById(R.id.gallery_chooser_list);
				
				long nids[] = lv.getCheckItemIds();
				String nstrs[] = new String[nids.length];
				
				for (int i = 0; i < nids.length; ++i)
					nstrs[i] = ids.get((int)nids[i]);
				
				Intent i = new Intent();
				i.putExtra("result", nstrs);
				
				setResult(Activity.RESULT_OK, i);
				finishActivity(Application.ACTION_ADD_GALLERY);
				finish();
			}
		});
		((Button)findViewById(R.id.gallery_chooser_cancel)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

		if (db.rowCount() == 0)
			refresh();
		else
			fetcherHandler.sendEmptyMessage(0);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.gallery_chooser, menu);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_gallery_chooser_update:
			new AlertDialog.Builder(ActivityGalleryChooser.this)
					.setTitle("확인")
					.setMessage("정말 새로 고치시겠습니까? 나의 갤러리 목록이 초기화됩니다.")
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setPositiveButton("네", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int which) {
							refresh();
						}
					})
					.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int which) {
							dialog.dismiss();
						}
					})
					.show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
