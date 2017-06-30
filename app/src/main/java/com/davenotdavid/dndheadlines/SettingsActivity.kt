package com.davenotdavid.dndheadlines

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.preference.Preference
import android.support.v7.preference.ListPreference
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.MenuItem

/**
 * Displays a list of settings via a PreferenceFragment.
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // Dismisses the Activity should the back button (action bar) be pressed.
        if (item.itemId == android.R.id.home) {
            finish()

            return true
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * A fragment subclass of [PreferenceFragmentCompat] that stores and retrieves the user's
     * preference data. A [ListPreference] will be displayed.
     */
    class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.settings_main)

            // In case more preferences are added later to the app, the total amount of preferences
            // is retrieved in order to iterate through each one to eventually set the
            // ListPreference as the preference summary.
            val sharedPreferences = preferenceScreen.sharedPreferences
            val preferenceCount = preferenceScreen.preferenceCount
            for (i in 0..preferenceCount - 1) {
                val preference = preferenceScreen.getPreference(i)
                if (preference is ListPreference) {
                    val value = sharedPreferences.getString(preference.getKey(), "")
                    setPreferenceSummary(preference, value)
                }
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Registers the SharedPreference to OnSharedPreferenceChangeListener.
            preferenceScreen.sharedPreferences
                    .registerOnSharedPreferenceChangeListener(this)
        }

        override fun onDestroy() {
            super.onDestroy()

            // Unregisters the SharedPreference to OnSharedPreferenceChangeListener.
            preferenceScreen.sharedPreferences
                    .unregisterOnSharedPreferenceChangeListener(this)
        }

        /**
         * Invoked whenever SharedPreferences changes.

         * @param sharedPreferences is a SharedPreferences object used to retrieve its preference
         * *                          value.
         * *
         * @param key is the preference key.
         */
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            val preference = findPreference(key)
            if (preference != null && preference is ListPreference) {
                val value = sharedPreferences.getString(key, "")
                setPreferenceSummary(preference, value)
            }
        }

        /**
         * Sets the preference parameter as the preference summary.

         * @param preference is the type of preference to be updated.
         * *
         * @param value is the value that the preference was updated to.
         */
        private fun setPreferenceSummary(preference: Preference, value: String) {
            if (preference is ListPreference) {
                val listPreference = preference
                val prefIndex = listPreference.findIndexOfValue(value)
                if (prefIndex >= 0) { // Custom way of validating
                    listPreference.summary = listPreference.entries[prefIndex]
                }
            }
        }
    }
}
