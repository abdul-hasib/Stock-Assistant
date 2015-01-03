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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.aaha.db.DBAdapter;
import com.aaha.db.DBAdapter.T_Stock;
import com.aaha.stockassistant.R;
import com.aaha.stockassistant.util.LogUtil;
import com.aaha.stockassistant.util.NAVUtil;
import com.aaha.stockassistant.util.SharesUtil;

public class Stock extends Activity {

	EditText etName;
	ListView entityList;

	DBAdapter db;
	private String entityType;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_stock);
		// Show the Up button in the action bar.
		setupActionBar();

		etName = (EditText) findViewById(R.id.etNewName);
		entityList = (ListView) findViewById(R.id.entityList);
		// registerForContextMenu(entityList);

		db = new DBAdapter(getApplicationContext());
		db.open();

		etName.setHint("Enter Stock name");
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
			LogUtil.toastShort(getApplicationContext(), "Error occurred loading "
					+ entityType + "s");
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

	public void addEntity(View view) {

		String name = etName.getText().toString().trim();

		if (name.isEmpty()) {
			LogUtil.toastShort(getApplicationContext(), "Please enter " + entityType
					+ " name!");
			return;
		}

		if (db.stock.isExist(name)) {
			LogUtil.toastShort(getApplicationContext(), "Stock name '" + name
					+ "' already exist");
			return;
		}

		if (db.stock.add(name) > -1) {
			LogUtil.toastShort(getApplicationContext(), "Stock name '" + name
					+ "' added");
			etName.setText("");
			loadEntityNames();

		} else {
			LogUtil.toastShort(getApplicationContext(), "Failed: Invaid Stock name");
		}
	}

	public void loadEntityNames() {
		CustomCursorAdapter adapter = new CustomCursorAdapter(this,
				R.layout.row_entity, db.stock.get(),
				new String[] { T_Stock.KEY_NAME }, new int[] { R.id.item_name });
		entityList.setAdapter(adapter);
	}

	public void update(long id, String name) {

		if (name.trim().isEmpty()) {
			LogUtil.toastShort(getApplicationContext(), "Name can not be blank");
			return;
		}
		if (db.stock.update(id, name)) {
			LogUtil.toastShort(getApplicationContext(), "Done!");
			loadEntityNames();
		} else {
			LogUtil.toastShort(getApplicationContext(), "Failed to rename!");
		}
	}

	public void deleteEntity(long id) {

		if (db.stock.delete(id)) {
			NAVUtil.updateNAV(getApplicationContext(),
					SharesUtil.getTotalValue(db));
			LogUtil.toastShort(getApplicationContext(), "Stock deleted!");
		} else {
			LogUtil.toastShort(getApplicationContext(), "Delete failed: " + id);
		}

		onResume();
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

			int colorPos = position % colors.length;
			view.setBackgroundColor(colors[colorPos]);

			final ImageView editName = (ImageView) view
					.findViewById(R.id.rename);
			final ImageView deleteName = (ImageView) view
					.findViewById(R.id.delete);

			editName.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {

					AlertDialog.Builder alert = new Builder(context);
					alert.setTitle("Update");
					alert.setMessage("Enter the new name");

					// Set an EditText view to get user input
					final EditText input = new EditText(context);
					input.setSingleLine();
					input.setHint(etName.getText().toString());
					alert.setView(input);

					alert.setPositiveButton("Save",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									update(id, input.getText().toString());
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

			deleteName.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					AlertDialog.Builder alert = new Builder(context);
					alert.setTitle("Are you sure?");
					alert.setIcon(android.R.drawable.ic_delete);
					alert.setMessage("This deletes all the transactions related to this"
							+ " stock");
					alert.setPositiveButton("Yes!, Delete",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									deleteEntity(id);
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
