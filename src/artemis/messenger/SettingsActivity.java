package artemis.messenger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the
 * <a href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			// TODO: If Settings has multiple levels, Up should navigate up
			// that hierarchy.
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		setupSimplePreferencesScreen();
	}

	/**
	 * Shows the simplified settings UI if the device configuration if the
	 * device configuration dictates that a simplified, single-pane UI should be
	 * shown.
	 */
	private void setupSimplePreferencesScreen() {		
		addPreferencesFromResource(R.xml.preference);
		
		ListPreference vesselDataPref = (ListPreference) findPreference(getString(R.string.vesselDataKey));
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			vesselDataPref.setEntries(getResources().getStringArray(R.array.vesselDataPrefNoCard));
			vesselDataPref.setEntryValues(getResources().getStringArray(R.array.vesselDataValueNoCard));
		} else {
			vesselDataPref.setEntries(getResources().getStringArray(R.array.vesselDataPrefList));
			vesselDataPref.setEntryValues(getResources().getStringArray(R.array.vesselDataValueList));
		}
		bindPreferenceSummaryToValue(vesselDataPref);
		
		Preference helpPref = findPreference(getString(R.string.helpTopicsKey));
		helpPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference pref) {
				Intent helpIntent = new Intent(SettingsActivity.this, HelpActivity.class);
				startActivity(helpIntent);
				return false;
			}
		});
		
		Preference showAllPref = findPreference(getString(R.string.showAllButton));
		showAllPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				sharedPreferences.edit()
				.putBoolean(getString(R.string.batteryChargeKey), true)
				.putBoolean(getString(R.string.extraCoolantKey), true)
				.putBoolean(getString(R.string.nuclearKey), true)
				.putBoolean(getString(R.string.speedKey), true)
				.putBoolean(getString(R.string.shieldKey), true)
				.commit();
				
				CheckBoxPreference batteryPref =
						(CheckBoxPreference) findPreference(getString(R.string.batteryChargeKey));
				batteryPref.setChecked(true);
				
				CheckBoxPreference coolantPref =
						(CheckBoxPreference) findPreference(getString(R.string.extraCoolantKey));
				coolantPref.setChecked(true);
				
				CheckBoxPreference nukePref =
						(CheckBoxPreference) findPreference(getString(R.string.nuclearKey));
				nukePref.setChecked(true);
				
				CheckBoxPreference speedPref =
						(CheckBoxPreference) findPreference(getString(R.string.speedKey));
				speedPref.setChecked(true);
				
				CheckBoxPreference shieldPref =
						(CheckBoxPreference) findPreference(getString(R.string.shieldKey));
				shieldPref.setChecked(true);
				return false;
			}
		});
		
		Preference showAllRoutingPref = findPreference(getString(R.string.showAllRoutingButton));
		showAllRoutingPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				sharedPreferences.edit()
				.putBoolean(getString(R.string.energyKey), true)
				.putBoolean(getString(R.string.hasTorpsKey), true)
				.putBoolean(getString(R.string.needEnergyKey), true)
				.putBoolean(getString(R.string.needDamConKey), true)
				.putBoolean(getString(R.string.malfunctionKey), true)
				.putBoolean(getString(R.string.hostageKey), true)
				.putBoolean(getString(R.string.commandeeredKey), true)
				.commit();
				
				CheckBoxPreference hasEnergyPref =
						(CheckBoxPreference) findPreference(getString(R.string.energyKey));
				hasEnergyPref.setChecked(true);
				
				CheckBoxPreference hasTorpsPref =
						(CheckBoxPreference) findPreference(getString(R.string.hasTorpsKey));
				hasTorpsPref.setChecked(true);
				
				CheckBoxPreference needEnergyPref =
						(CheckBoxPreference) findPreference(getString(R.string.needEnergyKey));
				needEnergyPref.setChecked(true);
				
				CheckBoxPreference needDamConPref =
						(CheckBoxPreference) findPreference(getString(R.string.needDamConKey));
				needDamConPref.setChecked(true);
				
				CheckBoxPreference malfunctionPref =
						(CheckBoxPreference) findPreference(getString(R.string.malfunctionKey));
				malfunctionPref.setChecked(true);
				
				CheckBoxPreference hostagePref =
						(CheckBoxPreference) findPreference(getString(R.string.hostageKey));
				hostagePref.setChecked(true);
				
				CheckBoxPreference commandeeredPref =
						(CheckBoxPreference) findPreference(getString(R.string.commandeeredKey));
				commandeeredPref.setChecked(true);
				
				return false;
			}
		});
		
		Preference showNoneRoutingPref = findPreference(getString(R.string.showNoneRoutingButton));
		showNoneRoutingPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				sharedPreferences.edit()
				.putBoolean(getString(R.string.energyKey), false)
				.putBoolean(getString(R.string.hasTorpsKey), false)
				.putBoolean(getString(R.string.needEnergyKey), false)
				.putBoolean(getString(R.string.needDamConKey), false)
				.putBoolean(getString(R.string.malfunctionKey), false)
				.putBoolean(getString(R.string.hostageKey), false)
				.putBoolean(getString(R.string.commandeeredKey), false)
				.commit();
				
				CheckBoxPreference hasEnergyPref =
						(CheckBoxPreference) findPreference(getString(R.string.energyKey));
				hasEnergyPref.setChecked(false);
				
				CheckBoxPreference hasTorpsPref =
						(CheckBoxPreference) findPreference(getString(R.string.hasTorpsKey));
				hasTorpsPref.setChecked(false);
				
				CheckBoxPreference needEnergyPref =
						(CheckBoxPreference) findPreference(getString(R.string.needEnergyKey));
				needEnergyPref.setChecked(false);
				
				CheckBoxPreference needDamConPref =
						(CheckBoxPreference) findPreference(getString(R.string.needDamConKey));
				needDamConPref.setChecked(false);
				
				CheckBoxPreference malfunctionPref =
						(CheckBoxPreference) findPreference(getString(R.string.malfunctionKey));
				malfunctionPref.setChecked(false);
				
				CheckBoxPreference hostagePref =
						(CheckBoxPreference) findPreference(getString(R.string.hostageKey));
				hostagePref.setChecked(false);
				
				CheckBoxPreference commandeeredPref =
						(CheckBoxPreference) findPreference(getString(R.string.commandeeredKey));
				commandeeredPref.setChecked(false);
				
				return false;
			}
		});
		
		Preference defaultPref = (Preference) findPreference(getString(R.string.restoreDefaultButton));
		defaultPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference pref) {
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				sharedPreferences.edit().clear().commit();
				PreferenceManager.setDefaultValues(getBaseContext(), R.xml.preference, true);
				
				CheckBoxPreference batteryPref =
						(CheckBoxPreference) findPreference(getString(R.string.batteryChargeKey));
				batteryPref.setChecked(sharedPreferences.getBoolean(getString(R.string.batteryChargeKey), true));
				
				CheckBoxPreference coolantPref =
						(CheckBoxPreference) findPreference(getString(R.string.extraCoolantKey));
				coolantPref.setChecked(sharedPreferences.getBoolean(getString(R.string.extraCoolantKey), true));
				
				CheckBoxPreference nukePref =
						(CheckBoxPreference) findPreference(getString(R.string.nuclearKey));
				nukePref.setChecked(sharedPreferences.getBoolean(getString(R.string.nuclearKey), true));
				
				CheckBoxPreference speedPref =
						(CheckBoxPreference) findPreference(getString(R.string.speedKey));
				speedPref.setChecked(sharedPreferences.getBoolean(getString(R.string.speedKey), true));
				
				CheckBoxPreference shieldPref =
						(CheckBoxPreference) findPreference(getString(R.string.shieldKey));
				shieldPref.setChecked(sharedPreferences.getBoolean(getString(R.string.shieldKey), true));
				
				CheckBoxPreference energyFirstPref =
						(CheckBoxPreference) findPreference(getString(R.string.energyFirstKey));
				energyFirstPref.setChecked(sharedPreferences.getBoolean(getString(R.string.energyFirstKey), true));
				
				CheckBoxPreference hasEnergyPref =
						(CheckBoxPreference) findPreference(getString(R.string.energyKey));
				hasEnergyPref.setChecked(sharedPreferences.getBoolean(getString(R.string.energyKey), false));
				
				CheckBoxPreference hasTorpsPref =
						(CheckBoxPreference) findPreference(getString(R.string.hasTorpsKey));
				hasTorpsPref.setChecked(sharedPreferences.getBoolean(getString(R.string.hasTorpsKey), false));
				
				CheckBoxPreference needEnergyPref =
						(CheckBoxPreference) findPreference(getString(R.string.needEnergyKey));
				needEnergyPref.setChecked(sharedPreferences.getBoolean(getString(R.string.needEnergyKey), false));
				
				CheckBoxPreference needDamConPref =
						(CheckBoxPreference) findPreference(getString(R.string.needDamConKey));
				needDamConPref.setChecked(sharedPreferences.getBoolean(getString(R.string.needDamConKey), false));
				
				CheckBoxPreference malfunctionPref =
						(CheckBoxPreference) findPreference(getString(R.string.malfunctionKey));
				malfunctionPref.setChecked(sharedPreferences.getBoolean(getString(R.string.malfunctionKey), false));
				
				CheckBoxPreference hostagePref =
						(CheckBoxPreference) findPreference(getString(R.string.hostageKey));
				hostagePref.setChecked(sharedPreferences.getBoolean(getString(R.string.hostageKey), false));
				
				CheckBoxPreference commandeeredPref =
						(CheckBoxPreference) findPreference(getString(R.string.commandeeredKey));
				commandeeredPref.setChecked(sharedPreferences.getBoolean(getString(R.string.commandeeredKey), false));
				
				CheckBoxPreference helpStartupPref =
						(CheckBoxPreference) findPreference(getString(R.string.helpStartupKey));
				helpStartupPref.setChecked(sharedPreferences.getBoolean(getString(R.string.helpStartupKey), true));
				
				return false;
			}
		});
		
		RingtonePreference newMissionPref = (RingtonePreference) findPreference(getString(R.string.newMissionPrefKey));
		bindPreferenceSummaryToValue(newMissionPref);
		
		RingtonePreference atSrcPref = (RingtonePreference) findPreference(getString(R.string.foundSrcPrefKey));
		bindPreferenceSummaryToValue(atSrcPref);
		
		RingtonePreference atDestPref = (RingtonePreference) findPreference(getString(R.string.foundDestPrefKey));
		bindPreferenceSummaryToValue(atDestPref);
	}
	
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();

			if (preference instanceof ListPreference) {
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);

				// Set the summary to reflect the new value.
				preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

			} else if (preference instanceof RingtonePreference) {
				// For ringtone preferences, look up the correct display value
				// using RingtoneManager.
				if (TextUtils.isEmpty(stringValue)) {
					// Empty values correspond to 'silent' (no ringtone).
					preference.setSummary(R.string.pref_ringtone_silent);

				} else {
					Ringtone ringtone = RingtoneManager.getRingtone(preference.getContext(), Uri.parse(stringValue));

					if (ringtone == null) {
						// Clear the summary if there was a lookup error.
						preference.setSummary(null);
					} else {
						// Set the summary to reflect the new ringtone display
						// name.
						String name = ringtone.getTitle(preference.getContext());
						preference.setSummary(name);
					}
				}

			} else {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(stringValue);
			}
			return true;
		}
	};

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 *
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager
				.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
	}
}
