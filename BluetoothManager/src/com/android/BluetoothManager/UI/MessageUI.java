package com.android.BluetoothManager.UI;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.android.BluetoothManager.Application.BluetoothManagerApplication;
import com.android.BluetoothManager.Application.R;

public class MessageUI extends Activity {

	private static final String TAG = "MessageUI";
	Button msg_send;
	EditText msg_input;
	int GET_DEVICE_FOR_MSG = 0;
	final String MSG_TYPE = "msg";
	
	ListView msg_list;
	static ArrayAdapter<String> msg_array;
	BluetoothManagerApplication bluetooth_manager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.message_ui);
		msg_send = (Button) findViewById(R.id.msg_button_send);
		msg_input = (EditText) findViewById(R.id.msg_text_input);
		bluetooth_manager = (BluetoothManagerApplication) getApplication();
		msg_list=(ListView) findViewById(R.id.msg_list_log);
		msg_array= new ArrayAdapter<String>(this, R.layout.device_name);
		msg_list.setAdapter(msg_array);
	}

	public void sendMsg(View v) {
		startActivityForResult(new Intent(this, DeviceListActivity.class),
				GET_DEVICE_FOR_MSG);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG,"onActivityResult() called !");
		if (requestCode == GET_DEVICE_FOR_MSG && resultCode == RESULT_OK) {
			String device = data
					.getStringExtra(DeviceListActivity.DEVICE_ADDRESS);
			String name = data.getStringExtra(DeviceListActivity.DEVICE_NAME);
			String msg = "msg,"+msg_input.getText().toString();
			Toast.makeText(this, device, Toast.LENGTH_SHORT).show();
			bluetooth_manager.sendDataToRoutingFromUI(device, name, msg, MSG_TYPE);
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
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}
	
	
}
