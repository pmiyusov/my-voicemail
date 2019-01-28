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
package com.pmiyusov.mvm.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.pmiyusov.mvm.Log;
import com.pmiyusov.mvm.R;
import com.pmiyusov.mvm.conf.BuildProp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

//import android.os.Build;
//import android.os.Environment;
//import com.pmiyusov.mvm.MyVoicemailDaemon;
//import com.pmiyusov.mvm.SettingsActivity;

public class Utils {
    private static final String TAG = "Utils";

    public static void rescan(Context c) {
//		String voicemailStoreDir = Environment.getExternalStorageDirectory()
//				.getAbsolutePath() + "/" + BuildProp.DEFAULT_STORAGE_DIRECTORY;
        String voicemailStoreDir = c.getSharedPreferences("preferences", c.MODE_PRIVATE).getString("storage_directory",
                c.getFilesDir() + "/" + BuildProp.STORAGE_DIRECTORY_NAME);
        String[] filesAndDirs = {voicemailStoreDir};
        MediaScannerConnection.scanFile(c, filesAndDirs,
                new String[]{"*/*"} /* mimeType */,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        Log.d(TAG, " Media Scan Completed");

                    }
                });
    }

    public static void rescan(Context c, String[] filesAndDirs) {
        MediaScannerConnection.scanFile(c, filesAndDirs,
                new String[]{"*/*"} /* mimeType */,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        // TODO Auto-generated method stub
                        // TODO notify interested in refreshing directory views
                        Log.d(TAG, " Media Scan Completed");

                    }
                });
    }

    public static String safeSubstring(String src, int beginIndex) {
        int endIndex = src.length();
        return
                safeSubstring(src, beginIndex, endIndex);
    }

    public static String safeSubstring(String src, int beginIndex, int endIndex) {
        if (endIndex < beginIndex) {
            Log.e(TAG, "safeSubstring(): " + src + " Begin: " + beginIndex
                    + " End: " + endIndex);
            endIndex = beginIndex;
            return "";
        }
        if (beginIndex < 0) {
            Log.e(TAG, "safeSubstring(): " + src + " Begin: " + beginIndex
                    + " End: " + endIndex);
            beginIndex = 0;
            return "";
        }
        if (beginIndex > endIndex) {
            Log.e(TAG, "safeSubstring(): " + src + " Begin: " + beginIndex
                    + " End: " + endIndex);
            beginIndex = endIndex;
            return "";
        }
        if (beginIndex > src.length()) {
            Log.e(TAG, "safeSubstring(): " + src + " Begin: " + beginIndex
                    + " End: " + endIndex);
            beginIndex = src.length();
            return "";
        }
        return src.substring(beginIndex, endIndex);
    }

    public static void hangUpPhone(Context context) {
        callITelephony(context, "endCall");
    }

    public static void pickUpPhone(Context context) {
        callITelephony(context, "answerRingingCall");
    }

    public static void callPhone(Context context) {
        callITelephony(context, "call");
    }

    public static void callITelephony(Context context, String method) {
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
            Log.d(TAG,
                    "FATAL ERROR: could not connect to telephony subsystem");
            Log.d(TAG, "Exception object: " + e);
        }
    }

    /*
     * http://www.edumobile.org/android/android-development/audio-recording-in-wav-format-in-android-programming/
     */
    public static void writeWavFromPcm(Context c, String inFilename, String outFilename, int sampleRate, int bitsPerSample) {
        FileInputStream in = null;
        FileOutputStream out = null;
        InputStream inBeep = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = sampleRate;
        int channels = 1;  // TOFIX hard coded
        long byteRate = bitsPerSample * sampleRate * channels / 8;
        int bufferSize = 1024; // memory/speed trade-off  
        byte[] indata = new byte[bufferSize];
        byte[] outdata = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            inBeep = c.getResources().openRawResource(R.raw.greeting_end_beep);
            long beepLen = 0;
            int size = 0;
            // Read the entire resource into a local byte buffer.
            byte[] buffer = new byte[1024];
            while ((size = inBeep.read(buffer, 0, 1024)) >= 0) {
                beepLen += size;
            }
            inBeep.close();
            beepLen = beepLen - 44;// less headers
            totalAudioLen = in.getChannel().size();
            totalAudioLen += beepLen;
            totalDataLen = totalAudioLen + 36;

            Log.d("copyWaveFile", "File size: " + totalDataLen);

            writeWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate, bitsPerSample);
            int cnt;
            short reg0, reg;
            while ((cnt = in.read(indata, 0, indata.length)) != -1) {
                for (int i = 0; i < (cnt - 1); i += 2) {
                    reg0 = (short) (((int) indata[i + 1]) << 8 & 0xff00 | ((int) indata[i]) & 0xff);
                    // reg0 = reg0*32;   // amplify  TOFIX
                    //reg = (short)(reg0<<8 & 0xff00 | reg0>>8&0xff); // swap bytes in short
                    reg = reg0;  // (short)((reg0*4)&~0x3);  // amplify and mask lower 2 bits
                    //reg = ((int)indata[i+1])<<8 & 0xff00 | ((int)indata[i])&0xff;
                    //reg = reg*32;   // amplify  TOFIX
                    // reg = reg<<2;   // amplify

                    outdata[i] = (byte) (reg >>> 8 & 0xff);
                    outdata[i + 1] = (byte) (reg & 0xff);
//                		outdata[i] = indata[i+1];   // swap bytes in short
//                		outdata[i+1] = indata[i];
                }
                out.write(outdata, 0, cnt);
            }

            in.close();
            // append beep
            // skip headers ...
            inBeep = c.getResources().openRawResource(R.raw.greeting_end_beep);
            inBeep.read(outdata, 0, 44);
            while ((cnt = inBeep.read(outdata, 0, outdata.length)) != -1) {
                out.write(outdata, 0, cnt);
            }
            out.close();
            inBeep.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate, int bitsPerSample) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = (byte) (bitsPerSample & 0xff);  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    public static void postNotification(Context context, String title, String message, Class<?> ActivityClass) {
        Intent resultIntent;
        PendingIntent resultPendingIntent;

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(message);
        if (ActivityClass != null) {
            // Creates an explicit intent for an Activity in your app
            resultIntent = new Intent(context, ActivityClass);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                // The stack builder object will contain an artificial back stack for the
                // started Activity.
                // This ensures that navigating backward from the Activity leads out of
                // your application to the Home screen.
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                // Adds the back stack for the Intent (but not the Intent itself)
                stackBuilder.addParentStack(ActivityClass);
                // Adds the Intent that starts the Activity to the top of the stack
                stackBuilder.addNextIntent(resultIntent);
                resultPendingIntent =
                        stackBuilder.getPendingIntent(
                                0,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );
            } else {
                resultPendingIntent = PendingIntent.getService(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            mBuilder.setContentIntent(resultPendingIntent);
        }
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        int mId = 100;
        mNotificationManager.notify(mId, mBuilder.build());
    }
}
