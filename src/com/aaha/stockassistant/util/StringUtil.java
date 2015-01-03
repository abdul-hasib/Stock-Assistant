package com.aaha.stockassistant.util;

import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Color;

public class StringUtil {

	public static int getHeaderColor() {
		return Color.rgb(169, 198, 236);
	}

	public static boolean isEmptyString(String s) {
		return s.trim().length() == 0;
	}

	public static String pad(int c) {
		if (c >= 10)
			return String.valueOf(c);
		else
			return "0" + String.valueOf(c);
	}

	public static String getString(Context context, int id) {
		return context.getResources().getString(id);
	}

	public static String round(double d, int decimalPlace) {
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(decimalPlace);
		df.setMinimumFractionDigits(decimalPlace);
		return df.format(d);
	}

}
