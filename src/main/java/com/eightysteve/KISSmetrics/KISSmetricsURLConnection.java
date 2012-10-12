/*
 * 
 * Copyright 2012 Steve Chan, http://80steve.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.eightysteve.KISSmetrics;

import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

public class KISSmetricsURLConnection {

	private static KISSmetricsURLConnection _connector;
	private static KISSmetricsAPI _apiContext;
	private static KISSmetricsURLConnectionCallbackInterface _callback;

	private static boolean _isConnected = false;

	private KISSmetricsURLConnection(KISSmetricsAPI context, KISSmetricsURLConnectionCallbackInterface callback) {
		_apiContext = context;
		_callback = callback;
	}

	public static KISSmetricsURLConnection initializeConnector(KISSmetricsAPI context) {
		if (_connector == null)
			_connector = new KISSmetricsURLConnection(context, context);
		return _connector;
	}

	public static KISSmetricsURLConnection initializeConnector(KISSmetricsAPI context, KISSmetricsURLConnectionCallbackInterface callback) {
		if (_connector == null)
			_connector = new KISSmetricsURLConnection(context, callback);
		return _connector;
	}

	public void connectURL(final String requestURL) {
		if (_isConnected || requestURL == null || requestURL.length() == 0) return;

		_isConnected = true;
		new Thread(new Runnable() {
			public void run() {
				int statusCode = -1;
				try {
					URL httpURL = new URL(requestURL);
					HttpURLConnection connection = (HttpURLConnection) httpURL.openConnection();
					connection.setConnectTimeout(15000);
					connection.setReadTimeout(30000);
					statusCode = connection.getResponseCode();
					connection.connect();
				} catch (Exception e) {
					_apiContext.getSendQueue().remove(0);
					Log.w("KISSmetricsAPI", "Failed to connect URL: " + requestURL);
				} finally {
					_isConnected = false;
				}

				if (statusCode == 200 || statusCode == 304) {
					_apiContext.getSendQueue().remove(0);
					_apiContext.archiveData();
				} else if (statusCode >= 400 && statusCode <= 500) {
					String failedURL = _apiContext.getSendQueue().get(0);
					_apiContext.getSendQueue().remove(0);
					_apiContext.getSendQueue().add(failedURL);
					_apiContext.archiveData();
				}
				if (_callback != null)
					_callback.finished(statusCode);
			}
		}).start();
	}

	public static KISSmetricsAPI getApiContext() {
		return _apiContext;
	}

	public static void setApiContext(KISSmetricsAPI apiContext) {
		KISSmetricsURLConnection._apiContext = apiContext;
	}
}
