package com.aaha.stockassistant.util;

import com.aaha.db.DBAdapter;

public class SharesUtil {

	public static float getTotalValue(DBAdapter db) {
		// get total vale
		return (db.transaction.getStocksValue() + db.amount.getTotalCash());
	}
}
