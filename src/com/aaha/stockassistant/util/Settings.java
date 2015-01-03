package com.aaha.stockassistant.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.aaha.stockassistant.R;

public class Settings extends PreferenceActivity {

	public static String PREF_NAV = "nav";
	public static String PREF_UNITS = "units";
	public static String PREF_SPLASH = "pref_hide_splash_screen";

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
	}

	public static void putPref(String key, String value, Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key, value);
		editor.commit();
	}

	public static String getString(String key, Context context,
			String defaultValue) {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		return preferences.getString(key, defaultValue);
	}

	public static int getInt(String key, Context context, int defaultValue) {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		int value = 0;
		try {
			String temp = preferences.getString(key,
					String.valueOf(defaultValue));
			value = Integer.valueOf(temp);
		} catch (ClassCastException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return value;
	}

	public static float getFloat(String key, Context context, float defaultValue) {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		float value = 0;
		try {
			String temp = preferences.getString(key,
					String.valueOf(defaultValue));
			value = Float.valueOf(temp);
		} catch (ClassCastException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return value;
	}

	public static boolean getBoolean(String key, Context context,
			boolean defaultValue) {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		return preferences.getBoolean(key, defaultValue);
	}

}
