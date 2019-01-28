/*
 * Copyright (C) 2019 Paul Miyusov
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
package com.pmiyusov.mvm;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.TextView;
import android.widget.Toast;

import com.pmiyusov.mvm.conf.BuildProp;
import com.pmiyusov.purchaseupgrade.IaUpgradeHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

// import android.util.Log;
// import com.pmiyusov.mvm.R;

public class MyVoicemail extends Activity implements OnKeyListener {

    private static final String TAG = MyVoicemail.class.getName();
//	private static final int PERMISSION_AUDIO_REQUEST = 0;
//	private static final int PERMISSION_PHONE_REQUEST = 1;
//	private static final int PERMISSION_BT_REQUEST = 2;

    public static final int MULTIPLE_PERMISSIONS = 10; // some code

    String[] permissions = new String[]{
// As of API level 23, the following permissions are classified as PROTECTION_NORMAL:
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.BROADCAST_STICKY,
// android.permission.INJECT_EVENTS
            // TODO Manifest.permission.INJECT_EVENTS,

// Dangerous permissions
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.ADD_VOICEMAIL,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE

    };


    private TextView statusText;
    //	private static final String TAG = "MainActivity";
    static final private int MENU_SETTINGS = Menu.FIRST;
    static final private int MENU_HELP = Menu.FIRST + 1;
    static final private int MENU_CLOSE = Menu.FIRST + 2;
    private static final int SHOW_PREFERENCES = 1;
    SharedPreferences sharedPrefs;
    Bundle prefsBundle;
    BuildProp buildProp;
    private Object dialogLock = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buildProp = new BuildProp(getApplicationContext());
        statusText = (TextView) this.findViewById(R.id.StatusTextView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.add(0, MENU_SETTINGS, Menu.NONE, R.string.menu_settings);
        menu.add(0, MENU_HELP, Menu.NONE, R.string.menu_item_help);
        menu.add(0, MENU_CLOSE, Menu.NONE, R.string.menu_item_close);
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, " onStart");
        if (buildProp.isDeviceSupported() == false) {
            synchronized (dialogLock) {
                new Thread() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        uninstallDialog();
                        Looper.loop();
                    }
                }.start();
                try {
                    dialogLock.wait();
                } catch (InterruptedException e) {
                    Log.d(TAG, "Caught InterruptedException");
                    //uninstallDialog done, continue
                }
            }
        }
        android.util.Log.d(TAG, "Checking RecordPermission");
        if (checkPermissions())
            //  permissions  granted.
//
//        if (!isRecordPermissionGranted()) {
//            android.util.Log.d(TAG, "Requesting RecordPermission");
//            requestRecordPermission();
//        }
//        if (!isRecordPermissionGranted()) {
//            android.util.Log.d(TAG, "RecordPermission not granted. Exiting");
//            finish();
//        }
//
            adjustPrefs();
        turnOnDaemon();
        Class<SettingsActivity> cl = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ? SettingsActivity.class
                : SettingsActivity.class;
        Intent i = new Intent(this, cl);
        startActivity(i);
        // startActivityForResult(i, SHOW_PREFERENCES);
        finish();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int itemId = item.getItemId();
        switch (itemId) {

            case (MENU_SETTINGS): {
                Log.d(TAG, "onOptionsItemSelected MENU_SETTINGS");

                Class<SettingsActivity> c = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ? SettingsActivity.class
                        : SettingsActivity.class;
                Intent i = new Intent(this, c);

                startActivityForResult(i, SHOW_PREFERENCES);
                return true;
            }
            case (MENU_HELP): {
                Log.d(TAG, "onOptionsItemSelected MENU_HELP");
                return true;
            }
            case (MENU_CLOSE): {
                Log.d(TAG, "onOptionsItemSelected MENU_CLOSE");
                finishActivity(SHOW_PREFERENCES);
                return true;
            }
            default: {
                Log.d(TAG, "Fall to MENU_SETTINGS");
                Class<SettingsActivity> c = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ? SettingsActivity.class
                        : SettingsActivity.class;
                Intent i = new Intent(this, c);
                startActivityForResult(i, SHOW_PREFERENCES);
                return true;
            }
        }
    }

    public void turnOnDaemon() {
        Log.d(TAG, "turnOnDaemon");
        Intent i = new Intent(this, com.pmiyusov.mvm.MyVoicemailDaemon.class);
        // i.putExtras(context);
        startService(i);
    }

    public void turnOffDaemon() {
        Log.d(TAG, "turnOffDaemon");
        stopService(new Intent(this, com.pmiyusov.mvm.MyVoicemailDaemon.class));
    }

    void main() {
        Log.d(TAG, " main");
        turnOnDaemon();

    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_O:
                broadcastKeyEvent(v, KeyEvent.KEYCODE_MENU);
                return true;
            case KeyEvent.KEYCODE_H:
                broadcastKeyEvent(v, KeyEvent.KEYCODE_HOME);
                return true;
            case KeyEvent.KEYCODE_BACK:
                broadcastKeyEvent(v, KeyEvent.KEYCODE_BACK);
                return true;
        }
        return false;
    }

    void broadcastKeyEvent(View v, int keyCode) {
        Context context = v.getContext();
        Log.d(TAG, " KeyEvent keyCode" + keyCode);
        // Simulate a press of button
        Intent buttonDown = new Intent(Intent.ACTION_MEDIA_BUTTON);

        buttonDown.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(
                KeyEvent.ACTION_DOWN, keyCode));

        context.sendOrderedBroadcast(buttonDown,
                null);
        // "android.permission.CALL_PRIVILEGED");

        Intent buttonUp = new Intent(Intent.ACTION_MEDIA_BUTTON);

        buttonUp.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(
                KeyEvent.ACTION_UP, keyCode));
        context.sendOrderedBroadcast(buttonUp,
                null);
        // "android.permission.CALL_PRIVILEGED");
/*
receiverPermission	String: (optional) String naming a permission that a receiver must hold in order to receive your broadcast. If null, no permission is required.
*/
    }

    void adjustPrefs() {
        Context c = getApplicationContext();
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(c);
        sharedPrefs = this.getSharedPreferences("preferences", MODE_PRIVATE);
        boolean firstRun = defaultPrefs.getBoolean("first_run", true);
        if (firstRun == true) {
            // now it is first run - "first_run" not used anywhere yet
            PreferenceManager.setDefaultValues(c, buildProp.PREFS_RESOURCE_ID, true);
            defaultPrefs.edit().putBoolean("first_run", false).apply();

        }
        if (buildProp == null)
            buildProp = new BuildProp(getApplicationContext());
        int versionCode = defaultPrefs.getInt("version_code", 0);
        if (versionCode != buildProp.VERSION_CODE) {
            // Update
            defaultPrefs.edit().clear().apply();
            PreferenceManager.setDefaultValues(c, buildProp.PREFS_RESOURCE_ID, true);
            defaultPrefs.edit().putInt("version_code", buildProp.VERSION_CODE).apply();
        }
        String state = Environment.getExternalStorageState();
        String storageDirectoryPath = null;
        if ((Environment.MEDIA_MOUNTED.equals(state) &&
                !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))) {
            storageDirectoryPath =
                    Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + buildProp.STORAGE_DIRECTORY_NAME;
        } else {
            storageDirectoryPath = getFilesDir() + "/" + buildProp.STORAGE_DIRECTORY_NAME;   // TOFIX HARDCODED
        }
        defaultPrefs.edit().putString("storage_directory", storageDirectoryPath).apply();
//		if(buildProp.EVALUATION){
        if (IaUpgradeHelper.isPremium()) {
            defaultPrefs.edit().putString("max_duration_sec", Integer.toString(buildProp.MAX_DURATION_SEC)).apply();
        }
        // copy default prefs to working copy
        Map<String, ?> map = defaultPrefs.getAll();
        for (Entry<String, ?> pair : map.entrySet()) {
            if (pair.getValue() instanceof Boolean) {
                boolean bv = (Boolean) pair.getValue();
                sharedPrefs.edit().putBoolean(pair.getKey(), bv).apply();
            }
            if (pair.getValue() instanceof String) {
                String sv = (String) pair.getValue();
                sharedPrefs.edit().putString(pair.getKey(), sv).apply();
            }
            if (pair.getValue() instanceof Integer) {
                Integer iv = (Integer) pair.getValue();
                sharedPrefs.edit().putInt(pair.getKey(), iv).apply();
            }
        }
        sharedPrefs.edit().commit();
        defaultPrefs.edit().commit();

    }

    void uninstallDialog() {
        synchronized (dialogLock) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

            // set title
            alertDialogBuilder.setTitle("myVoicemail");

            // set dialog message
            alertDialogBuilder
                    .setMessage("Your device\nis not supported. \nUninstall myVoicemail?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            Uri packageURI = Uri.parse("package:" + getPackageName());
                            // Uri packageURI = Uri.parse("package:"+MyMainActivity.class.getPackage().getName());
                            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                            startActivity(uninstallIntent);
                            // dialogLock.notifyAll();
                        }
                    })
                    .setNegativeButton("No, try anyway", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // just close the dialog box and do nothing
                            dialog.dismiss();
                            synchronized (dialogLock) {
                                dialogLock.notifyAll();
                            }
                        }
                    });

            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();

            // show it
            alertDialog.show();
        }
    }

//    private boolean isRecordPermissionGranted() {
//        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
//                PackageManager.PERMISSION_GRANTED);
//    }
//
//
//    private void requestRecordPermission() {
//        ActivityCompat.requestPermissions(
//                this,
//                new String[]{Manifest.permission.RECORD_AUDIO},
//                PERMISSION_AUDIO_REQUEST.ordinal());
//    }
///////////////////////////////


    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        String permissionsDenied = "";
        int i;
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0) {
                    //for (String per : permissions) {
                    for (i = 0; i < grantResults.length; i++)
                        if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            permissionsDenied += "\n" + permissions[i];

                        }

                }
                // Show permissionsDenied
                if (!Objects.equals(permissionsDenied, "")) {
                    Toast.makeText(this, "Permissions Denied: \n" + permissionsDenied, Toast.LENGTH_LONG)
                            .show();
                    Toast.makeText(this, "Go to settings and enable permissions", Toast.LENGTH_LONG)
                            .show();
                    //updateViews();
                }
            }
            return;
        }
    }
///////////////////////////////
}

