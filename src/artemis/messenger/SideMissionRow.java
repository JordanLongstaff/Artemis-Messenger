package artemis.messenger;

import java.util.EnumMap;

import android.content.Context;
import android.graphics.Color;
import android.widget.TableRow;
import android.widget.TextView;

public class SideMissionRow extends TableRow {
	// Side mission information
	private final String source, destination;
	private String ship;
	private boolean started = false, completed = false;
	private final EnumMap<RewardType, Integer> rewards;
	
	public SideMissionRow(Context context, String src, String dest, RewardType payout) {
		// Fill side mission information
		super(context);
		source = src;
		destination = dest;
		ship = "";
		rewards = new EnumMap<RewardType, Integer>(RewardType.class);
		rewards.put(payout, 1);
		
		// Build layout
		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		
		LayoutParams cellLayout = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
		
		// Add table row cells
		TextView sourceText = new TextView(context);
		sourceText.setTypeface(ListActivity.APP_FONT);
		sourceText.setText(source);
		sourceText.setLayoutParams(cellLayout);
		sourceText.setPadding(3, 3, 3, 3);
		sourceText.setTextColor(Color.LTGRAY);
		addView(sourceText);
		
		TextView destText = new TextView(context);
		destText.setTypeface(ListActivity.APP_FONT);
		destText.setText(destination);
		destText.setLayoutParams(cellLayout);
		destText.setPadding(3, 3, 3, 3);
		destText.setTextColor(Color.LTGRAY);
		addView(destText);
		
		TextView rewardText = new TextView(context);
		rewardText.setTypeface(ListActivity.APP_FONT);
		rewardText.setText(payout.getValue(context));
		rewardText.setLayoutParams(cellLayout);
		rewardText.setPadding(3, 3, 3, 3);
		rewardText.setTextColor(Color.LTGRAY);
		addView(rewardText);
		
		// Set background colours
		setBackgroundColor(Color.parseColor("#002060"));
		sourceText.setBackgroundColor(Color.parseColor("#bf9000"));
	}
	
	// Getter/manipulator methods
	public String getSource() { return source; }
	public String getDestination() { return destination; }
	public int getQuantity(RewardType reward) {
		if (rewards.containsKey(reward)) return rewards.get(reward);
		else return 0;
	}
	public boolean hasReward(String key) {
		return hasReward(RewardType.from(key));
	}
	public boolean hasReward(RewardType reward) {
		return getQuantity(reward) > 0;
	}
	public int getNumRewards() {
		int sum = 0;
		for (RewardType r: rewards.keySet()) {
			if (r.isShown()) {
				for (RewardType rt: rewards.keySet()) sum += rewards.get(rt);
				break;
			}
		}
		return sum;
	}
	public boolean isStarted() { return started; }
	public boolean isCompleted() { return completed; }
	public void addReward(String key) {
		addReward(RewardType.from(key));
	}
	public void addReward(RewardType reward) {
		if (rewards.containsKey(reward)) rewards.put(reward, rewards.get(reward) + 1);
		else rewards.put(reward, 1);
		updateRewardText();
	}
	public void updateRewardText() {
		final TextView rewardText = (TextView) getChildAt(2);
		final String rewardString = getRewardList();
		post(new Runnable() {
			@Override
			public void run() {
				rewardText.setText(rewardString);
			}
		});
	}
	public String getPlayerShip() { return ship; }
	public void setPlayerShip(String s) { ship = s; }
	
	// String formatter
	@Override
	public String toString() {
		return String.format("[%s, %s, %s]", source, destination, getRewardList());
	}
	
	// Get list of all rewards
	public String getRewardList() {
		String text = "";
		for (RewardType reward: rewards.keySet()) {
			if (!text.isEmpty()) text += ", ";
			text += reward.getValue(getContext());
			if (rewards.get(reward) > 1) text += " x" + rewards.get(reward);
		}
		return text;
	}
	
	// Need to change source text if source was previously ambiguous
	public void updateSource(final String src) {
		final TextView sourceText = (TextView) getChildAt(0);
		post(new Runnable() {
			@Override
			public void run() {
				sourceText.setText(src);
			}
		});
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
			final int green = Color.parseColor("#008000");
			final TextView rewardText = (TextView) getChildAt(2);
			final String rewards = getRewardList();
			post(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < getChildCount(); i++) getChildAt(i).setBackgroundColor(green);
					rewardText.setText(rewards + " (Acquired)");
				}
			});
		} else if (started) {
			final TextView sourceText = (TextView) getChildAt(0);
			final TextView destText = (TextView) getChildAt(1);
			post(new Runnable() {
				@Override
				public void run() {
					sourceText.setBackgroundColor(Color.parseColor("#002060"));
					destText.setBackgroundColor(Color.parseColor("#bf9000"));
				}
			});
		}
	}
}