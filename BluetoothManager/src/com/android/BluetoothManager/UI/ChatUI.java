package com.android.BluetoothManager.UI;

import android.app.Activity;
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

public class ChatUI extends Activity {

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

		if (adapter.deviceAddresses.size() == 0) {
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
				Log.d(TAG, "onPageSelected() called." + position);
				setCurrentPosition(position);
			}

			@Override
			public void onPageScrolled(int position, float positionOffset,
					int positionOffsetPixels) {
				Log.d(TAG, "onPageScrolled() called Position:" + position);
			}

			@Override
			public void onPageScrollStateChanged(int state) {
				// Log.d(TAG, "onPageScrollStateChanged() called");
			}
		});

	}

	private void setCurrentPosition(int position) {
		if(position == -1 ){
			currentAdapter = null;
			currentDevice = null;
			return;
		}
		currentAdapter = adapter.getChatAdapter(position);
		currentDevice = adapter.getDevice(position);
	}

	public void sendChatMsg(View v) {
		String msg = chat_edit_text.getText() + "";
		currentAdapter.add("me: " + msg);
		msg = "chat," + msg;
		bluetooth_manager
				.sendDataToRoutingFromUI(currentDevice, msg, CHAT_TYPE);
		chat_edit_text.setText("");
		chat_edit_text.clearFocus();
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
			int count = adapter.getCount();

			if (count == 0) {
				makeChatComponentsVisible(); // If this is the first device that
												// is being added then make the
												// components visible
			}

			Log.d(TAG, "No of Chats:" + count);
			if (!bluetooth_manager.ui_packet_receiver.conversation_map.containsKey(device)) {
				adapter.addDevice(device, name, "");
				adapter.notifyDataSetChanged();
				indicator.notifyDataSetChanged();
				pager.setCurrentItem(count);
				if (adapter.getCount() == 1) {
					setCurrentPosition(0);
				}
			} else {
				pager.setCurrentItem(bluetooth_manager.ui_packet_receiver.adapter.deviceAddresses
						.indexOf(device));
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		Log.d(TAG, "Base Activity: on Create Options!!");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_startChat:
			Log.d(TAG, "Start New Chat");
			startActivityForResult(new Intent(this, DeviceListActivity.class),
					GET_DEVICE_FOR_CHAT);
			break;
		case R.id.menu_endChat:
			if (currentDevice != null) {
				int position = adapter.deviceAddresses.indexOf(currentDevice);
				Log.d(TAG, "Position: " + position);
				removeChat(position);
			} else {
				Toast.makeText(bluetooth_manager, "Please start a chat first",
						Toast.LENGTH_LONG);
			}
			break;
		default:
			break;
		}
		Log.d(TAG, "Base Activity: On Option Item Selected !!");
		return true;
	}

	private void removeChat(int position) {

		int newPosition;
		adapter.deviceNames.remove(position);
		adapter.listViews.remove(position);
		adapter.conversation_map.remove(adapter.deviceAddresses.get(position));
		adapter.deviceAddresses.remove(position);
		indicator.notifyDataSetChanged();
		adapter.notifyDataSetChanged();
		adapter.printContents(adapter.deviceAddresses);
		adapter.printContents(adapter.deviceNames);
		if (adapter.deviceAddresses.size() > 0) {
			if (adapter.deviceAddresses.size() == 1) {
				newPosition = 0;
			} else {
				if (adapter.deviceAddresses.size() == position) {
					newPosition = position - 1;
				} else {
					newPosition = position;
				}
			}
			setCurrentPosition(newPosition);
		} else {
			setCurrentPosition(-1); // i.e. no chats are present.
		}
	}

	static void makeChatComponentsVisible() {
		chat_edit_text.setVisibility(View.VISIBLE);
		chat_msg_send.setVisibility(View.VISIBLE);
	}

}
