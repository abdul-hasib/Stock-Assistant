package com.aaha.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.aaha.stockassistant.util.Constants.AmountType;
import com.aaha.stockassistant.util.Constants.TransactionType;
import com.aaha.stockassistant.util.DateUtil;
import com.aaha.stockassistant.util.LogUtil;
import com.aaha.stockassistant.util.TransactionUtil;

public class DBAdapter {

	public static final String TAG = "StockAssistant";
	private static final String DATABASE_NAME = "stockAssistant.db";
	private static final int DATABASE_VERSION = 4;

	private final Context context;
	private DatabaseHelper DBHelper;
	public T_Account account;
	public T_Stock stock;
	public T_Amount amount;
	public T_Nifty nifty;
	public T_Transaction transaction;
	public T_Details details;
	public T_TransactionHistory transactionHistory;
	public T_AmountHistory amountHistory;
	private SQLiteDatabase db;

	public DBAdapter(Context ctx) {
		this.context = ctx;
		DBHelper = new DatabaseHelper(context);
		stock = new T_Stock();
		account = new T_Account();
		nifty = new T_Nifty();
		amount = new T_Amount();
		details = new T_Details();
		transaction = new T_Transaction();
		transactionHistory = new T_TransactionHistory();
		amountHistory = new T_AmountHistory();
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			try {
				Log.d(TAG, "Creating database tables");
				db.execSQL(T_Transaction.CREATE_SQL);
				Log.d(TAG, "Finished creating database table: "
						+ T_Transaction.TABLE_NAME);
				db.execSQL(T_Amount.CREATE_SQL);
				Log.d(TAG, "Finished creating database table: "
						+ T_Amount.TABLE_NAME + "\nUsing sql\n"
						+ T_Amount.CREATE_SQL);
				db.execSQL(T_Account.CREATE_SQL);
				Log.d(TAG, "Finished creating database table: "
						+ T_Account.TABLE_NAME);
				db.execSQL(T_Stock.CREATE_SQL);
				Log.d(TAG, "Finished creating database table: "
						+ T_Stock.TABLE_NAME);
				db.execSQL(T_Nifty.CREATE_SQL);
				Log.d(TAG, "Finished creating database table: "
						+ T_Nifty.TABLE_NAME);
				db.execSQL(T_Details.CREATE_SQL);
				Log.d(TAG, "Finished creating database table: "
						+ T_Details.TABLE_NAME);
				db.execSQL(T_TransactionHistory.CREATE_SQL);
				Log.d(TAG, "Finished creating database table: "
						+ T_TransactionHistory.TABLE_NAME);
				db.execSQL(T_AmountHistory.CREATE_SQL);
				Log.d(TAG, "Finished creating database table: "
						+ T_AmountHistory.TABLE_NAME);
			} catch (SQLException e) {
				Log.e(TAG,
						"Exception while creating database tables: "
								+ e.toString());
				e.printStackTrace();
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");

			switch (newVersion) {
			case 3:
				db.execSQL("DROP TABLE IF EXISTS " + T_Amount.TABLE_NAME);
				db.execSQL(T_Amount.CREATE_SQL);
				break;
			}
			onCreate(db);
		}
	}

	public class T_Account {
		public static final String TABLE_NAME = "T_Account";
		public static final String KEY_ID = "_id";
		public static final String KEY_NAME = "A_Name";
		public static final String KEY_BROKERAGE = "A_Brokerage";
		public static final String KEY_INTRADAY_BROKERAGE = "A_Intraday_Brokerage";

		private static final String CREATE_SQL = "CREATE TABLE IF NOT EXISTS "
				+ TABLE_NAME + " (" + KEY_ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_NAME
				+ " VARCHAR not null, " + KEY_BROKERAGE + " FLOAT, "
				+ KEY_INTRADAY_BROKERAGE + " FLOAT " + ");";

		public long add(String name, float brokerage, float intradayBrokerage) {
			ContentValues initialValues = new ContentValues();
			initialValues.put(KEY_NAME, name);
			initialValues.put(KEY_BROKERAGE, brokerage);
			initialValues.put(KEY_INTRADAY_BROKERAGE, intradayBrokerage);
			return db.insert(TABLE_NAME, null, initialValues);
		}

		public boolean delete(long id) {
			return db.delete(TABLE_NAME, KEY_ID + "=" + id, null) > 0;
		}

		public Cursor get() {
			return db.query(TABLE_NAME, new String[] { KEY_ID, KEY_NAME,
					KEY_BROKERAGE, KEY_INTRADAY_BROKERAGE }, null, null, null,
					null, KEY_NAME + " ASC", null);
		}

		public Cursor get(long id) {
			return db.query(TABLE_NAME, new String[] { KEY_ID, KEY_NAME,
					KEY_BROKERAGE, KEY_INTRADAY_BROKERAGE }, KEY_ID + "=?",
					new String[] { String.valueOf(id) }, null, null, KEY_NAME
							+ " ASC", null);
		}

		public String getName(long accountId) {
			Cursor c = db.query(TABLE_NAME, new String[] { KEY_NAME }, KEY_ID
					+ "=?", new String[] { String.valueOf(accountId) }, null,
					null, KEY_NAME + " ASC", null);

			String name = "UNKNOWN";
			if (c != null && c.moveToFirst()) {
				name = c.getString(c.getColumnIndex(T_Account.KEY_NAME));
				c.close();
			}
			return name;
		}

		public Cursor get(String name) {
			return db.query(TABLE_NAME, new String[] { KEY_ID, KEY_NAME,
					KEY_BROKERAGE, KEY_INTRADAY_BROKERAGE }, KEY_NAME + "=?",
					new String[] { name }, null, null, KEY_NAME + " ASC", null);
		}

		public long getId(String account) {
			Cursor c = db.query(TABLE_NAME, new String[] { KEY_ID }, KEY_NAME
					+ "=?", new String[] { account }, null, null, null, null);

			long id = -1;
			if (c != null && c.moveToFirst()) {
				id = c.getLong(c.getColumnIndex(T_Account.KEY_ID));
				c.close();
			}
			return id;
		}

		public boolean update(long id, String name, float brokerage,
				float intradayBrokerage) {
			ContentValues args = new ContentValues();
			args.put(KEY_NAME, name);
			args.put(KEY_BROKERAGE, brokerage);
			args.put(KEY_INTRADAY_BROKERAGE, intradayBrokerage);
			return db.update(TABLE_NAME, args, KEY_ID + "=?",
					new String[] { String.valueOf(id) }) > 0;
		}

		public boolean isExist(String name) {
			Cursor c = get(name);
			if (c != null && c.moveToFirst()) {
				c.close();
				return true;
			}
			return false;
		}

		public boolean isExist(long id) {
			Cursor c = get(id);
			if (c != null && c.moveToFirst()) {
				c.close();
				return true;
			}
			return false;
		}

		public Cursor getDataToExport() {
			Cursor c = db.query(TABLE_NAME, new String[] { KEY_NAME,
					KEY_BROKERAGE, KEY_INTRADAY_BROKERAGE }, null, null, null,
					null, KEY_NAME + " ASC", null);
			return c;
		}
	}

	public class T_Nifty {
		public static final String TABLE_NAME = "T_Nifty";
		public static final String KEY_ID = "_id";
		public static final String KEY_DATE = "A_Date";
		public static final String KEY_NIFTY_VALUE = "A_Value";
		public static final String KEY_STOCK_VALUE = "A_Stock_Value";

		private static final String CREATE_SQL = "CREATE TABLE IF NOT EXISTS "
				+ TABLE_NAME + " (" + KEY_ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_DATE
				+ " INTEGER not null, " + KEY_NIFTY_VALUE + " FLOAT, "
				+ KEY_STOCK_VALUE + " FLOAT " + ");";

		public long add(String date, float niftyValue, float stockValue) {
			ContentValues initialValues = new ContentValues();
			initialValues.put(KEY_DATE, DateUtil.parseDate(date).getTime());
			initialValues.put(KEY_NIFTY_VALUE, niftyValue);
			initialValues.put(KEY_STOCK_VALUE, stockValue);
			return db.insert(TABLE_NAME, null, initialValues);
		}

		public boolean delete(long id) {
			return db.delete(TABLE_NAME, KEY_ID + "=" + id, null) > 0;
		}

		public Cursor get() {
			return db.query(TABLE_NAME, new String[] { KEY_ID, KEY_DATE,
					KEY_NIFTY_VALUE, KEY_STOCK_VALUE }, null, null, null, null,
					KEY_DATE + " ASC", null);
		}

		public Cursor get(long id) {
			return db.query(TABLE_NAME, new String[] { KEY_ID, KEY_DATE,
					KEY_NIFTY_VALUE, KEY_STOCK_VALUE }, KEY_ID + "=?",
					new String[] { String.valueOf(id) }, null, null, KEY_DATE
							+ " ASC", null);
		}

		public Cursor get(String niftyDate) {
			long date = DateUtil.parseDate(niftyDate).getTime();
			return db.query(TABLE_NAME, new String[] { KEY_ID, KEY_DATE,
					KEY_NIFTY_VALUE, KEY_STOCK_VALUE }, KEY_DATE + "=?",
					new String[] { String.valueOf(date) }, null, null, KEY_DATE
							+ " ASC", null);
		}

		public boolean update(long id, String niftyDate, float niftyValue) {
			ContentValues args = new ContentValues();
			args.put(KEY_DATE, DateUtil.parseDate(niftyDate).getTime());
			args.put(KEY_NIFTY_VALUE, niftyValue);
			return db.update(TABLE_NAME, args, KEY_ID + "=?",
					new String[] { String.valueOf(id) }) > 0;
		}

		public boolean isExist(String niftyDate) {
			Cursor c = get(niftyDate);
			if (c != null && c.moveToFirst()) {
				c.close();
				return true;
			}
			return false;
		}

		public boolean isExist(long id) {
			Cursor c = get(id);
			if (c != null && c.moveToFirst()) {
				c.close();
				return true;
			}
			return false;
		}

		public Cursor getDataToExport() {
			Cursor c = db.query(TABLE_NAME, new String[] { KEY_DATE,
					KEY_NIFTY_VALUE, KEY_STOCK_VALUE }, null, null, null, null,
					null, null);
			return c;
		}
	}

	public class T_Stock {
		public static final String TABLE_NAME = "T_Stock";
		public static final String KEY_ID = "_id";
		public static final String KEY_NAME = "A_Name";

		private static final String CREATE_SQL = "CREATE TABLE IF NOT EXISTS "
				+ TABLE_NAME + " (" + KEY_ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_NAME
				+ " VARCHAR not null );";

		public long add(String name) {
			ContentValues initialValues = new ContentValues();
			initialValues.put(KEY_NAME, name);
			return db.insert(TABLE_NAME, null, initialValues);
		}

		public boolean delete(long id) {
			return db.delete(TABLE_NAME, KEY_ID + "=" + id, null) > 0;
		}

		public Cursor get() {
			return db.query(TABLE_NAME, new String[] { KEY_ID, KEY_NAME },
					null, null, null, null, KEY_NAME + " ASC", null);
		}

		public Cursor get(long id) {
			return db.query(TABLE_NAME, new String[] { KEY_ID, KEY_NAME },
					KEY_ID + "=?", new String[] { String.valueOf(id) }, null,
					null, KEY_NAME + " ASC", null);
		}

		public Cursor get(String name) {
			return db.query(TABLE_NAME, new String[] { KEY_ID, KEY_NAME },
					KEY_NAME + "=?", new String[] { name }, null, null,
					KEY_NAME + " ASC", null);
		}

		public long getId(String name) {
			Cursor c = db.query(TABLE_NAME, new String[] { KEY_ID }, KEY_NAME
					+ "=?", new String[] { name }, null, null, null, null);

			long id = -1;
			if (c != null && c.moveToFirst()) {
				id = c.getLong(c.getColumnIndex(T_Stock.KEY_ID));
				c.close();
			}
			return id;
		}

		public String getName(long id) {
			Cursor c = db.query(TABLE_NAME, new String[] { KEY_NAME }, KEY_ID
					+ "=?", new String[] { String.valueOf(id) }, null, null,
					null, null);

			String name = "UNKNOWN";
			if (c != null && c.moveToFirst()) {
				name = c.getString(c.getColumnIndex(T_Stock.KEY_NAME));
				c.close();
			}
			return name;
		}

		public boolean update(long id, String name) {
			ContentValues args = new ContentValues();
			args.put(KEY_NAME, name);
			return db.update(TABLE_NAME, args, KEY_ID + "=?",
					new String[] { String.valueOf(id) }) > 0;
		}

		public boolean isExist(String name) {
			Cursor c = get(name);
			if (c != null && c.moveToFirst()) {
				c.close();
				return true;
			}
			return false;
		}

		public Cursor getDataToExport() {
			Cursor c = db.query(TABLE_NAME, new String[] { KEY_NAME }, null,
					null, null, null, KEY_NAME + " ASC", null);
			return c;
		}
	}

	public class T_Amount {
		public static final String TABLE_NAME = "T_Amount";
		public static final String KEY_ID = "_id";
		public static final String KEY_DATE = "A_Date";
		public static final String KEY_ACCOUNT_ID = "A_AccountID";
		public static final String KEY_VALUE = "A_Value";
		public static final String KEY_TYPE = "A_Type";

		private static final String CREATE_SQL = "CREATE TABLE IF NOT EXISTS "
				+ TABLE_NAME + " (" + KEY_ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_DATE
				+ " INTEGER, " + KEY_ACCOUNT_ID + " INTEGER, " + KEY_VALUE
				+ " REAL, " + KEY_TYPE + " INTEGER," + " FOREIGN KEY( "
				+ KEY_ACCOUNT_ID + ") REFERENCES " + T_Account.TABLE_NAME + "("
				+ KEY_ID + ") ON DELETE CASCADE );";

		public long add(String date, String accountName, float amount,
				AmountType amountType) {

			return add(date, account.getId(accountName), amount, amountType);
		}

		public long add(String date, long accountId, float amount,
				TransactionType transactionType) {

			AmountType amountType = TransactionUtil
					.getAmountType(transactionType);

			return add(date, accountId, amount, amountType);
		}

		public long add(String date, long accountId, float amount,
				AmountType amountType) {

			if (amountType == AmountType.WITHDRAW) {
				amount *= -1;
			}
			LogUtil.d("Adding amount to database: " + amount);

			ContentValues initialValues = new ContentValues();
			initialValues.put(KEY_ACCOUNT_ID, accountId);
			initialValues.put(KEY_VALUE, amount);
			initialValues.put(KEY_DATE, DateUtil.parseDate(date).getTime());
			initialValues.put(KEY_TYPE, amountType.ordinal());
			return db.insert(TABLE_NAME, null, initialValues);
		}

		public boolean update(long id, String date, String accountName,
				float amount, AmountType amountType) {
			return update(id, date, account.getId(accountName), amount,
					amountType);
		}

		public boolean update(long id, String date, long accountId,
				float amount, AmountType amountType) {

			if (amountType == AmountType.WITHDRAW) {
				amount *= -1;
			}
			LogUtil.d("Updating amount details: " + amount);

			ContentValues initialValues = new ContentValues();
			initialValues.put(KEY_ACCOUNT_ID, accountId);
			initialValues.put(KEY_VALUE, amount);
			initialValues.put(KEY_DATE, DateUtil.parseDate(date).getTime());
			initialValues.put(KEY_TYPE, amountType.ordinal());

			return db.update(TABLE_NAME, initialValues, KEY_ID + "=?",
					new String[] { String.valueOf(id) }) > 0;
		}

		public boolean delete(long id) {
			return db.delete(TABLE_NAME, KEY_ID + "=" + id, null) > 0;
		}

		public Cursor get() {
			return db.query(false, TABLE_NAME, new String[] { KEY_ID, KEY_DATE,
					KEY_ACCOUNT_ID, KEY_VALUE, KEY_TYPE }, null, null, null,
					null, null, null);
		}

		public Cursor get(long id) {
			return db
					.query(TABLE_NAME, new String[] { KEY_ID, KEY_DATE,
							KEY_ACCOUNT_ID, KEY_VALUE, KEY_TYPE }, KEY_ID
							+ "=?", new String[] { String.valueOf(id) }, null,
							null, null, null);
		}

		public float getTotalCash() {
			String sql = "SELECT KEY_ACCOUNT_ID, KEY_VALUE FROM "
					+ T_Transaction.TABLE_NAME + " UNION ALL "
					+ " SELECT (SELECT KEY_ACCOUNT_ID FROM "
					+ T_Transaction.TABLE_NAME
					+ " t1 WHERE KEY_ID=t1.KEY_ID), KEY_VALUE FROM "
					+ T_Details.TABLE_NAME + " UNION ALL"
					+ " SELECT KEY_ACCOUNT_ID, KEY_VALUE FROM "
					+ T_Amount.TABLE_NAME;

			sql = sql.replace("KEY_ACCOUNT_ID", KEY_ACCOUNT_ID)
					.replace("KEY_ID", KEY_ID).replace("KEY_VALUE", KEY_VALUE)
					.replace("  ", " ");

			LogUtil.d("Getting total cash value");
			LogUtil.d(sql);

			float value = 0;
			Cursor c = db.rawQuery(sql, null);
			if (c != null && c.moveToFirst()) {
				do {
					value += c.getFloat(c.getColumnIndex(T_Amount.KEY_VALUE));
				} while (c.moveToNext());
				c.close();
			}
			return value;
		}

		public Cursor getAmountDetails(String accountName) {
			return getAmountDetails(account.getId(accountName));
		}

		// GROUP BY KEY_ACCOUNT_ID HAVING KEY_ACCOUNT_ID=KEY_ACOUNT_ID_VALUE
		public Cursor getAmountDetails(long accountId) {
			String sql = "SELECT KEY_ID, KEY_DATE, KEY_VALUE, "
					+ " (CASE WHEN KEY_VALUE < 0 THEN ? ELSE ? END) as KEY_TYPE"
					+ " FROM "
					+ T_Transaction.TABLE_NAME
					+ " WHERE KEY_ACCOUNT_ID=KEY_ACOUNT_ID_VALUE"
					+ " UNION ALL "
					+ " SELECT t2.KEY_ID, t2.KEY_DATE, t2.KEY_VALUE, "
					+ " (CASE WHEN t2.KEY_VALUE < 0 THEN ? ELSE ? END) as KEY_TYPE"
					+ " FROM "
					+ T_Details.TABLE_NAME
					+ " t2 JOIN "
					+ T_Transaction.TABLE_NAME
					+ " t1 ON t2.KEY_TRANSACTION_ID = t1.KEY_ID AND t1.KEY_ACCOUNT_ID = KEY_ACOUNT_ID_VALUE"
					+ " UNION ALL"
					+ " SELECT KEY_ID, KEY_DATE, KEY_VALUE, KEY_TYPE FROM "
					+ T_Amount.TABLE_NAME
					+ " WHERE KEY_ACCOUNT_ID=KEY_ACOUNT_ID_VALUE ORDER BY "
					+ KEY_DATE;

			sql = sql
					.replace("KEY_DATE", KEY_DATE)
					.replace("KEY_ID", KEY_ID)
					.replace("KEY_TYPE", KEY_TYPE)
					.replace("KEY_ACCOUNT_ID", T_Transaction.KEY_ACCOUNT_ID)
					.replace("KEY_ACOUNT_ID_VALUE", String.valueOf(accountId))
					.replace("KEY_TRANSACTION_ID", T_Details.KEY_TRANSACTION_ID)
					.replace("KEY_VALUE", KEY_VALUE);

			LogUtil.d("Getting amount details\n" + sql);
			return db.rawQuery(
					sql,
					new String[] {
							String.valueOf(AmountType.TO_SHARES.ordinal()),
							String.valueOf(AmountType.FROM_SHARES.ordinal()),
							String.valueOf(AmountType.TO_SHARES.ordinal()),
							String.valueOf(AmountType.FROM_SHARES.ordinal()) });
		}

		public Cursor getDataToExport() {
			String sql = "SELECT KEY_DATE, t2.KEY_NAME as KEY_ACCOUNT_ID, KEY_VALUE, KEY_TYPE "
					+ " FROM "
					+ TABLE_NAME
					+ " t1 LEFT JOIN "
					+ T_Account.TABLE_NAME
					+ " t2 on KEY_ACCOUNT_ID = t2.KEY_ID";

			sql = sql.replace("KEY_DATE", KEY_DATE)
					.replace("KEY_ACCOUNT_ID", KEY_ACCOUNT_ID)
					.replace("KEY_ID", KEY_ID)
					.replace("KEY_NAME", T_Account.KEY_NAME)
					.replace("KEY_VALUE", KEY_VALUE)
					.replace("KEY_TYPE", KEY_TYPE);
			LogUtil.d(sql);
			return db.rawQuery(sql, null);
		}
	}

	public class T_TransactionHistory {
		public static final String TABLE_NAME = "T_TransactionHistory";
		public static final String KEY_ID = "_id";
		public static final String KEY_DATE = "A_Date";
		public static final String KEY_ACCOUNT = "A_Account";
		public static final String KEY_STOCK = "A_Stock";
		public static final String KEY_VOLUME = "A_Volume";
		public static final String KEY_PRICE = "A_Price";
		public static final String KEY_NOTES = "A_Notes";

		private static final String CREATE_SQL = "CREATE TABLE IF NOT EXISTS "
				+ TABLE_NAME + " (" + KEY_ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_DATE
				+ " INTEGER, " + KEY_ACCOUNT + " VARCHAR, " + KEY_STOCK
				+ " VARCHAR, " + KEY_VOLUME + " INTEGER, " + KEY_PRICE
				+ " FLOAT, " + KEY_NOTES + " VARCHAR );";

		public long add(String date, long accountId, long stockId, int volume,
				float price, String notes) {
			return add(date, account.getName(accountId),
					stock.getName(stockId), volume, price, notes);
		}

		public long add(String date, String account, String stock, int volume,
				float price, String notes) {
			ContentValues initialValues = new ContentValues();
			initialValues.put(KEY_DATE, DateUtil.parseDate(date).getTime());
			initialValues.put(KEY_ACCOUNT, account);
			initialValues.put(KEY_STOCK, stock);
			initialValues.put(KEY_VOLUME, volume);
			initialValues.put(KEY_PRICE, price);
			initialValues.put(KEY_NOTES, notes);
			return db.insert(TABLE_NAME, null, initialValues);
		}

		public Cursor getDataToExport() {
			Cursor c = db.query(TABLE_NAME, new String[] { KEY_DATE,
					KEY_ACCOUNT, KEY_STOCK, KEY_VOLUME, KEY_PRICE, KEY_NOTES },
					null, null, null, null, KEY_DATE + " ASC", null);
			return c;
		}

	}

	public class T_AmountHistory {
		public static final String TABLE_NAME = "T_AmountHistory";
		public static final String KEY_ID = "_id";
		public static final String KEY_DATE = "A_Date";
		public static final String KEY_ACCOUNT = "A_Account";
		public static final String KEY_AMOUNT = "A_Amount";
		public static final String KEY_NOTES = "A_Notes";

		private static final String CREATE_SQL = "CREATE TABLE IF NOT EXISTS "
				+ TABLE_NAME + " (" + KEY_ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_DATE
				+ " INTEGER, " + KEY_ACCOUNT + " VARCHAR, " + KEY_AMOUNT
				+ " FLOAT, " + KEY_NOTES + " VARCHAR );";

		public long add(String date, long accountId, float amount, String notes) {
			return add(date, account.getName(accountId), amount, notes);
		}

		public long add(String date, String account, float amount, String notes) {
			ContentValues initialValues = new ContentValues();
			initialValues.put(KEY_DATE, DateUtil.parseDate(date).getTime());
			initialValues.put(KEY_ACCOUNT, account);
			initialValues.put(KEY_AMOUNT, amount);
			initialValues.put(KEY_NOTES, notes);
			return db.insert(TABLE_NAME, null, initialValues);
		}

		public Cursor getDataToExport() {
			Cursor c = db.query(TABLE_NAME, new String[] { KEY_DATE,
					KEY_ACCOUNT, KEY_AMOUNT, KEY_NOTES }, null, null, null,
					null, KEY_DATE + " ASC", null);
			return c;
		}
	}

	public class T_Transaction {
		public static final String TABLE_NAME = "T_Transaction";
		public static final String KEY_ID = "_id";
		public static final String KEY_TYPE = "A_Type";
		public static final String KEY_DATE = "A_Date";
		public static final String KEY_ACCOUNT_ID = "A_AccountID";
		public static final String KEY_STOCK_ID = "A_StockID";
		public static final String KEY_INTRADAY = "A_Intraday";
		public static final String KEY_VOLUME = "A_Volume";
		public static final String KEY_PRICE = "A_Price";
		public static final String KEY_VALUE = "A_Value";

		private static final String CREATE_SQL = "CREATE TABLE IF NOT EXISTS "
				+ TABLE_NAME + " (" + KEY_ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_DATE
				+ " INTEGER, " + KEY_ACCOUNT_ID + " INTEGER, " + KEY_STOCK_ID
				+ " INTEGER, " + KEY_TYPE + " INTEGER, " + KEY_INTRADAY
				+ " INTEGER, " + KEY_VOLUME + " INTEGER, " + KEY_PRICE
				+ " FLOAT, " + KEY_VALUE + " FLOAT, " + " FOREIGN KEY( "
				+ KEY_STOCK_ID + ") REFERENCES " + T_Stock.TABLE_NAME + "("
				+ KEY_ID + ") ON DELETE CASCADE," + " FOREIGN KEY( "
				+ KEY_ACCOUNT_ID + ") REFERENCES " + T_Account.TABLE_NAME + "("
				+ KEY_ID + ") ON DELETE CASCADE);";

		public boolean update(long id, String transactionDate,
				String accountName, String stockName, boolean intraday,
				int volume, float price, float value) {
			ContentValues values = new ContentValues();
			values.put(KEY_DATE, DateUtil.parseDate(transactionDate).getTime());
			values.put(KEY_ACCOUNT_ID, account.getId(accountName));
			values.put(KEY_STOCK_ID, stock.getId(stockName));
			values.put(KEY_INTRADAY, intraday);
			values.put(KEY_VOLUME, volume);
			values.put(KEY_PRICE, price);
			values.put(KEY_VALUE, value);
			return db.update(TABLE_NAME, values, KEY_ID + "=?",
					new String[] { String.valueOf(id) }) > 0;
		}

		public long add(TransactionType type, String transactionDate,
				String accountName, String stockName, boolean intraday,
				int volume, float price, float value) {

			return add(type, transactionDate, account.getId(accountName),
					stock.getId(stockName), intraday, volume, price, value);
		}

		public long add(TransactionType type, String transactionDate,
				long accountId, long stockId, boolean intraday, int volume,
				float price, float value) {
			ContentValues initialValues = new ContentValues();
			initialValues.put(KEY_ACCOUNT_ID, accountId);
			initialValues.put(KEY_DATE, DateUtil.parseDate(transactionDate)
					.getTime());
			initialValues.put(KEY_STOCK_ID, stockId);
			initialValues.put(KEY_TYPE, type.ordinal());
			initialValues.put(KEY_INTRADAY, intraday);
			initialValues.put(KEY_PRICE, price);
			initialValues.put(KEY_VOLUME, volume);
			initialValues.put(KEY_VALUE, value);
			return db.insert(TABLE_NAME, null, initialValues);
		}

		public Cursor get(long id) {
			return db
					.query(TABLE_NAME, new String[] { KEY_DATE, KEY_ID,
							KEY_TYPE, KEY_ACCOUNT_ID, KEY_STOCK_ID, KEY_VOLUME,
							KEY_PRICE, KEY_VALUE, KEY_INTRADAY },
							KEY_ID + "=?", new String[] { String.valueOf(id) },
							null, null, null, null);
		}

		public Cursor get() {
			return db.query(TABLE_NAME, new String[] { KEY_DATE, KEY_ID,
					KEY_TYPE, KEY_ACCOUNT_ID, KEY_STOCK_ID, KEY_VOLUME,
					KEY_PRICE, KEY_VALUE, KEY_INTRADAY }, null, null, null,
					null, null, null);
		}

		public Cursor getStockReportOLD(long accountId) {
			return db.query(TABLE_NAME, new String[] { KEY_DATE, KEY_ID,
					KEY_TYPE, KEY_ACCOUNT_ID, KEY_STOCK_ID, KEY_VOLUME,
					KEY_PRICE, KEY_VALUE, KEY_INTRADAY },
					KEY_ACCOUNT_ID + "=?",
					new String[] { String.valueOf(accountId) }, null, null,
					null, null);
		}

		public Cursor getStockReport(long accountId) {
			String sql = "SELECT KEY_ID, KEY_DATE, KEY_ACCOUNT_ID,"
					+ " KEY_PRICE, KEY_VOLUME, KEY_STOCK_ID,KEY_TYPE"
					+ " FROM "
					+ T_Transaction.TABLE_NAME
					+ " WHERE KEY_ACCOUNT_ID=?"
					+ " UNION ALL"
					+ " SELECT t2.KEY_ID, t2.KEY_DATE, KEY_ACCOUNT_ID,"
					+ " t2.KEY_PRICE, t2.KEY_VOLUME,KEY_STOCK_ID,"
					+ " (CASE WHEN t2.KEY_VALUE < 0 THEN DTK_BUY ELSE DTK_SELL END) as KEY_TYPE "
					+ " FROM " + T_Details.TABLE_NAME + " t2 JOIN "
					+ T_Transaction.TABLE_NAME
					+ " t1 on t2.KEY_TRANSACTION_ID = t1.KEY_ID "
					+ " WHERE KEY_ACCOUNT_ID=? ORDER BY " + KEY_DATE;

			// replace double spaces first
			sql = sql
					.replace("  ", " ")
					.replace("KEY_DATE", KEY_DATE)
					.replace("KEY_ID", KEY_ID)
					.replace("KEY_ACCOUNT_ID", KEY_ACCOUNT_ID)
					.replace("KEY_STOCK_ID", KEY_STOCK_ID)
					.replace("KEY_PRICE", KEY_PRICE)
					.replace("KEY_VOLUME", KEY_VOLUME)
					.replace("KEY_TYPE", KEY_TYPE)
					.replace("KEY_VALUE", KEY_VALUE)
					.replace("DTK_BUY",
							String.valueOf(TransactionType.BUY_BACK.ordinal()))
					.replace("DTK_SELL",
							String.valueOf(TransactionType.SELL.ordinal()))
					.replace("KEY_TRANSACTION_ID", T_Details.KEY_TRANSACTION_ID)
					.replace("?", String.valueOf(accountId));

			LogUtil.d("Getting stock report: \n" + sql);

			return db.rawQuery(sql, null);
		}

		public Cursor getStockReport(String accountName) {
			return getStockReport(account.getId(accountName));
		}

		public Cursor getAvailableStocks(String accountName,
				TransactionType type) {
			return getAvailableStocks(account.getId(accountName), type);
		}

		public Cursor getAvailableStocks(long accountId, TransactionType type) {

			String sql = "SELECT t1.KEY_ID, KEY_ACCOUNT_ID,"
					+ " KEY_STOCK_ID, KEY_TYPE, t1.KEY_PRICE, "
					+ " (t1.KEY_VOLUME - IFNULL(SUM(t2.KEY_VOLUME), 0)) as KEY_VOLUME, "
					+ " (t1.KEY_VOLUME - IFNULL(SUM(t2.KEY_VOLUME), 0)) as vol "
					+ " FROM T_TRANSACTION_TABLE_NAME t1 "
					+ " LEFT JOIN T_DETAILS_TABLE_NAME t2 ON t1.KEY_ID=t2.KEY_TRANSACTION_ID	"
					+ " GROUP BY t1.KEY_ID "
					+ " HAVING vol <> 0 AND KEY_ACCOUNT_ID=? AND t1.KEY_TYPE=?";

			sql = sql
					.replace("KEY_PRICE", KEY_PRICE)
					.replace("KEY_STOCK_ID", KEY_STOCK_ID)
					.replace("KEY_ACCOUNT_ID", KEY_ACCOUNT_ID)
					.replace("KEY_VOLUME", KEY_VOLUME)
					.replace("KEY_ID", KEY_ID)
					.replace("KEY_TYPE", KEY_TYPE)
					.replace("T_TRANSACTION_TABLE_NAME", TABLE_NAME)
					.replace("T_DETAILS_TABLE_NAME", T_Details.TABLE_NAME)
					.replace("KEY_TRANSACTION_ID", T_Details.KEY_TRANSACTION_ID);

			LogUtil.d("Getting available stocks\n" + sql);
			return db.rawQuery(sql, new String[] { String.valueOf(accountId),
					String.valueOf(type.ordinal()) });

		}

		private float getStocksValue(long accountId) {
			String sql = "SELECT KEY_ACCOUNT_ID, "
					+ " t1.KEY_VOLUME - IFNULL(Sum(t2.KEY_VOLUME),0) KEY_VOLUME, "
					+ " (CASE WHEN KEY_TYPE=0 THEN "
					+ " IFNULL((t1.KEY_PRICE * t1.KEY_VOLUME),0) - IFNULL(Sum(t2.KEY_PRICE * t2.KEY_VOLUME),0) "
					+ " ELSE "
					+ " IFNULL(Sum(t2.KEY_PRICE * t2.KEY_VOLUME),0) - IFNULL((t1.KEY_PRICE * t1.KEY_VOLUME),0) "
					+ " END) KEY_VALUE"
					+ " FROM T_TRANSACTION_TABLE_NAME t1 "
					+ " LEFT JOIN T_DETAILS_TABLE_NAME t2 on t1.KEY_ID=t2.KEY_TRANSACTION_ID ";
			if (accountId != -1) {
				sql += "WHERE t1.KEY_ACCOUNT_ID=? ";
			}
			sql += " GROUP BY t1.KEY_ID";
			sql = sql
					.replace("KEY_PRICE", KEY_PRICE)
					.replace("KEY_ACCOUNT_ID", KEY_ACCOUNT_ID)
					.replace("KEY_VALUE", KEY_VALUE)
					.replace("KEY_VOLUME", KEY_VOLUME)
					.replace("KEY_ID", KEY_ID)
					.replace("KEY_TYPE", KEY_TYPE)
					.replace("T_TRANSACTION_TABLE_NAME", TABLE_NAME)
					.replace("T_DETAILS_TABLE_NAME", T_Details.TABLE_NAME)
					.replace("KEY_TRANSACTION_ID", T_Details.KEY_TRANSACTION_ID);

			LogUtil.d("Getting stocks value\n" + sql);

			Cursor c;
			if (accountId == -1) {
				c = db.rawQuery(sql, null);
			} else {
				c = db.rawQuery(sql, new String[] { String.valueOf(accountId) });
			}
			LogUtil.d(sql);
			float value = 0;
			if (c != null && c.moveToFirst()) {
				do {
					if (c.getInt(c.getColumnIndex(KEY_VOLUME)) != 0) {
						value += c.getFloat(c.getColumnIndex(KEY_VALUE));
					}
				} while (c.moveToNext());
				c.close();
			}
			return value;
		}

		public float getStocksValue() {
			return getStocksValue(-1);
		}

		public float getStocksValue(String accountName) {
			return getStocksValue(account.getId(accountName));
		}

		public boolean delete(long id) {
			return db.delete(TABLE_NAME, KEY_ID + "=" + id, null) > 0;
		}

		public Cursor getDataToExport() {
			String sql = "SELECT t1.KEY_ID, KEY_DATE, t2.KEY_NAME as KEY_ACCOUNT_ID, "
					+ " t3.KEY_NAME as KEY_STOCK_ID, KEY_INTRADAY, "
					+ " KEY_VOLUME, KEY_PRICE, KEY_VALUE, KEY_TYPE FROM "
					+ TABLE_NAME
					+ " t1 LEFT JOIN "
					+ T_Account.TABLE_NAME
					+ " t2 on KEY_ACCOUNT_ID = t2.KEY_ID"
					+ " LEFT JOIN "
					+ T_Stock.TABLE_NAME + " t3 on KEY_STOCK_ID = t3.KEY_ID";

			sql = sql.replace("KEY_DATE", KEY_DATE)
					.replace("KEY_ACCOUNT_ID", KEY_ACCOUNT_ID)
					.replace("KEY_ID", KEY_ID)
					.replace("KEY_STOCK_ID", KEY_STOCK_ID)
					.replace("KEY_NAME", T_Account.KEY_NAME)
					.replace("KEY_INTRADAY", KEY_INTRADAY)
					.replace("KEY_VOLUME", KEY_VOLUME)
					.replace("KEY_PRICE", KEY_PRICE)
					.replace("KEY_VALUE", KEY_VALUE).replace("KEY_ID", KEY_ID)
					.replace("KEY_TYPE", KEY_TYPE);
			LogUtil.d(sql);
			Cursor c = db.rawQuery(sql, null);

			return c;
		}
	}

	public class T_Details {
		public static final String TABLE_NAME = "T_Details";
		public static final String KEY_ID = "_id";
		public static final String KEY_DATE = "A_Date";
		public static final String KEY_TRANSACTION_ID = "A_TransactionID";
		public static final String KEY_VOLUME = "A_Volume";
		public static final String KEY_PRICE = "A_Price";
		public static final String KEY_VALUE = "A_Value";

		private static final String CREATE_SQL = "CREATE TABLE IF NOT EXISTS "
				+ TABLE_NAME + " (" + KEY_ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_DATE
				+ " INTEGER, " + KEY_TRANSACTION_ID + " INTEGER, " + KEY_VOLUME
				+ " INTEGER, " + KEY_PRICE + " INTEGER, " + KEY_VALUE
				+ " FLOAT, " + " FOREIGN KEY( " + KEY_TRANSACTION_ID
				+ ") REFERENCES " + T_Transaction.TABLE_NAME + "(" + KEY_ID
				+ ") ON DELETE CASCADE );";

		public long add(String date, long transactionId, int volume,
				float price, float value) {
			ContentValues initialValues = new ContentValues();
			initialValues.put(KEY_TRANSACTION_ID, transactionId);
			initialValues.put(KEY_DATE, DateUtil.parseDate(date).getTime());
			initialValues.put(KEY_VOLUME, volume);
			initialValues.put(KEY_PRICE, price);
			initialValues.put(KEY_VALUE, value);
			return db.insert(TABLE_NAME, null, initialValues);
		}

		public Cursor get(long id) {
			return db
					.query(TABLE_NAME, new String[] { KEY_DATE, KEY_ID,
							KEY_VOLUME, KEY_PRICE, KEY_VALUE }, KEY_ID + "=?",
							new String[] { String.valueOf(id) }, null, null,
							null, null);
		}

		public boolean update(long id, int volume, float price, float value) {
			ContentValues args = new ContentValues();
			args.put(KEY_VOLUME, volume);
			args.put(KEY_VALUE, value);
			args.put(KEY_PRICE, price);
			return db.update(TABLE_NAME, args, KEY_ID + "=?",
					new String[] { String.valueOf(id) }) > 0;
		}

		public Cursor getByTransactionId(long transactionId) {
			String sql = "SELECT t2.*, t1.KEY_ACCOUNT_ID, t1.KEY_STOCK_ID,t1.KEY_TYPE "
					+ " FROM T_DETAIL_TABLE_NAME t2 "
					+ " LEFT JOIN T_TRANSACTION_TABLE_NAME t1 ON t2.KEY_TRANSACTION_ID=t1.KEY_ID"
					+ " WHERE t2.KEY_TRANSACTION_ID=?";

			sql = sql
					.replace("T_DETAIL_TABLE_NAME", TABLE_NAME)
					.replace("KEY_TYPE", T_Transaction.KEY_TYPE)
					.replace("KEY_ID", KEY_ID)
					.replace("KEY_ACCOUNT_ID", T_Transaction.KEY_ACCOUNT_ID)
					.replace("KEY_STOCK_ID", T_Transaction.KEY_STOCK_ID)
					.replace("KEY_TRANSACTION_ID", KEY_TRANSACTION_ID)
					.replace("T_TRANSACTION_TABLE_NAME",
							T_Transaction.TABLE_NAME);

			return db.rawQuery(sql,
					new String[] { String.valueOf(transactionId) });
		}

		public long getTransactionId(long id) {
			String sql = "SELECT " + KEY_TRANSACTION_ID + " FROM " + TABLE_NAME
					+ " WHERE " + KEY_ID + "=?";

			Cursor c = db.rawQuery(sql, new String[] { String.valueOf(id) });

			long transactionId = -1;

			if (c != null && c.moveToFirst()) {
				transactionId = c.getInt(c.getColumnIndex(KEY_TRANSACTION_ID));
				c.close();
			}
			return transactionId;
		}

		public int getVolume(long transactionId) {
			String sql = "SELECT " + " SUM(" + KEY_VOLUME + ") as "
					+ KEY_VOLUME + " FROM " + TABLE_NAME + " WHERE "
					+ KEY_TRANSACTION_ID + "=?";

			Cursor c = db.rawQuery(sql,
					new String[] { String.valueOf(transactionId) });

			int volume = 0;
			if (c != null && c.moveToFirst()) {
				volume = c.getInt(c.getColumnIndex(KEY_VOLUME));
				c.close();
			}
			return volume;
		}

		public boolean delete(long id) {
			return db.delete(TABLE_NAME, KEY_ID + "=" + id, null) > 0;
		}

		public Cursor getDataToExport() {
			Cursor c = db.query(TABLE_NAME, new String[] { KEY_DATE,
					KEY_TRANSACTION_ID, KEY_VOLUME, KEY_PRICE, KEY_VALUE },
					null, null, null, null, KEY_DATE + " ASC", null);
			return c;
		}
	}

	public Cursor getAllTables() {
		String sql = "SELECT * FROM sqlite_master";
		return db.rawQuery(sql, new String[0]);
	}

	public void resetAllTables() {
		resetAmountsTable();
		resetTransactionsTable();
		resetDetailsTable();
		resetNifyTable();
		resetTransactionHistoryTable();
		resetAmountHistoryTable();
		resetAccountsTable();
		resetStockTable();
	}

	public void resetAccountsTable() {
		db.execSQL("DROP TABLE IF EXISTS " + T_Account.TABLE_NAME);
		db.execSQL(T_Account.CREATE_SQL);
	}

	public void resetStockTable() {
		db.execSQL("DROP TABLE IF EXISTS " + T_Stock.TABLE_NAME);
		db.execSQL(T_Stock.CREATE_SQL);
	}

	public void resetAmountsTable() {
		db.execSQL("DROP TABLE IF EXISTS " + T_Amount.TABLE_NAME);
		db.execSQL(T_Amount.CREATE_SQL);
	}

	public void resetDetailsTable() {
		db.execSQL("DROP TABLE IF EXISTS " + T_Details.TABLE_NAME);
		db.execSQL(T_Details.CREATE_SQL);
	}

	public void resetNifyTable() {
		db.execSQL("DROP TABLE IF EXISTS " + T_Nifty.TABLE_NAME);
		db.execSQL(T_Nifty.CREATE_SQL);
	}

	public void resetTransactionHistoryTable() {
		db.execSQL("DROP TABLE IF EXISTS " + T_TransactionHistory.TABLE_NAME);
		db.execSQL(T_TransactionHistory.CREATE_SQL);
	}

	public void resetAmountHistoryTable() {
		db.execSQL("DROP TABLE IF EXISTS " + T_AmountHistory.TABLE_NAME);
		db.execSQL(T_AmountHistory.CREATE_SQL);
	}

	public void resetTransactionsTable() {
		db.execSQL("DROP TABLE IF EXISTS " + T_Transaction.TABLE_NAME);
		db.execSQL(T_Transaction.CREATE_SQL);
	}

	public DBAdapter open() throws SQLException {
		db = DBHelper.getWritableDatabase();

		// Enable foreign key constraints
		if (!db.isReadOnly()) {
			db.execSQL("PRAGMA foreign_keys = ON;");
		}
		return this;
	}

	public void close() {
		try {
			DBHelper.close();
		} catch (Exception e) {
			LogUtil.e("Exception occured: " + e);
		}
	}
}
