package com.aaha.stockassistant.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;

public class DateUtil {
	@SuppressLint("SimpleDateFormat")
	public static Date parseDate(String date) {
		SimpleDateFormat parser = new SimpleDateFormat("dd/MM/yy");
		try {
			return parser.parse(date);
		} catch (ParseException e) {
			LogUtil.e("Exception in parsing " + e.toString());
			e.printStackTrace();
		}
		return null;
	}

	@SuppressLint("SimpleDateFormat")
	public static String formatDate(long milliSeconds) {
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy");
		String formatedDate = formatter.format(milliSeconds);
		return formatedDate;
	}

	@SuppressLint("SimpleDateFormat")
	public static boolean isFriday(long milliSeconds) {
		SimpleDateFormat formatter = new SimpleDateFormat("EEE");
		String formatedDate = formatter.format(milliSeconds);
		return (formatedDate.equalsIgnoreCase("Fri"));
	}
}
