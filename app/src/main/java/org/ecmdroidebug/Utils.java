/*
 EcmDroid - Android Diagnostic Tool for Buell Motorcycles
 Copyright (C) 2012 by Michel Marti

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 3
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package org.ecmdroidebug;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.Menu;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.ResponseHandlerInterface;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpResponse;

/**
 * Collection of various static utility methods.
 */
public abstract class Utils {
	private static final String TAG = "Utils";
	private static final String PREFS_LOG_ONLINE = "log_online";

	public static boolean createOptionsMenu(Activity activity, Menu menu) {
		// MenuInflater mi = activity.getMenuInflater();
		// mi.inflate(R.menu.main, menu);
		return true;
	}

	public static String hexdump(byte[] data, int offset, int len) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; (i + offset) < Math.min(data.length, len); i++) {
			sb.append(":").append(String.format("%02X", data[i + offset] & 0xFF));
		}
		return sb.length() > 0 ? sb.substring(1) : "<empty>";
	}

	public static String hexdump(byte[] bytes) {
		return hexdump(bytes, 0, bytes.length);
	}

	public static boolean isEmptyString(Object str) {
		return (str == null || str.toString().trim().length() == 0);
	}

	public static String toHex(int i, int... width) {
		String fmt = "%0" + (width.length == 1 ? width[0] : 2) + "X";
		return String.format(fmt, i);
	}

	public static int freezeOrientation(Activity context) {
		int result = context.getRequestedOrientation();

		switch (context.getResources().getConfiguration().orientation) {
			case Configuration.ORIENTATION_LANDSCAPE:
				context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				break;
			case Configuration.ORIENTATION_PORTRAIT:
				context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				break;
			default:
				context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
		}
		return result;
	}

	public static String getAppVersion(Context context) {
		String result = null;
		try {
			PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			result = context.getText(R.string.app_name) + " " + pInfo.versionName;
		} catch (NameNotFoundException e1) {
		}
		return result;
	}

	public static boolean isExternalStorageAvailable() {
		String state = Environment.getExternalStorageState();
		return Environment.MEDIA_MOUNTED.equals(state);
	}

	public static void postDebug(Context ctx, Object obj){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		boolean logOnline = prefs.getBoolean(PREFS_LOG_ONLINE, false);
		String android_id = Settings.Secure.getString(ctx.getContentResolver(),
				Settings.Secure.ANDROID_ID);

		if(!logOnline) return; // For testing no need to save to db.
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		AsyncHttpClient client = new AsyncHttpClient();
		RequestParams params = new RequestParams();
		if (obj instanceof String) {
			params.put("debug", "[" + System.currentTimeMillis() + "] - [" + android_id + "] - " + obj);
		} else {
			byte[] bytes = (byte[]) obj;
			params.put("debug",  "[" + System.currentTimeMillis() + "] - [" + android_id + "] - " + Utils.hexdump(bytes, 0, bytes.length));
		}

		client.post(ctx, "https://chronoconnect.com/ecmdroidlog.php", params, new ResponseHandlerInterface() {
			@Override
			public void sendResponseMessage(HttpResponse response) throws IOException {

			}

			@Override
			public void sendStartMessage() {

			}

			@Override
			public void sendFinishMessage() {

			}

			@Override
			public void sendProgressMessage(long bytesWritten, long bytesTotal) {

			}

			@Override
			public void sendCancelMessage() {

			}

			@Override
			public void sendSuccessMessage(int statusCode, Header[] headers, byte[] responseBody) {

			}

			@Override
			public void sendFailureMessage(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

			}

			@Override
			public void sendRetryMessage(int retryNo) {

			}

			@Override
			public URI getRequestURI() {
				return null;
			}

			@Override
			public void setRequestURI(URI requestURI) {

			}

			@Override
			public Header[] getRequestHeaders() {
				return new Header[0];
			}

			@Override
			public void setRequestHeaders(Header[] requestHeaders) {

			}

			@Override
			public boolean getUseSynchronousMode() {
				return false;
			}

			@Override
			public void setUseSynchronousMode(boolean useSynchronousMode) {

			}

			@Override
			public boolean getUsePoolThread() {
				return false;
			}

			@Override
			public void setUsePoolThread(boolean usePoolThread) {

			}

			@Override
			public void onPreProcessResponse(ResponseHandlerInterface instance, HttpResponse response) {

			}

			@Override
			public void onPostProcessResponse(ResponseHandlerInterface instance, HttpResponse response) {

			}

			@Override
			public Object getTag() {
				return null;
			}

			@Override
			public void setTag(Object TAG) {

			}
		});
	}

	public static String md5(final String s) {
		final String MD5 = "MD5";
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest
					.getInstance(MD5);
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuilder hexString = new StringBuilder();
			for (byte aMessageDigest : messageDigest) {
				String h = Integer.toHexString(0xFF & aMessageDigest);
				while (h.length() < 2)
					h = "0" + h;
				hexString.append(h);
			}
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}
}
