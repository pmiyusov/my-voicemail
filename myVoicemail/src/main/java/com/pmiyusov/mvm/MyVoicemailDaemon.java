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
//import com.android.internal.telephony.*;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;

import com.pmiyusov.mvm.RecordService.RecordInfo;
import com.pmiyusov.mvm.conf.BuildProp;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Date;

// import android.util.Log;


/**
 * @author paul
 */

public class MyVoicemailDaemon extends Service {
    public static final String EXTRA_INCOMING_NUMBER = "extra_incoming_number";
    public static CallAlertDialog callAlertDialog = null;
    private static final String TAG = "MyVoicemailDaemon";
    private static TelephonyManager mTM = null;
    private static PhoneStateListener mListener;
    private static CallMonitor mCallMonitor;
    static int mState = TelephonyManager.CALL_STATE_IDLE;
    static Boolean recordCalls = false;
    static Uri greetingUri = null;
    static String alternativeGreetingFilePath = null;
    static Boolean playbackCompleted = true;
    static Boolean loopingFlag = false;
    static String lastNumber = "XXX";
    static MediaPlayer mp = null;
    static AudioTrack audioTrack = null;
    static MyVoicemailDaemon thisInstance;
    static ComponentName RecordServiceComponentName = null;
    static Thread recordServiceThread = null;
    static Boolean cancelRecording = false;
    static Boolean autoPickedUp = false;
    static Boolean userPickedUp = false;
    static Boolean outCall = false;
    static File fileToPlay = null;
    static int playStream = 0;
    PlayTrack playTask;
    static int currentGreetingStream = AudioManager.STREAM_VOICE_CALL;
    static int greetingVolume = 2;
    static boolean currentSpeakerOn = true;

    static boolean useDefaultGreeting = true;
    static boolean useAlternativeGreeting = false;
    static boolean useCustomGreeting = false;
    String greetingType = BuildProp.GREETING_TYPE_DEFAULT;
    static boolean useExternalStorage = true;
    static Context context;
    static final String SHARED_PREFS_FILE_NAME = "myvoicemail_prefs";
    static String storageDirectoryPath = null;
    static String customGreetingFilePath = null;
    static SharedPreferences sharedPrefs;

    public static MyVoicemailDaemon getInstance() {
        return thisInstance;
    }

    class CallMonitor extends Thread {
        @Override
        public void run() {
            mTM = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            if (mTM != null) {
                mTM.listen(mListener, PhoneStateListener.LISTEN_CALL_STATE);
                Log.i(TAG, " Listening for calls");
            } else {
                Log.e(TAG,
                        "TelephonyManager not initialized! Not listening for calls.");
            }
        }
    }

    /**
     * constructor
     */
    public MyVoicemailDaemon() {
        super();
        Log.d(TAG, " constructor");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, " onCreate");
        context = getApplicationContext();
        thisInstance = this;
        greetingUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.voicemail_greeting);
        alternativeGreetingFilePath = "/" + context.getPackageName() + "/" + R.raw.voicemail_alternative_greeting;
        sharedPrefs = this.getSharedPreferences("preferences", MODE_PRIVATE);


    }

    @Override
    public void onDestroy() {
        Context context = getApplicationContext();
        Boolean stopped = stopService(new Intent(context, RecordService.class));
        Log.d(TAG, "stopService for RecordService returned " + stopped);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        mListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (mState == state)
                    return; // no actual change
                Context c = getApplicationContext();
                sharedPrefs = getInstance().getSharedPreferences("preferences", MODE_PRIVATE);
                recordCalls = sharedPrefs.getBoolean("enable_voicemail", false);
                Log.d(TAG, "State transition from " + mState + " -> "
                        + state);
                Log.d(TAG, "recordCalls" + " = " + recordCalls);
                Log.d(TAG, "cancelRecording" + " = " + cancelRecording);
                Log.d(TAG, "autoPickedUp" + " = " + autoPickedUp);
                Log.d(TAG, "userPickedUp" + " = " + userPickedUp);
                Log.d(TAG, "outCall" + " = " + outCall);
                Log.d(TAG, "playbackCompleted" + " = " + playbackCompleted);
                Log.d(TAG, "RecordService.isRecording" + " = " + RecordService.isRecording); // TOFIX may crash
                if (recordCalls == false)
                    return; //voicemail disabled
                if (getSharedPreferences("preferences", MODE_PRIVATE).getString("storage_viewer", "sdcard").equals("sdcard"))
                    useExternalStorage = true;
                else
                    useExternalStorage = false;
                // useExternalStorage = sharedPrefs.getBoolean("use_external_storage", true);
                TelephonyManager tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
                switch (state) {

                    case TelephonyManager.CALL_STATE_IDLE:
                        if (mState == TelephonyManager.CALL_STATE_IDLE)
                            return;
                        if (callAlertDialog != null) {
                            callAlertDialog.dismiss();
                            callAlertDialog = null;
                        }
                        if (mState == TelephonyManager.CALL_STATE_OFFHOOK) {
                            mState = TelephonyManager.CALL_STATE_IDLE;
                            // in case greeting is playing
                            if (playbackCompleted == false) {
                                cancelRecording = true;
                                playbackCompleted = true;
                                if (mp != null)
                                    mp.release();
                                return;
                            }
                            Log.d(TAG, "CALL_STATE_IDLE, stop stopRecorder");
//						if(RecordServiceComponentName == null){
//							Log.d(TAG, "state changed to CALL_STATE_IDLE, RecordService == null");
//							return;
//						}
                            // service wil be stopped in headless onPostExecute();
                            RecordService.stopRecorder();
                            if (RecordService.lastRecord != null)
                                Log.d(TAG, "lastRecord.valid= " + RecordService.lastRecord.valid);
                            Log.d(TAG, "cancelRecording = " + cancelRecording);
                            if ((RecordService.lastRecord != null)
                                    //	&& RecordService.lastRecord.valid  // TOFIX not set when expected to be set
                                    && cancelRecording == false) {
                                RecordService.lastRecord.number = lastNumber;
                                RecordService.lastRecord.timeEndMs = new Date()
                                        .getTime();
                                try {
                                    saveNewVoicemail(RecordService.lastRecord);
                                } catch (Exception e) {
                                    Log.e(TAG, "saveNewVoicemail exception " + e);
                                    e.printStackTrace();
                                }
                            }
                        }
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mState == TelephonyManager.CALL_STATE_OFFHOOK) {
                            return; // ignore second call
                        }
                        autoPickedUp = false;
                        userPickedUp = false;
                        outCall = false;
                        cancelRecording = false;
                        mState = TelephonyManager.CALL_STATE_RINGING;
                        if ((recordCalls == false)
                                || RecordService.isRecording)
                            break;
                        Log.d(TAG, "CALL_STATE_RINGING  " + "incomingNumber: "
                                + incomingNumber);
                        if (incomingNumber != null)
                            if (!incomingNumber.isEmpty())
                                lastNumber = new String(incomingNumber);
                        try {
                            int delayRing = 1000 * Integer
                                    .parseInt(sharedPrefs.getString("delay_ring", "3"));

                            Thread.sleep(delayRing);
                        } catch (Exception e) {
                            Log.d(TAG, "Sleep interrupted");
                        }
                        if (tm.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK) { // user may pick up during sleep
                            userPickedUp = true;
                           // break;        // TODO uncomment break
                        }
                        pickupPhone(context);
                        autoPickedUp = true;

                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        Log.d(TAG, "CALL_STATE_OFFHOOK");
                        // TODO case when callee picks up while recording is in process
                        // idle -> off hook
                        if (mState == TelephonyManager.CALL_STATE_IDLE) {
                            outCall = true; // user is making call
                            cancelRecording = true;
                            mState = TelephonyManager.CALL_STATE_OFFHOOK;
                            Log.d(TAG, " Outgoing call ");
                            break;
                        }
                        // off hook -> off hook
                        if (mState != TelephonyManager.CALL_STATE_RINGING) {
                            mState = TelephonyManager.CALL_STATE_OFFHOOK;
                            break;
                        }
                        // Now process transition ringing -> off hook
                        mState = TelephonyManager.CALL_STATE_OFFHOOK;
                        if (recordCalls == false)
                            break; // voicemail not enabled, not recording
                        cancelRecording = false;
                        if (autoPickedUp == false) {
                            userPickedUp = true;
                            cancelRecording = true; // TODO support call recoding?
                            break;
                        }
                        callAlertDialog = new CallAlertDialog(getApplicationContext());
                        //callAlertDialog.show();
                        currentSpeakerOn = sharedPrefs.getBoolean("greeting_use_speakerphone", false);
                        currentGreetingStream = Integer
                                .parseInt(sharedPrefs.getString("greeting_stream", null));
                        playGreeting(currentGreetingStream, currentSpeakerOn);
                        //playGreetingAT(currentGreetingStream, currentSpeakerOn);
                        //playGreeting(AudioManager.STREAM_MUSIC, sharedPrefs.getBoolean("use_speakerphone", false));
                        // playGreeting(AudioManager.STREAM_VOICE_CALL, sharedPrefs.getBoolean("use_speakerphone", false));
                        startRecordService();
                        break;
                }
                Log.d(TAG, "onCallStateChanged  to " + mState);
            }
        };
        mCallMonitor = new CallMonitor();
        mCallMonitor.start();
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    public void hangUpPhone(Context context) {
        callITelephony(context, "endCall");
    }

    public void pickupPhone(Context context) {
        Log.d(TAG, " pickUpPhoneByTelephony");
        pickUpPhoneByTelephony(context);
//        Log.d(TAG, " pickupPhoneByHeadSetHook");
//        pickupPhoneByHeadSetHook(context);
//        Log.d(TAG, " pickupPhoneByCmdLine");
//        pickupPhoneByCmdLine(context);
    }

    public void pickUpPhoneByTelephony(Context context) {
        callITelephony(context, "answerRingingCall");
    }

    public void callITelephony(Context context, String method) {
        // required permission <uses-permission android:name="android.permission.CALL_PHONE"/>
        //telephonyCall       = telephonyClass.getMethod("call", String.class);
        //telephonyEndCall    = telephonyClass.getMethod("endCall");
        //telephonyAnswerCall = telephonyClass.getMethod("answerRingingCall");


        try {
            //String serviceManagerName = "android.os.IServiceManager";
            String serviceManagerName = "android.os.ServiceManager";
            String serviceManagerNativeName = "android.os.ServiceManagerNative";
            String telephonyName = "com.android.internal.telephony.ITelephony";

            Class telephonyClass;
            Class telephonyStubClass;
            Class serviceManagerClass;
            Class serviceManagerStubClass;
            Class serviceManagerNativeClass;
            Class serviceManagerNativeStubClass;

            Method telephonyCall;
            Method telephonyEndCall;
            Method telephonyAnswerCall;
            Method getDefault;

            Method[] temps;
            Constructor[] serviceManagerConstructor;

            // Method getService;
            Object telephonyObject;
            Object serviceManagerObject;

            telephonyClass = Class.forName(telephonyName);
            telephonyStubClass = telephonyClass.getClasses()[0];
            serviceManagerClass = Class.forName(serviceManagerName);
            serviceManagerNativeClass = Class.forName(serviceManagerNativeName);

            Method getService = // getDefaults[29];
                    serviceManagerClass.getMethod("getService", String.class);

            Method tempInterfaceMethod = serviceManagerNativeClass.getMethod(
                    "asInterface", IBinder.class);

            Binder tmpBinder = new Binder();
            tmpBinder.attachInterface(null, "fake");

            serviceManagerObject = tempInterfaceMethod.invoke(null, tmpBinder);
            IBinder retbinder = (IBinder) getService.invoke(serviceManagerObject, "phone");
            Method serviceMethod = telephonyStubClass.getMethod("asInterface", IBinder.class);

            telephonyObject = serviceMethod.invoke(null, retbinder);
            //telephonyCall = telephonyClass.getMethod("call", String.class);
            telephonyEndCall = telephonyClass.getMethod(method);
            //telephonyAnswerCall = telephonyClass.getMethod("answerRingingCall");

            telephonyEndCall.invoke(telephonyObject);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,
                    " ERROR: could not connect to telephony subsystem");
            Log.e(TAG, "Exception object: " + e);
        }
    }


    public void pickupPhoneByHeadSetHook(Context context) {
        Log.d(TAG, " pickupPhone");

        try {
            Intent buttonDown = new Intent(Intent.ACTION_MEDIA_BUTTON);
            buttonDown.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(
                    KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK));
            context.sendOrderedBroadcast(buttonDown,
                    null);
            // "android.permission.CALL_PRIVILEGED");
            Intent buttonUp = new Intent(Intent.ACTION_MEDIA_BUTTON);
            buttonUp.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(
                    KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
            context.sendOrderedBroadcast(buttonUp,
                    null);
            // "android.permission.CALL_PRIVILEGED");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,
                    " ERROR: could not send Ordered Broadcast");
            Log.e(TAG, "Exception object: " + e);
        }

    }

    public void pickupPhoneByCmdLine(Context context) {
        Log.d(TAG, " pickupPhone");
        try {
            Runtime.getRuntime().exec("input keyevent " + Integer.toString(KeyEvent.KEYCODE_HEADSETHOOK));
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, " pickupPhone exception:");
            Log.e(TAG,
                    " ERROR: could not execute command line");
            Log.e(TAG, "Exception object: " + e);
            //handle error here
        }

    }

    void playGreeting(int streamType, boolean speakerOn) {
        greetingType = getSharedPreferences("preferences", MODE_PRIVATE)
                .getString("greeting_type", BuildProp.GREETING_TYPE_DEFAULT);
        customGreetingFilePath = getSharedPreferences("preferences", MODE_PRIVATE)
                .getString("greeting_file_path", null);
        if (greetingType.equals(BuildProp.GREETING_TYPE_CUSTOM)) {
            useCustomGreeting = true;
            useAlternativeGreeting = false;
            useDefaultGreeting = false;
        } else if (greetingType.equals(BuildProp.GREETING_TYPE_ALTERNATIVE)) {
            useAlternativeGreeting = true;
            useCustomGreeting = false;
            useDefaultGreeting = false;
        } else {
            useDefaultGreeting = true;
            useAlternativeGreeting = false;
            useCustomGreeting = false;
        }
        audioSettingsGreeting(streamType, speakerOn);
        if ((useCustomGreeting) && (customGreetingFilePath != null)) {
            playFile(streamType, customGreetingFilePath, speakerOn, false);
        } else if (useAlternativeGreeting) {
            playResource(streamType, R.raw.voicemail_alternative_greeting, speakerOn, false);
            // SettingsActivity.useDefaultGreeting and also fall back if custom path bad
        } else {
            playResource(streamType, R.raw.voicemail_greeting, speakerOn, false);
        }
    }

    static void playFile(int streamType, String path, boolean speakerOn, boolean looping) {
        if (path.endsWith(".pcm"))
            MyVoicemailDaemon.getInstance().playFileAT(streamType, path, speakerOn, false);
        else {
            Uri fileUri = Uri.fromFile(new File(path));
            playUri(streamType, fileUri, speakerOn, looping);
        }
    }

    static void playResource(int streamType, int resId, boolean speakerOn, boolean looping) {
        Context c = context;
        Uri resUri = Uri.parse("android.resource://" + c.getPackageName() + "/"
                + resId);
        playUri(streamType, resUri, speakerOn, looping);
    }

    static void playUri(int streamType, Uri uri, boolean speakerOn, boolean looping) {
        playbackCompleted = false;
        loopingFlag = looping;
        // Context c = context;
        AudioManager am = (AudioManager) context
                .getSystemService(Context.AUDIO_SERVICE);
        am.setSpeakerphoneOn(speakerOn);
        mp = new MediaPlayer();
        try {
            mp.setDataSource(context, uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mp.setAudioStreamType(streamType);
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.d(TAG, "Prepare completed");
                mp.setLooping(loopingFlag);
                mp.start();
            }
        });
        mp.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playbackCompleted = true;
                Log.d(TAG, "Playback completed");
                mp.release();
            }
        });

        try {
            mp.prepare();
            // mp.start()will be called by OnPreparedListener
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Boolean startRecordService() {
        if (cancelRecording == true)
            return false;
        recordServiceThread = new Thread("RecordingThread") {
            public void run() {
                AudioManager am = (AudioManager) context
                        .getSystemService(Context.AUDIO_SERVICE);
                Log.d(TAG, "Entering sleep " + new Date().getTime());
                try {
                    // let greeting start
                    // should be in prepared state in 1000 ms
                    sleep(1000);
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                try {
                    sleep(mp.getDuration()); // let greeting play
                } catch (Exception e2) {
                    // may be released already
                    e2.printStackTrace();
                }
                Log.d(TAG, "Exited sleep " + new Date().getTime());
                //  while (am.isMusicActive()){
                while (playbackCompleted == false) {
                    Log.d("RecordingThread", " having a nap");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                // Abandon audio focus when playback complete
                am.abandonAudioFocus(afChangeListener);
                // in case it was auto restarted, we need to start it with new extras
                stopService(new Intent(context, RecordService.class));
                if (cancelRecording == true)
                    return;
                if (userPickedUp == true)  // TODO enable caall recording
                    return;
                Log.d(TAG, "CALL_STATE_OFFHOOK, start recording");
                audioSettingsRecording();
                Intent i = new Intent(context,
                        RecordService.class);
                i.putExtra(
                        com.pmiyusov.mvm.MyVoicemailDaemon.EXTRA_INCOMING_NUMBER,
                        lastNumber);
                RecordServiceComponentName = startService(i);
                if (null == RecordServiceComponentName) {
                    Log.e(TAG,
                            "startService for RecordService returned null ComponentName");
                    return;
                } else {
                    Log.d(TAG,
                            "startService returned " + RecordServiceComponentName.flattenToString());
                    return;
                }

            }
        };
        recordServiceThread.start();
        return true;
    }

    Boolean stopRecordService() {
        Intent i = new Intent(context,
                RecordService.class);
        Boolean stopped = stopService(i);
        Log.d(TAG, "stopService RecordService returned " + stopped);
        if (recordServiceThread != null)
            recordServiceThread.interrupt();
        recordServiceThread = null;
        return true;
    }

    void saveNewVoicemail(RecordInfo ri) throws Exception {
        Intent i = new Intent(this, AddVoicemailHeadlessActivity.class);
        i.putExtra("recordFilePath", ri.recordFilePath);
        i.putExtra("number", ri.number);
        i.putExtra("date", "today"); // FIXME
        i.putExtra("durationSec", ri.durationSec);
        i.putExtra("format", ri.format);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    public void audioSettingsGreeting(int streamType, boolean speakerOn) {
//		AudioGroup ag = new AudioGroup ();
//		ag.setMode(ag. MODE_NORMAL);
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int greetingAudioMode = Integer.parseInt(sharedPrefs.getString("greeting_audio_mode", "0"));
        am.setMode(greetingAudioMode);
        // am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        // am.setMode(AudioManager.MODE_IN_CALL);
        // Request audio focus for playback
        //am.getParameters(keys) // TODO what are the key?
        int result = am.requestAudioFocus(afChangeListener,
                streamType,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d(TAG, " Greeting : Audio focus not granted for stream " + streamType);
        } else {
            Log.d(TAG, " Greeting : Audio focus granted ! for stream " + streamType);
        }

        int maxVolIndex = am.getStreamMaxVolume(streamType);
        String greetingVolumestr = sharedPrefs.getString("greeting_volume", "2");
        try {
            greetingVolume = Integer.parseInt(greetingVolumestr);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing Integer " + greetingVolumestr + ". Setting 2");
            greetingVolume = 2;  // safe value
        }
        int newVolIndex = (int) (((greetingVolume * (double) maxVolIndex)) / 10.);
        if (newVolIndex < 0) newVolIndex = 0;
        if (newVolIndex > maxVolIndex) newVolIndex = maxVolIndex;
//		am.setStreamVolume (streamType, newVolIndex, 0 /*flags*/);
        am.setStreamVolume(streamType, 0, 0 /*flags*/);
        for (int i = 0; i < newVolIndex; i++)
            am.adjustStreamVolume(streamType, am.ADJUST_RAISE, 0);
        am.setSpeakerphoneOn(speakerOn);
        am.setMicrophoneMute(false);
        printAudioSettings(streamType);
    }

    //	public static void audioSettingsPlayback(MediaPlayer mp) {
//		// this is from UI
//		AudioManager am = (AudioManager) context
//				.getSystemService(Context.AUDIO_SERVICE);
//		int streamType = AudioManager.STREAM_MUSIC; //  
//		int index = am.getStreamMaxVolume(streamType);
//		am.setStreamVolume(streamType, index, 0 /* flags */);
//		mp.setAudioStreamType(streamType);
//		am.setSpeakerphoneOn(true);
//		printAudioSettings(streamType);
//	}
//
    public void audioSettingsRecording() {
        int streamType = AudioManager.STREAM_VOICE_CALL;
        AudioManager am = (AudioManager) getBaseContext()
                .getSystemService(Context.AUDIO_SERVICE);
        int recorderAudioMode = Integer.parseInt(sharedPrefs.getString("recorder_audio_mode", "0"));
        am.setMode(recorderAudioMode);
        int index = am.getStreamMaxVolume(streamType);
        am.setStreamVolume(streamType, index, 0 /* flags */);
        // Request audio focus for playback
        int result = am.requestAudioFocus(afChangeListener,
                streamType,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d(TAG, " Recording : Audio focus not granted for stream " + streamType);
        } else {
            Log.d(TAG, " Recording : Audio focus granted ! for stream " + streamType);
        }
        // am.setSpeakerphoneOn(false); // speaker off while recording
        am.setSpeakerphoneOn(sharedPrefs.getBoolean("recording_use_speakerphone", false));
        printAudioSettings(streamType);
    }

    public static void printAudioSettings(int streamType) {
        AudioManager am = (AudioManager) context
                .getSystemService(Context.AUDIO_SERVICE);
        int mode = am.getMode();
        String modeString = null;
        Log.d(TAG, "AUDIO SETTINGS");
        if (mode == AudioManager.MODE_NORMAL)
            modeString = "MODE_NORMAL";
        else if (mode == AudioManager.MODE_RINGTONE)
            modeString = "MODE_RINGTONE";
        else if (mode == AudioManager.MODE_IN_CALL)
            modeString = "MODE_IN_CALL";
        else if (mode == AudioManager.MODE_IN_COMMUNICATION)
            modeString = "MODE_IN_COMMUNICATION";
        else
            modeString = "Unknown mode " + new Integer(mode).toString();

//		Log.d(TAG,
//				"Sample Rate " + am.getProperty("PROPERTY_OUTPUT_SAMPLE_RATE"));
//		E/AndroidRuntime(19950): java.lang.NoSuchMethodError: android.media.AudioManager.getProperty
        Log.d(TAG, "Stream  " + streamType);
        Log.d(TAG, "Stream Volume " + am.getStreamVolume(streamType));
        Log.d(TAG, "AudioMgrMode " + modeString);
    }

    static OnAudioFocusChangeListener afChangeListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            String change = null;
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                // Pause playback
                change = "AUDIOFOCUS_LOSS_TRANSIENT";
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // Resume playback
                change = "AUDIOFOCUS_GAIN";
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                // Lower the volume
                change = "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK";
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                // am.abandonAudioFocus(afChangeListener);
                // Stop playback
                change = "AUDIOFOCUS_LOSS";
            } else
                change = new Integer(focusChange).toString();
            Log.d(TAG, "focusChange = " + change);

        }
    };

    void playGreetingAT(int streamType, boolean speakerOn) {
        playResourceAT(streamType, R.raw.voicemail_greeting, speakerOn, false);
    }

    void playResourceAT(int streamType, int resId, boolean speakerOn, boolean looping) {
        playbackCompleted = false;
        InputStream is;
        try {
            is = getResources().openRawResource(resId);
            playTask = new PlayTrack(speakerOn);
            playTask.execute(is, streamType);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void playFileAT(int streamType, String pathString, boolean speakerOn, boolean looping) {

        playbackCompleted = false;
        FileInputStream is;
        try {
            is = new FileInputStream(new File(pathString));
            playTask = new PlayTrack(streamType, pathString, speakerOn, looping);
            playTask.execute(is, streamType);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //	public void setGreetingVolume(double Vol){
//		// Vol ={0., ... 1.}
//		greetingVolume =Vol;
//		AudioManager am = (AudioManager) context
//				.getSystemService(Context.AUDIO_SERVICE);
//		int streamType = AudioManager.STREAM_MUSIC; 
//		int maxVolIndex = am.getStreamMaxVolume(streamType);
//		int newVolIndex = (int)(Vol*(double)maxVolIndex);
//		am.setStreamVolume (streamType, newVolIndex, 0 /*flags*/);
//		am.setSpeakerphoneOn(sharedPrefs.getBoolean("use_speakerphone", false));
//
//	}
    public double getGreetingVolume() {
        return greetingVolume;
    }

}

class PlayTrack extends AsyncTask<Object, Integer, Integer> {
    // supported are files	wav 16bit with one data chunk
    private int mSampleRate;
    private int mChannels;
    private int nFistChunkLengthBytes;
    int _streamType;
    String _pathString;
    boolean _speakerOn;
    boolean _looping;

    PlayTrack(int streamType, String pathString, boolean speakerOn, boolean looping) {
        _streamType = streamType;
        _pathString = pathString;
        _speakerOn = speakerOn;
        _looping = looping;

    }

    PlayTrack(boolean speakerOn) {
        _speakerOn = speakerOn;
    }

    @Override
    protected void onPostExecute(Integer result) {
        // TODO Auto-generated method stub
        super.onPostExecute(result);
        if (result.intValue() == 0) { // AudioTrack finished
            MyVoicemailDaemon.playbackCompleted = true;
            if (MyVoicemailDaemon.audioTrack != null)
                MyVoicemailDaemon.audioTrack.release();
        } else if (result.intValue() == 1) { // MediaPlayer finished IS IT ACYNC ?/?
            if (MyVoicemailDaemon.mp != null)
                MyVoicemailDaemon.mp.release();
        }
    }

    @Override
    protected void onPreExecute() {
        // TODO Auto-generated method stub
        super.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        // TODO Auto-generated method stub
        super.onProgressUpdate(values);
    }

    int frequency = 8000;
    int channelConfiguration = AudioFormat.CHANNEL_OUT_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    static boolean isPlaying = false;

    static public boolean isPlayingTrack() {
        return isPlaying;
    }

    static public boolean stopPlayingTrack() {
        return isPlaying = false;
    }

    @Override
    protected Integer doInBackground(Object... params) {
//		protected Void doPlay(InputStream  is, int playStreamType) {
        InputStream is = (InputStream) params[0];
        int playStreamType = (Integer) params[1];
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
        isPlaying = true;
        MyVoicemailDaemon.audioTrack = null;
        int bufferSize = AudioTrack.getMinBufferSize(frequency,
                channelConfiguration, audioEncoding);
        short[] audiodata = new short[bufferSize / 4];

        try {
            DataInputStream dis = new DataInputStream(
                    new BufferedInputStream(is));
            // skip .wav headers, seek to data
//				for(int i = 0;i<44;i++)
//					dis.readByte();
            readWavHeaders(dis);
            frequency = mSampleRate;
            if (mChannels == 1)
                channelConfiguration = AudioFormat.CHANNEL_OUT_MONO;
            else if (mChannels == 2)
                channelConfiguration = AudioFormat.CHANNEL_OUT_STEREO;
            else {
                Log.d("PlayTrack", "number of channels " + mChannels + " Not playing!");
                return null;
            }
            // if first chunk is too short, this is multi-frame file - not supported with AudioTrack
            // trry using playUri withMmediiaPlayer
            if (nFistChunkLengthBytes < mSampleRate * 2 / 10) {
                Log.d("PlayTrack", "nFistChunkLengthBytes " + nFistChunkLengthBytes + " Not playing!");
                Log.d("PlayTrack", "multi-frame file - not supported with AudioTrack");
                return Integer.valueOf(1);

            }

            MyVoicemailDaemon.audioTrack = new AudioTrack(
                    playStreamType, frequency,
                    channelConfiguration, audioEncoding, bufferSize,
                    AudioTrack.MODE_STREAM);
//				float maxVolume = audioTrack.getMaxVolume ();
//				audioTrack.setStereoVolume(maxVolume, maxVolume);
            MyVoicemailDaemon.getInstance().audioSettingsGreeting(playStreamType, false);
            MyVoicemailDaemon.audioTrack.play();

            while (isPlaying && dis.available() > 0) {
                int i = 0;
                while (dis.available() > 0 && i < audiodata.length) {
                    audiodata[i] = swap(dis.readShort());
                    i++;
                }
                MyVoicemailDaemon.audioTrack.write(audiodata, 0, audiodata.length);
            }

            dis.close();
        } catch (Throwable t) {
            Log.e("AudioTrack", "Playback Failed");
            MyVoicemailDaemon.playbackCompleted = true;
            t.printStackTrace();
            return Integer.valueOf(-1);
        }
        return Integer.valueOf(0);
    }

    public short swap(short value) {
        int b1 = value & 0xff;
        int b2 = (value >> 8) & 0xff;

        return (short) (b1 << 8 | b2 << 0);
    }
		/*
		Read Wav header and first fmt chunk
		Get number of channels and sample rate

		*/

    int readWavHeaders(DataInputStream stream) throws IOException {
        byte[] header = new byte[12];
        stream.read(header, 0, 12);
        if (header[0] != 'R' ||
                header[1] != 'I' ||
                header[2] != 'F' ||
                header[3] != 'F' ||
                header[8] != 'W' ||
                header[9] != 'A' ||
                header[10] != 'V' ||
                header[11] != 'E') {
            throw new java.io.IOException("Not a WAV file");
        }

        mChannels = 0;
        mSampleRate = 0;
        byte[] chunkHeader = new byte[8];
        stream.read(chunkHeader, 0, 8);
        int chunkLen =
                ((0xff & chunkHeader[7]) << 24) |
                        ((0xff & chunkHeader[6]) << 16) |
                        ((0xff & chunkHeader[5]) << 8) |
                        ((0xff & chunkHeader[4]));

        if (chunkHeader[0] == 'f' &&
                chunkHeader[1] == 'm' &&
                chunkHeader[2] == 't' &&
                chunkHeader[3] == ' ') {
            if (chunkLen < 16 || chunkLen > 1024) {
                throw new java.io.IOException(
                        "WAV file has bad fmt chunk");
            }

            byte[] fmt = new byte[chunkLen];
            stream.read(fmt, 0, chunkLen);

            int format =
                    ((0xff & fmt[1]) << 8) |
                            ((0xff & fmt[0]));
            mChannels =
                    ((0xff & fmt[3]) << 8) |
                            ((0xff & fmt[2]));
            mSampleRate =
                    ((0xff & fmt[7]) << 24) |
                            ((0xff & fmt[6]) << 16) |
                            ((0xff & fmt[5]) << 8) |
                            ((0xff & fmt[4]));

            if (format != 1) {
                throw new java.io.IOException(
                        "Unsupported WAV file encoding");
            }

        }
        // read first 8 bytes of first data chunk
        stream.read(chunkHeader, 0, 8);
        if (chunkHeader[0] == 'd' &&
                chunkHeader[1] == 'a' &&
                chunkHeader[2] == 't' &&
                chunkHeader[3] == 'a') {
            if (mChannels == 0 || mSampleRate == 0) {
                throw new java.io.IOException(
                        "Bad WAV file: data chunk before fmt chunk");
            }
        }
        nFistChunkLengthBytes =
                ((0xff & chunkHeader[7]) << 24) |
                        ((0xff & chunkHeader[6]) << 16) |
                        ((0xff & chunkHeader[5]) << 8) |
                        ((0xff & chunkHeader[4]));


        return 0;

    }
}
