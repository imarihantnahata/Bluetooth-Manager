package com.android.BluetoothManager.UI;

import com.android.BluetoothManager.Application.BluetoothManagerApplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UIPacketReceiver extends BroadcastReceiver {

	private final String TAG = "UIPacketReceiver"; 
	
	BluetoothManagerApplication bluetooth_manager;

	public UIPacketReceiver(BluetoothManagerApplication bluetooth_manager) {
		this.bluetooth_manager = bluetooth_manager;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String device = intent.getStringExtra("device");
		String msg = intent.getStringExtra("msg");
		Log.d(TAG, "Received msg:"+msg+" from:"+device);
	}

}
