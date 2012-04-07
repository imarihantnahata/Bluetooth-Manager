package com.android.BluetoothManager.UI;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.net.Uri;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.android.BluetoothManager.Application.BluetoothManagerApplication;
import com.android.BluetoothManager.Application.R;
import com.android.BluetoothManager.UI.viewpager.TitlePageIndicator;

public class UIPacketReceiver extends BroadcastReceiver {

	// This HashMap is used to map
	public HashMap<String, ArrayAdapter<String>> conversation_map;

	private final String TAG = "UIPacketReceiver";

	// These fields define the type of packet at the UI level.
	private String MSG_TYPE = "msg";
	private String CHAT_TYPE = "chat";
	private String FILE_TYPE= "file";

	private int NOTIFICATION_ID = 1;

	String notification_string = Context.NOTIFICATION_SERVICE;
	NotificationManager notification_service;

	// Adapter that holds all the ListViews, Device names and their MAC address
	public ViewPagerAdapter adapter;
	public TitlePageIndicator indicator;
	BluetoothManagerApplication bluetooth_manager;

	public UIPacketReceiver(BluetoothManagerApplication bluetooth_manager) {
		this.bluetooth_manager = bluetooth_manager;
		notification_service = (NotificationManager) bluetooth_manager
				.getSystemService(notification_string);

		conversation_map = new HashMap<String, ArrayAdapter<String>>();

		adapter = new ViewPagerAdapter(
				this.bluetooth_manager.getApplicationContext(),
				conversation_map);
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		String device = intent.getStringExtra("device");
		String msg = intent.getStringExtra("msg");
		String src_name = intent.getStringExtra("src_name");
		
		adapter.printContents(adapter.deviceNames);
		adapter.printContents(adapter.deviceAddresses);
		Log.d(TAG, "Received msg:" + msg + " from:" + device);

		// Find the type of packet received. i.e. chat or msg
		String dataType = msg.substring(0, msg.indexOf(","));
		
		// Process the packet according to the type.
		if (dataType.equals(MSG_TYPE)) {
			processMsgData(device, src_name, msg.substring(msg.indexOf(",") + 1));
		}
		else if (dataType.equals(CHAT_TYPE)) {
			processChatData(device, src_name, msg.substring(msg.indexOf(",") + 1));
		}
		else if(dataType.equals(FILE_TYPE))
		{
			processFileData(device, src_name, msg.substring(msg.indexOf(",") + 1));
		}
		return;
	}
	
	// Creates the received file in the /sdcard/bluetooth directory
	private void processFileData(String device, String src_name,
			String msg) {
		String filename=msg.substring(0,msg.indexOf(',')-1);
		String fileData=msg.substring(msg.indexOf(',')+1);
		try{
			File f;
			f=new File("/sdcard/bluetooth/"+filename);
			if(!f.exists())
			  f.createNewFile();
			else
				return;
			FileWriter fw=new FileWriter(f);
			fw.write(fileData);
			fw.close();		
		}
		catch(IOException e)
		{
			Log.d(TAG,e.getMessage());
		}
		setNotificationForMsg("File Received from: "+src_name, filename, "Saved in /sdcard/bluetooth");
		
	}

	/* Checks if it a new chat or continuation of the old chat.
	 * if old then just add the string to the chatAdapter, else
	 * add a new device to the ViewPagerAdapter.
	 */
	private void processChatData(String device, String name, String msg) {
		
		if (conversation_map.containsKey(device)) {
			Log.d(TAG, "Device found: " + device);
			ArrayAdapter<String> chatAdapter = conversation_map.get(device);
			Log.d(TAG, "chatAdapter:" + chatAdapter);
			chatAdapter.add(name+":"+ msg);
			chatAdapter.notifyDataSetChanged();
		} else {
			adapter.addDevice(device, name, msg);
			adapter.notifyDataSetChanged();
		}
		
	}
	/* After receiving the MSG packet, it is added to in box and a 
	 * notification is shown to the user.
	 */
	private void processMsgData(String device, String name, String msg) {
		Log.d(TAG,"Message received to UI"+msg);
		addMsgToInbox(name, msg);
		setNotificationForMsg("New message from: " + name, name, msg);
	}

	private void addMsgToInbox(String name, String msg) {
		Log.d(TAG, "Adding SMS to inbox.");
		ContentValues values = new ContentValues();
		values.put("address", name);
		values.put("body", msg);
		bluetooth_manager.getContentResolver().insert(
				Uri.parse("content://sms/inbox"), values);
		Log.d(TAG, "SMS Added to inbox.");
	}

	private void setNotificationForMsg(String ticker, String title, String text) {

		int icon = R.drawable.ic_menu_dialog;
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, ticker, when);
		Context context = bluetooth_manager.getApplicationContext();
		Intent notificationIntent = new Intent(Intent.ACTION_MAIN);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.setType("vnd.android-dir/mms-sms");
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(context, title, text, contentIntent);
		notification_service.notify(NOTIFICATION_ID, notification);

	}
}
