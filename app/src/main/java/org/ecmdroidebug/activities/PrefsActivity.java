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
package org.ecmdroidebug.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.Menu;

import org.ecmdroidebug.R;
import org.ecmdroidebug.Utils;

import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * Application Preferences
 */
public class PrefsActivity extends PreferenceActivity implements OnPreferenceChangeListener {
	private static final int STORAGE_LOCATION = 1;
	private Preference onlinelog;
	private Preference onlinelogpass;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.app_prefs);

		ListPreference list = (ListPreference) findPreference("connection_type");
		boolean tcp = getString(R.string.prefs_tcp_connection).equals(list.getValue());
		list.setSummary(list.getEntry());
		list.setOnPreferenceChangeListener(this);

		EditTextPreference txt = (EditTextPreference) this.findPreference("tcp_port");
		txt.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
		txt.setSummary(txt.getText());
		txt.setEnabled(tcp);
		txt.setOnPreferenceChangeListener(this);

		txt = (EditTextPreference) this.findPreference("tcp_host");
		txt.setEnabled(tcp);
		txt.setSummary(txt.getText());
		txt.setOnPreferenceChangeListener(this);

		onlinelogpass = findPreference("log_online_pass");
		onlinelogpass.setOnPreferenceChangeListener(this);

		Preference storage = findPreference("storage_location");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = prefs.edit();

		onlinelog = findPreference("log_online");
		onlinelog.setEnabled(false);
		editor.putBoolean("log_online", false);
		editor.commit();
		onlinelog.setOnPreferenceChangeListener(this);
		getPreferenceScreen().removePreference(onlinelog);

		String storageLocation = prefs.getString("storage.location", null);
		storage.setSummary(storageLocation == null ? getString(R.string.setup_storage_hint) : Uri.parse(storageLocation).getLastPathSegment());
		storage.setOnPreferenceClickListener(preference -> {
			Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
			intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
			startActivityForResult(intent, STORAGE_LOCATION);
			return true;
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == STORAGE_LOCATION && resultCode == Activity.RESULT_OK) {
			Uri uri;
			if (data != null) {
				uri = data.getData();
				if (uri != null) {
					getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
							| Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
					SharedPreferences.Editor editor = prefs.edit();
					editor.putString("storage.location", uri.toString());
					editor.commit();
					Preference storage = findPreference("storage_location");
					storage.setSummary(uri.getLastPathSegment());
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return Utils.createOptionsMenu(this, menu);
	}

	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference instanceof ListPreference) {
			ListPreference lp = (ListPreference) preference;
			int idx = lp.findIndexOfValue((String) newValue);
			preference.setSummary(lp.getEntries()[idx]);
			boolean tcp = getString(R.string.prefs_tcp_connection).equals(newValue);
			findPreference("tcp_host").setEnabled(tcp);
			findPreference("tcp_port").setEnabled(tcp);
		} else if(preference.getKey().equals("log_online_pass")){
			// Check and activate logonline.
			String date1 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
			String md51 = Utils.md5(date1); // 2026-02-26 = 40101526bfeb6f95bd7ee528cc8bffe0

			DateFormat date2 = new SimpleDateFormat("yyyy-MM-dd");
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, -1);
			String md52 = Utils.md5(date2.format(cal.getTime())); // 2026-02-25 = b6ac0d4942dd01963c118e089639b4f6
			if(md51.substring(0, 8).equals(newValue) || md52.substring(0, 8).equals(newValue)){
				getPreferenceScreen().addPreference(onlinelog);
				onlinelog.setEnabled(true);
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString("log_online_pass", "");
				editor.commit();
				getPreferenceScreen().removePreference(onlinelogpass);
				return false;
			}
		} else if(preference.getKey().equals("log_online")){
			if(!(boolean)newValue){
				onlinelog.setEnabled(false);
				getPreferenceScreen().addPreference(onlinelogpass);
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString("log_online_pass", "");
				editor.commit();
				getPreferenceScreen().removePreference(onlinelog);
				return false;
			}
		} else  {
			preference.setSummary(newValue == null ? "" : newValue.toString());
		}
		return true;
	}
}
