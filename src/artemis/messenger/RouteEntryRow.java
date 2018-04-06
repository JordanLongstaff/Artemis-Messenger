package artemis.messenger;

import java.util.EnumMap;
import java.util.Locale;

import com.walkertribe.ian.world.ArtemisBase;
import com.walkertribe.ian.world.ArtemisNpc;
import com.walkertribe.ian.world.ArtemisObject;

import android.content.Context;
import android.graphics.Color;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * A row in the Routing table, that indicates a stop on the route to complete all side missions.
 * @author Jordan Longstaff
 *
 */
public class RouteEntryRow extends TableRow {
	private final ArtemisObject point;
	private final EnumMap<RouteEntryReason, Integer> reasons;
	
	private final TextView pointText, distanceText;
	
	private static final int clr_blue = Color.parseColor("#002060");
	
	public RouteEntryRow(Context context, ArtemisObject pt, String object) {
		super(context);
		point = pt;
		reasons = new EnumMap<RouteEntryReason, Integer>(RouteEntryReason.class);
		
		// Prepare row layout
		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

		LayoutParams leftLayout = new LayoutParams(0, LayoutParams.MATCH_PARENT, 3);
		LayoutParams rightLayout = new LayoutParams(0, LayoutParams.MATCH_PARENT, 2);
		
		pointText = new TextView(context);
		pointText.setTypeface(ListActivity.APP_FONT);
		pointText.setText(object);
		pointText.setLayoutParams(leftLayout);
		pointText.setPadding(3, 3, 3, 3);
		addView(pointText);
		
		distanceText = new TextView(context);
		distanceText.setTypeface(ListActivity.APP_FONT);
		distanceText.setLayoutParams(rightLayout);
		distanceText.setPadding(3, 3, 3, 3);
		addView(distanceText);
		
		// Paint row in blue
		setBackgroundColor(clr_blue);
	}
	
	public String getObjectName() {
		TextView pointText = (TextView) getChildAt(0);
		return pointText.getText().toString().split("\n")[0];
	}
	
	public void updateDistance(ArtemisObject player, boolean forceThreeDigits) {
		float distX = point.getX() - player.getX();
		float distZ = point.getZ() - player.getZ();
		
		double angle = Math.atan2(distZ, distX);
		int direction = (270 - (int)Math.toDegrees(angle)) % 360;
		float distance = point.distance(player);

		distanceText.setText(String.format("DIR %" +
		(forceThreeDigits ? "03" : "") +
		"d\nRANGE %.1f", direction, distance));
	}
	
	public void setReasons(RouteEntryReason... rers) {
		// Clear previous reasons
		for (RouteEntryReason reason: RouteEntryReason.values()) {
			reasons.put(reason, 0);
		}
		
		// Populate route entry reasons
		for (RouteEntryReason reason: rers) {
			reasons.put(reason, reasons.get(reason) + 1);
		}
	}
	
	public void updateReasons() {
		// Change row colour based on statuses
		boolean commandeered = false, malfunction = false, other = false;
		
		// Get text for left column
		String line2 = "";
		for (RouteEntryReason reason: reasons.keySet()) {
			if (reasons.get(reason) == 0) continue;
			String entry = reason.name().replaceAll("_", " ").toLowerCase(Locale.getDefault());
			switch (reason) {
			case DAMCON:
				entry = "needs DamCon";
				other = true;
				break;
			case MISSION:
				int numMissions = reasons.get(reason);
				other = true;
				if (numMissions == 1) break;
				entry = numMissions + " missions";
				break;
			case AMBASSADOR:
			case MALFUNCTION:
				malfunction = true;
				break;
			case COMMANDEERED:
				commandeered = true;
				break;
			default:
				other = true;
				break;
			}
			line2 += ", " + entry;
		}
		if (!line2.equals("")) line2 = "\n" + Character.toUpperCase(line2.charAt(2)) + line2.substring(3);
		pointText.setText(getObjectName() + line2);
		
		int color = clr_blue;
		boolean damaged = false;
		if (point instanceof ArtemisNpc) {
			ArtemisNpc npc = (ArtemisNpc) point;
			damaged = npc.getShieldsFront() < npc.getShieldsFrontMax();
			damaged |= npc.getShieldsRear() < npc.getShieldsRearMax();
		} else {
			ArtemisBase base = (ArtemisBase) point;
			damaged = base.getShieldsFront() < base.getShieldsRear();
		}
		
		if (damaged) color = Color.parseColor("#bf9000");
		else if (commandeered) color = Color.RED;
		else if (malfunction) {
			if (other) color = Color.parseColor("#008000");
			else color = Color.parseColor("#c55a11");
		}
		for (int i = 0; i < getChildCount(); i++)
			getChildAt(i).setBackgroundColor(color);
	}
}
