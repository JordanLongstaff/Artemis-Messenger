package artemis.messenger;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class HelpPageActivity extends Activity {
	private static final int[] titleBarIDs = new int[] {
			R.id.basicsTitle,
			R.id.missionsTitle,
			R.id.alliesTitle,
			R.id.stationsTitle,
			R.id.routingTitle,
			R.id.settingsTitle,
			R.id.aboutTitle
	};

	private static final int[] layoutIDs = new int[] {
			R.layout.activity_basics_page,
			R.layout.activity_missions_page,
			R.layout.activity_allies_page,
			R.layout.activity_stations_page,
			R.layout.activity_routing_page,
			R.layout.activity_settings_page,
			R.layout.activity_about_page
	};

	private static final int[] backButtonIDs = new int[] {
			R.id.basicsBack,
			R.id.missionsBack,
			R.id.alliesBack,
			R.id.stationsBack,
			R.id.routingBack,
			R.id.settingsBack,
			R.id.aboutBack
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_basics_page);

		// Get help page number so a single class can handle one of seven different layouts
		Intent intent = getIntent();
		int helpPageID = intent.getIntExtra("Page", 0);
		setContentView(layoutIDs[helpPageID]);
		
		// Title bar
		TextView titleBar = (TextView)findViewById(titleBarIDs[helpPageID]);
		titleBar.setBackgroundColor(Color.parseColor("#707070"));
		titleBar.setText(getResources().getStringArray(R.array.helpTopicsList)[helpPageID]);
		
		// Back button
		Button backButton = (Button)findViewById(backButtonIDs[helpPageID]);
		backButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
}
