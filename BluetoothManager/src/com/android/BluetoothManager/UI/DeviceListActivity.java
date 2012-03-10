package com.android.BluetoothManager.UI;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.content.Intent;
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

	ArrayAdapter<String> bondedDevices;

	BluetoothManagerApplication bluetooth_manager;

	HashMap<String, String> btPaired;

	public static String DEVICE_ADDRESS = "address";
	public static String DEVICE_NAME = "name";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device_list);
		bluetooth_manager = (BluetoothManagerApplication) getApplication();
		Log.d(TAG,"Calling startDiscovery from DeviceListActivity");
		bluetooth_manager.connection.startDiscovery();
		btPaired = bluetooth_manager.connection.getPairedDevices();
		
		//btPaired = bluetooth_manager.getConnectableDevices();
		
		lv = (ListView) findViewById(R.id.paired_devices);
		bondedDevices = new ArrayAdapter<String>(this, R.layout.device_name);
		Iterator<Map.Entry<String, String>> devices = btPaired.entrySet().iterator();
		while (devices.hasNext()) {
			Map.Entry<String, String> device = (Map.Entry<String, String>) devices
					.next();
			bondedDevices.add(device.getValue() + "\n" + device.getKey());
		}
		lv.setAdapter(bondedDevices);
		lv.setOnItemClickListener(this);
		setResult(RESULT_CANCELED);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
		
		Log.d(TAG,"OnItemClick");
		
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

}