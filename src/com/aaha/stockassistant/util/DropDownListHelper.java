package com.aaha.stockassistant.util;

import java.util.List;

import android.content.Context;
import android.widget.ArrayAdapter;

import com.aaha.db.DBAdapter;
import com.aaha.stockassistant.R;
import com.aaha.stockassistant.util.Constants.EntityType;

public class DropDownListHelper {

	private static ArrayAdapter<String> getDataAdapter(Context ctx,
			DBAdapter db, EntityType entityType) {

		List<String> lables = entityType == EntityType.ACCOUNT ? Entity
				.getAccountNames(db) : Entity.getStockNames(db);

		// Creating adapter for spinner
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(ctx,
				R.layout.spinner_item, lables);

		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);

		return dataAdapter;
	}

	public static ArrayAdapter<String> getAccountAdapter(Context ctx,
			DBAdapter db) {
		return getDataAdapter(ctx, db, EntityType.ACCOUNT);
	}

	public static ArrayAdapter<String> getStockAdapter(Context ctx, DBAdapter db) {
		return getDataAdapter(ctx, db, EntityType.STOCK);
	}
}
