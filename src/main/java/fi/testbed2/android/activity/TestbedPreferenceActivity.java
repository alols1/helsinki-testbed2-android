package fi.testbed2.android.activity;

import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.ListPreference;
import com.google.inject.Inject;
import com.threefiftynice.android.preference.ListPreferenceMultiSelect;
import fi.testbed2.R;
import fi.testbed2.android.app.Logger;
import fi.testbed2.android.ui.dialog.DialogBuilder;
import fi.testbed2.domain.MapLocationGPS;
import fi.testbed2.service.LocationService;
import fi.testbed2.service.MunicipalityService;
import fi.testbed2.service.SettingsService;
import roboguice.activity.RoboPreferenceActivity;

public class TestbedPreferenceActivity extends RoboPreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    MunicipalityService municipalityService;

    @Inject
    SettingsService settingsService;

    @Inject
    DialogBuilder dialogBuilder;

    private ListPreference locationProviderList;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        initMunicipalityList();

        locationProviderList = (ListPreference)getPreferenceScreen().findPreference(SettingsService.PREF_LOCATION_PROVIDER);

    }

    @Override
    protected void onResume() {
        super.onResume();

        String provider = settingsService.getLocationProvider();
        locationProviderList.setSummary(getStringForCurrentlySelectedLocationProvider(provider));

        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    private void initMunicipalityList() {

        String[] entries = municipalityService.getFinlandMunicipalityNamesShownInTestbedMap();
        String[] entryValues = entries;
        ListPreferenceMultiSelect lp =
                (ListPreferenceMultiSelect)getPreferenceManager().
                        findPreference(SettingsService.PREF_LOCATION_SHOW_MUNICIPALITIES);
        lp.setEntries(entries);
        lp.setEntryValues(entryValues);

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        Logger.debug("PreferenceActivity.onSharedPreferenceChanged: " + key);

        if (key.equals(SettingsService.PREF_LOCATION_PROVIDER)) {

            Logger.debug("Updating location provider...");

            String newProvider = sharedPreferences.getString(key, LocationManager.NETWORK_PROVIDER);

            if (newProvider.equals(LocationService.LOCATION_PROVIDER_FIXED)) {

                MapLocationGPS loc = settingsService.saveCurrentLocationAsFixedLocation();
                Logger.debug("Using fixed provider: " + loc);

                /**
                 * If no location is available, revert to GPS provider.
                 */
                if (loc==null) {
                    newProvider = LocationManager.GPS_PROVIDER;
                    settingsService.setLocationProvider(newProvider);
                    dialogBuilder.getErrorDialog(
                            this.getString(R.string.preference_location_provider_fixed_error_dialog)).show();
                }

            }

            locationProviderList.setSummary(getStringForCurrentlySelectedLocationProvider(newProvider));

        }

    }

    private String getStringForCurrentlySelectedLocationProvider(String newProvider) {

        String currentValue = "";

        if (newProvider.equals(LocationService.LOCATION_PROVIDER_FIXED)) {
            MapLocationGPS loc = settingsService.getSavedFixedLocation();
            String coordinates = "lat: "+loc.getLatitude()+", lon: "+loc.getLongitude();
            currentValue = this.getString(R.string.preference_location_provider_fixed, coordinates);
        } else if (newProvider.equals(LocationManager.NETWORK_PROVIDER)) {
            currentValue = this.getString(R.string.preference_location_provider_network);
        } else if (newProvider.equals(LocationManager.GPS_PROVIDER)) {
            currentValue = this.getString(R.string.preference_location_provider_gps);
        }

        return this.getString(R.string.preference_location_provider_summary, currentValue);

    }

}
