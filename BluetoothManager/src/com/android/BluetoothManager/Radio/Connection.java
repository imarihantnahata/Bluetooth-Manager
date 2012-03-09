package com.android.BluetoothManager.Radio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.util.Log;

import com.android.BluetoothManager.Application.BluetoothManagerApplication;
import com.android.BluetoothManager.UI.R;

public class Connection {

	private static final String TAG = "Connection";

	public static final int MAX_CONNECTIONS_SUPPORTED = 7;

	public static final int SUCCESS = 0;

	public static final int FAILURE = 1;

	private boolean server_isRunning = false;

	private BluetoothAdapter BtAdapter;
	
	private Thread server_thread;

	private String service_name = "BluetoothManagerService"; // Random String
														// used for
														// starting
														// server.

	private String friend_service_name = "Friend_Service"; // Random String

	private ArrayList<UUID> Uuids; // List of UUID's

	UUID friend_uuid; // UUID to check if this application is running on other
						// phone or not.

	ArrayList<String> BtConnectedDeviceAddresses; // List of addresses
													// to which
	// the devices are currently
	// connected

	HashMap<String, BluetoothSocket> BtSockets; // Mapping between
												// address and the
												// corresponding Scoket

	HashMap<String, String> BtFoundDevices; // Mapping between the
											// devices and the names.
											// this list to be passed to
											// the UI layer.contains
											// only found devices

	HashMap<String, String> BtBondedDevices; // Mapping between the
												// devices and the
												// names. this list to
												// be passed to the UI
												// layer. contains only
												// Bonded devices

	HashMap<String, Thread> BtStreamWatcherThreads;

	BluetoothManagerApplication bluetooth_manager;

	private long lastDiscovery = 0; // Stores the time of the last discovery

	public Connection(BluetoothManagerApplication bluetooth_manager) {

		Log.d(TAG, "Started at");
		

		this.bluetooth_manager = bluetooth_manager;

		Uuids = new ArrayList<UUID>();
		// Allow up to 7 devices to connect to the server
		Uuids.add(UUID.fromString("a60f35f0-b93a-11de-8a39-08002009c666"));
		Uuids.add(UUID.fromString("503c7430-bc23-11de-8a39-0800200c9a66"));
		Uuids.add(UUID.fromString("503c7431-bc23-11de-8a39-0800200c9a66"));
		Uuids.add(UUID.fromString("503c7432-bc23-11de-8a39-0800200c9a66"));
		Uuids.add(UUID.fromString("503c7433-bc23-11de-8a39-0800200c9a66"));
		Uuids.add(UUID.fromString("503c7434-bc23-11de-8a39-0800200c9a66"));
		Uuids.add(UUID.fromString("503c7435-bc23-11de-8a39-0800200c9a66"));

		friend_uuid = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

		// Registration for Bluetooth Events.
		IntentFilter i = new IntentFilter();
		i.addAction(BluetoothDevice.ACTION_FOUND);
		i.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		i.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		bluetooth_manager.registerReceiver(receiver, i);

		startRadio();
	}

	/* Function that will start Discovery only if it was done
	 * more than a minute ago
	 */
	public void startDiscovery()
	{
		if (BtAdapter.isDiscovering()) {
			return;
		}
		Log.d(TAG, "Starting Discovery !!");
		if (System.currentTimeMillis() / 1000 - lastDiscovery > 60) {
			BtAdapter.startDiscovery();
			lastDiscovery = System.currentTimeMillis() / 1000;
			try {
				Thread.sleep(12000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/* Function to connect to a remote device.
	 * Will first check if already connected. If not, it will try
	 * to create a socket from the list of 7 UUID's on the device.
	 * If found, it will create a watcher thread for that device and
	 * add to the connected devices and start listening
	 */
	private int connect(String device) throws RemoteException {

		Log.d(TAG, "Trying to connect to: " + device);
		if (BtConnectedDeviceAddresses.contains(device)) {
			Log.d(TAG, "Already connected to: " + device);
			return Connection.SUCCESS;
		}

		BluetoothDevice myBtServer = BtAdapter.getRemoteDevice(device);

		BluetoothSocket myBSock = null;

		Log.d(TAG, "Creating Sockets");

		for (int i = 0; i < Connection.MAX_CONNECTIONS_SUPPORTED
				&& myBSock == null; i++) {
			myBSock = getConnectedSocket(myBtServer, Uuids.get(i));
			
			if (myBSock == null) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					Log.e(TAG, "InterruptedException in connect", e);
				}
			} else {
				break;
			}
		}
		if (myBSock == null) {
			return Connection.FAILURE;
		}

		BtSockets.put(device, myBSock);
		BtConnectedDeviceAddresses.add(device);
		Thread BtStreamWatcherThread = new Thread(new BtStreamWatcher(device));
		BtStreamWatcherThread.start();
		BtStreamWatcherThreads.put(device, BtStreamWatcherThread);
		return Connection.SUCCESS;
	}

	/* Function that returns a connection socket if connection
	 * succeeds on the particular uuid on the myBtServer, i.e, remote device
	 */
	private BluetoothSocket getConnectedSocket(BluetoothDevice myBtServer,
			UUID uuidToTry) {
		BluetoothSocket myBSock;
		try {
			myBSock = myBtServer.createRfcommSocketToServiceRecord(uuidToTry);
			Log.d(TAG,
					"Trying to connect to socket of:" + myBtServer.getAddress()
					+ " with UUID +"+uuidToTry);
			myBSock.connect();
			return myBSock;
		} catch (IOException e) {
			Log.i(TAG,
					"IOException in getConnectedSocket. Msg:" + e.getMessage());
		}
		return null;
	}

	/* Function that will broadcast RREQ's.
	 * Will first search for all found devices. Will then connect
	 * to all found devices and send an RREQ to them.
	 */
	public int broadcastMessage(String message) throws RemoteException {

		startDiscovery();
		connectToFoundDevices();
		int size = BtConnectedDeviceAddresses.size();
		for (int i = 0; i < size; i++) {
			sendMessageToDestination(BtConnectedDeviceAddresses.get(i), message);
		}
		return Connection.SUCCESS;
	}

	//For debugging. To print connections at a certain point of time
	public String getConnections(String srcApp) throws RemoteException {

		String connections = "";
		int size = BtConnectedDeviceAddresses.size();
		for (int i = 0; i < size; i++) {
			connections = connections + BtConnectedDeviceAddresses.get(i) + ",";
		}
		return connections;
	}

	/* Method that actually sends the stream of bytes to a remote device.
	 * First tries to connect. If successful, writes data after
	 * fetching its socket
	 */
	public int sendMessageToDestination(String destination, String message)
			throws RemoteException {

		int status = connect(destination);

		if (status == Connection.SUCCESS) {
			try {
				BluetoothSocket myBsock = BtSockets.get(destination);
				if (myBsock != null) {
					OutputStream outStream = myBsock.getOutputStream();
					byte[] stringAsBytes = (message + " ").getBytes();
					stringAsBytes[stringAsBytes.length - 1] = 0; // Add a stop
					// marker
					outStream.write(stringAsBytes);
					return Connection.SUCCESS;
				}
			} catch (IOException e) {
				Log.i(TAG, "IOException in sendMessage - Dest:" + destination
						+ ", Msg:" + message, e);
			}
		}
		return Connection.FAILURE;
	}

	public String getAddress() throws RemoteException {
		return BtAdapter.getAddress();
	}

	public BluetoothAdapter getBluetoothAdapter() {
		return BtAdapter;
	}

	/* Function that will return the devices that
	 * are already paired, but not necessarily in range 
	 */
	public HashMap<String, String> getPairedDevices() {

		Set<BluetoothDevice> devices = BtAdapter.getBondedDevices();
		for (BluetoothDevice device : devices) {
			BtBondedDevices.put(device.getAddress(), device.getName());
		}

		return BtBondedDevices;
	}

	/* Function that will make connections to only the devices which are found. 
	 * They have to be discoverable and within range
	 */
	public void connectToFoundDevices() {
		Log.d(TAG, "connectToFoundDevices() called");
		Iterator devices = BtFoundDevices.entrySet().iterator();
		while (devices.hasNext()) {
			Map.Entry<String, String> device = (Map.Entry<String, String>) devices
					.next();
			try {
				if (isMyFriend(device.getKey())) {
					connect(device.getKey());
				} else {
					Log.d(TAG, device.getKey() + " is not my friend.");
				}
			} catch (RemoteException e) {
				Log.d(TAG, "Couldn't connect to " + device.getKey());
			}
		}
	}

	public void makeDeviceDisocverable() {
		Log.d(TAG, "Making Device Discoverable.");
		Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		bluetooth_manager.startActivity(i);
	}

	public boolean isMyFriend(String address) {

		boolean is_my_friend = false;

		Log.d(TAG, "Checking if " + address + " is my friend");

		BluetoothDevice myBtServer = BtAdapter.getRemoteDevice(address);

		BluetoothSocket myBSock = null;

		/*
		 * Try this for three times since there might be other nodes which are
		 * trying to connect to the same device.
		 */
		for (int i = 0; i < 2; i++) {

			Log.d(TAG, " Trying to create Socket to see if " + address
					+ "is my friend");
			myBSock = getConnectedSocket(myBtServer, friend_uuid);
			Log.d(TAG, "After getConnectedSocket(): " + myBSock);
			if (myBSock == null) {
				try {
					Thread.sleep(25);
				} catch (InterruptedException e) {
					Log.e(TAG, "InterruptedException in connect", e);
				}
			} else {
				is_my_friend = true;
				try {
					myBSock.close();
				} catch (IOException e) {
					Log.d(TAG,
							"Error while closing friend socket. "
									+ e.getMessage());
				}
				break;
			}
		}
		return is_my_friend;
	}

	/*
	 * Start the server which listens for connections used to determine if the
	 * other node has our application running or not.
	 */
	public void startFriendServer() {
		(new Thread(new FriendServer())).start();
	}

	/*
	 * Function to be called when Application starts and later when Bluetooth 
	 * is turned on. Instantiates the lists, checks if Adapter is enabled and starts 
	 * the server thread
	 */
	public int startRadio() {
			
		BtSockets = new HashMap<String, BluetoothSocket>();

		BtConnectedDeviceAddresses = new ArrayList<String>();

		BtBondedDevices = new HashMap<String, String>();

		BtFoundDevices = new HashMap<String, String>();

		BtStreamWatcherThreads = new HashMap<String, Thread>();
		
		BtAdapter = BluetoothAdapter.getDefaultAdapter();
		if (BtAdapter != null) {
			BtAdapter.enable();
		}
		
		if (server_isRunning) {
			return Connection.FAILURE;
		}
		if (BtAdapter.isEnabled()) {
			server_thread=new Thread(new ConnectionWaiter());
			server_thread.start();
			Log.d(TAG, " ++ Server Started ++");
			server_isRunning = true;
			return Connection.SUCCESS;
		}
		return Connection.FAILURE;

	}

	/*
	 * Function called when Bluetooth will be turned off. Stops the thread which
	 * listens for other connections. Removes sockets and other lists
	 * for GC and make the Routing thread wait till it is started again
	 */
	public int stopRadio() {
		if(server_isRunning)
		{
			try {
				int size = BtConnectedDeviceAddresses.size();
				for (int i = 0; i < size; i++) {
					BluetoothSocket myBsock = BtSockets
							.get(BtConnectedDeviceAddresses.get(i));
					myBsock.close();
				}
				BtSockets = null;
				BtStreamWatcherThreads = null;
				BtConnectedDeviceAddresses = null;
				BtFoundDevices = null;
				server_thread.interrupt();
				server_isRunning=false;
				Log.d(TAG, " ++ Server Stopped ++");
			} catch (IOException e) {
				Log.d(TAG,"Error in stopRadio(). "+e.getMessage());
			}
			return Connection.SUCCESS;
		}
		else
		{
			return Connection.FAILURE;
		}
		
	}
	
	private void communicateFromRadioToRouting(String address, String message) {
		String ACTION = bluetooth_manager.getResources().getString(
				R.string.RADIO_TO_ROUTING);
		Intent i = new Intent();
		i.setAction(ACTION);
		i.putExtra("layer", "radio");
		i.putExtra("device", address);
		i.putExtra("msg", message);
		bluetooth_manager.sendBroadcast(i);
		Log.d(TAG, "Intent Send from Radio to routing");
	}

	/*
	 * FriendServer that listens for friendly connections. This mechanism is
	 * just used to check if the node is running our application.
	 */
	private class FriendServer implements Runnable {

		@Override
		public void run() {
			BluetoothServerSocket myServerSocket;
			while (true) {
				try {
					// Start listening with friend_uuid
					Log.d(TAG, "Starting FriendServer");
					myServerSocket = BtAdapter
							.listenUsingRfcommWithServiceRecord(
									friend_service_name, friend_uuid);
					Log.d(TAG, "FriendServer Started..");
					BluetoothSocket myBSock = myServerSocket.accept();
					myServerSocket.close(); // Close the socket now that the
											// connection has been made.
					if (myBSock != null) {
						Log.d(TAG, "Found a Friend:"
								+ myBSock.getRemoteDevice().getName() + " "
								+ myBSock.getRemoteDevice().getAddress());
						myBSock.close(); // Close this socket too since we now
											// know
											// that the node is running our
											// application
					}

				} catch (IOException e) {
					Log.d(TAG, "Error in FriendServer: " + e.getMessage());
				}
			}

		}
	}

	// The BroadcastReceiver that listens for discovered devices and
	// changes the title when discovery is finished
	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				BluetoothClass bc = device.getBluetoothClass();
				if (bc.getMajorDeviceClass() == BluetoothClass.Device.Major.PHONE) {
					BtFoundDevices.put(device.getAddress(), device.getName());
				}
				Log.d(TAG, "Found " + device.getAddress());

			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {
				// Do something when the search finishes.
				Log.d(TAG, "Service Discovery Finished !");
			} else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				Log.d(TAG,"Bluetooth State Changed");
				String state = intent.getStringExtra(BluetoothAdapter.ACTION_STATE_CHANGED);
				Log.d(TAG,"State: "+state);
				/*if (state == BluetoothAdapter.STATE_TURNING_OFF) {
					Log.d(TAG,"Bluetooth is turning Off, stopping radio");
					stopRadio();
				} else {
					if (state==BluetoothAdapter.STATE_ON) {
						Log.d(TAG,"Bluetooth is on, starting radio");
						startRadio();
					}
				}*/

			}
		}
	};

	/*
	 * This class is responsible for listening for new connections. Once a
	 * connections is accepted, a new thread is created to manage the i/p, o/p
	 * with the newly established connection
	 */
	private class ConnectionWaiter implements Runnable {

		public void run() {
			try {
				for (int i = 0; i < Connection.MAX_CONNECTIONS_SUPPORTED; i++) {
					BluetoothServerSocket myServerSocket = BtAdapter
							.listenUsingRfcommWithServiceRecord(service_name,
									Uuids.get(i));
					BluetoothSocket myBSock = myServerSocket.accept();
					myServerSocket.close(); // Close the socket now that the
											// connection has been made.

					String address = myBSock.getRemoteDevice().getAddress();
					// String name = myBSock.getRemoteDevice().getName();

					BtSockets.put(address, myBSock);
					BtConnectedDeviceAddresses.add(address);
					Thread BtStreamWatcherThread = new Thread(
							new BtStreamWatcher(address));
					BtStreamWatcherThread.start();
					BtStreamWatcherThreads.put(address, BtStreamWatcherThread);

				}

			} catch (IOException e) {
				Log.i(TAG, "IOException in ConnectionService:ConnectionWaiter",
						e);
			}
		}
	}

	/*
	 * Thread which maintains the I/O of one stream for one device
	 */
	private class BtStreamWatcher implements Runnable {
		private String address;
		long lastReceived=0;

		public BtStreamWatcher(String deviceAddress) {
			address = deviceAddress;
		}

		public void run() {
			int bufferSize = 1024;
			byte[] buffer = new byte[bufferSize];
			BluetoothSocket bSock = BtSockets.get(address);
			try {
				InputStream instream = bSock.getInputStream();
				int bytesRead = -1;
				String message = "";
				while (true) {
					message = "";
					bytesRead = instream.read(buffer);
					if (bytesRead != -1) {
						while ((bytesRead == bufferSize)
								&& (buffer[bufferSize - 1] != 0)) {
							message = message
									+ new String(buffer, 0, bytesRead);
							bytesRead = instream.read(buffer);
						}
						message = message
								+ new String(buffer, 0, bytesRead - 1);

						Log.d(TAG, "Received " + message + " from " + address
								+ "In Connection");
						lastReceived=System.currentTimeMillis()/1000;
						communicateFromRadioToRouting(address, message);
						
					}
				}
			} catch (IOException e) {
				Log.i(TAG,
						"IOException in BtStreamWatcher - probably caused by normal disconnection",
						e);
				Log.d(TAG, "Closing Thread since probably disconnected");

			}
			// Getting out of the while loop means the connection is dead.
			try {
				BtConnectedDeviceAddresses.remove(address);
				BtSockets.remove(address);
				BtStreamWatcherThreads.remove(address);

			} catch (Exception e) {
				Log.e(TAG, "Exception in BtStreamWatcher while disconnecting",
						e);
			}
		}
	}
	
	

}
