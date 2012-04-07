package com.android.BluetoothManager.Radio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
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
import com.android.BluetoothManager.Routing.PacketHandlerService;
import com.android.BluetoothManager.Routing.RouteTable;
import com.android.BluetoothManager.Application.R;

public class Connection_Manager {

	private static final String TAG = "Connection Manager";

	public static final int MAX_CONNECTIONS_SUPPORTED = 6;

	public static final int SUCCESS = 0;

	public static final int FAILURE = 1;

	public boolean is_req_from_gui = false;

	private boolean server_isRunning = false;

	private BluetoothAdapter BtAdapter;

	private Thread server_thread = null;

	//Random String used for starting server
	private String service_name = "BluetoothManagerService";
	
	//private String friend_service_name = "Friend_Service"; // Random String

	private ArrayList<UUID> Uuids; // List of UUID's

	// UUID to check if this application is running on other phone or not.
	UUID friend_uuid; 

	Thread friendServer;
	
	ArrayList<String> BtConnectedDeviceAddresses;	// MAC's already connected to 

	HashMap<String, BluetoothSocket> BtSockets;		// Mapping between a MAC and its socket
	 
	HashMap<String, String> BtFoundDevices;		// Found devices and their names

	HashMap<String, String> BtBondedDevices; 	// Paired devices and their names

	HashMap<String, BtStreamWatcher> BtStreamWatcherThreads;

	Queue<String> broadcast_msg_queue;

	BluetoothManagerApplication bluetooth_manager;

	gc_thread maintainence_thread;				//Thread that will do the maintenance

	public boolean isSearching = false;

	private long lastDiscovery = 0; // Stores the time of the last discovery

	public Connection_Manager(BluetoothManagerApplication bluetooth_manager) {

		this.bluetooth_manager = bluetooth_manager;

		Uuids = new ArrayList<UUID>();

		// Allow up to 6 devices to connect to the server
		Uuids.add(UUID.fromString("503c7430-bc23-11de-8a39-0800200c9a66"));
		Uuids.add(UUID.fromString("503c7431-bc23-11de-8a39-0800200c9a66"));
		Uuids.add(UUID.fromString("503c7432-bc23-11de-8a39-0800200c9a66"));
		Uuids.add(UUID.fromString("503c7433-bc23-11de-8a39-0800200c9a66"));
		Uuids.add(UUID.fromString("503c7434-bc23-11de-8a39-0800200c9a66"));
		Uuids.add(UUID.fromString("503c7435-bc23-11de-8a39-0800200c9a66"));
		// Uuids.add(UUID.fromString("a60f35f0-b93a-11de-8a39-08002009c666"));

		friend_uuid = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

		// Registration for Bluetooth Events.
		IntentFilter i = new IntentFilter();
		i.addAction(BluetoothDevice.ACTION_FOUND);
		i.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		i.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		bluetooth_manager.registerReceiver(receiver, i);

		maintainence_thread = new gc_thread();
		maintainence_thread.start();

		// friendServer=new Thread(new FriendServer());

		Log.d(TAG, "Connection Object Constructed");
	}

	/* Function that will start Discovery only if it was done more than a minute
	 * ago
	 */
	public void startDiscovery() {
		if (server_isRunning) {
			if (BtAdapter.isDiscovering()) {
				Log.d(TAG, "Already Discovering !!");
				return;
			}
			Log.d(TAG, "Starting Discovery !!");
			if ((System.currentTimeMillis() / 1000) - lastDiscovery > 60) {
				BtFoundDevices.clear();
				isSearching = true;
				BtAdapter.startDiscovery();
			} else {
				Log.d(TAG,
						"Cancelling discovery because it was done too recently !! ");
				dispatchBroadcastQueue();
			}

		}
	}

	/* Function to connect to a remote device. Will first check if already
	 * connected. If not, it will try to create a socket from the list of 7
	 * UUID's on the device. If found, it will create a watcher thread for that
	 * device and add to the connected devices and start listening
	 */
	private int connect(String device) throws RemoteException {

		if (server_isRunning) {
			Log.d(TAG, "Trying to connect to: " + device);
			if (BtConnectedDeviceAddresses.contains(device)) {
				Log.d(TAG, "Already connected to: " + device);
				return Connection_Manager.SUCCESS;
			}

			BluetoothDevice myBtServer = BtAdapter.getRemoteDevice(device);

			BluetoothSocket myBSock = null;

			Log.d(TAG, "Creating Socket");

			for (int i = 0; i < Connection_Manager.MAX_CONNECTIONS_SUPPORTED
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
				return Connection_Manager.FAILURE;
			}

			BtSockets.put(device, myBSock);
			BtConnectedDeviceAddresses.add(device);
			BtStreamWatcher BtStreamWatcherThread = new BtStreamWatcher(device);
			BtStreamWatcherThread.start();
			BtStreamWatcherThreads.put(device, BtStreamWatcherThread);
			return Connection_Manager.SUCCESS;
		} else
			return Connection_Manager.FAILURE;
	}

	/*
	 * Function that returns a connection socket if connection succeeds on the
	 * particular uuid on the myBtServer, i.e, remote device
	 */
	private BluetoothSocket getConnectedSocket(BluetoothDevice myBtServer,
			UUID uuidToTry) {
		BluetoothSocket myBSock;
		try {
			myBSock = myBtServer.createRfcommSocketToServiceRecord(uuidToTry);
			Log.d(TAG,
					"Trying to connect to socket of:" + myBtServer.getAddress()
							+ " with UUID +" + uuidToTry);
			myBSock.connect();
			return myBSock;
		} catch (IOException e) {
			Log.i(TAG,
					"IOException in getConnectedSocket. Msg:" + e.getMessage()
							+ e.getStackTrace());
		}
		return null;
	}

	/*
	 * Function that will broadcast RREQ's. Will first search for all found
	 * devices. Will then connect to all found devices and send an RREQ to them.
	 */
	public void broadcastMessage(String message) throws RemoteException {
		broadcast_msg_queue.add(message);
		startDiscovery();
	}

	public int sendMessageToConnectedDevices(String message) {
		int size = BtConnectedDeviceAddresses.size();
		for (int i = 0; i < size; i++) {
			Log.d(TAG, "Sending msg: " + message + " to:"
					+ BtConnectedDeviceAddresses.get(i));
			try {
				sendMessageToDestination(BtConnectedDeviceAddresses.get(i),
						message);
			} catch (RemoteException e) {
				Log.d(TAG, "Error in sendMsgToDestination:" + e.getMessage());
			}
		}
		return Connection_Manager.SUCCESS;
	}

	// For debugging. To print connections at a certain point of time
	public String getConnections() {

		String connections = "";
		int size = BtConnectedDeviceAddresses.size();
		for (int i = 0; i < size; i++) {
			connections = connections + BtConnectedDeviceAddresses.get(i) + ",";
		}
		return connections;
	}

	/*
	 * Method that actually sends the stream of bytes to a remote device. First
	 * tries to connect. If successful, writes data after fetching its socket
	 */
	public int sendMessageToDestination(String destination, String message)
			throws RemoteException {
		Log.d(TAG,"Unicasting "+message+" to :"+destination);

		int status = connect(destination);

		if (status == Connection_Manager.SUCCESS) {
			try {
				BluetoothSocket myBsock = BtSockets.get(destination);
				if (myBsock != null) {
					Log.d(TAG, "Writing on socket of: " + destination);
					OutputStream outStream = myBsock.getOutputStream();
					byte[] stringAsBytes = (message + " ").getBytes();
					stringAsBytes[stringAsBytes.length - 1] = 0; // Add a stop
					// marker
					outStream.write(stringAsBytes);
					return Connection_Manager.SUCCESS;
				}
			} catch (IOException e) {
				Log.i(TAG, "IOException in sendMessage - Dest:" + destination
						+ ", Msg:" + message, e);
			}
		}
		return Connection_Manager.FAILURE;
	}

	public String getSelfAddress() throws RemoteException {
		return BtAdapter.getAddress();
	}

	public BluetoothAdapter getBluetoothAdapter() {
		return BtAdapter;
	}

	//Function returning devices that are paired
	public HashMap<String, String> getPairedDevices() {

		Set<BluetoothDevice> devices = BtAdapter.getBondedDevices();
		for (BluetoothDevice device : devices) {
			BtBondedDevices.put(device.getAddress(), device.getName());
		}

		return BtBondedDevices;
	}
	
	//Function returning devices that are found
	public HashMap<String, String> getFoundDevices() {
		return BtFoundDevices;
	}

	/* Function that will make connections to only the devices which are found.
	 * They have to be discoverable and within range
	 */
	public void connectToFoundDevices() {

		if (server_isRunning) {

			Log.d(TAG, "Connecting to "+BtFoundDevices.size()+" found devices. Last Search :" + lastDiscovery);
			Iterator<Map.Entry<String, String>> devices = BtFoundDevices
					.entrySet().iterator();
			while (devices.hasNext()) {
				Map.Entry<String, String> device = (Map.Entry<String, String>) devices
						.next();
				try {
					Log.d(TAG, "Start checking isMyFriend...");
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
	}

	/* Function that is called to make the device discoverable. 
	 * Gets maximum time depending on device.
	 */
	public void makeDeviceDisocverable() {
		Log.d(TAG, "Making Device Discoverable.");
		Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		bluetooth_manager.startActivity(i);
	}

	/* This function takes the MAC address of the device and tries to find if
	 * this application is running on the device with the given MAC address. It
	 * tries to connect to the device, if connection is successful then we can
	 * conclude that the application is running on the device.
	 */
	public boolean isMyFriend(String address) {

		return true;

		// boolean is_my_friend = false;
		//
		// Log.d(TAG, "Checking if " + address + " is my friend");
		//
		// BluetoothDevice myBtServer = BtAdapter.getRemoteDevice(address);
		//
		// BluetoothSocket myBSock = null;
		//
		// /*
		// * Try this for three times since there might be other nodes which are
		// * trying to connect to the same device.
		// */
		// for (int i = 0; i < 3; i++) {
		//
		// Log.d(TAG, " Trying to create Socket to see if " + address
		// + "is my friend");
		// myBSock = getConnectedSocket(myBtServer, friend_uuid);
		// Log.d(TAG, "After getConnectedSocket(): in friend " + myBSock);
		// if (myBSock == null) {
		// try {
		// Thread.sleep(25);
		// } catch (InterruptedException e) {
		// Log.e(TAG, "InterruptedException in connect", e);
		// }
		// } else {
		// is_my_friend = true;
		// try {
		// myBSock.close();
		// } catch (IOException e) {
		// Log.d(TAG,
		// "Error while closing friend socket. "
		// + e.getMessage());
		// }
		// break;
		// }
		// }
		// Log.d(TAG, "Returning " + is_my_friend + "from isMyFriend()");
		// return is_my_friend;
	}

	/*
	// * Start the server which listens for connection which is used to
	// determine
	// * if the other node has our application running or not.
	// */
	// public void startFriendServer() {
	// friendServer=new Thread(new FriendServer());
	// friendServer.start();
	// }

	/* Function to be called when Application starts and later when Bluetooth is
	 * turned on. Instantiates the lists, checks if Adapter is enabled and
	 * starts the server thread
	 */
	public int startRadio() {

		if (server_isRunning) {
			return Connection_Manager.FAILURE;
		}
		
		BtSockets = new HashMap<String, BluetoothSocket>();
		
		BtConnectedDeviceAddresses = new ArrayList<String>();

		BtBondedDevices = new HashMap<String, String>();

		BtFoundDevices = new HashMap<String, String>();

		BtStreamWatcherThreads = new HashMap<String, BtStreamWatcher>();

		broadcast_msg_queue = new LinkedList<String>();

		bluetooth_manager.route_table = new RouteTable(bluetooth_manager);
		
		BtAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if (BtAdapter != null) {
			BtAdapter.enable();
		}
		
		if (BtAdapter.isEnabled()) {

			if (server_thread == null) {
				server_thread = new Thread(new ConnectionWaiter());
				server_thread.start();
				server_isRunning = true;
				Log.d(TAG, "++Listening thread Started++");
			} else
				Log.d(TAG, "Server already Listening");

			Log.d(TAG,"++Starting Routing thread++");
			bluetooth_manager.routing_thread = new PacketHandlerService();
			bluetooth_manager.routing_thread.start();
			// friendServer= new Thread(new FriendServer());
			// friendServer.start();
			Log.d(TAG, " ++ Server Started ++");
			return Connection_Manager.SUCCESS;
		}
		
		return Connection_Manager.FAILURE;

	}

	/*
	 * Function called when Bluetooth will be turned off. Stops the thread which
	 * listens for other connections. Removes sockets and other lists for GC and
	 * make the Routing thread wait till it is started again
	 */
	public int stopRadio() {
		if (server_isRunning) {
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
				server_thread = null;
				server_isRunning = false;
				bluetooth_manager.route_table = null;
				//friendServer.interrupt();
				//friendServer = null;
				bluetooth_manager.routing_thread.interrupt();
				bluetooth_manager.routing_thread = null;
				Log.d(TAG, " ++ Server Stopped ++");

			} catch (IOException e) {
				Log.d(TAG, "Error in stopRadio(). " + e.getMessage());
			}
			return Connection_Manager.SUCCESS;
		} else {
			return Connection_Manager.FAILURE;
		}

	}
	
	/* Function that will dispatch all broadcast messages waiting in 
	 * the queue to the devices it has found. 
	 */
	public void dispatchBroadcastQueue(){
		Log.d(TAG,"Value of flag:"+is_req_from_gui);
		
		if (!is_req_from_gui) {
			connectToFoundDevices();
			Iterator<String> broadcast_msg_itr = broadcast_msg_queue
					.iterator();
			while (broadcast_msg_itr.hasNext()) {
				String message = broadcast_msg_itr.next();
				sendMessageToConnectedDevices(message);
				broadcast_msg_itr.remove();
			}
		} else {
			is_req_from_gui = false;
		}
	}

	// Send a message from radio to routing
	private void communicateFromRadioToRouting(String address, String message) {
		String ACTION = bluetooth_manager.getResources().getString(
				R.string.RADIO_TO_ROUTING);
		Intent i = new Intent();
		i.setAction(ACTION);
		i.putExtra("device", address);
		i.putExtra("msg", message);
		bluetooth_manager.sendBroadcast(i);
		Log.d(TAG, "Intent Send from Radio to routing");
	}
	
	/*
	 * FriendServer that listens for friendly connections. This mechanism is
	 * just used to check if the node is running our application.
	 
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
	}*/

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
				
				// if its a mobile phone, put it on the foundDevices list
				if (bc.getMajorDeviceClass() == BluetoothClass.Device.Major.PHONE) {
					BtFoundDevices.put(device.getAddress(), device.getName());
				}
				Log.d(TAG, "Found " + device.getAddress());

			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {

				isSearching = false;
				lastDiscovery = System.currentTimeMillis() / 1000;
				Intent discoveryFinished= new Intent(bluetooth_manager.getResources().getString(R.string.DISCOVERY_COMPLETE));
				bluetooth_manager.sendBroadcast(discoveryFinished);
				Log.d(TAG, "Service Discovery Finished !");
				
				//Discovery is finished, now send all messages waiting to be broadcast
				dispatchBroadcastQueue();
				
			} else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				Log.d(TAG, "Bluetooth State Changed");
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
						BluetoothAdapter.ERROR);
				Log.d(TAG, "State: " + state);
				if (state == BluetoothAdapter.STATE_TURNING_OFF) {
					Log.d(TAG, "Bluetooth is turning Off, stopping radio");
					stopRadio();
				} else {
					if (state == BluetoothAdapter.STATE_ON) {
						Log.d(TAG, "Bluetooth is on, starting radio");
						startRadio();
					}
				}
			}
		}
	};

	/* This class is responsible for listening for new connections. Once a
	 * connections is accepted, a new thread is created to manage the I/O
	 * with the newly established connection
	 */
	private class ConnectionWaiter implements Runnable {

		public void run() {
			try {
				for (int i = 0;; i++) {
					BluetoothServerSocket myServerSocket = BtAdapter
							.listenUsingRfcommWithServiceRecord(service_name,
									Uuids.get(i));
					Log.d(TAG, "Listening for UUID : " + Uuids.get(i));
					BluetoothSocket myBSock = myServerSocket.accept();
					myServerSocket.close(); // Close the socket now that the
											// connection has been made.

					String address = myBSock.getRemoteDevice().getAddress();
					// String name = myBSock.getRemoteDevice().getName();

					BtSockets.put(address, myBSock);
					BtConnectedDeviceAddresses.add(address);
					BtStreamWatcher BtStreamWatcherThread = new BtStreamWatcher(
							address);
					BtStreamWatcherThread.start();
					BtStreamWatcherThreads.put(address, BtStreamWatcherThread);
					if (i == Connection_Manager.MAX_CONNECTIONS_SUPPORTED - 1) {
						i = 0;
					}
				}

			} catch (IOException e) {
				Log.i(TAG, "IOException in ConnectionService:ConnectionWaiter",
						e);
			}
		}
	}

	// Thread which maintains the I/O of one stream for one device
	
	private class BtStreamWatcher extends Thread {
		private String address;
		private String TAG;
		private long lastReceived = 0;

		public long getLastReceived() {
			return lastReceived;
		}
		
		public void setLastReceived(long time){
			this.lastReceived=time;
		}

		public BtStreamWatcher(String deviceAddress) {
			address = deviceAddress;
			TAG="StreamWatcher for "+address;
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
						bluetooth_manager.ui_handler.obtainMessage(1,
								"Received " + message + " from: " + address)
								.sendToTarget();
						Log.d(TAG, "Received packet " + message + " from " + address);
						lastReceived = System.currentTimeMillis() / 1000;
						communicateFromRadioToRouting(address, message);
					}
				}
			}
			catch (IOException e) {
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

	/*
	 * Thread that will destroy all the threads on which there is no
	 * communication since the last minute. It will check the lastReceived value
	 * of the StreamWatcher Compare it with current time. If more than a minute,
	 * kill the thread and remove the socket. Also makes the device discoverable
	 * if server is running
	 */
	private class gc_thread extends Thread {

		Iterator<Map.Entry<String, BtStreamWatcher>> it;

		String TAG="Maintenance Thread";
		public void run() {
			long time;
			while (true) {
				if (server_isRunning) {
					if (bluetooth_manager.connection_manager.getBluetoothAdapter()
							.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {

						bluetooth_manager.connection_manager.makeDeviceDisocverable();
					}

					Log.d(TAG, "Connections are :" + getConnections());
					it = BtStreamWatcherThreads.entrySet().iterator();
					Log.d(TAG, "Doing Maintainence");
					try {
						while (it.hasNext()) {
							Map.Entry<String, BtStreamWatcher> pairs = (Map.Entry<String, BtStreamWatcher>) it
									.next();
							time = ((BtStreamWatcher) pairs.getValue())
									.getLastReceived();
							if(time==0)
							{
								time=System.currentTimeMillis()/1000;
								((BtStreamWatcher) pairs.getValue()).setLastReceived(time);
							}
							Log.d(TAG,"Thread Duration in seconds:"+(System.currentTimeMillis() / 1000 - time));
							if (System.currentTimeMillis() / 1000 - time > 300) {
								String address = (String) pairs.getKey();
								BtStreamWatcher listener = (BtStreamWatcher) pairs
										.getValue();
								BluetoothSocket myBtSocket = BtSockets
										.get(address);
								myBtSocket.getInputStream().close();
								myBtSocket.getOutputStream().close();
								myBtSocket.close();
								it.remove();
								listener.interrupt();
								listener = null;
								BtSockets.remove(address);
								BtConnectedDeviceAddresses.remove(address);
								Log.d(TAG, "Disconnected " + address);
								bluetooth_manager.route_table
										.removeRouteToDest(address);
							}
						}
						Thread.sleep(30000);
					} catch (IOException e) {
						Log.d(TAG, "Failed to close socket" + e.getMessage());
					} catch (InterruptedException e) {
						Log.d(TAG, "Garbage Collection Thread Interrupted!!");
					}
				}
			}
		}
	}

	public String getSelfName() {
		return BtAdapter.getName();
	}
}
