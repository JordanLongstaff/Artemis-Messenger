package artemis.messenger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.walkertribe.ian.protocol.udp.Server;
import com.walkertribe.ian.protocol.udp.ServerDiscoveryRequester;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;

/**
 * The Connect activity opens when the Connect button is pressed or when the app starts, if
 * UDP broadcasts are enabled. When the activity is opened, or the "Find servers" option is
 * pressed, the app broadcasts a server discovery request and listens for acknowledgements.
 * @author jrdnl
 */
public class ConnectActivity extends Activity implements ServerDiscoveryRequester.Listener {
	// Server list/view fields
	private SimpleAdapter connectAdapter, serverAdapter, recentAdapter;
	private ArrayList<HashMap<String, String>> serverList;	
	private ArrayList<HashMap<String, String>> optionsList;
	private ArrayList<HashMap<String, String>> recentList;
	private ListView connectMenuView, serverListView, recentListView;
	
	// Other fields
	private ServerDiscoveryRequester requester;
	private WifiLock lock;
	private String address;
	private boolean scanning;
	private int scanIndex;
	private int timeout;
	
	// Adapter key-value pairs
	private static final String[] keys = new String[] { "name", "address" };
	private static final int[] views = new int[] { R.id.serverNameField, R.id.serverAddressField };
	
	// Runnables
	private Runnable updateConnectMenu = new Runnable() {
		@Override
		public void run() {
			connectAdapter.notifyDataSetChanged();
		}
	};
	
	private Runnable updateServerList = new Runnable() {
		@Override
		public void run() {
			serverAdapter.notifyDataSetChanged();
		}
	};
	
	@Override
	public void onBackPressed() {
		// Back button pressed, return to main activity
		Intent resultIntent = new Intent();
		setResult(Activity.RESULT_CANCELED, resultIntent);
		finish();
	}
	
	// Click listener for main menu items
	private final OnItemClickListener connectClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
			if (address.equals("")) pos++;
			
			switch (pos) {
			case 0:
				Intent resultIntent = new Intent();
				resultIntent.putExtra("Address", address);
				resultIntent.putExtra("Error", true);
				setResult(Activity.RESULT_OK, resultIntent);
				finish();
				break;
			case 1:
				if (!scanning) findServers();
				break;
			case 2:
				Intent helpIntent = new Intent(getApplicationContext(), HelpActivity.class);
				startActivity(helpIntent);
				break;
			}
		}
	};
	
	// Click listener for discovered server items
	private final OnItemClickListener serverClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
			address = serverList.get(pos).get(keys[1]);
			if (address.isEmpty()) address = serverList.get(pos).get(keys[0]);
			
			Intent resultIntent = new Intent();
			resultIntent.putExtra("Address", address);
			setResult(Activity.RESULT_OK, resultIntent);
			finish();
		}
	};
	
	// Click listener for recent server items
	private final OnItemClickListener recentClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
			address = recentList.get(pos).get(keys[0]);
			
			Intent resultIntent = new Intent();
			resultIntent.putExtra("Address", address);
			setResult(Activity.RESULT_OK, resultIntent);
			finish();
		}
	};
	
	@Override
	public void onDiscovered(Server server) {
		// Get data from discovered server
		String serverIP = server.getIp();
		String hostName = server.getHostName();
		
		// If server is already in the list, do nothing
		for (HashMap<String, String> entry: serverList) {
			if (entry.get(keys[0]).equals(hostName) && entry.get(keys[1]).equals(serverIP))
				return;
		}

		// Add server to list
		HashMap<String, String> serverEntry = new HashMap<String, String>(keys.length);
		serverEntry.put(keys[0], hostName);
		serverEntry.put(keys[1], serverIP);
		serverList.add(serverEntry);
		serverListView.post(updateServerList);
	}
	
	@Override
	public void onQuit() {
		// Release WiFi lock
		if (lock != null) lock.release();
		
		// Reset scan option
		HashMap<String, String> scanOption = optionsList.get(scanIndex);
		scanOption.put(keys[0], getString(R.string.scanServers));
		connectMenuView.post(updateConnectMenu);
		scanning = false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_connect);
		
		// Get timeout for server discovery
		Intent intent = getIntent();
		timeout = Integer.parseInt(intent.getStringExtra("Timeout") + "000");
		
		// Initialize lists of menu items
		serverList = new ArrayList<HashMap<String, String>>();
		optionsList = new ArrayList<HashMap<String, String>>();
		recentList = new ArrayList<HashMap<String, String>>();

		// Initialize adapters for the two menus
		serverAdapter =
				new SimpleAdapter(getApplicationContext(), serverList, R.layout.server_list_entry, keys, views);
		connectAdapter =
				new SimpleAdapter(getApplicationContext(), optionsList, R.layout.server_list_entry, keys, views);
		recentAdapter =
				new SimpleAdapter(getApplicationContext(), recentList, R.layout.server_list_entry, keys, views);
		
		// If there was a default address, add that connection as an option
		address = intent.getStringExtra("URL");
		if (!address.equals("")) {
			HashMap<String, String> connectOption = new HashMap<String, String>();
			connectOption.put(keys[0], "Connect to " + address);
			optionsList.add(connectOption);
		}
		
		scanIndex = optionsList.size();
		
		// Add option to start scanning for servers
		HashMap<String, String> scanOption = new HashMap<String, String>();
		scanOption.put(keys[0], getString(R.string.scanServers));
		optionsList.add(scanOption);
		
		// Add option to open Help topics
		HashMap<String, String> helpOption = new HashMap<String, String>();
		helpOption.put(keys[0], getString(R.string.helpButton));
		optionsList.add(helpOption);
		
		// Get list of recent servers
		String recentHosts = intent.getStringExtra("Recent");
		if (!recentHosts.isEmpty()) {
			for (String h: recentHosts.split("\\*")) {
				HashMap<String, String> recent = new HashMap<String, String>();
				recent.put(keys[0], h);
				recentList.add(recent);
			}
		}
		
		// Set up the menu adapters
		connectMenuView = (ListView) findViewById(R.id.connectList);
		connectMenuView.setAdapter(connectAdapter);
		serverListView = (ListView) findViewById(R.id.serverList);
		serverListView.setAdapter(serverAdapter);
		recentListView = (ListView) findViewById(R.id.recentList);
		recentListView.setAdapter(recentAdapter);
		
		// Set up the headers with grey backgrounds
		LinearLayout connectHeader = (LinearLayout) findViewById(R.id.connectHeaderBar);
		LinearLayout serverHeader = (LinearLayout) findViewById(R.id.serverHeaderBar);
		LinearLayout recentHeader = (LinearLayout) findViewById(R.id.recentHeaderBar);

		int bgcolor = Color.parseColor("#707070");
		connectHeader.setBackgroundColor(bgcolor);
		serverHeader.setBackgroundColor(bgcolor);
		recentHeader.setBackgroundColor(bgcolor);
		
		// Set up the click listeners
		connectMenuView.setOnItemClickListener(connectClickListener);
		serverListView.setOnItemClickListener(serverClickListener);
		recentListView.setOnItemClickListener(recentClickListener);
		
		// Start scanning immediately
		findServers();
	}
	
	private void findServers() {
		// Empty the list of servers to connect to
		while (!serverList.isEmpty()) {
			serverList.remove(0).clear();
		}
		
		// Update scan option to make it unselectable again
		HashMap<String, String> scanOption = optionsList.get(scanIndex);
		scanOption.put(keys[0], getString(R.string.scanning));
		connectAdapter.notifyDataSetChanged();
		scanning = true;
		
		if (requester == null) {
			// Initialize server discovery requester
			try { requester = new ServerDiscoveryRequester(this, timeout); }
			catch (IOException e) {
				Intent resultIntent = new Intent();
				resultIntent.putExtra("Address", address);
				setResult(Activity.RESULT_OK, resultIntent);
				finish();
				return;
			}
		}
		
		// Acquire WiFi lock to allow ACKs to be received
		WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
		lock = manager.createWifiLock("artemis.messenger");
		lock.acquire();
		
		// Start scanning
		new Thread(requester).start();
	}
}