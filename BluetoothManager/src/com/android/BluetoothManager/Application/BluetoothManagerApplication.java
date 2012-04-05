package com.android.BluetoothManager.Application;

import java.util.HashMap;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import com.android.BluetoothManager.Radio.Connection_Manager;
import com.android.BluetoothManager.Radio.RadioPacketReceiver;
import com.android.BluetoothManager.Routing.PacketHandlerService;
import com.android.BluetoothManager.Routing.RouteTable;
import com.android.BluetoothManager.Routing.RoutingPacketReceiver;
import com.android.BluetoothManager.UI.R;
import com.android.BluetoothManager.UI.UIPacketReceiver;

/* Context used for access to objects across application.
 * Initialises the radio and routing objects.
 * All the registration of the BroadcastReceivers are done here.
 */

public class BluetoothManagerApplication extends Application {

	private static final String TAG = "BluetoothManagerApplication";

	// Packet receiver for radio layer
	RadioPacketReceiver radio_packet_receiver;
	
	// Radio layer object managing connections
	public Connection_Manager connection_manager;
	
	// Packet receiver for routing layer
	RoutingPacketReceiver routing_packet_receiver;

	//Routing thread
	public PacketHandlerService routing_thread;

	// The table that will be used for routing
	public RouteTable route_table;
	
	// Packet receiver for UI layer
	public UIPacketReceiver ui_packet_receiver;

	public Handler ui_handler;
	
	@Override
	public void onCreate() {
		super.onCreate();

		// Instantiate the radio connection_manager
		connection_manager = new Connection_Manager(this);
		
		// Getting the intent strings from the XML file.
		Log.d(TAG, "Application OnCreate");
		String UI_TO_ROUTING = getResources().getString(R.string.UI_TO_ROUTING);
		String RADIO_TO_ROUTING = getResources().getString(
				R.string.RADIO_TO_ROUTING);
		String ROUTING_TO_UI = getResources().getString(R.string.ROUTING_TO_UI);
		String ROUTING_TO_RADIO = getResources().getString(
				R.string.ROUTING_TO_RADIO);

		// Instantiate the PacketRecievers and registering it to listen for respective packets
		routing_packet_receiver = new RoutingPacketReceiver(this);
		IntentFilter r = new IntentFilter();
		r.addAction(UI_TO_ROUTING);
		r.addAction(RADIO_TO_ROUTING);
		registerReceiver(routing_packet_receiver, r);

		// Instantiate the UI Receiver and registering it.
		ui_packet_receiver = new UIPacketReceiver(this);
		IntentFilter i = new IntentFilter();
		i.addAction(ROUTING_TO_UI);
		registerReceiver(ui_packet_receiver, i);

		// Instantiate the Radio layer receiver and register it.
		radio_packet_receiver = new RadioPacketReceiver(this);
		IntentFilter p = new IntentFilter();
		p.addAction(ROUTING_TO_RADIO);
		registerReceiver(radio_packet_receiver, p);

		// Initialise the route table on startup
		route_table = new RouteTable(this);

		// The routing thread is created which processes the UI an Radio Queues.
		routing_thread = new PacketHandlerService();

		//Start the radio layer, which will also start routing thread
		connection_manager.startRadio();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();

		route_table=null;
		routing_thread.interrupt();
		routing_thread=null;
		connection_manager.stopRadio();
		connection_manager=null;
	}

	public String getSelfAddress() {
		try {
			return connection_manager.getSelfAddress();
		} catch (RemoteException e) {
			Log.d(TAG, " ++ Unable to retrieve selfAddress");
		}
		return null;
	}

	public HashMap<String, String> getConnectableDevices() {
		return connection_manager.getPairedDevices();
	}

	public void sendDataToRoutingFromUI(String device, String msg, String type) {

		Intent intent = new Intent();
		intent.setAction(getResources().getString(R.string.UI_TO_ROUTING));
		intent.putExtra("device", device);
		intent.putExtra("msg", msg);
		Log.d(TAG, "Sending msg to Routing from UI :" + msg);
		sendBroadcast(intent);

	}
}
