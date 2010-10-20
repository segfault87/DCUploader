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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class SignOnRealName implements SignOnBase {
	private static final String AUTH_URL = "http://cert.namecheck.co.kr/certnc_inner_proc.asp";
	
	public Runnable getMethodSignOn(final Application app, final Bundle b, final Handler resultHandler) {
		return new Runnable() {
			public void run() {
				Message m = resultHandler.obtainMessage();
				Bundle bm = m.getData();
				
				Log.d("dcuploader", "authenticating...");
				
				String encdata = b.getString("enc_data");
				String name = b.getString("name");
				String code1 = b.getString("code1");
				String code2 = b.getString("code2");
				
				HttpPost post = new HttpPost(AUTH_URL);
				List<NameValuePair> vlist = new ArrayList<NameValuePair>();
				vlist.add(new BasicNameValuePair("enc_data", encdata));
				vlist.add(new BasicNameValuePair("result_code", "1"));
				vlist.add(new BasicNameValuePair("contract_type", "S"));
				vlist.add(new BasicNameValuePair("au_chk", "F"));
				vlist.add(new BasicNameValuePair("name", name));
				vlist.add(new BasicNameValuePair("juminid1", code1));
				vlist.add(new BasicNameValuePair("juminid2", code2));
				try {
					post.setEntity(new UrlEncodedFormEntity(vlist));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				
				HttpResponse response = null;
				try {
					response = app.sendPostRequest(post);
				} catch (Exception e) {
					e.printStackTrace();
					
					bm.putBoolean("result", false);
					bm.putString("resultString", "서버 오류");
					resultHandler.sendMessage(m);
					return;
				}
				
				HttpEntity entity = response.getEntity();
				BufferedReader r;
				try {
					r = new BufferedReader(new InputStreamReader(entity.getContent(), "EUC-KR"));
					
					while (true) {
						String line = r.readLine();
						if (line == null) break;
						
						if (line.contains("주민번호와 이름을 바르게 입력하세요")) {
							bm.putBoolean("result", false);
							bm.putString("resultString", "인증 실패");
							resultHandler.sendMessage(m);
							return;
						}
					}
				} catch (Exception e) {
					bm.putBoolean("result", false);
					bm.putString("resultString", "파싱 실패");
					resultHandler.sendMessage(m);
					
					return;
				}
				
				/* abnormal status. traffic limit exceeded? */
				bm.putBoolean("result", true);
				resultHandler.sendMessage(m);
			}
		};
	}

	public Runnable getMethodSignOff(final Application app, final Handler resultHandler) {
		return new Runnable() {
			public void run() {
				// AFAIK there is no way to sign off. Let the session die alone...
				Message m = resultHandler.obtainMessage();
				m.getData().putBoolean("result", true);
				resultHandler.handleMessage(m);
			}
		};
	}

	public int getMethodId() {
		return Application.AUTHENTICATION_METHOD_REAL_NAME;
	}
}
