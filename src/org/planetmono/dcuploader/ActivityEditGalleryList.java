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

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class ActivityEditGalleryList extends ListActivity {
	private final static int ACTION_REMOVE = 0;
	private final static int ACTION_SET_IMAGE = 1;
	private final static int ACTION_REMOVE_IMAGE = 2;
	
	private ArrayList<String> names = new ArrayList<String>();
	private ArrayList<String> ids = new ArrayList<String>();
	private ArrayList<String> urls = new ArrayList<String>();
	
	private int currentPosition = -1; /* not so graceful solution */
	
	private void update() {
		ids.clear();
		names.clear();
		urls.clear();
		
		DatabaseHelper db = new DatabaseHelper(this);
		db.getFavorites(ids, names, urls);
		db.close();
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names);
		getListView().setAdapter(adapter);
	}
	
	@Override
	public void onCreate(Bundle b) {
		super.onCreate(b);
		
		getListView().setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				int position = ((AdapterContextMenuInfo)menuInfo).position;
				
				menu.setHeaderTitle("동작 선택");
				menu.add(0, ACTION_REMOVE, 0, "삭제");
				menu.add(0, ACTION_SET_IMAGE, 0, "짤방 지정");
				
				if (urls.get(position) != null && urls.get(position).length() > 0)
					menu.add(0, ACTION_REMOVE_IMAGE, 0, "짤방 삭제");
			}
		});
		
		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				openContextMenu(view);
			}
		});
		
		update();
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
		final int position = menuInfo.position;
		
		switch (item.getItemId()) {
		case ACTION_REMOVE:
			new AlertDialog.Builder(this)
					.setTitle("확인")
					.setMessage("정말 삭제하시겠습니까?")
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setPositiveButton("네", new OnClickListener() {
						public void onClick(DialogInterface dialog,
								int which) {
							DatabaseHelper db = new DatabaseHelper(ActivityEditGalleryList.this);
							db.wipeFavorite(ids.get(position));
							db.close();
							
							update();
							
							dialog.dismiss();
						}
					})
					.setNegativeButton("아니오", new OnClickListener() {
						public void onClick(DialogInterface dialog,
								int which) {
							dialog.dismiss();
						}
					})
					.show();
			
			return true;
		case ACTION_SET_IMAGE:
			Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
			i.setType("image/*");
			currentPosition = position;
			
			startActivityForResult(i, 0);
			
			return true;
		case ACTION_REMOVE_IMAGE:
			new AlertDialog.Builder(this)
					.setTitle("확인")
					.setMessage("정말 삭제하시겠습니까?")
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setPositiveButton("네", new OnClickListener() {
						public void onClick(DialogInterface dialog,
								int which) {
							DatabaseHelper db = new DatabaseHelper(ActivityEditGalleryList.this);
							db.setImage(ids.get(position), "");
							urls.set(position, null);
							db.close();
							
							Toast.makeText(ActivityEditGalleryList.this, "삭제했습니다.", Toast.LENGTH_SHORT).show();
							
							dialog.dismiss();
						}
					})
					.setNegativeButton("아니오", new OnClickListener() {
						public void onClick(DialogInterface dialog,
								int which) {
							dialog.dismiss();
						}
					})
					.show();
			
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
			String url = data.getData().toString();
			
			DatabaseHelper db = new DatabaseHelper(ActivityEditGalleryList.this);
			db.setImage(ids.get(currentPosition), url);
			db.close();
			
			urls.set(currentPosition, url);
		}
	}
}
