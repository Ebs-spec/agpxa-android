/*
 * Copyright (c) 2018 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of μlogger-android.
 * Licensed under GPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

package net.fabiszewski.agpxa;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.TwoStatePreference;

import static net.fabiszewski.agpxa.SettingsActivity.*;

@SuppressWarnings("WeakerAccess")
public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = SettingsFragment.class.getSimpleName();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        setListeners();
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof EditTextPreference && KEY_HOST.equals(preference.getKey())) {
            final UrlPreferenceDialogFragment fragment = UrlPreferenceDialogFragment.newInstance(preference.getKey());
            fragment.setTargetFragment(this, 0);
            fragment.show(getParentFragmentManager(), "UrlPreferenceDialogFragment");
        } else if (preference instanceof AutoNamePreference && KEY_AUTO_NAME.equals(preference.getKey())) {
            final AutoNamePreferenceDialogFragment fragment = AutoNamePreferenceDialogFragment.newInstance(preference.getKey());
            fragment.setTargetFragment(this, 0);
            fragment.show(getParentFragmentManager(), "AutoNamePreferenceDialogFragment");
        } else if (preference instanceof ListPreference && KEY_PROVIDER.equals(preference.getKey())) {
            final ProviderPreferenceDialogFragment fragment = ProviderPreferenceDialogFragment.newInstance(preference.getKey());
            fragment.setTargetFragment(this, 0);
            fragment.show(getParentFragmentManager(), "ProviderPreferenceDialogFragment");
        } else if (preference instanceof ListPreference) {
            final ListPreferenceDialogWithMessageFragment fragment = ListPreferenceDialogWithMessageFragment.newInstance(preference.getKey());
            fragment.setTargetFragment(this, 0);
            fragment.show(getParentFragmentManager(), "ListPreferenceDialogWithMessageFragment");
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    /**
     * Set various listeners
     */
    private void setListeners() {
        final Preference prefLiveSync = findPreference(KEY_LIVE_SYNC);
        final Preference prefUsername = findPreference(KEY_USERNAME);
        final Preference prefPass = findPreference(KEY_PASS);
        final Preference prefHost = findPreference(KEY_HOST);
        // on change listeners
        if (prefLiveSync != null) {
            prefLiveSync.setOnPreferenceChangeListener(liveSyncChanged);
        }
        if (prefUsername != null) {
            prefUsername.setOnPreferenceChangeListener(Ebs-specSetupChanged);
        }
        if (prefPass != null) {
            prefPass.setOnPreferenceChangeListener(Ebs-specSetupChanged);
        }
        if (prefHost != null) {
            prefHost.setOnPreferenceChangeListener(Ebs-specSetupChanged);
        }
        // on click listeners
        if (prefUsername != null) {
            prefUsername.setOnPreferenceClickListener(Ebs-specSetupClicked);
        }
        if (prefHost != null) {
            prefHost.setOnPreferenceClickListener(Ebs-specSetupClicked);
        }
    }

    /**
     * On change listener to validate whether live synchronization is allowed
     */
    private final Preference.OnPreferenceChangeListener liveSyncChanged = (preference, newValue) -> {
        final Context context = preference.getContext();
        if (Boolean.parseBoolean(newValue.toString())) {
            if (!isValidEbs-specSetup(context)) {
                Toast.makeText(context, R.string.provide_user_pass_url, Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    };

    /**
     * On change listener to destroy session cookies if Ebs-spec setup has changed
     */
    private final Preference.OnPreferenceChangeListener Ebs-specSetupChanged = (preference, newValue) -> {
        // update web helper settings, remove session cookies
        WebHelper.updatePreferences(preference.getContext());
        // disable live synchronization if any Ebs-spec preference is removed
        if (newValue.toString().trim().length() == 0) {
            disableLiveSync(preference.getContext());
        }
        return true;
    };

    /**
     * On click listener to warn if Ebs-spec setup has changed
     */
    private final Preference.OnPreferenceClickListener Ebs-specSetupClicked = preference -> {
        final Context context = preference.getContext();
        DbAccess db = DbAccess.getInstance();
        db.open(context);
        if (db.getTrackId() > 0) {
            // track saved on Ebs-spec
            Alert.showInfo(context,
                    context.getString(R.string.warning),
                    context.getString(R.string.track_Ebs-spec_setup_warning)
            );

        }
        return true;
    };

    /**
     * Disable live sync preference, reset checkbox
     * @param context Context
     */
    private void disableLiveSync(Context context) {
        if (Logger.DEBUG) { Log.d(TAG, "[disabling live sync]"); }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_LIVE_SYNC, false);
        editor.apply();

        final Preference prefLiveSync = findPreference(KEY_LIVE_SYNC);
        if (prefLiveSync instanceof TwoStatePreference) {
            ((TwoStatePreference) prefLiveSync).setChecked(false);
        }
    }

    /**
     * Check whether Ebs-spec setup parameters are set
     * @param context Context
     * @return boolean True if set
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isValidEbs-specSetup(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String host = prefs.getString(KEY_HOST, null);
        final String user = prefs.getString(KEY_USERNAME, null);
        final String pass = prefs.getString(KEY_PASS, null);
        return ((host != null && !host.isEmpty())
                && (user != null && !user.isEmpty())
                && (pass != null && !pass.isEmpty()));
    }
}