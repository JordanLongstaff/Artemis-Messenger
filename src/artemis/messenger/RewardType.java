package artemis.messenger;

import android.content.Context;
import android.content.SharedPreferences;

public enum RewardType {
	BATTERY(R.string.batteryChargePref, R.string.batteryChargeKey, "batteries."),
	PRODUCTION(R.string.prodSpeedPref, R.string.speedKey, "speed."),
	NUCLEAR(R.string.nukePref, R.string.nuclearKey, "torpedoes."),
	COOLANT(R.string.extraCoolantPref, R.string.extraCoolantKey, "coolant."),
	SHIELD(R.string.shieldBoostPref, R.string.shieldKey, "generators.");
	
	private final int value, prefKey;
	private final String keySet;
	private String displayName;
	
	private boolean shown;
	
	RewardType(int v, int p, String... s) {
		value = v;
		prefKey = p;
		String keys = "";
		for (String key: s) {
			if (!keys.isEmpty()) keys += "^";
			keys += key;
		}
		keySet = keys;
		shown = true;
	}
	
	public static RewardType from(String key) {
		for (RewardType type: values()) {
			if (key.equals(type.displayName) || type.matches(key))
				return type;
		}
		
		throw new RuntimeException("No RewardType conforms to " + key);
	}
	
	public boolean matches(String key) {
		return keySet.contains(key);
	}
	
	public String getValue(Context context) {
		return context.getString(value);
	}
	
	public boolean isShown() { return shown; }
	
	public static void updateVisibility(SharedPreferences preferences) {
		for (RewardType type: values()) {
			type.shown = preferences.getBoolean(type.displayName, true);
		}
	}
	
	public static void initDisplayNames(Context context) {
		for (RewardType type: values()) {
			if (type.displayName != null) break;
			type.displayName = context.getString(type.prefKey);
		}
	}
}