package com.aaha.stockassistant.report;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.aaha.db.DBAdapter;
import com.aaha.db.DBAdapter.T_Amount;
import com.aaha.stockassistant.R;
import com.aaha.stockassistant.manage.Amount;
import com.aaha.stockassistant.util.Constants;
import com.aaha.stockassistant.util.Constants.AmountType;
import com.aaha.stockassistant.util.DateUtil;
import com.aaha.stockassistant.util.DropDownListHelper;
import com.aaha.stockassistant.util.LogUtil;
import com.aaha.stockassistant.util.NAVUtil;
import com.aaha.stockassistant.util.SharesUtil;
import com.aaha.stockassistant.util.StringUtil;

public class Cash extends Fragment {
	/**
	 * The fragment argument representing the section number for this fragment.
	 */
	Spinner spinnerAccountName;
	ListView amountList;
	Context context;
	Cursor cursor;
	TextView reportSummaryText;
	DBAdapter db;
	public static SimpleCursorAdapter mCursorAdapter;

	final String cashTypeWithdraw = "Withdraw";
	final String cashTypeDeposit = "Deposit";
	final String cashTypeSold = "Sold";
	final String cashTypeBought = "Bought";

	public Cash() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_report, container,
				false);

		context = rootView.getContext();

		spinnerAccountName = (Spinner) rootView
				.findViewById(R.id.reportAccountName);

		reportSummaryText = (TextView) rootView
				.findViewById(R.id.reportSummaryText);

		setAvailableCash(reportSummaryText, 0);

		amountList = (ListView) rootView.findViewById(R.id.reportList);
		registerForContextMenu(amountList);

		spinnerAccountName
				.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent,
							View selectedItemView, int position, long id) {
						loadReport();
					}

					@Override
					public void onNothingSelected(AdapterView<?> parentView) {
						// your code here
					}
				});

		return rootView;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		menu.setHeaderTitle("Options");
		menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
		inflater.inflate(R.menu.report, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		if (getUserVisibleHint() == false) {
			return false; // Pass the event to the next fragment
		}

		// Get extra info about list item that was long-pressed
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
				.getMenuInfo();

		View view = menuInfo.targetView;
		TextView tv = (TextView) view.findViewById(R.id.item_cash_type);
		String type = tv.getText().toString();

		switch (item.getItemId()) {
		case R.id.action_report_delete:
			try {
				if (type == cashTypeWithdraw || type == cashTypeDeposit) {
					deleteAmount(menuInfo.id);
				} else {
					LogUtil.toastLong(context,
							"The amount can not be deleted, delete the transaction instead");
				}
			} catch (Exception e) {
				LogUtil.toastShort(getActivity().getApplicationContext(),
						"Exception while deleting prayer: " + e.toString());
			}
			break;
		case R.id.action_report_edit:
			try {
				if (type == cashTypeWithdraw || type == cashTypeDeposit) {
					Intent intent = new Intent(getActivity(), Amount.class);
					intent.putExtra(Constants.TRANSACTION_ID, menuInfo.id);
					startActivity(intent);
				} else {
					LogUtil.toastLong(context,
							"The amount can not be edited, edit the transaction instead");
				}
			} catch (Exception e) {
				LogUtil.toastShort(getActivity().getApplicationContext(),
						"Exception while deleting prayer: " + e.toString());
			}
			break;
		}

		return true;
	}

	public void deleteAmount(final long id) {
		AlertDialog.Builder alert = new Builder(context);
		alert.setTitle("Confirm");
		alert.setIcon(android.R.drawable.ic_delete);
		alert.setMessage("This operation can not be undone, are you sure?");
		alert.setPositiveButton("Yes!  Delete",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if (db.amount.delete(id)) {
							NAVUtil.updateUnits(getActivity(),
									SharesUtil.getTotalValue(db));
							LogUtil.toastShort(getActivity(), "Amount Deleted");
							mCursorAdapter.runQueryOnBackgroundThread("");
							mCursorAdapter.notifyDataSetChanged();
							onStart();
						} else {
							LogUtil.toastShort(getActivity(),
									"Amount could not be deleted");
						}
					}
				});

		alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});
		alert.show();
	}

	@Override
	public void onStart() {
		super.onStart();
		db = new DBAdapter(getActivity().getApplicationContext());
		db.open();
		spinnerAccountName.setAdapter(DropDownListHelper.getAccountAdapter(
				context, db));
		loadReport();
	}

	@Override
	public void onStop() {
		try {
			db.close();
		} catch (Exception ignoreMe) {
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		super.onStop();
	}

	public void loadReport() {
		try {
			if (spinnerAccountName.getChildCount() <= 0) {
				return;
			}
			loadCashDetails();
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.toast(context,
					"Exception occurred while genearting report", e);
		}
	}

	@SuppressWarnings("deprecation")
	private void loadCashDetails() {
		float totalAmount = 0;

		String account = spinnerAccountName.getSelectedItem().toString();
		cursor = db.amount.getAmountDetails(account);

		if (cursor != null && cursor.moveToFirst()) {
			do {
				float amount = cursor.getFloat(cursor
						.getColumnIndex(T_Amount.KEY_VALUE));
				totalAmount += amount;
			} while (cursor.moveToNext());

		}
		setAvailableCash(reportSummaryText, totalAmount);

		String[] dbColNames = new String[] { T_Amount.KEY_DATE,
				T_Amount.KEY_TYPE, T_Amount.KEY_VALUE };

		int[] toViewIDs = new int[] { R.id.item_cash_date, R.id.item_cash_type,
				R.id.item_cash_amount };

		mCursorAdapter = new SimpleCursorAdapter(context,
				R.layout.layout_cash_report, cursor, dbColNames, toViewIDs);

		mCursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {

			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex) {

				long date = cursor.getLong(cursor
						.getColumnIndex(T_Amount.KEY_DATE));

				if (cursor.getColumnName(columnIndex).equalsIgnoreCase(
						T_Amount.KEY_DATE)) {

					((TextView) view).setText(DateUtil.formatDate(date));
					return true;
				} else if (cursor.getColumnName(columnIndex).equalsIgnoreCase(
						T_Amount.KEY_TYPE)) {
					int type = cursor.getInt(cursor
							.getColumnIndex(T_Amount.KEY_TYPE));
					String value = String.valueOf(type);
					switch (AmountType.values()[type]) {
					case WITHDRAW:
						value = cashTypeWithdraw;
						break;
					case DEPOSIT:
						value = cashTypeDeposit;
						break;
					case FROM_SHARES:
						value = cashTypeSold;
						break;
					case TO_SHARES:
						value = cashTypeBought;
						break;
					}
					((TextView) view).setText(value);
					return true;
				} else if (cursor.getColumnName(columnIndex).equalsIgnoreCase(
						T_Amount.KEY_VALUE)) {
					float amount = cursor.getLong(cursor
							.getColumnIndex(T_Amount.KEY_VALUE));
					if (amount < 0) {
						((TextView) view).setTextColor(getResources().getColor(
								R.color.darkred));
					} else {
						((TextView) view).setTextColor(getResources().getColor(
								R.color.darkgreen));
					}
					// since amount text is not modified, return only false
					return false;
				}

				return false;
			}
		});

		mCursorAdapter.runQueryOnBackgroundThread("");
		mCursorAdapter.notifyDataSetChanged();
		amountList.setAdapter(mCursorAdapter);
	}

	private static void setAvailableCash(TextView reportSummaryText,
			double amount) {

		reportSummaryText.setText("Amount in Cash: "
				+ StringUtil.round(amount, 2));
	}
}
