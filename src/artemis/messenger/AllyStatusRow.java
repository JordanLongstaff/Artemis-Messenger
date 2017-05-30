package artemis.messenger;

import android.content.Context;
import android.graphics.Color;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * A row in the Allies table, that shows status information for an ally ship
 * @author Jordan Longstaff
 *
 */
public class AllyStatusRow extends TableRow {
	// Ally ship information
	private int front, rear, missions;
	private final int fMax, rMax;
	private boolean energy, blind, torps, building;
	private AllyStatus status;
	
	// Colours to paint row in based on ally ship's status
	private static final int[] statusColors = {
		Color.parseColor("#008000"), // Green
		Color.parseColor("#bf9000"), // Yellow
		Color.parseColor("#bf9000"), // Yellow
		Color.parseColor("#c55a11"), // Orange
		Color.RED                    // Red - duh
	};

	public AllyStatusRow(Context context, String n, int f, int r, int fmax, int rmax) {
		// Initialize ally ship information
		super(context);
		front = f;
		rear = r;
		fMax = fmax;
		rMax = rmax;
		energy = false;
		
		// Start by assuming status is normal
		status = AllyStatus.NORMAL;

		// Set up row layout
		LayoutParams cellLayout = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
		
		TextView nameText = new TextView(context);
		nameText.setTypeface(ListActivity.APP_FONT);
		nameText.setText(n);
		nameText.setLayoutParams(cellLayout);
		nameText.setPadding(3, 3, 3, 3);
		addView(nameText);
		
		TextView shieldsText = new TextView(context);
		shieldsText.setTypeface(ListActivity.APP_FONT);
		shieldsText.setLayoutParams(cellLayout);
		shieldsText.setPadding(3, 3, 3, 3);
		addView(shieldsText);
		
		TextView statusText = new TextView(context);
		statusText.setTypeface(ListActivity.APP_FONT);
		statusText.setLayoutParams(cellLayout);
		statusText.setPadding(3, 3, 3, 3);
		addView(statusText);
		
		// Update text
		updateShields();
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
	
	// Number of missions ship is involved with
	public int getMissions() { return missions; }
	
	// Set front shields - minimum 0
	public void setFront(int f) {
		if (f < 0) front = 0;
		else front = f;
		updateShields();
	}
	
	// Set rear shields - minimum 0
	public void setRear(int r) {
		if (r < 0) rear = 0;
		else rear = r;
		updateShields();
	}
	
	// Does ship have energy to spare?
	public void setEnergy(boolean e) {
		energy = e;
		updateStatus();
	}
	
	// Does ship have torpedoes in Deep Strike mode?
	public void setTorpedoes(boolean t) {
		torps = t;
		building |= t;
		updateStatus();
	}
	
	// Is ship building torpedoes in Deep Strike mode?
	public boolean isBuildingTorpedoes() { return building; }
	
	// Set ship status
	public void setStatus(AllyStatus as) {
		status = as;
		if (as == AllyStatus.FLYING_BLIND || as == AllyStatus.REWARD) blind = true;
		updateStatus();
	}
	public AllyStatus getStatus() { return status; }
	
	// Is ship flying blind?
	public boolean isFlyingBlind() { return blind; }
	public void setBlind(boolean b) { blind = b; }
	
	// Update status text
	public void updateStatus() {
		for (int i = 0; i < getChildCount(); i++)
			getChildAt(i).setBackgroundColor(statusColors[status.ordinal() >> 1]);
		TextView statusText = (TextView) getChildAt(2);
		String line2 = status.m2;
		if (status.ordinal() < AllyStatus.FIGHTERS.ordinal()) {
			if (energy) {
				line2 = "Energy";
				if (torps) line2 += ", torpedoes";
				if (missions == 1) line2 += ", mission";
				else if (missions > 0) line2 += ", " + missions + " missions";
			} else if (torps) {
				line2 = "Torpedoes";
				if (missions == 1) line2 += ", mission";
				else if (missions > 0) line2 += ", " + missions + " missions";
			} else if (missions == 1) line2 = "Mission";
			else if (missions > 0) line2 = missions + " missions";
		}
		statusText.setText(status.m1 + "\n" + line2);
	}
	
	// Update shields text
	public void updateShields() {
		TextView shieldsText = (TextView) getChildAt(1);
		shieldsText.setText(String.format("F %d/%d%nR %d/%d", front, fMax, rear, rMax));
	}
}