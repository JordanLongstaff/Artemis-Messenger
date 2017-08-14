package artemis.messenger;

import java.util.ArrayList;

import com.walkertribe.ian.iface.DisconnectEvent;
import com.walkertribe.ian.protocol.core.GameOverReasonPacket;
import com.walkertribe.ian.protocol.core.comm.CommsIncomingPacket;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import artemis.messenger.R;

public class CommsService extends Service {
	// Copy a lot of information from the app
	private NotificationCompat.Builder builder;
	private NotificationManager manager;
	private ArrayList<String> senders, messages;
	private int stations, allies, missions;
	private ArtemisBinder binder;
	
	public static final String BROADCAST = "artemis.messenger.BROADCAST";
	public static final String TITLE = "Artemis Messenger";

	@Override
	public void onCreate() {
		HandlerThread thread = new HandlerThread(getClass().getName(), Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		
		binder = new ArtemisBinder();
		builder = new NotificationCompat.Builder(getBaseContext())
				.setSmallIcon(R.drawable.ic_launcher);
		manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		senders = new ArrayList<String>();
		messages = new ArrayList<String>();
		messages.add("");
	}
	
	public void update(int numAllies, int numStations, int numMissions) {
		// Refresh fields
		allies = numAllies;
		stations = numStations;
		missions = numMissions;
		
		// Set up notification showing number of missions/allies/stations
		Intent notIntent = new Intent(this, ListActivity.class);
		notIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pending = PendingIntent.getActivity(this, 0, notIntent, 0);
		
		builder
		.setStyle(new NotificationCompat.BigTextStyle()
				.bigText(getMainMessage())
				.setBigContentTitle(TITLE))
		.setContentTitle(TITLE)
		.setContentText(getMainMessage())
		.setContentIntent(pending)
		.setOngoing(true);
		manager.notify(0, builder.build());
	}
	
	// Parse lists given from intent
	private String getMainMessage() {
		String message = "";
		if (stations > 0) {
			message += stations + " station";
			if (stations > 1) message += "s";
		}
		if (allies > 0) {
			if (!message.equals("")) message += ", ";
			if (allies == 1) message += "1 ally";
			else message += allies + " allies";
		}
		if (missions > 0) {
			if (!message.equals("")) message += ", ";
			message += missions + " mission";
			if (missions > 1) message += "s";
		}
		return message;
	}
	
	// Comms service's incoming message protocol
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
			builder
			.setStyle(new NotificationCompat.BigTextStyle()
					.bigText(message)
					.setBigContentTitle(sender))
			.setContentTitle(sender)
			.setContentText(message)
			.setContentIntent(pending)
			.setOngoing(false);
			manager.notify(messages.size(), builder.build());
			
			// Add information to appropriate lists
			senders.add(sender);
			messages.add(message);
			
			// Refresh top notification
			builder
			.setStyle(new NotificationCompat.BigTextStyle()
					.bigText(getMainMessage())
					.setBigContentTitle(TITLE))
			.setContentTitle(TITLE)
			.setContentText(getMainMessage())
			.setContentIntent(pending)
			.setOngoing(true);
			manager.notify(0, builder.build());
		} else if (message.contains("to deliver the")) {
			// Play "arrived at source" ringtone
			Uri ringtoneUri = Uri.parse(pref.getString(getString(R.string.foundSrcPrefKey), "Default"));
			Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
			ringtone.play();
			
			// Create notification containing message
			builder
			.setStyle(new NotificationCompat.BigTextStyle()
					.bigText(message)
					.setBigContentTitle(sender))
			.setContentTitle(sender)
			.setContentText(message)
			.setContentIntent(pending)
			.setOngoing(false);
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
			
			// Refresh top notification
			senders.add(sender);
			messages.add(message);
			builder
			.setStyle(new NotificationCompat.BigTextStyle()
					.bigText(getMainMessage())
					.setBigContentTitle(TITLE))
			.setContentTitle(TITLE)
			.setContentText(getMainMessage())
			.setContentIntent(pending)
			.setOngoing(true);
			manager.notify(0, builder.build());
			
			// Create notification containing message
			builder
			.setStyle(new NotificationCompat.BigTextStyle()
					.bigText(message)
					.setBigContentTitle(sender))
			.setContentTitle(sender)
			.setContentText(message)
			.setContentIntent(pending)
			.setOngoing(false);
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
		}
	}
	
	// Destroy Comms service when returning to the app
	@Override
	public void onDestroy() {
		manager.cancelAll();
	}
	
	public void onDisconnect(DisconnectEvent e) {
		// Destroy all notifications
		manager.cancelAll();
		
		// Set up a notification about the disconnect event
		Intent intent = new Intent(this, ListActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pending = PendingIntent.getActivity(this, 0, intent, 0);
		builder
		.setStyle(new NotificationCompat.BigTextStyle()
				.bigText(e.getCause().name().replaceAll("_", " "))
				.setBigContentTitle(TITLE))
		.setContentTitle(TITLE)
		.setContentText(e.getCause().name().replaceAll("_", " "))
		.setContentIntent(pending)
		.setOngoing(false);
		manager.notify(0, builder.build());
	}
	
	public void onPacket(GameOverReasonPacket pkt) {
		// Destroy all notifications
		manager.cancelAll();
		
		// Set up a notification about the game over event
		Intent intent = new Intent(this, ListActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pending = PendingIntent.getActivity(this, 0, intent, 0);
		
		StringBuilder b = new StringBuilder();
		for (String line: pkt.getText()) b.append("\n" + line);
		String message = b.substring(14);
		
		builder
		.setStyle(new NotificationCompat.BigTextStyle()
				.bigText(message)
				.setBigContentTitle(TITLE))
		.setContentTitle(TITLE)
		.setContentText(message)
		.setContentIntent(pending)
		.setOngoing(false);
		manager.notify(0, builder.build());
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	public class ArtemisBinder extends Binder {
		public CommsService getService() {
			return CommsService.this;
		}
	}
}