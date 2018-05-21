package artemis.messenger;

import java.util.Date;

import com.walkertribe.ian.ArtemisContext;
import com.walkertribe.ian.enums.OrdnanceType;
import com.walkertribe.ian.world.ArtemisBase;

import android.content.Context;
import android.graphics.Color;
import android.widget.TextView;

/**
 * A row in the Stations table, that shows information about a station and can be touched to request preparations for
 * docking.
 * @author Jordan Longstaff
 *
 */
public class StationStatusRow extends ObjectStatusRow {
	// Station info
	private int shields, fighters;
	private final int maxShields;
	private final int[] ordnance;
	private OrdnanceType building;
	private boolean setMissile, firstMissile;
	private long startTime, endTime;
	private final String name;
	private boolean docking, docked, ready, paused, closest;
	private int speed, rowColor;
	private final int productionCoeff;
	
	private static final int ONE_MINUTE = 60000;
	
	private static final int damagedColor = Color.parseColor("#bf9000");
	private static final int healthyColor = Color.parseColor("#008000");
	
	public static final int STATUS_TEXT_COLUMN = 0; 
	public static final int ORDNANCE_TEXT_COLUMN = 1;

	public StationStatusRow(Context base, ArtemisBase ab, ArtemisContext context) {
		super(base, STATUS_TEXT_COLUMN);
		
		// Set up final fields
		maxShields = (int) ab.getShieldsRear();
		ordnance = new int[OrdnanceType.ordnances().size()];
		
		// Set shields/color
		setShields((int) ab.getShieldsFront());
		
		// Set production coefficient
		productionCoeff = (int) (ab.getVessel(context).getProductionCoeff() * 2);
		
		// Set up layout
		LayoutParams cellLayout = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
		name = ab.getName() + " " + ab.getVessel(context).getFullName();
		
		TextView nameText = new TextView(base);
		nameText.setTypeface(ListActivity.APP_FONT);
		nameText.setText(name);
		nameText.setLayoutParams(cellLayout);
		nameText.setPadding(3, 3, 3, 3);
		nameText.setTextColor(Color.LTGRAY);
		addView(nameText);
		
		TextView ordnanceText = new TextView(base);
		ordnanceText.setTypeface(ListActivity.APP_FONT);
		ordnanceText.setLayoutParams(cellLayout);
		ordnanceText.setPadding(3, 3, 3, 3);
		ordnanceText.setTextColor(Color.LTGRAY);
		addView(ordnanceText);
		
		// Set up initial fields
		firstMissile = false;
		speed = 1;
		
		updateStatusText();
		updateStatusUI();
	}
	
	public void setShields(int sh) {
		// Set station's shields - minimum 0
		shields = sh;
		if (sh < 0) shields = 0;
		
		// If station has been hit, turn row yellow, otherwise green
		if (shields < maxShields) rowColor = damagedColor;
		else rowColor = healthyColor;
	}
	
	// Increase production speed
	public void incProductionSpeed() {
		speed++;
	}
	
	// Ordnance type station is building
	public OrdnanceType getOrdnanceType() {
		return building;
	}
	
	public void setOrdnanceType(OrdnanceType type) {
		if (setMissile) return;
		startTime = new Date().getTime();
		building = type;
		if (firstMissile) {
			int buildTime = type.getBuildTime() << 1;
			buildTime /= productionCoeff;
			buildTime /= speed;
			endTime = startTime + buildTime;
		}
		firstMissile = true;
		setMissile = true;
	}
	
	// Set on-click listeners on components of the row
	public void setOnClickListener(int viewIndex, OnClickListener listener) {
		this.getChildAt(viewIndex).setOnClickListener(listener);
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
	
	// Recalibrate speed if an incoming message says it's incorrect
	public void reconcileSpeed(int minutes) {
		int normalBuildTime = building.getBuildTime() << 1;
		normalBuildTime /= productionCoeff;
		int buildTime = normalBuildTime / speed;
		int predictedMinutes = (buildTime - 1) / ONE_MINUTE + 1;
		if (predictedMinutes == minutes) return;
		
		for (speed = 1;; speed++) {
			buildTime = normalBuildTime / speed;
			predictedMinutes = (buildTime - 1) / ONE_MINUTE + 1;
			if (predictedMinutes == minutes) break;
			else if (predictedMinutes < minutes) {
				if (speed > 1) speed--;
				buildTime = minutes * ONE_MINUTE;
				break;
			}
		}
		
		endTime = buildTime + startTime;
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
	
	public void incFighters(int f) {
		fighters += f;
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
	
	public void completeDock() {
		docked = true;
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
	@Override
	public String getStatusText() {
		String status = name + "\nShields " + shields + "/" + maxShields;
		int missions = getMissions();
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

	@Override
	public int getColor() {
		return rowColor;
	}
	
	// Update ordnance text
	public String getOrdnanceText() {
		String stock = "";
		for (OrdnanceType t: OrdnanceType.ordnances()) {
			if (!stock.equals("")) stock += "/";
			stock += ordnance[t.ordinal()] + " " + t.getLabel();
		}
		if (speed > 1) {
			stock += " (speed x" + speed + ")";
		}
		
		if (building != null) {
			int eta = (int)(endTime - new Date().getTime());
			int millis = eta % 1000;
			int seconds = (eta / 1000) % 60;
			int minutes = eta / ONE_MINUTE;
			if (eta < 0) {
				millis = 0;
				seconds = 0;
				minutes = 0;
			} else if (millis > 0) {
				seconds++;
				if (seconds == 60) {
					seconds = 0;
					minutes++;
				}
			}
			stock += String.format("\n%s ready in %d:%02d", building.toString(), minutes, seconds);
		}
		return stock;
	}
	
	public void updateOrdnance(String stock) {
		final TextView ordnanceText = (TextView) getChildAt(1);
		ordnanceText.setText(stock);
	}
}