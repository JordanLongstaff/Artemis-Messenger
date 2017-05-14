package artemis.messenger;

import android.content.Context;
import android.graphics.Color;
import android.widget.TableRow;
import android.widget.TextView;

public class AllyStatusRow extends TableRow {
	private int front, rear, missions;
	private final int fMax, rMax;
	private boolean energy, blind, torps, building;
	private AllyStatus status;
	
	private static final int[] statusColors = {
		Color.parseColor("#008000"),
		Color.parseColor("#bf9000"),
		Color.parseColor("#bf9000"),
		Color.parseColor("#c55a11"),
		Color.RED
	};

	public AllyStatusRow(Context context, String n, int f, int r, int fmax, int rmax) {
		super(context);
		front = f;
		rear = r;
		fMax = fmax;
		rMax = rmax;
		energy = false;
		status = AllyStatus.NORMAL;

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
		
		updateShields();
		updateStatus();
	}
	
	public void addMission() {
		missions++;
		updateStatus();
	}
	public void removeMission() {
		missions--;
		if (missions < 0) missions = 0;
		updateStatus();
	}
	public int getMissions() { return missions; }
	
	public void setFront(int f) {
		if (f < 0) front = 0;
		else front = f;
		updateShields();
	}
	public void setRear(int r) {
		if (r < 0) rear = 0;
		else rear = r;
		updateShields();
	}
	
	public void setEnergy(boolean e) {
		energy = e;
		updateStatus();
	}
	
	public void setTorpedoes(boolean t) {
		torps = t;
		updateStatus();
	}
	
	public void setBuildingTorpedoes(boolean t) { building = t; }
	public boolean isBuildingTorpedoes() { return building; }
	
	public void setStatus(AllyStatus as) {
		status = as;
		if (as == AllyStatus.FLYING_BLIND || as == AllyStatus.REWARD) blind = true;
		updateStatus();
	}
	public AllyStatus getStatus() { return status; }
	
	public boolean isFlyingBlind() { return blind; }
	public void setBlind(boolean b) { blind = b; }
	
	public void updateStatus() {
		for (int i = 0; i < getChildCount(); i++)
			getChildAt(i).setBackgroundColor(statusColors[status.ordinal() / 2]);
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
	
	public void updateShields() {
		TextView shieldsText = (TextView) getChildAt(1);
		shieldsText.setText(String.format("F %d/%d%nR %d/%d", front, fMax, rear, rMax));
	}
}