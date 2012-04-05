package com.android.BluetoothManager.UI;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.BluetoothManager.Application.BluetoothManagerApplication;

public class DeviceListActivity extends BaseActivity implements OnItemClickListener {
	private static final String TAG = "DeviceListActivity";
	
	ListView lv;

	ArrayAdapter<String> listOfDevices;

	HashMap<String, String> btPaired;
	
	HashMap<String, String> btFound;

	ProgressDialog progDialog;
	
	public static String DEVICE_ADDRESS = "address";
	public static String DEVICE_NAME = "name";
	public static final int Scanning_Dialog=0;
	boolean showing_ScanningDialog=false;
	
	BroadcastReceiver receiver= new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(getResources().getString(R.string.SCAN_COMPLETE)))
			{
				Log.d(TAG,"Received Scan Complete Intent");
				dismissDialog(Scanning_Dialog);
				pollDevices();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device_list);
		
		registerReceiver(receiver, new IntentFilter(getResources().getString(R.string.SCAN_COMPLETE)));
				
		UI.bluetooth_manager.connection_manager.startDiscovery();
		UI.is_UI_searching=true;
		
		showDialog(Scanning_Dialog);
		showing_ScanningDialog=true;
		
		bluetooth_manager = (BluetoothManagerApplication) getApplication();
		Log.d(TAG,"Calling startDiscovery from DeviceListActivity");
		UI.bluetooth_manager.connection_manager.is_req_from_gui = true;
				
		setResult(RESULT_CANCELED);
	}
	
	// Will receive the devices in btPaired and btFound and add them to UI
	public void pollDevices()
	{
		lv = (ListView) findViewById(R.id.paired_devices);
		
		listOfDevices = new ArrayAdapter<String>(this, R.layout.device_name);
		
		lv.setAdapter(listOfDevices);
		lv.setOnItemClickListener(this);
		btFound=UI.bluetooth_manager.connection_manager.getFoundDevices();
		btPaired=UI.bluetooth_manager.connection_manager.getPairedDevices();
		
		
		Iterator<Map.Entry<String, String>> devices = btFound.entrySet().iterator();
		while (devices.hasNext()) {
			Map.Entry<String, String> device = (Map.Entry<String, String>) devices
					.next();
			listOfDevices.add(device.getValue() + "\n" + device.getKey());
		}
		btPaired = UI.bluetooth_manager.connection_manager.getPairedDevices();
		devices = btPaired.entrySet().iterator();
		while (devices.hasNext()) {
			Map.Entry<String, String> device = (Map.Entry<String, String>) devices
					.next();
			listOfDevices.add(device.getValue() + "\n" + device.getKey());
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
		
		// Get the device MAC address, which is the last 17 chars in the View
		String info = ((TextView) v).getText().toString();
		String address = info.substring(info.length() - 17);
		String name = info.substring(0,info.length() - 17);
		Intent i = new Intent();
		i.putExtra(DEVICE_ADDRESS, address);
		i.putExtra(DEVICE_NAME, name);
		setResult(RESULT_OK, i);
		finish();
	}

	@Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case Scanning_Dialog: {
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setTitle("Searching");
                dialog.setMessage("Please wait while discovering bluetooth devices...");
                dialog.setIndeterminate(true);
                dialog.setCancelable(false);
                return dialog;
            }
        }
        return null;
    }

}