package com.davenotdavid.dndheadlines;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.MenuItem;

/**
 * Displays a list of settings via a PreferenceFragment.
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Dismisses the Activity should the back button (action bar) be pressed.
        if (item.getItemId() == android.R.id.home) {
            finish();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A fragment subclass of {@link PreferenceFragmentCompat} that stores and retrieves the user's
     * preference data. A {@link ListPreference} will be displayed.
     */
    public static class SettingsFragment extends PreferenceFragmentCompat implements
            OnSharedPreferenceChangeListener {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.settings_main);

            // In case more preferences are added later to the app, the total amount of preferences
            // is retrieved in order to iterate through each one to eventually set the
            // ListPreference as the preference summary.
            SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
            int preferenceCount = getPreferenceScreen().getPreferenceCount();
            for (int i = 0; i < preferenceCount; i++) {
                Preference preference = getPreferenceScreen().getPreference(i);
                if (preference instanceof ListPreference) {
                    String value = sharedPreferences.getString(preference.getKey(), "");
                    setPreferenceSummary(preference, value);
                }
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Registers the SharedPreference to OnSharedPreferenceChangeListener.
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            // Unregisters the SharedPreference to OnSharedPreferenceChangeListener.
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        /**
         * Invoked whenever SharedPreferences changes.
         *
         * @param sharedPreferences is a SharedPreferences object used to retrieve its preference
         *                          value.
         * @param key is the preference key.
         */
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference preference = findPreference(key);
            if (preference != null && preference instanceof ListPreference) {
                String value = sharedPreferences.getString(key, "");
                setPreferenceSummary(preference, value);
            }
        }

        /**
         * Sets the preference parameter as the preference summary.
         *
         * @param preference is the type of preference to be updated.
         * @param value is the value that the preference was updated to.
         */
        private void setPreferenceSummary(Preference preference, String value) {
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int prefIndex = listPreference.findIndexOfValue(value);
                if (prefIndex >= 0) { // Custom way of validating
                    listPreference.setSummary(listPreference.getEntries()[prefIndex]);
                }
            }
        }
    }
}
