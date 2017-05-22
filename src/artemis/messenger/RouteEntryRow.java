package artemis.messenger;

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
	public RouteEntryRow(Context context, String object, double distance, int direction) {
		super(context);
		
		// Prepare row layout
		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		
		LayoutParams cellLayout = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
		
		TextView pointText = new TextView(context);
		pointText.setTypeface(ListActivity.APP_FONT);
		pointText.setText(object);
		pointText.setLayoutParams(cellLayout);
		pointText.setPadding(3, 3, 3, 3);
		addView(pointText);
		
		TextView distanceText = new TextView(context);
		distanceText.setTypeface(ListActivity.APP_FONT);
		distanceText.setText(String.format("DIR %d\nRANGE %.1f", direction, distance));
		distanceText.setLayoutParams(cellLayout);
		distanceText.setPadding(3, 3, 3, 3);
		addView(distanceText);
		
		// Paint row in blue
		setBackgroundColor(Color.parseColor("#002060"));
	}
}
