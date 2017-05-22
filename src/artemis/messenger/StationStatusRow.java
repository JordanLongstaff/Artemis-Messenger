package artemis.messenger;

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
	private final ArtemisBase station;
	private final String name;
	private boolean docking;

	public StationStatusRow(Context base, ArtemisBase ab, final ArtemisNetworkInterface server,
			final com.walkertribe.ian.Context context) {
		super(base);
		
		// Set up final fields
		station = ab;
		shields = (int) ab.getShieldsFront();
		maxShields = (int) ab.getShieldsRear();
		ordnance = new int[OrdnanceType.COUNT];
		
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
	
	// Ordnance type station is building
	public void setOrdnanceType(OrdnanceType type) {
		building = type;
		updateOrdnance();
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
		updateStatus();
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
		statusText.setText(status);
	}
	
	// Update ordnance text
	public void updateOrdnance() {
		String stock = "";
		for (OrdnanceType t: OrdnanceType.values()) {
			if (!stock.equals("")) stock += "/";
			stock += ordnance[t.ordinal()] + " " + t; 
		}
		if (building != null) stock += "\nBuilding Type " + building.getType() + " " + building;
		TextView ordnanceText = (TextView) getChildAt(1);
		ordnanceText.setText(stock);
	}
}