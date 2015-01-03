package com.aaha.stockassistant.util;

import android.content.Context;

public class NAVUtil {
	public static float getNAV(Context context) {
		float nav = 0;
		try {
			nav = Settings.getFloat(Settings.PREF_NAV, context, 0);
			LogUtil.d("Current NAV: " + nav);
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.toastShort(context, "Exception while getting nav:" + e);
		}
		return nav;
	}

	public static float getUnits(Context context) {
		float units = 0;
		try {
			units = Settings.getFloat(Settings.PREF_UNITS, context, 0);
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.toastShort(context, "Exception while getting units:" + e);
		}
		return units;
	}

	// change units based on amount transactions
	public static void updateUnits(Context context, float totalValue) {
		float nav = getNAV(context);
		if (nav == 0)
			return;

		float units = totalValue / nav;
		try {
			Settings.putPref(Settings.PREF_UNITS, String.valueOf(units),
					context);
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.toastShort(context, "Exception while setting nav value:" + e);
		}
	}

	// change NAV based on stock transactions
	public static void updateNAV(Context context, float totalValue) {
		float units = getUnits(context);

		if (units == 0) {
			return;
		}

		try {
			LogUtil.d("Updating NAV to: " + totalValue + " for units: " + units);
			Settings.putPref(Settings.PREF_NAV,
					String.valueOf(totalValue / units), context);
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.toastShort(context, "Exception while setting nav value:" + e);
		}
	}

	// reset NAV
	public static void resetNAV(Context context, float nav, float totalValue) {
		float units = 0;
		if (nav != 0) {
			units = totalValue / nav;
		}
		try {
			LogUtil.d("Resetting NAV to: " + nav);
			Settings.putPref(Settings.PREF_NAV, String.valueOf(nav), context);
			Settings.putPref(Settings.PREF_UNITS, String.valueOf(units),
					context);
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.toastShort(context, "Exception while setting nav value:" + e);
		}
	}
}
