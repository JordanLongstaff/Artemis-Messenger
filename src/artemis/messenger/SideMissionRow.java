package artemis.messenger;

import java.util.HashMap;

import android.content.Context;
import android.graphics.Color;
import android.widget.TableRow;
import android.widget.TextView;

public class SideMissionRow extends TableRow {
	// Side mission information
	private final String source, destination;
	private boolean started = false, completed = false;
	private final HashMap<String, Integer> rewards;
	
	// Reward keys
	public static final String BATTERY_KEY = "batteries.";
	public static final String PRODUCTION_KEY = "speed.";
	public static final String NUCLEAR_KEY = "torpedoes.";
	public static final String COOLANT_KEY = "coolant.";
	public static final String SHIELD_KEY = "generators.";
	
	// Reward map
	private static final HashMap<String, String> rewardMap = new HashMap<String, String>();
	
	public SideMissionRow(Context context, String src, String dest, String payout) {
		// Fill side mission information
		super(context);
		source = src;
		destination = dest;
		rewards = new HashMap<String, Integer>();
		rewards.put(payout, 1);
		
		// Set up reward map
		if (rewardMap.isEmpty()) {
			rewardMap.put(PRODUCTION_KEY, context.getString(R.string.prodSpeedPref));
			rewardMap.put(NUCLEAR_KEY, context.getString(R.string.nukePref));
			rewardMap.put(COOLANT_KEY, context.getString(R.string.extraCoolantPref));
			rewardMap.put(BATTERY_KEY, context.getString(R.string.batteryChargePref));
			rewardMap.put(SHIELD_KEY, context.getString(R.string.shieldBoostPref));
		}
		
		// Build layout
		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		
		LayoutParams cellLayout = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
		
		// Add table row cells
		TextView sourceText = new TextView(context);
		sourceText.setTypeface(ListActivity.APP_FONT);
		sourceText.setText(source);
		sourceText.setLayoutParams(cellLayout);
		sourceText.setPadding(3, 3, 3, 3);
		addView(sourceText);
		
		TextView destText = new TextView(context);
		destText.setTypeface(ListActivity.APP_FONT);
		destText.setText(destination);
		destText.setLayoutParams(cellLayout);
		destText.setPadding(3, 3, 3, 3);
		addView(destText);
		
		TextView rewardText = new TextView(context);
		rewardText.setTypeface(ListActivity.APP_FONT);
		rewardText.setText(rewardMap.get(payout));
		rewardText.setLayoutParams(cellLayout);
		rewardText.setPadding(3, 3, 3, 3);
		addView(rewardText);
		
		// Set background colours
		setBackgroundColor(Color.parseColor("#002060"));
		sourceText.setBackgroundColor(Color.parseColor("#bf9000"));
	}
	
	// Getter/manipulator methods
	public String getSource() { return source; }
	public String getDestination() { return destination; }
	public int getQuantity(String reward) {
		if (rewards.containsKey(reward)) return rewards.get(reward);
		else return 0;
	}
	public boolean hasReward(String reward) {
		return getQuantity(reward) > 0;
	}
	public int getNumRewards() {
		int sum = 0;
		for (String r: rewards.keySet()) sum += rewards.get(r);
		return sum;
	}
	public boolean isStarted() { return started; }
	public boolean isCompleted() { return completed; }
	public void addReward(String reward) {
		if (rewardMap.containsValue(reward)) {
			for (String key: rewardMap.keySet()) {
				if (rewardMap.get(key).equals(reward)) {
					reward = key;
					break;
				}
			}
		}
		if (rewards.containsKey(reward)) rewards.put(reward, rewards.get(reward) + 1);
		else rewards.put(reward, 1);
		TextView rewardText = (TextView) getChildAt(2);
		rewardText.setText(getRewardList());
	}
	
	// String formatter
	@Override
	public String toString() {
		return String.format("[%s, %s, %s]", source, destination, getRewardList());
	}
	
	// Get list of all rewards
	public String getRewardList() {
		String text = "";
		for (String reward: rewards.keySet()) {
			if (text.length() > 0) text += ", ";
			text += rewardMap.get(reward);
			if (rewards.get(reward) > 1) text += " x" + rewards.get(reward);
		}
		return text;
	}
	
	// Need to change source text if source was previously ambiguous
	public void updateSource(String src) {
		TextView sourceText = (TextView) getChildAt(0);
		sourceText.setText(src);
	}
	
	// Mark mission as started
	public void markAsStarted() {
		started = true;
	}
	
	// Mark mission as completed
	public void markAsCompleted() {
		started = true;
		completed = true;
	}
	
	// Update row design based on progress
	public void updateProgress() {
		if (completed) {
			int green = Color.parseColor("#008000");
			for (int i = 0; i < getChildCount(); i++) getChildAt(i).setBackgroundColor(green);
			TextView rewardText = (TextView) getChildAt(2);
			rewardText.setText(getRewardList() + " (Acquired)");
		} else if (started) {
			TextView sourceText = (TextView) getChildAt(0);
			sourceText.setBackgroundColor(Color.parseColor("#002060"));
			TextView destText = (TextView) getChildAt(1);
			destText.setBackgroundColor(Color.parseColor("#bf9000"));
		}
	}
}