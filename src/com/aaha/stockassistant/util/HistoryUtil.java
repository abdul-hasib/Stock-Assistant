package com.aaha.stockassistant.util;

import com.aaha.db.DBAdapter;
import com.aaha.stockassistant.util.Constants.AmountType;
import com.aaha.stockassistant.util.Constants.TransactionType;

public class HistoryUtil {

	public static void addTransaction(DBAdapter db, String date,
			String stockName, String accountName,
			TransactionType transactionType, int volume, float price) {

		String typeMsg = "";
		switch (transactionType) {
		case BUY:
			typeMsg = "Bought";
			break;
		case BUY_BACK:
			typeMsg = "Bought back";
			break;
		case SELL:
			typeMsg = "Sold";
			break;
		case SHORT:
			typeMsg = "Short";
			break;
		}

		String notes = typeMsg + " " + stockName + " stocks from account "
				+ accountName + " ";

		db.transactionHistory.add(date, accountName, stockName, volume, price,
				notes);
	}

	public static void addAmmount(DBAdapter db, String date, String account,
			AmountType amountType, float amount) {

		String notes = "";
		switch (amountType) {
		case TO_SHARES:
			notes = "Amount from shares deposited";
			break;
		case FROM_SHARES:
			notes = "Amount spent on shares";
			break;
		case DEPOSIT:
			notes = "Amount as cash deposited";
			break;
		case WITHDRAW:
			notes = "Amount as cash withdran";
			break;
		}

		db.amountHistory.add(date, account, amount, notes);
	}

}
