package artemis.messenger;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;

/**
 * This activity shows the help menu.
 * 
 * @author Jordan Longstaff
 */
public class HelpActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
		
		// Grey title bar
		LinearLayout titleBar = (LinearLayout)findViewById(R.id.helpTitleBar);
		titleBar.setBackgroundColor(Color.parseColor("#707070"));
		
		// Set up help menu navigations
		ListView list = (ListView)findViewById(R.id.helpList);
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
				if (pos == 7) {
					// Back button
					HelpActivity.this.finish();
					return;
				}
				
				Intent helpPageIntent = new Intent(HelpActivity.this, HelpPageActivity.class);
				helpPageIntent.putExtra("Page", pos);
				startActivity(helpPageIntent);
			}
		});
	}
}
