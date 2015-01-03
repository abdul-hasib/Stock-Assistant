package com.aaha.stockassistant.manage;

import java.util.Calendar;
import java.util.Date;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.aaha.db.DBAdapter;
import com.aaha.db.DBAdapter.T_Amount;
import com.aaha.stockassistant.R;
import com.aaha.stockassistant.util.Constants;
import com.aaha.stockassistant.util.Constants.AmountType;
import com.aaha.stockassistant.util.DateUtil;
import com.aaha.stockassistant.util.DropDownListHelper;
import com.aaha.stockassistant.util.HistoryUtil;
import com.aaha.stockassistant.util.LogUtil;
import com.aaha.stockassistant.util.NAVUtil;
import com.aaha.stockassistant.util.SharesUtil;

public class Amount extends Activity {

	public int year, month, day, hour, minute;
	static final int DATE_DIALOG_ID = 1;

	DBAdapter db;
	EditText etAmount;
	Button addRemoveAmount;
	RadioGroup rgOption;
	Button btnSelectDate, btnAddAdmount;
	Spinner spinnerAccount;

	long globalTransactionId = 0;

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

		// Show the Up button in the action bar.
		setupActionBar();

		db = new DBAdapter(getApplicationContext());
		db.open();

		setContentView(R.layout.activity_add_amount);

		etAmount = (EditText) findViewById(R.id.etAmmount);
		rgOption = (RadioGroup) findViewById(R.id.radioOptions);
		btnSelectDate = (Button) findViewById(R.id.btnSelectDate);
		btnAddAdmount = (Button) findViewById(R.id.btnAddAmount);
		spinnerAccount = (Spinner) findViewById(R.id.spinnerAccount);

		setDate(new Date());
		spinnerAccount.setAdapter(DropDownListHelper
				.getAccountAdapter(this, db));

		if (getIntent().hasExtra(Constants.TRANSACTION_ID)) {
			globalTransactionId = getIntent().getExtras().getLong(
					Constants.TRANSACTION_ID);
			loadAmountDetails(globalTransactionId);
		}

	}

	@SuppressWarnings("unchecked")
	private void loadAmountDetails(long id) {
		Cursor c = db.amount.get(id);

		if (c == null || !c.moveToFirst()) {
			return;
		}

		String amount = String.valueOf(Math.abs(c.getInt(c
				.getColumnIndex(T_Amount.KEY_VALUE))));

		int type = c.getInt(c.getColumnIndex(T_Amount.KEY_TYPE));
		String date = DateUtil.formatDate(c.getLong(c
				.getColumnIndex(T_Amount.KEY_DATE)));
		String account = db.account.getName(c.getLong(c
				.getColumnIndex(T_Amount.KEY_ACCOUNT_ID)));

		etAmount.setText(amount);
		btnSelectDate.setText(date);
		spinnerAccount.setSelection(((ArrayAdapter<String>) spinnerAccount
				.getAdapter()).getPosition(account));

		if (type == Constants.AmountType.DEPOSIT.ordinal()) {
			rgOption.check(R.id.rbDeposit);
		} else {
			rgOption.check(R.id.rbWithdraw);
		}

		btnAddAdmount.setText(getResources().getString(R.string.update));

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

	public void addTransaction(View view) {

		if (spinnerAccount.getChildCount() <= 0) {
			LogUtil.toastShort(getApplicationContext(), "Please select account");
			return;
		}

		String amount = etAmount.getText().toString().trim();
		if (amount.isEmpty()) {
			LogUtil.toastShort(getApplicationContext(), "Please enter amount");
			return;
		}
		String accountName = spinnerAccount.getSelectedItem().toString();
		String date = btnSelectDate.getText().toString();

		AmountType amountType;
		if (rgOption.getCheckedRadioButtonId() == R.id.rbDeposit) {
			amountType = AmountType.DEPOSIT;
		} else {
			amountType = AmountType.WITHDRAW;
		}

		float amountValue = Float.valueOf(amount);

		boolean transactionCompleted = false;
		if (globalTransactionId != 0) {
			transactionCompleted = db.amount.update(globalTransactionId, date,
					accountName, amountValue, amountType);
			LogUtil.toastShort(getApplicationContext(), "Amount updated");
		} else {
			transactionCompleted = db.amount.add(date, accountName,
					amountValue, amountType) > 0;
			LogUtil.toastShort(getApplicationContext(), "Amount added");
		}

		if (transactionCompleted) {
			HistoryUtil.addAmmount(db, date, spinnerAccount.getSelectedItem()
					.toString(), amountType, amountValue);

			NAVUtil.updateUnits(getApplicationContext(),
					SharesUtil.getTotalValue(db));
			this.finish();
		} else {
			LogUtil.toastShort(getApplicationContext(),
					"Failed to add/update amount");
		}
	}
}
