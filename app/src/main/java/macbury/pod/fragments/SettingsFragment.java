package macbury.pod.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import macbury.pod.R;
import macbury.pod.managers.App;
import macbury.pod.managers.SettingsManager;

/**
 * Created by macbury on 09.09.14.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.settings);

    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onResume() {
    super.onResume();
    updateUI();
  }

  private void updateUI() {
    PreferenceScreen screen                = getPreferenceScreen();
    App application         = (App)(getActivity().getApplication());
    SettingsManager    manager             = application.settings;

    EditTextPreference endpointPreference  = (EditTextPreference)screen.findPreference(SettingsManager.KEY_API_ENDPOINT);
    endpointPreference.setSummary(manager.getApiEndpoint());

    ListPreference syncFreq                = (ListPreference)screen.findPreference(SettingsManager.KEY_SYNC_FREQ);
    syncFreq.setSummary(syncFreq.getEntry());

    EditTextPreference proxyHostPreference = (EditTextPreference)screen.findPreference(SettingsManager.KEY_PROXY_HOST);
    proxyHostPreference.setSummary(manager.getProxyHost());

    EditTextPreference proxyPortPreference = (EditTextPreference)screen.findPreference(SettingsManager.KEY_PROXY_PORT);
    proxyPortPreference.setSummary(String.valueOf(manager.getProxyPort()));

  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    updateUI();
    App.current().settings.update();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }
}
