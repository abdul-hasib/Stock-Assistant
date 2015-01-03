package com.aaha.stockassistant.util;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;

public class Util {

	static String inputValue = "";

	public static String getUserInput(Context context) {
		AlertDialog.Builder alert = new Builder(context);
		alert.setTitle("Title");
		alert.setMessage("Message");

		final EditText input = new EditText(context);
		input.setSingleLine();
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		alert.setView(input);

		alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				inputValue = input.getText().toString();
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});
		alert.show();
		return inputValue;
	}
}
