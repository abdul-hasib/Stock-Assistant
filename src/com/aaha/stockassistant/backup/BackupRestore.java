package com.aaha.stockassistant.backup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.aaha.db.DBAdapter;
import com.aaha.db.DBAdapter.T_Account;
import com.aaha.db.DBAdapter.T_Amount;
import com.aaha.db.DBAdapter.T_AmountHistory;
import com.aaha.db.DBAdapter.T_Details;
import com.aaha.db.DBAdapter.T_Stock;
import com.aaha.db.DBAdapter.T_Transaction;
import com.aaha.db.DBAdapter.T_TransactionHistory;
import com.aaha.stockassistant.R;
import com.aaha.stockassistant.util.Constants.AmountType;
import com.aaha.stockassistant.util.Constants.TransactionType;
import com.aaha.stockassistant.util.LogUtil;

public class BackupRestore extends Activity implements OnClickListener {

	Button btnExport, btnImport;
	TextView backupStatus, importStatus, importField, exportField;
	DBAdapter db;
	Cursor mCursor = null;
	FileWriter writer;
	String username;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_backup_restore);

		db = new DBAdapter(getApplicationContext());
		db.open();

		btnExport = (Button) findViewById(R.id.btnExport);
		btnImport = (Button) findViewById(R.id.btnImport);

		backupStatus = (TextView) findViewById(R.id.txtbackupStatus);
		importStatus = (TextView) findViewById(R.id.txtImportStatus);

		backupStatus.setText("");
		importStatus.setText("");

		importField = (TextView) findViewById(R.id.importField);
		exportField = (TextView) findViewById(R.id.exportField);

		btnExport.setOnClickListener(this);
		btnImport.setOnClickListener(this);
	}

	private int importData(String filename) {
		File fXmlFile = new File(Environment.getExternalStorageDirectory(),
				filename);
		try {

			db.resetAllTables();

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			doc.getDocumentElement().normalize();

			NodeList nTableNodes = doc.getElementsByTagName("table");

			Node nAccountTableNode = getChildNode(nTableNodes,
					T_Account.TABLE_NAME);
			importDataIntoTables(nAccountTableNode);

			Node nStockTableNode = getChildNode(nTableNodes, T_Stock.TABLE_NAME);
			importDataIntoTables(nStockTableNode);

			Node nAmountTableNode = getChildNode(nTableNodes,
					T_Amount.TABLE_NAME);
			importDataIntoTables(nAmountTableNode);

			Node nTransactionTableNode = getChildNode(nTableNodes,
					T_Transaction.TABLE_NAME);
			Node nDetailsTableNode = getChildNode(nTableNodes,
					T_Details.TABLE_NAME);
			importTransactions(nTransactionTableNode, nDetailsTableNode);

			Node nTransactionHistoryTableNode = getChildNode(nTableNodes,
					T_TransactionHistory.TABLE_NAME);
			importDataIntoTables(nTransactionHistoryTableNode);

			Node nAmountHistoryTableNode = getChildNode(nTableNodes,
					T_AmountHistory.TABLE_NAME);
			importDataIntoTables(nAmountHistoryTableNode);

			return 0;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return 1;
		} catch (Exception e) {
			e.printStackTrace();
			return 3;
		}

	}

	private void importTransactions(Node nTransactionTableNode,
			Node nDetailsTableNode) {

		NodeList nTransactionNode = nTransactionTableNode.getChildNodes();
		Element eElement = (Element) nTransactionTableNode;
		String tableName = eElement.getAttribute("name");
		LogUtil.d("Importing data from table: " + tableName);
		LogUtil.d("Number of nodes: "
				+ String.valueOf(nTransactionNode.getLength()));

		for (int row = 0; row < nTransactionNode.getLength(); row++) {
			Node nRowNode = nTransactionNode.item(row);
			Map<String, String> rowData = getRowData(nRowNode);

			LogUtil.d(rowData);

			String transactionDate = rowData.get(T_Transaction.KEY_DATE);
			String accountName = rowData.get(T_Transaction.KEY_ACCOUNT_ID);
			TransactionType type = TransactionType.values()[Integer
					.valueOf(rowData.get(T_Transaction.KEY_TYPE))];
			String stockName = rowData.get(T_Transaction.KEY_STOCK_ID);
			boolean intraday = rowData.get(T_Transaction.KEY_INTRADAY).equals(
					"1");
			int volume = Integer.valueOf(rowData.get(T_Transaction.KEY_VOLUME));
			float price = Float.valueOf(rowData.get(T_Transaction.KEY_PRICE));
			float value = Float.valueOf(rowData.get(T_Transaction.KEY_VALUE));

			long transactionId = db.transaction.add(type, transactionDate,
					accountName, stockName, intraday, volume, price, value);

			// if there are sub transactions, insert them first
			if (nDetailsTableNode != null) {

				// import all the sub transactions of the main transaction
				int oldTransactionId = Integer.valueOf(rowData
						.get(T_Transaction.KEY_ID));

				NodeList nDetailNode = nDetailsTableNode.getChildNodes();
				Element eSubElement = (Element) nDetailsTableNode;
				String subTableName = eSubElement.getAttribute("name");
				LogUtil.d("Importing data from table: " + subTableName);
				LogUtil.d("Number of nodes: "
						+ String.valueOf(nDetailNode.getLength()));

				for (int subRow = 0; subRow < nDetailNode.getLength(); subRow++) {
					Node nDetailRowNode = nDetailNode.item(subRow);
					Map<String, String> detailRowData = getRowData(nDetailRowNode);
					LogUtil.d(detailRowData);

					long id = Long.valueOf(detailRowData
							.get(T_Details.KEY_TRANSACTION_ID));

					if (oldTransactionId != id) {
						continue;
					}

					String date = detailRowData.get(T_Details.KEY_DATE);
					int detailVolume = Integer.valueOf(detailRowData
							.get(T_Details.KEY_VOLUME));
					float detailPrice = Float.valueOf(detailRowData
							.get(T_Details.KEY_PRICE));
					float detailValue = Float.valueOf(detailRowData
							.get(T_Details.KEY_VALUE));

					db.details.add(date, transactionId, detailVolume,
							detailPrice, detailValue);
				}
			}
		}

	}

	public static Node getChildNode(NodeList nTableNodes, String nodeName) {
		for (int i = 0; i < nTableNodes.getLength(); i++) {
			Element eElement = (Element) nTableNodes.item(i);
			if (eElement.getAttribute("name").equalsIgnoreCase(nodeName)) {
				return nTableNodes.item(i);
			}
		}
		return null;
	}

	private float getRowDataValue(Map<String, String> rowData, String key) {
		float value = 0;
		String temp = rowData.get(key);
		if (temp != null) {
			value = Float.valueOf(temp);
		}
		return value;
	}

	public void importDataIntoTables(Node nTableNode) {

		if (nTableNode == null) {
			return;
		}

		NodeList nRowNodes = nTableNode.getChildNodes();
		Element eElement = (Element) nTableNode;
		String tableName = eElement.getAttribute("name");
		LogUtil.d("Importing data from table: " + tableName);
		LogUtil.d("Number of nodes: " + String.valueOf(nRowNodes.getLength()));

		for (int row = 0; row < nRowNodes.getLength(); row++) {
			Node nRowNode = nRowNodes.item(row);
			Map<String, String> rowData = getRowData(nRowNode);

			LogUtil.d("rowData: " + rowData);

			if (tableName.equals(T_Account.TABLE_NAME)) {
				String name = rowData.get(T_Account.KEY_NAME);
				float brokerage = getRowDataValue(rowData,
						T_Account.KEY_BROKERAGE);
				float intradayBrokerage = getRowDataValue(rowData,
						T_Account.KEY_INTRADAY_BROKERAGE);

				db.account.add(name, brokerage, intradayBrokerage);

			} else if (tableName.equals(T_Stock.TABLE_NAME)) {
				String name = rowData.get(T_Stock.KEY_NAME);
				db.stock.add(name);

			} else if (tableName.equals(T_Amount.TABLE_NAME)) {
				String date = rowData.get(T_Amount.KEY_DATE);
				String accountName = rowData.get(T_Amount.KEY_ACCOUNT_ID);
				float amount = Float.valueOf(rowData.get(T_Amount.KEY_VALUE));
				AmountType amountType = AmountType.values()[Integer
						.valueOf(rowData.get(T_Amount.KEY_TYPE))];
				db.amount.add(date, accountName, amount, amountType);

			} else if (tableName.equals(T_TransactionHistory.TABLE_NAME)) {
				String account = rowData.get(T_TransactionHistory.KEY_ACCOUNT);
				String date = rowData.get(T_TransactionHistory.KEY_DATE);
				String stock = rowData.get(T_TransactionHistory.KEY_STOCK);
				int volume = Integer.valueOf(rowData
						.get(T_TransactionHistory.KEY_VOLUME));
				float price = Float.valueOf(rowData
						.get(T_TransactionHistory.KEY_PRICE));
				String notes = rowData.get(T_TransactionHistory.KEY_NOTES);
				db.transactionHistory.add(date, account, stock, volume, price,
						notes);
			} else if (tableName.equals(T_AmountHistory.TABLE_NAME)) {

				String date = rowData.get(T_AmountHistory.KEY_DATE);
				String account = rowData.get(T_AmountHistory.KEY_ACCOUNT);
				float amount = Float.valueOf(rowData
						.get(T_AmountHistory.KEY_AMOUNT));
				String notes = rowData.get(T_AmountHistory.KEY_NOTES);
				db.amountHistory.add(date, account, amount, notes);
			}
		}
	}

	public static Map<String, String> getRowData(Node nRowNode) {
		NodeList nColNodes = nRowNode.getChildNodes();
		Map<String, String> rowData = new HashMap<String, String>();
		LogUtil.d("Number of columns: " + nColNodes.getLength());
		for (int col = 0; col < nColNodes.getLength(); col++) {
			Node nColNode = nColNodes.item(col);
			Element eElement = (Element) nColNode;
			String fieldName = eElement.getNodeName();
			String value = eElement.getTextContent();
			rowData.put(fieldName, value);
		}
		return rowData;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			mCursor.close();
		} catch (Exception ignoreMe) {

		}

		try {
			db.close();
		} catch (Exception ignoreMe) {
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnExport:

			if (exportField.getText().toString().trim().length() == 0) {
				LogUtil.toastShort(getApplicationContext(),
						"Please enter the filename");
				return;
			}
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(
					"This might overwrite existing backup file\nAre you sure you want to continue?")
					.setTitle("Confirm")
					.setCancelable(true)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									backupApplicationData();
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog alert = builder.create();
			alert.show();
			break;
		case R.id.btnImport:

			if (importField.getText().toString().trim().length() == 0) {
				LogUtil.toastShort(getApplicationContext(),
						"Please enter the filename");
				return;
			}

			builder = new AlertDialog.Builder(this);
			builder.setMessage(
					"The existing data will be lost\n Are you sure you want to restore?")
					.setTitle("Confirm")
					.setCancelable(true)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									restoreApplicationData();
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			alert = builder.create();
			alert.show();
			break;
		}
	}

	public void backupApplicationData() {
		final String temp = exportField.getText().toString().trim();
		final ProgressDialog progress = ProgressDialog.show(BackupRestore.this,
				"Exporting", "Please wait...", true, false);
		new Thread(new Runnable() {
			public void run() {
				final String filename = temp.contains(".xml") ? temp : temp
						+ ".xml";
				BackupAssistant export = new BackupAssistant(
						getApplicationContext(), db, filename);
				final int status = export.exportData();

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						switch (status) {
						case 0:
							LogUtil.toastLong(getApplicationContext(),
									"Data exported successfully!!!");
							backupStatus
									.setText("Data has been saved to SD card: "
											+ filename);
							break;
						case 1:
							LogUtil.toastLong(getApplicationContext(),
									"Problem while reading data!!!");
							backupStatus.setText("Problem while reading data");
							break;
						case 2:
							LogUtil.toastLong(getApplicationContext(),
									"No Data available to export");
							backupStatus.setText("No Data available to export");
							break;
						case 3:
							LogUtil.toastLong(getApplicationContext(),
									"No SD card found");
							backupStatus.setText("No SD card found");
							break;
						}
					}
				});
				progress.cancel();
			}
		}).start();
	}

	public void restoreApplicationData() {
		final ProgressDialog progress = ProgressDialog.show(BackupRestore.this,
				"Restoring", "Please wait...", true, false);

		new Thread(new Runnable() {
			public void run() {
				String temp = importField.getText().toString().trim();
				final String filename = temp.contains(".xml") ? temp : temp
						+ ".xml";
				final int status = importData(filename);

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						switch (status) {
						case 0:
							LogUtil.toastShort(getApplicationContext(),
									"Data restored successfully");
							importStatus.setText("Data imported from: "
									+ filename);
							break;
						case 1:
							LogUtil.toastShort(getApplicationContext(),
									"File not found or invalid file");
							importStatus
									.setText("File not found or invalid file: "
											+ filename);
							break;
						case 2:
							LogUtil.toastShort(getApplicationContext(),
									"No data available to restore");
							importStatus
									.setText("No data available to restore from file: "
											+ filename);
							break;
						case 3:
							LogUtil.toastShort(getApplicationContext(),
									"Either backup file is corrupt or does not have any data");
							importStatus
									.setText("Either backup file is corrupt or does not have any data: "
											+ filename);
							break;
						}
					}
				});
				progress.cancel();
			}
		}).start();
	}

}