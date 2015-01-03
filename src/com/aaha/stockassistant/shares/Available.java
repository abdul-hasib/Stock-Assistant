package com.aaha.stockassistant.shares;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.aaha.db.DBAdapter;
import com.aaha.db.DBAdapter.T_Transaction;
import com.aaha.stockassistant.R;
import com.aaha.stockassistant.util.Constants;
import com.aaha.stockassistant.util.Constants.TransactionType;
import com.aaha.stockassistant.util.DropDownListHelper;
import com.aaha.stockassistant.util.LogUtil;

public class Available extends Activity {

	DBAdapter db;
	Cursor cursor;
	ListView stockList;
	Spinner spinnerAccount;
	ImageView sellStock;
	TransactionType transactionType;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_available_stocks);

		// Show the Up button in the action bar.
		setupActionBar();

		db = new DBAdapter(getApplicationContext());
		db.open();

		spinnerAccount = (Spinner) findViewById(R.id.availableStocksAccountName);
		stockList = (ListView) findViewById(R.id.stockList);

		Bundle extras = getIntent().getExtras();
		transactionType = (TransactionType) extras
				.get(Constants.TRANSACTION_TYPE);

		spinnerAccount.setAdapter(DropDownListHelper
				.getAccountAdapter(this, db));
		spinnerAccount.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent,
					View selectedItemView, int position, long id) {
				loadAvailableStocks(transactionType);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
				// your code here
			}
		});
	}

	@Override
	protected void onDestroy() {
		try {
			cursor.close();
		} catch (Exception e) {
		}
		try {
			db.close();
		} catch (Exception e) {

		}

		super.onDestroy();
	}

	@Override
	protected void onResume() {
		try {
			loadAvailableStocks(transactionType);
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.toastShort(getApplicationContext(),
					"Exception occurred while loading available stocks");
		}
		super.onResume();
	}

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
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.stocks_details, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.action_sort:
			LogUtil.toastShort(getApplicationContext(), "Still under development");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void loadAvailableStocks(TransactionType type) {

		if (spinnerAccount.getChildCount() <= 0) {
			return;
		}

		String account = spinnerAccount.getSelectedItem().toString();
		cursor = db.transaction.getAvailableStocks(account, type);

		String[] databaseColumnNames = new String[] {
				T_Transaction.KEY_STOCK_ID, T_Transaction.KEY_VOLUME,
				T_Transaction.KEY_PRICE };

		int[] toViewIDs = new int[] { R.id.item_stock_name,
				R.id.item_shares_volume, R.id.item_buy_price };

		CustomCursorAdapter mCursorAdapter = new CustomCursorAdapter(this,
				R.layout.layout_available_stocks, cursor,
				databaseColumnNames, toViewIDs);

		mCursorAdapter.setViewBinder(new CustomCursorAdapter.ViewBinder() {

			@Override
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex) {

				int type = cursor.getInt(cursor
						.getColumnIndex(T_Transaction.KEY_TYPE));
				TransactionType transactionType = TransactionType.values()[type];

				if (cursor.getColumnName(columnIndex).equalsIgnoreCase(
						T_Transaction.KEY_STOCK_ID)) {
					long tempId = cursor.getLong(cursor
							.getColumnIndex(T_Transaction.KEY_STOCK_ID));
					((TextView) view).setText(db.stock.getName(tempId));
					return true;
				} else if (cursor.getColumnName(columnIndex).equalsIgnoreCase(
						T_Transaction.KEY_PRICE)) {
					TextView tv = ((TextView) view);

					String temp = "Bought @";
					if (transactionType == TransactionType.SHORT) {
						temp = "Short @";
					}
					tv.setText(temp
							+ cursor.getInt(cursor
									.getColumnIndex(T_Transaction.KEY_PRICE)));
					((TextView) view).setTextColor(Color.rgb(0, 128, 0));
					return true;
				} else if (cursor.getColumnName(columnIndex).equalsIgnoreCase(
						T_Transaction.KEY_VOLUME)) {

					int totalShares = cursor.getInt(cursor
							.getColumnIndex(T_Transaction.KEY_VOLUME));
					((TextView) view).setText(String.valueOf(Math
							.abs(totalShares)));

					if (transactionType == TransactionType.SHORT) {
						((TextView) view).setTextColor(Color.RED);
					} else {
						((TextView) view).setTextColor(Color.rgb(0, 128, 0));

					}
					((TextView) view).setText(String.valueOf(totalShares));
					return true;
				}
				return false;
			}

		});

		stockList.setAdapter(mCursorAdapter);
	}

	class CustomCursorAdapter extends SimpleCursorAdapter {
		Context context;
		Activity activity;
		Cursor cursor;

		@SuppressWarnings("deprecation")
		public CustomCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to) {
			super(context, layout, c, from, to);
			this.context = context;
			this.cursor = c;
			this.activity = (Activity) context;
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			View view = super.getView(position, convertView, parent);

			final ImageView image = (ImageView) view
					.findViewById(R.id.sellStock);
			final TextView sellableShares = (TextView) view
					.findViewById(R.id.item_shares_volume);

			image.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent i = new Intent(context, TradeExisting.class);
					if (cursor.moveToPosition(position)) {
						TransactionType transactionType = TransactionType
								.values()[cursor.getInt(cursor
								.getColumnIndex(T_Transaction.KEY_TYPE))];

						switch (transactionType) {
						case SHORT:
							i.putExtra(Constants.TRANSACTION_TYPE,
									TransactionType.BUY_BACK);
							break;
						case BUY:
							i.putExtra(Constants.TRANSACTION_TYPE,
									TransactionType.SELL);
							break;
						default:
							LogUtil.d("invalid transaction type: "
									+ transactionType);
						}

						i.putExtra(Constants.TRANSACTION_ID, cursor
								.getLong(cursor
										.getColumnIndex(T_Transaction.KEY_ID)));
						i.putExtra(Constants.ALLOWED_SHARES, Integer
								.valueOf(sellableShares.getText().toString()));
						startActivity(i);
					} else {
						LogUtil.toastShort(context, "Some error occurred");
					}
				}
			});
			return view;
		}
	}
}
