/*
 * Copyright (C) 2008 The Android Open Source Project
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
 * Developed by swpark@nexell.co.kr
 */

package com.android.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.SeekBarDialogPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.AttributeSet;
import android.view.View;
import android.view.Display;
import android.view.IWindowManager;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.util.Log;
import android.graphics.Point;

public class HDMIScreenZoomPreference extends SeekBarDialogPreference implements SeekBar.OnSeekBarChangeListener {
    private static final String LOG_TAG = "HDMIScreenZoomPreference";

    private SeekBar mSeekBar;

    private int mOldZoom;
    private int mCurZoom = -1;

    private int mWidth = 1920;
    private int mHeight = 1080;

    private IWindowManager mWm;

    private static final int SEEK_BAR_RANGE = 10;
    private static final String HDMI_SCALE_PROPERTY = "hwc.scale";

    public HDMIScreenZoomPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mWm = IWindowManager.Stub.asInterface(ServiceManager.checkService(
                        Context.WINDOW_SERVICE));
        if (mWm == null) {
            //System.err.println(NO_SYSTEM_ERROR_CODE);
            //throw new AndroidException("Can't connect to window manager; is the system running?");
            Log.e(LOG_TAG, "Can't get WindowManager Handle!!!");
        }

        setDialogLayoutResource(R.layout.preference_dialog_hdmi_screen_zoom);
        // TODO : get icon
        setDialogIcon(R.drawable.ic_settings_display);


        Point initialSize = new Point();
        Point baseSize = new Point();
        try {
            mWm.getInitialDisplaySize(Display.DEFAULT_DISPLAY, initialSize);
            mWm.getBaseDisplaySize(Display.DEFAULT_DISPLAY, baseSize);
            System.out.println("Physical size: " + initialSize.x + "x" + initialSize.y);
            if (!initialSize.equals(baseSize)) {
                System.out.println("Override size: " + baseSize.x + "x" + baseSize.y);
            }
            mWidth = initialSize.x;
            mHeight = initialSize.y;
            Log.d("HDMIScreenZoom", "Resolution ===> " + mWidth + "x" + mHeight);
        } catch (RemoteException e) {
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mSeekBar = getSeekBar(view);
        mSeekBar.setMax(SEEK_BAR_RANGE);
        mOldZoom = getZoom();
        mSeekBar.setProgress(mOldZoom);

        mSeekBar.setEnabled(true);
        mSeekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        Log.d(LOG_TAG, "Dialog close...");
        //if (!positiveResult) {
            //setZoom(mOldZoom);
        //}
        if (positiveResult)
            setZoom(mCurZoom);
        else
            mCurZoom = mOldZoom;
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        //setZom(progress);
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        // NA
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        // NA
        //setZoom(seekBar.getProgress());
        int progress = seekBar.getProgress();
        if (progress != mCurZoom)
            mCurZoom = progress;
    }

    private void setZoom(int zoom) {
        //if (zoom != mCurZoom) {
            //mCurZoom = zoom;
            SystemProperties.set(HDMI_SCALE_PROPERTY, Integer.toString(mCurZoom));
            Settings.System.putInt(getContext().getContentResolver(), HDMI_SCALE_PROPERTY, mCurZoom);
            // this is test code
            try {
                int scaledWidth;
                int scaledHeight;
                scaledWidth = mWidth - (mWidth/(SEEK_BAR_RANGE*5))*(SEEK_BAR_RANGE - mCurZoom);
                if ((scaledWidth % 2) != 0)
                    scaledWidth++;
                scaledHeight = mHeight - (mHeight/(SEEK_BAR_RANGE*5))*(SEEK_BAR_RANGE - mCurZoom);
                if ((scaledHeight % 2) != 0)
                    scaledHeight++;
                Log.d(LOG_TAG, "scaled Resolution: " + scaledWidth + "x" + scaledHeight);
                mWm.setForcedDisplaySize(0, scaledWidth, scaledHeight);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Failed to setForcedDisplaySize!!!");
            }
            Log.d(LOG_TAG, "hwc.scale set: " + mCurZoom);
        //}
    }

    private int getZoom() {
        String scaleVal = SystemProperties.get(HDMI_SCALE_PROPERTY, "3");
        Log.d(LOG_TAG, "hwc.scale value: " + scaleVal);
        return Integer.parseInt(scaleVal);
    }
}
