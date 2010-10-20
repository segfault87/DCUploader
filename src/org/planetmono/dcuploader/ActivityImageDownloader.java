package org.planetmono.dcuploader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import android.app.Activity;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class ActivityImageDownloader extends Activity {
	static private final String REFERER_URL = "http://gall.dcinside.com/list.php";
	
	private String downloadPath = null;
	
	private Uri downloadUrl = null;
	
	Handler finished = new Handler() {
		@Override
		public void handleMessage(Message m) {
			String result;
			if (m.getData().getBoolean("result", false))
				result = "다운로드를 성공하였습니다.";
			else
				result = "다운로드를 실패하였습니다.";
			
			Toast.makeText(ActivityImageDownloader.this, result, Toast.LENGTH_SHORT).show();
				
			finish();
			
			if (getLooper() != Looper.getMainLooper())
				getLooper().quit();
		}
	};
	
	Runnable downloaderThread = new Runnable() {
		private MediaScannerConnection msc = null;
		
		public void run() {
			Message m = finished.obtainMessage();
			
			HttpGet get = new HttpGet(downloadUrl.toString());
			get.setHeader("Referer", REFERER_URL);
			
			File imageDir = new File(Environment.getExternalStorageDirectory(), downloadPath);
			imageDir.mkdir();
			
			HttpResponse resp;
			try {
				resp = ((Application)getApplication()).sendGetRequest(get);
			} catch (Exception e) {
				e.printStackTrace();
				finished.handleMessage(m);
				return;
			}
			
			Header cd[] = resp.getHeaders("Content-Disposition");
			if (cd.length == 0) {
				Log.d(Application.TAG, "No Content-Disposition header");
				finished.handleMessage(m);
				return;
			}
			
			Pattern pattern = Pattern.compile("filename=\"(.*?)\"");
			Matcher matcher = null;
			try {
				matcher = pattern.matcher(new String(cd[0].getValue().getBytes("ISO-8859-1"), "EUC-KR"));
			} catch (UnsupportedEncodingException e1) {}
			
			String filename;
			if (!matcher.find()) {
				Log.d(Application.TAG, "No filename");
				filename = getIntent().getData().getQueryParameter("f_no");
			} else {
				filename = matcher.group(1);
				String lfn = filename.toLowerCase();
				if (!lfn.endsWith(".jpg") && !lfn.endsWith(".jpeg") &&
					!lfn.endsWith(".png") && !lfn.endsWith(".gif") &&
					!lfn.endsWith(".bmp"))
					filename = filename.substring(0, filename.length() - 1);
				Log.d(Application.TAG, "Filename: " + filename);
			}
			
			File of = null;
			FileOutputStream os = null;
			try {
				of = new File(imageDir, filename);
				os = new FileOutputStream(of);
			} catch (FileNotFoundException e) {} // Not likely to happen
			
			InputStream is;
			try {
				is = resp.getEntity().getContent();
			} catch (Exception e) {
				Log.d(Application.TAG, "Could open input stream: " + e.toString());
				finished.handleMessage(m);
				return;
			}
			byte data[] = new byte[16384];
			int read;
			try {
				while ((read = is.read(data)) > 0) {
					os.write(data, 0, read);
				}
			} catch (IOException e) {
				Log.d(Application.TAG, "Could open download: " + e.toString());
				finished.handleMessage(m);
				return;
			}
			
			try {
				os.close();
				is.close();
			} catch (IOException e) {}
			
			final String ofn = of.getAbsolutePath();
			
			File nomedia = new File(imageDir, ".nomedia");
			if (!nomedia.exists()) {
				MediaScannerConnectionClient mscc = new MediaScannerConnectionClient() {
					public void onMediaScannerConnected() {
						msc.scanFile(ofn, null);
					}
				
					public void onScanCompleted(String path, Uri uri) {}				
				};
			
				msc = new MediaScannerConnection(ActivityImageDownloader.this, mscc);
				msc.connect();
			}
			
			m.getData().putBoolean("result", true);
			finished.handleMessage(m);
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		
		downloadPath = getSharedPreferences(Application.APP, Activity.MODE_PRIVATE).getString(ActivityPreferences.KEY_DOWNLOAD_PATH, ActivityPreferences.DEFAULT_DOWNLOAD_PATH);
		
		setContentView(R.layout.downloader);
		
		downloadUrl = getIntent().getData();
		new LooperDelegate(downloaderThread).start();
	}
}
