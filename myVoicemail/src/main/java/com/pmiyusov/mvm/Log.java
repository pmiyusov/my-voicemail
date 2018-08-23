package com.pmiyusov.mvm;

import com.pmiyusov.conf.BuildProp;

public class Log {
    static final boolean LOG = com.pmiyusov.conf.BuildProp.DEBUG;

    public static void i(String tag, String string)  {
        android.util.Log.i("myVoicemail", string);
    }
    public static void e(String tag, String string) {
        android.util.Log.e("myVoicemail", string);
    }
    public static void d(String tag, String string) { android.util.Log.d("myVoicemail", string); }
    public static void v(String tag, String string) {
        if (LOG) android.util.Log.v("myVoicemail", string);
    }
    public static void w(String tag, String string) { android.util.Log.w("myVoicemail", string);}
}