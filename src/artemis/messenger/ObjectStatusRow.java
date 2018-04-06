package artemis.messenger;

import android.content.Context;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * Represents either a {@code AllyStatusRow} or a {@code StationStatusRow}.
 * @author jrdnl
 */
public abstract class ObjectStatusRow extends TableRow {
	private final int statusColumn;
	private int missions;
	private String statusString;
	
	public ObjectStatusRow(Context context, int col) {
		super(context);
		statusColumn = col;
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
	public final void addMission() {
		missions++;
	}
	
	// Remove mission
	public final void removeMission() {
		if (--missions < 0) missions = 0;
	}
	
	// Number of missions ship is involved with
	public final int getMissions() { return missions; }
}