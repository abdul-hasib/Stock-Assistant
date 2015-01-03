package com.aaha.stockassistant.manage;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.aaha.db.DBAdapter;
import com.aaha.db.DBAdapter.T_Account;
import com.aaha.stockassistant.R;
import com.aaha.stockassistant.util.LogUtil;
import com.aaha.stockassistant.util.NAVUtil;
import com.aaha.stockassistant.util.StringUtil;

public class Account extends Activity {

	EditText etName, etBrokerage, etIntradayBrokerage;
	ListView entityList;
	DBAdapter db;
	Button addAccount;
	long accountToBeEdited = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_account);
		// Show the Up button in the action bar.
		setupActionBar();

		etName = (EditText) findViewById(R.id.etNewName);
		etBrokerage = (EditText) findViewById(R.id.etBrokerage);
		etIntradayBrokerage = (EditText) findViewById(R.id.etIntradayBrokerage);
		entityList = (ListView) findViewById(R.id.entityList);
		addAccount = (Button) findViewById(R.id.addAccount);

		db = new DBAdapter(getApplicationContext());
		db.open();
	}

	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		try {
			loadEntityNames();
		} catch (Exception e) {
			LogUtil.toast(getApplicationContext(),
					"Error occurred while loading accounts", e);
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

	public void addAccount(View view) {

		String name = etName.getText().toString().trim();
		String brokerage = etBrokerage.getText().toString().trim();
		String intradayBrokerage = etIntradayBrokerage.getText().toString()
				.trim();

		if (name.isEmpty()) {
			LogUtil.toastShort(getApplicationContext(), "Please enter account name!");
			return;
		}

		if (brokerage.isEmpty() || intradayBrokerage.isEmpty()) {
			LogUtil.toastShort(getApplicationContext(), "Please enter brokerage!");
			return;
		}

		if (accountToBeEdited != -1) {
			updateAccountDetails(accountToBeEdited, name, brokerage,
					intradayBrokerage);
		} else {
			addAccountDetails(name, brokerage, intradayBrokerage);
		}

	}

	private void addAccountDetails(String name, String brokerage,
			String intradayBrokerage) {
		if (db.account.isExist(name)) {
			LogUtil.toastShort(getApplicationContext(), "Account name '" + name
					+ "' already exist");
			return;
		}

		if (db.account.add(name, Float.valueOf(brokerage),
				Float.valueOf(intradayBrokerage)) > -1) {
			LogUtil.toastShort(getApplicationContext(), "Account name '" + name
					+ "' added");
			resetAccountForm();
		} else {
			LogUtil.toastShort(getApplicationContext(), "Failed to add account name");
		}
	}

	private void resetAccountForm() {
		accountToBeEdited = -1;
		etName.setText("");
		etBrokerage.setText("");
		etIntradayBrokerage.setText("");
		addAccount.setText(getResources().getString(R.string.Add));
		loadEntityNames();
	}

	private void updateAccountDetails(long id, String name, String brokerage,
			String intradayBrokerage) {
		if (!db.account.isExist(id)) {
			LogUtil.toastShort(getApplicationContext(), "Account name '" + name
					+ "' does not exist");
			return;
		}

		if (db.account.update(id, name, Float.valueOf(brokerage),
				Float.valueOf(intradayBrokerage))) {
			LogUtil.toastShort(getApplicationContext(), "Account details updated");
			resetAccountForm();
		} else {
			LogUtil.toastShort(getApplicationContext(), "Failed to update account");
		}
	}

	public void loadEntityNames() {
		CustomCursorAdapter adapter = new CustomCursorAdapter(this,
				R.layout.row_account, db.account.get(), new String[] {
						T_Account.KEY_NAME, T_Account.KEY_BROKERAGE,
						T_Account.KEY_INTRADAY_BROKERAGE }, new int[] {
						R.id.item_name, R.id.item_brokerage,
						R.id.item_intraday_brokerage });
		entityList.setAdapter(adapter);
	}

	public void update(long id, String name, float brokerage,
			float intradayBrokerage) {

		if (name.trim().isEmpty()) {
			LogUtil.toastShort(getApplicationContext(), "Name can not be blank");
			return;
		}
		if (db.account.update(id, name, brokerage, intradayBrokerage)) {
			LogUtil.toastShort(getApplicationContext(), "Account updated!");
			loadEntityNames();
		} else {
			LogUtil.toastShort(getApplicationContext(), "Update failed: " + id);
		}
	}

	public void deleteAccount(long id) {

		if (db.account.delete(id)) {
			NAVUtil.resetNAV(getApplicationContext(), 0, 0);
			LogUtil.toastShort(getApplicationContext(), "Account deleted!");
		} else {
			LogUtil.toastShort(getApplicationContext(), "Delete failed: " + id);
		}

		resetAccountForm();
	}

	class CustomCursorAdapter extends SimpleCursorAdapter {
		Context context;
		Activity activity;
		ImageView editName;
		ImageView deleteName;

		private int[] colors = new int[] { Color.parseColor("#D2E4FC"),
				Color.parseColor("#F0F0F0") };

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

			view.setBackgroundColor(Color.WHITE);

			int colorPos = position % colors.length;
			view.setBackgroundColor(colors[colorPos]);

			final ImageView editName = (ImageView) view
					.findViewById(R.id.rename);
			final ImageView deleteName = (ImageView) view
					.findViewById(R.id.delete);

			editName.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					accountToBeEdited = id;
					Cursor c = db.account.get(id);
					if (c != null && c.moveToFirst()) {
						etName.setText(c.getString(c
								.getColumnIndex(T_Account.KEY_NAME)));
						etBrokerage.setText(StringUtil.round(c.getFloat(c
								.getColumnIndex(T_Account.KEY_BROKERAGE)), 4));
						etIntradayBrokerage.setText(StringUtil.round(
								c.getFloat(c
										.getColumnIndex(T_Account.KEY_INTRADAY_BROKERAGE)),
								4));
						addAccount.setText("Update");
					} else {
						LogUtil.toastShort(context,
								"Error occured while editing account");
					}
				}
			});

			deleteName.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					AlertDialog.Builder alert = new Builder(context);
					alert.setTitle("Are you sure?");
					alert.setIcon(android.R.drawable.ic_delete);
					alert.setMessage("This deletes all the transactions related to this"
							+ " account and also resets NAV");
					alert.setPositiveButton("Yes! Delete",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									deleteAccount(id);
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
				}
			});
			return view;
		}
	}
}
