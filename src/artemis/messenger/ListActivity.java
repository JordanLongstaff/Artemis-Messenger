package artemis.messenger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.walkertribe.ian.protocol.core.GameOverPacket;
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
	private com.walkertribe.ian.Context context;
	private ArtemisNetworkInterface server;
	private SystemManager manager;
	private String host, dockingStation;
	private int playerShip;
	private LinearLayout missionsTable, alliesTable, stationsTable, routeTable;
	private TableLayout alliesView, stationsView, missionsView, routeView;
	private boolean updateRunning, objectControl, routing;
	private Handler updateHandler, stationHandler, dsHandler, routeHandler;
	private List<SideMissionRow> missions;
	private Map<String, ArrayList<SideMissionRow>> closed;
	private Map<String, HashMap<String, StationStatusRow>> bases;
	private Map<String, HashMap<String, AllyStatusRow>> allies;
	private Map<String, ArrayList<String>> rogues;
	private Map<AllyStatus, Integer> statuses;
	private List<CommsIncomingPacket> inPackets;
	private List<CommsOutgoingPacket> outPackets;
	private boolean assetsFail;
	
	private static final String dataFile = "server.dat";
	private static final int updateInterval = 100;
	private static final int dsInterval = 300000;
	private static final int routeInterval = 500;
	
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
		missions = Collections.synchronizedList(new ArrayList<SideMissionRow>());
		closed = Collections.synchronizedMap(new HashMap<String, ArrayList<SideMissionRow>>());
		allies = Collections.synchronizedMap(new HashMap<String, HashMap<String, AllyStatusRow>>());
		bases = Collections.synchronizedMap(new HashMap<String, HashMap<String, StationStatusRow>>());
		rogues = Collections.synchronizedMap(new HashMap<String, ArrayList<String>>());
		statuses = Collections.synchronizedMap(new EnumMap<AllyStatus, Integer>(AllyStatus.class));
		inPackets = Collections.synchronizedList(new ArrayList<CommsIncomingPacket>());
		outPackets = Collections.synchronizedList(new ArrayList<CommsOutgoingPacket>());
		
		// Hide all views except missions
		alliesView.setVisibility(View.GONE);
		stationsView.setVisibility(View.GONE);
		routeView.setVisibility(View.GONE);
		
		// Initialize handlers
		updateHandler = new Handler();
		stationHandler = new Handler();
		dsHandler = new Handler();
		routeHandler = new Handler();
		
		// Initialize app font
		if (APP_FONT == null) APP_FONT = Typeface.createFromAsset(getAssets(), "fonts/BUTTERUNSALTED.TTF");
		
		// Initialize address field
		final EditText addressField = (EditText) findViewById(R.id.addressField);
		
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
		
		// Initialize ship selection box
		Spinner shipSpinner = (Spinner) findViewById(R.id.shipSpinner);
		shipSpinner.setBackgroundColor(Color.parseColor("#bf9000"));
		shipSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				// Wait for server
				while (server == null);
				
				// Set up Comms connection from selected ship
				playerShip = position + 1;
				server.send(new SetShipPacket(position + 1));
				server.send(new SetConsolePacket(Console.COMMUNICATIONS, true));
				
				// Update docking status
				if (dockingStation != null) uiThreadControl(resetDockedRow);
				dockingStation = null;
				if (ListActivity.this.manager.getPlayerShip(playerShip) == null) return;
				for (ArtemisObject o: ListActivity.this.manager.getObjects(ObjectType.BASE)) {
					ArtemisBase base = (ArtemisBase) o;
					if (base.getId() == ListActivity.this.manager.getPlayerShip(playerShip).getDockingBase()) {
						dockingStation = base.getName();
						uiThreadControl(updateDockedRow);
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
				allyViewButton.setBackgroundColor(Color.parseColor("#bf9000"));
				missionViewButton.setBackgroundColor(Color.parseColor("#cf5a11"));
				stationViewButton.setBackgroundColor(Color.parseColor("#cf5a11"));
				routeViewButton.setBackgroundColor(Color.parseColor("#cf5a11"));
			}
		});
		
		missionViewButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				alliesView.setVisibility(View.GONE);
				missionsView.setVisibility(View.VISIBLE);
				stationsView.setVisibility(View.GONE);
				routeView.setVisibility(View.GONE);
				missionViewButton.setBackgroundColor(Color.parseColor("#bf9000"));
				allyViewButton.setBackgroundColor(Color.parseColor("#cf5a11"));
				stationViewButton.setBackgroundColor(Color.parseColor("#cf5a11"));
				routeViewButton.setBackgroundColor(Color.parseColor("#cf5a11"));
			}
		});
		
		stationViewButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				alliesView.setVisibility(View.GONE);
				missionsView.setVisibility(View.GONE);
				stationsView.setVisibility(View.VISIBLE);
				routeView.setVisibility(View.GONE);
				stationViewButton.setBackgroundColor(Color.parseColor("#bf9000"));
				missionViewButton.setBackgroundColor(Color.parseColor("#cf5a11"));
				allyViewButton.setBackgroundColor(Color.parseColor("#cf5a11"));
				routeViewButton.setBackgroundColor(Color.parseColor("#cf5a11"));
			}
		});
		
		routeViewButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				alliesView.setVisibility(View.GONE);
				missionsView.setVisibility(View.GONE);
				stationsView.setVisibility(View.GONE);
				routeView.setVisibility(View.VISIBLE);
				routeViewButton.setBackgroundColor(Color.parseColor("#bf9000"));
				stationViewButton.setBackgroundColor(Color.parseColor("#cf5a11"));
				missionViewButton.setBackgroundColor(Color.parseColor("#cf5a11"));
				allyViewButton.setBackgroundColor(Color.parseColor("#cf5a11"));
			}
		});
		
		// Initialize view button colours
		missionViewButton.setBackgroundColor(Color.parseColor("#bf9000"));
		allyViewButton.setBackgroundColor(Color.parseColor("#cf5a11"));
		stationViewButton.setBackgroundColor(Color.parseColor("#cf5a11"));
		routeViewButton.setBackgroundColor(Color.parseColor("#cf5a11"));
		
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
		
		// Set up preference change listener
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
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
		if (server != null) {
			server.stop();
			server = null;
			inPackets.clear();
		}
		final EditText addressField = (EditText) findViewById(R.id.addressField);
		final LinearLayout addressRow = (LinearLayout) findViewById(R.id.addressRow);
		final Spinner shipSpinner = (Spinner) findViewById(R.id.shipSpinner);
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.ship_name_spinner);
		final String ip = addressField.getText().toString();
		if (host != null) {
			uiThreadControl(clearAllTables);
		}
		
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		int resolverIndex = Integer.parseInt(pref.getString(getString(R.string.vesselDataKey), "0"));
		PathResolver resolver;
		
		if (resolverIndex == 0) {
			resolver = new AssetsResolver(getAssets());
		} else {
			Toast.makeText(this, "Unpacking assets...", Toast.LENGTH_SHORT).show();
			String filesDir;
			if (resolverIndex == 2) filesDir = Environment.getExternalStorageDirectory() + "/artemis/dat";
			else filesDir = getFilesDir() + "/artemis/dat";
			File datDir = new File(filesDir);
			if (!datDir.exists()) datDir.mkdirs();
			AssetManager am = getAssets();
			try {
				for (String in: am.list("dat")) {
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
				assetsFail = true;
				Toast.makeText(this, "Failed to unpack assets, switching location to Default...", Toast.LENGTH_SHORT).show();
				pref.edit().putString(getString(R.string.vesselDataKey), "0").commit();
				resolver = new AssetsResolver(getAssets());
			}
		}
		
		context = new com.walkertribe.ian.Context(resolver);
		
		Toast.makeText(this, "Connecting to " + ip + "...", Toast.LENGTH_LONG).show();
		
		new Thread(new Runnable() {
			@Override public void run() {
				try {
					server = new ThreadedArtemisNetworkInterface(ip, 2010, context);
					server.addListener(ListActivity.this);
					
					manager = new SystemManager(context);
					server.addListener(manager);
					
					server.start();
				} catch (IOException e) {
//					Log.e(ListActivity.class.getName(), "Failed to connect", e);
					uiThreadControl(new Runnable() {
						@Override
						public void run() {
							addressRow.setBackgroundColor(Color.RED);
							shipSpinner.setAdapter(adapter);
						}
					});
				} catch (NullPointerException e) {
//					Log.e(ListActivity.class.getName(), "Null pointer exception", e);
				}
			}
		}).start();
		uiThreadControl(new Runnable() {
			@Override
			public void run() {
				addressRow.setBackgroundColor(Color.parseColor("#c55a11"));
			}
		});
	}
	
	@Listener
	public void onConnectSuccess(ConnectionSuccessEvent e) {
		server.send(new SetConsolePacket(Console.COMMUNICATIONS, true));
		server.send(new ReadyPacket());
		
		try {
			FileOutputStream fos = openFileOutput(dataFile, Context.MODE_PRIVATE);
			EditText addressField = (EditText) findViewById(R.id.addressField);
			host = addressField.getText().toString();
			fos.write(host.getBytes());
			fos.close();
		} catch (IOException ex) {
//			Log.e(getClass().getName(), "Failed to write data", ex);
		}

		uiThreadControl(new Runnable() {
			@Override
			public void run() {
				updateTables.run();
				LinearLayout addressRow = (LinearLayout) findViewById(R.id.addressRow);
				addressRow.setBackgroundColor(Color.parseColor("#008000"));
			}
		});
		missions.clear();
		for (ArrayList<SideMissionRow> list: closed.values()) list.clear();
		for (HashMap<String, StationStatusRow> map: bases.values()) map.clear();
		for (HashMap<String, AllyStatusRow> map: allies.values()) map.clear();
		for (ArrayList<String> list: rogues.values()) list.clear();
		closed.clear();
		bases.clear();
		allies.clear();
		rogues.clear();
		for (AllyStatus s: AllyStatus.values()) statuses.put(s, 0);

		new AsyncTask<String, String, String>() {

			@Override
			protected String doInBackground(String... params) {
				handlePackets();
				return null;
			}
			
		}.execute();
	}
	
	@Listener
	public void onDisconnect(DisconnectEvent event) {
		if (event.getCause() == DisconnectEvent.Cause.LOCAL_DISCONNECT) return;
		do {} while (objectControl);
		inPackets.clear();
		outPackets.clear();
		server.stop();
		server = null;
		host = null;
		final LinearLayout addressRow = (LinearLayout) findViewById(R.id.addressRow);
		final Spinner shipSpinner = (Spinner) findViewById(R.id.shipSpinner);
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.ship_name_spinner);
		updateHandler.removeCallbacks(updateEntities);
		updateRunning = false;
		dsHandler.removeCallbacks(askForTorps);
		dockingStation = null;
		routeHandler.removeCallbacks(updateRoute);
		routing = false;
		missions.clear();
		for (ArrayList<SideMissionRow> list: closed.values()) list.clear();
		for (HashMap<String, StationStatusRow> map: bases.values()) map.clear();
		for (HashMap<String, AllyStatusRow> map: allies.values()) map.clear();
		for (ArrayList<String> list: rogues.values()) list.clear();
		closed.clear();
		bases.clear();
		allies.clear();
		rogues.clear();
		uiThreadControl(new Runnable() {
			@Override
			public void run() {
				clearAllTables.run();
				shipSpinner.setAdapter(adapter);
				shipSpinner.setEnabled(true);
				addressRow.setBackgroundColor(Color.BLACK);
			}
		});
	}
	
	@Listener
	public void onPacket(GameOverPacket pkt) {
		missions.clear();
		for (HashMap<String, AllyStatusRow> map: allies.values()) map.clear();
		allies.clear();
		for (HashMap<String, StationStatusRow> map: bases.values()) map.clear();
		bases.clear();
		for (ArrayList<SideMissionRow> list: closed.values()) list.clear();
		closed.clear();
		for (ArrayList<String> list: rogues.values()) list.clear();
		rogues.clear();
		for (AllyStatus status: AllyStatus.values()) statuses.put(status, 0);
		dsHandler.removeCallbacks(askForTorps);
		inPackets.clear();
		outPackets.clear();
		dockingStation = null;
		final Spinner shipSpinner = (Spinner) findViewById(R.id.shipSpinner);
		uiThreadControl(new Runnable() {
			@Override
			public void run() {
				clearAllTables.run();
				shipSpinner.setEnabled(true);
			}
		});
	}
	
	@Listener
	public void onPacket(ObjectUpdatePacket pkt) {
		final Spinner shipSpinner = (Spinner) findViewById(R.id.shipSpinner);
		uiThreadControl(new Runnable() {
			@Override
			public void run() { shipSpinner.setEnabled(false); }
		});
		
		objectControl = true;
		List<ArtemisObject> objects = manager.getObjects(ObjectType.NPC_SHIP);
		for (ArtemisObject obj: objects) {
			ArtemisNpc npc = (ArtemisNpc) obj;
			if (npc.getVessel(context) == null || npc.getVessel(context).getFaction() == null || npc.getVessel(context).getFaction().getId() > 1) continue;
			if (allies.containsKey(npc.getName())) continue;
			if (npc.getVessel(context).is(VesselAttribute.FIGHTER) || npc.getVessel(context).is(VesselAttribute.SINGLESEAT)) continue;
			allies.put(npc.getName(), new HashMap<String, AllyStatusRow>());
			if (!rogues.containsKey(npc.getName())) rogues.put(npc.getName(), new ArrayList<String>());
			final AllyStatusRow row = new AllyStatusRow(
					getBaseContext(),
					npc.getName() + " " + npc.getVessel(context).getName(),
					(int) npc.getShieldsFront(),
					(int) npc.getShieldsRear(),
					(int) npc.getShieldsFrontMax(),
					(int) npc.getShieldsRearMax());
			allies.get(npc.getName()).put(npc.getVessel(context).getName(), row);
			uiThreadControl(new Runnable() {
				@Override
				public void run() {
					try { alliesTable.addView(row); }
					catch (IllegalStateException e) { }
				}
			});
			statuses.put(AllyStatus.NORMAL, statuses.get(AllyStatus.NORMAL) + 1);
			outPackets.add(new CommsOutgoingPacket(npc, OtherMessage.HAIL, context));
			dsHandler.postDelayed(askForTorps, dsInterval);
		}
		objects = manager.getObjects(ObjectType.BASE);
		for (ArtemisObject obj: objects) {
			final ArtemisBase base = (ArtemisBase) obj;
			if (base.getVessel(context) == null || base.getVessel(context).getFaction() == null || base.getVessel(context).getFaction().getId() > 1) continue;
			if (bases.containsKey(base.getName())) continue;
			bases.put(base.getName(), new HashMap<String, StationStatusRow>());
			final StationStatusRow row = new StationStatusRow(getBaseContext(), base, server, context);
			bases.get(base.getName()).put(base.getVessel(context).getName(), row);
			outPackets.add(new CommsOutgoingPacket(base, BaseMessage.PLEASE_REPORT_STATUS, context));
			uiThreadControl(new Runnable() {
				@Override
				public void run() {
					try { stationsTable.addView(row); }
					catch (IllegalStateException e) { }
				}
			});
		}
		if (dockingStation == null) for (ArtemisObject o: pkt.getObjects()) {
			if (o.getType() != ObjectType.PLAYER_SHIP) continue;
			ArtemisPlayer player = (ArtemisPlayer)manager.getObject(o.getId());
			if (player == null) continue;
			if (player.getShipNumber() == playerShip &&
					player.getDockingBase() > 0) {
				for (ArtemisObject b: objects) {
					if (b.getId() == player.getDockingBase()) {
						dockingStation = b.getName();
						uiThreadControl(updateDockedRow);
						stationHandler.postDelayed(waitForUndock, updateInterval);
						break;
					}
				}
			}
		}
		if (!updateRunning) {
			updateHandler.postDelayed(updateEntities, updateInterval);
		}
		if (!routing) {
			routeHandler.postDelayed(updateRoute, routeInterval);
		}
		objectControl = false;
	}
	
	@Listener
	public void onPacket(AllShipSettingsPacket pkt) {
		final Spinner shipSpinner = (Spinner) findViewById(R.id.shipSpinner);
		final int selIndex = shipSpinner.getSelectedItemPosition();
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
		for (int i = 1; i <= Artemis.SHIP_COUNT; i++) {
			adapter.add(pkt.getShip(i).getName());
		}
		uiThreadControl(new Runnable() {
			@Override
			public void run() {
				shipSpinner.setAdapter(adapter);
				shipSpinner.setSelection(selIndex);
			}
		});
	}
	
	@Listener
	public void onPacket(DestroyObjectPacket pkt) {
		objectControl = true;
		final ArtemisObject object = manager.getObject(pkt.getTarget());
		if (object instanceof ArtemisBase) {
			ArtemisBase base = (ArtemisBase) object;
			if (base.getVessel(context) == null || base.getVessel(context).getFaction() == null || base.getVessel(context).getFaction().getId() > 1) return;
			if (bases.containsKey(base.getName())) {
				for (final StationStatusRow row: bases.get(base.getName()).values()) {
					uiThreadControl(new Runnable() {
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
			ArtemisNpc npc = (ArtemisNpc) object;
			if (npc.getVessel(context) == null || npc.getVessel(context).getFaction() == null || npc.getVessel(context).getFaction().getId() > 1) return;
			if (allies.containsKey(npc.getName())) {
				final AllyStatusRow row = allies.get(npc.getName()).get(npc.getVessel(context).getName());
				uiThreadControl(new Runnable() {
					@Override
					public void run() {
						alliesTable.removeView(row);
					}
				});
				statuses.put(row.getStatus(), statuses.get(row.getStatus()) - 1);
				allies.get(npc.getName()).remove(npc.getVessel(context).getName());
				if (allies.get(npc.getName()).isEmpty()) allies.remove(npc.getName());
			}
			if (rogues.containsKey(npc.getName())) {
				rogues.get(npc.getName()).remove(npc.getVessel(context).getName());
				if (rogues.get(npc.getName()).isEmpty()) rogues.remove(npc.getName());
			}
		} else {
			objectControl = false;
			return;
		}
		uiThreadControl(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < missions.size(); i++) {
					SideMissionRow row = missions.get(i);
					if (row.isCompleted()) continue;
					if ((!row.isStarted() && row.getSource().startsWith(object.getName())) || row.getDestination().startsWith(object.getName())) {
						try {
							if (!row.isStarted() && row.getSource().startsWith(object.getName())) {
								String[] other = row.getDestination().split(" ", 3);
								try { allies.get(other[0]).get(other[2]).removeMission(); }
								catch (NullPointerException e) { bases.get(other[0]).get(other[2]).removeMission(); }
							}
							missionsTable.removeView(row);
						}
						catch (Exception e) {
//							Log.e(ListActivity.class.getName(), e.getLocalizedMessage(), e);
						}
						missions.remove(i--);
						if (object instanceof ArtemisNpc && closed.containsKey(object.getName())) closed.get(object.getName()).add(row);
					}
				}
			}
		});
		objectControl = false;
	}
	
	@Listener
	public void onPacket(CommsIncomingPacket pkt) {
		inPackets.add(pkt);
	}
	
	private void handlePackets() {
		while (true) {
			try {
				while (!inPackets.isEmpty()) {
					final CommsIncomingPacket pkt = inPackets.remove(0);
					final String sender = pkt.getFrom();
					final String message = pkt.getMessage();
					if (message.contains("\nOur shipboard computer")) {
						final String id = sender.substring(sender.lastIndexOf(" ") + 1);
						final String name = sender.split(" " + id)[0];
						uiThreadControl(new Runnable() {
							@Override
							public void run() {
								AllyStatusRow row = allies.get(id).get(name);
								setStatus(row, AllyStatus.BROKEN_COMPUTER);
								row.setEnergy(message.endsWith("you need some."));
							}
						});
					} else if (message.startsWith("Docking complete")) {
						for (ArtemisObject o: manager.getObjects(ObjectType.BASE)) {
							ArtemisBase base = (ArtemisBase) o;
							if (base.getId() == manager.getPlayerShip(playerShip).getDockingBase()) {
								dockingStation = sender.split(" ")[0];
								uiThreadControl(updateDockedRow);
								stationHandler.postDelayed(waitForUndock, updateInterval);
								break;
							}
						}
					} else if (message.startsWith("We've produced") || message.contains("ing production of")) {
						for (ArtemisObject o: manager.getObjects(ObjectType.BASE)) {
							if (!sender.startsWith(o.getName())) continue;
							outPackets.add(new CommsOutgoingPacket(o, BaseMessage.PLEASE_REPORT_STATUS, context));
							break;
						}
					} else if (message.contains("Our stores are")) {
						final String[] list = message.split("\n");
						final int shields = Integer.parseInt(list[0].split("at ")[1].split(" ")[0]);
						final String[] senderParts = sender.split(" ", 3);
						final StationStatusRow row = bases.get(senderParts[0]).get(senderParts[2]);
						uiThreadControl(new Runnable() {
							@Override
							public void run() {
								row.setShields(shields);
								for (OrdnanceType type: OrdnanceType.values()) {
									row.setStock(type, Integer.parseInt(list[type.ordinal() + 1].split("of")[0].trim()));
									if (list[list.length - 1].contains(type.toString())) row.setOrdnanceType(type);
								}
								if (list.length > 7) row.setFighters(Integer.parseInt(list[6].split(" ")[1]));
							}
						});
					} else if (message.contains("\nWe are holding this ship")) {
						final String id = sender.substring(sender.lastIndexOf(" ") + 1);
						final String name = sender.split(" " + id)[0];
						uiThreadControl(new Runnable() {
							@Override
							public void run() {
								setStatus(allies.get(id).get(name), AllyStatus.HOSTAGE);
							}
						});
					} else if (message.contains("\nWe have commandeered")) {
						final String id = sender.substring(sender.lastIndexOf(" ") + 1);
						final String name = sender.split(" " + id)[0];
						uiThreadControl(new Runnable() {
							@Override
							public void run() {
								setStatus(allies.get(id).get(name), AllyStatus.COMMANDEERED);
							}
						});
					} else if (message.contains("\nOur sensors are all down")) {
						final String id = sender.substring(sender.lastIndexOf(" ") + 1);
						final String name = sender.split(" " + id)[0];
						uiThreadControl(new Runnable() {
							@Override
							public void run() {
								AllyStatusRow row = allies.get(id).get(name);
								setStatus(row, AllyStatus.FLYING_BLIND);
								row.setEnergy(message.endsWith("you need some."));
							}
						});
					} else if (message.contains("\nOur engines are damaged")) {
						final String id = sender.substring(sender.lastIndexOf(" ") + 1);
						final String name = sender.split(" " + id)[0];
						uiThreadControl(new Runnable() {
							@Override
							public void run() {
								AllyStatusRow row = allies.get(id).get(name);
								setStatus(row, AllyStatus.NEED_DAMCON);
								row.setEnergy(message.endsWith("you need some."));
							}
						});
					} else if (message.contains("\nWe're out of energy")) {
						final String id = sender.substring(sender.lastIndexOf(" ") + 1);
						final String name = sender.split(" " + id)[0];
						uiThreadControl(new Runnable() {
							@Override
							public void run() {
								AllyStatusRow row = allies.get(id).get(name);
								setStatus(row, AllyStatus.NEED_ENERGY);
	//							row.setEnergy(message.endsWith("you need some."));
							}
						});
					} else if (message.startsWith("Torpedo transfer")) {
						uiThreadControl(new Runnable() {
							@Override
							public void run() {
								for (AllyStatusRow row: allies.get(sender).values()) row.setTorpedoes(false);
							}
						});
					} else if (message.startsWith("Here's the energy")) {
						uiThreadControl(new Runnable() {
							@Override
							public void run() {
								for (AllyStatusRow row: allies.get(sender).values()) row.setEnergy(false);
							}
						});
					} else if (message.endsWith("when we get there.")) {
						uiThreadControl(new Runnable() {
							@Override
							public void run() {
								for (AllyStatusRow row: allies.get(sender).values()) setStatus(row, AllyStatus.REWARD);
							}
						});
					} else if (message.endsWith("Could you come help us?") || message.endsWith("How are you?")) {
						final String id = sender.substring(sender.lastIndexOf(" ") + 1);
						final String name = sender.split(" " + id)[0];
						if (!rogues.containsKey(id)) rogues.put(id, new ArrayList<String>());
						rogues.get(id).add(name);
						uiThreadControl(new Runnable() {
							@Override
							public void run() {
								try { setStatus(allies.get(id).get(name), message.endsWith("How are you?") ? AllyStatus.MINE_TRAP : AllyStatus.FIGHTERS); }
								catch (NullPointerException e) { }
								for (int i = 0; i < missions.size(); i++) {
									SideMissionRow row = (SideMissionRow) missions.get(i);
									if (row.isCompleted()) continue;
									if ((!row.isStarted() && row.getSource().startsWith(id)) || row.getDestination().startsWith(id)) {
										missions.remove(i--);
									}
								}
								clearTable.run();
								updateTables.run();
							}
						});
					} else if (message.contains("and we'll")) {
						final String[] senderParts = sender.split(" ", 3);
						if (rogues.containsKey(senderParts[0]) && rogues.get(senderParts[0]).contains(senderParts[2])) return;
						final String srcShip = message.split("with ")[1].split(" ")[0];
						final String source;
						if (srcShip.startsWith("DS")) {
							if (!bases.containsKey(srcShip)) continue;
							String srcStation = "";
							for (String key: bases.get(srcShip).keySet()) srcStation = key;
							source = srcShip + " Terran " + srcStation;
							final String station = srcStation;
							uiThreadControl(new Runnable() {
								@Override
								public void run() {
									bases.get(srcShip).get(station).addMission();
									try { allies.get(senderParts[0]).get(senderParts[2]).addMission(); }
									catch (NullPointerException e) { bases.get(senderParts[0]).get(senderParts[2]).addMission(); }
								}
							});
						} else {
							if (rogues.containsKey(srcShip) && allies.get(srcShip).size() == rogues.get(srcShip).size()) continue;
							if (!allies.containsKey(srcShip)) continue;
							String srcString = srcShip;
							if (allies.get(srcShip).size() == 1) {
								srcString += " Terran ";
								for (String s: allies.get(srcShip).keySet()) srcString += s;
							}
							source = srcString;
							if (allies.get(srcShip).size() > 1 && !closed.containsKey(srcShip)) closed.put(srcShip, new ArrayList<SideMissionRow>());
							uiThreadControl(new Runnable() {
								@Override
								public void run() {
									allies.get(srcShip).get(source.substring(srcShip.length() + 8)).addMission();
									try { allies.get(senderParts[0]).get(senderParts[2]).addMission(); }
									catch (NullPointerException e) { bases.get(senderParts[0]).get(senderParts[2]).addMission(); }
								}
							});
						}
						String[] words = message.split(" ");
						final String reward = words[words.length - 1];
						uiThreadControl(new Runnable() {
							@Override
							public void run() {
								SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
								boolean show;
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
								boolean exists = false;
								for (SideMissionRow row: missions) {
									if (row.isStarted() || !row.getSource().contains(" ") || !row.getSource().startsWith(source) || !row.getDestination().equals(sender)) continue;
									row.addReward(reward);
									exists = true;
									break;
								}
								if (!exists) {
									SideMissionRow newRow = new SideMissionRow(getBaseContext(), source, sender, reward);
									missions.add(newRow);
									if (show) missionsTable.addView(newRow);
								} else if (show) {
									clearTable.run();
									updateTables.run();
								}
							}
						});
					} else if (message.contains("to deliver the")) {
	//					Log.d(getClass().getName(), message);
						String destination = message.split(" to ")[1];
						for (int i = 0; i < missions.size(); i++) {
							final SideMissionRow row = missions.get(i);
							if (row.isStarted() || !sender.startsWith(row.getSource()) || !row.getDestination().startsWith(destination)) continue;
							uiThreadControl(new Runnable() {
								@Override
								public void run() {
									row.updateSource(sender);
									row.markAsStarted();
									for (int i = 0; i < missions.size(); i++) {
										SideMissionRow other = missions.get(i);
										if (other.equals(row) || !other.isStarted() || other.isCompleted() || !other.getSource().equals(sender) || !other.getDestination().equals(row.getDestination())) continue;
										String[] rewards = row.getRewardList().split(", ");
										for (String s: rewards) {
											int quantity = 1;
											if (s.charAt(s.length() - 2) == 'x') {
												quantity = Integer.parseInt(s.substring(s.length() - 1));
												s = s.split(" x")[0];
											}
											for (int t = 0; t < quantity; t++) other.addReward(s);
										}
										try { missionsTable.removeView(row); }
										catch (Exception e) {
	//												Log.e(ListActivity.class.getName(), e.getLocalizedMessage(), e);
										}
										missions.remove(row);
										break;
									}
									clearTable.run();
									updateTables.run();
								}
							});
						}
						final String[] senderParts = sender.split(" ", 3);
						String id = senderParts[0];
						uiThreadControl(new Runnable() {
							@Override
							public void run() {
								try { allies.get(senderParts[0]).get(senderParts[2]).removeMission(); }
								catch (NullPointerException e) { bases.get(senderParts[0]).get(senderParts[2]).removeMission(); }
							}
						});
						if (!closed.containsKey(id)) continue;
						for (int i = 0; i < closed.get(id).size(); i++) {
							final SideMissionRow row = missions.get(i);
							if (row.isStarted() || !sender.startsWith(row.getSource()) || !row.getDestination().startsWith(destination)) continue;
							missions.add(row);
							closed.get(id).remove(row);
							uiThreadControl(new Runnable() {
								@Override
								public void run() {
									row.updateSource(sender);
									row.markAsStarted();
									missionsTable.addView(row);
								}
							});
							i--;
						}
					} else if (message.startsWith("As promised")) {
	//					Log.d(getClass().getName(), message);
						String[] words = message.split(" ");
						String reward = words[words.length - 1];
						for (int i = 0; i < missions.size(); i++) {
							final SideMissionRow row = missions.get(i);
							if (row.isCompleted() || !row.isStarted() || !row.getDestination().equals(sender) || !row.hasReward(reward)) continue;
							uiThreadControl(new Runnable() {
								@Override
								public void run() {
									row.markAsCompleted();
									row.setOnClickListener(new OnClickListener() {
	
										@Override
										public void onClick(View arg0) {
											missionsTable.removeView(row);
											missions.remove(row);
										}
										
									});
								}
							});
						}
						final String[] senderParts = sender.split(" ", 3);
						String id = senderParts[0];
						uiThreadControl(new Runnable() {
							@Override
							public void run() {
								try { allies.get(senderParts[0]).get(senderParts[2]).removeMission(); }
								catch (NullPointerException e) { bases.get(senderParts[0]).get(senderParts[2]).removeMission(); }
							}
						});
						if (!closed.containsKey(id)) continue;
						for (int i = 0; i < closed.get(id).size(); i++) {
							final SideMissionRow row = closed.get(id).get(i);
							if (row.isCompleted() || !row.isStarted() || !row.getDestination().equals(sender) || !row.hasReward(reward)) continue;
							missions.add(row);
							closed.get(id).remove(row);
							uiThreadControl(new Runnable() {
								@Override
								public void run() {
									row.markAsCompleted();
									missionsTable.addView(row);
									row.setOnClickListener(new OnClickListener() {
	
										@Override
										public void onClick(View arg0) {
											missionsTable.removeView(row);
											missions.remove(row);
										}
										
									});
								}
							});
							i--;
						}
					} else if (message.endsWith("Enjoy your reward!")) {
						uiThreadControl(new Runnable() {
							@Override
							public void run() {
								for (ArtemisObject o: manager.getObjects(ObjectType.BASE)) {
									outPackets.add(new CommsOutgoingPacket(o, BaseMessage.PLEASE_REPORT_STATUS, context));
								}
								for (AllyStatusRow row: allies.get(sender).values()) {
									row.setBlind(false);
									row.setStatus(AllyStatus.NORMAL);
								}
							}
						});
					} else if (message.endsWith("Let us upgrade your shield generators.")) {
						uiThreadControl(new Runnable() {
							@Override
							public void run() {
								for (AllyStatusRow row: allies.get(sender).values())
									setStatus(row, AllyStatus.NORMAL);
							}
						});
					} else if (message.contains("we are turning")) {
						uiThreadControl(new Runnable() {
							@Override
							public void run() {
								for (AllyStatusRow row: allies.get(sender).values())
									if (row.getStatus() == AllyStatus.FLYING_BLIND) setStatus(row, AllyStatus.NORMAL);
							}
						});
					} else if (message.startsWith("Okay, going")) {
						uiThreadControl(new Runnable() {
							@Override
							public void run() {
								for (AllyStatusRow row: allies.get(sender).values()) {
									if (row.isFlyingBlind()) {
										setStatus(row, AllyStatus.REWARD);
									}
								}
							}
						});
					} else if (message.startsWith("Our shields") && !message.contains("\nWe're heading to the station") && !sender.startsWith("DS")) {
						final String id = sender.substring(sender.lastIndexOf(" ") + 1);
						final String name = sender.split(" " + id)[0];
						uiThreadControl(new Runnable() {
							@Override
							public void run() {
								AllyStatusRow row;
								while (true) {
									try {
										row = allies.get(id).get(name);
										break;
									} catch (NullPointerException e) { }
								}
								if (row.getStatus() != AllyStatus.REWARD) setStatus(row, AllyStatus.NORMAL);
								row.setEnergy(message.endsWith("you need some."));
								row.setTorpedoes(message.contains("We have some torpe"));
							}
						});
					}
				}
				if (!outPackets.isEmpty()) server.send(outPackets.remove(0));
			} catch (NullPointerException e) { break; }
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (server == null || !server.isConnected()) return;
		String allyList = "", missionList = "", stationList = "";
		for (String key: allies.keySet()) {
			for (AllyStatusRow row: allies.get(key).values()) {
				if (row.getStatus().ordinal() < AllyStatus.FIGHTERS.ordinal()) {
					if (!allyList.equals("")) allyList += "/";
					allyList += key;
				}
			}
		}
		for (String key: bases.keySet()) {
			for (@SuppressWarnings("unused") String name: bases.get(key).keySet()) {
				if (!stationList.equals("")) stationList += "/";
				stationList += key;
			}
		}
		for (SideMissionRow row: missions) {
			if (!row.isCompleted()) {
				if (!missionList.equals("")) missionList += "/";
				missionList += row.getSource().split(" ")[0] + "," + row.getDestination().split(" ")[0];
			}
		}
		int location = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.vesselDataKey), "0"));
		Intent startBackground = new Intent(this, CommsService.class).putExtra("Server", host).putExtra("Allies", allyList).putExtra("Missions", missionList).putExtra("Stations", stationList).putExtra("VesselData", location);
		startService(startBackground);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		stopService(new Intent(ListActivity.this, CommsService.class));
	}
	
	final Runnable clearTable = new Runnable() {
		@Override
		public void run() {
			missionsTable.removeAllViews();
		}
	};
	
	final Runnable clearAllTables = new Runnable() {
		@Override
		public void run() {
			missionsTable.removeAllViews();
			alliesTable.removeAllViews();
			stationsTable.removeAllViews();
		}
	};
	
	final Runnable updateTables = new Runnable() {
		@Override
		public void run() {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			for (SideMissionRow row: missions) {
				missionsTable.addView(row);
				if (row.hasReward(SideMissionRow.BATTERY_KEY) && preferences.getBoolean(getString(R.string.batteryChargeKey), true)) continue;
				if (row.hasReward(SideMissionRow.COOLANT_KEY) && preferences.getBoolean(getString(R.string.extraCoolantKey), true)) continue;
				if (row.hasReward(SideMissionRow.NUCLEAR_KEY) && preferences.getBoolean(getString(R.string.nuclearKey), true)) continue;
				if (row.hasReward(SideMissionRow.PRODUCTION_KEY) && preferences.getBoolean(getString(R.string.speedKey), true)) continue;
				if (row.hasReward(SideMissionRow.SHIELD_KEY) && preferences.getBoolean(getString(R.string.shieldKey), true)) continue;
				missionsTable.removeView(row);
			}
			
			for (String n: bases.keySet()) {
				for (StationStatusRow r: bases.get(n).values()) {
					try { stationsTable.addView(r); }
					catch (Exception e) { }
				}
			}
			
			for (String n: allies.keySet()) {
				for (AllyStatusRow r: allies.get(n).values()) {
					try { alliesTable.addView(r); }
					catch (Exception e) { }
				}
			}
		}
	};
	
	private void setStatus(AllyStatusRow row, AllyStatus status) {
		statuses.put(row.getStatus(), statuses.get(row.getStatus()) - 1);
		alliesTable.removeView(row);
		row.setStatus(status);
		alliesTable.addView(row, statuses.get(status));
		statuses.put(status, statuses.get(status) + 1);
	}
	
	final Runnable updateDockedRow = new Runnable() {
		@Override
		public void run() {
			for (StationStatusRow row: bases.get(dockingStation).values()) {
				row.setDocking(true);
			}
		}
	};
	
	final Runnable resetDockedRow = new Runnable() {
		@Override
		public void run() {
			for (StationStatusRow row: bases.get(dockingStation).values()) {
				row.setDocking(false);
			}
		}
	};
	
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
					uiThreadControl(resetDockedRow);
					dockingStation = null;
				} else stationHandler.postDelayed(this, updateInterval);
			}
		}
	};
	
	private boolean uiThreadInUse;
	
	private void uiThreadControl(Runnable run) {
		do {} while (uiThreadInUse);
		uiThreadInUse = true;
		runOnUiThread(run);
		uiThreadInUse = false;
	}
	
	final Runnable askForTorps = new Runnable() {
		@Override
		public void run() {
			for (ArtemisObject obj: manager.getObjects(ObjectType.NPC_SHIP)) {
				try {
					for (AllyStatusRow row: allies.get(obj.getName()).values()) {
						if (!row.isBuildingTorpedoes()) continue;
						outPackets.add(new CommsOutgoingPacket(obj, OtherMessage.HAIL, context));
					}
				} catch (NullPointerException e) { }
			}
			dsHandler.postDelayed(this, dsInterval);
		}
	};
	
	final Runnable updateRoute = new Runnable() {
		@Override
		public void run() {
			routing = true;
			uiThreadControl(new Runnable() {
				@Override
				public void run() {
					routeTable.removeAllViews();
				}
			});
			if (routeView.getVisibility() == View.VISIBLE) {
				final RoutingGraph graph = new RoutingGraph(manager, manager.getPlayerShip(playerShip).getId());
				
				for (SideMissionRow row: missions) {
					if (row.isCompleted()) continue;

					SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
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
					if (!show) continue;
					
					String srcCall = row.getSource().split(" ")[0];
					String destCall = row.getDestination().split(" ")[0];
					
					if (row.isStarted()) {
						srcCall = destCall;
						destCall = null;
					}
					
					if (!bases.containsKey(srcCall) && !allies.containsKey(srcCall)) continue;
					if (destCall != null && !bases.containsKey(destCall) && !allies.containsKey(destCall)) continue;
					
					ArtemisObject srcObject = null, destObject = null;
					
					List<ArtemisObject> points = new ArrayList<ArtemisObject>(manager.getObjects(ObjectType.BASE));
					points.addAll(manager.getObjects(ObjectType.NPC_SHIP));
					
					for (ArtemisObject obj: points) {
						BaseArtemisShielded npc = (BaseArtemisShielded) obj;
						if (npc.getVessel(context) == null || npc.getVessel(context).getFaction() == null || npc.getVessel(context).getFaction().getId() > 1) continue;
						if (!npc.getName().equals(srcCall)) continue;
						if (npc.getVessel(context).is(VesselAttribute.FIGHTER) || npc.getVessel(context).is(VesselAttribute.SINGLESEAT)) continue;
						srcObject = npc;
						break;
					}
					
					if (destCall != null) {
						for (ArtemisObject obj: points) {
							BaseArtemisShielded npc = (BaseArtemisShielded) obj;
							if (npc.getVessel(context) == null || npc.getVessel(context).getFaction() == null || npc.getVessel(context).getFaction().getId() > 1) continue;
							if (!npc.getName().equals(destCall)) continue;
							if (npc.getVessel(context).is(VesselAttribute.FIGHTER) || npc.getVessel(context).is(VesselAttribute.SINGLESEAT)) continue;
							destObject = npc;
							break;
						}
					}
					
					graph.addPath(srcObject, destObject);
				}
				
				final ArrayList<Integer> route = graph.calculateRoute();
				
				uiThreadControl(new Runnable() {
					@Override
					public void run() {
						for (int id: route) {
							BaseArtemisShielded object = (BaseArtemisShielded)manager.getObject(id);
							String objName = object.getName() + " Terran " + object.getVessel(context).getName();
							float distance = graph.getDistanceFromShip(id);
							RouteEntryRow newRow = new RouteEntryRow(getBaseContext(), objName, distance);
							routeTable.addView(newRow);
						}
					}
				});
			}
			routeHandler.postDelayed(this, routeInterval);
		}
	};
	
	final Runnable updateEntities = new Runnable() {
		@Override
		public void run() {
			updateRunning = true;
			for (ArtemisObject o: manager.getObjects(ObjectType.NPC_SHIP)) {
				ArtemisNpc npc = (ArtemisNpc) o;
				if (npc.getVessel(context) == null || npc.getVessel(context).getFaction() == null || npc.getVessel(context).getFaction().getId() > 1) continue;
				try {
					AllyStatusRow row = allies.get(npc.getName()).get(npc.getVessel(context).getName());
					row.setFront((int) npc.getShieldsFront());
					row.setRear((int) npc.getShieldsRear());
				} catch (NullPointerException e) {}
			}
			for (ArtemisObject o: manager.getObjects(ObjectType.BASE)) {
				ArtemisBase base = (ArtemisBase) o;
				if (base.getVessel(context) == null) continue;
				try {
					StationStatusRow row = bases.get(base.getName()).get(base.getVessel(context).getName());
					row.setShields((int) base.getShieldsFront());
				} catch (NullPointerException e) {}
			}
			updateHandler.postDelayed(this, updateInterval);
		}
	};

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Toast.makeText(this, "Updating preferences...", Toast.LENGTH_SHORT).show();
		if (key.equals(getString(R.string.vesselDataKey))) {
			if (assetsFail) assetsFail = false;
			else if (server != null && server.isConnected()) createConnection();
		} else if (key.endsWith("CheckBox")) {
			uiThreadControl(clearTable);
			uiThreadControl(updateTables);
		}
	}
}