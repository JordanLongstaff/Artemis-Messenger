package artemis.messenger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.walkertribe.ian.ArtemisContext;
import com.walkertribe.ian.FilePathResolver;
import com.walkertribe.ian.PathResolver;
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
import com.walkertribe.ian.protocol.core.setup.VersionPacket;
import com.walkertribe.ian.protocol.core.world.DestroyObjectPacket;
import com.walkertribe.ian.protocol.core.world.DockedPacket;
import com.walkertribe.ian.protocol.core.world.ObjectUpdatePacket;
import com.walkertribe.ian.util.BoolState;
import com.walkertribe.ian.util.Version;
import com.walkertribe.ian.vesseldata.Vessel;
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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
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
	// Artemis server/game fields
	private ArtemisContext context;
	private ArtemisNetworkInterface server;
	private SystemManager manager;
	private String host, missionShip;
	private String recentHosts;
	private int heartbeatTimeout;
	private int dockingStation;
	private RoutingGraph graph;
	private Version version;
	private byte destroyedShips;
	
	// Multithreading variables
	private Thread packetThread;
	private boolean connected;
	private Handler updateHandler, routeHandler, buttonHandler;
	private boolean updateRunning, routing, routeRunning;
	private ParseMethod[] parseMethods;
	
	// Data table fields
	private CopyOnWriteArrayList<SideMissionRow> missions;
	private ConcurrentHashMap<String, CopyOnWriteArrayList<SideMissionRow>> closed;
	private ConcurrentHashMap<String, ConcurrentHashMap<String, StationStatusRow>> bases;
	private ConcurrentHashMap<String, ConcurrentHashMap<String, AllyStatusRow>> allies;
	private ConcurrentHashMap<String, CopyOnWriteArrayList<String>> rogues;
	private CopyOnWriteArrayList<String> baseKeys;
	private LinkedList<CommsIncomingPacket> inPackets;
	private LinkedList<CommsOutgoingPacket> outPackets;
	
	// Android views
	private LinearLayout missionsTable, alliesTable, stationsTable, routeTable;
	private TableLayout alliesView, stationsView, missionsView, routeView;
	private EditText addressField;
	private Button allyViewButton, missionViewButton, stationViewButton, routeViewButton;
	private Spinner shipSpinner;
	private LinearLayout addressRow;
	private TextView hintText;
	
	// Other Android fields
	private Toast toast;
	private SharedPreferences preferences;
	private boolean assetsFail;
	private ArrayAdapter<String> emptyAdapter;
	
	// Miscellaneous view-related variables
	private boolean missionFlash, allyFlash, stationFlash;
	private boolean destroyFlash;
	private long startTime;

	// Notification fields
	private NotificationCompat.Builder builder;
	private NotificationManager notManager;
	private ArrayList<String> senders, messages;
	private HashMap<String, Integer> attackMessages;
	private int numMissions, numAllies, numBases;
	private boolean serviceRunning, gameRunning;
	
	// Runnables
	private final Runnable updateHintText = new Runnable() {
		@Override
		public void run() {
			// Check if hint text should be shown - if not, hide it
			boolean connectHint = server == null || !server.isConnected();
			boolean showHint = preferences.getBoolean(getString(R.string.showHintTextKey), true);
			
			if (showHint) {
				hintText.setVisibility(View.VISIBLE);
			} else {
				hintText.setVisibility(View.GONE);
				return;
			}
			
			// Update hint text at bottom of screen based on current view
			if (connectHint) {
				hintText.setText(getString(R.string.connectHint));
			} else if (alliesView.getVisibility() == View.VISIBLE) {
				hintText.setText(getString(R.string.alliesHint));
			} else if (missionsView.getVisibility() == View.VISIBLE) {
				hintText.setText(getString(R.string.missionsHint));
			} else if (stationsView.getVisibility() == View.VISIBLE) {
				hintText.setText(getString(R.string.stationsHint));
			} else if (routeView.getVisibility() == View.VISIBLE) {
				hintText.setText(getString(R.string.routingHint));
			}
		}
	};
	
	private final Runnable topRowOrange = new Runnable() {
		@Override
		public void run() {
			// Address row turns orange
			addressRow.setBackgroundColor(clr_orange);
		}
	};
	
	private final Runnable topRowRed = new Runnable() {
		@Override
		public void run() {
			// Address row turns red
			addressRow.setBackgroundColor(clr_red);
		}
	};
	
	private final Runnable topRowGreen = new Runnable() {
		@Override
		public void run() {
			// Address row turns green
			addressRow.setBackgroundColor(clr_green);
		}
	};
	
	private final Runnable clearSpinner = new Runnable() {
		@Override
		public void run() {
			// Remove all options from the ship selector
			shipSpinner.setAdapter(emptyAdapter);
		}
	};
	
	private final Runnable clearAlliesTable = new Runnable() {
		@Override
		public void run() {
			// Empty out Allies table
			alliesTable.removeAllViews();
		}
	};
	
	private final Runnable clearRoutingTable = new Runnable() {
		@Override
		public void run() {
			// Empty out Route table
			routeTable.removeAllViews();
		}
	};
	
	private final Runnable clearMissionsTable = new Runnable() {
		@Override
		public void run() {
			// Empty out Missions table
			missionsTable.removeAllViews();
		}
	};
	
	private final Runnable clearStationsTable = new Runnable() {
		@Override
		public void run() {
			// Empty out Stations table
			stationsTable.removeAllViews();
		}
	};
	
	final Runnable updateRoute = new Runnable() {
		@Override
		public void run() {
			// If current ship does not exist, do nothing except empty table
			if (playerDestroyed()) {
				routeView.post(clearRoutingTable);
				return;
			}
			
			// If we've destroyed our graph, Route table should be empty
			if (graph == null) {
				routeView.post(clearRoutingTable);
			}
			
			// If we're not calculating routes, do nothing
			if (!routeRunning) {
				routing = false;
				return;
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
			if (route != null) entryRows = createEntryRows(route);
			else entryRows = getCurrentRows();
			checkReasons(entryRows);
			
			// Refresh current route
			routeView.post(new Runnable() {
				@Override
				public void run() {
					routeTable.removeAllViews();
					for (RouteEntryRow row: entryRows) {
						row.updateDistance(manager.getPlayerShip(),
								preferences.getBoolean(getString(R.string.threeDigitsPrefKey), false));
						row.updateReasons();
						routeTable.addView(row);
					}
				}
			});
				
			// Run this again after 1/10 of a second
			routeHandler.postDelayed(this, updateInterval);
		}
	};
	
	final Runnable updateButtons = new Runnable() {
		@Override
		public void run() {
			if (serviceRunning) {
				senders.clear();
				attackMessages.clear();
				messages.clear();
				messages.add("");
				return;
			}
			
			long flashTimer = new Date().getTime();
			boolean flashOn = (flashTimer - startTime) % (flashTime << 1) < flashTime;
			flashOn &= !playerDestroyed();
			
			if (missionsView.getVisibility() == View.VISIBLE) missionViewButton.setBackgroundColor(clr_yellow);
			else if (missionFlash && flashOn) missionViewButton.setBackgroundColor(clr_flash);
			else missionViewButton.setBackgroundColor(clr_orange);
			
			if (alliesView.getVisibility() == View.VISIBLE) allyViewButton.setBackgroundColor(clr_yellow);
			else if ((allyFlash || destroyFlash) && flashOn) allyViewButton.setBackgroundColor(clr_flash);
			else allyViewButton.setBackgroundColor(clr_orange);
			
			if (stationsView.getVisibility() == View.VISIBLE) stationViewButton.setBackgroundColor(clr_yellow);
			else if ((stationFlash) && flashOn) stationViewButton.setBackgroundColor(clr_flash);
			else stationViewButton.setBackgroundColor(clr_orange);
			
			if (routeView.getVisibility() == View.VISIBLE) routeViewButton.setBackgroundColor(clr_yellow);
			else if (flashOn && graph != null && graph.size() > 0) routeViewButton.setBackgroundColor(clr_flash);
			else routeViewButton.setBackgroundColor(clr_orange);
			
			buttonHandler.postDelayed(this, updateInterval);
		}
	};
	
	final Runnable updateService = new Runnable() {
		@Override
		public void run() {
			// If notification service isn't running, cancel all notifications and clear data
			if (!serviceRunning) {
				removeAllNotifications();
				return;
			}
			
			if (gameRunning) {
				// Count side missions
				numMissions = 0;
				for (int i = 0; i < missions.size(); i++) {
					SideMissionRow row = missions.get(i);
					if (!row.isCompleted() && isShown(row)) numMissions += row.getNumRewards();
				}
	
				// Count allies
				numAllies = 0;
				for (CharSequence key: allies.keySet()) {
					int numRogues = 0;
					if (rogues.containsKey(key)) numRogues = rogues.get(key).size();
					numAllies += allies.get(key).size() - numRogues;
				}
				
				// Count stations
				numBases = 0;
				for (CharSequence key: bases.keySet()) {
					numBases += bases.get(key).size();
				}
				
				// Set up notification showing number of missions/allies/stations
				createNotification(TITLE, getMainMessage(), 0, true);
			}
			
			buttonHandler.postDelayed(this, updateInterval);
		}
	};
	
	final Runnable updateEntities = new Runnable() {
		@Override
		public void run() {
			if (!updateRunning) return;
			
			checkForUndock();
			updateAllyShipShields();
			updateStations();
			
			updateHandler.postDelayed(this, updateInterval);
		}
	};
	
	final Runnable handlePackets = new Runnable() {
		@Override
		public void run() {
			connected = true;
			while (connected) {
				// Open inbox
				while (!inPackets.isEmpty()) {
					CommsIncomingPacket pkt = inPackets.getFirst();
					if (pkt == null) {
						continue;
					}
					inPackets.removeFirst();
					
					String sender = pkt.getFrom().toString();
					String message = pkt.getMessage().toString();
					
					// Search for applicable parse protocol
					for (ParseMethod protocol: parseMethods) {
						if (protocol.parse(sender, message))
							break;
					}
				}
					
				// Send everything from outbox
				if (!outPackets.isEmpty()) {
					if (outPackets.getFirst() == null) {
						continue;
					}
					server.send(outPackets.removeFirst());
				}
			}
		}
	};
	
	// Constants
	private static final String dataFile = "server.dat";
	private static final int updateInterval = 100;
	private static final int timeout = 9000;
	private static final int flashTime = 500;
	private static final int connectReqCode = 1;
	
	public static final String BROADCAST = "artemis.messenger.BROADCAST";
	public static final String TITLE = "Artemis Messenger";
	
	public static final Version VERSION27 = new Version("2.7.0");
	
	// Colors
	private static final int clr_red = Color.parseColor("#c00000");
	private static final int clr_orange = Color.parseColor("#c55a11");
	private static final int clr_green = Color.parseColor("#008000");
	private static final int clr_yellow = Color.parseColor("#bf9000");
	private static final int clr_flash = Color.parseColor("#f8cbad");
	
	// Static font for public access
	public static Typeface APP_FONT;
	
	// Initialize data tables
	private void initDataTables() {
		missions = new CopyOnWriteArrayList<SideMissionRow>();
		closed = new ConcurrentHashMap<String, CopyOnWriteArrayList<SideMissionRow>>();
		allies = new ConcurrentHashMap<String, ConcurrentHashMap<String, AllyStatusRow>>();
		bases = new ConcurrentHashMap<String, ConcurrentHashMap<String, StationStatusRow>>();
		baseKeys = new CopyOnWriteArrayList<String>();
		rogues = new ConcurrentHashMap<String, CopyOnWriteArrayList<String>>();
		inPackets = new LinkedList<CommsIncomingPacket>();
		outPackets = new LinkedList<CommsOutgoingPacket>();
	}
	
	// Initialize table and view fields
	private void initLayoutFields() {
		missionsTable = (LinearLayout) findViewById(R.id.missionTableRows);
		missionsView = (TableLayout) findViewById(R.id.sideMissionsTable);
		alliesTable = (LinearLayout) findViewById(R.id.allyTableRows);
		alliesView = (TableLayout) findViewById(R.id.alliesView);
		stationsTable = (LinearLayout) findViewById(R.id.stationTableRows);
		stationsView = (TableLayout) findViewById(R.id.stationsView);
		routeTable = (LinearLayout) findViewById(R.id.routeTableRows);
		routeView = (TableLayout) findViewById(R.id.routeView);
	}

	// Initialize handlers
	private void initHandlers() {
		updateHandler = new Handler();
		routeHandler = new Handler();
		buttonHandler = new Handler();
	}
	
	// Initialize view buttons and hint text view
	private void initViewButtons() {
		allyViewButton = (Button) findViewById(R.id.allyViewButton);
		missionViewButton = (Button) findViewById(R.id.missionViewButton);
		stationViewButton = (Button) findViewById(R.id.stationViewButton);
		routeViewButton = (Button) findViewById(R.id.routeViewButton);
		hintText = (TextView) findViewById(R.id.hintText);
		
		// Set on click listeners for each button
		allyViewButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				alliesView.setVisibility(View.VISIBLE);
				missionsView.setVisibility(View.GONE);
				stationsView.setVisibility(View.GONE);
				routeView.setVisibility(View.GONE);
				destroyFlash = false;
				
				updateHintText.run();
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
				
				updateHintText.run();
			}
		});
		
		stationViewButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				alliesView.setVisibility(View.GONE);
				missionsView.setVisibility(View.GONE);
				stationsView.setVisibility(View.VISIBLE);
				routeView.setVisibility(View.GONE);
				
				updateHintText.run();
			}
		});
		
		routeViewButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				alliesView.setVisibility(View.GONE);
				missionsView.setVisibility(View.GONE);
				stationsView.setVisibility(View.GONE);
				routeView.setVisibility(View.VISIBLE);
				
				updateHintText.run();
			}
		});
	}
	
	// Initialize connect, settings and help buttons
	private void initUtilityButtons() {
		ImageButton connectButton = (ImageButton) findViewById(R.id.connectButton);
		connectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (preferences.getBoolean(getString(R.string.allowUDPKey), true)) {
					startConnectActivity();
				} else {
					createConnection();
				}
			}
		});
		
		ImageButton settingsButton = (ImageButton) findViewById(R.id.settingsButton);
		settingsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
				startActivity(settingsIntent);
			}
		});
		
		ImageButton helpButton = (ImageButton) findViewById(R.id.helpButton);
		helpButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent helpIntent = new Intent(getApplicationContext(), HelpActivity.class);
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
	}
	
	// Initialize address field
	private void initAddressField() {
		addressRow = (LinearLayout) findViewById(R.id.addressRow);
		addressField = (EditText) findViewById(R.id.addressField);

		// Attempt to load previous address from data file
		try {
			FileInputStream fis = openFileInput(dataFile);
			String ip = "";
			while (true) {
				int read = fis.read();
				if (read < 0) break;
				ip += (char) read;
			}
			recentHosts = ip;
			if (recentHosts.isEmpty()) addressField.setText("");
			else addressField.setText(ip.split("\\*")[0]);
			fis.close();
		} catch (IOException e) { }
		
		// Set up address editor
		addressField.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId != EditorInfo.IME_ACTION_DONE) return false;
				createConnection();
				return true;
			}
		});
	}
	
	private void initShipSelector() {
		// Initialize ship selector
		shipSpinner = (Spinner) findViewById(R.id.shipSpinner);
		shipSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				// Wait for server
				while (server == null);
				
				// Set up Comms and Main Screen connection from selected ship
				manager.setShipIndex(position);
				server.send(new SetShipPacket(position));
				server.send(new SetConsolePacket(Console.COMMUNICATIONS, true));
				server.send(new SetConsolePacket(Console.MAIN_SCREEN, true));
				
				// Update docking status
				resetDockedRow();
				if (ListActivity.this.manager.getPlayerShip() == null) return;
				for (ArtemisObject o: ListActivity.this.manager.getObjects(ObjectType.BASE)) {
					ArtemisBase base = (ArtemisBase) o;
					if (base.getId() == ListActivity.this.manager.getPlayerShip().getDockingBase()) {
						dockingStation = base.getId();
						updateDockedRow();
						break;
					}
				}
				
				// Update table views
				clearTableViews();
				updateMissionsTable();
				updateAlliesTable();
				updateStationsTable();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) { }
		});
		
		emptyAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.ship_name_spinner);
	}
	
	private void showToast(final String text, final int length) {
		updateHandler.post(new Runnable() {
			@Override
			public void run() {
				toast.setText(text);
				toast.setDuration(length);
				toast.show();
			}
		});
	}
	
	private void startConnectActivity() {
		Intent connectIntent = new Intent(getApplicationContext(), ConnectActivity.class);
		connectIntent
		.putExtra("URL", addressField.getText().toString())
		.putExtra("Timeout", preferences.getString(getString(R.string.udpTimeoutKey), "10"))
		.putExtra("Recent", recentHosts);
		startActivityForResult(connectIntent, connectReqCode);
	}
	
	private void initFont() {
		if (APP_FONT == null)
			APP_FONT = Typeface.createFromAsset(getAssets(), "fonts/Rajdhani-Medium.ttf");
		
		// Update app font
		LinearLayout appLayout = (LinearLayout) findViewById(R.id.appLayout);
		updateFont(appLayout);
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
	
	// Initialize notification builder and tables
	private void initNotifications() {
		builder = new NotificationCompat.Builder(getApplicationContext())
				.setSmallIcon(R.drawable.ic_launcher);
		notManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		senders = new ArrayList<String>();
		messages = new ArrayList<String>();
		attackMessages = new HashMap<String, Integer>();
	}
	
	// Tell notification manager to set up a notification
	private void createNotification(String title, String message, int index, boolean ongoing) {
		Intent intent = new Intent(getApplicationContext(), ListActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pending = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
		
		builder
		.setStyle(new NotificationCompat.BigTextStyle()
				.bigText(message)
				.setBigContentTitle(title))
		.setContentTitle(title)
		.setContentText(message)
		.setContentIntent(pending)
		.setOngoing(ongoing);
		notManager.notify(index, builder.build());
	}
	
	// Remove all notifications
	private void removeAllNotifications() {
		senders.clear();
		attackMessages.clear();
		messages.clear();
		messages.add("");
		notManager.cancelAll();
	}
	
	// Method to empty out all data in server data tables
	private void clearDataTables() {
		destroyedShips = 0;
		missions.clear();
		for (CopyOnWriteArrayList<SideMissionRow> list: closed.values()) list.clear();
		for (ConcurrentHashMap<String, StationStatusRow> map: bases.values()) map.clear();
		for (ConcurrentHashMap<String, AllyStatusRow> map: allies.values()) map.clear();
		for (CopyOnWriteArrayList<String> list: rogues.values()) list.clear();
		closed.clear();
		bases.clear();
		allies.clear();
		rogues.clear();
		baseKeys.clear();
	}
	
	// Returns whether or not the current player ship, if it exists, is destroyed
	private boolean playerDestroyed() {
		return manager != null && manager.getPlayerShip() != null &&
				(destroyedShips & (1 << manager.getPlayerShip().getShipIndex())) != 0;
	}
	
	// Create a path resolver for vesselData.xml, location is according to preferences
	private PathResolver createPathResolver() {
		int resolverIndex = Integer.parseInt(preferences.getString(getString(R.string.vesselDataKey), "0"));
		
		if (resolverIndex == 0) {
			// If it's the app assets, point there
			return new AssetsResolver(getAssets());
		} else {
			// Otherwise, point to location and unpack from assets if necessary
			showToast("Unpacking assets\u2026", Toast.LENGTH_SHORT);
			
			// Get storage directory
			File[] storage = ContextCompat.getExternalFilesDirs(getApplicationContext(), null);
			File storageLoc = storage[resolverIndex - 1];
			String filesDir = storageLoc.getAbsolutePath() + "/dat";
			
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
				return new FilePathResolver(storageLoc.getAbsolutePath());
			} catch (IOException e) {
				// If something went wrong, fall back on assets
				assetsFail = true;
				showToast("Failed to unpack assets, switching location to Default\u2026", Toast.LENGTH_SHORT);
				preferences.edit().putString(getString(R.string.vesselDataKey), "0").commit();
				return new AssetsResolver(getAssets());
			}
		}
	}
	
	private void writeRecentHostsToFile() {
		try {
			// Write URL to data
			FileOutputStream fos = openFileOutput(dataFile, Context.MODE_PRIVATE);
			fos.write(recentHosts.getBytes());
			fos.close();
		} catch (IOException ex) { }
	}
	
	/**
	 * Sets up a connection to a running Artemis server.
	 */
	private void createConnection() {
		// If a server was running, stop it
		endServer();
		
		// Get URL
		final String url = addressField.getText().toString();
		
		// If one was already in use, clear tables and prep for new connection
		clearTableViews();
		
		// Find selected vesselData.xml location
		PathResolver resolver = createPathResolver();
		context = new com.walkertribe.ian.DefaultContext(resolver);
		final int port = Integer.parseInt(preferences.getString(getString(R.string.serverPortKey), "2010"));
		
		showToast("Connecting to " + url + "\u2026", Toast.LENGTH_LONG);
		
		// Try setting up a connection
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					server = new ThreadedArtemisNetworkInterface(url, port, timeout);
					server.addListener(ListActivity.this);
					
					manager = new SystemManager();
					server.addListener(manager);
					
					server.start();
				} catch (IOException e) {
					showToast("Connection failed", Toast.LENGTH_LONG);
					addressRow.post(topRowRed);
					shipSpinner.post(clearSpinner);
				} catch (NullPointerException e) {
				}
			}
		}).start();
		addressRow.post(topRowOrange);
	}
	
	// Terminates a server connection
	private void endServer() {
		if (server != null) {
			server.stop();
			server = null;
			host = null;
			inPackets.clear();
			outPackets.clear();
			updateHandler.post(updateHintText);
		}
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

		// Update current host and recent hosts
		host = addressField.getText().toString();

		// Update recent hosts
		if (recentHosts.isEmpty()) {
			recentHosts = host;
		} else {
			String[] split = recentHosts.split("\\*");
			ArrayList<String> hostList = new ArrayList<String>(split.length);
			for (String h: split) hostList.add(h);
			if (hostList.contains(host)) {
				hostList.remove(host);
			}
			recentHosts = host;
			int recentLimit = Integer.parseInt(preferences.getString(getString(R.string.recentHostsKey), "10"));
			for (String h: hostList) {
				if (--recentLimit <= 0) break;
				if (!recentHosts.isEmpty()) recentHosts += "*";
				recentHosts += h;
			}
		}
		writeRecentHostsToFile();

		addressRow.post(topRowGreen);
		updateMissionsTable();
		updateStationsTable();
		updateAlliesTable();
		updateHandler.post(updateHintText);
		
		// Clear all current server data
		clearDataTables();
		
		// Set start time
		startTime = new Date().getTime();
 
		// Start packet handler
		if (packetThread == null) {
			packetThread = new Thread(handlePackets);
			packetThread.start();
		}
	}
	
	/**
	 * Called when the client disconnects from the server.
	 * @param event the disconnect event
	 */
	@Listener
	public void onDisconnect(DisconnectEvent event) {
		// Terminate packet thread
		if (packetThread != null) {
			connected = false;
			packetThread = null;
		}
		
		// Shut down packet handler
		inPackets.clear();
		outPackets.clear();
		
		// Stop all handlers
		updateRunning = false;
		dockingStation = -1;
		routeRunning = false;
		gameRunning = false;
		
		// Stop buttons flashing
		missionFlash = false;
		allyFlash = false;
		stationFlash = false;
		destroyFlash = false;
		
		// Clean up Routing graph
		graph = null;
		
		// If it was a local disconnect, do nothing else
		if (event.getCause() == DisconnectEvent.Cause.LOCAL_DISCONNECT) return;

		// Show reason for disconnect
		String cause = event.getCause().name().replaceAll("_", " ");
		if (toast != null) showToast(cause, Toast.LENGTH_LONG);
		
		// Shut down connection
		if (serviceRunning) {
			// Destroy all notifications
			removeAllNotifications();
			
			// Set up a notification about the disconnect event
			createNotification(TITLE, cause, 0, false);
		}
		
		// Clear server data
		endServer();
		clearDataTables();
		numMissions = 0;
		numAllies = 0;
		numBases = 0;
		clearTableViews();
		shipSpinner.post(clearSpinner);
		addressRow.post(topRowRed);
		hintText.post(updateHintText);
	}
	
	/**
	 * Called when a game over reason packet is received by the client.
	 * @param pkt the game over reason packet
	 */
	@Listener
	public void onPacket(GameOverReasonPacket pkt) {
		// Disable heartbeat dependency
		try { server.setTimeout(0); }
		catch (IOException e) { endServer(); }
		
		// Signal end of game
		gameRunning = false;
		routeRunning = false;
		
		// Stop buttons flashing
		missionFlash = false;
		allyFlash = false;
		stationFlash = false;
		destroyFlash = false;
		
		// Clean up Routing graph
		graph = null;
		
		// Stop Comms service
		if (serviceRunning) {
			// Destroy all notifications
			removeAllNotifications();
			
			// Set up a notification about the game over event
			StringBuilder b = new StringBuilder();
			for (CharSequence line: pkt.getText()) b.append("\n" + line);
			createNotification(TITLE, b.substring(14), 0, false);
		}
		
		// Clear current data
		clearDataTables();
		inPackets.clear();
		outPackets.clear();
		dockingStation = -1;
		clearTableViews();
	}
	
	/**
	 * Called whenever an object update packet gives an update to a docking station.
	 */
	@Listener
	public void onObjectUpdate(final ArtemisBase base) {
		Vessel baseVessel = base.getVessel(context);
		
		// Skip enemy stations (in Border War or Deep Strike)
		if (baseVessel == null || baseVessel.getFaction() == null || baseVessel.getFaction().getId() > 1)
			return;
		
		// Skip stations we already know about
		final String baseName = base.getName().toString();
		final String vesselName = baseVessel.getFullName();
		if (bases.containsKey(baseName)) return;
		
		// Set up table row in Stations view
		bases.put(baseName, new ConcurrentHashMap<String, StationStatusRow>());
		updateHandler.post(new Runnable() {
			@Override
			public void run() {
				synchronized (bases) {
					final StationStatusRow row = new StationStatusRow(getApplicationContext(), base, context);
					row.setOnClickListener(StationStatusRow.STATUS_TEXT_COLUMN, new OnClickListener() {
						@Override
						public void onClick(View v) {
							outPackets.add(new CommsOutgoingPacket(
									base, BaseMessage.STAND_BY_FOR_DOCKING, context));
						}
					});
					row.setOnClickListener(StationStatusRow.ORDNANCE_TEXT_COLUMN, new OnClickListener() {
						@Override
						public void onClick(View v) {
							outPackets.add(new CommsOutgoingPacket(
									base, BaseMessage.build(row.getOrdnanceType().next()), context));
						}
					});
					bases.get(baseName).put(vesselName, row);
					bases.notifyAll();
				}
			}
		});
		
		// Send status report request
		outPackets.add(new CommsOutgoingPacket(base, BaseMessage.PLEASE_REPORT_STATUS, context));
		
		// Wait for row to be added to memory
		synchronized (bases) {
			while (!bases.get(baseName).containsKey(vesselName)) {
				try { bases.wait(); }
				catch (InterruptedException e) { }
			}
		}

		// Add table row to Stations view
		final StationStatusRow row = bases.get(baseName).get(vesselName);
		final int rowIndex = insertBaseKey(baseName);
		stationsView.post(new Runnable() {
			@Override
			public void run() {
				try { stationsTable.addView(row, rowIndex); }
				catch (IllegalStateException e) { }
			}
		});
	}
	
	// Calculate index of where to insert a StationStatusRow using binary search
	private int insertBaseKey(String name) {
		int index = 0;
		int minIndex = 0, maxIndex = baseKeys.size();
		while (minIndex < maxIndex) {
			index = (maxIndex + minIndex) / 2;
			String rowName = name;
			String otherName = baseKeys.get(index);
			
			// If station names both start with DS, sort by number
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
		baseKeys.add(index, name);
		return index;
	}
	
	/**
	 * Called whenever an object update packet gives an update to an NPC ship.
	 */
	@Listener
	public void onObjectUpdate(final ArtemisNpc npc) {
		// Skip enemy ships
		Vessel npcVessel = npc.getVessel(context);
		if (npc.getVessel(context) == null ||
				npc.getVessel(context).getFaction() == null ||
				npc.getVessel(context).getFaction().getId() > 1) return;
		
		// Skip fighters
		if (npcVessel.is(VesselAttribute.FIGHTER) || npcVessel.is(VesselAttribute.SINGLESEAT)) return;
		
		// Skip NPCs we already know about
		final String allyName = npc.getName().toString();
		final String shipName = npcVessel.getFullName();
		if (allies.containsKey(allyName)) return;
		
		// Set up ally table entry
		allies.put(allyName, new ConcurrentHashMap<String, AllyStatusRow>());
		
		// Set up a rogues table entry in case we discover it's a trap
		if (!rogues.containsKey(allyName))
			rogues.put(allyName, new CopyOnWriteArrayList<String>());
		
		// Set up table row in Allies view
		updateHandler.post(new Runnable() {
			@Override
			public void run() {
				final AllyStatusRow row = new AllyStatusRow(getApplicationContext(),
						npc,
						npc.getName() + " " + shipName);
				row.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						outPackets.add(new CommsOutgoingPacket(npc, OtherMessage.HAIL, context));
					}
				});
				allies.get(allyName).put(shipName, row);
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
	
	/**
	 * Called whenever an object update packet gives an update to a player ship.
	 */
	@Listener
	public void onObjectUpdate(ArtemisPlayer player) {
		// Check if we are docked with a station
		if (manager.getPlayerShip() != null && dockingStation == -1) {
			if (player.getId() == manager.getPlayerShip().getId() && player.getDockingBase() > 0) {
				for (ArtemisObject baseObj: manager.getObjects(ObjectType.BASE)) {
					ArtemisBase b = (ArtemisBase) baseObj;
					if (b.getId() == player.getDockingBase()) {
						dockingStation = b.getId();
						updateDockedRow();
						break;
					}
				}
			}
		}
		
		// Prepare Routing graph
		if (graph == null && manager.getPlayerShip() != null) {
			if (player == null || player.getId() != manager.getPlayerShip().getId()) return;
			graph = new RoutingGraph(manager, player.getId());
		}
		
		// Start up object update handler
		if (!updateRunning) {
			updateRunning = true;
			updateHandler.postDelayed(updateEntities, updateInterval);
		}
		
		// Start up routing algorithm handler
		if (!routeRunning) {
			routeRunning = true;
			routeHandler.postDelayed(updateRoute, updateInterval);
		}
		
		if (manager.getPlayerShip() != null) {
			String newShip = manager.getPlayerShip().getName().toString();
			String oldShip = ObjectStatusRow.setCurrentShip(newShip);
			if (newShip.equals(oldShip)) return;
			
			// Update object status rows based on their missions
			HashSet<String> updated = new HashSet<String>();
			for (SideMissionRow row: missions) {
				if (row.isCompleted()) continue;
				if (!row.getPlayerShip().equals(oldShip) && !row.getPlayerShip().equals(newShip)) continue;
				String destination = row.getDestination();
				if (updated.contains(destination)) continue;
				String[] destParts = destination.split(" ", 2);
				String name = destParts[0];
				String title = destParts[1];
				ObjectStatusRow objRow = null;
				if (allies.containsKey(name)) objRow = allies.get(name).get(title);
				else if (bases.containsKey(name)) objRow = bases.get(name).get(title);
				if (objRow == null) continue;
				updated.add(destination);
				postStatusUpdate(objRow);
			}
		}
	}
	
	/**
	 * Called whenever an object update packet is received by the client. This is how the client knows a simulation has
	 * started.
	 * @param pkt the object update packet
	 */
	@Listener
	public void onPacket(final ObjectUpdatePacket pkt) {
		// Activate heartbeat dependency
		try { server.setTimeout(heartbeatTimeout); }
		catch (IOException e) { endServer(); }
		
		// Signal start of game
		gameRunning = true;
	}
    
    @Listener
    public void onPacket(DockedPacket pkt) {
    	ArtemisObject obj = manager.getObject(pkt.getObjectId());
    	if (obj instanceof ArtemisPlayer) {
    		ArtemisPlayer plr = (ArtemisPlayer) obj;
    		plr.setDocked(BoolState.TRUE);
    		
    		ArtemisBase base = (ArtemisBase) manager.getObject(plr.getDockingBase());
    		if (base != null) dockingStation = base.getId();
    		updateDockedRow();
    	}
    }
	
	/**
	 * Called when a ship settings packet is received by a client. This occurs upon connection to a server or when a
	 * ship's name is changed. The ship selection box is populated here.
	 * @param pkt the ship settings packet
	 */
	@Listener
	public void onPacket(AllShipSettingsPacket pkt) {
		final int selIndex = shipSpinner.getSelectedItemPosition();
		
		// Set up app font in selection box and list
		final ArrayAdapter<CharSequence> adapter =
				new ArrayAdapter<CharSequence>(getApplicationContext(), R.layout.ship_name_spinner) {
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
		for (int i = 0; i < Artemis.SHIP_COUNT; i++) {
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
	 * Called whenever a base is destroyed.
	 */
	private void onObjectDestroyed(ArtemisBase base) {
		// Skip enemy stations (in Border War and Deep Strike)
		Vessel baseVessel = base.getVessel(context);
		if (baseVessel == null || baseVessel.getFaction() == null || baseVessel.getFaction().getId() > 1) return;
		
		// Remove from Stations table
		String baseName = base.getName().toString();
		if (bases.containsKey(baseName)) {
			for (final StationStatusRow row: bases.get(baseName).values()) {
				stationsView.post(new Runnable() {
					@Override
					public void run() {
						stationsTable.removeView(row);
					}
				});
			}
			bases.get(baseName).clear();
			bases.remove(baseName);
			baseKeys.remove(baseName);
		}
	}
	
	/**
	 * Called whenever an ally ship is destroyed.
	 */
	private void onObjectDestroyed(ArtemisNpc npc) {
		// Skip enemy ships
		Vessel npcVessel = npc.getVessel(context);
		if (npcVessel == null || npcVessel.getFaction() == null || npcVessel.getFaction().getId() > 1) return;
		
		// Get names
		String allyName = npc.getName().toString();
		String shipName = npcVessel.getFullName();
		
		// Set ship's row to DESTROYED status, zero out shields
		if (allies.containsKey(allyName)) {
			final AllyStatusRow row = allies.get(allyName).get(shipName);
			row.setStatus(AllyStatus.DESTROYED);
			destroyFlash = alliesView.getVisibility() != View.GONE;
			row.setFront(0);
			row.setRear(0);
			postStatusUpdate(row);
			postColorUpdate(row);
			
			// Remove the row if we're not showing destroyed allies
			final boolean show = preferences.getBoolean(getString(R.string.showDestroyedKey), true);
			updateHandler.post(new Runnable() {
				@Override
				public void run() {
					try {
						alliesTable.removeView(row);
						if (show) {
							row.updateShields();
							alliesTable.addView(row);
						}
					} catch (IllegalStateException e) { }
				}
			});
		}
		
		// If it's a trap, remove from rogues table
		if (rogues.containsKey(allyName)) {
			rogues.get(allyName).remove(shipName);
			if (rogues.get(allyName).isEmpty()) rogues.remove(allyName);
		}
	}
	
	/**
	 * Called whenever a player ship is destroyed.
	 */
	private void onObjectDestroyed(ArtemisPlayer plr) {
		// If the game's over, nothing to do
		if (!gameRunning) return;

		// Mark player ship destruction
		destroyedShips |= 1 << plr.getShipIndex();
		
		// If it was the player ship we were on, we'll get kicked out of the game
		if (plr.getId() == manager.getPlayerShip().getId()) {
			// Clear current data
			dockingStation = -1;
			clearTableViews();
		}
		
		if (version.ge(VERSION27)) {
			// Cancel their exclusive side missions
			String name = plr.getName().toString();
			for (int i = 0; i < missions.size(); i++) {
				final SideMissionRow row = missions.get(i);
				if (row.getPlayerShip().equals(name)) {
					missions.remove(i--);
					if (!row.isCompleted()) {
						ObjectStatusRow destRow = null;
						String[] destParts = row.getDestination().split(" ", 2);
						if (allies.containsKey(destParts[0]))
							destRow = allies.get(destParts[0]).get(destParts[1]);
						else if (bases.containsKey(destParts[0]))
							destRow = bases.get(destParts[0]).get(destParts[1]);
						if (destRow != null) {
							destRow.removeMission(row);
							postStatusUpdate(destRow);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Called when a destroyed object packet is received by the client.
	 * @param pkt the destroyed object packet
	 */
	@Listener
	public void onPacket(DestroyObjectPacket pkt) {
		// Get destroyed object
		final ArtemisObject object = manager.getObject(pkt.getTarget());
		if (object == null || object.getType() == null)
			return;
		
		switch (object.getType()) {
		case BASE:
			onObjectDestroyed((ArtemisBase) object);
			break;
		case NPC_SHIP:
			onObjectDestroyed((ArtemisNpc) object);
			break;
		case PLAYER_SHIP:
			onObjectDestroyed((ArtemisPlayer) object);
			break;
		default:
			// If neither a base nor an ally ship, exit
			return;
		}
		
		// If it was a base or an ally ship, remove all affiliated side missions
		for (int i = 0; i < missions.size(); i++) {
			final SideMissionRow row = missions.get(i);
			
			// Skip missions that are already completed
			if (row.isCompleted()) continue;
			
			// Check if this object is affiliated with this mission
			String objName = object.getName().toString();
			if ((!row.isStarted() && row.getSource().startsWith(objName)) ||
					row.getDestination().startsWith(objName)) {
				try {
					// If mission wasn't started, one less mission for entity that wasn't destroyed
					if (!row.isStarted()) {
						String[] other = null;
						if (row.getSource().startsWith(objName)) {
							other = row.getDestination().split(" ", 2);
						} else if (row.getDestination().startsWith(objName)) {
							other = row.getSource().split(" ", 2);
						}
						if (other != null) {
							for (int n = 0; n < row.getNumRewards(); n++) {
								try {
									ObjectStatusRow otherRow = null;
									if (allies.containsKey(other[0])) {
										otherRow = allies.get(other[0]).get(other[1]);
									} else if (bases.containsKey(other[0])) {
										otherRow = bases.get(other[0]).get(other[1]);
									}
									otherRow.removeMission(row);
									postStatusUpdate(otherRow);
								} catch (NullPointerException e) { }
							}
						}
					}
					
					// Remove row from Missions table
					missionsView.post(new Runnable() {
						@Override
						public void run() {
							try { missionsTable.removeView(row); }
							catch (IllegalStateException e) { }
						}
					});
				} catch (Exception e) { }
				missions.remove(i--);
				
				// Designate mission as uncompletable
				if (object instanceof ArtemisNpc && closed.containsKey(objName))
					closed.get(objName).add(row);
			}
		}
		
		// Tell Missions button to stop flashing, maybe
		boolean stopFlash = true;
		for (int i = 0; i < missions.size(); i++) {
			if (!missions.get(i).isCompleted()) {
				stopFlash = false;
				break;
			}
		}
		missionFlash &= !stopFlash;
		
		// Remove point/missions from route
		updateRouteGraph(false);
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
		for (CharSequence key: bases.keySet()) {
			for (StationStatusRow row: bases.get(key).values()) {
				row.setPaused(pkt.getPaused().getBooleanValue());
			}
		}
	}
	
	/**
	 * Called when a VersionPacket is received from the client. Some functions change depend on the server version.
	 */
	@Listener
	public void onPacket(VersionPacket pkt) {
		version = pkt.getVersion();
	}
	
	@Message(ParseProtocol.MALFUNCTION)
	public boolean parseMalfunction(String sender, String message) {
		if (!message.contains("\nOur shipboard computer")) return false;
		
		// Ally ship has malfunctioning computer
		final String id = sender.substring(sender.lastIndexOf(" ") + 1);
		final String name = sender.split(" " + id)[0];
		if (allies.containsKey(id)) {
			for (String key: allies.get(id).keySet()) {
				if (!key.endsWith(name)) continue;
				AllyStatusRow row = allies.get(id).get(key);
				row.setStatus(AllyStatus.BROKEN_COMPUTER);
				row.setEnergy(message.endsWith("you need some."));
				postStatusUpdate(row);
				postColorUpdate(row);
				updateAlliesTable();
			}
		}
		
		// Update the Routing graph
		updateRouteGraph(preferences.getBoolean(getString(R.string.malfunctionKey), false));
		
		return true;
	}
	
	@Message(ParseProtocol.STANDBY)
	public boolean parseStandby(String sender, String message) {
		if (!message.startsWith("Docking crew")) return false;
		
		// Make sure this ship is the correct one
		if (message.split(", ")[1].startsWith(manager.getPlayerShip().getName().toString())) {
			for (ArtemisObject o: manager.getObjects(ObjectType.BASE)) {
				final String[] senderParts = sender.split(" ", 2);
				if (o.getName().toString().startsWith(senderParts[0]) && bases.containsKey(senderParts[0])) {
					final StationStatusRow row = bases.get(senderParts[0]).get(senderParts[1]);
					row.setReady(true);
					postStatusUpdate(row);
				}
			}
		}
		
		return true;
	}
	
	@Message(ParseProtocol.PRODUCTION)
	public boolean parseProduction(String sender, String message) {
		if (!message.startsWith("We've produced") && !message.contains("ing production of")) return false;
		
		// Production of previous ordnance ended
		String base = sender.split(" ")[0];
		if (bases.containsKey(base)) {
			for (StationStatusRow row: bases.get(base).values()) {
				// If a new missile was produced...
				if (message.startsWith("We've")) {
					// Recalibrate production speed
					row.recalibrateSpeed();

					if (serviceRunning) {
						// Play "new mission" ringtone
						Uri ringtoneUri =
								Uri.parse(preferences.getString(getString(R.string.productionPrefKey), "Default"));
						Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
						ringtone.play();

						// Create notification containing message
						createNotification(sender, message, messages.size(), false);
						
						// Add information to appropriate lists
						senders.add(sender);
						messages.add(message);
					}
				}
				row.resetBuildProgress();
				row.resetMissile();
			}
		}
		
		// Commencing production of a new ordnance
		for (ArtemisObject o: manager.getObjects(ObjectType.BASE)) {
			if (!sender.startsWith(o.getName().toString())) continue;
			outPackets.add(new CommsOutgoingPacket(o, BaseMessage.PLEASE_REPORT_STATUS, context));
			break;
		}
		
		return true;
	}
	
	@Message(ParseProtocol.FIGHTER)
	public boolean parseFighter(String sender, String message) {
		if (!message.startsWith("We had an extra")) return false;
		
		// Station reimburses fighter
		String base = sender.split(" ")[0];
		if (bases.containsKey(base)) {
			for (StationStatusRow row: bases.get(base).values()) {
				row.incFighters(-1);
				postStatusUpdate(row);
			}
		}
		
		return true;
	}
	
	@Message(ParseProtocol.ORDNANCE)
	public boolean parseOrdnance(String sender, String message) {
		if (!message.contains("Our stores are")) return false;
		
		// Docking station status report
		final String[] list = message.split("\n");
		final int shields = Integer.parseInt(list[0].split("at ")[1].split(" ")[0]);
		final String[] senderParts = sender.split(" ", 2);
		final StationStatusRow row = bases.get(senderParts[0]).get(senderParts[1]);
		row.setShields(shields);
		for (OrdnanceType type: OrdnanceType.ordnances()) {
			row.setStock(type, Integer.parseInt(list[type.ordinal()+1].split("of")[0].trim()));
			if (list[list.length - 1].contains(type.toString())) {
				String[] lastWords = list[list.length - 1].split(" ");
				int minutes = Integer.parseInt(lastWords[lastWords.length - 2]);
				row.setBuildTime(minutes);
				row.setOrdnanceType(type);
				if (version.ge(VERSION27)) {
					row.resetMissile();
					row.setOrdnanceType(type);
					row.reconcileSpeed(minutes);
				}
			}
		}
		int ordnances = OrdnanceType.ordnances().size();
		if (list.length > 2 + ordnances) row.setFighters(Integer.parseInt(list[1 + ordnances].split(" ")[1]));
		postStatusUpdate(row);
		postColorUpdate(row);
		postOrdnanceUpdate(row);
		
		return true;
	}
	
	@Message(ParseProtocol.HOSTAGE)
	public boolean parseHostage(String sender, String message) {
		if (!message.contains("\nWe are holding this ship")) return false;
		
		// Ally ship taken hostage
		final String id = sender.substring(sender.lastIndexOf(" ") + 1);
		final String name = sender.split(" " + id)[0];
		if (allies.containsKey(id)) {
			for (String key: allies.get(id).keySet()) {
				if (!key.endsWith(name)) continue;
				AllyStatusRow row = allies.get(id).get(key);
				row.setStatus(AllyStatus.HOSTAGE);
				row.setEnergy(message.endsWith("you need some."));
				postStatusUpdate(row);
				postColorUpdate(row);
				updateAlliesTable();
			}
		}
		
		// Update the Routing graph
		updateRouteGraph(preferences.getBoolean(getString(R.string.hostageKey), false));
		
		return true;
	}
	
	@Message(ParseProtocol.COMMANDEERED)
	public boolean parseCommandeered(String sender, String message) {
		if (!message.contains("\nWe have commandeered")) return false;
		
		// Ally ship commandeered
		final String id = sender.substring(sender.lastIndexOf(" ") + 1);
		final String name = sender.split(" " + id)[0];
		if (allies.containsKey(id)) {
			for (String key: allies.get(id).keySet()) {
				if (!key.endsWith(name)) continue;
				AllyStatusRow row = allies.get(id).get(key);
				row.setStatus(AllyStatus.COMMANDEERED);
				row.setEnergy(message.endsWith("you need some."));
				postStatusUpdate(row);
				postColorUpdate(row);
				updateAlliesTable();
			}
		}
		
		return true;
	}
	
	@Message(ParseProtocol.FLYING_BLIND)
	public boolean parseFlyingBlind(String sender, String message) {
		if (!message.contains("\nOur sensors are all down")) return false;
		
		// Ally ship flying blind
		final String id = sender.substring(sender.lastIndexOf(" ") + 1);
		final String name = sender.split(" " + id)[0];
		if (allies.containsKey(id)) {
			for (String key: allies.get(id).keySet()) {
				if (!key.endsWith(name)) continue;
				AllyStatusRow row = allies.get(id).get(key);
				row.setStatus(AllyStatus.FLYING_BLIND);
				row.setEnergy(message.endsWith("you need some."));
				postStatusUpdate(row);
				postColorUpdate(row);
				updateAlliesTable();
			}
		}
		
		return true;
	}
	
	@Message(ParseProtocol.AMBASSADOR)
	public boolean parseAmbassador(String sender, String message) {
		if (!message.contains("\nWe're dead")) return false;
		
		// Ally ship has ambassador
		final String id = sender.substring(sender.lastIndexOf(" ") + 1);
		final String name = sender.split(" " + id)[0];
		if (allies.containsKey(id)) {
			for (String key: allies.get(id).keySet()) {
				if (!key.endsWith(name)) continue;
				AllyStatusRow row = allies.get(id).get(key);
				row.setStatus(AllyStatus.AMBASSADOR);
				row.setEnergy(message.endsWith("you need some."));
				postStatusUpdate(row);
				postColorUpdate(row);
				updateAlliesTable();
			}
		}
		
		// Update the Routing graph
		updateRouteGraph(preferences.getBoolean(getString(R.string.ambassadorKey), false));
		
		return true;
	}
	
	@Message(ParseProtocol.CARRYING_SUPPLIES)
	public boolean parseContraband(String sender, String message) {
		if (!message.contains("clear our way.") && !message.contains("\nHail, Bold")) return false;
		
		// Ally ship has Pirate contraband
		final String id = sender.substring(sender.lastIndexOf(" ") + 1);
		final String name = sender.split(" " + id)[0];
		if (allies.containsKey(id)) {
			for (String key: allies.get(id).keySet()) {
				if (!key.endsWith(name)) continue;
				AllyStatusRow row = allies.get(id).get(key);
				row.setStatus(AllyStatus.PIRATE_SUPPLIES);
				row.setEnergy(message.endsWith("you need some."));
				row.setPirateAware(message.contains("Hail, Bold"));
				postStatusUpdate(row);
				postColorUpdate(row);
				updateAlliesTable();
			}
		}
		
		return true;
	}
	
	@Message(ParseProtocol.CONTRABAND)
	public boolean parseHeaveTo(String sender, String message) {
		if (!message.startsWith("Heave to")) return false;
		
		// Ally ship caught with Pirate contraband
		final String id = message.split(", ")[1].split("!")[0];
		if (allies.containsKey(id)) {
			for (AllyStatusRow row: allies.get(id).values()) {
				row.setStatus(AllyStatus.NORMAL);
				postStatusUpdate(row);
				postColorUpdate(row);
			}
			updateAlliesTable();
		}
		
		return true;
	}
	
	@Message(ParseProtocol.SECURE_DATA)
	public boolean parseSecureData(String sender, String message) {
		if (!message.contains("secure") && !message.contains("\nPirate scum")) return false;
		
		// Ally ship has secret data
		final String id = sender.substring(sender.lastIndexOf(" ") + 1);
		final String name = sender.split(" " + id)[0];
		if (allies.containsKey(id)) {
			for (String key: allies.get(id).keySet()) {
				if (!key.endsWith(name)) continue;
				AllyStatusRow row = allies.get(id).get(key);
				row.setStatus(AllyStatus.PIRATE_DATA);
				row.setEnergy(message.endsWith("you need some."));
				row.setPirateAware(message.contains("scum"));
				postStatusUpdate(row);
				postColorUpdate(row);
				updateAlliesTable();
			}
		}
		
		return true;
	}
	
	@Message(ParseProtocol.NEED_DAMCON)
	public boolean parseNeedsDamcon(String sender, String message) {
		if (!message.contains("\nOur engines are damaged")) return false;
		
		// Ally ship needs DamCon team
		final String id = sender.substring(sender.lastIndexOf(" ") + 1);
		final String name = sender.split(" " + id)[0];
		if (allies.containsKey(id)) {
			for (String key: allies.get(id).keySet()) {
				if (!key.endsWith(name)) continue;
				AllyStatusRow row = allies.get(id).get(key);
				row.setStatus(AllyStatus.NEED_DAMCON);
				row.setEnergy(message.endsWith("you need some."));
				postStatusUpdate(row);
				postColorUpdate(row);
				updateAlliesTable();
			}
		}
		
		// Update the Routing graph
		updateRouteGraph(preferences.getBoolean(getString(R.string.needDamConKey), false));
		
		return true;
	}
	
	@Message(ParseProtocol.NEED_ENERGY)
	public boolean parseNeedsEnergy(String sender, String message) {
		if (!message.contains("\nWe're out of energy")) return false;
		
		// Ally ship needs energy
		final String id = sender.substring(sender.lastIndexOf(" ") + 1);
		final String name = sender.split(" " + id)[0];
		if (allies.containsKey(id)) {
			for (String key: allies.get(id).keySet()) {
				if (!key.endsWith(name)) continue;
				AllyStatusRow row = allies.get(id).get(key);
				row.setStatus(AllyStatus.NEED_ENERGY);
				postStatusUpdate(row);
				postColorUpdate(row);
				updateAlliesTable();
			}
		}
		
		// Update the Routing graph
		updateRouteGraph(preferences.getBoolean(getString(R.string.needEnergyKey), false));
		
		return true;
	}
	
	@Message(ParseProtocol.TORPEDO_TRANSFER)
	public boolean parseTorpedo(String sender, String message) {
		if (!message.startsWith("Torpedo transfer")) return false;
		
		// Ally ship gives you torpedoes in Deep Strike
		if (allies.containsKey(sender)) {
			for (AllyStatusRow row: allies.get(sender).values()) {
				row.setTorpedoes(false);
				postStatusUpdate(row);
			}
		}
		
		return true;
	}
	
	@Message(ParseProtocol.ENERGY_TRANSFER)
	public boolean parseGivingEnergy(String sender, String message) {
		if (!message.startsWith("Here's the energy")) return false;
		
		// Ally ship gives you energy
		if (allies.containsKey(sender)) {
			for (AllyStatusRow row: allies.get(sender).values()) {
				row.setEnergy(false);
				postStatusUpdate(row);
			}
			updateAlliesTable();
		}
		
		return true;
	}
	
	@Message(ParseProtocol.DELIVERING_REWARD)
	public boolean parseDeliveringReward(String sender, String message) {
		if (!message.endsWith("when we get there.")) return false;
		
		// Ally ship delivering reward
		if (allies.containsKey(sender)) {
			for (AllyStatusRow row: allies.get(sender).values()) {
				row.setStatus(AllyStatus.REWARD);
				postStatusUpdate(row);
				postColorUpdate(row);
			}
			updateAlliesTable();
		}
		
		return true;
	}
	
	@Message(ParseProtocol.TRAP)
	public boolean parseTrap(String sender, String message) {
		boolean howAreYou = message.contains("How are you?");
		if (!howAreYou && !message.contains("We're broken down!")) return false;
		
		// Ally ship is actually a trap
		final String id = sender.substring(sender.lastIndexOf(" ") + 1);
		final String name = sender.split(" " + id)[0];
		if (!rogues.containsKey(id)) rogues.put(id, new CopyOnWriteArrayList<String>());
		if (allies.containsKey(id)) {
			for (String key: allies.get(id).keySet()) {
				if (!key.endsWith(name)) continue;
				AllyStatusRow row = allies.get(id).get(key);
				rogues.get(id).add(key);
				row.setStatus(howAreYou ? AllyStatus.MINE_TRAP : AllyStatus.FIGHTERS);
				postStatusUpdate(row);
				postColorUpdate(row);
			}
		}
		
		// If there are any missions associated with this "ally", remove them
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
		
		return true;
	}
	
	@Message(ParseProtocol.NEW_MISSION)
	public boolean parseNewMission(String sender, String message) {
		if (!message.contains("and we'll")) return false;
		
		// New side mission
		final String[] senderParts = sender.split(" ", 2);
		
		// If sender is a trap ship, ignore this
		if (rogues.containsKey(senderParts[0]) && rogues.get(senderParts[0]).contains(senderParts[1]))
			return true;
		
		// Get source
		final String srcShip = message.split("with ")[1].split(" ")[0];
		final String source;
		ObjectStatusRow srcRow = null, destRow = null;
		
		if (bases.containsKey(srcShip)) {
			// Source location is a station
			String srcStation = "";
			for (String key: bases.get(srcShip).keySet()) srcStation = key;
			source = srcShip + " " + srcStation;
			try {
				ObjectStatusRow row = null;
				if (allies.containsKey(senderParts[0])) {
					row = allies.get(senderParts[0]).get(senderParts[1]);
				} else if (bases.containsKey(senderParts[0])) {
					row = bases.get(senderParts[0]).get(senderParts[1]);
				}
				destRow = row;
			} catch (NullPointerException e) { }
			if (bases.containsKey(srcShip)) {
				srcRow = bases.get(srcShip).get(srcStation);
			}
		} else {
			// Source location is an ally ship - skip if it's a rogue
			if (rogues.containsKey(srcShip) && allies.get(srcShip).size() == rogues.get(srcShip).size())
				return true;
			if (!allies.containsKey(srcShip)) return true;
			
			String srcString = srcShip;
			if (allies.get(srcShip).size() == 1) {
				srcString += " ";
				for (String s: allies.get(srcShip).keySet()) srcString += s;
			}
			source = srcString;
			if (allies.get(srcShip).size() > 1 && !closed.containsKey(srcShip))
				closed.put(srcShip, new CopyOnWriteArrayList<SideMissionRow>());
			try {
				ObjectStatusRow row = null;
				if (allies.containsKey(senderParts[0])) {
					row = allies.get(senderParts[0]).get(senderParts[1]);
				} else if (bases.containsKey(senderParts[0])) {
					row = bases.get(senderParts[0]).get(senderParts[1]);
				}
				destRow = row;
			} catch (NullPointerException e) { }
			if (allies.containsKey(srcShip)) {
				srcRow = allies.get(srcShip).get(source.substring(srcShip.length() + 1));
			}
		}
		
		// Extract reward
		String[] words = message.split(" ");
		final String reward = words[words.length - 1];
		
		if (serviceRunning) {
			// Play "new mission" ringtone
			Uri ringtoneUri = Uri.parse(preferences.getString(getString(R.string.newMissionPrefKey), "Default"));
			Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
			ringtone.play();

			// Create notification containing message
			createNotification(sender, message, messages.size(), false);
			
			// Add information to appropriate lists
			senders.add(sender);
			messages.add(message);
		}
		
		// If a matching mission row already exists, combine with that one
		boolean show = false;
		boolean exists = false;
		for (int i = 0; i < missions.size(); i++) {
			final SideMissionRow row = missions.get(i);
			if (row.isStarted() ||
					!row.getSource().contains(" ") ||
					!row.getSource().startsWith(source) ||
					!row.getDestination().equals(sender)) continue;
			row.addReward(reward);
			exists = true;
			updateMissionsTable();
			show = isShown(row);
			break;
		}
		
		// If not, add to Missions view if filters allow
		if (!exists) {
			final SideMissionRow newRow =
					new SideMissionRow(getApplicationContext(), source, sender, RewardType.from(reward));
			missions.add(newRow);
			show = isShown(newRow);
			if (show) {
				missionFlash = missionsView.getVisibility() == View.GONE;
				missionsView.post(new Runnable() {
					@Override
					public void run() {
						missionsTable.addView(newRow);
					}
				});
			}
			if (srcRow != null && destRow != null) {
				srcRow.addMission(newRow);
				postStatusUpdate(srcRow);
				destRow.addMission(newRow);
				postStatusUpdate(destRow);
			}
		}
		
		// Update the Routing graph
		updateRouteGraph(!show);
		
		return true;
	}
	
	@Message(ParseProtocol.VISITED_SOURCE)
	public boolean parseToDestination(String sender, String message) {
		if (!message.startsWith("Transfer")) return false;
		
		// Update last mission to make progress on a mission
		String[] messageParts = message.substring(0, message.length() - 1).split(", ", 2)[1].split("\\.");
		missionShip = messageParts[0];
		for (int i = 1; i < messageParts.length - 1; i++)
			missionShip += "." + messageParts[i];
		
		// If progress was completion, do nothing (no other parsers)
		if (message.endsWith("!")) return true;
		
		// Visited source of side mission
		String destination = message.split(" to ")[1];
		boolean thisShip = true;
		final String[] senderParts = sender.split(" ", 2);
		String id = senderParts[0];
		for (int i = 0; i < missions.size(); i++) {
			final SideMissionRow row = missions.get(i);
			if (row.isStarted() ||
					!sender.startsWith(row.getSource()) ||
					!row.getDestination().startsWith(destination)) continue;
			row.markAsStarted();
			missionFlash = missionsView.getVisibility() == View.GONE;
			row.updateSource(sender);
			row.updateProgress();

			// Remove source from mission
			ObjectStatusRow srcRow = null;
			if (allies.containsKey(senderParts[0])) {
				srcRow = allies.get(senderParts[0]).get(senderParts[1]);
			} else if (bases.containsKey(senderParts[0])) {
				srcRow = bases.get(senderParts[0]).get(senderParts[1]);
			}
			if (srcRow != null) {
				srcRow.removeMission(row);
				postStatusUpdate(srcRow);
			}

			// Update destination's record of the mission
			ObjectStatusRow destRow = null;
			if (allies.containsKey(destination))
				destRow = allies.get(destination).get(row.getDestination().substring(destination.length() + 1));
			else if (bases.containsKey(destination))
				destRow = bases.get(destination).get(row.getDestination().substring(destination.length() + 1));
			if (version.ge(VERSION27)) {
				if (destRow != null) {
					destRow.removeMission(row);
				}				
				row.setPlayerShip(missionShip);
				thisShip = missionShip.contentEquals(manager.getPlayerShip().getName());
				if (destRow != null) {
					destRow.addMission(row);
					postStatusUpdate(destRow);
				}
			}
			
			// Merge rows with same statuses and parameters
			for (int j = 0; j < missions.size(); j++) {
				final SideMissionRow other = missions.get(j);
				if (j == i ||
						!other.isStarted() ||
						other.isCompleted() ||
						!other.getPlayerShip().equals(missionShip) ||
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
				missions.remove(row);
				if (destRow != null) {
					destRow.removeMission(row);
					postStatusUpdate(destRow);
				}
				break;
			}
			updateMissionsTable();
		}
		
		// Salvage missions that were designated uncompletable
		if (closed.containsKey(id)) { 
			for (int i = 0; i < closed.get(id).size(); i++) {
				final SideMissionRow row = missions.get(i);
				if (row.isStarted() ||
						!sender.startsWith(row.getSource()) ||
						!row.getDestination().startsWith(destination)) continue;
				missions.add(row);
				row.markAsStarted();
				closed.get(id).remove(row);
				row.updateSource(sender);
				if (version.ge(VERSION27)) {
					row.setPlayerShip(missionShip);
					thisShip = missionShip.contentEquals(manager.getPlayerShip().getName());
				}
				row.updateProgress();
				
				ObjectStatusRow destRow = null;
				if (allies.containsKey(destination))
					destRow = allies.get(destination).get(row.getDestination().substring(destination.length() + 1));
				else if (bases.containsKey(destination))
					destRow = bases.get(destination).get(row.getDestination().substring(destination.length() + 1));
				if (destRow != null) {
					destRow.addMission(row);
					postStatusUpdate(destRow);
				}
				
				missionsView.post(new Runnable() {
					@Override
					public void run() {
						missionsTable.addView(row);
					}
				});
				i--;
			}
		}
		
		if (serviceRunning && thisShip) {
			// Play "arrived at source" ringtone
			Uri ringtoneUri = Uri.parse(preferences.getString(getString(R.string.foundSrcPrefKey), "Default"));
			Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
			ringtone.play();
			
			int index = messages.size();
			for (int i = 1; i < messages.size(); i++) {
				// If the notification about the new side mission still exists, replace it instead
				if (!messages.get(i).contains("and we'll")) continue;
				String item = messages.get(i).split(" we need")[0];
				item = item.substring(item.lastIndexOf(" ") + 1);
				if (!message.endsWith(item + ".")) continue;
				String src = messages.get(i).split("with ")[1].split(" ")[0];
				if (!sender.startsWith(src)) continue;
				String dest = message.split(" to ")[1];
				if (!senders.get(i - 1).startsWith(dest) || senders.get(i - 1).length() == dest.length()) continue;
				index = i;
				senders.set(i - 1, sender);
				messages.set(i, message);
				break;
			}
			if (index == messages.size()) {
				senders.add(sender);
				messages.add(message);
			}

			// Create notification containing message
			createNotification(sender, message, index, false);
		}
		
		return true;
	}
	
	@Message(ParseProtocol.MISSION_COMPLETED)
	public boolean parseCompletedMission(String sender, String message) {
		if (!message.startsWith("As promised")) return false;
		
		// End of side mission
		String[] words = message.split(" ");
		String reward = words[words.length - 1];
		final String[] senderParts = sender.split(" ", 2);
		String id = senderParts[0];
		
		if (RewardType.PRODUCTION.matches(reward)) {
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
			if (!row.getPlayerShip().isEmpty() && !missionShip.equals(row.getPlayerShip())) continue;
			row.markAsCompleted();
			missionFlash = missionsView.getVisibility() == View.GONE;
			row.updateProgress();
			
			// Remove sender from mission as it is now completed
			try {
				ObjectStatusRow objRow = null;
				if (allies.containsKey(senderParts[0])) {
					objRow = allies.get(senderParts[0]).get(senderParts[1]);
				} else if (bases.containsKey(senderParts[0])) {
					objRow = bases.get(senderParts[0]).get(senderParts[1]);
				}
				objRow.removeMission(row);
				postStatusUpdate(objRow);
			} catch (NullPointerException e) { }
			
			// Set up touch-to-remove function
			row.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					missions.remove(row);
					missionsTable.removeView(row);
				}
			});
		}
		
		// Salvage missions that were designated uncompletable
		if (closed.containsKey(id)) {
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
		}
		
		if (serviceRunning) {
			// Play "arrived at destination" ringtone
			Uri ringtoneUri = Uri.parse(preferences.getString(getString(R.string.foundDestPrefKey), "Default"));
			Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
			ringtone.play();
			
			int index = messages.size();
			for (int i = 1; i < messages.size(); i++) {
				// If the notification about arriving at the source still exists, replace it instead
				if (!messages.get(i).contains("to deliver the")) continue;
				String src = messages.get(i).split("with ")[1].split(" ")[0];
				if (!sender.startsWith(src)) continue;
				String dest = message.split(" to ")[1];
				if (!senders.get(i - 1).startsWith(dest)) continue;
				senders.set(i - 1, sender);
				messages.set(i, message);
				index = i;
				break;
			}
			if (index == messages.size()) {
				senders.add(sender);
				messages.add(message);
			}
			
			// Create notification containing message
			createNotification(sender, message, index, false);
		}
		
		return true;
	}
	
	@Message(ParseProtocol.ATTACK)
	public boolean parseUnderAttack(String sender, String message) {
		if (!message.startsWith("We're under") && !message.contains("%!")) return false;
		
		// Send to Comms service
		if (serviceRunning) {
			// Play "station under attack" ringtone
			Uri ringtoneUri = Uri.parse(preferences.getString(getString(R.string.underAttackPrefKey), "Default"));
			Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
			ringtone.play();
			
			// If previous notification exists, update it
			if (attackMessages.containsKey(sender)) {
				messages.set(attackMessages.get(sender), message);
			} else {
				attackMessages.put(sender, messages.size());
				senders.add(sender);
				messages.add(message);
			}
			createNotification(sender, message, attackMessages.get(sender), false);
		}
		
		return true;
	}
	
	@Message(ParseProtocol.DESTROYED)
	public boolean parseDestroyed(String sender, String message) {
		if (!message.startsWith("We've detected")) return false;
		
		// Send to Comms service
		if (serviceRunning) {
			// Play "station under attack" ringtone
			Uri ringtoneUri = Uri.parse(preferences.getString(getString(R.string.baseDestroyedPrefKey), "Default"));
			Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
			ringtone.play();
			
			String destroyedBase = message.split(" ")[9];
			
			// If previous notification exists, update it
			if (attackMessages.containsKey(destroyedBase)) {
				createNotification(sender, message, attackMessages.get(destroyedBase), false);
				messages.set(attackMessages.get(destroyedBase), message);
				attackMessages.remove(destroyedBase);
			} else {
				createNotification(sender, message, messages.size(), false);
				senders.add(sender);
				messages.add(message);
			}
		}
		
		return true;
	}
	
	@Message(ParseProtocol.REWARD_DELIVERED)
	public boolean parseRewardDelivered(String sender, String message) {
		if (!message.endsWith("Enjoy your reward!")) return false;
		
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
		
		return true;
	}
	
	@Message(ParseProtocol.RESCUED)
	public boolean parseRescued(String sender, String message) {
		if (!message.startsWith("Thanks for rescuing") && !message.endsWith("Let us upgrade your shield generators."))
			return false;
		
		// Ally ship rescued from captors
		if (allies.containsKey(sender)) {
			for (AllyStatusRow row: allies.get(sender).values()) {
				row.setStatus(AllyStatus.NORMAL);
				postStatusUpdate(row);
				postColorUpdate(row);
			}
			updateAlliesTable();
		}
		
		return true;
	}
	
	@Message(ParseProtocol.DIRECTIONS)
	public boolean parseDirections(String sender, String message) {
		if (!message.contains("we are turning")) return false;
		
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
		
		return true;
	}
	
	@Message(ParseProtocol.TO_STATION)
	public boolean parseHasDestination(String sender, String message) {
		if (!message.startsWith("Okay, going")) return false;
		
		// Ally ship flying blind now has a destination
		if (allies.containsKey(sender)) {
			// Make sure the destination is a station
			String destination = message.substring(message.contains("rendez") ? 31 : 22);
			destination = destination.substring(0, destination.length() - 1);
			if (!bases.containsKey(destination)) return true;
			
			for (AllyStatusRow row: allies.get(sender).values()) {
				if (row.isFlyingBlind()) {
					row.setStatus(AllyStatus.REWARD);
					postStatusUpdate(row);
					postColorUpdate(row);
				}
			}
			updateAlliesTable();
		}
		
		return true;
	}
	
	@Message(ParseProtocol.OTHER)
	public boolean parseOther(String sender, String message) {
		if (!message.startsWith("Our shields") ||
				message.contains("\nWe're heading to the station") ||
				sender.startsWith("DS"))
			return false;
		
		// Any other message from ally ships
		final String id = sender.substring(sender.lastIndexOf(" ") + 1);
		final String name = sender.split(" " + id)[0];
		final boolean hasEnergy = message.endsWith("you need some.");
		if (allies.containsKey(id)) {
			for (String key: allies.get(id).keySet()) {
				if (!key.endsWith(name)) continue;
				AllyStatusRow row = allies.get(id).get(key);
				if (row.getStatus() != AllyStatus.REWARD) row.setStatus(AllyStatus.NORMAL);
				row.setEnergy(hasEnergy);
				row.setTorpedoes(message.contains("We have some torpe"));
				postStatusUpdate(row);
				postColorUpdate(row);
				updateAlliesTable();
			}
		}
		
		// Update the Routing graph
		updateRouteGraph(!hasEnergy);
		
		return true;
	}
	
	// Empty out all tables
	public void clearTableViews() {
		missionsView.post(clearMissionsTable);
		alliesView.post(clearAlliesTable);
		stationsView.post(clearStationsTable);
	}
	
	// Update Missions table
	public void updateMissionsTable() {
		if (playerDestroyed()) return;
		missionsView.post(clearMissionsTable);
		for (int i = 0; i < missions.size(); i++) {
			final SideMissionRow row = missions.get(i);
			missionsView.post(new Runnable() {
				@Override
				public void run() {
					if (isShown(row)) missionsTable.addView(row);
				}
			});
			row.updateRewardText();
			row.updateProgress();
		}
	}
	
	// Update Stations table
	public void updateStationsTable() {
		if (playerDestroyed()) return;
		for (final String n: baseKeys) {
			for (final String s: bases.get(n).keySet()) {
				stationsView.post(new Runnable() {
					@Override
					public void run() {
						try { stationsTable.addView(bases.get(n).get(s)); }
						catch (IllegalStateException e) { }
					}
				});
			}
		}
	}
	
	// Update Allies table
	public void updateAlliesTable() {
		// Start by emptying the table
		alliesView.post(clearAlliesTable);
		
		// If current player destroyed, do nothing else
		if (playerDestroyed()) return;
		
		int rowCount = 0;
		for (CharSequence n: allies.keySet()) rowCount += allies.get(n).size();
		final ArrayList<AllyStatusRow> rows = new ArrayList<AllyStatusRow>(rowCount);
		boolean showDestroyed = preferences.getBoolean(getString(R.string.showDestroyedKey), true);
		
		for (CharSequence n: allies.keySet()) {
			for (final AllyStatusRow r: allies.get(n).values()) {
				// Don't show allies that have been destroyed if preferences say not to 
				if (!showDestroyed && r.getStatus() == AllyStatus.DESTROYED) continue;
				
				// Make sure statuses are up to date
				r.updateStatusText();
				
				// Find index to insert row at using binary search
				int index = 0;
				int minIndex = 0, maxIndex = rows.size();
				while (minIndex < maxIndex) {
					index = (maxIndex + minIndex) / 2;
					AllyStatusRow otherRow = rows.get(index);
					if (otherRow == null) continue;
					int compare = compare(r, otherRow);
					if (compare < 0) {
						maxIndex = index;
					} else if (compare > 0) {
						minIndex = ++index;
					} else break;
				}
				rows.add(index, r);
			}
		}
		
		// Add all rows to Allies table
		alliesView.post(new Runnable() {
			@Override
			public void run() {
				for (AllyStatusRow r: rows) {
					r.updateStatusUI();
					try { alliesTable.addView(r); }
					catch (IllegalStateException e) { }
				}
			}
		});
	}
	
	// Binary search comparison method for ally status rows
	private int compare(AllyStatusRow row1, AllyStatusRow row2) {
		// Always put destroyed ally rows last
		if (row1.getStatus() == AllyStatus.DESTROYED || row2.getStatus() == AllyStatus.DESTROYED) {
			if (row1.getStatus() != AllyStatus.DESTROYED) return -1;
			if (row2.getStatus() != AllyStatus.DESTROYED) return 1;
		}
		
		// Find variables including sort method
		int methodIndex = Integer.parseInt(preferences.getString(getString(R.string.allySortKey), "0"));
		AllySortMethod method = AllySortMethod.values()[methodIndex];
		TextView text1 = (TextView) row1.getChildAt(0);
		TextView text2 = (TextView) row2.getChildAt(0);
		String[] name1 = text1.getText().toString().split(" ", 2);
		String[] name2 = text2.getText().toString().split(" ", 2);
		int compare = 0;
		
		// Switch-case system include fallthroughs to reduce duplicate code
		switch (method) {
		case NONE:
			// No method
			break;
		case CLASS_STATUS_NAME:
			// Sort by class
			compare = name1[1].compareTo(name2[1]);
			if (compare != 0) return compare;
			// If not, then status
		case STATUS_NAME:
			// Sort by status
			if (row1.getStatus().index < row2.getStatus().index) return -1;
			else if (row1.getStatus().index > row2.getStatus().index) return 1;
			
			// Put ships that have energy first?
			if (preferences.getBoolean(getString(R.string.energyFirstKey), false)) {
				if (row1.hasEnergy() && !row2.hasEnergy()) return -1;
				else if (!row1.hasEnergy() && row2.hasEnergy()) return 1;
			}
			// If still not distinguished, go to name
		case NAME:
			// Sort by name
			compare = name1[0].compareTo(name2[0]);
			break;
		case STATUS_CLASS_NAME:
			// Sort by status
			if (row1.getStatus().index < row2.getStatus().index) return -1;
			else if (row1.getStatus().index > row2.getStatus().index) return 1;
			
			// Put ships that have energy first?
			if (preferences.getBoolean(getString(R.string.energyFirstKey), false)) {
				if (row1.hasEnergy() && !row2.hasEnergy()) return -1;
				else if (!row1.hasEnergy() && row2.hasEnergy()) return 1;
			}
			// If still not distinguished, go to class
		case CLASS_NAME:
			// Sort by class
			compare = name1[1].compareTo(name2[1]);
			if (compare != 0) return compare;
			
			// If not, then name
			compare = name1[0].compareTo(name2[0]);
			break;
		case CLASS_STATUS:
			// Sort by class
			compare = name1[1].compareTo(name2[1]);
			if (compare != 0) return compare;
			// If not, then status
		case STATUS:
			// Sort by status
			if (row1.getStatus().index < row2.getStatus().index) return -1;
			else if (row1.getStatus().index > row2.getStatus().index) return 1;
			
			// Put ships that have energy first?
			if (preferences.getBoolean(getString(R.string.energyFirstKey), false)) {
				if (row1.hasEnergy() && !row2.hasEnergy()) return -1;
				else if (!row1.hasEnergy() && row2.hasEnergy()) return 1;
			}
			break;
		case STATUS_CLASS:
			// Sort by status
			if (row1.getStatus().index < row2.getStatus().index) return -1;
			else if (row1.getStatus().index > row2.getStatus().index) return 1;
			
			// Put ships that have energy first?
			if (preferences.getBoolean(getString(R.string.energyFirstKey), false)) {
				if (row1.hasEnergy() && !row2.hasEnergy()) return -1;
				else if (!row1.hasEnergy() && row2.hasEnergy()) return 1;
			}
			// If still not distinguished, go to class
		case CLASS:
			// Sort by class
			compare = name1[1].compareTo(name2[1]);
			break;
		}
		
		return compare;
	}
	
	// Update station row, we are docked there
	public void updateDockedRow() {
		if (dockingStation == -1) return;
		for (StationStatusRow row: bases.get(getDockingStationName()).values()) {
			row.setDocking(true);
			if (manager.getPlayerShip().isDocked().getBooleanValue())
				row.completeDock();
		}
		for (CharSequence key: bases.keySet()) {
			for (StationStatusRow row: bases.get(key).values()) {
				row.setReady(false);
				postStatusUpdate(row);
			}
		}
	}
	
	// We are no longer docked at a station
	public void resetDockedRow() {
		for (String key: bases.keySet()) {
			for (StationStatusRow row: bases.get(key).values()) {	
				row.setDocking(false);
				postStatusUpdate(row);
			}
		}
		dockingStation = -1;
	}
	
	// Finds an object from a list based on its call sign
	private ArtemisObject findObject(List<ArtemisObject> objects, String callSign) {
		ArtemisObject object = null;
		for (ArtemisObject obj: objects) {
			BaseArtemisShielded npc = (BaseArtemisShielded) obj;
			
			// Skip enemies
			if (npc.getVessel(context) == null ||
					npc.getVessel(context).getFaction() == null ||
					npc.getVessel(context).getFaction().getId() > 1) continue;
			
			// Make sure call sign is a match
			if (!npc.getName().toString().equals(callSign)) continue;
			
			// Make sure object is not a fighter ship
			if (npc.getVessel(context).is(VesselAttribute.FIGHTER) ||
					npc.getVessel(context).is(VesselAttribute.SINGLESEAT)) continue;
			object = npc;
			break;
		}
		
		return object;
	}
	
	private void addMissionsToRoute() {
		for (int i = 0; i < missions.size(); i++) {
			SideMissionRow row = missions.get(i);
			
			// Skip completed missions or missions that are not shown because of reward filters
			if (row.isCompleted() || !isShown(row)) continue;
			
			// Get call signs of ships
			String srcCall = row.getSource().split(" ")[0];
			String destCall = row.getDestination().split(" ")[0];
			
			// If mission was started, ignore source
			if (row.isStarted()) {
				srcCall = destCall;
				destCall = null;
			}
			
			// Get list of stations and allies
			ArrayList<ArtemisObject> points = new ArrayList<ArtemisObject>(manager.getObjects(ObjectType.BASE));
			points.addAll(manager.getObjects(ObjectType.NPC_SHIP));
			
			// Find source among stations and allies
			ArtemisObject srcObject = findObject(points, srcCall);
			
			// If source could not be found, mission is probably invalid
			if (srcObject == null) continue;
			
			// Do the same for destination
			ArtemisObject destObject = null;
			if (destCall != null) {
				destObject = findObject(points, destCall);
				if (destObject == null) continue;
			}
			
			// Add path to routing graph
			if (destObject == null) graph.addPath(srcObject);
			else graph.addPath(srcObject, destObject);
		}
	}
	
	private void addAlliesToRoute() {
		// Check for allies
		for (CharSequence key: allies.keySet()) {
			for (AllyStatusRow row: allies.get(key).values()) {
				if (row.getStatus().ordinal() < AllyStatus.FIGHTERS.ordinal() && !getReasons(row).isEmpty())
					graph.addPath(row.getAllyShip());
			}
		}
	}
	
	public void updateRouteGraph(boolean keepMinCost) {
		if (graph == null) return;
		graph.resetGraph();
		routing = true;
		
		addMissionsToRoute();
		addAlliesToRoute();
		
		graph.purgePaths();
		keepMinCost &= graph.size() <= routeTable.getChildCount();
		
		if (!keepMinCost) {
			graph.resetMinimumPoints();
		}
		
		graph.recalculateCurrentRoute();
	}

	// Method to run object row status updates on UI thread
	private void postStatusUpdate(final ObjectStatusRow row) {
		row.updateStatusText();
		updateHandler.post(new Runnable() {
			@Override
			public void run() {
				row.updateStatusUI();
			}
		});
	}

	// Method to run object row color updates on UI thread
	private void postColorUpdate(final ObjectStatusRow row) {
		updateHandler.post(new Runnable() {
			@Override
			public void run() {
				row.updateColor();
			}
		});
	}
	
	// Method to run ordnance text updates on UI thread
	private void postOrdnanceUpdate(final StationStatusRow row) {
		final String stockText = row.getOrdnanceText();
		updateHandler.post(new Runnable() {
			@Override
			public void run() {
				row.updateOrdnance(stockText);
			}
		});
	}
	
	private String getDockingStationName() {
		return manager.getObject(dockingStation).getName().toString();
	}
	
	// Build message for top notification
	private String getMainMessage() {
		String message = "";
		if (numBases > 0) {
			message += numBases + " station";
			if (numBases > 1) message += "s";
		}
		if (numAllies > 0) {
			if (!message.equals("")) message += ", ";
			if (numAllies == 1) message += "1 ally";
			else message += numAllies + " allies";
		}
		if (numMissions > 0) {
			if (!message.equals("")) message += ", ";
			message += numMissions + " mission";
			if (numMissions > 1) message += "s";
		}
		return message;
	}
	
	// Create a set of route entry rows from a list of object IDs
	private ArrayList<RouteEntryRow> createEntryRows(List<Integer> route) {
		ArrayList<RouteEntryRow> entryRows = new ArrayList<RouteEntryRow>(route.size());
		ArtemisObject player = manager.getPlayerShip();
		if (player == null) return entryRows;
		
		for (int id: route) {
			BaseArtemisShielded object = (BaseArtemisShielded)manager.getObject(id);
			if (object.getVessel(context) == null) continue;
			String objName = object.getName() + " " + object.getVessel(context).getFullName();
			
			RouteEntryRow newRow = new RouteEntryRow(getApplicationContext(), object, objName);
			newRow.updateDistance(player, preferences.getBoolean(getString(R.string.threeDigitsPrefKey), false));
			entryRows.add(newRow);
		}
		return entryRows;
	}
	
	// Get all rows currently in the routing table
	private ArrayList<RouteEntryRow> getCurrentRows() {
		ArrayList<RouteEntryRow> entryRows = new ArrayList<RouteEntryRow>(routeTable.getChildCount());
		for (int i = 0; i < routeTable.getChildCount(); i++)
			entryRows.add((RouteEntryRow)routeTable.getChildAt(i));
		return entryRows;
	}
	
	// Get each row's reason(s) for being in the routing table
	private void checkReasons(List<RouteEntryRow> entryRows) {
		for (int r = 0; r < entryRows.size(); r++) {
			RouteEntryRow row = entryRows.get(r);
			String objName = row.getObjectName().split(" ")[0];
			RouteEntryReason[] reasons = new RouteEntryReason[0];
			
			if (bases.containsKey(objName)) {
				// If it's a docking station, the side missions are the reasons
				for (StationStatusRow baseRow: bases.get(objName).values()) {
					reasons = new RouteEntryReason[baseRow.getMissions()];
					for (int i = 0; i < reasons.length; i++)
						reasons[i] = RouteEntryReason.MISSION;
					break;
				}
			} else if (allies.containsKey(objName)) {
				// If it's an ally ship, there can be many reasons including side missions
				for (AllyStatusRow allyRow: allies.get(objName).values()) {
					int numMissions = allyRow.getMissions();
					EnumSet<RouteEntryReason> others = getReasons(allyRow);
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
			
			if (reasons.length == 0) entryRows.remove(r--);
			else row.setReasons(reasons);
		}
	}
	
	// Get reasons for an ally ship being in the route besides side missions
	private EnumSet<RouteEntryReason> getReasons(AllyStatusRow row) {
		EnumSet<RouteEntryReason> reasons = EnumSet.noneOf(RouteEntryReason.class);
		
		// Check if ally ship has energy or torpedoes to give
		if (preferences.getBoolean(getString(R.string.energyKey), false) && row.hasEnergy())
			reasons.add(RouteEntryReason.HAS_ENERGY);
		if (preferences.getBoolean(getString(R.string.hasTorpsKey), false) && row.hasTorpedoes())
			reasons.add(RouteEntryReason.TORPEDOES);
		
		// Get reasons based on status
		switch (row.getStatus()) {
		case NEED_ENERGY:
			if (preferences.getBoolean(getString(R.string.needEnergyKey), false))
				reasons.add(RouteEntryReason.NEEDS_ENERGY);
			break;
		case NEED_DAMCON:
			if (preferences.getBoolean(getString(R.string.needDamConKey), false))
				reasons.add(RouteEntryReason.DAMCON);
			break;
		case BROKEN_COMPUTER:
			if (preferences.getBoolean(getString(R.string.malfunctionKey), false))
				reasons.add(RouteEntryReason.MALFUNCTION);
			break;
		case AMBASSADOR:
			if (preferences.getBoolean(getString(R.string.ambassadorKey), false))
				reasons.add(RouteEntryReason.AMBASSADOR);
			break;
		case HOSTAGE:
			if (preferences.getBoolean(getString(R.string.hostageKey), false))
				reasons.add(RouteEntryReason.HOSTAGE);
			break;
		case COMMANDEERED:
			if (preferences.getBoolean(getString(R.string.commandeeredKey), false))
				reasons.add(RouteEntryReason.COMMANDEERED);
			break;
		default:
			break;
		}
		
		return reasons;
	}
	
	private boolean isShown(SideMissionRow row) {
		if (!row.getPlayerShip().isEmpty() && !row.getPlayerShip().contentEquals(manager.getPlayerShip().getName()))
			return false;
		
		for (RewardType type: RewardType.values()) {
			if (type.isShown() && row.hasReward(type)) return true;
		}
		
		return false;
	}
	
	// Check to see if the player has undocked from a station
	private void checkForUndock() {
		if (dockingStation != -1 && manager.getPlayerShip() != null && manager.getPlayerShip().getDockingBase() < 1) {
			for (ArtemisObject o: manager.getObjects(ObjectType.BASE)) {
				if (dockingStation != o.getId()) continue;
				outPackets.add(new CommsOutgoingPacket(o, BaseMessage.PLEASE_REPORT_STATUS, context));
				break;
			}
			resetDockedRow();
		}
	}
	
	// Keep ally ship shield values updated
	private void updateAllyShipShields() {
		allyFlash = false;
		
		for (ArtemisObject o: manager.getObjects(ObjectType.NPC_SHIP)) {
			ArtemisNpc npc = (ArtemisNpc) o;
			if (npc.getVessel(context) == null ||
					npc.getVessel(context).getFaction() == null ||
					npc.getVessel(context).getFaction().getId() > 1) continue;
			try {
				AllyStatusRow row = allies.get(npc.getName().toString()).get(npc.getVessel(context).getFullName());
				if (row.getStatus() == AllyStatus.DESTROYED) continue;
				row.setFront((int) npc.getShieldsFront());
				row.setRear((int) npc.getShieldsRear());
				row.updateShields();
				allyFlash |= npc.getShieldsFront() < npc.getShieldsFrontMax() ||
						npc.getShieldsRear() < npc.getShieldsRearMax();
			} catch (NullPointerException e) {}
		}
	}
	
	// Update docking station status rows
	private void updateStations() {
		stationFlash = false;

		ArtemisPlayer player = manager.getPlayerShip();
		ArtemisBase closest = null;
		for (ArtemisObject o: manager.getObjects(ObjectType.BASE)) {
			ArtemisBase base = (ArtemisBase) o;
			try {
				// Update docking station's status 
				StationStatusRow row = bases.get(base.getName().toString()).get(base.getVessel(context).getFullName());
				row.setClosest(false);
				row.setShields((int) base.getShieldsFront());
				row.updateOrdnance(row.getOrdnanceText());
				stationFlash |= base.getShieldsFront() < base.getShieldsRear();
				
				// Find closest station
				if (closest == null) {
					closest = base;
					row.setClosest(true);
				} else {
					float closestDistance = closest.distance(player);
					float currentDistance = base.distance(player);
					if (currentDistance < closestDistance) {
						for (StationStatusRow closestRow: bases.get(closest.getName().toString()).values()) {
							closestRow.setClosest(false);
							closestRow.updateStatusText();
							closestRow.updateStatusUI();
						}
						row.setClosest(true);
						closest = base;
					}
				}
				row.updateStatusText();
				row.updateStatusUI();
				row.updateColor();
			} catch (NullPointerException e) {}
		}
	}
	
	@Override
	protected void onActivityResult(int reqCode, int resCode, Intent intent) {
		if (reqCode != connectReqCode || resCode != Activity.RESULT_OK) return;
		
		String addr = intent.getStringExtra("Address");
		addressField.setText(addr);
		if (intent.hasExtra("Error"))
			showToast("Failed to find broadcast address", Toast.LENGTH_SHORT);
		
		if (!addr.equals("")) createConnection();
	}
	
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
	            	   endServer();
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
		
		// Initialize variables
		initLayoutFields();
		initDataTables();
		initHandlers();
		
		// Hide all views except missions
		alliesView.setVisibility(View.GONE);
		stationsView.setVisibility(View.GONE);
		routeView.setVisibility(View.GONE);
		
		// Get previously used server address
		initAddressField();
		
		// Initialize buttons and ship selector
		initShipSelector();
		initViewButtons();
		initUtilityButtons();
		
		// Initialize app font
		initFont();
		
		// Initialize reward types
		RewardType.initDisplayNames(this);
		
		// Start notification cleanup service
		startService(new Intent(getApplicationContext(), NotificationCleanupService.class));
		
		// Set up preference change listener
		preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		preferences.registerOnSharedPreferenceChangeListener(this);
		
		// Initialize preferences
		PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preference, false);
		RewardType.updateVisibility(preferences);
		
		// Hide hint text
		updateHintText.run();
		
		// Initialize message parse protocols
		ArrayList<ParseMethod> methods = new ArrayList<ParseMethod>(ParseProtocol.NUM_PROTOCOLS);
		methods.addAll(ParseMethod.search(this, EnumSet.allOf(ParseProtocol.class)));
		parseMethods = methods.toArray(new ParseMethod[methods.size()]);
		
		// Initialize timeout
		heartbeatTimeout = Integer.parseInt(preferences.getString(getString(R.string.serverTimeoutKey), "2") + "000");
		
		// Start up help activity if set
		if (preferences.getBoolean(getString(R.string.connectStartupKey), true) &&
				preferences.getBoolean(getString(R.string.allowUDPKey), true)) {
			startConnectActivity();
		}
		
		// Initialize Toast message display
		toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
		
		// Set up notification builder
		initNotifications();
	}
	
	@Override
	public void onDestroy() {
		// Shut down packet handler
		inPackets.clear();
		outPackets.clear();
		
		// Shut down connection
		if (serviceRunning) {
			// Destroy all notifications
			removeAllNotifications();
		}
		
		// Clear server data
		endServer();
		clearDataTables();
		
		super.onDestroy();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		// If there's no server running, do nothing
		if (server == null || !server.isConnected()) return;
		
		// Start up Comms service
		serviceRunning = true;
		buttonHandler.post(updateService);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// App resumed, stop Comms service
		serviceRunning = false;
		
		// Start button handler
		buttonHandler.post(updateButtons);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (toast != null)
			showToast("Updating preferences\u2026", Toast.LENGTH_SHORT);
		
		if (key.equals(getString(R.string.vesselDataKey))) {
			if (assetsFail) assetsFail = false;
			else if (server != null && server.isConnected()) createConnection();
		} else if (key.equals(getString(R.string.recentHostsKey))) {
			String[] recentList = recentHosts.split("\\*");
			int numRecent = Integer.parseInt(sharedPreferences.getString(key, "" + recentList.length));
			recentHosts = "";
			for (int i = 0; i < recentList.length && i < numRecent; i++) {
				if (!recentHosts.isEmpty()) recentHosts += "*";
				recentHosts += recentList[i];
			}
			writeRecentHostsToFile();
		} else if (key.equals(getString(R.string.serverTimeoutKey))) {
			heartbeatTimeout =
					Integer.parseInt(sharedPreferences.getString(key, "" + (heartbeatTimeout / 1000)) + "000");
		} else if (key.equals(getString(R.string.showHintTextKey))) {
			updateHintText.run();
		} else if (key.equals(getString(R.string.allySortKey)) || key.equals(getString(R.string.showDestroyedKey))) {
			updateAlliesTable();
			
			updateRouteGraph(false);
		} else if (key.endsWith("CheckBox") && !key.startsWith("connect") && !key.startsWith("allow")) {
			RewardType.updateVisibility(sharedPreferences);
			
			updateMissionsTable();
			updateAlliesTable();
			
			updateRouteGraph(false);
		}
	}
}