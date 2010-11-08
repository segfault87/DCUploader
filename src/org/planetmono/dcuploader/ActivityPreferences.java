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

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityPreferences extends PreferenceActivity {
	/* Sign-on category */
	public static final String KEY_SAVE_ID_PASSWORD = "save_id_password";
	public static final boolean DEFAULT_SAVE_ID_PASSWORD = false;
	public static final String KEY_AUTO_SIGN_ON = "auto_sign_on";
	public static final boolean DEFAULT_AUTO_SIGN_ON = false;
	
	/* Gallery category */
	private static final String KEY_EDIT_GALLERY_LIST = "edit_gallery_list";
	private static final String KEY_WIPE_GALLERY_LIST = "wipe_gallery_list";
	
	/* Image category */
	public static final String KEY_IMAGE_RESIZE_THRESHOLD = "image_resize_threshold";
	public static final int DEFAULT_IMAGE_RESIZE_THRESHOLD = 200000;
	public static final String KEY_MAXIMUM_RESOLUTION = "maximum_resolution";
	public static final int DEFAULT_MAXIMUM_RESOLUTION = 1024;
	public static final String KEY_IMAGE_QUALITY = "image_quality";
	public static final int DEFAULT_IMAGE_QUALITY = 90;
	
	/* Downloader category */
	public static final String KEY_DOWNLOAD_PATH = "download_path";
	public static final String DEFAULT_DOWNLOAD_PATH = "dcdownloader";
	public static final String KEY_ADD_TO_PROVIDER = "add_to_provider";
	
	/* Misc category */
	public static final String KEY_MOBILE_PAGE_PROVIDER = "mobile_page_provider";
	public static final String DEFAULT_MOBILE_PAGE_PROVIDER = "boxweb";
	public static final String KEY_DESTINATION = "destination";
	public static final String DEFAULT_DESTINATION = "ask_always";
	public static final String KEY_ALWAYS_ENCLOSE_POSITION = "always_enclose_position";
	public static final boolean DEFAULT_ALWAYS_ENCLOSE_POSITION = false;
	public static final String KEY_ALWAYS_ENCLOSE_MODEL = "always_enclose_model";
	public static final boolean DEFAULT_ALWAYS_ENCLOSE_MODEL = false;
	
	/* Etc. */
	private static final String KEY_ABOUT_THIS_APP = "about_this_app";
	
	private static final String aboutString = "<h2>심심해서 만든 DCUploader</h2>" +
			"(C)2010 Park \"segfault\" J. K.<br /><br />" +
			"<p><a href='mailto:mastermind@planetmono.org'>mastermind@planetmono.org</a><br />" +
			"<a href='http://planetmono.org'>http://planetmono.org</a><br />" +
			"<a href='http://twitter.com/segfault87'>http://twitter.com/segfault87</a></p>" +
			"프로젝트 홈페이지 : <a href='http://palladium.planetmono.org/dcuploader'>http://palladium.planetmono.org/dcuploader</a><br /><br />" +
			"segfault의 안드로이드 첫 번째 프로젝트입니다. 스터디하는 셈 치고 겸사겸사 만들었습니다.<br />" +
			"만약 사용하다가 문제를 발견하셨다면 위 연락처로 알려주시면 감사하겠습니다.<br /><br />" +
			"이 소프트웨어는 자유 소프트웨어입니다. 자세한 정보는 구글 코드의 프로젝트 페이지를 참고하십시오.<br />" +
			"<a href='http://code.google.com/p/segfault-snippets'>http://code.google.com/p/segfault-snippets</a>";
	
	/* configuration items */
	private boolean itemSaveIdPw = DEFAULT_SAVE_ID_PASSWORD;
	private boolean itemAutoSignOn = DEFAULT_AUTO_SIGN_ON;
	private int itemResizeThreshold = DEFAULT_IMAGE_RESIZE_THRESHOLD;
	private int itemMaximumResolution = DEFAULT_MAXIMUM_RESOLUTION;
	private int itemImageQuality = DEFAULT_IMAGE_QUALITY;
	private String itemDownloadPath = DEFAULT_DOWNLOAD_PATH;
	private String itemMobilePageProvider = DEFAULT_MOBILE_PAGE_PROVIDER;
	private String itemDestination = DEFAULT_DESTINATION;
	private boolean itemAlwaysEnclosePosition = DEFAULT_ALWAYS_ENCLOSE_POSITION;
	private boolean itemAlwaysEncloseModel = DEFAULT_ALWAYS_ENCLOSE_MODEL;
	
	private void resetConfigurableOptions() {
		int choice = 0;
		
		CheckBoxPreference saveIdPw = (CheckBoxPreference)findPreference(KEY_SAVE_ID_PASSWORD);
		saveIdPw.setChecked(itemSaveIdPw);
		
		CheckBoxPreference autoSignOn = (CheckBoxPreference)findPreference(KEY_AUTO_SIGN_ON);
		autoSignOn.setChecked(itemAutoSignOn);
		
		ListPreference resizeThreshold = (ListPreference)findPreference(KEY_IMAGE_RESIZE_THRESHOLD);
		switch (itemResizeThreshold) {
		case 100000:
			choice = 0;
			break;
		case 200000:
			choice = 1;
			break;
		case 300000:
			choice = 2;
			break;
		case 400000:
			choice = 3;
			break;
		case 500000:
			choice = 4;
			break;
		}
		resizeThreshold.setValueIndex(choice);
		
		ListPreference maximumResolution = (ListPreference)findPreference(KEY_MAXIMUM_RESOLUTION);
		switch (itemMaximumResolution) {
		case 320:
			choice = 0;
			break;
		case 640:
			choice = 1;
			break;
		case 960:
			choice = 2;
			break;
		case 1024:
			choice = 3;
			break;
		case 1280:
			choice = 4;
			break;
		}
		maximumResolution.setValueIndex(choice);
		
		ListPreference imageQuality = (ListPreference)findPreference(KEY_IMAGE_QUALITY);
		switch (itemImageQuality) {
		case 70:
			choice = 0;
			break;
		case 80:
			choice = 1;
			break;
		case 90:
			choice = 2;
			break;
		case 95:
			choice = 3;
			break;
		}
		imageQuality.setValueIndex(choice);
		
		EditTextPreference downloadPath = (EditTextPreference)findPreference(KEY_DOWNLOAD_PATH);
		downloadPath.setText(itemDownloadPath);
		
		resetAddToProvider();
		
		if (itemMobilePageProvider.equals("moolzo"))
			choice = 2;
		else if (itemMobilePageProvider.equals("boxweb_old"))
			choice = 1;
		else
			choice = 0;
		((ListPreference)findPreference(KEY_MOBILE_PAGE_PROVIDER)).setValueIndex(choice);
		
		if (itemDestination.equals("dcinside"))
			choice = 1;
		else if (itemDestination.equals("mobile"))
			choice = 2;
		else
			choice = 0;
		((ListPreference)findPreference(KEY_DESTINATION)).setValueIndex(choice);
		
		
		CheckBoxPreference alwaysEnclosePosition = (CheckBoxPreference)findPreference(KEY_ALWAYS_ENCLOSE_POSITION);
		alwaysEnclosePosition.setChecked(itemAlwaysEnclosePosition);
		
		CheckBoxPreference alwaysEncloseModel = (CheckBoxPreference)findPreference(KEY_ALWAYS_ENCLOSE_MODEL);
		alwaysEncloseModel.setChecked(itemAlwaysEncloseModel);
	}
	
	private void resetAddToProvider() {
		CheckBoxPreference addToProvider = (CheckBoxPreference)findPreference(KEY_ADD_TO_PROVIDER); 
		File f = new File(Environment.getExternalStorageDirectory() + "/" + itemDownloadPath, ".nomedia");
		addToProvider.setChecked(!f.exists());
	}
	
	private void createAboutDialog() {
		final SpannableString s = new SpannableString(Html.fromHtml(aboutString));
		final ScrollView sv = new ScrollView(this);
		
		final TextView message = new TextView(this);
		Linkify.addLinks(s, Linkify.WEB_URLS);
		message.setText(s);
		message.setMovementMethod(LinkMovementMethod.getInstance());
		
		sv.setPadding(14, 2, 10, 12);
		sv.addView(message);
		
		new AlertDialog.Builder(this).setView(sv).setNeutralButton("닫기", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}}).show();
	}
	
	private void commitChanges() {
		SharedPreferences.Editor e = getSharedPreferences(Application.APP, Activity.MODE_PRIVATE).edit();
		if (!itemSaveIdPw) {
			e.putString("id", "");
			e.putString("pw", "");
		}
		e.putBoolean(KEY_SAVE_ID_PASSWORD, itemSaveIdPw);
		e.putBoolean(KEY_AUTO_SIGN_ON, itemAutoSignOn);
		e.putInt(KEY_IMAGE_RESIZE_THRESHOLD, itemResizeThreshold);
		e.putInt(KEY_MAXIMUM_RESOLUTION, itemMaximumResolution);
		e.putInt(KEY_IMAGE_QUALITY, itemImageQuality);
		e.putString(KEY_DOWNLOAD_PATH, itemDownloadPath);
		e.putString(KEY_MOBILE_PAGE_PROVIDER, itemMobilePageProvider);
		e.putString(KEY_DESTINATION, itemDestination);
		e.putBoolean(KEY_ALWAYS_ENCLOSE_POSITION, itemAlwaysEnclosePosition);
		e.putBoolean(KEY_ALWAYS_ENCLOSE_MODEL, itemAlwaysEncloseModel);
		e.commit();
		
		Log.d(Application.TAG, "changes committed.");
		
		setResult(Activity.RESULT_OK);
		finishActivity(Application.ACTION_PREFERENCES);
		finish();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		
		SharedPreferences pref = getSharedPreferences(Application.APP, Activity.MODE_PRIVATE);
		
		itemSaveIdPw = pref.getBoolean(KEY_SAVE_ID_PASSWORD, DEFAULT_SAVE_ID_PASSWORD);
		itemAutoSignOn = pref.getBoolean(KEY_AUTO_SIGN_ON, DEFAULT_AUTO_SIGN_ON);
		
		itemResizeThreshold = pref.getInt(KEY_IMAGE_RESIZE_THRESHOLD, DEFAULT_IMAGE_RESIZE_THRESHOLD);
		itemMaximumResolution = pref.getInt(KEY_MAXIMUM_RESOLUTION, DEFAULT_MAXIMUM_RESOLUTION);
		itemImageQuality = pref.getInt(KEY_IMAGE_QUALITY, DEFAULT_IMAGE_QUALITY);
		
		itemDownloadPath = pref.getString(KEY_DOWNLOAD_PATH, DEFAULT_DOWNLOAD_PATH);
		
		itemMobilePageProvider = pref.getString(KEY_MOBILE_PAGE_PROVIDER, DEFAULT_MOBILE_PAGE_PROVIDER);
		itemDestination = pref.getString(KEY_DESTINATION, DEFAULT_DESTINATION);
		itemAlwaysEnclosePosition = pref.getBoolean(KEY_ALWAYS_ENCLOSE_POSITION, DEFAULT_ALWAYS_ENCLOSE_POSITION);
		itemAlwaysEncloseModel = pref.getBoolean(KEY_ALWAYS_ENCLOSE_MODEL, DEFAULT_ALWAYS_ENCLOSE_MODEL);
		
		findPreference(KEY_SAVE_ID_PASSWORD).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				itemSaveIdPw = (Boolean)newValue;
				return true;
			}
		});
		
		findPreference(KEY_AUTO_SIGN_ON).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				itemAutoSignOn = (Boolean)newValue;
				return true;
			}
		});
		
		findPreference(KEY_EDIT_GALLERY_LIST).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(ActivityPreferences.this, ActivityEditGalleryList.class));
				
				return true;
			}
		});
		
		findPreference(KEY_WIPE_GALLERY_LIST).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				new AlertDialog.Builder(ActivityPreferences.this)
						.setTitle("확인")
						.setMessage("정말 삭제하시겠습니까?")
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setPositiveButton("네", new OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								DatabaseHelper db = new DatabaseHelper(ActivityPreferences.this);
								db.wipeFavorites();
								db.close();
								
								dialog.dismiss();
								Toast.makeText(ActivityPreferences.this, "삭제했습니다.", Toast.LENGTH_SHORT).show();
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
			}
		});
		
		findPreference(KEY_IMAGE_RESIZE_THRESHOLD).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				itemResizeThreshold = Integer.parseInt((String)newValue);
				return true;
			}
		});
		
		findPreference(KEY_MAXIMUM_RESOLUTION).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				itemMaximumResolution = Integer.parseInt((String)newValue);
				return true;
			}
		});
		
		findPreference(KEY_IMAGE_QUALITY).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				itemImageQuality = Integer.parseInt((String)newValue);
				return true;
			}
		});
		
		findPreference(KEY_DOWNLOAD_PATH).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				itemDownloadPath = (String)newValue;
				resetAddToProvider();
				
				return true;
			}
		});
		
		findPreference(KEY_ADD_TO_PROVIDER).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				File dpath = new File(Environment.getExternalStorageDirectory(), itemDownloadPath);
				File nomedia = new File(dpath, ".nomedia");
				
				if ((Boolean)newValue) {
					if (!dpath.exists())
						return true;
					
					nomedia.delete();
				} else {
					if (!dpath.exists())
						dpath.mkdir();
					
					try {
						nomedia.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
						return false;
					}
				}

				return true;
			}
		});
		
		findPreference(KEY_MOBILE_PAGE_PROVIDER).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				itemMobilePageProvider = (String)newValue;
				return true;
			}
		});
		
		findPreference(KEY_DESTINATION).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				itemDestination = (String)newValue;
				return true;
			}
		});
		
		findPreference(KEY_ALWAYS_ENCLOSE_POSITION).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				itemAlwaysEnclosePosition = (Boolean)newValue;
				return true;
			}
		});
		
		findPreference(KEY_ALWAYS_ENCLOSE_MODEL).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				itemAlwaysEncloseModel = (Boolean)newValue;
				return true;
			}
		});
		
		findPreference(KEY_ABOUT_THIS_APP).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				createAboutDialog();
				
				return true;
			}
		});
		
		resetConfigurableOptions();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		commitChanges();
	}
	
	@Override
	public void onBackPressed() {
		commitChanges();
	}
}
