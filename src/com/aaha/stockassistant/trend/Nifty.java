package com.aaha.stockassistant.trend;

import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.aaha.db.DBAdapter;
import com.aaha.db.DBAdapter.T_Nifty;
import com.aaha.stockassistant.R;
import com.aaha.stockassistant.util.DateUtil;
import com.aaha.stockassistant.util.LogUtil;
import com.aaha.stockassistant.util.NAVUtil;
import com.aaha.stockassistant.util.SharesUtil;

public class Nifty extends Activity {
	Button btnSelectDate, btnAddNifty;
	EditText etNiftyValue;
	DBAdapter db;
	ListView niftyList;
	public int year, month, day, hour, minute;
	static final int DATE_DIALOG_ID = 1;
	long niftyToBeEdited = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_nifty);
		// Show the Up button in the action bar.
		setupActionBar();

		db = new DBAdapter(getApplicationContext());
		db.open();

		btnSelectDate = (Button) findViewById(R.id.btnSelectDate);
		btnAddNifty = (Button) findViewById(R.id.btnAddNifty);
		etNiftyValue = (EditText) findViewById(R.id.etNiftyValue);
		niftyList = (ListView) findViewById(R.id.niftyList);

		setDate(new Date());
		loadNiftyDetails();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.nifty, menu);
		return true;
	}

	@Override
	protected void onResume() {
		try {
			loadNiftyDetails();
		} catch (Exception e) {
			LogUtil.toast(getApplicationContext(),
					"Error occurred while loading accounts", e);
		}
		super.onResume();
	}

	public void addNiftyValue(View view) {
		String date = btnSelectDate.getText().toString().trim();
		String niftyValue = etNiftyValue.getText().toString().trim();

		if (niftyValue.isEmpty()) {
			LogUtil.toastShort(getApplicationContext(), "Please enter NIFTY value!");
			return;
		}

		if (niftyToBeEdited != -1) {
			updateNiftyDetailsInDB(niftyToBeEdited, date, niftyValue);
		} else {
			addNiftyDetailsToDB(date, Float.valueOf(niftyValue));
		}
	}

	private void addNiftyDetailsToDB(String date, float value) {
		if (db.nifty.isExist(date)) {
			LogUtil.toastShort(getApplicationContext(), "NIFTY value for date '"
					+ date + "' already exist");
			return;
		}

		int currentValue = (int) NAVUtil.getNAV(getApplicationContext());

		if (db.nifty.add(date, value, currentValue) > -1) {
			LogUtil.toastShort(getApplicationContext(), "NIFTY value '" + value
					+ "' added");
			resetNiftyForm();
		} else {
			LogUtil.toastShort(getApplicationContext(), "Failed to add NIFTY value");
		}
	}

	private void updateNiftyDetailsInDB(long id, String date, String value) {
		if (!db.nifty.isExist(id)) {
			LogUtil.toastShort(getApplicationContext(), "NIFTY value '" + value
					+ "' does not exist");
			return;
		}

		if (db.nifty.update(id, date, Float.valueOf(value))) {
			LogUtil.toastShort(getApplicationContext(), "NIFTY details updated");
			resetNiftyForm();
		} else {
			LogUtil.toastShort(getApplicationContext(),
					"Failed to update NIFTY details");
		}
	}

	private void resetNiftyForm() {
		niftyToBeEdited = -1;
		setDate(new Date());
		btnAddNifty.setText(getResources().getString(R.string.Add));
		etNiftyValue.setText("");
		loadNiftyDetails();
	}

	public void loadNiftyDetails() {
		CustomCursorAdapter adapter = new CustomCursorAdapter(this,
				R.layout.row_nifty, db.nifty.get(), new String[] {
						T_Nifty.KEY_DATE, T_Nifty.KEY_NIFTY_VALUE,
						T_Nifty.KEY_STOCK_VALUE }, new int[] {
						R.id.item_nifty_date, R.id.item_nifty_value,
						R.id.item_total_value });

		adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {

			@Override
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex) {
				if (cursor.getColumnName(columnIndex).equalsIgnoreCase(
						T_Nifty.KEY_DATE)) {

					long date = cursor.getLong(cursor
							.getColumnIndex(T_Nifty.KEY_DATE));
					((TextView) view).setText(DateUtil.formatDate(date));
					return true;
				}

				return false;
			}

		});

		niftyList.setAdapter(adapter);
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
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
		case R.id.action_nav_reset:
			AlertDialog.Builder alert = new Builder(this);
			alert.setTitle("Reset");
			alert.setMessage("Enter new NAV value");

			final EditText input = new EditText(this);
			input.setSingleLine();
			input.setInputType(InputType.TYPE_CLASS_NUMBER);
			alert.setView(input);

			alert.setPositiveButton("Save",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							resetNAV(input.getText().toString());
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
		return super.onOptionsItemSelected(item);
	}

	private void resetNAV(String value) {
		NAVUtil.resetNAV(getApplicationContext(), Float.valueOf(value),
				SharesUtil.getTotalValue(db));
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

	@Override
	protected void onDestroy() {
		if (db != null) {
			db.close();
		}

		super.onDestroy();
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

	public void deleteNifty(long id) {

		if (db.nifty.delete(id)) {
			LogUtil.toastShort(getApplicationContext(), "NIFTY value deleted!");
		} else {
			LogUtil.toastShort(getApplicationContext(), "Delete failed: " + id);
		}
		resetNiftyForm();
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

			if (position >= 0) {
				int colorPos = position % colors.length;
				view.setBackgroundColor(colors[colorPos]);
			}

			final ImageView editName = (ImageView) view
					.findViewById(R.id.rename);
			final ImageView deleteName = (ImageView) view
					.findViewById(R.id.delete);

			editName.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					niftyToBeEdited = id;
					Cursor c = db.nifty.get(id);
					if (c != null && c.moveToFirst()) {
						String date = DateUtil.formatDate(c.getLong(c
								.getColumnIndex(T_Nifty.KEY_DATE)));
						btnSelectDate.setText(date);
						etNiftyValue.setText(String.valueOf(c.getInt(c
								.getColumnIndex(T_Nifty.KEY_NIFTY_VALUE))));
						btnAddNifty.setText("Update");
					} else {
						LogUtil.toastShort(context,
								"Error occured while editing nifty value");
					}
				}
			});

			deleteName.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					AlertDialog.Builder alert = new Builder(context);
					alert.setTitle("Confirm");
					alert.setMessage("Are you sure you want to delete?");
					alert.setPositiveButton("Delete",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									deleteNifty(id);
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
