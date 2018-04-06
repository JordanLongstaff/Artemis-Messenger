package artemis.messenger;

import com.walkertribe.ian.world.ArtemisNpc;

import android.content.Context;
import android.graphics.Color;
import android.widget.TextView;

/**
 * A row in the Allies table, that shows status information for an ally ship
 * @author Jordan Longstaff
 *
 */
public class AllyStatusRow extends ObjectStatusRow {
	// Ally ship information
	private final ArtemisNpc ally;
	private int front, rear;
	private final int fMax, rMax;
	private boolean energy, blind, torps, building, pirate;
	private AllyStatus status;
	
	// Colours to paint row in based on ally ship's status
	private static final int[] statusColors = {
		Color.parseColor("#008000"), // Green
		Color.parseColor("#bf9000"), // Yellow
		Color.parseColor("#bf9000"), // Yellow
		Color.parseColor("#c55a11"), // Orange
		Color.RED,                   // Red - duh
		Color.BLACK                  // Black - destroyed ships
	};
	
	private static final int STATUS_TEXT_COLUMN = 2;

	public AllyStatusRow(Context context, ArtemisNpc npc, String n) {
		// Initialize ally ship information
		super(context, STATUS_TEXT_COLUMN);
		ally = npc;
		front = (int) npc.getShieldsFront();
		rear = (int) npc.getShieldsRear();
		fMax = (int) npc.getShieldsFrontMax();
		rMax = (int) npc.getShieldsRearMax();
		energy = false;
		
		// Start by assuming status is normal
		status = AllyStatus.NORMAL;

		// Set up row layout
		LayoutParams nameLayout = new LayoutParams(0, LayoutParams.MATCH_PARENT, 5);
		LayoutParams shieldLayout = new LayoutParams(0, LayoutParams.MATCH_PARENT, 3);
		LayoutParams statusLayout = new LayoutParams(0, LayoutParams.MATCH_PARENT, 7);
		
		TextView nameText = new TextView(context);
		nameText.setTypeface(ListActivity.APP_FONT);
		nameText.setText(n);
		nameText.setLayoutParams(nameLayout);
		nameText.setPadding(3, 3, 3, 3);
		addView(nameText);
		
		TextView shieldsText = new TextView(context);
		shieldsText.setTypeface(ListActivity.APP_FONT);
		shieldsText.setLayoutParams(shieldLayout);
		shieldsText.setPadding(3, 3, 3, 3);
		addView(shieldsText);
		
		TextView statusText = new TextView(context);
		statusText.setTypeface(ListActivity.APP_FONT);
		statusText.setLayoutParams(statusLayout);
		statusText.setPadding(3, 3, 3, 3);
		addView(statusText);
		
		// Update text
		updateShields();
		updateStatusText();
		updateStatusUI();
		updateColor();
	}
	
	// Get ally ship reference
	public ArtemisNpc getAllyShip() {
		return ally;
	}
	
	// Set front shields - minimum 0
	public void setFront(int f) {
		if (f < 0) front = 0;
		else front = f;
	}
	
	// Set rear shields - minimum 0
	public void setRear(int r) {
		if (r < 0) rear = 0;
		else rear = r;
	}
	
	// Does ship have energy to spare?
	public void setEnergy(boolean e) {
		energy = e;
	}
	public boolean hasEnergy() { return energy; }
	
	// Does ship have torpedoes in Deep Strike mode?
	public void setTorpedoes(boolean t) {
		torps = t;
		building |= t;
	}
	
	public boolean hasTorpedoes() { return torps; }
	
	// Is ship building torpedoes in Deep Strike mode?
	public boolean isBuildingTorpedoes() { return building; }
	
	// Set ship status
	public void setStatus(AllyStatus as) {
		if (status == AllyStatus.DESTROYED) return;
		status = as;
		if (as == AllyStatus.FLYING_BLIND || as == AllyStatus.REWARD) blind = true;
	}
	public AllyStatus getStatus() { return status; }
	
	// Is ship flying blind?
	public boolean isFlyingBlind() { return blind; }
	public void setBlind(boolean b) { blind = b; }
	
	// Is ship aware that you are a Pirate?
	public boolean isPirateAware() { return pirate; }
	public void setPirateAware(boolean p) { pirate = p; }
	
	// Update status text
	public String getStatusText() {
		if (status == AllyStatus.DESTROYED) return status.m1;
		String line2 = status.m2;
		if (status.ordinal() < AllyStatus.FIGHTERS.ordinal()) {
			if (pirate) {
				if (status == AllyStatus.PIRATE_DATA) {
					return status.m1 + "\n" + status.m3;
				} else if (status == AllyStatus.PIRATE_SUPPLIES) {
					line2 = status.m3;
				}
			}
			int missions = getMissions();
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
		return status.m1 + "\n" + line2;
	}
	
	@Override
	public int getColor() {
		int index = status.ordinal();
		if (index >= AllyStatus.AMBASSADOR.ordinal()) index -= 3;
		return statusColors[index >> 1];
	}
	
	// Update shields text
	public void updateShields() {
		final TextView shieldsText = (TextView) getChildAt(1);
		shieldsText.setText(String.format("F %d/%d%nR %d/%d", front, fMax, rear, rMax));
	}
}