package com.android.BluetoothManager.UI;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.BluetoothManager.Application.BluetoothManagerApplication;
import com.android.BluetoothManager.Application.R;
import com.android.BluetoothManager.UI.viewpager.TitlePageIndicator;

public class ChatUI extends BaseActivity {

	private final String TAG = "ChatUI";
	
	private ViewPagerAdapter adapter;
	private ViewPager pager;
	private TitlePageIndicator indicator;
	private ArrayAdapter<String> currentAdapter = null;
	private String currentDevice = null;
	
	static Button chat_msg_send;
	static EditText chat_edit_text;
	
	private String CHAT_TYPE = "chat";
	int GET_DEVICE_FOR_CHAT = 1;


	BluetoothManagerApplication bluetooth_manager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_ui);
		
		bluetooth_manager = (BluetoothManagerApplication) getApplication();
		
		adapter = bluetooth_manager.ui_packet_receiver.adapter;
		
		indicator = (TitlePageIndicator) findViewById(R.id.indicator);
		pager = (ViewPager) findViewById(R.id.viewpager);
		chat_msg_send = (Button) findViewById(R.id.chat_button_send);
		chat_edit_text = (EditText) findViewById(R.id.edit_text_chat);
				
		bluetooth_manager.ui_packet_receiver.indicator = indicator;
		
		if(bluetooth_manager.ui_packet_receiver.adapter.deviceAddresses.size() == 0)
		{
			chat_msg_send.setVisibility(View.GONE);
			chat_edit_text.setVisibility(View.GONE);
		}
		
		pager.setAdapter(adapter);
		indicator.setViewPager(pager);
		
		adapter.notifyDataSetChanged();
		indicator.notifyDataSetChanged();
		
		indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				Log.d(TAG,"onPageSelected() called");
				currentAdapter = adapter.getChatAdapter(position);
				currentDevice = adapter.getDevice(position);
			}

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				Log.d(TAG,"onPageScrolled() called");
			}

			@Override
			public void onPageScrollStateChanged(int state) {
				Log.d(TAG,"onPageScrollStateChanged() called");
			}
		});
		
	}
	
	public void sendChatMsg(View v){
		if(bluetooth_manager.ui_packet_receiver.adapter.getCount() == 1){
			currentAdapter = bluetooth_manager.ui_packet_receiver.adapter.getChatAdapter(0);
			currentDevice = bluetooth_manager.ui_packet_receiver.adapter.getDevice(0);
		}
		String msg = chat_edit_text.getText()+"";
		currentAdapter.add("me: "+msg);
		msg = "chat,"+msg;
		bluetooth_manager.sendDataToRoutingFromUI(currentDevice, msg, CHAT_TYPE );
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		Log.d(TAG, "Base Activity: on Create Options!!");
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "ActivityResult Called");
		if (requestCode == GET_DEVICE_FOR_CHAT && resultCode == RESULT_OK) {
			String device = data
					.getStringExtra(DeviceListActivity.DEVICE_ADDRESS);
			String name = data.getStringExtra(DeviceListActivity.DEVICE_NAME);
			Toast.makeText(this, name, Toast.LENGTH_SHORT).show();

			
			Log.d(TAG, "Name of the new Device: " + name);
			int count = bluetooth_manager.ui_packet_receiver.adapter.getCount();

			if (count == 0) {
				makeChatComponentsVisible(); // If this is the first device that
												// is being added then make the
												// components visible
			}

			Log.d(TAG, "No of Chats:" + count);
			if (!bluetooth_manager.ui_packet_receiver.conversation_map
					.containsKey(device)) {
				bluetooth_manager.ui_packet_receiver.adapter.addDevice(device,
						name, "");
				bluetooth_manager.ui_packet_receiver.adapter
						.notifyDataSetChanged();
				bluetooth_manager.ui_packet_receiver.indicator
						.notifyDataSetChanged();
				pager.setCurrentItem(count);
			} else {
				pager.setCurrentItem(bluetooth_manager.ui_packet_receiver.adapter.deviceAddresses
						.indexOf(device));
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_startChat:
			Log.d(TAG, "Start New Chat");
			startActivityForResult(new Intent(this, DeviceListActivity.class),
					GET_DEVICE_FOR_CHAT);
			break;
		default:
			break;
		}
		Log.d(TAG, "Base Activity: On Option Item Selected !!");
		return true;
	}
	
	static void makeChatComponentsVisible()
	{
		chat_edit_text.setVisibility(View.VISIBLE);
		chat_msg_send.setVisibility(View.VISIBLE);
	}

}
