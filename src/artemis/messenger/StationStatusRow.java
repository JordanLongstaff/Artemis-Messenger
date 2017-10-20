package artemis.messenger;

import java.util.Date;
import java.util.HashMap;

import com.walkertribe.ian.enums.OrdnanceType;
import com.walkertribe.ian.world.ArtemisBase;

import android.content.Context;
import android.graphics.Color;
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
	private final String name;
	private boolean docking, docked, ready, paused, closest;
	private int speed, rowColor;
	
	private static final int ONE_MINUTE = 60000;
	
	public static final String INDUSTRIAL = "Industrial";
	public static final String SCIENCE    = "Science";
	public static final String DEEP_SPACE = "Deep";
	public static final String COMMAND    = "Command";
	public static final String CIVILIAN   = "Civilian";
	
	private static final HashMap<String, Integer> buildFactors = new HashMap<String, Integer>(); 

	public StationStatusRow(Context base, ArtemisBase ab, com.walkertribe.ian.Context context) {
		super(base);
		
		// Set up final fields
		shields = (int) ab.getShieldsFront();
		maxShields = (int) ab.getShieldsRear();
		ordnance = new int[OrdnanceType.COUNT];
		
		// Set color
		if (shields < maxShields) rowColor = Color.parseColor("#bf9000");
		else rowColor = Color.parseColor("#008000");
		
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
		
		updateStatus(getStatusText());
	}
	
	public void setShields(int sh) {
		// Set station's shields - minimum 0
		shields = sh;
		if (sh < 0) shields = 0;
		
		// If station has been hit, turn row yellow, otherwise green
		if (shields < maxShields) rowColor = Color.parseColor("#bf9000");
		else rowColor = Color.parseColor("#008000");
	}
	
	// Add mission
	public void addMission() {
		missions++;
	}
	
	// Remove mission
	public void removeMission() {
		if (--missions < 0) missions = 0;
	}
	
	// Number of missions station is involved with
	public int getMissions() { return missions; }
	
	// Increase production speed
	public void incProductionSpeed() {
		speed++;
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
	}
	
	// Number of each ordnance type in stock
	public void setStock(OrdnanceType type, int stock) {
		ordnance[type.ordinal()] = stock;
	}
	
	// Is ship docked here?
	public void setDocking(boolean dock) {
		docking = dock;
		docked &= dock;
	}
	
	public boolean completeDock() {
		if (!docking) return false;
		docked = true;
		return true;
	}
	
	// Is station ready for a ship?
	public void setReady(boolean crewReady) {
		ready = crewReady;
	}
	
	// Is station the closest to you?
	public void setClosest(boolean close) {
		closest = close;
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
	public String getStatusText() {
		String status = name + "\nShields " + shields + "/" + maxShields;
		if (missions == 1) status += ", 1 mission";
		else if (missions > 1) status += ", " + missions + " missions";
		if (fighters == 1) status += ", 1 fighter";
		else if (fighters > 1) status += ", " + fighters + " fighters";
		if (docked) status += " (docked)";
		else if (docking) status += " (docking)";
		else if (ready) status += " (standby)";
		else if (closest) status += " (closest)";
		return status;
	}
	
	public void updateStatus(String status) {
		final TextView statusText = (TextView) getChildAt(0);
		statusText.setText(status);
	}
	
	public void updateColor() {
		for (int i = 0; i < getChildCount(); i++) {
			getChildAt(i).setBackgroundColor(rowColor);
		}
	}
	
	// Update ordnance text
	public String getOrdnanceText() {
		String stock = "";
		for (OrdnanceType t: OrdnanceType.values()) {
			if (!stock.equals("")) stock += "/";
			stock += ordnance[t.ordinal()] + " " + t; 
		}
		if (speed > 1) {
			stock += " (speed x" + speed + ")";
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
		return stock;
	}
	
	public void updateOrdnance(String stock) {
		final TextView ordnanceText = (TextView) getChildAt(1);
		ordnanceText.setText(stock);
	}
}