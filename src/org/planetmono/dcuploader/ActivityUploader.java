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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;

import org.planetmono.dcuploader.exceptions.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class ActivityUploader extends Activity {
	private static final String GALL_URL = "http://gall.dcinside.com";
	private static final String FORM_URL = "http://gall.dcinside.com/article_write.php";
	private static final String LOGIN_STATUS_URL = "http://gn.dcinside.com/gn_top_gall_right_write.php";
	private static final String UPLOAD_URL = "http://upload.dcinside.com/g_write.php";
	private static final String UPLOAD_IMAGE_URL = "http://upload.dcinside.com/upload_imgFree.php";
	private static final String AGE_VERIFICATION_URL = "http://cert.namecheck.co.kr/certnc_inner_write.asp";

	private static final int MENU_GROUP_SELECT_GALLERY = 0;
	private static final int MENU_ADD_GALLERY = -1;

	private static final int MENU_GROUP_VISIT = 1;
	private static final int MENU_DCINSIDE = 0;
	private static final int MENU_MOBILE = 1;

	private static final String STAMP_MODEL = " (using " + Build.MODEL + ")";

	private int maximumResolution;
	private int imageQuality;
	private int sizeThreshold;
	private String mobilePageProvider;
	private String pageDestination;

	private ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();
	private String postfix = "";
	private String mapstring = "";
	private int destination = 0;
	private int targetNo = 0;
	private boolean called = false;
	private boolean passThrough = false;
	private boolean galleryChanged = false;
	
	/* must be kept */
	private File tempFile = null;
	private String target = null;
	private ArrayList<Uri> contents = new ArrayList<Uri>();
	private ArrayList<String> tempFiles = new ArrayList<String>();

	/* progress dialogs */
	private GenericProgressHandler uploadProgressDialog = new GenericProgressHandler(
			ActivityUploader.this, "업로드 중");
	private GenericProgressHandler signOffProgressDialog = new GenericProgressHandler(
			ActivityUploader.this, "로그아웃 중");

	/* For saving form data when orientation changes */
	String formGallery = null;
	String formTitle = null;
	String formBody = null;
	boolean formLocation = false;
	
	private boolean sign = true;
	private String customSign = null;
	private boolean signModel = false;

	/* Location listener */
	private boolean locationEnabled = false;
	private Location currentLocation = null;

	LocationListener locationTracker = new LocationListener() {
		public void onLocationChanged(Location location) {
			currentLocation = location;
			((Button) findViewById(R.id.upload_ok)).setEnabled(true);
			findViewById(R.id.upload_location_progress).setVisibility(
					View.INVISIBLE);
		}

		public void onProviderDisabled(String provider) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};

	/* when pressed 'OK' button */
	OnClickListener proceedHandler = new OnClickListener() {
		public void onClick(View v) {
			Application app = (Application) getApplication();

			EditText title, contents;
			title = (EditText) findViewById(R.id.upload_title);
			contents = (EditText) findViewById(R.id.upload_text);
			
			DialogInterface.OnClickListener simpleCloser = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			};
			
			if (target == null) {
				((EditText)findViewById(R.id.upload_target)).setText("");
				
				new AlertDialog.Builder(ActivityUploader.this).setTitle("오류")
					.setMessage("갤러리를 선택해 주십시오.")
					.setNeutralButton("확인", simpleCloser)
					.show();
				
				return;
			}

			if (title.getText().length() == 0 || contents.getText().length() == 0) {
				new AlertDialog.Builder(ActivityUploader.this).setTitle("오류")
					.setMessage("내용을 입력해 주십시오.")
					.setNeutralButton("확인", simpleCloser)
					.show();
				
				return;
			}

			if (!app.isSignedOn()) {
				startActivityForResult(new Intent(ActivityUploader.this,
						ActivitySignOn.class), Application.ACTION_SIGN_ON);
			} else {
				publish();
			}
		}
	};

	/* class for binding image data into the image view */
	private class ImageUriAdapter extends BaseAdapter {
		private int gallery_background;

		public ImageUriAdapter() {
			TypedArray a = obtainStyledAttributes(R.styleable.default_gallery);
			gallery_background = a.getResourceId(
					R.styleable.default_gallery_android_galleryItemBackground,
					0);
		}

		public int getCount() {
			return contents.size();
		}

		public Object getItem(int position) {
			return contents.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView iv = new ImageView(ActivityUploader.this);

			iv.setImageBitmap(bitmaps.get(position));
			iv.setAdjustViewBounds(true);
			iv.setBackgroundResource(gallery_background);

			return iv;
		}

	};

	private int queryOrientation(Uri uri) {
		Log.d(Application.TAG, "querying " + uri.toString());

		ContentResolver cr = ActivityUploader.this.getContentResolver();

		int orientation = 0;

		Cursor c = cr.query(uri,
				new String[] { MediaStore.Images.ImageColumns.ORIENTATION, },
				null, null, null);

		if (c != null) {
			c.moveToFirst();
			orientation = c.getInt(0);
			c.close();

			return orientation;
		}

		/* If there's no such item in ContentResolver, query EXIF */
		return queryExifOrientation(uri.getPath());
	}

	static private Constructor<?> exifConstructor = null;
	static private Method exifGetAttributeInt = null;

	public int queryExifOrientation(String path) {
		int orientation = 0;

		if (exifConstructor == null) {
			try {
				exifConstructor = ExifInterface.class
						.getConstructor(new Class[] { String.class });
				exifGetAttributeInt = ExifInterface.class.getMethod(
						"getAttributeInt", new Class[] { String.class,
								int.class });
			} catch (NoSuchMethodException e) {
				return 0;
			}
		}

		Integer o = 0;
		try {
			Object obj = exifConstructor.newInstance(new Object[] { path });
			o = (Integer) exifGetAttributeInt.invoke(obj,
					ExifInterface.TAG_ORIENTATION, new Integer(0));
		} catch (Exception e) {
			return 0;
		}

		Log.d(Application.TAG, "orientation " + o);

		switch (o) {
		case ExifInterface.ORIENTATION_ROTATE_90:
			orientation = 90;
			break;
		case ExifInterface.ORIENTATION_ROTATE_180:
			orientation = 180;
			break;
		case ExifInterface.ORIENTATION_ROTATE_270:
			orientation = 270;
		}

		return orientation;
	}

	private Bitmap getBitmapThumbnail(Uri uri) {
		Bitmap b = null;
		try {
			b = BitmapFactory.decodeStream(getContentResolver()
					.openInputStream(uri));
		} catch (FileNotFoundException e1) {
			try {
				b = BitmapFactory.decodeStream(new FileInputStream(uri
						.getPath()));
			} catch (FileNotFoundException e2) {
				return Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_4444);
			}
		}

		int orientation = queryOrientation(uri);

		Bitmap resized = BitmapHelper.getResizedBitmap(b, 64,
				BitmapHelper.Axis.Vertical, orientation);
		b.recycle();

		return resized;
	}

	private void reloadConfigurations() {
		SharedPreferences pref = getSharedPreferences(Application.APP,
				Activity.MODE_PRIVATE);

		maximumResolution = pref.getInt(
				ActivityPreferences.KEY_MAXIMUM_RESOLUTION,
				ActivityPreferences.DEFAULT_MAXIMUM_RESOLUTION);
		imageQuality = pref.getInt(ActivityPreferences.KEY_IMAGE_QUALITY,
				ActivityPreferences.DEFAULT_IMAGE_QUALITY);
		sizeThreshold = pref.getInt(
				ActivityPreferences.KEY_IMAGE_RESIZE_THRESHOLD,
				ActivityPreferences.DEFAULT_IMAGE_RESIZE_THRESHOLD);
		mobilePageProvider = pref.getString(
				ActivityPreferences.KEY_MOBILE_PAGE_PROVIDER,
				ActivityPreferences.DEFAULT_MOBILE_PAGE_PROVIDER);
		pageDestination = pref.getString(ActivityPreferences.KEY_DESTINATION,
				ActivityPreferences.DEFAULT_DESTINATION);

		formLocation = pref.getBoolean(
				ActivityPreferences.KEY_ALWAYS_ENCLOSE_POSITION,
				ActivityPreferences.DEFAULT_ALWAYS_ENCLOSE_POSITION);
		
		
		sign = pref.getBoolean(
				ActivityPreferences.KEY_SIGNATURE,
				ActivityPreferences.DEFAULT_SIGNATURE);
		boolean use_custom_signature = pref.getBoolean(
				ActivityPreferences.KEY_USE_CUSTOM_SIGNATURE,
				ActivityPreferences.DEFAULT_USE_CUSTOM_SIGNATURE);
		if (use_custom_signature)
			customSign = pref.getString(
					ActivityPreferences.KEY_CUSTOM_SIGNATURE,
					"");
		signModel = pref.getBoolean(
				ActivityPreferences.KEY_ALWAYS_ENCLOSE_MODEL,
				ActivityPreferences.DEFAULT_ALWAYS_ENCLOSE_MODEL);

		((CheckBox) findViewById(R.id.upload_enclose_position))
				.setChecked(formLocation);
	}

	private void resetThumbnails() {
		bitmaps.clear();

		for (int i = 0; i < contents.size(); ++i)
			bitmaps.add(getBitmapThumbnail(contents.get(i)));
	}

	private void setDefaultImage() {
		if (galleryChanged || target == null)
			return;

		contents.clear();
		bitmaps.clear();

		DatabaseHelper db = new DatabaseHelper(this);
		String path = db.getImage(target);
		db.close();

		if (path != null && path.length() > 0)
			contents.add(Uri.parse(path));

		updateImageButtons();
		resetThumbnails();
		updateGallery();
	}

	private void updateGallery() {
		Gallery g = (Gallery) findViewById(R.id.upload_images);

		ImageUriAdapter ua = new ImageUriAdapter();
		g.setAdapter(ua);
	}

	private void updateImageButtons() {
		Button ba = (Button) findViewById(R.id.upload_photo_add);
		Button bd = (Button) findViewById(R.id.upload_photo_delete);
		Button bt = (Button) findViewById(R.id.upload_photo_take);

		if (contents.size() == 5) {
			bt.setEnabled(false);
			ba.setEnabled(false);
			bd.setEnabled(true);
		} else if (contents.size() == 0) {
			galleryChanged = false;
			bt.setEnabled(true);
			ba.setEnabled(true);
			bd.setEnabled(false);
		} else {
			bt.setEnabled(true);
			ba.setEnabled(true);
			bd.setEnabled(true);
		}
	}

	Runnable onSuccess = new Runnable() {
		public void run() {
			Intent i = new Intent(ActivityUploader.this, ActivityResult.class);
			i.putExtra("target", target);
			i.putExtra("no", targetNo);
			i.putExtra("called", called);
			i.putExtra("destination", destination);

			finish();
			startActivity(i);
		}
	};

	Runnable reSignOn = new Runnable() {
		public void run() {
			startActivityForResult(new Intent(ActivityUploader.this,
					ActivitySignOn.class), Application.ACTION_SIGN_ON);
		}
	};

	Handler signOffResultHandler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			signOffProgressDialog.stop();

			if (m.getData().getBoolean("result")) {
				Application app = (Application) getApplication();
				app.setSignedOn(false, app.authenticationMethod());
			}
		}
	};

	/*
	 * Uploader procedure
	 */

	Runnable publisherThread = new Runnable() {
		private void uploadImages(MultipartEntity flout)
				throws TemporarilyUnavailable, PageError {
			if (contents.size() == 0)
				return;

			HttpPost post = new HttpPost(UPLOAD_IMAGE_URL);
			MultipartEntity entity = new MultipartEntity();
			ContentResolver cr = ActivityUploader.this.getContentResolver();
			Application app = (Application) getApplication();

			try {
				entity.addPart("imgId", new StringBody(target, Charset
						.forName("utf-8")));
				entity.addPart("mode", new StringBody("write", Charset
						.forName("utf-8")));
			} catch (Exception e) {
			}

			ArrayList<InputStream> streams = new ArrayList<InputStream>();
			int cnt = 0;
			for (Uri uri : contents) {
				boolean localfile = false;
				if (uri.getScheme().equals("file"))
					localfile = true;

				int size = 0;
				String mime = "image/jpeg";
				int orientation = 0;
				String filename = "";
				boolean nodata = false;
				if (!localfile) {
					Cursor c = cr.query(uri, new String[] {
							MediaStore.Images.ImageColumns.SIZE,
							MediaStore.Images.ImageColumns.MIME_TYPE,
							MediaStore.Images.ImageColumns.ORIENTATION,
							MediaStore.MediaColumns.DISPLAY_NAME }, null, null,
							null);

					if (c != null) {
						c.moveToFirst();
						size = c.getInt(0);
						mime = c.getString(1);
						orientation = c.getInt(2);
						filename = c.getString(3);
						c.close();
					} else {
						Log.d(Application.TAG, "file " + uri.toString()
								+ " not found.");
						nodata = true;
					}
				}

				/* not in contentprovider database */
				if (localfile || nodata) {
					size = (int) new File(uri.getPath()).length();
					orientation = queryOrientation(uri);
					filename = uri.getLastPathSegment();

					String lfn = filename.toLowerCase();
					if (lfn.endsWith(".jpg") || lfn.endsWith(".jpeg"))
						mime = "image/jpeg";
					else if (lfn.endsWith(".png"))
						mime = "image/png";
					else if (lfn.endsWith(".gif"))
						mime = "image/gif";
					else if (lfn.endsWith(".bmp"))
						mime = "image/bmp";
					else
						mime = "image/jpeg"; // presume
				}

				String lfn = filename.toLowerCase();

				if (mime.equals("image/jpeg")) {
					if (!lfn.endsWith(".jpg") && !lfn.endsWith(".jpeg"))
						filename += ".jpg";
				} else if (mime.equals("image/png")) {
					if (!lfn.endsWith(".png"))
						filename += ".png";
				} else if (mime.equals("image/gif")) {
					if (!lfn.endsWith(".gif"))
						filename += ".gif";
				} else {
					if (!filename.contains("."))
						filename += ".jpg"; // presume
				}

				InputStream is;

				if (size > sizeThreshold || orientation != 0) {
					/* convert image file */

					Log.d(Application.TAG, "image " + filename
							+ " needs to be converted.");

					try {
						File f = File.createTempFile("dcuploader_resize",
								".jpg");
						String path = f.getAbsolutePath();
						tempFiles.add(path);

						mime = "image/jpeg";

						FileOutputStream fo = new FileOutputStream(f);
						Bitmap o = BitmapFactory.decodeStream(cr
								.openInputStream(uri));
						Bitmap b = BitmapHelper.getResizedBitmapConstrained(o,
								maximumResolution, orientation);
						b.compress(CompressFormat.JPEG, imageQuality, fo);
						o.recycle();
						b.recycle();
						fo.close();

						is = new FileInputStream(path);
					} catch (Exception e) {
						e.printStackTrace();

						continue;
					}
				} else {
					try {
						is = cr.openInputStream(uri);
					} catch (FileNotFoundException e) {
						Log.d(Application.TAG, "file not found: "
								+ uri.toString());
						e.printStackTrace();

						continue;
					}
				}

				streams.add(is);

				try {
					entity.addPart("file_txt" + (cnt + 1), new StringBody(
							filename, Charset.defaultCharset()));
				} catch (Exception e) {} /* this is not going to happen */
				
				entity.addPart("upload[" + cnt + "]", new InputStreamBody(is,
						mime, filename));

				Log.d(Application.TAG, "Image " + filename + " added.");

				++cnt;
			}

			post.setEntity(entity);
			post.setHeader("Referer", FORM_URL + "?id=" + target);
			post.setHeader("Origin", GALL_URL);

			HttpResponse resp;
			try {
				resp = app.sendPostRequest(post);

				/* clean up all the mess */
				for (InputStream is : streams)
					is.close();
				for (String path : tempFiles)
					new File(path).delete();
				tempFiles.clear();
			} catch (SocketTimeoutException e) {
				throw new TemporarilyUnavailable();
			} catch (Exception e) {
				uploadProgressDialog.error(e.toString());
				return;
			}

			HttpEntity resultEntity = resp.getEntity();
			BufferedReader r;
			try {
				r = new BufferedReader(new InputStreamReader(resultEntity
						.getContent(), "UTF-8"));
			} catch (Exception e) {
				e.printStackTrace();
				uploadProgressDialog.error(e.toString());

				return;
			}

			String fldata = "";
			String ofldata = "";
			while (true) {
				String line;
				try {
					line = r.readLine();
				} catch (IOException e) {
					e.printStackTrace();

					throw new PageError(e.toString());
				}
				if (line == null)
					break;

				if (line.contains("\"FL_DATA\"")) {
					fldata = line.substring(line.indexOf('\''))
							.replace("'", "").replace(";", "").trim();
				} else if (line.contains("\"OFL_DATA\"")) {
					ofldata = line.substring(line.indexOf('\'')).replace("'",
							"").replace(";", "").trim();
				}
			}

			Log.d(Application.TAG, "Images uploaded successfully.");

			try {
				flout.addPart("FL_DATA", new StringBody(fldata, Charset
						.defaultCharset()));
				flout.addPart("OFL_DATA", new StringBody(ofldata, Charset
						.defaultCharset()));

				String rot = "0";
				for (int i = 1; i < cnt; ++i)
					rot += ";" + i;
				flout.addPart("rot", new StringBody(rot, Charset
						.defaultCharset()));
			} catch (UnsupportedEncodingException e) {
			}
		}

		private void fetchFormPage(MultipartEntity mpentity) throws Exception {
			Application app = (Application) getApplication();
			HttpGet req;
			HttpResponse resp;
			HttpEntity entity;
			BufferedReader r;

			/* Get POST variables */
			req = new HttpGet(FORM_URL + "?id=" + target);
			try {
				resp = ((Application) getApplication()).sendGetRequest(req);
			} catch (SocketTimeoutException e) {
				throw new TemporarilyUnavailable();
			} catch (Exception e) {
				e.printStackTrace();
				uploadProgressDialog.error(e.toString());

				throw e;
			}

			entity = resp.getEntity();
			try {
				r = new BufferedReader(new InputStreamReader(entity
						.getContent(), "UTF-8"));
			} catch (Exception e) {
				e.printStackTrace();
				uploadProgressDialog.error(e.toString());

				throw e;
			}

			boolean errorFlag = false;
			boolean gettingErrorStr = false;
			String errorMsg = "";
			while (true) {
				String line;
				try {
					line = r.readLine();
				} catch (IOException e) {
					throw new TemporarilyUnavailable();
				}
				if (line == null)
					break;

				String key, value;

				if (line.contains(LOGIN_STATUS_URL)) {
					if (line.contains("&chk=N")
							&& app.authenticationMethod() == Application.AUTHENTICATION_METHOD_GALLOG)
						throw new TimedOut();
				} else if (line.contains(AGE_VERIFICATION_URL)) {
					if (app.authenticationMethod() == Application.AUTHENTICATION_METHOD_GALLOG)
						throw new TimedOut();
					else
						throw new AgeVerificationNeeded();
				} else if (line.contains("사용권한이 없습니다")) {
					throw new NoPermission();
				} else if (line.contains("<input type=hidden")) {
					Pattern p;
					Matcher m;

					p = Pattern.compile("name=([\"_a-zA-Z0-9]+)");
					m = p.matcher(line);
					m.find();
					key = m.group(1).replace("\"", "").trim();

					/* we're going to add those later */
					if (key.equals("FL_DATA") || key.equals("OFL_DATA")
							|| key.equals("rot") || key.equals("check_attack"))
						continue;

					p = Pattern.compile("value=([\"_a-zA-Z0-9]+)");
					try {
						m = p.matcher(line);
						m.find();
						value = m.group(1).replace("\"", "").trim();
					} catch (IllegalStateException e) {
						value = "";
					}

					try {
						mpentity.addPart(key, new StringBody(value, Charset
								.forName("utf-8")));
					} catch (Exception e) {
					}
				} else if (line.contains("Message")) {
					Log.d(Application.TAG, "Unexpected error.");

					errorFlag = true;
					gettingErrorStr = true;
				} else if (gettingErrorStr) {
					if (line.contains("Move Back")) {
						gettingErrorStr = false;
					} else {
						String stripped = line.replaceAll("\\<.*?>", "").trim();
						if (stripped.length() > 0)
							errorMsg += stripped + "\n";
					}
				}
			}

			if (errorFlag) {
				throw new PageError(errorMsg);
			}

			try {
				entity.consumeContent();
			} catch (IOException e) {
			}
		}

		public void run() {
			MultipartEntity entity = new MultipartEntity();

			try {
				fetchFormPage(entity);
				uploadImages(entity);
			} catch (TimedOut e) {
				Application app = (Application) getApplication();
				app.setSignedOn(false, app.authenticationMethod());
				uploadProgressDialog.stop();
				new Handler().post(reSignOn);
				return;
			} catch (AgeVerificationNeeded e) {
				uploadProgressDialog.error("성인인증 필요.");
				return;
			} catch (NoPermission e) {
				uploadProgressDialog.error("글을 쓸 권한이 없습니다.");
				return;
			} catch (TemporarilyUnavailable e) {
				uploadProgressDialog.error("서버에 접속할 수 없습니다.");
				return;
			} catch (PageError e) {
				uploadProgressDialog.error(e.toString());
				return;
			} catch (Exception e) {
				return;
			}

			EditText subject = (EditText) findViewById(R.id.upload_title);
			EditText contents = (EditText) findViewById(R.id.upload_text);
			String cstr = contents.getText().toString().trim().replace("<",
					"&lt;").replace(">", "&gt;").replace("\n", "<br />");
			
			String postfix_;
			if (sign) {
				postfix_ = "<br /><br />";
				
				if (customSign != null)
					postfix_ += customSign;
				else
					postfix_ += postfix;
				
				if (signModel)
					postfix_ += STAMP_MODEL;
			} else {
				postfix_ = "";
			}

			try {
				entity.addPart("subject", new StringBody(subject.getText()
						.toString(), Charset.forName("utf-8")));
				entity.addPart("memo", new StringBody(cstr + mapstring
						+ postfix_, Charset
						.forName("utf-8")));
			} catch (Exception e) {
			}

			HttpPost post = new HttpPost(UPLOAD_URL);
			post.setHeader("Origin", GALL_URL);
			post.setHeader("Referer", FORM_URL + "?id=" + target);
			post.setEntity(entity);

			Log.d(Application.TAG, "Publishing article...");

			Application app = (Application) getApplication();
			HttpResponse resp;
			try {
				resp = app.sendPostRequest(post);
			} catch (Exception e) {
				uploadProgressDialog.error(e.toString());
				return;
			}

			HttpEntity rentity = resp.getEntity();
			BufferedReader r;
			try {
				r = new BufferedReader(new InputStreamReader(rentity
						.getContent(), "UTF-8"));
			} catch (Exception e) {
				e.printStackTrace();
				uploadProgressDialog.error(e.toString());

				return;
			}

			boolean errorFlag = false;
			boolean gettingErrorStr = false;
			String errorMsg = "";
			while (true) {
				String line;
				try {
					line = r.readLine();
				} catch (IOException e) {
					e.printStackTrace();
					uploadProgressDialog.error(e.toString());

					return;
				}
				if (line == null)
					break;

				Log.d(Application.TAG, line);

				if (line.contains("Message")) {
					errorFlag = true;
					gettingErrorStr = true;
				} else if (gettingErrorStr) {
					if (line.contains("Move Page")) {
						gettingErrorStr = false;
					} else {
						String stripped = line.replaceAll("\\<.*?>", "").trim();
						if (stripped.length() > 0)
							errorMsg += stripped + "\n";
					}
				} else if (line.contains("http-equiv=\"refresh\"")) {
					Pattern p = Pattern.compile("no=([0-9]+)");
					Matcher m = p.matcher(line);

					if (!m.find()) {
						uploadProgressDialog
								.error("알 수 없는 이유로 글이 올라가지 않은 것 같습니다.");
						return;
					}
					targetNo = Integer.parseInt(m.group(1));
				}
			}

			if (errorFlag) {
				uploadProgressDialog.error(errorMsg);
				return;
			}

			DatabaseHelper db = new DatabaseHelper(ActivityUploader.this);
			db.incrementHit(target);
			db.close();

			new Handler().post(onSuccess);

			uploadProgressDialog.stop();
		}
	};

	/* publish your form */
	private void publish() {
		/* include location info */
		if (locationEnabled && currentLocation != null) {
			double latitude = currentLocation.getLatitude();
			double longitude = currentLocation.getLongitude();

			mapstring = "<br /><br /><img src='http://maps.google.com/maps/api/staticmap?center="
					+ latitude
					+ ","
					+ longitude
					+ "&zoom=12&size=300x140&maptype=roadmap&markers=color:blue|label:|"
					+ latitude
					+ ","
					+ longitude
					+ "&markers=color:green&sensor=false' />";
		}

		uploadProgressDialog.start();

		new Handler() {
			@Override
			public void handleMessage(Message m) {
				new LooperDelegate(publisherThread).start();
			}
		}.sendEmptyMessage(0);
	}

	private void initViews() {
		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		if (wm.getDefaultDisplay().getOrientation() == 0)
			setContentView(R.layout.upload_portrait);
		else
			setContentView(R.layout.upload_landscape);

		EditText uploadTarget = (EditText) findViewById(R.id.upload_target);
		EditText uploadTitle = (EditText) findViewById(R.id.upload_title);
		EditText uploadText = (EditText) findViewById(R.id.upload_text);
		Button uploadVisit = (Button) findViewById(R.id.upload_visit);
		Button uploadPhotoTake = (Button) findViewById(R.id.upload_photo_take);
		Button uploadPhotoAdd = (Button) findViewById(R.id.upload_photo_add);
		Button uploadPhotoDelete = (Button) findViewById(R.id.upload_photo_delete);
		CheckBox uploadEnclosePosition = (CheckBox) findViewById(R.id.upload_enclose_position);
		Button uploadOk = (Button) findViewById(R.id.upload_ok);
		Button uploadCancel = (Button) findViewById(R.id.upload_cancel);

		/* set button behavior */

		if (passThrough)
			uploadTarget.setClickable(false);
		else {
			uploadTarget.setClickable(true);
			registerForContextMenu(uploadTarget);
		}

		uploadTarget.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!passThrough)
					openContextMenu(v);
			}
		});

		registerForContextMenu(uploadVisit);
		uploadVisit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				openContextMenu(v);
			}
		});

		uploadPhotoTake.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				try {
					tempFile = File.createTempFile("dcuploader_photo_", ".jpg");
				} catch (IOException e) {
					Toast.makeText(ActivityUploader.this, "임시 파일을 만들 수 없습니다.", Toast.LENGTH_SHORT).show();
					
					tempFile = null;
					
					return;
				}

				Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				if (tempFile != null)
					i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));

				startActivityForResult(i, Application.ACTION_TAKE_PHOTO);
			}
		});

		uploadPhotoAdd.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_GET_CONTENT);
				i.setType("image/*");
				i.addCategory(Intent.CATEGORY_DEFAULT);

				startActivityForResult(i, Application.ACTION_ADD_PHOTO);
			}
		});

		uploadPhotoDelete.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Gallery g = (Gallery) findViewById(R.id.upload_images);

				int pos = g.getSelectedItemPosition();
				if (pos == -1)
					return;

				contents.remove(pos);
				bitmaps.remove(pos);

				updateGallery();
				updateImageButtons();

				if (contents.size() == 0)
					pos = -1;
				else if (pos >= contents.size())
					--pos;

				g.setSelection(pos);
			}
		});

		uploadOk.setOnClickListener(proceedHandler);

		uploadCancel.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

		uploadEnclosePosition.setChecked(formLocation);

		uploadEnclosePosition
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						locationEnabled = isChecked;

						queryLocation(isChecked);
					}
				});

		/* restore data when orientation changes */
		if (formGallery != null) {
			if (target != null) {
				uploadTarget.setText(formGallery);
				formGallery = null;
			}
		}

		if (formTitle != null) {
			uploadTitle.setText(formTitle);
			formTitle = null;
		}

		if (formBody != null) {
			uploadText.setText(formBody);
			formBody = null;
		}

		updateImageButtons();
		updateGallery();
	}

	private void queryLocation(boolean enabled) {
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		if (enabled) {
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
					locationTracker);
			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0,
					locationTracker);
			((Button) findViewById(R.id.upload_ok)).setEnabled(false);
			findViewById(R.id.upload_location_progress).setVisibility(
					View.VISIBLE);
		} else {
			lm.removeUpdates(locationTracker);
			((Button) findViewById(R.id.upload_ok)).setEnabled(true);
			findViewById(R.id.upload_location_progress).setVisibility(
					View.INVISIBLE);
		}
	}

	private void visitPage(int id) {
		Uri uri;
		String queryString = "?id=" + target;

		if (id == MENU_DCINSIDE)
			uri = Uri.parse(Application.URL_LIST_DCINSIDE + queryString);
		else {
			if (mobilePageProvider.equals("moolzo"))
				uri = Uri.parse(Application.URL_LIST_MOOLZO + queryString);
			else
				uri = Uri.parse(Application.URL_LIST_DCMYS + queryString);
		}

		startActivity(new Intent(Intent.ACTION_VIEW, uri));
	}
	
	private void resolveTarget(String id) {
		target = id;

		DatabaseHelper db = new DatabaseHelper(this);
		String title = db.getTitle(id);
		db.close();

		if (title.length() > 0)
			((EditText) findViewById(R.id.upload_target))
					.setText(title + " (" + id + ")");
		else
			((EditText) findViewById(R.id.upload_target))
					.setText(target);
	}

	/*
	 * UI definition
	 */
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		if (tempFile != null)
			outState.putString("tempfile", tempFile.getAbsolutePath());
		if (target != null)
			outState.putString("target", target);
		if (tempFiles.size() > 0)
			outState.putStringArrayList("tempfiles", tempFiles);
		if (contents.size() > 0) {
			String[] carr = new String[contents.size()];
			
			int t = 0;
			for (Uri u : contents)
				carr[t++] = u.toString();
			
			outState.putStringArray("contents", carr);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedState) {
		super.onCreate(savedState);

		initViews();
		if (formLocation)
			queryLocation(true);
		
		if (savedState != null) {
			if (savedState.containsKey("tempfile"))
				tempFile = new File(savedState.getString("tempfile"));
			if (savedState.containsKey("target"))
				resolveTarget(savedState.getString("target"));
			if (savedState.containsKey("tempfiles"))
				tempFiles = savedState.getStringArrayList("tempfiles");
			if (savedState.containsKey("contents")) {
				contents = new ArrayList<Uri>();
				String[] carr = savedState.getStringArray("contents");
				for (String s : carr)
					contents.add(Uri.parse(s));
			}
		}
		
		postfix = "from <a href=\"http://palladium.planetmono.org/dcuploader\">DCUploader</a>";
		
		Button uploadVisit = (Button) findViewById(R.id.upload_visit);
		if (passThrough || target == null)
			uploadVisit.setEnabled(false);
		else
			uploadVisit.setEnabled(true);

		/* populate data by getting STREAM parameter */
		Intent i = getIntent();
		Bundle b = i.getExtras();
		String action = i.getAction();

		if (action.equals(Intent.ACTION_SEND)
				|| action.equals(Intent.ACTION_SEND_MULTIPLE)) {
			called = true;

			if (i.hasExtra(Intent.EXTRA_STREAM)) {
				Object o = b.get(Intent.EXTRA_STREAM);

				/* quick and dirty. any better idea? */
				try {
					contents.add((Uri) o);
				} catch (Exception e1) {
					try {
						contents = (ArrayList<Uri>) ((ArrayList<Uri>) o)
								.clone();
					} catch (Exception e2) {
					}
				}

				boolean exceeded = false;
				if (contents.size() > 5) {
					exceeded = true;

					do {
						contents.remove(5);
					} while (contents.size() > 5);
				}

				galleryChanged = true;

				updateImageButtons();
				resetThumbnails();
				updateGallery();

				if (exceeded)
					Toast.makeText(this,
							"최대 5개까지만 업로드가 가능합니다. 5개 이상의 항목은 제거됩니다.",
							Toast.LENGTH_LONG).show();
			}
			if (i.hasExtra(Intent.EXTRA_TEXT)) {
				((EditText) findViewById(R.id.upload_text)).setText(b
						.getString(Intent.EXTRA_TEXT));
			}
		} else if (action.equals("share")) {
			called = true;
			/* HTC web browser uses non-standard intent */

			((EditText) findViewById(R.id.upload_text)).setText(b
					.getString(Intent.EXTRA_TITLE));
		} else if (action.equals(Intent.ACTION_VIEW)) {
			Uri uri = i.getData();

			if (i.getCategories().contains(Intent.CATEGORY_BROWSABLE)) {
				passThrough = true;

				Pattern p = Pattern.compile("id=([\\-a-zA-Z0-9_]+)");
				Matcher m = p.matcher(uri.toString());

				if (m.find()) {
					resolveTarget(m.group(1));
				} else {
					passThrough = false;
				}

				if (uri.getHost().equals(Application.HOST_DCMYS)) {
					destination = Application.DESTINATION_DCMYS;
					postfix = "from dc.m.dcmys.kr w/ <a href=\"http://palladium.planetmono.org/dcuploader\">DCUploader</a>";
				} else if (uri.getHost().equals(Application.HOST_MOOLZO)) {
					destination = Application.DESTINATION_MOOLZO;
					postfix = "- From m.oolzo.com w/ <a href=\"http://palladium.planetmono.org/dcuploader\">DCUploader</a>";
				} else if (uri.getHost().equals(Application.HOST_DCINSIDE)) {
					destination = Application.DESTINATION_DCINSIDE;
				}
				
				setDefaultImage();
			}
		}

		reloadConfigurations();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		Log.d(Application.TAG, "stopping activity.");

		/* stop location query when going out */
		if (locationEnabled) {
			LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			lm.removeUpdates(locationTracker);
		}

		for (String path : tempFiles)
			new File(path).delete();

		tempFiles.clear();
	}

	@Override
	public void onResume() {
		super.onResume();

		/* resume location query */
		if (locationEnabled) {
			LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
					locationTracker);
			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0,
					locationTracker);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == Application.ACTION_ADD_PHOTO
					|| requestCode == Application.ACTION_TAKE_PHOTO) {
				/* add photo */

				if (contents.size() == 5)
					return;

				Uri selectedImage;
				Bitmap tbm;

				if (requestCode == Application.ACTION_ADD_PHOTO) {
					selectedImage = data.getData();
					tbm = getBitmapThumbnail(selectedImage);
				} /*else if (requestCode == Application.ACTION_TAKE_PHOTO && data != null && data.hasExtra("data")) {
					Bundle b = data.getExtras();
					String mpath = null;

					Bitmap bitmap = (Bitmap) b.get("data");
					try {
						if (tempFile == null) {
							File f = File.createTempFile("dcuploader_photo_",
									".jpg", Environment
											.getDownloadCacheDirectory());
							FileOutputStream fo = new FileOutputStream(f);
							mpath = f.getAbsolutePath();

							bitmap.compress(CompressFormat.JPEG, 90, fo);
							fo.close();
						} else {
							mpath = tempFile.getAbsolutePath();
							tempFile = null;
						}
					} catch (IOException e) {
						e.printStackTrace();

						return;
					}

					selectedImage = Uri.parse("file://" + mpath);
					tempFiles.add(mpath);
					tbm = getBitmapThumbnail(bitmap);

					bitmap.recycle();
				} */else {
					/* take photo */
					selectedImage = Uri.fromFile(tempFile);
					tempFiles.add(tempFile.getAbsolutePath());
					tbm = getBitmapThumbnail(selectedImage);
				}

				Gallery g = (Gallery) findViewById(R.id.upload_images);

				int npos = 0;
				int pos = g.getSelectedItemPosition();

				if (pos == -1 || pos == 3) {
					contents.add(selectedImage);
					bitmaps.add(tbm);
					if (pos == 3)
						npos = pos + 1;
				} else {
					contents.add(pos + 1, selectedImage);
					bitmaps.add(pos + 1, tbm);

					npos = pos + 1;
				}

				updateGallery();
				updateImageButtons();
				g.setSelection(npos);

				galleryChanged = true;
			} else if (requestCode == Application.ACTION_ADD_GALLERY) {
				/* add gallery */

				DatabaseHelper db = new DatabaseHelper(ActivityUploader.this);
				db.setFavorites(data.getStringArrayExtra("result"));
				db.close();

				openContextMenu(findViewById(R.id.upload_target));
			} else if (requestCode == Application.ACTION_SIGN_ON) {
				/* when signed on, go on. */

				publish();
			} else if (requestCode == Application.ACTION_PREFERENCES) {
				/* apply preferences */

				Log.d(Application.TAG, "reloading configuration...");

				reloadConfigurations();
				setDefaultImage();

				queryLocation(formLocation);
			}
		}
	}

	private ArrayList<String> ids = new ArrayList<String>();
	private ArrayList<String> names = new ArrayList<String>();

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (v.getId() == R.id.upload_target) {
			if (passThrough)
				return;

			menu.clear();

			DatabaseHelper db = new DatabaseHelper(ActivityUploader.this);
			db.getFavorites(ids, names);
			db.close();

			menu.setHeaderTitle("갤러리 선택");
			for (int i = 0; i < names.size(); ++i)
				menu.add(MENU_GROUP_SELECT_GALLERY, i, 0, names.get(i));
			menu.add(MENU_GROUP_SELECT_GALLERY, MENU_ADD_GALLERY, 0,
					"갤러리 추가...");
		} else if (v.getId() == R.id.upload_visit) {
			menu.clear();

			if (pageDestination.equals("dcinside")) {
				visitPage(MENU_DCINSIDE);
			} else if (pageDestination.equals("mobile")) {
				visitPage(MENU_MOBILE);
			} else {
				menu.setHeaderTitle("어디로 갈까요?");

				String dest;
				if (mobilePageProvider.equals("moolzo"))
					dest = "물조";
				else
					dest = "DCmys";

				menu.add(MENU_GROUP_VISIT, MENU_DCINSIDE, 0, "디시인사이드");
				menu.add(MENU_GROUP_VISIT, MENU_MOBILE, 0, dest);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int id = item.getItemId();
		int group = item.getGroupId();

		if (group == MENU_GROUP_SELECT_GALLERY) {
			switch (id) {
			case MENU_ADD_GALLERY:
				startActivityForResult(new Intent(this,
						ActivityGalleryChooser.class),
						Application.ACTION_ADD_GALLERY);

				return true;
			default:
				resolveTarget(ids.get(id));
				
				setDefaultImage();

				if (!passThrough)
					((Button) findViewById(R.id.upload_visit)).setEnabled(true);

				return true;
			}
		} else if (group == MENU_GROUP_VISIT) {
			visitPage(id);

			return true;
		}

		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.uploader, menu);

		Application app = (Application) getApplication();
		if (!app.isSignedOn())
			menu.findItem(R.id.menu_uploader_sign_off).setEnabled(false);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_uploader_preferences:
			Intent i = new Intent(ActivityUploader.this,
					ActivityPreferences.class);
			startActivityForResult(i, Application.ACTION_PREFERENCES);
			return true;
		case R.id.menu_uploader_sign_off:
			SignOnBase sob = new SignOnGallog(); /* TODO: subject to change */
			Application app = (Application) getApplication();

			signOffProgressDialog.start();

			new Thread(sob.getMethodSignOff(app, signOffResultHandler)).start();

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
				|| newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			formGallery = ((EditText) findViewById(R.id.upload_target))
					.getText().toString();
			formTitle = ((EditText) findViewById(R.id.upload_title)).getText()
					.toString();
			formBody = ((EditText) findViewById(R.id.upload_text)).getText()
					.toString();
			formLocation = ((CheckBox) findViewById(R.id.upload_enclose_position))
					.isChecked();

			initViews();
		}
	}
}
