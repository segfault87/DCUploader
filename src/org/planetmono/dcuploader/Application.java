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

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class Application extends android.app.Application {
	public static final String TAG = "dcuploader";
	public static final String APP = "dcuploader";
	
	public static final String HOST_DCINSIDE = "gall.dcinside.com";
	public static final String HOST_BOXWEB_OLD = "m.boxweb.net";
	public static final String HOST_BOXWEB = "dc.boxweb.net";
	public static final String HOST_MOOLZO = "dc.m.oolzo.com";
	
	public static final String URL_LIST_DCINSIDE = "http://gall.dcinside.com/list.php";
	public static final String URL_LIST_BOXWEB = "http://dc.boxweb.net/list.php";
	public static final String URL_LIST_BOXWEB_OLD = "http://m.boxweb.net/c/dc/list.php";
	public static final String URL_LIST_MOOLZO = "http://dc.m.oolzo.com/List.aspx";
	
	public static final String URL_VIEW_DCINSIDE = URL_LIST_DCINSIDE;
	public static final String URL_VIEW_BOXWEB = URL_LIST_BOXWEB;
	public static final String URL_VIEW_BOXWEB_OLD = URL_LIST_BOXWEB_OLD;
	public static final String URL_VIEW_MOOLZO = "http://dc.m.oolzo.com/View.aspx";
	
	private static final int TIMEOUT_CONNECTION = 15000;
	private static final int TIMEOUT_SOCKET = 30000;
	
	public static final int ACTION_ADD_GALLERY = 0;
	public static final int ACTION_ADD_PHOTO = 1;
	public static final int ACTION_SIGN_ON = 2;
	public static final int ACTION_PREFERENCES = 3;
	public static final int ACTION_TAKE_PHOTO = 4;
	
	public static final int DESTINATION_BOXWEB = 1;
	public static final int DESTINATION_DCINSIDE = 2;
	public static final int DESTINATION_MOOLZO = 3;
	public static final int DESTINATION_BOXWEB_OLD = 4;
	
	private DefaultHttpClient client;
	
	private boolean isActive = false;
	private int method = -1;
	
	public static final int AUTHENTICATION_METHOD_GALLOG = 0;
	public static final int AUTHENTICATION_METHOD_REAL_NAME = 1;
	
	@Override
	public void onCreate() {
		HttpParams defaultParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(defaultParams, TIMEOUT_CONNECTION);
		HttpConnectionParams.setSoTimeout(defaultParams, TIMEOUT_SOCKET);
		
		client = new DefaultHttpClient(defaultParams);
	}
	
	public boolean isSignedOn() {
		return isActive;
	}
	
	public int authenticationMethod() {
		return method;
	}
	
	public void setSignedOn(boolean signedOn, int m) {
		isActive = signedOn;
		method = m;
	}
	
	public HttpResponse sendGetRequest(HttpGet req) throws ClientProtocolException, IOException {
		req.setHeader("User-Agent", "Mozilla/5.0");
		
		return client.execute(req);
	}
	
	public HttpResponse sendPostRequest(HttpPost req) throws ClientProtocolException, IOException {
		req.setHeader("User-Agent", "Mozilla/5.0");
		
		return client.execute(req);
	}
}
