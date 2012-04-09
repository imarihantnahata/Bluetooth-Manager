package com.android.BluetoothManager.UI;

import com.android.BluetoothManager.Application.BluetoothManagerApplication;
import com.android.BluetoothManager.Application.R;
import com.android.BluetoothManager.UI.viewpager.TitlePageIndicator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class BaseActivity extends Activity {

	private String TAG = "BaseActivity";

	ViewPagerAdapter adapter;
	ViewPager pager;

	TitlePageIndicator indicator;

	BluetoothManagerApplication bluetooth_manager;

	int GET_DEVICE_FOR_CHAT = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bluetooth_manager = (BluetoothManagerApplication) getApplication();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
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
		default:
			break;
		}
		Log.d(TAG, "Base Activity: On Option Item Selected !!");
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
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

}
