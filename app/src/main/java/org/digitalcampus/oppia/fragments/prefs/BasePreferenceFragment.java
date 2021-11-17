package org.digitalcampus.oppia.fragments.prefs;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import org.digitalcampus.oppia.activity.PrefsActivity;
import org.digitalcampus.oppia.application.AdminSecurityManager;
import org.digitalcampus.oppia.application.App;
import org.digitalcampus.oppia.utils.custom_prefs.AdminEditTextPreference;
import org.digitalcampus.oppia.utils.custom_prefs.AdminPreference;

import java.util.ArrayList;
import java.util.List;

public abstract class BasePreferenceFragment extends PreferenceFragmentCompat {

    protected List<String> adminProtectedEditTextPrefs = new ArrayList<>();
    protected SharedPreferences parentPrefs;
    private AdminSecurityManager adminSecurityManager;

    public void setPrefs(SharedPreferences prefs) {
        this.parentPrefs = prefs;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adminSecurityManager = AdminSecurityManager.with(getActivity());

        protectAdminPreferences(getPreferenceScreen());

    }

    private void protectAdminPreferences(PreferenceGroup preferenceGroup) {
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            Preference preference = preferenceGroup.getPreference(i);
            if (preference instanceof PreferenceCategory || preference instanceof PreferenceScreen) {
                protectAdminPreferences((PreferenceGroup) preference);
            }

            if (preference instanceof AdminPreference && adminSecurityManager.isActionProtected(preference.getKey())) {
                AdminPreference adminPreference = (AdminPreference) preference;
                adminPreference.setOnAdminPreferenceClickListener(adminPref -> {
                    adminSecurityManager.promptAdminPassword(() -> adminPreference.onAccessGranted());
                    return true;
                });

            }
        }
    }

    void protectAdminEditTextPreferences() {
        for (String prefKey : adminProtectedEditTextPrefs) {

            final EditTextPreference editTextPreference = findPreference(prefKey);
            if (editTextPreference == null) {
                continue;
            }

            if (editTextPreference instanceof AdminEditTextPreference) {
                editTextPreference.setOnPreferenceClickListener(preference -> {

                    if (parentPrefs == null) {
                        parentPrefs = App.getPrefs(getActivity());
                    }

                    if (parentPrefs.getBoolean(PrefsActivity.PREF_ADMIN_PROTECTION, false)) {
                        AdminSecurityManager.with((Activity) getContext()).promptAdminPassword(() -> {
                            getPreferenceManager().showDialog(editTextPreference);
                        });
                        return true;
                    }

                    return false;
                });
            }
        }
    }

    protected boolean onPreferenceChangedDelegate(Preference preference, Object newValue) {
        return true;
    }

    void liveUpdateSummary(String prefKey) {
        liveUpdateSummary(prefKey, "");
    }

    void liveUpdateSummary(String prefKey, final String append) {

        Preference pref = findPreference(prefKey);
        if (pref instanceof ListPreference) {
            final ListPreference listPref = (ListPreference) pref;
            listPref.setSummary(listPref.getEntry() + append);
            listPref.setOnPreferenceChangeListener((preference, newValue) -> {
                CharSequence[] entryValues = listPref.getEntryValues();
                for (int i = 0; i < entryValues.length; i++) {
                    if (entryValues[i].equals(newValue)) {
                        listPref.setSummary(listPref.getEntries()[i] + append);
                        break;
                    }
                }
                return true;
            });
        } else if (pref instanceof EditTextPreference) {
            final EditTextPreference editPref = (EditTextPreference) pref;
            editPref.setSummary(editPref.getText() + append);
            editPref.setOnPreferenceChangeListener((preference, newValue) -> {

                boolean mustUpdate = onPreferenceChangedDelegate(preference, newValue);
                if (!mustUpdate) {
                    return false;
                }

                editPref.setSummary(newValue + append);
                return true;
            });
        }

    }

}
