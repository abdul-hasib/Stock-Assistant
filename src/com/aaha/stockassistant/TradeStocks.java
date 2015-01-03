package com.aaha.stockassistant;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;

import com.aaha.db.DBAdapter;
import com.aaha.db.DBAdapter.T_Trade;
import com.aaha.stockassistant.util.Constants;
import com.aaha.stockassistant.util.Settings;
import com.aaha.stockassistant.util.Util;

public class TradeStocks extends Activity {

	CheckBox cbIntraday;
	Spinner accountName, stockName;
	EditText etPrice, etVolume, etBroker;
	DBAdapter db;
	Button btnAdd;

	Cursor sellStockCursor = null;

	private String tradeType;
	private long tradeId;

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_trading);

		// Show the Up button in the action bar.
		setupActionBar();

		db = new DBAdapter(getApplicationContext());
		db.open();

		cbIntraday = (CheckBox) findViewById(R.id.cbIntraday);
		accountName = (Spinner) findViewById(R.id.spinnerAcount);
		stockName = (Spinner) findViewById(R.id.spinnerStock);
		etPrice = (EditText) findViewById(R.id.etPrice);
		etVolume = (EditText) findViewById(R.id.etVolume);
		btnAdd = (Button) findViewById(R.id.btnAddTransaction);

		etPrice.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_UP)
					calculateBrokerage();
				return false;
			}
		});

		etVolume.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_UP)
					calculateBrokerage();
				return false;
			}
		});

		cbIntraday.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				calculateBrokerage();
			}
		});

		etBroker = (EditText) findViewById(R.id.etBroker);

		accountName.setAdapter(getDataAdapter(Constants.Entity.ACCOUNT));
		stockName.setAdapter(getDataAdapter(Constants.Entity.STOCK));

		Bundle extras = getIntent().getExtras();
		tradeType = extras.getString(Constants.TYPE);
		btnAdd.setText(tradeType);

		if (tradeType.equals(Constants.Trade.SELL)) {

			tradeId = extras.getLong(Constants.ID);
			sellStockCursor = db.trade.get(tradeId);
			if (sellStockCursor != null) {
				sellStockCursor.moveToFirst();
			}
			try {
				setSellValidations(tradeId);
			} catch (Exception e) {
				Util.Toast(getApplicationContext(),
						"Unable to load the stock details");
				this.finish();
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void setSellValidations(long tradeId) {
		accountName.setEnabled(false);
		stockName.setEnabled(false);

		accountName
				.setSelection(((ArrayAdapter<String>) accountName.getAdapter())
						.getPosition(sellStockCursor.getString(sellStockCursor
								.getColumnIndex(T_Trade.KEY_ACCOUNT))));
		stockName.setSelection(((ArrayAdapter<String>) stockName.getAdapter())
				.getPosition(sellStockCursor.getString(sellStockCursor
						.getColumnIndex(T_Trade.KEY_STOCK))));

		etVolume.setText(String.valueOf(sellStockCursor.getInt(sellStockCursor
				.getColumnIndex(T_Trade.KEY_STOCKS))));

		int intraday = sellStockCursor.getInt(sellStockCursor
				.getColumnIndex(T_Trade.KEY_INTRADAY));
		Util.Toast(getApplicationContext(), intraday);
		cbIntraday.setChecked(intraday == 1);
		cbIntraday.setEnabled(false);
	}

	@Override
	protected void onDestroy() {
		if (sellStockCursor != null) {
			sellStockCursor.close();
		}

		if (db != null) {
			db.close();
		}

		super.onDestroy();
	}

	private void calculateBrokerage() {

		float price = getPrice();
		int vol = getVolume();

		if (price < 0 || vol < 0) {
			etBroker.setText("");
			return;
		}

		float brokerage = 0;

		if (cbIntraday.isChecked()) {
			brokerage = Settings.getFloat(Settings.PREF_BROKERAGE_INTRADAY,
					getApplicationContext(), 0);
		} else {
			brokerage = Settings.getFloat(Settings.PREF_BROKERAGE,
					getApplicationContext(), 0);
		}

		etBroker.setText(String.valueOf(price * vol * brokerage));
	}

	private float getValue(EditText editText) {
		float price = -1;

		String value = editText.getText().toString().trim();
		if (value.isEmpty()) {
			return price;
		}

		try {
			price = Float.valueOf(value);
		} catch (NumberFormatException e) {
			return price;
		}

		return price;
	}

	private float getPrice() {
		return getValue(etPrice);
	}

	private float getBrokerage() {
		return getValue(etBroker);
	}

	private int getVolume() {
		return (int) getValue(etVolume);
	}

	private ArrayAdapter<String> getDataAdapter(String entity) {
		// Spinner Drop down elements
		List<String> lables = getAllNames(entity);

		// Creating adapter for spinner
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, lables);

		// Drop down layout style - list view with radio button
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		return dataAdapter;
	}

	private List<String> getAllNames(String entity) {
		List<String> names = new ArrayList<String>();

		Cursor cursor = db.entity.get(entity);

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

	public void addTransaction(View view) {

		if (accountName.getChildCount() <= 0 || stockName.getChildCount() <= 0) {
			Util.Toast(getApplicationContext(),
					"Do you know the stocks you bought?");
			return;
		}

		String account = accountName.getSelectedItem().toString().trim();
		String stock = stockName.getSelectedItem().toString().trim();

		float price = getPrice();
		int vol = getVolume();
		float brokerage = getBrokerage();

		if (price <= 0) {
			Util.Toast(getApplicationContext(), "What is the stock price?");
			return;
		}

		if (price <= 0 || vol <= 0) {
			Util.Toast(getApplicationContext(), "How many stocks you bought?");
			return;
		}

		if (tradeType.equalsIgnoreCase(Constants.Trade.BUY)) {
			boolean intraday = cbIntraday.isChecked();
			if (db.trade.buy("7/7/2014", account, stock, price, vol, brokerage,
					intraday) > -1) {
				Util.Toast(getApplicationContext(),
						"Stocks are in you account now! Happy trading");
			} else {
				Util.Toast(getApplicationContext(), "Failed to add transaction");
			}
		} else {
			int sellableShares = sellStockCursor.getInt(sellStockCursor
					.getColumnIndex(T_Trade.KEY_STOCKS));

			if (vol > sellableShares) {
				Util.Toast(getApplicationContext(),
						"You don't own so many stocks, please buy more stocks");
				return;
			}

			int remainingShares = sellableShares - vol;
			String prevPrices = sellStockCursor.getString(sellStockCursor
					.getColumnIndex(T_Trade.KEY_PREV_PRICES));

			prevPrices = prevPrices == null ? String.valueOf(price)
					: prevPrices + "," + String.valueOf(price);

			int prevVol = sellStockCursor.getInt(sellStockCursor
					.getColumnIndex(T_Trade.KEY_SELL_VOLUME));

			float prevBrokerage = sellStockCursor.getInt(sellStockCursor
					.getColumnIndex(T_Trade.KEY_SELL_BROKERAGE));

			String[] soldPrices = prevPrices.split(",");

			// calculate the average price
			if (soldPrices.length > 0) {
				for (String soldPrice : soldPrices) {
					price = price + Float.valueOf(soldPrice);
				}
				price = price / soldPrices.length;
			}
			if (db.trade.sell(tradeId, "7/7/2014", account, stock, price, vol
					+ prevVol, brokerage + prevBrokerage, prevPrices,
					remainingShares) > -1) {
				Util.Toast(getApplicationContext(), "Stocks sold");
				this.finish();
			} else {
				Util.Toast(getApplicationContext(), "Failed to add transaction");
			}
		}

	}
}
