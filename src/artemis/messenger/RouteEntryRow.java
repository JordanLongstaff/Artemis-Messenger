package artemis.messenger;

import android.content.Context;
import android.graphics.Color;
import android.widget.TableRow;
import android.widget.TextView;

public class RouteEntryRow extends TableRow {

	public RouteEntryRow(Context context, String object, float distance) {
		super(context);
		
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
		distanceText.setText(String.format("%.1f km", distance));
		distanceText.setLayoutParams(cellLayout);
		distanceText.setPadding(3, 3, 3, 3);
		addView(distanceText);
		
		setBackgroundColor(Color.parseColor("#002060"));
	}
}
