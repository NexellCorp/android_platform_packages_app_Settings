/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * developed by swpark@nexell.co.kr
 */

package com.android.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.preference.CheckBoxPreference;

import java.util.ArrayList;

public class HDMISettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String LOG_TAG = "HDMISettings";

    private static final String HDMI_RESOLUTION_PROPERTY = "hwc.resolution";
    private static final String HDMI_MODE_PROPERTY = "hwc.hdmimode";
    private static final String HDMI_SCREEN_DOWNSIZING_PROPERTY = "hwc.screendownsizing";

    private static final String HDMI_MODE_SECONDARY = "secondary";
    private static final String HDMI_MODE_PRIMARY = "primary";

    private static final String KEY_HDMI_RESOLUTION = "hdmi_resolution";
    private static final String KEY_HDMI_SCREEN_ZOOM = "hdmi_screen_zoom";
    private static final String KEY_ENABLE_HDMI_SCREEN_DOWNSIZING = "enable_hdmi_screen_downsizing";

    private ListPreference mHDMIResolutionPreference;
    private CheckBoxPreference mEnableHDMIScreenDownsizingPreference;
    private HDMIScreenZoomPreference mHDMIScreenZoomPreference;

    private int mCurResolution;
    private int mScreenDownsizing;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.hdmi_settings);

        mHDMIResolutionPreference = (ListPreference)findPreference(KEY_HDMI_RESOLUTION);
        mEnableHDMIScreenDownsizingPreference = (CheckBoxPreference)findPreference(KEY_ENABLE_HDMI_SCREEN_DOWNSIZING);
        mHDMIScreenZoomPreference = (HDMIScreenZoomPreference)findPreference(KEY_HDMI_SCREEN_ZOOM);

        mCurResolution = getResolution();
        mScreenDownsizing = getScreenDownsizing();
        mHDMIResolutionPreference.setValue(String.valueOf(mCurResolution));
        mHDMIResolutionPreference.setOnPreferenceChangeListener(this);

        if (HDMI_MODE_PRIMARY.equals(getHDMIMode())) {
            mHDMIResolutionPreference.setEnabled(false);
            mEnableHDMIScreenDownsizingPreference.setEnabled(false);
            mHDMIScreenZoomPreference.setEnabled(true);
        } else {
            mHDMIResolutionPreference.setEnabled(true);
            mEnableHDMIScreenDownsizingPreference.setEnabled(true);
            if (mScreenDownsizing > 0)
                mEnableHDMIScreenDownsizingPreference.setChecked(true);
            else
                mEnableHDMIScreenDownsizingPreference.setChecked(false);
            mHDMIScreenZoomPreference.setEnabled(false);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_HDMI_RESOLUTION.equals(key)) {
            int value = Integer.parseInt((String)objValue);
            setResolution(value);
        }
        return true;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mEnableHDMIScreenDownsizingPreference) {
            if (mEnableHDMIScreenDownsizingPreference.isChecked()) {
                setScreenDownsizing(1);
            } else {
                setScreenDownsizing(0);
            }
        }
        return false;
    }

    private void setResolution(int resolution) {
        if (resolution != mCurResolution) {
            mCurResolution = resolution;
            SystemProperties.set(HDMI_RESOLUTION_PROPERTY, Integer.toString(mCurResolution));
            Settings.System.putInt(mHDMIResolutionPreference.getContext().getContentResolver(), HDMI_RESOLUTION_PROPERTY, mCurResolution);
        }
    }

    private void setScreenDownsizing(int downsizing) {
        if (downsizing != mScreenDownsizing) {
            Log.d(LOG_TAG, "setScreenDownsizing " + downsizing);
            mScreenDownsizing = downsizing;
            SystemProperties.set(HDMI_SCREEN_DOWNSIZING_PROPERTY, Integer.toString(mScreenDownsizing));
            Settings.System.putInt(mEnableHDMIScreenDownsizingPreference.getContext().getContentResolver(),
                    HDMI_SCREEN_DOWNSIZING_PROPERTY, mScreenDownsizing);
        }
    }

    private int getResolution() {
        String resolutionVal = SystemProperties.get(HDMI_RESOLUTION_PROPERTY, "18");
        Log.d(LOG_TAG, "hwc.resolution value: " + resolutionVal);
        return Integer.parseInt(resolutionVal);
    }

    private String getHDMIMode() {
        String hdmiMode = SystemProperties.get(HDMI_MODE_PROPERTY, "secondary");
        Log.d(LOG_TAG, "hwc.hdmimode: " + hdmiMode);
        return hdmiMode;
    }

    private int getScreenDownsizing() {
         String screenDownsizing = SystemProperties.get(HDMI_SCREEN_DOWNSIZING_PROPERTY, "0");
         Log.d(LOG_TAG, "hwc.screenDownsizing: " + screenDownsizing);
         return Integer.parseInt(screenDownsizing);
    }
}
