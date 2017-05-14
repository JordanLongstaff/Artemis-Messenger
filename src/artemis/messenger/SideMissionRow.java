package artemis.messenger;

import java.util.HashMap;

import android.content.Context;
import android.graphics.Color;
import android.widget.TableRow;
import android.widget.TextView;

public class SideMissionRow extends TableRow {
	private final String source, destination;
	private boolean started = false, completed = false;
	private final HashMap<String, Integer> rewards;
	
	public static final String BATTERY_KEY = "batteries.";
	public static final String PRODUCTION_KEY = "speed.";
	public static final String NUCLEAR_KEY = "torpedoes.";
	public static final String COOLANT_KEY = "coolant.";
	public static final String SHIELD_KEY = "generators.";
	
	private static final HashMap<String, String> rewardMap = new HashMap<String, String>();
	
	public SideMissionRow(Context context, String src, String dest, String payout) {
		super(context);
		source = src;
		destination = dest;
		rewards = new HashMap<String, Integer>();
		rewards.put(payout, 1);
		
		if (rewardMap.isEmpty()) {
			rewardMap.put(PRODUCTION_KEY, context.getString(R.string.prodSpeedPref));
			rewardMap.put(NUCLEAR_KEY, context.getString(R.string.nukePref));
			rewardMap.put(COOLANT_KEY, context.getString(R.string.extraCoolantPref));
			rewardMap.put(BATTERY_KEY, context.getString(R.string.batteryChargePref));
			rewardMap.put(SHIELD_KEY, context.getString(R.string.shieldBoostPref));
		}
		
		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		
		LayoutParams cellLayout = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
		
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
		
		setBackgroundColor(Color.parseColor("#002060"));
		sourceText.setBackgroundColor(Color.parseColor("#bf9000"));
	}
	
	public String getSource() { return source; }
	public String getDestination() { return destination; }
	public int getQuantity(String reward) {
		if (rewards.containsKey(reward)) return rewards.get(reward);
		else return 0;
	}
	public boolean hasReward(String reward) {
		return getQuantity(reward) > 0;
	}
	public boolean isStarted() { return started; }
	public boolean isCompleted() { return completed; }
	public void addReward(String reward) {
		if (rewards.containsKey(reward)) rewards.put(reward, rewards.get(reward) + 1);
		else rewards.put(reward, 1);
		TextView rewardText = (TextView) getChildAt(2);
		rewardText.setText(getRewardList());
	}
	
	@Override
	public String toString() {
		return String.format("[%s, %s, %s]", source, destination, getRewardList());
	}
	
	public String getRewardList() {
		String text = "";
		for (String reward: rewards.keySet()) {
			if (text.length() > 0) text += ", ";
			text += rewardMap.get(reward);
			if (rewards.get(reward) > 1) text += " x" + rewards.get(reward);
		}
		return text;
	}
	
	public void updateSource(String src) {
		TextView sourceText = (TextView) getChildAt(0);
		sourceText.setText(src);
	}
	
	public void markAsStarted() {
		started = true;
		TextView sourceText = (TextView) getChildAt(0);
		sourceText.setBackgroundColor(Color.parseColor("#002060"));
//		sourceText.setTextColor(Color.parseColor("#ffffff"));
		TextView destText = (TextView) getChildAt(1);
//		destText.setTextColor(Color.parseColor("#000000"));
		destText.setBackgroundColor(Color.parseColor("#bf9000"));
	}
	public void markAsCompleted() {
		completed = true;
		int green = Color.parseColor("#008000");
		for (int i = 0; i < getChildCount(); i++) getChildAt(i).setBackgroundColor(green);
		TextView rewardText = (TextView) getChildAt(2);
		rewardText.setText(getRewardList() + " (Acquired)");
	}
}