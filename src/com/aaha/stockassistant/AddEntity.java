package com.aaha.stockassistant;

import java.util.Locale;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

import com.aaha.db.DBAdapter;
import com.aaha.stockassistant.util.Constants;
import com.aaha.stockassistant.util.Util;

public class AddEntity extends Activity {

	EditText etName;
	DBAdapter db;
	private String entityName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_entity);
		// Show the Up button in the action bar.
		setupActionBar();

		etName = (EditText) findViewById(R.id.etNewName);

		db = new DBAdapter(getApplicationContext());
		db.open();

		Bundle extras = getIntent().getExtras();
		entityName = extras.getString(Constants.TYPE);
		etName.setHint("Enter " + entityName.toLowerCase(Locale.ENGLISH)
				+ " name");
	}

	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();
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
		getMenuInflater().inflate(R.menu.add_account, menu);
		return true;
	}

	public void addEntity(View view) {

		String name = etName.getText().toString().trim();

		if (name.isEmpty()) {
			Util.Toast(getApplicationContext(), "Please enter " + entityName
					+ " name!");
			return;
		}

		if (db.entity.isExist(name, entityName)) {
			Util.Toast(getApplicationContext(), entityName + " name '" + name
					+ "' already exist");
			return;
		}

		if (db.entity.add(name, entityName) > -1) {
			Util.Toast(getApplicationContext(), entityName + " name '" + name
					+ "' added");
			etName.setText("");
		} else {
			Util.Toast(getApplicationContext(), "Failed: Invaid " + entityName
					+ " name");
		}
	}

}
