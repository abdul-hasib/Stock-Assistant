package com.aaha.stockassistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aaha.db.DBAdapter;
import com.aaha.stockassistant.backup.BackupRestore;
import com.aaha.stockassistant.manage.Account;
import com.aaha.stockassistant.manage.Amount;
import com.aaha.stockassistant.manage.Stock;
import com.aaha.stockassistant.report.Report;
import com.aaha.stockassistant.shares.Available;
import com.aaha.stockassistant.shares.MainTransaction;
import com.aaha.stockassistant.trend.Nifty;
import com.aaha.stockassistant.util.Constants;
import com.aaha.stockassistant.util.LogUtil;
import com.aaha.stockassistant.util.NAVUtil;
import com.aaha.stockassistant.util.Settings;
import com.aaha.stockassistant.util.SharesUtil;
import com.aaha.stockassistant.util.StringUtil;
import com.aaha.stockassistant.util.Constants.TransactionType;

public class Home extends Activity {
	TextView tvStockValue, tvCashValue, tvTotalValue;
	Button btnShortShares, btnBuyBackShares, btnBuyShares, btnSellShares;
	DBAdapter db;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);

		db = new DBAdapter(getApplicationContext());
		db.open();

		tvStockValue = (TextView) findViewById(R.id.tvStockValue);
		tvCashValue = (TextView) findViewById(R.id.tvCashValue);
		tvTotalValue = (TextView) findViewById(R.id.tvTotalValue);

		btnShortShares = (Button) findViewById(R.id.btnShortShares);
		btnBuyBackShares = (Button) findViewById(R.id.btnBuyBackShares);
		btnBuyShares = (Button) findViewById(R.id.btnBuyShares);
		btnSellShares = (Button) findViewById(R.id.btnSellShares);
	}

	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();
	}

	@Override
	protected void onResume() {

		float stockValue = db.transaction.getStocksValue();
		setSummaryValue(tvStockValue, stockValue);

		float cashValue = db.amount.getTotalCash();
		setSummaryValue(tvCashValue, cashValue);

		setSummaryValue(tvTotalValue, stockValue + cashValue);

		setTransactionOptionsState(true);

		super.onResume();
	}

	private void setTransactionOptionsState(boolean disable) {
		LinearLayout transactionOptionsLayout = (LinearLayout) findViewById(R.id.transactionOptionsLayout);

		if (disable)
			transactionOptionsLayout.setVisibility(View.GONE);
		else
			transactionOptionsLayout.setVisibility(View.VISIBLE);
	}

	private void setSummaryValue(TextView tvTextView, float value) {

		tvTextView.setText(StringUtil.round(value, 0));
		if (value < 0) {
			tvTextView.setTextColor(Color.RED);
		} else {
			tvTextView.setTextColor(Color.rgb(0, 128, 0));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.home, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		switch (item.getItemId()) {
		case R.id.action_backup:
			i = new Intent(this, BackupRestore.class);
			startActivity(i);
			break;
		case R.id.action_settings:
			i = new Intent(this, Settings.class);
			startActivity(i);
			break;
		case R.id.action_add_account:
			i = new Intent(this, Account.class);
			startActivity(i);
			break;
		case R.id.action_add_stock:
			i = new Intent(this, Stock.class);
			startActivity(i);
			break;
		case R.id.action_factory_reset:
			doFactoryReset();
			NAVUtil.resetNAV(getApplicationContext(), 0,
					SharesUtil.getTotalValue(db));
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void doFactoryReset() {
		AlertDialog.Builder alert = new Builder(this);
		alert.setTitle("Alert");
		alert.setIcon(android.R.drawable.ic_delete);
		alert.setMessage("This removes all transactions data"
				+ " and it can not be undone!\n Are you sure?");

		alert.setPositiveButton("Reset", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				db.resetAllTables();
				onResume();
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});
		alert.show();
	}

	public void startTransactSharesActivity(View view) {
		setTransactionOptionsState(false);
	}

	public void startBuySharesActivity(View view) {
		Intent i = new Intent(this, MainTransaction.class);
		i.putExtra(Constants.TRANSACTION_TYPE, TransactionType.BUY);
		startActivity(i);
	}

	public void startSellSharesActivity(View view) {
		Intent i = new Intent(this, Available.class);
		i.putExtra(Constants.TRANSACTION_TYPE, TransactionType.BUY);
		startActivity(i);
	}

	public void startBuyBackActivity(View view) {
		Intent i = new Intent(this, Available.class);
		i.putExtra(Constants.TRANSACTION_TYPE, TransactionType.SHORT);
		startActivity(i);
	}

	public void startShortSharesActivity(View view) {
		Intent i = new Intent(this, MainTransaction.class);
		i.putExtra(Constants.TRANSACTION_TYPE, TransactionType.SHORT);
		startActivity(i);
	}

	public void startReportActivity(View view) {
		Intent i = new Intent(this, Report.class);
		startActivity(i);
	}

	public void startAmountActivity(View view) {
		Intent i = new Intent(this, Amount.class);
		startActivity(i);
	}

	public void startNiftyActivity(View view) {
		if (NAVUtil.getNAV(getApplicationContext()) == 0.0) {
			// Intent i = new Intent(this, NetAssetValue.class);
			// startActivity(i);

			AlertDialog.Builder alert = new Builder(this);
			alert.setTitle("Net Asset Value (NAV)");
			alert.setMessage("Set NAV to compare with NIFTY");

			final EditText input = new EditText(this);
			input.setSingleLine();
			input.setInputType(InputType.TYPE_CLASS_NUMBER);
			alert.setView(input);

			alert.setPositiveButton("Save",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							saveNAV(input.getText().toString());
						}
					});

			alert.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// Canceled.
						}
					});
			alert.show();

		} else {
			Intent i = new Intent(this, Nifty.class);
			startActivity(i);
		}
	}

	private void saveNAV(String nav) {

		try {
			float navValue = Float.valueOf(nav);

			if (navValue <= 0) {
				return;
			}

			Settings.putPref(Settings.PREF_NAV, nav, getApplicationContext());
			DBAdapter db = new DBAdapter(getApplicationContext());
			db.open();
			float totalUnits = (SharesUtil.getTotalValue(db) / navValue);
			db.close();

			Settings.putPref(Settings.PREF_UNITS, String.valueOf(totalUnits),
					getApplicationContext());
			Intent i = new Intent(this, Nifty.class);
			startActivity(i);
		} catch (Exception e) {
			LogUtil.toastShort(getApplicationContext(),
					"Error occurred while saving NAV");
		}

	}
}
