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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static final String DBNAME = "GalleryList";
	private static final int DBREV = 3;
	
	private static final String KEY_IDX = "no";
	private static final String KEY_ID = "id";
	private static final String KEY_DESC = "name";
	private static final String KEY_GALLID = "idx";
	private static final String KEY_REFS = "ref";
	private static final String KEY_IMAGE = "image";
	
	private static final String TABLE_GALLERIES = "Galleries";
	private static final String TABLE_RECENTS = "Recents";
	
	private static final String STMT_CREATE_TABLE_GALLERIES =
		"CREATE TABLE IF NOT EXISTS " + TABLE_GALLERIES + " (" +
		"  " + KEY_IDX + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		"  " + KEY_ID + "  TEXT, " +
		"  " + KEY_DESC + " TEXT " +
		");";
	private static final String STMT_CREATE_TABLE_RECENTS = 
		"CREATE TABLE IF NOT EXISTS " + TABLE_RECENTS + " (" +
		"  " + KEY_IDX + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		"  " + KEY_GALLID + " INTEGER, " +
		"  " + KEY_REFS + " INTEGER, " +
		"  " + KEY_IMAGE + " TEXT" +
		");";
	
	public DatabaseHelper(Context ctx) {
		super(ctx, DBNAME, null, DBREV);
		
	}
	
	private int getId(String name) {
		SQLiteDatabase dbctx = this.getReadableDatabase();
		
		Cursor c = dbctx.query(
				TABLE_GALLERIES,
				new String[] { KEY_IDX },
				KEY_ID + " = '" + name + "'",
				null,
				null,
				null,
				null
		);
		
		if (c.getCount() == 0)
			return -1;
		
		c.moveToFirst();
		int id = c.getInt(0);
		c.close();
		
		close();
		
		return id;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(STMT_CREATE_TABLE_GALLERIES);
		db.execSQL(STMT_CREATE_TABLE_RECENTS);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < DBREV && newVersion == DBREV) {
			db.execSQL("DROP TABLE " + TABLE_RECENTS);
			db.execSQL(STMT_CREATE_TABLE_RECENTS);
		}
		
		onCreate(db);
	}
	
	public void clearFields() {
		SQLiteDatabase dbctx = getWritableDatabase();
		
		dbctx.delete(TABLE_GALLERIES, null, null);
		dbctx.delete(TABLE_RECENTS, null, null);
		
		close();
	}
	
	public boolean contains(String id) {
		SQLiteDatabase dbctx = getReadableDatabase();
		
		Cursor c = dbctx.query(
			TABLE_GALLERIES,
			new String[] { KEY_IDX },
			KEY_ID + " = '" + id + "'",
			null,
			null,
			null,
			null
		);
		
		int count = c.getCount();
		
		c.close();
		dbctx.close();
		
		if (count > 0)
			return true;
		else
			return false;
	}
	
	SQLiteDatabase __dbctx;
	boolean isOpen = false;
	
	@Override
	public void close() {
		super.close();
		
		isOpen = false;
	}
	
	@Override
	public SQLiteDatabase getReadableDatabase() {
		__dbctx = super.getReadableDatabase();
		
		return __dbctx;
	}
	
	@Override
	public SQLiteDatabase getWritableDatabase() {
		__dbctx = super.getWritableDatabase();
		
		return __dbctx;
	}
	
	public void begin() {
		SQLiteDatabase dbctx = getWritableDatabase();
		
		dbctx.beginTransaction();
	}
	
	public void end() {
		SQLiteDatabase dbctx = getWritableDatabase();
		
		dbctx.endTransaction();
	}
	
	public void insert(String id, String name) {
		if (contains(id))
			return;
		
		ContentValues values = new ContentValues();
		
		values.put("id", id);
		values.put("name", name);
		
		SQLiteDatabase dbctx = getWritableDatabase();
		
		dbctx.insert(TABLE_GALLERIES, "", values);
	}
	
	public int rowCount() {
		SQLiteDatabase dbctx = this.getReadableDatabase();
		
		Cursor c = dbctx.query(
			TABLE_GALLERIES,
			new String[] { "COUNT(*)" },
			null,
			null,
			null,
			null,
			null
		);
		
		c.moveToFirst();
		int rc = c.getInt(0);
		c.close();
		
		close();
		
		return rc;
	}
	
	public interface CursorAdapter {
		public void handleCursor(final Cursor c);
	}
	
	public void fetchList(CursorAdapter a) {
		SQLiteDatabase dbctx = this.getReadableDatabase();
		
		Cursor c = dbctx.query(
			TABLE_GALLERIES,
			new String[] { KEY_ID, KEY_DESC },
			null,
			null,
			null,
			null,
			null
		);
		
		c.moveToFirst();
		while (!c.isAfterLast()) {
			a.handleCursor(c);
			
			c.moveToNext();
		}
		
		close();
	}
	
	public void fetchList(CursorAdapter a, String searchTerm) {
		SQLiteDatabase dbctx = this.getReadableDatabase();
		
		Cursor c = dbctx.query(
			TABLE_GALLERIES,
			new String[] { KEY_ID, KEY_DESC },
			KEY_ID + " LIKE '%" + searchTerm + "%' OR " + KEY_DESC +" LIKE '%" + searchTerm + "%'" ,
			null,
			null,
			null,
			null
		);
		
		c.moveToFirst();
		while (!c.isAfterLast()) {
			a.handleCursor(c);
			c.moveToNext();
		}
		
		c.close();
		close();
	}
	
	public void getFavorites(ArrayList<String> keys, ArrayList<String> values) {
		SQLiteDatabase dbctx = this.getReadableDatabase();
		
		keys.clear();
		values.clear();
		
		Cursor c = dbctx.rawQuery(
				"SELECT a." + KEY_ID + ", a." + KEY_DESC + " " +
				"FROM " + TABLE_GALLERIES + " AS a, " + TABLE_RECENTS + " AS b " +
				"WHERE a." + KEY_IDX + " = b." + KEY_GALLID + " " +
				"ORDER BY " + KEY_REFS + " DESC",
				null
		);
		
		c.moveToFirst();
		while (!c.isAfterLast()) {
			keys.add(c.getString(0));
			values.add(c.getString(1));
			c.moveToNext();
		}
		
		c.close();
		close();
	}
	
	public void getFavorites(ArrayList<String> keys, ArrayList<String> values, ArrayList<String> urls) {
		SQLiteDatabase dbctx = this.getReadableDatabase();
		
		keys.clear();
		values.clear();
		
		Cursor c = dbctx.rawQuery(
				"SELECT a." + KEY_ID + ", a." + KEY_DESC + ", b." + KEY_IMAGE + " " +
				"FROM " + TABLE_GALLERIES + " AS a, " + TABLE_RECENTS + " AS b " +
				"WHERE a." + KEY_IDX + " = b." + KEY_GALLID + " " +
				"ORDER BY " + KEY_REFS + " DESC",
				null
		);
		
		c.moveToFirst();
		while (!c.isAfterLast()) {
			keys.add(c.getString(0));
			values.add(c.getString(1));
			urls.add(c.getString(2));
			c.moveToNext();
		}
		
		c.close();
		close();
	}
	
	public void setFavorites(String[] list) {
		SQLiteDatabase dbctx = this.getReadableDatabase();
		
		for (String s : list) {
			Cursor c = dbctx.query(
					TABLE_GALLERIES,
					new String[] { KEY_IDX },
					KEY_ID + " = '" + s + "'",
					null,
					null,
					null,
					null
			);
			
			if (c.getCount() == 0)
				continue;
			
			c.moveToFirst();
			
			ContentValues cv = new ContentValues();
			cv.put(KEY_GALLID, new Integer(c.getInt(0)));
			cv.put(KEY_REFS, new Integer(0));
			
			Cursor ci = dbctx.query(
					TABLE_RECENTS,
					new String[] { "COUNT(*)" },
					KEY_GALLID + " = " + c.getInt(0),
					null,
					null,
					null,
					null
			);
			
			if (ci.getCount() == 1) {
				ci.moveToFirst();
				
				if (ci.getInt(0) == 0)
					dbctx.insert(TABLE_RECENTS, "", cv);
				
				ci.close();
			}
			
			c.close();
		}
		
		close();
	}
	
	public void wipeFavorites() {
		SQLiteDatabase dbctx = getWritableDatabase();
		
		dbctx.delete(TABLE_RECENTS, null, null);
		
		close();
	}
	
	public void wipeFavorite(String id) {
		int nid = getId(id);
		
		SQLiteDatabase dbctx = this.getWritableDatabase();
				
		dbctx.delete(TABLE_RECENTS, KEY_GALLID + " = " + nid, null);
		
		close();
	}
	
	public void incrementHit(String name) {
		int nid = getId(name);
		
		SQLiteDatabase dbctx = this.getWritableDatabase();
		
		dbctx.execSQL("UPDATE " + TABLE_RECENTS + " SET " + KEY_REFS + " = " + KEY_REFS + " + 1 WHERE " + KEY_GALLID + " = " + nid);
		
		close();
	}
	
	public String getImage(String name) {
		int nid = getId(name);
		
		if (nid == -1)
			return null;
		
		SQLiteDatabase dbctx = this.getReadableDatabase();
		
		Cursor c = dbctx.query(
				TABLE_RECENTS,
				new String[] { KEY_IMAGE },
				KEY_GALLID + " = " + nid,
				null,
				null,
				null,
				null
		);
		
		if (c.getCount() == 0)
			return null;
		
		c.moveToFirst();
		String out = c.getString(0);
		c.close();
		
		close();
		
		if (out == null || out.length() == 0)
			return null;
		
		return out;
	}
	
	public void setImage(String name, String url) {
		int nid = getId(name);
		
		if (nid == -1)
			return;
		
		SQLiteDatabase dbctx = this.getWritableDatabase();
		
		dbctx.execSQL("UPDATE " + TABLE_RECENTS + " SET " + KEY_IMAGE + " = '" + url + "' WHERE " + KEY_GALLID + " = " + nid);
		
		close();
	}
	
	public String getTitle(String id) {
		SQLiteDatabase dbctx = this.getReadableDatabase();
		
		Cursor c = dbctx.query(
				TABLE_GALLERIES,
				new String[] { KEY_DESC },
				KEY_ID + " = '" + id + "'",
				null,
				null,
				null,
				null
		);
		
		if (c.getCount() == 0)
			return "";
		
		c.moveToFirst();
		String output = c.getString(0);
		c.close();
		
		close();
		
		return output;
	}
}
