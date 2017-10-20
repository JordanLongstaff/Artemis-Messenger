package artemis.messenger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.walkertribe.ian.enums.BaseMessage;
import com.walkertribe.ian.enums.Console;
import com.walkertribe.ian.enums.ObjectType;
import com.walkertribe.ian.enums.OrdnanceType;
import com.walkertribe.ian.enums.OtherMessage;
import com.walkertribe.ian.iface.ArtemisNetworkInterface;
import com.walkertribe.ian.iface.ConnectionSuccessEvent;
import com.walkertribe.ian.iface.DisconnectEvent;
import com.walkertribe.ian.iface.Listener;
import com.walkertribe.ian.iface.ThreadedArtemisNetworkInterface;
import com.walkertribe.ian.protocol.core.GameOverReasonPacket;
import com.walkertribe.ian.protocol.core.PausePacket;
import com.walkertribe.ian.protocol.core.comm.CommsIncomingPacket;
import com.walkertribe.ian.protocol.core.comm.CommsOutgoingPacket;
import com.walkertribe.ian.protocol.core.setup.AllShipSettingsPacket;
import com.walkertribe.ian.protocol.core.setup.ReadyPacket;
import com.walkertribe.ian.protocol.core.setup.SetConsolePacket;
import com.walkertribe.ian.protocol.core.setup.SetShipPacket;
import com.walkertribe.ian.protocol.core.world.DestroyObjectPacket;
import com.walkertribe.ian.protocol.core.world.ObjectUpdatePacket;
import com.walkertribe.ian.vesseldata.FilePathResolver;
import com.walkertribe.ian.vesseldata.PathResolver;
import com.walkertribe.ian.vesseldata.VesselAttribute;
import com.walkertribe.ian.world.Artemis;
import com.walkertribe.ian.world.ArtemisBase;
import com.walkertribe.ian.world.ArtemisNpc;
import com.walkertribe.ian.world.ArtemisObject;
import com.walkertribe.ian.world.ArtemisPlayer;
import com.walkertribe.ian.world.BaseArtemisShielded;
import com.walkertribe.ian.world.SystemManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import artemis.messenger.R;

/**
 * The app's main activity.
 * @author Jordan Longstaff
 */
public class ListActivity extends Activity implements OnSharedPreferenceChangeListener {
	// All app fields
	private com.walkertribe.ian.Context context;
	private ArtemisNetworkInterface server;
	private SystemManager manager;
	private CommsService service;
	private ServiceConnection connection;
	private String host, dockingStation;
	private int playerShip;
	private LinearLayout missionsTable, alliesTable, stationsTable, routeTable;
	private TableLayout alliesView, stationsView, missionsView, routeView;
	private boolean updateRunning, routing;
	private Handler updateHandler, stationHandler, routeHandler, serviceHandler, buttonHandler;
	private CopyOnWriteArrayList<SideMissionRow> missions;
	private ConcurrentHashMap<String, CopyOnWriteArrayList<SideMissionRow>> closed;
	private ConcurrentHashMap<String, ConcurrentHashMap<String, StationStatusRow>> bases;
	private ConcurrentHashMap<String, ConcurrentHashMap<String, AllyStatusRow>> allies;
	private ConcurrentHashMap<String, CopyOnWriteArrayList<String>> rogues;
	private ArrayList<CommsIncomingPacket> inPackets;
	private ArrayList<CommsOutgoingPacket> outPackets;
	private boolean assetsFail;
	private boolean gameRunning;
	private RoutingGraph graph;
	private Toast toast;
	private boolean missionFlash, allyFlash, stationFlash;
	private long startTime;
	
	// Constants
	private static final String dataFile = "server.dat";
	private static final int updateInterval = 100;
	private static final int timeout = 9000;
	private static final int heartbeatTimeout = 2000;
	private static final int flashTime = 500;
	
	// Static font for public access
	public static Typeface APP_FONT;
	
	/*
	 * (non-Javadoc)
	 * This method is called when the back button is pressed. The back button closes the app
	 * following a confirmation from the user if the app is connected to a server.
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		// If no server, just exit
		if (server == null || !server.isConnected()) finish();
		
		// Otherwise, tie to an alert dialog
		else {
			new AlertDialog.Builder(this)
	        	.setMessage("You are still connected to a server. Are you sure you want to exit?")
	        	.setCancelable(false)
	        	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	        		public void onClick(DialogInterface dialog, int id) {
	            	   if (server != null) server.stop();
	            	   host = null;
	                   ListActivity.this.finish();
	        		}
	        	})
	        	.setNegativeButton("No", null)
	        	.show();
		}
	}

	@SuppressLint("ShowToast")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Initialize layout
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_list);
		
		// Initialize table and view fields
		missionsTable = (LinearLayout) findViewById(R.id.missionTableRows);
		missionsView = (TableLayout) findViewById(R.id.sideMissionsTable);
		alliesTable = (LinearLayout) findViewById(R.id.allyTableRows);
		alliesView = (TableLayout) findViewById(R.id.alliesView);
		stationsTable = (LinearLayout) findViewById(R.id.stationTableRows);
		stationsView = (TableLayout) findViewById(R.id.stationsView);
		routeTable = (LinearLayout) findViewById(R.id.routeTableRows);
		routeView = (TableLayout) findViewById(R.id.routeView);
		
		// Initialize data tables
		missions = new CopyOnWriteArrayList<SideMissionRow>();
		closed = new ConcurrentHashMap<String, CopyOnWriteArrayList<SideMissionRow>>();
		allies = new ConcurrentHashMap<String, ConcurrentHashMap<String, AllyStatusRow>>();
		bases = new ConcurrentHashMap<String, ConcurrentHashMap<String, StationStatusRow>>();
		rogues = new ConcurrentHashMap<String, CopyOnWriteArrayList<String>>();
		inPackets = new ArrayList<CommsIncomingPacket>();
		outPackets = new ArrayList<CommsOutgoingPacket>();
		
		// Hide all views except missions
		alliesView.setVisibility(View.GONE);
		stationsView.setVisibility(View.GONE);
		routeView.setVisibility(View.GONE);
		
		// Initialize handlers
		updateHandler = new Handler();
		stationHandler = new Handler();
		routeHandler = new Handler();
		serviceHandler = new Handler();
		buttonHandler = new Handler();
		
		// Initialize app font
		if (APP_FONT == null) APP_FONT = Typeface.createFromAsset(getAssets(), "fonts/Rajdhani-Medium.ttf");
		
		// Initialize address field
		final EditText addressField = (EditText) findViewById(R.id.addressField);
		
		// Initialize service connection
		connection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName className, IBinder binder) {
				CommsService.ArtemisBinder amBinder = (CommsService.ArtemisBinder) binder;
				service = amBinder.getService();
				serviceHandler.post(updateService);
			}

			@Override
			public void onServiceDisconnected(ComponentName className) {
				service = null;
				unbindService(this);
			}
		};
		
		// Attempt to load previous address from data file
		try {
			FileInputStream fis = openFileInput(dataFile);
			String ip = "";
			while (true) {
				int read = fis.read();
				if (read < 0) break;
				ip += (char) read;
			}
			addressField.setText(ip);
			fis.close();
		} catch (IOException e) { }
		
		// Initialize ship selection components
		Spinner shipSpinner = (Spinner) findViewById(R.id.shipSpinner);
		shipSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				// Wait for server
				while (server == null);
				
				// Set up Comms and Main Screen connection from selected ship
				playerShip = position + 1;
				server.send(new SetShipPacket(position + 1));
				server.send(new SetConsolePacket(Console.COMMUNICATIONS, true));
				server.send(new SetConsolePacket(Console.MAIN_SCREEN, true));
				
				// Update docking status
				if (dockingStation != null) resetDockedRow();
				if (ListActivity.this.manager.getPlayerShip(playerShip) == null) return;
				for (ArtemisObject o: ListActivity.this.manager.getObjects(ObjectType.BASE)) {
					ArtemisBase base = (ArtemisBase) o;
					if (base.getId() == ListActivity.this.manager.getPlayerShip(playerShip).getDockingBase()) {
						dockingStation = base.getName();
						updateDockedRow();
						stationHandler.postDelayed(waitForUndock, updateInterval);
						break;
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) { }
			
		});
		
		// Initialize view buttons
		final Button allyViewButton = (Button) findViewById(R.id.allyViewButton);
		final Button missionViewButton = (Button) findViewById(R.id.missionViewButton);
		final Button stationViewButton = (Button) findViewById(R.id.stationViewButton);
		final Button routeViewButton = (Button) findViewById(R.id.routeViewButton);
		
		// Set on click listeners for each button
		allyViewButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				alliesView.setVisibility(View.VISIBLE);
				missionsView.setVisibility(View.GONE);
				stationsView.setVisibility(View.GONE);
				routeView.setVisibility(View.GONE);
			}
		});
		
		missionViewButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				alliesView.setVisibility(View.GONE);
				missionsView.setVisibility(View.VISIBLE);
				stationsView.setVisibility(View.GONE);
				routeView.setVisibility(View.GONE);
				missionFlash = false;
			}
		});
		
		stationViewButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				alliesView.setVisibility(View.GONE);
				missionsView.setVisibility(View.GONE);
				stationsView.setVisibility(View.VISIBLE);
				routeView.setVisibility(View.GONE);
			}
		});
		
		routeViewButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				alliesView.setVisibility(View.GONE);
				missionsView.setVisibility(View.GONE);
				stationsView.setVisibility(View.GONE);
				routeView.setVisibility(View.VISIBLE);
			}
		});
		
		// Start button handler
		buttonHandler.post(updateButtons);
		
		// Initialize connect, settings and help buttons
		ImageButton connectButton = (ImageButton) findViewById(R.id.connectButton);
		connectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) { createConnection(); }
		});
		
		ImageButton settingsButton = (ImageButton) findViewById(R.id.settingsButton);
		settingsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent settingsIntent = new Intent(ListActivity.this, SettingsActivity.class);
				startActivity(settingsIntent);
			}
		});
		
		ImageButton helpButton = (ImageButton) findViewById(R.id.helpButton);
		helpButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent helpIntent = new Intent(ListActivity.this, HelpActivity.class);
				startActivity(helpIntent);
			}
		});
		
		ImageButton closeButton = (ImageButton) findViewById(R.id.closeButton);
		closeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		// Set up address editor
		addressField.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId != EditorInfo.IME_ACTION_DONE) return false;
				createConnection();
				return true;
			}
		});
		
		// Update app font
		LinearLayout appLayout = (LinearLayout) findViewById(R.id.appLayout);
		updateFont(appLayout);
		
		// Start notification cleanup service
		startService(new Intent(this, NotificationCleanupService.class));
		
		// Set up preference change listener
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		preferences.registerOnSharedPreferenceChangeListener(this);
		
		// Initialize preferences
		PreferenceManager.setDefaultValues(this, R.xml.preference, false);
		
		// Start up help activity if set
		if (preferences.getBoolean(getString(R.string.helpStartupKey), true)) {
			Intent helpIntent = new Intent(this, HelpActivity.class);
			startActivity(helpIntent);
		}
		
		// Initialize Toast message display
		toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
	}
	
	// Set all views in app layout to have the same font
	private void updateFont(ViewGroup viewGroup) {
		for (int i = 0; i < viewGroup.getChildCount(); i++) {
			View child = viewGroup.getChildAt(i);
			if (child instanceof ViewGroup) {
				updateFont((ViewGroup) child);
			} else if (child instanceof TextView) {
				TextView tv = (TextView) child;
				tv.setTypeface(APP_FONT);
			}
		}
	}
	
	/**
	 * Sets up a connection to a running Artemis server.
	 */
	private void createConnection() {
		// If a server was running, stop it
		if (server != null) {
			server.stop();
			server = null;
			inPackets.clear();
		}
		
		// Initialize important components required for runnables
		final EditText addressField = (EditText) findViewById(R.id.addressField);
		final LinearLayout addressRow = (LinearLayout) findViewById(R.id.addressRow);
		final Spinner shipSpinner = (Spinner) findViewById(R.id.shipSpinner);
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.ship_name_spinner);
		
		// Get URL
		final String url = addressField.getText().toString();
		
		// If one was already in use, clear tables and prep for new connection
		if (host != null) {
			clearAllTables();
		}
		
		// Find selected vesselData.xml location
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		int resolverIndex = Integer.parseInt(pref.getString(getString(R.string.vesselDataKey), "0"));
		PathResolver resolver;
		
		if (resolverIndex == 0) {
			// If it's the app assets, point there
			resolver = new AssetsResolver(getAssets());
		} else {
			// Otherwise, point to location and unpack from assets if necessary
			toast.setText("Unpacking assets...");
			toast.setDuration(Toast.LENGTH_SHORT);
			toast.show();
			String filesDir;
			
			// Point to either internal or external storage
			if (resolverIndex == 2) filesDir = Environment.getExternalStorageDirectory() + "/artemis/dat";
			else filesDir = getFilesDir() + "/artemis/dat";
			
			// Create path if it doesn't already exist
			File datDir = new File(filesDir);
			if (!datDir.exists()) datDir.mkdirs();
			
			// Point to assets
			AssetManager am = getAssets();
			try {
				for (String in: am.list("dat")) {
					// Copy each file to storage if it doesn't already exist
					File outFile = new File(filesDir + "/" + in);
					if (outFile.exists()) continue;
					InputStream inStream = am.open("dat/" + in);
					FileOutputStream outStream = new FileOutputStream(outFile);
					byte[] bytes = new byte[inStream.available()];
					inStream.read(bytes);
					outStream.write(bytes);
					inStream.close();
					outStream.close();
				}
				resolver = new FilePathResolver(filesDir.split("/dat")[0]);
			} catch (IOException e) {
				// If something went wrong, fall back on assets
				assetsFail = true;
				toast.setText("Failed to unpack assets, switching location to Default...");
				toast.setDuration(Toast.LENGTH_SHORT);
				toast.show();
				pref.edit().putString(getString(R.string.vesselDataKey), "0").commit();
				resolver = new AssetsResolver(getAssets());
			}
		}
		
		context = new com.walkertribe.ian.Context(resolver);
		
		toast.setText("Connecting to " + url + "...");
		toast.setDuration(Toast.LENGTH_LONG);
		toast.show();
		
		// Try setting up a connection
		new Thread(new Runnable() {
			@Override public void run() {
				try {
					server = new ThreadedArtemisNetworkInterface(url, 2010, timeout, context);
					server.addListener(ListActivity.this);
					
					manager = new SystemManager(context);
					server.addListener(manager);
					
					server.start();
				} catch (IOException e) {
					toast.setDuration(Toast.LENGTH_LONG);
					toast.setText("Connection failed");
					toast.show();
					addressRow.post(new Runnable() {
						@Override
						public void run() {
							addressRow.setBackgroundColor(Color.parseColor("#c00000"));
							shipSpinner.setAdapter(adapter);
						}
					});
				} catch (NullPointerException e) {
				}
			}
		}).start();
		addressRow.post(new Runnable() {
			@Override
			public void run() {
				addressRow.setBackgroundColor(Color.parseColor("#c55a11"));
			}
		});
	}
	
	/**
	 * Called when a connection attempt succeeds.
	 * @param e the connection success event
	 */
	@Listener
	public void onConnectSuccess(ConnectionSuccessEvent e) {
		// Connect to Comms
		server.send(new SetConsolePacket(Console.COMMUNICATIONS, true));
		
		// Also need to connect to Main Screen to receive proper game over signal
		server.send(new SetConsolePacket(Console.MAIN_SCREEN, true));
		server.send(new ReadyPacket());
		
		try {
			// Write URL to data
			FileOutputStream fos = openFileOutput(dataFile, Context.MODE_PRIVATE);
			EditText addressField = (EditText) findViewById(R.id.addressField);
			host = addressField.getText().toString();
			fos.write(host.getBytes());
			fos.close();
		} catch (IOException ex) { }

		final LinearLayout addressRow = (LinearLayout) findViewById(R.id.addressRow);
		addressRow.post(new Runnable() {
			@Override
			public void run() {
				addressRow.setBackgroundColor(Color.parseColor("#008000"));
			}
		});
		updateMissionsTable();
		updateStationsTable();
		updateAlliesTable();
		
		// Clear all current server data
		missions.clear();
		for (CopyOnWriteArrayList<SideMissionRow> list: closed.values()) list.clear();
		for (ConcurrentHashMap<String, StationStatusRow> map: bases.values()) map.clear();
		for (ConcurrentHashMap<String, AllyStatusRow> map: allies.values()) map.clear();
		for (CopyOnWriteArrayList<String> list: rogues.values()) list.clear();
		closed.clear();
		bases.clear();
		allies.clear();
		rogues.clear();
		
		// Set start time
		startTime = new Date().getTime();

		// Start packet handler
		new AsyncTask<String, String, String>() {

			@Override
			protected String doInBackground(String... params) {
				handlePackets();
				return null;
			}
			
		}.execute();
	}
	
	/**
	 * Called when the client disconnects from the server.
	 * @param event the disconnect event
	 */
	@Listener
	public void onDisconnect(DisconnectEvent event) {
		// If it was on purpose, do nothing
		if (event.getCause() == DisconnectEvent.Cause.LOCAL_DISCONNECT) return;
		
		// Shut down packet handler
		inPackets.clear();
		outPackets.clear();
		
		// Shut down connection
		if (service != null) service.onDisconnect(event);
		else server.stop();
		server = null;
		host = null;
		
		// Clear all fields
		final LinearLayout addressRow = (LinearLayout) findViewById(R.id.addressRow);
		final Spinner shipSpinner = (Spinner) findViewById(R.id.shipSpinner);
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.ship_name_spinner);
		
		// Stop all handlers
		updateHandler.removeCallbacks(updateEntities);
		updateRunning = false;
		dockingStation = null;
		routeHandler.removeCallbacks(updateRoute);
		routing = false;
		
		// Stop buttons flashing
		missionFlash = false;
		allyFlash = false;
		stationFlash = false;
		
		// Clean up Routing graph
		graph = null;
		
		// Clear server data
		missions.clear();
		for (CopyOnWriteArrayList<SideMissionRow> list: closed.values()) list.clear();
		for (ConcurrentHashMap<String, StationStatusRow> map: bases.values()) map.clear();
		for (ConcurrentHashMap<String, AllyStatusRow> map: allies.values()) map.clear();
		for (CopyOnWriteArrayList<String> list: rogues.values()) list.clear();
		closed.clear();
		bases.clear();
		allies.clear();
		rogues.clear();
		clearAllTables();
		addressRow.post(new Runnable() {
			@Override
			public void run() {
				shipSpinner.setAdapter(adapter);
				addressRow.setBackgroundColor(Color.parseColor("#c00000"));
			}
		});
	}
	
	/**
	 * Called when a game over reason packet is received by the client.
	 * @param pkt the game over reason packet
	 */
	@Listener
	public void onPacket(GameOverReasonPacket pkt) {
		// Disable heartbeat dependency
		try { server.setTimeout(0); }
		catch (IOException e) { server.stop(); }
		
		// Stop Comms service
		if (service != null) {
			service.onPacket(pkt);
			gameRunning = false;
		}
		
		// Stop buttons flashing
		missionFlash = false;
		allyFlash = false;
		stationFlash = false;
		
		// Clean up Routing graph
		graph = null;
		
		// Clear current data
		missions.clear();
		for (ConcurrentHashMap<String, AllyStatusRow> map: allies.values()) map.clear();
		allies.clear();
		for (ConcurrentHashMap<String, StationStatusRow> map: bases.values()) map.clear();
		bases.clear();
		for (CopyOnWriteArrayList<SideMissionRow> list: closed.values()) list.clear();
		closed.clear();
		for (CopyOnWriteArrayList<String> list: rogues.values()) list.clear();
		rogues.clear();
		inPackets.clear();
		outPackets.clear();
		dockingStation = null;
		clearAllTables();
	}
	
	/**
	 * Called whenever an object update packet is received by the client. This is how the client knows a simulation has
	 * started.
	 * @param pkt the object update packet
	 */
	@Listener
	public void onPacket(final ObjectUpdatePacket pkt) {
		// Activate Comms service handler
		gameRunning = true;
		
		// Activate heartbeat dependency
		try { server.setTimeout(heartbeatTimeout); }
		catch (IOException e) { server.stop(); }
		
		// Prepare Routing graph
		if (graph == null)
			try {
				int id = manager.getPlayerShip(playerShip).getId();
				ArtemisObject object = manager.getObject(id);
				graph = new RoutingGraph(manager, object.getId());
			} catch (Exception e) { }
		
		// Find all NPC ships
		List<ArtemisObject> objects = manager.getObjects(ObjectType.NPC_SHIP);
		for (ArtemisObject obj: objects) {
			final ArtemisNpc npc = (ArtemisNpc) obj;
			
			// Skip enemy ships
			if (npc.getVessel(context) == null ||
					npc.getVessel(context).getFaction() == null ||
					npc.getVessel(context).getFaction().getId() > 1) continue;
			
			// Skip NPCs we already know about
			if (allies.containsKey(npc.getName())) continue;
			
			// Skip fighters
			if (npc.getVessel(context).is(VesselAttribute.FIGHTER) ||
					npc.getVessel(context).is(VesselAttribute.SINGLESEAT)) continue;
			
			// Set up ally table entry
			allies.put(npc.getName(), new ConcurrentHashMap<String, AllyStatusRow>());
			
			// Set up a rogues table entry in case we discover it's a trap
			if (!rogues.containsKey(npc.getName())) rogues.put(npc.getName(), new CopyOnWriteArrayList<String>());
			
			// Set up table row in Allies view
			updateHandler.post(new Runnable() {
				@Override
				public void run() {
					final AllyStatusRow row = new AllyStatusRow(getBaseContext(),
							npc,
							npc.getName() + " " + npc.getVessel(context).getName());
					row.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							outPackets.add(new CommsOutgoingPacket(npc, OtherMessage.HAIL, context));
						}
					});
					allies.get(npc.getName()).put(npc.getVessel(context).getName(), row);
					alliesView.post(new Runnable() {
						@Override
						public void run() {
							try { alliesTable.addView(row); }
							catch (IllegalStateException e) { }
						}
					});
				}
			});
			
			// Send a hail message to discover actual status
			outPackets.add(new CommsOutgoingPacket(npc, OtherMessage.HAIL, context));
		}
		
		// Find all docking stations
		objects = manager.getObjects(ObjectType.BASE);
		for (ArtemisObject obj: objects) {
			final ArtemisBase base = (ArtemisBase) obj;
			
			// Skip enemy stations (in Border War or Deep Strike)
			if (base.getVessel(context) == null ||
					base.getVessel(context).getFaction() == null ||
					base.getVessel(context).getFaction().getId() > 1) continue;
			
			// Skip stations we already know about
			if (bases.containsKey(base.getName())) continue;
			
			// Set up table row in Stations view
			bases.put(base.getName(), new ConcurrentHashMap<String, StationStatusRow>());
			updateHandler.post(new Runnable() {
				@Override
				public void run() {
					synchronized (bases) {
						final StationStatusRow row = new StationStatusRow(getBaseContext(), base, context);
						row.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								outPackets.add(new CommsOutgoingPacket(base, BaseMessage.STAND_BY_FOR_DOCKING, context));
							}
						});
						bases.get(base.getName()).put(base.getVessel(context).getName(), row);
						bases.notifyAll();
					}
				}
			});
			
			// Send status report request
			outPackets.add(new CommsOutgoingPacket(base, BaseMessage.PLEASE_REPORT_STATUS, context));
			
			// Wait for row to be added to memory
			synchronized (bases) {
				while (!bases.get(base.getName()).containsKey(base.getVessel(context).getName())) {
					try { bases.wait(); }
					catch (InterruptedException e) { }
				}
			}
			
			// Add table row to Stations View
			final StationStatusRow row = bases.get(base.getName()).get(base.getVessel(context).getName());
			int index = -1;
			int minIndex = 0, maxIndex = stationsTable.getChildCount();
			while (minIndex < maxIndex) {
				index = (maxIndex + minIndex) / 2;
				String rowName = base.getName();
				StationStatusRow otherRow = (StationStatusRow) stationsTable.getChildAt(index);
				TextView statusText = (TextView) otherRow.getChildAt(0);
				String otherName = statusText.getText().toString().split(" ")[0];
				if (otherName.startsWith("DS") && rowName.startsWith("DS")) {
					rowName = rowName.substring(2);
					otherName = otherName.substring(2);
					while (rowName.length() < otherName.length()) rowName = "0" + rowName;
					while (otherName.length() < rowName.length()) otherName = "0" + otherName;
				}
				if (rowName.compareTo(otherName) < 0) {
					maxIndex = index;
				} else if (rowName.compareTo(otherName) > 0) {
					minIndex = ++index;
				} else break;
			}
			final int rowIndex = index;
			stationsView.post(new Runnable() {
				@Override
				public void run() {
					try {
						stationsTable.addView(row, rowIndex);
					} catch (IllegalStateException e) { }
				}
			});
		}
		
		// Check if we are docked with a station
		if (dockingStation == null) {
			for (ArtemisObject o: pkt.getObjects()) {
				if (o.getType() != ObjectType.PLAYER_SHIP) continue;
				ArtemisPlayer player = (ArtemisPlayer)manager.getObject(o.getId());
				if (player == null) continue;
				if (player.getShipNumber() == playerShip && player.getDockingBase() > 0) {
					for (ArtemisObject b: objects) {
						if (b.getId() == player.getDockingBase()) {
							dockingStation = b.getName();
							updateDockedRow();
							stationHandler.postDelayed(waitForUndock, updateInterval);
							break;
						}
					}
				}
			}
		}
		
		// Start up object update handler
		if (!updateRunning) {
			updateHandler.postDelayed(updateEntities, updateInterval);
		}
		
		// Start up routing algorithm handler
		if (!routing) {
			routeHandler.postDelayed(updateRoute, updateInterval);
		}
	}
	
	/**
	 * Called when a ship settings packet is received by a client. This occurs upon connection to a server or when a
	 * ship's name is changed. The ship selection box is populated here.
	 * @param pkt the ship settings packet
	 */
	@Listener
	public void onPacket(AllShipSettingsPacket pkt) {
		final Spinner shipSpinner = (Spinner) findViewById(R.id.shipSpinner);
		final int selIndex = shipSpinner.getSelectedItemPosition();
		
		// Set up app font in selection box and list
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.ship_name_spinner) {
			@Override
			public View getView(int index, View convertView, ViewGroup parent) {
				TextView v = (TextView) super.getView(index, convertView, parent);
				v.setTypeface(APP_FONT);
				return v;
			}
			
			@Override
			public View getDropDownView(int index, View convertView, ViewGroup parent) {
				TextView v = (TextView) super.getDropDownView(index, convertView, parent);
				v.setTypeface(APP_FONT);
				return v;
			}
		};
		
		// Add ship names to selection box
		for (int i = 1; i <= Artemis.SHIP_COUNT; i++) {
			adapter.add(pkt.getShip(i).getName());
		}
		
		// Set up adapter
		shipSpinner.post(new Runnable() {
			@Override
			public void run() {
				shipSpinner.setAdapter(adapter);
				shipSpinner.setSelection(selIndex);
			}
		});
	}
	
	/**
	 * Called when a destroyed object packet is received by the client.
	 * @param pkt the destroyed object packet
	 */
	@Listener
	public void onPacket(DestroyObjectPacket pkt) {
		// Get destroyed object
		final ArtemisObject object = manager.getObject(pkt.getTarget());
		
//		new AsyncTask<String, String, String>() {
//			@Override
//			public String doInBackground(String... params) {
				
				if (object instanceof ArtemisBase) {
					// If it was a station...
					ArtemisBase base = (ArtemisBase) object;
					
					// Skip enemy stations (in Border War and Deep Strike)
					if (base.getVessel(context) == null ||
							base.getVessel(context).getFaction() == null ||
							base.getVessel(context).getFaction().getId() > 1) return;
					
					// Remove from Stations table
					if (bases.containsKey(base.getName())) {
						for (final StationStatusRow row: bases.get(base.getName()).values()) {
							stationsView.post(new Runnable() {
								@Override
								public void run() {
									stationsTable.removeView(row);
								}
							});
						}
						bases.get(base.getName()).clear();
						bases.remove(base.getName());
					}
				} else if (object instanceof ArtemisNpc) {
					// If it was an NPC ship...
					ArtemisNpc npc = (ArtemisNpc) object;
					
					// Skip enemy ships
					if (npc.getVessel(context) == null ||
							npc.getVessel(context).getFaction() == null ||
							npc.getVessel(context).getFaction().getId() > 1) return;
					
					// Remove from Allies table
					if (allies.containsKey(npc.getName())) {
						final AllyStatusRow row = allies.get(npc.getName()).get(npc.getVessel(context).getName());
						alliesView.post(new Runnable() {
							@Override
							public void run() {
								alliesTable.removeView(row);
							}
						});
						allies.get(npc.getName()).remove(npc.getVessel(context).getName());
						if (allies.get(npc.getName()).isEmpty()) allies.remove(npc.getName());
					}
					
					// If it's a trap, remove from rogues table
					if (rogues.containsKey(npc.getName())) {
						rogues.get(npc.getName()).remove(npc.getVessel(context).getName());
						if (rogues.get(npc.getName()).isEmpty()) rogues.remove(npc.getName());
					}
				} else {
					// If neither a base nor an ally ship, exit
					return;
				}
				
				// If it was a base or an ally ship, remove all affiliated side missions
				for (int i = 0; i < missions.size(); i++) {
					final SideMissionRow row = missions.get(i);
					
					// Skip missions that are already completed
					if (row.isCompleted()) continue;
					
					// Check if this object is affiliated with this mission
					if ((!row.isStarted() && row.getSource().startsWith(object.getName())) ||
							row.getDestination().startsWith(object.getName())) {
						try {
							// If mission wasn't started, one less mission for entity that wasn't destroyed
							if (!row.isStarted()) {
								String[] other = null;
								if (row.getSource().startsWith(object.getName())) {
									other = row.getDestination().split(" ", 3);
								} else if (row.getDestination().startsWith(object.getName())) {
									other = row.getSource().split(" ", 3);
								}
								if (other != null) {
									for (int n = 0; n < row.getNumRewards(); n++) {
										try {
											if (allies.containsKey(other[0])) {
												AllyStatusRow otherRow = allies.get(other[0]).get(other[2]);
												otherRow.removeMission();
												postStatusUpdate(otherRow);
											} else if (bases.containsKey(other[0])) {
												StationStatusRow otherRow = bases.get(other[0]).get(other[2]);
												otherRow.removeMission();
												postStatusUpdate(otherRow);
											}
										} catch (NullPointerException e) { }
									}
								}
							}
							
							// Remove row from Missions table
							missionsView.post(new Runnable() {
								@Override
								public void run() {
									missionsTable.removeView(row);
								}
							});
						} catch (Exception e) { }
						missions.remove(i--);
						
						// Designate mission as uncompletable
						if (object instanceof ArtemisNpc && closed.containsKey(object.getName()))
							closed.get(object.getName()).add(row);
					}
				}
//				return null;
//			}
//		}.execute();
	}
	
	/**
	 * Called when a Comms packet is received by the client. Adds it to the app's mailbox.
	 * @param pkt the incoming Comms packet
	 */
	@Listener
	public void onPacket(CommsIncomingPacket pkt) {
		inPackets.add(pkt);
	}
	
	/**
	 * Called when a PausePacket is received by the client. Pauses build countdown timers on all stations.
	 * @param pkt the pause packet
	 */
	@Listener
	public void onPacket(PausePacket pkt) {
		for (String key: bases.keySet()) {
			for (StationStatusRow row: bases.get(key).values()) {
				row.setPaused(pkt.getPaused().getBooleanValue());
			}
		}
	}
	
	private void handlePackets() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		while (true) {
//			try {
				// Open inbox
				while (!inPackets.isEmpty()) {
					final CommsIncomingPacket pkt = inPackets.get(0);
					if (pkt == null) {
						continue;
					}
					inPackets.remove(0);
					final String sender = pkt.getFrom();
					final String message = pkt.getMessage();
					
					if (message.contains("\nOur shipboard computer")) {
						// Ally ship has malfunctioning computer
						final String id = sender.substring(sender.lastIndexOf(" ") + 1);
						final String name = sender.split(" " + id)[0];
						if (allies.containsKey(id)) {
							AllyStatusRow row = allies.get(id).get(name);
							row.setStatus(AllyStatus.BROKEN_COMPUTER);
							row.setEnergy(message.endsWith("you need some."));
							postStatusUpdate(row);
							postColorUpdate(row);
							updateAlliesTable();
						}
						
						// Update the Routing graph
						updateRouteGraph(pref.getBoolean(getString(R.string.malfunctionKey), false));
					} else if (message.startsWith("Docking crew")) {
						// Make sure this ship is the correct one
						if (message.split(", ")[1].startsWith(manager.getPlayerShip(playerShip).getName())) {
							for (ArtemisObject o: manager.getObjects(ObjectType.BASE)) {
								final String[] senderParts = sender.split(" ", 3);
								if (o.getName().startsWith(senderParts[0]) && bases.containsKey(senderParts[0])) {
									final StationStatusRow row = bases.get(senderParts[0]).get(senderParts[2]);
									row.setReady(true);
									postStatusUpdate(row);
								}
							}
						}
					} else if (message.startsWith("Docking complete")) {
						// Docking at a station
						for (ArtemisObject o: manager.getObjects(ObjectType.BASE)) {
							ArtemisBase base = (ArtemisBase) o;
							if (base.getId() == manager.getPlayerShip(playerShip).getDockingBase()) {
								dockingStation = sender.split(" ")[0];
								updateDockedRow();
								for (StationStatusRow row: bases.get(dockingStation).values()) {
									if (row.completeDock()) {
										postStatusUpdate(row);
									}
								}
								stationHandler.postDelayed(waitForUndock, updateInterval);
								break;
							}
						}
					} else if (message.startsWith("We've produced") || message.contains("ing production of")) {
						// Production of previous ordnance ended
						String base = sender.split(" ")[0];
						if (bases.containsKey(base)) {
							for (StationStatusRow row: bases.get(base).values()) {
								// If a new missile was produced, recalibrate production speed
								if (message.startsWith("We've")) {
									row.recalibrateSpeed();
								}
								row.resetMissile();
							}
						}
						
						// Commencing production of a new ordnance
						for (ArtemisObject o: manager.getObjects(ObjectType.BASE)) {
							if (!sender.startsWith(o.getName())) continue;
							outPackets.add(new CommsOutgoingPacket(o, BaseMessage.PLEASE_REPORT_STATUS, context));
							break;
						}
					} else if (message.contains("Our stores are")) {
						// Docking station status report
						final String[] list = message.split("\n");
						final int shields = Integer.parseInt(list[0].split("at ")[1].split(" ")[0]);
						final String[] senderParts = sender.split(" ", 3);
						final StationStatusRow row = bases.get(senderParts[0]).get(senderParts[2]);
						row.setShields(shields);
						for (OrdnanceType type: OrdnanceType.values()) {
							row.setStock(type, Integer.parseInt(list[type.ordinal()+1].split("of")[0].trim()));
							if (list[list.length - 1].contains(type.toString())) {
								String[] lastWords = list[list.length - 1].split(" ");
								int minutes = Integer.parseInt(lastWords[lastWords.length - 2]);
								row.setBuildTime(minutes);
								row.setOrdnanceType(type);
							}
						}
						if (list.length > 7) row.setFighters(Integer.parseInt(list[6].split(" ")[1]));
						postStatusUpdate(row);
						postColorUpdate(row);
						postOrdnanceUpdate(row);
					} else if (message.contains("\nWe are holding this ship")) {
						// Ally ship taken hostage
						final String id = sender.substring(sender.lastIndexOf(" ") + 1);
						final String name = sender.split(" " + id)[0];
						if (allies.containsKey(id)) {
							AllyStatusRow row = allies.get(id).get(name);
							row.setStatus(AllyStatus.HOSTAGE);
							postStatusUpdate(row);
							postColorUpdate(row);
							updateAlliesTable();
						}
						
						// Update the Routing graph
						updateRouteGraph(pref.getBoolean(getString(R.string.hostageKey), false));
					} else if (message.contains("\nWe have commandeered")) {
						// Ally ship commandeered
						final String id = sender.substring(sender.lastIndexOf(" ") + 1);
						final String name = sender.split(" " + id)[0];
						if (allies.containsKey(id)) {
							AllyStatusRow row = allies.get(id).get(name);
							row.setStatus(AllyStatus.COMMANDEERED);
							postStatusUpdate(row);
							postColorUpdate(row);
							updateAlliesTable();
						}
					} else if (message.contains("\nOur sensors are all down")) {
						// Ally ship flying blind
						final String id = sender.substring(sender.lastIndexOf(" ") + 1);
						final String name = sender.split(" " + id)[0];
						if (allies.containsKey(id)) {
							AllyStatusRow row = allies.get(id).get(name);
							row.setStatus(AllyStatus.FLYING_BLIND);
							row.setEnergy(message.endsWith("you need some."));
							postStatusUpdate(row);
							postColorUpdate(row);
							updateAlliesTable();
						}
					} else if (message.contains("\nOur engines are damaged")) {
						// Ally ship needs DamCon team
						final String id = sender.substring(sender.lastIndexOf(" ") + 1);
						final String name = sender.split(" " + id)[0];
						if (allies.containsKey(id)) {
							AllyStatusRow row = allies.get(id).get(name);
							row.setStatus(AllyStatus.NEED_DAMCON);
							row.setEnergy(message.endsWith("you need some."));
							postStatusUpdate(row);
							postColorUpdate(row);
							updateAlliesTable();
						}
						
						// Update the Routing graph
						updateRouteGraph(pref.getBoolean(getString(R.string.needDamConKey), false));
					} else if (message.contains("\nWe're out of energy")) {
						// Ally ship needs energy
						final String id = sender.substring(sender.lastIndexOf(" ") + 1);
						final String name = sender.split(" " + id)[0];
						if (allies.containsKey(id)) {
							AllyStatusRow row = allies.get(id).get(name);
							row.setStatus(AllyStatus.NEED_ENERGY);
							postStatusUpdate(row);
							postColorUpdate(row);
							updateAlliesTable();
						}
						
						// Update the Routing graph
						updateRouteGraph(pref.getBoolean(getString(R.string.needEnergyKey), false));
					} else if (message.startsWith("Torpedo transfer")) {
						// Ally ship gives you torpedoes in Deep Strike
						if (allies.containsKey(sender)) {
							for (AllyStatusRow row: allies.get(sender).values()) {
								row.setTorpedoes(false);
								postStatusUpdate(row);
							}
						}
					} else if (message.startsWith("Here's the energy")) {
						// Ally ship gives you energy
						if (allies.containsKey(sender)) {
							for (AllyStatusRow row: allies.get(sender).values()) {
								row.setEnergy(false);
								postStatusUpdate(row);
							}
							updateAlliesTable();
						}
					} else if (message.endsWith("when we get there.")) {
						// Ally ship delivering reward
						if (allies.containsKey(sender)) {
							for (AllyStatusRow row: allies.get(sender).values()) {
								row.setStatus(AllyStatus.REWARD);
								postStatusUpdate(row);
								postColorUpdate(row);
							}
							updateAlliesTable();
						}
					} else if (message.contains("We're broken down!") || message.contains("How are you?")) {
						// Ally ship is actually a trap
						final String id = sender.substring(sender.lastIndexOf(" ") + 1);
						final String name = sender.split(" " + id)[0];
						if (!rogues.containsKey(id)) rogues.put(id, new CopyOnWriteArrayList<String>());
						rogues.get(id).add(name);
						try {
							AllyStatusRow row = allies.get(id).get(name);
							row.setStatus(message.contains("How are you?") ?
									AllyStatus.MINE_TRAP : AllyStatus.FIGHTERS);
							postStatusUpdate(row);
							postColorUpdate(row);
						} catch (NullPointerException e) { }
						for (int i = 0; i < missions.size(); i++) {
							SideMissionRow row = (SideMissionRow) missions.get(i);
							if (row.isCompleted()) continue;
							if ((!row.isStarted() && row.getSource().startsWith(id)) ||
									row.getDestination().startsWith(id)) {
								missions.remove(i--);
							}
						}
						updateMissionsTable();
						updateAlliesTable();
					} else if (message.contains("and we'll")) {
						// Send to Comms service
						if (service != null) service.onPacket(pkt);
						
						// New side mission
						final String[] senderParts = sender.split(" ", 3);
						
						// If sender is a trap ship, ignore this
						if (rogues.containsKey(senderParts[0]) && rogues.get(senderParts[0]).contains(senderParts[2]))
							continue;
						
						// Get source
						final String srcShip = message.split("with ")[1].split(" ")[0];
						final String source;
						
						if (bases.containsKey(srcShip)) {
							// Source location is a station
							String srcStation = "";
							for (String key: bases.get(srcShip).keySet()) srcStation = key;
							source = srcShip + " Terran " + srcStation;
							try {
								if (allies.containsKey(senderParts[0])) {
									AllyStatusRow row = allies.get(senderParts[0]).get(senderParts[2]);
									row.addMission();
									postStatusUpdate(row);
								} else if (bases.containsKey(senderParts[0])) {
									StationStatusRow row = bases.get(senderParts[0]).get(senderParts[2]);
									row.addMission();
									postStatusUpdate(row);
								}
							} catch (NullPointerException e) { }
							if (bases.containsKey(srcShip)) {
								StationStatusRow row = bases.get(srcShip).get(srcStation);
								row.addMission();
								postStatusUpdate(row);
							}
						} else {
							// Source location is an ally ship - skip if it's a rogue
							if (rogues.containsKey(srcShip) && allies.get(srcShip).size() == rogues.get(srcShip).size())
								continue;
							if (!allies.containsKey(srcShip)) continue;
							
							String srcString = srcShip;
							if (allies.get(srcShip).size() == 1) {
								srcString += " Terran ";
								for (String s: allies.get(srcShip).keySet()) srcString += s;
							}
							source = srcString;
							if (allies.get(srcShip).size() > 1 && !closed.containsKey(srcShip))
								closed.put(srcShip, new CopyOnWriteArrayList<SideMissionRow>());
							try {
								if (allies.containsKey(senderParts[0])) {
									AllyStatusRow row = allies.get(senderParts[0]).get(senderParts[2]);
									row.addMission();
									postStatusUpdate(row);
								} else if (bases.containsKey(senderParts[0])) {
									StationStatusRow row = bases.get(senderParts[0]).get(senderParts[2]);
									row.addMission();
									postStatusUpdate(row);
								}
							} catch (NullPointerException e) { }
							if (allies.containsKey(srcShip)) {
								AllyStatusRow row = allies.get(srcShip).get(source.substring(srcShip.length() + 8));
								row.addMission();
								postStatusUpdate(row);
							}
						}
						
						// Extract reward
						String[] words = message.split(" ");
						final String reward = words[words.length - 1];
						
						// Check if this mission should be displayed according to reward filters
						final boolean show;
						if (reward.equals(SideMissionRow.BATTERY_KEY)) {
							show = pref.getBoolean(getString(R.string.batteryChargeKey), true);
						} else if (reward.equals(SideMissionRow.NUCLEAR_KEY)) {
							show = pref.getBoolean(getString(R.string.nuclearKey), true);
						} else if (reward.equals(SideMissionRow.SHIELD_KEY)) {
							show = pref.getBoolean(getString(R.string.shieldKey), true);
						} else if (reward.equals(SideMissionRow.COOLANT_KEY)) {
							show = pref.getBoolean(getString(R.string.extraCoolantKey), true);
						} else {
							show = pref.getBoolean(getString(R.string.speedKey), true);
						}

						// Check if there's a row to merge with this one
						boolean exists = false;
						for (int i = 0; i < missions.size(); i++) {
							final SideMissionRow row = missions.get(i);
							if (row.isStarted() ||
									!row.getSource().contains(" ") ||
									!row.getSource().startsWith(source) ||
									!row.getDestination().equals(sender)) continue;
							row.addReward(reward);
							exists = true;
							break;
						}
						
						// If not, add to Missions view if filters allow
						if (!exists) {
							final SideMissionRow newRow =
									new SideMissionRow(getBaseContext(), source, sender, reward);
							missions.add(newRow);
							if (show) {
								missionFlash = missionsView.getVisibility() == View.GONE;
								missionsView.post(new Runnable() {
									@Override
									public void run() {
										missionsTable.addView(newRow);
									}
								});
							}
						} else if (show)
							updateMissionsTable();
						
						// Update the Routing graph
						updateRouteGraph(!show);
					} else if (message.contains("to deliver the")) {
						// Send to Comms service
						if (service != null)
							service.onPacket(pkt);
						
						// Visited source of side mission
						String destination = message.split(" to ")[1];
						for (int i = 0; i < missions.size(); i++) {
							final SideMissionRow row = missions.get(i);
							if (row.isStarted() ||
									!sender.startsWith(row.getSource()) ||
									!row.getDestination().startsWith(destination)) continue;
							row.markAsStarted();
							missionFlash = missionsView.getVisibility() == View.GONE;
							row.updateSource(sender);
							row.updateProgress();
							
							for (int j = 0; j < missions.size(); j++) {
								final SideMissionRow other = missions.get(j);
								if (j == i ||
										!other.isStarted() ||
										other.isCompleted() ||
										!other.getSource().equals(sender) ||
										!other.getDestination().equals(row.getDestination())) continue;
								String[] rewards = row.getRewardList().split(", ");
								for (String s: rewards) {
									int quantity = 1;
									if (s.charAt(s.length() - 2) == 'x') {
										quantity = Integer.parseInt(s.substring(s.indexOf(" x") + 2));
										s = s.split(" x")[0];
									}
									final String rewardStr = s;
									for (int t = 0; t < quantity; t++) other.addReward(rewardStr);
								}
								missionsView.post(new Runnable() {
									@Override
									public void run() {
										try { missionsTable.removeView(row); }
										catch (IllegalStateException e) { }
									}
								});
								missions.remove(row);
								break;
							}
							updateMissionsTable();
						}
						
						// Remove sender from the mission, their part of it is done
						final String[] senderParts = sender.split(" ", 3);
						String id = senderParts[0];
						try {
							if (allies.containsKey(senderParts[0])) {
								AllyStatusRow row = allies.get(senderParts[0]).get(senderParts[2]);
								row.removeMission();
								postStatusUpdate(row);
							} else if (bases.containsKey(senderParts[0])) {
								StationStatusRow row = bases.get(senderParts[0]).get(senderParts[2]);
								row.removeMission();
								postStatusUpdate(row);
							}
						} catch (NullPointerException e) { }
						
						// Salvage missions that were designated uncompletable
						if (!closed.containsKey(id)) continue;
						for (int i = 0; i < closed.get(id).size(); i++) {
							final SideMissionRow row = missions.get(i);
							if (row.isStarted() ||
									!sender.startsWith(row.getSource()) ||
									!row.getDestination().startsWith(destination)) continue;
							missions.add(row);
							row.markAsStarted();
							closed.get(id).remove(row);
							row.updateSource(sender);
							row.updateProgress();
							missionsView.post(new Runnable() {
								@Override
								public void run() {
									missionsTable.addView(row);
								}
							});
							i--;
						}
					} else if (message.startsWith("As promised")) {
						// Send to Comms service
						if (service != null)
							service.onPacket(pkt);
						
						// End of side mission
						String[] words = message.split(" ");
						String reward = words[words.length - 1];
						
						if (reward.equals(SideMissionRow.PRODUCTION_KEY)) {
							String baseName = sender.split(" ")[0];
							if (bases.containsKey(baseName)) {
								for (StationStatusRow baseRow: bases.get(baseName).values()) {
									baseRow.incProductionSpeed();
									postOrdnanceUpdate(baseRow);
								}
							}
						}
						
						// Find side mission row and finalize that mission
						for (int i = 0; i < missions.size(); i++) {
							final SideMissionRow row = missions.get(i);
							if (row.isCompleted() ||
									!row.isStarted() ||
									!row.getDestination().equals(sender) ||
									!row.hasReward(reward)) continue;
							row.markAsCompleted();
							missionFlash = missionsView.getVisibility() == View.GONE;
							row.updateProgress();
							
							// Set up touch-to-remove function
							row.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View arg0) {
									missions.remove(row);
									missionsTable.removeView(row);
								}
							});
						}
						
						// Remove sender from mission as it is now completed
						final String[] senderParts = sender.split(" ", 3);
						String id = senderParts[0];
						try {
							if (allies.containsKey(senderParts[0])) {
								AllyStatusRow row = allies.get(senderParts[0]).get(senderParts[2]);
								row.removeMission();
								postStatusUpdate(row);
							} else if (bases.containsKey(senderParts[0])) {
								StationStatusRow row = bases.get(senderParts[0]).get(senderParts[2]);
								row.removeMission();
								postStatusUpdate(row);
							}
						} catch (NullPointerException e) { }
						
						// Salvage missions that were designated uncompletable
						if (!closed.containsKey(id)) continue;
						for (int i = 0; i < closed.get(id).size(); i++) {
							final SideMissionRow row = closed.get(id).get(i);
							if (row.isCompleted() ||
									!row.isStarted() ||
									!row.getDestination().equals(sender) ||
									!row.hasReward(reward)) continue;
							missions.add(row);
							row.markAsCompleted();
							closed.get(id).remove(row);
							row.updateProgress();
							row.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View arg0) {
									missions.remove(row);
									missionsTable.removeView(row);
								}
							});
							missionsView.post(new Runnable() {
								@Override
								public void run() {
									missionsTable.addView(row);
								}
							});
							i--;
						}
					} else if (message.endsWith("Enjoy your reward!")) {
						// Ally ship delivered a reward
						for (ArtemisObject o: manager.getObjects(ObjectType.BASE)) {
							outPackets.add(
									new CommsOutgoingPacket(o, BaseMessage.PLEASE_REPORT_STATUS, context));
						}
						if (allies.containsKey(sender)) {
							for (AllyStatusRow row: allies.get(sender).values()) {
								row.setBlind(false);
								row.setStatus(AllyStatus.NORMAL);
								postStatusUpdate(row);
								postColorUpdate(row);
							}
							updateAlliesTable();
						}
					} else if (message.endsWith("Let us upgrade your shield generators.")) {
						// Ally ship rescued from captors
						if (allies.containsKey(sender)) {
							for (AllyStatusRow row: allies.get(sender).values()) {
								row.setStatus(AllyStatus.NORMAL);
								postStatusUpdate(row);
								postColorUpdate(row);
							}
							updateAlliesTable();
						}
					} else if (message.contains("we are turning")) {
						// Ally ship flying blind has been given directions
						if (allies.containsKey(sender)) {
							for (AllyStatusRow row: allies.get(sender).values())
								if (row.getStatus() == AllyStatus.FLYING_BLIND) {
									row.setStatus(AllyStatus.NORMAL);
									postStatusUpdate(row);
									postColorUpdate(row);
								}
							updateAlliesTable();
						}
					} else if (message.startsWith("Okay, going")) {
						// Ally ship flying blind now has a destination
						if (!allies.containsKey(sender)) {
							for (AllyStatusRow row: allies.get(sender).values()) {
								if (row.isFlyingBlind()) {
									row.setStatus(AllyStatus.REWARD);
									postStatusUpdate(row);
									postColorUpdate(row);
								}
							}
							updateAlliesTable();
						}
					} else if (message.startsWith("Our shields") &&
							!message.contains("\nWe're heading to the station") &&
							!sender.startsWith("DS")) {
						// Any other message from ally ships
						final String id = sender.substring(sender.lastIndexOf(" ") + 1);
						final String name = sender.split(" " + id)[0];
						final boolean hasEnergy = message.endsWith("you need some.");
						if (allies.containsKey(id)) {
							AllyStatusRow row = allies.get(id).get(name);
							if (row.getStatus() != AllyStatus.REWARD) row.setStatus(AllyStatus.NORMAL);
							row.setEnergy(hasEnergy);
							row.setTorpedoes(message.contains("We have some torpe"));
							postStatusUpdate(row);
							postColorUpdate(row);
							updateAlliesTable();
						}
						
						// Update the Routing graph
						updateRouteGraph(!hasEnergy);
					}
				}
				
				// Send everything from outbox
				if (!outPackets.isEmpty()) {
					if (outPackets.get(0) == null) {
						continue;
					}
					server.send(outPackets.remove(0));
				}
//			} catch (NullPointerException e) { break; }
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		// If there's no server running, do nothing
		if (server == null || !server.isConnected()) return;
		
		// Start up Comms service
		bindService(new Intent(this, CommsService.class), connection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// App resumed, stop Comms service
		if (service != null) {
			unbindService(connection);
			service = null;
		}
	}
	
	// Empty out all tables
	public void clearAllTables() {
		missionsView.post(new Runnable() {
			@Override
			public void run() {
				missionsTable.removeAllViews();
			}
		});
		alliesView.post(new Runnable() {
			@Override
			public void run() {
				alliesTable.removeAllViews();
			}
		});
		stationsView.post(new Runnable() {
			@Override
			public void run() {
				stationsTable.removeAllViews();
			}
		});
	}
	
	// Update Missions table
	public void updateMissionsTable() {
		missionsView.post(new Runnable() {
			@Override
			public void run() {
				missionsTable.removeAllViews();
			}
		});
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		for (int i = 0; i < missions.size(); i++) {
			final SideMissionRow row = missions.get(i);
			boolean show = false;
			show |= row.hasReward(SideMissionRow.BATTERY_KEY) &&
					preferences.getBoolean(getString(R.string.batteryChargeKey), true);
			show |= row.hasReward(SideMissionRow.COOLANT_KEY) &&
					preferences.getBoolean(getString(R.string.extraCoolantKey), true);
			show |= row.hasReward(SideMissionRow.NUCLEAR_KEY) &&
					preferences.getBoolean(getString(R.string.nuclearKey), true);
			show |= row.hasReward(SideMissionRow.PRODUCTION_KEY) &&
					preferences.getBoolean(getString(R.string.speedKey), true);
			show |= row.hasReward(SideMissionRow.SHIELD_KEY) &&
					preferences.getBoolean(getString(R.string.shieldKey), true);
			final boolean showRow = show;
			missionsView.post(new Runnable() {
				@Override
				public void run() {
					if (showRow) missionsTable.addView(row);
				}
			});
		}
	}
	
	// Update Stations table
	public void updateStationsTable() {
		for (final String n: bases.keySet()) {
			for (final String s: bases.get(n).keySet()) {
				stationsView.post(new Runnable() {
					@Override
					public void run() {
						try { stationsTable.addView(bases.get(n).get(s)); }
						catch (Exception e) { }
					}
				});
			}
		}
	}
	
	// Update Allies table
	public void updateAlliesTable() {
		alliesView.post(new Runnable() {
			@Override
			public void run() {
				alliesTable.removeAllViews();
			}
		});
		int rowCount = 0;
		for (String n: allies.keySet()) {
			for (final AllyStatusRow r: allies.get(n).values()) {
				int index = -1;
				int minIndex = 0, maxIndex = rowCount;
				while (minIndex < maxIndex) {
					index = (maxIndex + minIndex) / 2;
					AllyStatusRow otherRow = (AllyStatusRow) alliesTable.getChildAt(index);
					if (otherRow == null) continue;
					int compare = compare(r, otherRow);
					if (compare < 0) {
						maxIndex = index;
					} else if (compare > 0) {
						minIndex = ++index;
					} else break;
				}
				final int rowIndex = index;
				alliesView.post(new Runnable() {
					@Override
					public void run() {
						try { alliesTable.addView(r, rowIndex); }
						catch (IllegalStateException e) { }
					}
				});
				rowCount++;
			}
		}
	}
	
	private int compare(AllyStatusRow row1, AllyStatusRow row2) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		int methodIndex = Integer.parseInt(pref.getString(getString(R.string.allySortKey), "0"));
		AllySortMethod method = AllySortMethod.values()[methodIndex];
		TextView text1 = (TextView) row1.getChildAt(0);
		TextView text2 = (TextView) row2.getChildAt(0);
		String[] name1 = text1.getText().toString().split(" ", 2);
		String[] name2 = text2.getText().toString().split(" ", 2);
		int compare = 0;
		
		switch (method) {
		case NONE:
			break;
		case CLASS_STATUS_NAME:
			compare = name1[1].compareTo(name2[1]);
			if (compare != 0) return compare;
		case STATUS_NAME:
			if (row1.getStatus().index < row2.getStatus().index) return -1;
			else if (row1.getStatus().index > row2.getStatus().index) return 1;
			if (pref.getBoolean(getString(R.string.energyFirstKey), false)) {
				if (row1.hasEnergy() && !row2.hasEnergy()) return -1;
				else if (!row1.hasEnergy() && row2.hasEnergy()) return 1;
			}
		case NAME:
			compare = name1[0].compareTo(name2[0]);
			break;
		case STATUS_CLASS_NAME:
			if (row1.getStatus().index < row2.getStatus().index) return -1;
			else if (row1.getStatus().index > row2.getStatus().index) return 1;
			if (pref.getBoolean(getString(R.string.energyFirstKey), false)) {
				if (row1.hasEnergy() && !row2.hasEnergy()) return -1;
				else if (!row1.hasEnergy() && row2.hasEnergy()) return 1;
			}
		case CLASS_NAME:
			compare = name1[1].compareTo(name2[1]);
			if (compare != 0) return compare;
			compare = name1[0].compareTo(name2[0]);
			break;
		case CLASS_STATUS:
			compare = name1[1].compareTo(name2[1]);
			if (compare != 0) return compare;
		case STATUS:
			if (row1.getStatus().index < row2.getStatus().index) return -1;
			else if (row1.getStatus().index > row2.getStatus().index) return 1;
			if (pref.getBoolean(getString(R.string.energyFirstKey), false)) {
				if (row1.hasEnergy() && !row2.hasEnergy()) return -1;
				else if (!row1.hasEnergy() && row2.hasEnergy()) return 1;
			}
			break;
		case STATUS_CLASS:
			if (row1.getStatus().index < row2.getStatus().index) return -1;
			else if (row1.getStatus().index > row2.getStatus().index) return 1;
			if (pref.getBoolean(getString(R.string.energyFirstKey), false)) {
				if (row1.hasEnergy() && !row2.hasEnergy()) return -1;
				else if (!row1.hasEnergy() && row2.hasEnergy()) return 1;
			}
		case CLASS:
			compare = name1[1].compareTo(name2[1]);
			break;
		}
		
		return compare;
	}
	
	// Update station row, we are docked there
	public void updateDockedRow() {
		if (dockingStation == null) return;
		for (StationStatusRow row: bases.get(dockingStation).values()) {
			row.setDocking(true);
		}
		for (String key: bases.keySet()) {
			for (StationStatusRow row: bases.get(key).values()) {
				row.setReady(false);
				postStatusUpdate(row);
			}
		}
	}
	
	// We are no longer docked at a station
	public void resetDockedRow() {
		for (StationStatusRow row: bases.get(dockingStation).values()) {
			row.setDocking(false);
			postStatusUpdate(row);
		}
		dockingStation = null;
	}
	
	// Run by station handler while ship is docked, receives status report when undock happens
	final Runnable waitForUndock = new Runnable() {
		@Override
		public void run() {
			if (dockingStation != null) {
				if (manager.getPlayerShip(playerShip) == null) stationHandler.postDelayed(this, updateInterval);
				else if (manager.getPlayerShip(playerShip).getDockingBase() < 1) {
					for (ArtemisObject o: manager.getObjects(ObjectType.BASE)) {
						if (!o.getName().equals(dockingStation)) continue;
						outPackets.add(new CommsOutgoingPacket(o, BaseMessage.PLEASE_REPORT_STATUS, context));
						break;
					}
					resetDockedRow();
				} else stationHandler.postDelayed(this, updateInterval);
			}
		}
	};
	
	public void updateRouteGraph(boolean keepMinCost) {
		if (graph == null) return;
		graph.resetGraph();
		routing = true;
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		for (int i = 0; i < missions.size(); i++) {
			SideMissionRow row = missions.get(i);
			// Skip completed missions
			if (row.isCompleted()) continue;

			boolean show = false;
			if (pref.getBoolean(getString(R.string.batteryChargeKey), true)) {
				show |= row.hasReward(SideMissionRow.BATTERY_KEY);
			}
			if (pref.getBoolean(getString(R.string.nuclearKey), true)) {
				show |= row.hasReward(SideMissionRow.NUCLEAR_KEY);
			}
			if (pref.getBoolean(getString(R.string.shieldKey), true)) {
				show |= row.hasReward(SideMissionRow.SHIELD_KEY);
			}
			if (pref.getBoolean(getString(R.string.extraCoolantKey), true)) {
				show |= row.hasReward(SideMissionRow.COOLANT_KEY);
			}
			if (pref.getBoolean(getString(R.string.speedKey), true)) {
				show |= row.hasReward(SideMissionRow.PRODUCTION_KEY);
			}
			
			// Skip missions that are not shown because of reward filters
			if (!show) continue;
			
			// Get call signs of ships
			String srcCall = row.getSource().split(" ")[0];
			String destCall = row.getDestination().split(" ")[0];
			
			// If mission was started, ignore source
			if (row.isStarted()) {
				srcCall = destCall;
				destCall = null;
			}
			
			// Make sure both call signs belong to an ally or station
			if (!bases.containsKey(srcCall) && !allies.containsKey(srcCall)) continue;
			if (destCall != null && !bases.containsKey(destCall) && !allies.containsKey(destCall)) continue;
			
			ArtemisObject srcObject = null, destObject = null;
			
			ArrayList<ArtemisObject> points = new ArrayList<ArtemisObject>(manager.getObjects(ObjectType.BASE));
			points.addAll(manager.getObjects(ObjectType.NPC_SHIP));
			
			// Find source among stations and allies
			for (ArtemisObject obj: points) {
				BaseArtemisShielded npc = (BaseArtemisShielded) obj;
				
				// Skip enemies
				if (npc.getVessel(context) == null ||
						npc.getVessel(context).getFaction() == null ||
						npc.getVessel(context).getFaction().getId() > 1) continue;
				
				// Make sure call sign is a match
				if (!npc.getName().equals(srcCall)) continue;
				
				// Make sure object is not a fighter ship
				if (npc.getVessel(context).is(VesselAttribute.FIGHTER) ||
						npc.getVessel(context).is(VesselAttribute.SINGLESEAT)) continue;
				srcObject = npc;
				break;
			}
			
			// Do the same for destination
			if (destCall != null) {
				for (ArtemisObject obj: points) {
					BaseArtemisShielded npc = (BaseArtemisShielded) obj;
					if (npc.getVessel(context) == null ||
							npc.getVessel(context).getFaction() == null ||
							npc.getVessel(context).getFaction().getId() > 1) continue;
					if (!npc.getName().equals(destCall)) continue;
					if (npc.getVessel(context).is(VesselAttribute.FIGHTER) ||
							npc.getVessel(context).is(VesselAttribute.SINGLESEAT)) continue;
					destObject = npc;
					break;
				}
			}
			
			// Add path to routing graph
			graph.addPath(srcObject, destObject);
		}
		
		// Check for allies
		for (String key: allies.keySet()) {
			for (AllyStatusRow row: allies.get(key).values()) {
				if (row.getStatus().ordinal() >= AllyStatus.FIGHTERS.ordinal()) continue;
				
				boolean show = false;
				if (pref.getBoolean(getString(R.string.energyKey), false)) {
					show |= row.hasEnergy();
				}
				if (pref.getBoolean(getString(R.string.hasTorpsKey), false)) {
					show |= row.hasTorpedoes();
				}
				if (pref.getBoolean(getString(R.string.needEnergyKey), false)) {
					show |= row.getStatus() == AllyStatus.NEED_ENERGY;
				}
				if (pref.getBoolean(getString(R.string.needDamConKey), false)) {
					show |= row.getStatus() == AllyStatus.NEED_DAMCON;
				}
				if (pref.getBoolean(getString(R.string.malfunctionKey), false)) {
					show |= row.getStatus() == AllyStatus.BROKEN_COMPUTER;
				}
				if (pref.getBoolean(getString(R.string.hostageKey), false)) {
					show |= row.getStatus() == AllyStatus.HOSTAGE;
				}
				if (pref.getBoolean(getString(R.string.commandeeredKey), false)) {
					show |= row.getStatus() == AllyStatus.COMMANDEERED;
				}
				
				if (!show) continue;
				
				graph.addPath(row.getAllyShip(), null);
			}
		}
		
		graph.purgePaths();
		keepMinCost &= graph.size() <= routeTable.getChildCount();
		
		if (!keepMinCost) {
			graph.resetMinimumPoints();
		}
		
		graph.recalculateCurrentRoute();
	}
	
	// Updates Routing table
	final Runnable updateRoute = new Runnable() {
		@Override
		public void run() {
			// If we've destroyed our graph, Route table should be empty
			if (graph == null) {
				routeView.post(new Runnable() {
					@Override
					public void run() {
						routeTable.removeAllViews();
					}
				});
			}
			
			ArrayList<Integer> route = null;
			
			try {
				// Re-populate our graph
				updateRouteGraph(routing);
				
				// Guess optimal route
				route = graph.guessRoute();
			}
			catch (IllegalArgumentException e) { }
			catch (NullPointerException e) { }
			
			// If we have a route to fill, do so
			final ArrayList<RouteEntryRow> entryRows;
			if (route != null) {
				entryRows = new ArrayList<RouteEntryRow>(route.size());
				for (int id: route) {
					BaseArtemisShielded object = (BaseArtemisShielded)manager.getObject(id);
					String objName = object.getName() + " Terran " + object.getVessel(context).getName();
					ArtemisObject player = manager.getPlayerShip(playerShip);
					
					RouteEntryRow newRow = new RouteEntryRow(getBaseContext(), object, objName);
					newRow.updateDistance(player);
					entryRows.add(newRow);
				}
			} else {
				entryRows = new ArrayList<RouteEntryRow>(routeTable.getChildCount());
				for (int i = 0; i < routeTable.getChildCount(); i++)
					entryRows.add((RouteEntryRow)routeTable.getChildAt(i));
			}
			
			for (int r = 0; r < entryRows.size(); r++) {
				RouteEntryRow row = entryRows.get(r);
				String objName = row.getObjectName().split(" ")[0];
				RouteEntryReason[] reasons = new RouteEntryReason[0];
				
				if (objName.startsWith("DS") && bases.containsKey(objName)) {
					for (StationStatusRow baseRow: bases.get(objName).values()) {
						reasons = new RouteEntryReason[baseRow.getMissions()];
						for (int i = 0; i < reasons.length; i++)
							reasons[i] = RouteEntryReason.MISSION;
						break;
					}
				} else if (allies.containsKey(objName)) {
					SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
					for (AllyStatusRow allyRow: allies.get(objName).values()) {
						int numMissions = allyRow.getMissions();
						EnumSet<RouteEntryReason> others = EnumSet.noneOf(RouteEntryReason.class);
						if (pref.getBoolean(getString(R.string.energyKey), false) && allyRow.hasEnergy())
							others.add(RouteEntryReason.HAS_ENERGY);
						if (pref.getBoolean(getString(R.string.hasTorpsKey), false) && allyRow.hasTorpedoes())
							others.add(RouteEntryReason.TORPEDOES);
						switch (allyRow.getStatus()) {
						case NEED_ENERGY:
							if (pref.getBoolean(getString(R.string.needEnergyKey), false))
								others.add(RouteEntryReason.NEEDS_ENERGY);
							break;
						case NEED_DAMCON:
							if (pref.getBoolean(getString(R.string.needDamConKey), false))
								others.add(RouteEntryReason.DAMCON);
							break;
						case BROKEN_COMPUTER:
							if (pref.getBoolean(getString(R.string.malfunctionKey), false))
								others.add(RouteEntryReason.MALFUNCTION);
							break;
						case HOSTAGE:
							if (pref.getBoolean(getString(R.string.hostageKey), false))
								others.add(RouteEntryReason.HOSTAGE);
							break;
						case COMMANDEERED:
							if (pref.getBoolean(getString(R.string.commandeeredKey), false))
								others.add(RouteEntryReason.COMMANDEERED);
							break;
						default:
							break;
						}
						
						reasons = new RouteEntryReason[numMissions + others.size()];
						for (int i = 0; i < numMissions; i++) {
							reasons[i] = RouteEntryReason.MISSION;
						}
						
						System.arraycopy(
								others.toArray(new RouteEntryReason[others.size()]),
								0,
								reasons,
								numMissions,
								others.size());
						
						break;
					}
				}
				
				if (reasons.length == 0) {
					entryRows.remove(r--);
				}
				else row.setReasons(reasons);
			}
			
			// Refresh current route
			routeView.post(new Runnable() {
				@Override
				public void run() {
					routeTable.removeAllViews();
					for (RouteEntryRow row: entryRows) {
						row.updateDistance(manager.getPlayerShip(playerShip));
						row.updateReasons();
						routeTable.addView(row);
					}
				}
			});
				
			// Run this again after 1/10 of a second
			routeHandler.postDelayed(this, updateInterval);
		}
	};
	
	final Runnable updateService = new Runnable() {
		@Override
		public void run() {
			// Stop if there's no Comms service running
			if (service == null) return;
			
			// If game is running, update notifications
			if (gameRunning) {
				// Count allies
				int numAllies = 0, numBases = 0, numMissions = 0;
				for (String key: allies.keySet()) {
					int numRogues = 0;
					if (rogues.containsKey(key)) numRogues = rogues.get(key).size();
					numAllies += allies.get(key).size() - numRogues;
				}
				
				// Count stations
				for (String key: bases.keySet()) {
					numBases += bases.get(key).size();
				}
				
				// Count side missions
				for (int i = 0; i < missions.size(); i++) {
					SideMissionRow row = missions.get(i);
					if (!row.isCompleted()) {
						numMissions += row.getNumRewards();
					}
				}
				
				// Update Comms service
				try { service.update(numAllies, numBases, numMissions); }
				catch (NullPointerException e) { return; }
			}
			
			// Run this again after 1/10 of a second
			serviceHandler.postDelayed(this, updateInterval);
		}
	};
	
	final Runnable updateButtons = new Runnable() {
		@Override
		public void run() {
			Button allyViewButton = (Button) findViewById(R.id.allyViewButton);
			Button missionViewButton = (Button) findViewById(R.id.missionViewButton);
			Button stationViewButton = (Button) findViewById(R.id.stationViewButton);
			Button routeViewButton = (Button) findViewById(R.id.routeViewButton);
			
			int yellow = Color.parseColor("#bf9000");
			int orange = Color.parseColor("#c55a11");
			int bright = Color.parseColor("#f8cbad");
			
			long flashTimer = new Date().getTime();
			boolean flashOn = (flashTimer - startTime) % (flashTime << 1) < flashTime;
			
			if (missionsView.getVisibility() == View.VISIBLE) missionViewButton.setBackgroundColor(yellow);
			else if (missionFlash && flashOn) missionViewButton.setBackgroundColor(bright);
			else missionViewButton.setBackgroundColor(orange);
			
			if (alliesView.getVisibility() == View.VISIBLE) allyViewButton.setBackgroundColor(yellow);
			else if (allyFlash && flashOn) allyViewButton.setBackgroundColor(bright);
			else allyViewButton.setBackgroundColor(orange);
			
			if (stationsView.getVisibility() == View.VISIBLE) stationViewButton.setBackgroundColor(yellow);
			else if (stationFlash && flashOn) stationViewButton.setBackgroundColor(bright);
			else stationViewButton.setBackgroundColor(orange);
			
			if (routeView.getVisibility() == View.VISIBLE) routeViewButton.setBackgroundColor(yellow);
			else if (flashOn && graph != null && graph.size() > 0) routeViewButton.setBackgroundColor(bright);
			else routeViewButton.setBackgroundColor(orange);
			buttonHandler.postDelayed(this, updateInterval);
		}
	};
	
	final Runnable updateEntities = new Runnable() {
		@Override
		public void run() {
			updateRunning = true;
			allyFlash = false;
			stationFlash = false;
			for (ArtemisObject o: manager.getObjects(ObjectType.NPC_SHIP)) {
				ArtemisNpc npc = (ArtemisNpc) o;
				if (npc.getVessel(context) == null ||
						npc.getVessel(context).getFaction() == null ||
						npc.getVessel(context).getFaction().getId() > 1) continue;
				try {
					AllyStatusRow row = allies.get(npc.getName()).get(npc.getVessel(context).getName());
					row.setFront((int) npc.getShieldsFront());
					row.setRear((int) npc.getShieldsRear());
					row.updateShields();
					allyFlash |= npc.getShieldsFront() < npc.getShieldsFrontMax() ||
							npc.getShieldsRear() < npc.getShieldsRearMax();
				} catch (NullPointerException e) {}
			}
			ArtemisBase closest = null;
			for (ArtemisObject o: manager.getObjects(ObjectType.BASE)) {
				ArtemisBase base = (ArtemisBase) o;
				if (base.getVessel(context) == null) continue;
				try {
					StationStatusRow row = bases.get(base.getName()).get(base.getVessel(context).getName());
					row.setClosest(false);
					row.setShields((int) base.getShieldsFront());
					row.updateOrdnance(row.getOrdnanceText());
					stationFlash |= base.getShieldsFront() < base.getShieldsRear();
					
					if (closest == null) {
						closest = base;
						row.setClosest(true);
					} else {
						ArtemisPlayer player = manager.getPlayerShip(playerShip);
						double closestDistance = Math.hypot(
								closest.getX() - player.getX(),
								Math.hypot(
										closest.getY() - player.getY(),
										closest.getZ() - player.getZ()));
						double currentDistance = Math.hypot(
								base.getX() - player.getX(),
								Math.hypot(
										base.getY() - player.getY(),
										base.getZ() - player.getZ()));
						if (currentDistance < closestDistance) {
							for (StationStatusRow closestRow: bases.get(closest.getName()).values()) {
								closestRow.setClosest(false);
								closestRow.updateStatus(closestRow.getStatusText());
							}
							row.setClosest(true);
							closest = base;
						}
					}
					row.updateStatus(row.getStatusText());
					row.updateColor();
				} catch (NullPointerException e) {}
			}
			updateHandler.postDelayed(this, updateInterval);
		}
	};
	
	private void postStatusUpdate(final AllyStatusRow row) {
		final String statusText = row.getStatusText();
		updateHandler.post(new Runnable() {
			@Override
			public void run() {
				row.updateStatus(statusText);
			}
		});
	}
	
	private void postColorUpdate(final AllyStatusRow row) {
		updateHandler.post(new Runnable() {
			@Override
			public void run() {
				row.updateColor();
			}
		});
	}
	
	private void postStatusUpdate(final StationStatusRow row) {
		final String statusText = row.getStatusText();
		updateHandler.post(new Runnable() {
			@Override
			public void run() {
				row.updateStatus(statusText);
			}
		});
	}
	
	private void postColorUpdate(final StationStatusRow row) {
		updateHandler.post(new Runnable() {
			@Override
			public void run() {
				row.updateColor();
			}
		});
	}
	
	private void postOrdnanceUpdate(final StationStatusRow row) {
		final String stockText = row.getOrdnanceText();
		updateHandler.post(new Runnable() {
			@Override
			public void run() {
				row.updateOrdnance(stockText);
			}
		});
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (toast != null) {
			toast.setText("Updating preferences...");
			toast.setDuration(Toast.LENGTH_SHORT);
			toast.show();
		}
		if (key.equals(getString(R.string.vesselDataKey))) {
			if (assetsFail) assetsFail = false;
			else if (server != null && server.isConnected()) createConnection();
		} else if (key.endsWith("CheckBox")) {
			updateMissionsTable();
			updateAlliesTable();
			
			updateRouteGraph(false);
		} else if (key.equals(getString(R.string.allySortKey))) {
			updateAlliesTable();
			
			updateRouteGraph(false);
		}
	}
}