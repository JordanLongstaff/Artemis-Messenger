package artemis.messenger;

import java.io.IOException;
import java.util.ArrayList;

import com.walkertribe.ian.enums.Console;
import com.walkertribe.ian.iface.ArtemisNetworkInterface;
import com.walkertribe.ian.iface.ConnectionSuccessEvent;
import com.walkertribe.ian.iface.Listener;
import com.walkertribe.ian.iface.ThreadedArtemisNetworkInterface;
import com.walkertribe.ian.protocol.core.comm.CommsIncomingPacket;
import com.walkertribe.ian.protocol.core.setup.ReadyPacket;
import com.walkertribe.ian.protocol.core.setup.SetConsolePacket;
import com.walkertribe.ian.protocol.core.setup.SetShipPacket;
import com.walkertribe.ian.protocol.core.world.DestroyObjectPacket;
import com.walkertribe.ian.vesseldata.FilePathResolver;
import com.walkertribe.ian.vesseldata.PathResolver;
import com.walkertribe.ian.world.ArtemisBase;
import com.walkertribe.ian.world.ArtemisNpc;
import com.walkertribe.ian.world.SystemManager;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import artemis.messenger.R;

public class CommsService extends Service {
	// Copy a lot of information from the app
	private com.walkertribe.ian.Context context;
	private NotificationCompat.Builder builder;
	private NotificationManager manager;
	private ArrayList<String> senders, messages, missions;
	private ArtemisNetworkInterface server;
	private String stations, allies;
	private SystemManager gameManager;
	private int playerShip;
	
	public static final String BROADCAST = "artemis.messenger.BROADCAST";
	public static final String TITLE = "Artemis Messenger";

	@Override
	public void onCreate() {
		HandlerThread thread = new HandlerThread(getClass().getName(), Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		
		builder = new NotificationCompat.Builder(getBaseContext()).setSmallIcon(R.drawable.ic_launcher);
		manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		senders = new ArrayList<String>();
		messages = new ArrayList<String>();
		missions = new ArrayList<String>();
		messages.add("");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startID) {
		// Get information from intent
		final String host = intent.getStringExtra("Server");
		playerShip = intent.getIntExtra("Ship", 1);
		allies = intent.getStringExtra("Allies");
		stations = intent.getStringExtra("Stations");
		String[] miss = intent.getStringExtra("Missions").split("/");
		final int location = intent.getIntExtra("VesselData", 0);
		
		// Count running missions
		if (miss.length > 1 || !miss[0].equals("")) for (String s: miss) missions.add(s);
		
		// Start up connection for Comms service
		if (host != null) new Thread(new Runnable() {
			@Override public void run() {
				try {
					PathResolver resolver;
					switch (location) {
					case 1:
						resolver = new FilePathResolver(getFilesDir() + "/artemis");
						break;
					case 2:
						resolver = new FilePathResolver(Environment.getExternalStorageDirectory().toString() +
								"/artemis");
						break;
					default:
						resolver = new AssetsResolver(getAssets());
					}
					context = new com.walkertribe.ian.Context(resolver);
					server = new ThreadedArtemisNetworkInterface(host, 2010, context);
					server.addListener(CommsService.this);
					gameManager = new SystemManager(context);
					server.addListener(manager);
					server.start();
				} catch (IOException e) { }
			}
		}).start();
		
		// Set up notification showing number of missions/allies/stations
		Intent notIntent = new Intent(this, ListActivity.class);
		notIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pending = PendingIntent.getActivity(this, 0, notIntent, 0);
		
		builder.setContentTitle(TITLE).setContentText(getMainMessage()).setContentIntent(pending).setOngoing(true);
		manager.notify(0, builder.build());
		return START_NOT_STICKY;
	}
	
	// Parse lists given from intent
	private String getMainMessage() {
		String message = "";
		int numStations = stations.split("/").length;
		if (!stations.equals("")) {
			if (numStations == 1) message += "1 station";
			else message += numStations + " stations";
		}
		int numAllies = allies.split("/").length;
		if (!allies.equals("")) {
			if (!message.equals("")) message += ", ";
			if (numAllies == 1) message += "1 ally";
			else message += numAllies + " allies";
		}
		if (!missions.isEmpty()) {
			if (!message.equals("")) message += ", ";
			message += missions.size() + " mission";
			if (missions.size() > 1) message += "s";
		}
		return message;
	}
	
	// Comms service's destroyed object protocol
	@Listener
	public void onPacket(DestroyObjectPacket pkt) {
		switch (pkt.getTargetType()) {
		case NPC_SHIP:
			// Destroyed object is an ally ship
			ArtemisNpc ship = (ArtemisNpc) gameManager.getObject(pkt.getTarget());
			if (ship.getVessel(context) == null ||
					ship.getVessel(context).getFaction() == null ||
					ship.getVessel(context).getFaction().getId() > 1) return;
			if (!allies.contains(ship.getName())) return;
			
			// Set up Intent to return to app
			Intent intent = new Intent(this, ListActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			PendingIntent pending = PendingIntent.getActivity(this, 0, intent, 0);
			
			// Remove ally ship from list
			String allyList = "";
			for (String name: allies.split("/")) {
				if (!name.startsWith(ship.getName())) {
					// If this isn't it, maintain current order in list
					if (!allyList.equals("")) allyList += "/";
					allyList += name;
				} else {
					// If this is it, remove any missions associated with it
					for (int i = 0; i < missions.size(); i++) {
						if (missions.get(i).contains(ship.getName())) {
							missions.remove(i--);
						}
					}
				}
			}
			
			// Refresh ally ship list
			allies = allyList;
			
			// Refresh top notification
			builder.setContentTitle(TITLE).setContentText(getMainMessage()).setContentIntent(pending).setOngoing(true);
			manager.notify(0, builder.build());
			break;
		case BASE:
			// Destroyed object is a station
			ArtemisBase base = (ArtemisBase) gameManager.getObject(pkt.getTarget());
			if (!stations.contains(base.getName())) return;

			// Set up Intent to return to app
			intent = new Intent(this, ListActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			pending = PendingIntent.getActivity(this, 0, intent, 0);
			
			// Remove station from list
			String stationList = "";
			for (String name: stations.split("/")) {
				if (!name.startsWith(base.getName())) {
					// If this isn't it, maintain current order in list
					if (!stationList.equals("")) stationList += "/";
					stationList += name;
				} else {
					// If this is it, remove any missions associated with it
					for (int i = 0; i < missions.size(); i++) {
						if (missions.get(i).contains(base.getName())) {
							missions.remove(i--);
						}
					}
				}
			}
			
			// Refresh station list
			stations = stationList;
			
			// Refresh top notification
			builder.setContentTitle(TITLE).setContentText(getMainMessage()).setContentIntent(pending).setOngoing(true);
			manager.notify(0, builder.build());
			break;
		default:
			return;
		}
	}
	
	// Comms service's incoming message protocol
	@Listener
	public void onPacket(CommsIncomingPacket pkt) {
		// Get message info
		String sender = pkt.getFrom();
		String message = pkt.getMessage();

		// Set up Intent to return to app
		Intent intent = new Intent(this, ListActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pending = PendingIntent.getActivity(this, 0, intent, 0);
		
		// Get ringtone preferences
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (message.contains("and we'll")) {
			// Play "new mission" ringtone
			Uri ringtoneUri = Uri.parse(pref.getString(getString(R.string.newMissionPrefKey), "Default"));
			Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
			ringtone.play();
			
			// Create notification containing message
			builder.setContentTitle(sender).setContentText(message).setContentIntent(pending).setOngoing(false);
			manager.notify(messages.size(), builder.build());
			
			// Add information to appropriate lists
			senders.add(sender);
			messages.add(message);
			missions.add(message.split("with ")[1].split(" ")[0] + "," + sender.split(" ")[0]);
			
			// Refresh top notification
			builder.setContentTitle(TITLE).setContentText(getMainMessage()).setContentIntent(pending).setOngoing(true);
			manager.notify(0, builder.build());
		} else if (message.contains("to deliver the")) {
			// Play "arrived at source" ringtone
			Uri ringtoneUri = Uri.parse(pref.getString(getString(R.string.foundSrcPrefKey), "Default"));
			Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
			ringtone.play();
			
			// Remove source from appropriate side mission
			for (int i = 0; i < missions.size(); i++) {
				if (!sender.startsWith(missions.get(i).split(",")[0])) continue;
				String dest = message.split(" to ")[1];
				if (!missions.get(i).endsWith(dest)) continue;
				missions.set(i, dest);
			}
			
			// Create notification containing message
			builder.setContentTitle(sender).setContentText(message).setContentIntent(pending).setOngoing(false);
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
				manager.notify(i, builder.build());
				senders.set(i - 1, sender);
				messages.set(i, message);
				return;
			}
			manager.notify(messages.size(), builder.build());
			senders.add(sender);
			messages.add(message);
		} else if (message.startsWith("As promised")) {
			// Play "arrived at destination" ringtone
			Uri ringtoneUri = Uri.parse(pref.getString(getString(R.string.foundDestPrefKey), "Default"));
			Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
			ringtone.play();
			
			// Remove side mission from list
			for (int i = 0; i < missions.size(); i++) {
				if (!sender.startsWith(missions.get(i))) continue;
				missions.remove(i--);
			}
			
			// Refresh top notification
			builder.setContentTitle(TITLE).setContentText(getMainMessage()).setContentIntent(pending).setOngoing(true);
			manager.notify(0, builder.build());
			
			// Create notification containing message
			builder.setContentTitle(sender).setContentText(message).setContentIntent(pending).setOngoing(false);
			for (int i = 1; i < messages.size(); i++) {
				// If the notification about arriving at the source still exists, replace it instead
				if (!messages.get(i).contains("to deliver the")) continue;
				String src = messages.get(i).split("with ")[1].split(" ")[0];
				if (!sender.startsWith(src)) continue;
				String dest = message.split(" to ")[1];
				if (!senders.get(i - 1).startsWith(dest)) continue;
				manager.notify(i, builder.build());
				senders.set(i - 1, sender);
				messages.set(i, message);
				return;
			}
			manager.notify(messages.size(), builder.build());
			senders.add(sender);
			messages.add(message);
		}
	}
	
	// Set up Comms client within Comms service
	@Listener
	public void onConnectSuccess(ConnectionSuccessEvent e) {
		server.send(new SetShipPacket(playerShip));
		server.send(new SetConsolePacket(Console.COMMUNICATIONS, true));
		server.send(new ReadyPacket());
	}
	
	// Destroy Comms service when returning to the app
	@Override
	public void onDestroy() {
		if (server != null) server.stop();
		manager.cancelAll();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}