package com.aaha.db;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.aaha.db.DBAdapter.T_Entity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;

/*
 * Exporting data to xml
 */

@SuppressLint("WorldReadableFiles")
public class DatabaseBackupAssistant {
	private Context _ctx;
	private Exporter _exporter;
	DBAdapter _db;
	Cursor mCursor = null;
	String _fileName = null;

	public DatabaseBackupAssistant(Context ctx, DBAdapter db, String fileName) {
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
		try {
			_exporter.startDbExport("al-salah");
			mCursor = _db.getAllTables();
			Log.d(DBAdapter.TAG, "show tables, cur size " + mCursor.getCount());
			mCursor.moveToFirst();

			String tableName;
			while (mCursor.getPosition() < mCursor.getCount()) {
				tableName = mCursor.getString(mCursor.getColumnIndex("name"));

				// process only these tables
				if (tableName.equalsIgnoreCase(T_Entity.TABLE_NAME)) {
					exportTable(tableName);
				}

				mCursor.moveToNext();
			}
			_exporter.endDbExport();
			_exporter.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (mCursor != null) {
				mCursor.close();
			}
		}
		return 0;
	}

	private void exportTable(String tableName) throws IOException {
		_exporter.startTable(tableName);

		Cursor mCursor = null;
		if (tableName.equalsIgnoreCase(T_Entity.TABLE_NAME)) {
			mCursor = _db.entity.getEntitiesToExport();
		}

		if (mCursor == null) {
			return;
		}

		// get everything from the table
		int numcols = mCursor.getColumnCount();

		mCursor.moveToFirst();

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

				// if (name.equalsIgnoreCase(Stock.KEY_DATE)) {
				// val = Util.formatDate(Long.valueOf(val) * 1000).toString();
				// }

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
		private static final String START_DB = "<export-database name='";
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
