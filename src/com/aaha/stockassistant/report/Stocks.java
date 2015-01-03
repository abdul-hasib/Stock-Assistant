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
import com.aaha.db.DBAdapter.T_Transaction;
import com.aaha.stockassistant.R;
import com.aaha.stockassistant.shares.EditSubTransaction;
import com.aaha.stockassistant.shares.MainTransaction;
import com.aaha.stockassistant.util.Constants;
import com.aaha.stockassistant.util.Constants.TransactionType;
import com.aaha.stockassistant.util.DateUtil;
import com.aaha.stockassistant.util.DropDownListHelper;
import com.aaha.stockassistant.util.LogUtil;
import com.aaha.stockassistant.util.NAVUtil;
import com.aaha.stockassistant.util.SharesUtil;
import com.aaha.stockassistant.util.StringUtil;

public class Stocks extends Fragment {
	Spinner accountSpinner;
	ListView stockList;
	Context ctx;
	Cursor cursor;
	TextView reportSummaryText;
	DBAdapter db;
	SimpleCursorAdapter mCursorAdapter;

	public Stocks() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_report, container,
				false);

		ctx = rootView.getContext();

		accountSpinner = (Spinner) rootView
				.findViewById(R.id.reportAccountName);

		reportSummaryText = (TextView) rootView
				.findViewById(R.id.reportSummaryText);

		setAvailableStock(reportSummaryText, 0);

		stockList = (ListView) rootView.findViewById(R.id.reportList);
		registerForContextMenu(stockList);

		accountSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
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
	public void onStart() {
		super.onStart();
		db = new DBAdapter(getActivity().getApplicationContext());
		db.open();
		accountSpinner
				.setAdapter(DropDownListHelper.getAccountAdapter(ctx, db));
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
		TextView tv = (TextView) view.findViewById(R.id.item_transaction_type);
		TransactionType transactionType = TransactionType.valueOf(tv.getText()
				.toString());
		TextView tvSharesVolume = (TextView) view
				.findViewById(R.id.item_stock_volume);
		int numberOfShares = Integer.valueOf(tvSharesVolume.getText()
				.toString());

		switch (item.getItemId()) {
		case R.id.action_report_delete:
			try {
				deleteTransaction(menuInfo.id, transactionType);
			} catch (Exception e) {
				LogUtil.toastShort(getActivity().getApplicationContext(),
						"Exception while deleting prayer: " + e.toString());
			}
			break;
		case R.id.action_report_edit:
			try {
				Intent intent = new Intent();
				intent.putExtra(Constants.TRANSACTION_ID, menuInfo.id);
				intent.putExtra(Constants.TRANSACTION_TYPE, transactionType);

				switch (transactionType) {
				case BUY:
				case SHORT:

					if (isTransactionInitiated(menuInfo.id)) {
						LogUtil.toastLong(getActivity(),
								"This transaction can not be edited");
						return true;
					}
					intent.setClass(getActivity(), MainTransaction.class);
					break;

				case BUY_BACK:
				case SELL:
					intent.setClass(getActivity(), EditSubTransaction.class);
					intent.putExtra(
							Constants.MAXIMUM_ALLOWED_SHARES,
							getMaximumSharesAllowed(menuInfo.id, numberOfShares));
					break;
				}

				startActivity(intent);

			} catch (Exception e) {
				LogUtil.toastShort(getActivity().getApplicationContext(),
						"Exception while editing prayer: " + e.toString());
			}
			break;
		}

		return true;
	}

	private boolean isTransactionInitiated(long id) {
		Cursor c = db.details.getByTransactionId(id);

		if (c != null && c.moveToFirst()) {
			c.close();
			return true;
		}

		return false;
	}

	private int getMaximumSharesAllowed(long id, int currentTransactionShares) {
		long transactionId = db.details.getTransactionId(id);
		int initialTransactionShares = 0;

		Cursor c = db.transaction.get(transactionId);
		if (c != null && c.moveToFirst()) {
			initialTransactionShares = c.getInt(c
					.getColumnIndex(T_Transaction.KEY_VOLUME));
			c.close();
		}

		int transactionCompletedShares = db.details.getVolume(transactionId);

		return (initialTransactionShares - transactionCompletedShares + currentTransactionShares);
	}

	public void deleteTransaction(final long id,
			final TransactionType transactionType) {
		AlertDialog.Builder alert = new Builder(ctx);
		alert.setTitle("Confirm");
		alert.setIcon(android.R.drawable.ic_delete);
		alert.setMessage("This operation can not be undone, are you sure?");
		alert.setPositiveButton("Yes!  Delete",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						boolean deleteStatus = false;
						switch (transactionType) {
						case BUY:
						case SHORT:
							deleteStatus = db.transaction.delete(id);
							break;
						case SELL:
						case BUY_BACK:
							deleteStatus = db.details.delete(id);
							break;
						}
						if (deleteStatus) {
							NAVUtil.updateNAV(getActivity(),
									SharesUtil.getTotalValue(db));
							LogUtil.toastShort(getActivity(),
									"Transaction Deleted");
							mCursorAdapter.runQueryOnBackgroundThread("");
							mCursorAdapter.notifyDataSetChanged();
							onStart();
						} else {
							LogUtil.toastShort(getActivity(),
									"Transaction could not be deleted");
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

	private void loadReport() {
		try {
			if (accountSpinner.getChildCount() <= 0) {
				return;
			}
			loadStockDetails();
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.toast(ctx, "Exception occurred while genearting report", e);
		}

	}

	@SuppressWarnings("deprecation")
	private void loadStockDetails() {

		String account = accountSpinner.getSelectedItem().toString();
		cursor = db.transaction.getStockReport(account);

		setAvailableStock(reportSummaryText,
				db.transaction.getStocksValue(account));

		String[] databaseColumnNames = new String[] { T_Transaction.KEY_DATE,
				T_Transaction.KEY_STOCK_ID, T_Transaction.KEY_VOLUME,
				T_Transaction.KEY_PRICE, T_Transaction.KEY_TYPE };

		int[] toViewIDs = new int[] { R.id.item_stock_date,
				R.id.item_stock_name, R.id.item_stock_volume,
				R.id.item_stock_price, R.id.item_transaction_type };

		mCursorAdapter = new SimpleCursorAdapter(ctx,
				R.layout.layout_stock_report, cursor, databaseColumnNames,
				toViewIDs);

		mCursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {

			@Override
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex) {

				if (cursor.getColumnName(columnIndex).equalsIgnoreCase(
						T_Transaction.KEY_DATE)) {
					long date = cursor.getLong(cursor
							.getColumnIndex(T_Transaction.KEY_DATE));
					((TextView) view).setText(DateUtil.formatDate(date));
					return true;
				} else if (cursor.getColumnName(columnIndex).equalsIgnoreCase(
						T_Transaction.KEY_STOCK_ID)) {
					long tempId = cursor.getLong(cursor
							.getColumnIndex(T_Transaction.KEY_STOCK_ID));
					((TextView) view).setText(db.stock.getName(tempId));
					return true;
				} else if (cursor.getColumnName(columnIndex).equalsIgnoreCase(
						T_Transaction.KEY_TYPE)) {
					int type = cursor.getInt(cursor
							.getColumnIndex(T_Transaction.KEY_TYPE));
					((TextView) view).setText(TransactionType.values()[type]
							.name());
					return true;
				}
				return false;
			}
		});

		stockList.setAdapter(mCursorAdapter);
	}

	public static void setAvailableStock(TextView reportSummaryText,
			double amount) {
		reportSummaryText.setText("Amount in Stock: "
				+ StringUtil.round(amount, 2));
	}
}