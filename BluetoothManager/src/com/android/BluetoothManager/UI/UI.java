package com.android.BluetoothManager.UI;

import com.android.BluetoothManager.Application.BluetoothManagerApplication;
import com.android.BluetoothManager.Application.R;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TabHost;
import android.widget.Toast;

public class UI extends TabActivity {

	//application object reference to be used throughout the UI
	static BluetoothManagerApplication bluetooth_manager;
	
	//static variable indicating if DeviceListActivity is Searching
	static boolean is_UI_searching;
		
	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui);
		
		bluetooth_manager = (BluetoothManagerApplication) getApplication();
		bluetooth_manager.ui_handler=this.ui_handler;
		is_UI_searching=false;
		Resources res = getResources(); // Resource object to get Drawables
		TabHost tabHost = getTabHost(); // The activity TabHost
		TabHost.TabSpec spec; // Resusable TabSpec for each tab
		Intent intent; // Reusable Intent for each tab

		// Create an Intent to launch an Activity for the tab (to be reused)
		intent = new Intent().setClass(this, MessageUI.class);
 
		// Initialize a TabSpec for each tab and add it to the TabHost
		spec = tabHost
				.newTabSpec("message")
				.setIndicator("Message", res.getDrawable(R.drawable.ic_tab_msg))
				.setContent(intent);
		tabHost.addTab(spec);

		// Do the same for the other tabs
		intent = new Intent().setClass(this, ChatUI.class);
		spec = tabHost.newTabSpec("chat")
				.setIndicator("Chat", res.getDrawable(R.drawable.ic_tab_chat))
				.setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, FTPUI.class);
		spec = tabHost.newTabSpec("ftp")
				.setIndicator("FTP", res.getDrawable(R.drawable.ic_tab_ftp))
				.setContent(intent);
		tabHost.addTab(spec);

		tabHost.setCurrentTab(0);
		
	}

	public final Handler ui_handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what)
			{
				
				default:
					Toast.makeText(bluetooth_manager, (String)msg.obj, Toast.LENGTH_LONG).show();
			}
			
		}
		
	};

}