package artemis.messenger;

import java.util.Date;
import java.util.HashMap;

import com.walkertribe.ian.enums.BaseMessage;
import com.walkertribe.ian.enums.OrdnanceType;
import com.walkertribe.ian.iface.ArtemisNetworkInterface;
import com.walkertribe.ian.protocol.core.comm.CommsOutgoingPacket;
import com.walkertribe.ian.world.ArtemisBase;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * A row in the Stations table, that shows information about a station and can be touched to request preparations for
 * docking.
 * @author Jordan Longstaff
 *
 */
public class StationStatusRow extends TableRow {
	// Station info
	private int shields, missions, fighters;
	private final int maxShields;
	private final int[] ordnance;
	private OrdnanceType building;
	private boolean setMissile, firstMissile;
	private long startTime, endTime;
	private final ArtemisBase station;
	private final String name;
	private boolean docking, ready, paused;
	private int readySignals, speed;
	
	private static final int ONE_MINUTE = 60000;
	
	public static final String INDUSTRIAL = "Industrial";
	public static final String SCIENCE    = "Science";
	public static final String DEEP_SPACE = "Deep";
	public static final String COMMAND    = "Command";
	public static final String CIVILIAN   = "Civilian";
	
	private static final HashMap<String, Integer> buildFactors = new HashMap<String, Integer>(); 

	public StationStatusRow(Context base, ArtemisBase ab, final ArtemisNetworkInterface server,
			final com.walkertribe.ian.Context context) {
		super(base);
		
		// Set up final fields
		station = ab;
		shields = (int) ab.getShieldsFront();
		maxShields = (int) ab.getShieldsRear();
		ordnance = new int[OrdnanceType.COUNT];
		
		// Set up build factor map
		if (buildFactors.isEmpty()) {
			buildFactors.put(INDUSTRIAL, 6);
			buildFactors.put(SCIENCE, 1);
			buildFactors.put(DEEP_SPACE, 2);
			buildFactors.put(COMMAND, 4);
			buildFactors.put(CIVILIAN, 1);
		}
		
		// Set up layout
		LayoutParams cellLayout = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
		name = ab.getName() + " Terran " + ab.getVessel(context).getName();
		
		TextView nameText = new TextView(base);
		nameText.setTypeface(ListActivity.APP_FONT);
		nameText.setText(name);
		nameText.setLayoutParams(cellLayout);
		nameText.setPadding(3, 3, 3, 3);
		addView(nameText);
		
		TextView ordnanceText = new TextView(base);
		ordnanceText.setTypeface(ListActivity.APP_FONT);
		ordnanceText.setLayoutParams(cellLayout);
		ordnanceText.setPadding(3, 3, 3, 3);
		addView(ordnanceText);
		
		// Set up initial fields
		firstMissile = false;
		speed = 1;
		
		updateStatus();

		// Add touch function to request docking procedure
		setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				server.send(new CommsOutgoingPacket(station, BaseMessage.STAND_BY_FOR_DOCKING, context));
			}
		});
	}
	
	public void setShields(int sh) {
		// Set station's shields - minimum 0
		shields = sh;
		if (sh < 0) shields = 0;
		
		// If station has been hit, turn row yellow, otherwise green
		for (int i = 0; i < getChildCount(); i++)
			if (shields < maxShields) getChildAt(i).setBackgroundColor(Color.parseColor("#bf9000"));
			else getChildAt(i).setBackgroundColor(Color.parseColor("#008000"));
		
		// Update text
		updateStatus();
	}
	
	// Add mission
	public void addMission() {
		missions++;
		updateStatus();
	}
	
	// Remove mission
	public void removeMission() {
		if (--missions < 0) missions = 0;
		updateStatus();
	}
	
	// Number of missions station is involved with
	public int getMissions() { return missions; }
	
	// Increase production speed
	public void incProductionSpeed() {
		speed++;
		updateOrdnance();
	}
	
	// Ordnance type station is building
	public void setOrdnanceType(OrdnanceType type) {
		if (setMissile) return;
		startTime = new Date().getTime();
		building = type;
		if (firstMissile) {
			int buildTime = type.getBuildTime() << 1;
			TextView statusText = (TextView) getChildAt(0);
			buildTime /= buildFactors.get(statusText.getText().toString().split(" ")[2]);
			buildTime /= speed;
			endTime = startTime + buildTime;
		}
		firstMissile = true;
		setMissile = true;
		updateOrdnance();
	}
	
	// Override previous missile type we were building
	public void resetMissile() {
		setMissile = false;
	}
	
	// Recalibrate speed if it's incorrect
	public void recalibrateSpeed() {
		if (paused) {
			paused = false;
			return;
		}
		long recalibrateTime = new Date().getTime() - startTime;
		long buildTime = endTime - startTime;
		speed = (int)Math.round((double)speed * buildTime / recalibrateTime); 
	}
	
	// Set build time for ordnance
	public void setBuildTime(int minutes) {
		if (firstMissile) return;
		endTime = new Date().getTime() + minutes * ONE_MINUTE;
	}
	
	// Number of fighters on the station
	public void setFighters(int f) {
		fighters = f;
		updateStatus();
	}
	
	// Number of each ordnance type in stock
	public void setStock(OrdnanceType type, int stock) {
		ordnance[type.ordinal()] = stock;
		updateOrdnance();
	}
	
	// Is ship docked here?
	public void setDocking(boolean dock) {
		docking = dock;
		if (dock) {
			if (ready) readySignals++;
			ready = false;
		}
		updateStatus();
	}
	
	// Is station ready for a ship?
	public void setReady(boolean crewReady) {
		if (!crewReady && readySignals > 0) {
			readySignals--;
			return;
		}
		if (docking) {
			if (crewReady) readySignals++;
			return;
		}
		ready = crewReady;
		updateStatus();
	}
	
	// Is the game paused?
	public void setPaused(boolean p) {
		if (p) {
			paused = true;
		} else if (endTime >= new Date().getTime()) {
			paused = false;
		}
	}
	
	// Update status text
	public void updateStatus() {
		TextView statusText = (TextView) getChildAt(0);
		String status = name + "\nShields " + shields + "/" + maxShields;
		if (missions == 1) status += ", 1 mission";
		else if (missions > 1) status += ", " + missions + " missions";
		if (fighters == 1) status += ", 1 fighter";
		else if (fighters > 1) status += ", " + fighters + " fighters";
		if (docking) status += " (docked)";
		else if (ready) status += " (standby)";
		statusText.setText(status);
	}
	
	// Update ordnance text
	public void updateOrdnance() {
		String stock = "";
		for (OrdnanceType t: OrdnanceType.values()) {
			if (!stock.equals("")) stock += "/";
			stock += ordnance[t.ordinal()] + " " + t; 
		}
		if (speed > 1) {
			stock += " (speed x" + speed + ")";
		}
		updateTime(stock);
	}
	
	// Update missile-building text
	public void updateTime(String stock) {
		TextView ordnanceText = (TextView) getChildAt(1);
		if (stock == null) {
			stock = ordnanceText.getText().toString().split("\n")[0];
		}
		if (building != null) {
			int eta = (int)(endTime - new Date().getTime());
			int millis = eta % 1000;
			int seconds = (eta / 1000) % 60;
			int minutes = eta / 60000;
			if (eta < 0) {
				millis = 0;
				seconds = 0;
				minutes = 0;
			}
			if (millis > 0) {
				seconds++;
				if (seconds == 60) {
					seconds = 0;
					minutes++;
				}
			}
			stock += String.format("\nType %d %s ready in %d:%02d", building.getType(), building.toString(), minutes, seconds);
		}
		ordnanceText.setText(stock);
	}
}