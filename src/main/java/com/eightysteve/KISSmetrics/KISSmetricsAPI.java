package com.eightysteve.KISSmetrics;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;

public class KISSmetricsAPI implements KISSmetricsURLConnectionCallbackInterface {
	public static final String BASE_URL = "https://trk.KISSmetrics.com";
	public static final String EVENT_PATH = "/e";
	public static final String PROP_PATH = "/s";
	public static final String ALIAS_PATH = "/a";
	public static final int RETRY_INTERVAL = 5;
	public static final String ACTION_FILE = "KISSmetricsAction";
	public static final String IDENTITY_PREF = "KISSmetricsIdentityPreferences";
	public static final String PROPS_PREF = "KISSmetricsPropsPreferences";

	private String _apiKey;
	private String _identity;
	private List<String> _sendQueue;
	private Context _context;
	private HashMap<String, String> propsToSend;


	private static KISSmetricsAPI sharedAPI = null;

	private KISSmetricsAPI(String apiKey, Context context) {
		this._apiKey = apiKey;
		this._context = context;
		if (this._context == null) return;
		SharedPreferences pref = this._context.getSharedPreferences(IDENTITY_PREF, Activity.MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = null;
		this._identity = pref.getString("identity", null);
		if (this._identity == null) {
			TelephonyManager tm = (TelephonyManager) this._context.getSystemService(Context.TELEPHONY_SERVICE);
			this._identity = tm.getDeviceId();
			prefEditor = pref.edit();
			prefEditor.putString("identity", this._identity);
			prefEditor.commit();
		}

		boolean shouldSendProps = true;
		pref = this._context.getSharedPreferences(PROPS_PREF, Activity.MODE_PRIVATE);
		propsToSend = new HashMap<String, String>();
		for (String s : pref.getAll().keySet()) {
			propsToSend.put(s, pref.getString(s, null));
		}
		if (!propsToSend.isEmpty()) {
			shouldSendProps = false;
			if (propsToSend.get("systemVersion") != android.os.Build.VERSION.RELEASE) {
				shouldSendProps = true;
			}
		}

		if (shouldSendProps) {
			propsToSend.clear();
			propsToSend.put("systemName", "android");
			propsToSend.put("systemVersion", android.os.Build.VERSION.RELEASE);
			prefEditor = pref.edit();
			for (String s : propsToSend.keySet()) {
				prefEditor.putString(s, propsToSend.get(s));
			}
			prefEditor.commit();
		} else {
			propsToSend = null;
		}

		this.unarchiveData();
		try {
			this.setProperties(propsToSend);
		} catch (Exception e) {
			Log.w("KISSmetricsAPI", "Failed to set properties");
		}
	}

	public static synchronized KISSmetricsAPI sharedAPI(String apiKey, Context context) {
		if (sharedAPI == null) {
			sharedAPI = new KISSmetricsAPI(apiKey, context);
		}
		return sharedAPI;
	}

	public static synchronized KISSmetricsAPI sharedAPI() {
		if (sharedAPI == null) {
			Log.e("KISSmetricsAPI", "KISSmetricsAPI has not been initialized, please call the method new KISSmetricsAPI(<API_KEY>)");
		}
		return sharedAPI;
	}

	public void send() {
		synchronized (this) {
			if (this._sendQueue.size() == 0)
				return;

			String nextAPICall = this._sendQueue.get(0);
			KISSmetricsURLConnection connector = KISSmetricsURLConnection.initializeConnector(this);
			connector.connectURL(nextAPICall);
		}
	}

	public void recordEvent(String name, HashMap<String, String> properties) throws UnsupportedEncodingException {
		if (name == null || name.length() == 0) {
			Log.w("KISSmetricsAPI", "Name cannot be null");
			return;
		}

		String escapedEventName = URLEncoder.encode(name,"UTF-8");
		String escapedIdentity = URLEncoder.encode(this._identity, "UTF-8");
		long timeOfEvent = (long)System.currentTimeMillis()/1000;
		String theURL = String.format("%s%s?_k=%s&_p=%s&_d=1&_t=%d&_n=%s", BASE_URL, EVENT_PATH, this._apiKey, escapedIdentity, timeOfEvent, escapedEventName);

		if (properties != null) {
			String additionalURL = "";
			for (int i = 0; i < properties.keySet().size(); i++){
				String key = (String) properties.keySet().toArray()[i];
				additionalURL += URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(properties.get(key), "UTF-8");
				if(i < properties.keySet().size() - 1)
					additionalURL += "&";
			}
			if (additionalURL != "" && additionalURL.length() > 0) {
				theURL += "&" + additionalURL;
			}
		}

		synchronized (this) {
			this._sendQueue.add(theURL);
			this.archiveData();
		}
		this.send();
	}

	public void alias(String firstIdentity, String secondIdentity) throws Exception {
		if (firstIdentity == null || firstIdentity.length() == 0 || secondIdentity == null || secondIdentity.length() == 0) {
			Log.w("KISSmetricsAPI", String.format("Attempted to use nil or empty identities in alias (%s and %s). Ignoring.", firstIdentity, secondIdentity));
		}
		String escapedFirstIdentity = URLEncoder.encode(firstIdentity, "UTF-8");
		String escapedSecondIdentity = URLEncoder.encode(secondIdentity, "UTF-8");

		String theUrl = String.format("%s%s?_k=%s&_p=%s&_n=%s", BASE_URL, ALIAS_PATH, this._apiKey, escapedFirstIdentity, escapedSecondIdentity);
		synchronized (this) {
			this._sendQueue.add(theUrl);
			this.archiveData();
		}
		this.send();
	}

	public void identify(String identity) throws Exception {
		if (identity == null || identity.length() == 0) {
			Log.w("KISSmetricsAPI", "Attempted to use nil or empty identity. Ignoring.");
			return;
		}
		String escapedOldIdentity = URLEncoder.encode(this._identity, "UTF-8");
		String escapedNewIdentity = URLEncoder.encode(identity, "UTF-8");

		String theURL = String.format("%s%s?_k=%s&_p=%s&_n=%s", BASE_URL, ALIAS_PATH, this._apiKey, escapedOldIdentity, escapedNewIdentity);
		synchronized (this) {
			this._identity = identity;

			SharedPreferences pref = this._context.getSharedPreferences(IDENTITY_PREF, Activity.MODE_PRIVATE);
			SharedPreferences.Editor prefEditor = pref.edit();
			prefEditor.putString("identity", this._identity);
			prefEditor.commit();

			this._sendQueue.add(theURL);
			this.archiveData();
		}
		this.send();
	}

	public void setProperties(HashMap<String, String> properties) throws Exception {
		if (properties == null || properties.size() == 0) {
			Log.w("KISSmetricsAPI", "Tried to set properties with no properties in it..");
			return;
		}

		String additionalURL = "";
		for (int i = 0; i < properties.keySet().size(); i++){
			String key = (String) properties.keySet().toArray()[i];
			additionalURL += URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(properties.get(key).toString(), "UTF-8");
			if(i < properties.keySet().size() - 1)
				additionalURL += "&";
		}
		if (additionalURL.length() == 0) {
			Log.w("KISSmetricsAPI", "No valid properties in setProperties:. Ignoring call");
			return;
		}

		String escapedIdentity = URLEncoder.encode(this._identity, "UTF-8");
		long timeOfEvent = (long)System.currentTimeMillis()/1000;

		String theURL = String.format("%s%s?_k=%s&_p=%s&_d=1&_t=%d", BASE_URL, PROP_PATH, this._apiKey, escapedIdentity, timeOfEvent);
		theURL += "&" + additionalURL;

		synchronized (this) {
			this._sendQueue.add(theURL);
			this.archiveData();
		}
		this.send();
	}

	public void archiveData() {
		try {
			FileOutputStream fos = this._context.openFileOutput(ACTION_FILE, Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this._sendQueue);
            oos.close();
		} catch (Exception e) {
			Log.w("KISSmetricsAPI", "Unable to archive data");
		}
	}

	@SuppressWarnings("unchecked")
	public void unarchiveData() {
		try {
			FileInputStream fis = this._context.openFileInput(ACTION_FILE);
			ObjectInputStream ois = new ObjectInputStream(fis);
			this._sendQueue = (List<String>) ois.readObject();
			ois.close();
			fis.close();
		} catch (Exception e) {
			Log.w("KISSmetricsAPI", "Unable to unarchive data");
		}

		if (this._sendQueue == null)
			this._sendQueue = new ArrayList<String>();
		else
			this.send();
	}

	public void finished(int statusCode) {
		this.send();
	}

	public List<String> getSendQueue() {
		return _sendQueue;
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
}
