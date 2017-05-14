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

public class StationStatusRow extends TableRow {
	private int shields, missions, fighters;
	private final int maxShields;
	private final int[] ordnance;
	private OrdnanceType building;
	private final ArtemisBase station;
	private final String name;
	private boolean docking;

	public StationStatusRow(Context base, ArtemisBase ab, final ArtemisNetworkInterface server, final com.walkertribe.ian.Context context) {
		super(base);
		station = ab;
		shields = (int) ab.getShieldsFront();
		maxShields = (int) ab.getShieldsRear();
		ordnance = new int[OrdnanceType.COUNT];
		
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

		setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				server.send(new CommsOutgoingPacket(station, BaseMessage.STAND_BY_FOR_DOCKING, context));
			}
		});
	}
	
	public void setShields(int sh) {
		shields = sh;
		if (sh < 0) shields = 0;
		for (int i = 0; i < getChildCount(); i++)
			if (shields < maxShields) getChildAt(i).setBackgroundColor(Color.parseColor("#bf9000"));
			else getChildAt(i).setBackgroundColor(Color.parseColor("#008000"));
		updateStatus();
	}
	
	public void addMission() {
		missions++;
		updateStatus();
	}
	public void removeMission() {
		if (--missions < 0) missions = 0;
		updateStatus();
	}
	public int getMissions() { return missions; }
	
	public void setOrdnanceType(OrdnanceType type) {
		building = type;
		updateOrdnance();
	}
	
	public void setFighters(int f) {
		fighters = f;
		updateStatus();
	}
	
	public void setStock(OrdnanceType type, int stock) {
		ordnance[type.ordinal()] = stock;
		updateOrdnance();
	}
	
	public void setDocking(boolean dock) {
		docking = dock;
		updateStatus();
	}
	
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