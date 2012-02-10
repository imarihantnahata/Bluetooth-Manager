package com.android.BluetoothManager.Application;

import com.android.BluetoothManager.Radio.BluetoothManagerService;
import com.android.BluetoothManager.Routing.PacketReceiver;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;

/*
 * Contains global adapters and other state variables required
 * Initialzes the service
 */
public class BluetoothManagerApplication extends Application {

	
	//Packet Reciever object
	PacketReceiver packet_receiver;
	
	public static final String PACKET_RECEIVE_INTENT_ACTION = "com.android.BluetoothManager.PACKET_RECEIVED";

	public BluetoothManagerService bluetooth_manager_service;
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		// Instantiate the PacketReciever
		packet_receiver= new PacketReceiver();
		
		//register the PacketReciever to listen
		registerReceiver(packet_receiver, new IntentFilter(PACKET_RECEIVE_INTENT_ACTION));
		
		startService(new Intent(this,BluetoothManagerService.class));
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		stopService(new Intent(this,BluetoothManagerService.class));
	}

}
