package com.aaha.stockassistant.util;

public class Constants {

	public static final String TYPE = "Type";
	public static final String TRANSACTION_TYPE = "transaction_type";
	public static final String TRANSACTION_ID = "transaction_id";
	public static final String ACCOUNT_NAME = "account_name";
	public static final String STOCK_NAME = "stock_name";
	public static final String INTRADAY = "intraday";
	public static final String ID = "id";
	public static final String ALLOWED_SHARES = "allowed_shares";
	public static final String MAXIMUM_ALLOWED_SHARES = "maximum_allowed_shares";
	public static final String MINIMUM_ALLOWED_SHARES = "minimum_allowed_shares";

	public static class Entity {
		public static final String ACCOUNT = "Account";
		public static final String STOCK = "Stock";
	}

	public enum EntityType {
		ACCOUNT, STOCK;
	}

	public static class Trade {
		public static final String BUY = "Buy";
		public static final String SELL = "Sell";
	}

	public enum TransactionType {
		BUY, SHORT, SELL, BUY_BACK
	}

	public enum AmountType {
		WITHDRAW, DEPOSIT, FROM_SHARES, TO_SHARES
	}

	public enum NAVType {
		NEGATIVE, POSITIVE
	}

}
