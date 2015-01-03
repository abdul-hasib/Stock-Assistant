package com.aaha.stockassistant.shares;

import java.util.Calendar;
import java.util.Date;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;

import com.aaha.db.DBAdapter;
import com.aaha.db.DBAdapter.T_Account;
import com.aaha.db.DBAdapter.T_Transaction;
import com.aaha.stockassistant.R;
import com.aaha.stockassistant.util.Constants;
import com.aaha.stockassistant.util.Constants.TransactionType;
import com.aaha.stockassistant.util.DateUtil;
import com.aaha.stockassistant.util.DropDownListHelper;
import com.aaha.stockassistant.util.HistoryUtil;
import com.aaha.stockassistant.util.LogUtil;
import com.aaha.stockassistant.util.NAVUtil;
import com.aaha.stockassistant.util.SharesUtil;
import com.aaha.stockassistant.util.StringUtil;

public class TradeExisting extends Activity {

	public int year, month, day, hour, minute;
	static final int DATE_DIALOG_ID = 1;

	Switch swtIntraday;
	Spinner spinnerAccount, spinnerStock;
	EditText etPrice, etVolume, etBrokerage;
	DBAdapter db;
	Button btnAdd, btnSelectDate;

	private TransactionType globalTransactionType;
	private long globalTransactionId;
	private int sellableShares = 0;

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
		setContentView(R.layout.activity_stock_trade);

		// Show the Up button in the action bar.
		setupActionBar();

		db = new DBAdapter(getApplicationContext());
		db.open();

		swtIntraday = (Switch) findViewById(R.id.swtIntraday);
		btnSelectDate = (Button) findViewById(R.id.btnSelectDate);
		spinnerAccount = (Spinner) findViewById(R.id.spinnerAcount);
		spinnerStock = (Spinner) findViewById(R.id.spinnerStock);
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

		swtIntraday.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				calculateBrokerage();
			}
		});

		etBrokerage = (EditText) findViewById(R.id.etBroker);

		spinnerAccount.setAdapter(DropDownListHelper
				.getAccountAdapter(this, db));
		spinnerStock.setAdapter(DropDownListHelper.getStockAdapter(this, db));

		setDate(new Date());

		Bundle extras = getIntent().getExtras();
		globalTransactionType = (TransactionType) extras
				.get(Constants.TRANSACTION_TYPE);
		sellableShares = extras.getInt(Constants.ALLOWED_SHARES);

		globalTransactionId = extras.getLong(Constants.TRANSACTION_ID);

		btnAdd.setText(globalTransactionType == TransactionType.BUY_BACK ? getResources()
				.getString(R.string.buy_back) : getResources().getString(
				R.string.sell));

		setSellValidations(globalTransactionId);

	}

	private void setDate(Date date) {
		Date today = new Date();
		if (date.compareTo(today) > 0) {
			LogUtil.toastShort(getApplicationContext(),
					"Thanks for testing, Please select correct date");
		} else {
			btnSelectDate.setText(DateUtil.formatDate(date.getTime()));
		}
	}

	@SuppressWarnings("deprecation")
	public void SetDate(View view) {
		showDialog(DATE_DIALOG_ID);
	}

	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int yearSelected,
				int monthOfYear, int dayOfMonth) {
			year = yearSelected;
			month = monthOfYear + 1;
			day = dayOfMonth;
			Date date = new Date();
			try {
				date = DateUtil.parseDate(day + "/" + month + "/" + year);
			} catch (Exception e) {
				LogUtil.toastShort(getApplicationContext(),
						"Unexpected error while parsing date");
			}
			setDate(date);
		}
	};

	@SuppressWarnings("unchecked")
	private void setSellValidations(long transactionId) {

		Cursor c = db.transaction.get(transactionId);

		if (c == null || !c.moveToFirst()) {
			return;
		}
		LogUtil.d("transactionId: " + transactionId);
		String account = db.account.getName(c.getLong(c
				.getColumnIndex(T_Transaction.KEY_ACCOUNT_ID)));
		LogUtil.d("account: " + account);

		String stock = db.stock.getName(c.getLong(c
				.getColumnIndex(T_Transaction.KEY_STOCK_ID)));

		spinnerAccount.setSelection(((ArrayAdapter<String>) spinnerAccount
				.getAdapter()).getPosition(account));
		spinnerAccount.setEnabled(false);

		spinnerStock.setSelection(((ArrayAdapter<String>) spinnerStock
				.getAdapter()).getPosition(stock));
		spinnerStock.setEnabled(false);

		String temp = "Bought @";
		TransactionType type = TransactionType.values()[c.getInt(c
				.getColumnIndex(T_Transaction.KEY_TYPE))];
		if (type == TransactionType.SHORT) {
			temp = "Short @";
		}

		etPrice.setHint(temp
				+ c.getString(c.getColumnIndex(T_Transaction.KEY_PRICE)));
		etVolume.setHint(String.valueOf(sellableShares));

		swtIntraday.setChecked(c.getInt(c
				.getColumnIndex(T_Transaction.KEY_INTRADAY)) == 1);
		swtIntraday.setEnabled(false);
	}

	@Override
	protected void onDestroy() {
		if (db != null) {
			db.close();
		}
		super.onDestroy();
	}

	private void calculateBrokerage() {

		if (spinnerAccount.getChildCount() == 0
				|| spinnerStock.getChildCount() == 0) {
			return;
		}
		String accountName = spinnerAccount.getSelectedItem().toString();
		boolean intraday = swtIntraday.isChecked();

		float brokerage = 0;
		float price = getPrice();
		int vol = getVolume();

		if (price < 0 || vol < 0) {
			etBrokerage.setText("");
			return;
		}

		Cursor c = db.account.get(accountName);
		if (c != null && c.moveToFirst()) {
			if (intraday)
				brokerage = c.getFloat(c
						.getColumnIndex(T_Account.KEY_INTRADAY_BROKERAGE));
			else
				brokerage = c.getFloat(c
						.getColumnIndex(T_Account.KEY_BROKERAGE));
			c.close();
		}
		etBrokerage.setText(StringUtil.round(price * vol * brokerage, 2));
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
		return getValue(etBrokerage);
	}

	private int getVolume() {
		return (int) getValue(etVolume);
	}

	public void addTransaction(View view) {

		if (spinnerAccount.getChildCount() <= 0
				|| spinnerStock.getChildCount() <= 0) {
			LogUtil.toastShort(getApplicationContext(),
					"Do you know the stocks you bought?");
			return;
		}

		float price = getPrice();
		int volume = getVolume();
		float brokerage = getBrokerage();

		if (price <= 0 || volume <= 0) {
			LogUtil.toastShort(getApplicationContext(),
					"How many stocks you bought?");
			return;
		}

		if (volume > sellableShares) {
			LogUtil.toastShort(getApplicationContext(),
					"You don't own so many stocks, please buy!");
			return;
		}

		float value = (price * volume);
		if (globalTransactionType == TransactionType.SELL) {
			value -= brokerage;
		} else {
			value += brokerage;
			value *= -1;
		}

		String date = btnSelectDate.getText().toString();
		long transactionDetailsId = db.details.add(date, globalTransactionId,
				volume, price, value);

		if (transactionDetailsId > -1) {
			LogUtil.toastShort(getApplicationContext(), "Transaction completed");

			HistoryUtil.addTransaction(db, date, spinnerStock.getSelectedItem()
					.toString(), spinnerAccount.getSelectedItem().toString(),
					globalTransactionType, volume, price);

			NAVUtil.updateNAV(getApplicationContext(),
					SharesUtil.getTotalValue(db));
			this.finish();
		} else {
			LogUtil.toastShort(getApplicationContext(),
					"Failed to complete transaction");
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DATE_DIALOG_ID:
			Calendar c = Calendar.getInstance();
			year = c.get(Calendar.YEAR);
			month = c.get(Calendar.MONTH);
			day = c.get(Calendar.DAY_OF_MONTH);

			return new DatePickerDialog(this, mDateSetListener, year, month,
					day);
		}
		return null;
	}
}
