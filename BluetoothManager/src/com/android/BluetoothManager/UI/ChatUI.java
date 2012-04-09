package com.android.BluetoothManager.UI;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import com.android.BluetoothManager.Application.BluetoothManagerApplication;
import com.android.BluetoothManager.Application.R;
import com.android.BluetoothManager.UI.viewpager.TitlePageIndicator;

public class ChatUI extends BaseActivity {

	private final String TAG = "ChatUI";
	
	
	private ArrayAdapter<String> currentAdapter = null;
	private String currentDevice = null;
	
	private Button chat_msg_send;
	private EditText chat_edit_text;
	
	private String CHAT_TYPE = "chat";
	
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

}
