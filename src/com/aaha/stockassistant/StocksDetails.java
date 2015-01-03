package com.aaha.stockassistant;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.aaha.db.DBAdapter;
import com.aaha.db.DBAdapter.T_Trade;
import com.aaha.stockassistant.util.Constants;
import com.aaha.stockassistant.util.Settings;
import com.aaha.stockassistant.util.Util;

public class StocksDetails extends Activity {

	DBAdapter db;
	Cursor cursor;
	ListView stockList;
	ImageView sellStock;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stocks_details);

		// Show the Up button in the action bar.
		setupActionBar();

		db = new DBAdapter(getApplicationContext());
		db.open();

		stockList = (ListView) findViewById(R.id.stockList);
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
			loadStockDetails();
		} catch (Exception e) {
			e.printStackTrace();
			Util.Toast(getApplicationContext(),
					"Exception occurred while loading stock details");
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
			Util.Toast(getApplicationContext(), "Still under development");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void loadStockDetails() {
		int limit = 0;
		try {
			limit = Settings.getInt(Settings.PREF_STOCKS_LIMIT,
					getApplicationContext(), 0);
		} catch (Exception e) {
			e.printStackTrace();
			Util.Toast(getApplicationContext(),
					"Exception while retrieving limit from settings:" + e);
		}

		if (limit > 0) {
			cursor = db.trade.get(limit);
		} else {
			cursor = db.trade.get();
		}

		String[] databaseColumnNames = new String[] { T_Trade.KEY_STOCK,
				T_Trade.KEY_STOCKS, T_Trade.KEY_BUY_PRICE };

		int[] toViewIDs = new int[] { R.id.item_stock, R.id.item_shares,
				R.id.item_buy_price };

		CustomCursorAdapter mCursorAdapter = new CustomCursorAdapter(this,
				R.layout.table_layout_stocks, cursor, databaseColumnNames,
				toViewIDs);

		mCursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {

			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex) {

				if (view.getClass() != TextView.class) {
					return true;
				}

				switch (view.getId()) {
				case R.id.item_buy_price:
					float price = cursor.getFloat(cursor
							.getColumnIndex(T_Trade.KEY_BUY_PRICE));
					((TextView) view).setText("Bought @ "
							+ String.valueOf(price));

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
		ImageView image;

		@SuppressWarnings("deprecation")
		public CustomCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to) {
			super(context, layout, c, from, to);
			this.context = context;
			this.activity = (Activity) context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			final long id = getItemId(position);
			final ImageView image = (ImageView) view
					.findViewById(R.id.sellStock);

			image.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent i = new Intent(context, TradeStocks.class);
					i.putExtra(Constants.TYPE, Constants.Trade.SELL);
					i.putExtra(Constants.ID, id);
					startActivity(i);
				}
			});
			return view;
		}

	}

}
