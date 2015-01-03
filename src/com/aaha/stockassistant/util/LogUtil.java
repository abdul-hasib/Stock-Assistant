package com.aaha.stockassistant.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.aaha.db.DBAdapter;

public class LogUtil {

	public static void toastLong(Context ctx, Object message) {
		toast(ctx, message, Toast.LENGTH_LONG);
	}

	private static void toast(Context ctx, Object message, int toastType) {
		try {
			Toast toast = new Toast(ctx);
			toast = Toast.makeText(ctx, message.toString(), toastType);
			toast.show();
			d(message);
		} catch (Exception ex) {
			Log.e(DBAdapter.TAG, "Exception in toasing");
			ex.printStackTrace();
		}
	}

	public static void toastShort(Context ctx, Object message) {
		toast(ctx, message, Toast.LENGTH_SHORT);
	}

	public static void toast(Context ctx, Object message, Exception e) {
		toast(ctx, message + e.toString(), Toast.LENGTH_SHORT);
		e.printStackTrace();
	}

	public static void d(Object message) {
		Log.d(DBAdapter.TAG, "" + message);
	}

	public static void e(Object message) {
		Log.d(DBAdapter.TAG, "" + message);
	}
}
