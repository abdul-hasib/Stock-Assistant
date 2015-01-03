package com.aaha.stockassistant.backup;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;

import com.aaha.db.DBAdapter;
import com.aaha.db.DBAdapter.T_Account;
import com.aaha.db.DBAdapter.T_Amount;
import com.aaha.db.DBAdapter.T_AmountHistory;
import com.aaha.db.DBAdapter.T_Details;
import com.aaha.db.DBAdapter.T_Nifty;
import com.aaha.db.DBAdapter.T_Stock;
import com.aaha.db.DBAdapter.T_Transaction;
import com.aaha.db.DBAdapter.T_TransactionHistory;
import com.aaha.stockassistant.util.DateUtil;
import com.aaha.stockassistant.util.LogUtil;

/*
 * Exporting data to xml
 */

@SuppressLint("WorldReadableFiles")
public class BackupAssistant {
	private Context _ctx;
	private Exporter _exporter;
	DBAdapter _db;
	Cursor mCursor = null;
	String _fileName = null;

	public BackupAssistant(Context ctx, DBAdapter db, String fileName) {
		_ctx = ctx;
		_db = db;
		_fileName = fileName;

		try {
			File myFile = new File(Environment.getExternalStorageDirectory(),
					fileName);

			myFile.createNewFile();

			FileOutputStream fOut = new FileOutputStream(myFile);
			BufferedOutputStream bos = new BufferedOutputStream(fOut);

			_exporter = new Exporter(bos);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int exportData() {
		int result = 0;
		try {
			_exporter.startDbExport("stockAssistant");
			mCursor = _db.getAllTables();
			LogUtil.d("Show tables, cur size " + mCursor.getCount());
			mCursor.moveToFirst();

			String tableName;
			while (mCursor.getPosition() < mCursor.getCount()) {
				tableName = mCursor.getString(mCursor.getColumnIndex("name"));
				LogUtil.d("Check if '" + tableName
						+ "' table data needs to be exported");
				exportTable(tableName);
				mCursor.moveToNext();
			}
			_exporter.endDbExport();
			_exporter.close();
		} catch (IOException e) {
			e.printStackTrace();
			result = 3;
		} catch (Exception e) {
			e.printStackTrace();
			result = 1;
		} finally {
			if (mCursor != null) {
				mCursor.close();
			}
		}
		return result;
	}

	// add all the tables to export here
	private Cursor getTableDataToExport(String tableName) {
		Cursor mCursor = null;
		if (tableName.equalsIgnoreCase(T_Account.TABLE_NAME)) {
			mCursor = _db.account.getDataToExport();
		} else if (tableName.equalsIgnoreCase(T_Stock.TABLE_NAME)) {
			mCursor = _db.stock.getDataToExport();
		} else if (tableName.equalsIgnoreCase(T_Amount.TABLE_NAME)) {
			mCursor = _db.amount.getDataToExport();
		} else if (tableName.equalsIgnoreCase(T_Transaction.TABLE_NAME)) {
			mCursor = _db.transaction.getDataToExport();
		} else if (tableName.equalsIgnoreCase(T_Details.TABLE_NAME)) {
			mCursor = _db.details.getDataToExport();
		} else if (tableName.equalsIgnoreCase(T_TransactionHistory.TABLE_NAME)) {
			mCursor = _db.transactionHistory.getDataToExport();
		} else if (tableName.equalsIgnoreCase(T_AmountHistory.TABLE_NAME)) {
			mCursor = _db.amountHistory.getDataToExport();
		} else if (tableName.equalsIgnoreCase(T_Nifty.TABLE_NAME)) {
			mCursor = _db.nifty.getDataToExport();
		}
		return mCursor;
	}

	private void exportTable(String tableName) throws IOException {
		Cursor mCursor = getTableDataToExport(tableName);

		if (mCursor == null || !mCursor.moveToFirst()) {
			return;
		}
		LogUtil.d("Exporting data from table: " + tableName);

		_exporter.startTable(tableName);

		// get everything from the table
		int numcols = mCursor.getColumnCount();

		// move through the table, creating rows
		// and adding each column with name and value
		// to the row
		while (mCursor.getPosition() < mCursor.getCount()) {
			_exporter.startRow();
			String name;
			String val;
			for (int idx = 0; idx < numcols; idx++) {
				name = mCursor.getColumnName(idx);
				val = mCursor.getString(idx);
				LogUtil.d("Column name: " + name + " value: " + val);
				if (name.equalsIgnoreCase(T_Transaction.KEY_DATE)) {
					val = DateUtil.formatDate(Long.valueOf(val)).toString();
				}
				_exporter.addColumn(name, val);
			}
			_exporter.endRow();
			mCursor.moveToNext();
		}
		mCursor.close();
		_exporter.endTable();
	}

	class Exporter {
		private static final String CLOSING_WITH_TICK = "'>";
		private static final String START_DB = "<?xml version='1.0' encoding='utf-8'?>"
				+ "\n<export-database name='";
		private static final String END_DB = "</export-database>";
		private static final String START_TABLE = "<table name='";
		private static final String END_TABLE = "</table>";
		private static final String START_ROW = "<row>";
		private static final String END_ROW = "</row>";

		private BufferedOutputStream _bos;

		@SuppressWarnings("deprecation")
		public Exporter() throws FileNotFoundException {
			this(new BufferedOutputStream(_ctx.openFileOutput(_fileName,
					Context.MODE_WORLD_READABLE)));
		}

		public Exporter(BufferedOutputStream bos) {
			_bos = bos;
		}

		public void close() throws IOException {
			if (_bos != null) {
				_bos.close();
			}
		}

		public void startDbExport(String dbName) throws IOException {
			String stg = START_DB + dbName + CLOSING_WITH_TICK;
			_bos.write(stg.getBytes());
		}

		public void endDbExport() throws IOException {
			_bos.write(END_DB.getBytes());
		}

		public void startTable(String tableName) throws IOException {
			String stg = START_TABLE + tableName + CLOSING_WITH_TICK;
			_bos.write(stg.getBytes());
		}

		public void endTable() throws IOException {
			_bos.write(END_TABLE.getBytes());
		}

		public void startRow() throws IOException {
			_bos.write(START_ROW.getBytes());
		}

		public void endRow() throws IOException {
			_bos.write(END_ROW.getBytes());
		}

		public void addColumn(String name, String val) throws IOException {
			String stg = "<" + name + ">" + val + "</" + name + ">";
			_bos.write(stg.getBytes());
		}
	}

}
