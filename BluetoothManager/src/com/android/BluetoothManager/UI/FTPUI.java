package com.android.BluetoothManager.UI;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.BluetoothManager.Application.BluetoothManagerApplication;
import com.android.BluetoothManager.Application.R;

public class FTPUI extends ListActivity {

	private enum DISPLAYMODE {
		ABSOLUTE, RELATIVE;
	}

	private static final int GET_DEVICE_TO_SEND = 1;

	private final DISPLAYMODE displayMode = DISPLAYMODE.RELATIVE;
	private List<String> directoryEntries = new ArrayList<String>();
	private File currentDirectory = new File("/");
	private final String TAG = "FileTransferUI";
	private int MAX_FILE_SIZE = 512; // in KiloBytes.
	private BluetoothManagerApplication bluetooth_manager;
	final String MSG_TYPE = "file";
	File fileToSend;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		// setContentView() gets called within the next line,
		// so we do not need it here.
		browseToRoot();
		Log.d(TAG, "Environment: " + Environment.getExternalStorageDirectory());
	}

	/**
	 * This function browses to the root-directory of the file-system.
	 */
	private void browseToRoot() {
		browseTo(new File("/sdcard/"));
	}

	/**
	 * This function browses up one level according to the field:
	 * currentDirectory
	 */
	private void upOneLevel() {
		if (this.currentDirectory.getParent() != null)
			this.browseTo(this.currentDirectory.getParentFile());
	}

	private void browseTo(final File aDirectory) {
		if (aDirectory.isDirectory()) {
			this.currentDirectory = aDirectory;
			fill(aDirectory.listFiles());
		} else {
			OnClickListener okButtonListener = new OnClickListener() {
				// @Override
				public void onClick(DialogInterface arg0, int arg1) {
					fileToSend = aDirectory;
					long length = new File(fileToSend.getPath()).length();
					Log.d(TAG,"Length of file: "+length);
					if (length > MAX_FILE_SIZE * 1000) {
						Toast.makeText(getApplicationContext(),
								"File should not be greater then 512KB.",
								Toast.LENGTH_LONG).show();
						return;
					}
					startActivityForResult(new Intent(getApplicationContext(),
							DeviceListActivity.class), GET_DEVICE_TO_SEND);
				}
			};
			OnClickListener cancelButtonListener = new OnClickListener() {
				// @Override
				public void onClick(DialogInterface arg0, int arg1) {
					// Do nothing
				}
			};

			new AlertDialog.Builder(this)
					.setTitle("Question")
					.setMessage(
							"Do you want to send this file?n"
									+ aDirectory.getName())
					.setPositiveButton("OK", okButtonListener)
					.setNegativeButton("Cancel", cancelButtonListener).show();
		}
	}
	
	

	private void fill(File[] files) {
		this.directoryEntries.clear();

		// Add the "." and the ".." == 'Up one level'
		try {
			Thread.sleep(10);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		this.directoryEntries.add(".");

		if (this.currentDirectory.getParent() != null)
			this.directoryEntries.add("..");

		switch (this.displayMode) {
		case ABSOLUTE:
			for (File file : files) {
				this.directoryEntries.add(file.getPath());
			}
			break;
		case RELATIVE: // On relative Mode, we have to add the current-path to
						// the beginning
			int currentPathStringLenght = this.currentDirectory
					.getAbsolutePath().length();
			for (File file : files) {
				this.directoryEntries.add(file.getAbsolutePath().substring(
						currentPathStringLenght));
			}
			break;
		}

		ArrayAdapter<String> directoryList = new ArrayAdapter<String>(this,
				R.layout.file_row, this.directoryEntries);

		this.setListAdapter(directoryList);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		String selectedFileString = this.directoryEntries.get(position);
		if (selectedFileString.equals(".")) {
			// Refresh
			this.browseTo(this.currentDirectory);
		} else if (selectedFileString.equals("..")) {
			this.upOneLevel();
		} else {
			File clickedFile = null;
			switch (this.displayMode) {
			case RELATIVE:
				clickedFile = new File(this.currentDirectory.getAbsolutePath()
						+ this.directoryEntries.get(position));
				break;
			case ABSOLUTE:
				clickedFile = new File(this.directoryEntries.get(position));
				break;
			}
			if (clickedFile != null)
				this.browseTo(clickedFile);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult() called !");
		if (requestCode == GET_DEVICE_TO_SEND && resultCode == RESULT_OK) {
			String device = data
					.getStringExtra(DeviceListActivity.DEVICE_ADDRESS);
			String name = data.getStringExtra(DeviceListActivity.DEVICE_NAME);
			String msg = "file," + fileToSend.getName() + ","
					+ readFileAsString(fileToSend.getPath());
			Toast.makeText(this, device, Toast.LENGTH_SHORT).show();
			UI.bluetooth_manager.sendDataToRoutingFromUI(device, name, msg, MSG_TYPE);
		}
	}

	private String readFileAsString(String filePath) {
		long fileLength = new File(filePath).length();
		Log.d(TAG, "Size of File Selected: " + fileLength);
		byte[] buffer = new byte[(int) new File(filePath).length()];
		;
		try {
			BufferedInputStream f = new BufferedInputStream(
					new FileInputStream(filePath));
			f.read(buffer);
		} catch (IOException e) {
			Log.d(TAG, e.getMessage());
		}
		return new String(buffer);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		
	}
}
