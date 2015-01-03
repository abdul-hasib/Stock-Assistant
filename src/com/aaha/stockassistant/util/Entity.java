package com.aaha.stockassistant.util;

import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;

import com.aaha.db.DBAdapter;

public class Entity {

	public static List<String> getAccountNames(DBAdapter db) {
		return getEntityNames(db, Constants.Entity.ACCOUNT);
	}

	public static List<String> getStockNames(DBAdapter db) {
		return getEntityNames(db, Constants.Entity.STOCK);
	}

	private static List<String> getEntityNames(DBAdapter db, String entity) {
		List<String> names = new ArrayList<String>();

		Cursor cursor = null;
		if (entity.equalsIgnoreCase(Constants.Entity.ACCOUNT)) {
			cursor = db.account.get();
		} else {
			cursor = db.stock.get();
		}

		if (cursor == null) {
			return names;
		}

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				names.add(cursor.getString(1));
			} while (cursor.moveToNext());
		}

		// closing connection
		cursor.close();

		// returning names
		return names;
	}
}
