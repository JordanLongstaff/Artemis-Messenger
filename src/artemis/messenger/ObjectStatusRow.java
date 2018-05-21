package artemis.messenger;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * Represents either a {@code AllyStatusRow} or a {@code StationStatusRow}.
 * @author jrdnl
 */
public abstract class ObjectStatusRow extends TableRow {
	private final int statusColumn;
	private final HashMap<String, ArrayList<SideMissionRow>> missions;
	private String statusString;
	private int numMissions;
	private boolean missionUpdate;
	
	private static String currentShip = "";
	
	public ObjectStatusRow(Context context, int col) {
		super(context);
		statusColumn = col;
		missions = new HashMap<String, ArrayList<SideMissionRow>>();
		missions.put("", new ArrayList<SideMissionRow>());
	}
	
	public abstract int getColor();
	public abstract String getStatusText();
	
	public final void updateColor() {
		int color = getColor();
		for (int i = 0; i < getChildCount(); i++) {
			getChildAt(i).setBackgroundColor(color);
		}
	}
	
	public final void updateStatusText() {
		statusString = getStatusText();
	}
	
	public final void updateStatusUI() {
		TextView statusText = (TextView) getChildAt(statusColumn);
		statusText.setText(statusString);
	}
	
	// Add mission
	public final void addMission(SideMissionRow row) {
		String ship = row.getPlayerShip();
		if (!missions.containsKey(ship)) missions.put(ship, new ArrayList<SideMissionRow>());
		missions.get(ship).add(row);
		missionUpdate = true;
	}
	
	// Remove mission, if it is in the list
	public final boolean removeMission(SideMissionRow row) {
		String ship = row.getPlayerShip();
		if (!missions.get(ship).contains(row)) return false;
		missions.get(ship).remove(row);
		missionUpdate = true;
		return true;
	}
	
	// Number of missions ship is involved with
	public final int getMissions() {
		if (missionUpdate) {
			numMissions = 0;
			for (SideMissionRow row: missions.get("")) numMissions += row.getNumRewards();
			if (!currentShip.isEmpty() && missions.containsKey(currentShip)) {
				for (SideMissionRow row: missions.get(currentShip))
					numMissions += row.getNumRewards();
			}
		}
		return numMissions;
	}
	
	// Change current ship, return old ship name
	public static String setCurrentShip(String ship) {
		String oldShip = currentShip;
		currentShip = ship;
		return oldShip;
	}
}