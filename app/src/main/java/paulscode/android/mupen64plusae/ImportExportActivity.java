/*
 * Mupen64PlusAE, an N64 emulator for the Android platform
 *
 * Copyright (C) 2013 Paul Lamb
 *
 * This file is part of Mupen64PlusAE.
 *
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * Authors: fzurita
 */
package paulscode.android.mupen64plusae;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;

import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.preference.PrefUtil;
import paulscode.android.mupen64plusae.util.LegacyFilePicker;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;

public class ImportExportActivity extends AppCompatPreferenceActivity implements Preference.OnPreferenceClickListener
{
    // These constants must match the keys used in res/xml/preferences.xml
    private static final String ACTION_EXPORT_GAME_DATA = "actionExportGameData";
    private static final String ACTION_EXPORT_CHEATS_AND_PROFILES = "actionExportCheatsAndProfiles";
    private static final String ACTION_IMPORT_GAME_DATA = "actionImportGameData";
    private static final String ACTION_IMPORT_CHEATS_AND_PROFILES = "actionImportCheatsAndProfiles";

    private static final int PICK_FILE_EXPORT_GAME_DATA_REQUEST_CODE = 1;
    private static final int PICK_FILE_EXPORT_CHEATS_AND_PROFILES_REQUEST_CODE = 2;
    private static final int PICK_FILE_IMPORT_GAME_DATA_REQUEST_CODE = 3;
    private static final int PICK_FILE_IMPORT_CHEATS_AND_PROFILES_REQUEST_CODE = 4;

    private static final String STATE_COPY_TO_SD_FRAGMENT= "STATE_COPY_TO_SD_FRAGMENT";
    private CopyToSdFragment mCopyToSdFragment = null;
    private static final String STATE_COPY_FROM_SD_FRAGMENT= "STATE_COPY_FROM_SD_FRAGMENT";
    private CopyFromSdFragment mCopyFromSdFragment = null;

    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;

    @Override
    protected void attachBaseContext(Context newBase) {
        if(TextUtils.isEmpty(LocaleContextWrapper.getLocalCode()))
        {
            super.attachBaseContext(newBase);
        }
        else
        {
            super.attachBaseContext(LocaleContextWrapper.wrap(newBase,LocaleContextWrapper.getLocalCode()));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        final FragmentManager fm = getSupportFragmentManager();
        mCopyToSdFragment = (CopyToSdFragment) fm.findFragmentByTag(STATE_COPY_TO_SD_FRAGMENT);

        if(mCopyToSdFragment == null)
        {
            mCopyToSdFragment = new CopyToSdFragment();
            fm.beginTransaction().add(mCopyToSdFragment, STATE_COPY_TO_SD_FRAGMENT).commit();
        }

        mCopyFromSdFragment = (CopyFromSdFragment) fm.findFragmentByTag(STATE_COPY_FROM_SD_FRAGMENT);

        if(mCopyFromSdFragment == null)
        {
            mCopyFromSdFragment = new CopyFromSdFragment();
            fm.beginTransaction().add(mCopyFromSdFragment, STATE_COPY_FROM_SD_FRAGMENT).commit();
        }

        // Get app data and user preferences
        mAppData = new AppData(this);
        mGlobalPrefs = new GlobalPrefs(this, mAppData);

        PreferenceManager.setDefaultValues( this, R.xml.import_export_data, false );

        // Load user preference menu structure from XML and update view
        addPreferencesFromResource(null, R.xml.import_export_data);

        // Refresh the preference data wrapper
        mGlobalPrefs = new GlobalPrefs(this, mAppData);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void OnPreferenceScreenChange(String key)
    {
        PrefUtil.setOnPreferenceClickListener(this, ACTION_EXPORT_GAME_DATA, this);
        PrefUtil.setOnPreferenceClickListener(this, ACTION_EXPORT_CHEATS_AND_PROFILES, this);
        PrefUtil.setOnPreferenceClickListener(this, ACTION_IMPORT_GAME_DATA, this);
        PrefUtil.setOnPreferenceClickListener(this, ACTION_IMPORT_CHEATS_AND_PROFILES, this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference)
    {
        // Handle the clicks on certain menu items that aren't actually
        // preferences
        final String key = preference.getKey();

        switch (key) {
            case ACTION_EXPORT_GAME_DATA:
                startFilePicker(PICK_FILE_EXPORT_GAME_DATA_REQUEST_CODE, Intent.FLAG_GRANT_WRITE_URI_PERMISSION, false);
                break;
            case ACTION_EXPORT_CHEATS_AND_PROFILES:
                startFilePicker(PICK_FILE_EXPORT_CHEATS_AND_PROFILES_REQUEST_CODE, Intent.FLAG_GRANT_WRITE_URI_PERMISSION, false);
                break;
            case ACTION_IMPORT_GAME_DATA:
                startFilePicker(PICK_FILE_IMPORT_GAME_DATA_REQUEST_CODE, Intent.FLAG_GRANT_READ_URI_PERMISSION, true);
                break;
            case ACTION_IMPORT_CHEATS_AND_PROFILES:
                startFilePicker(PICK_FILE_IMPORT_CHEATS_AND_PROFILES_REQUEST_CODE, Intent.FLAG_GRANT_READ_URI_PERMISSION, true);
                break;
            default:
                // Let Android handle all other preference clicks
                return false;
        }

        // Tell Android that we handled the click
        return true;
    }

    private void startFilePicker(int requestCode, int permissions, boolean canViewExtStorage)
    {
        AppData appData = new AppData( this );
        Intent intent;
        if (appData.useLegacyFileBrowser) {
            intent = new Intent(this, LegacyFilePicker.class);
            intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, false );
            intent.putExtra( ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, canViewExtStorage);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(permissions);
            intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        }
        startActivityForResult( intent, requestCode );
    }

    private Uri getUri(Intent data)
    {
        AppData appData = new AppData( this );
        Uri returnValue = null;
        if (appData.useLegacyFileBrowser) {
            final Bundle extras = data.getExtras();

            if (extras != null) {
                final String searchUri = extras.getString(ActivityHelper.Keys.SEARCH_PATH);
                returnValue = Uri.parse(searchUri);
            }
        } else {
            returnValue = data.getData();
        }

        return returnValue;
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Uri fileUri = getUri(data);

            if (requestCode == PICK_FILE_EXPORT_GAME_DATA_REQUEST_CODE) {
                mCopyToSdFragment.copyToSd(new File(mAppData.gameDataDir), fileUri);
            } else if (requestCode == PICK_FILE_EXPORT_CHEATS_AND_PROFILES_REQUEST_CODE) {
                mCopyToSdFragment.copyToSd(new File(mGlobalPrefs.profilesDir), fileUri);
            } else if (requestCode == PICK_FILE_IMPORT_GAME_DATA_REQUEST_CODE) {
                mCopyFromSdFragment.copyFromSd(fileUri, new File(mAppData.gameDataDir));
            } else if (requestCode == PICK_FILE_IMPORT_CHEATS_AND_PROFILES_REQUEST_CODE) {
                mCopyFromSdFragment.copyFromSd(fileUri, new File(mGlobalPrefs.profilesDir));
            }
        }
    }
}
