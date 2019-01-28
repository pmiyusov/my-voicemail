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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.WindowManager;

import com.pmiyusov.mvm.utils.Utils;

/**
 * @author paul
 */
public class CallAlertDialog extends AlertDialog {
    private static final String TAG = "CallAlertDialog";
    Context c;
    AlertDialog alert;

    /**
     * @param context
     */
    public CallAlertDialog(Context context) {
        super(context);
        c = context;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.app_name);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setMessage(R.string.incoming_call);
        builder.setNegativeButton(R.string.hang_up, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                hangUpHandler(c);
                dialog.dismiss();
            }
        });
        builder.setNeutralButton(R.string.keep_recording, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(R.string.take_this_call, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                pickUpHandler(c);
                dialog.dismiss();
            }
        });

        alert = builder.create();
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alert.show();
    }

    @Override
    public void dismiss() {
        alert.dismiss();
        super.dismiss();
    }

    void hangUpHandler(Context context) {
        Utils.hangUpPhone(context);
        try {
            MyVoicemailDaemon.cancelRecording = true;
            if (MyVoicemailDaemon.mp != null) {
                MyVoicemailDaemon.mp.release();
                //greetingIsPlaying = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // RecordService may not have started if greeting still playing
        Boolean stopped = context.stopService(new Intent(context, RecordService.class));
        Log.d(TAG, "hangUpHandler: stopService for RecordService returned " + stopped);

    }

    void pickUpHandler(Context context) {
        try {
            MyVoicemailDaemon.cancelRecording = true;
            if (MyVoicemailDaemon.mp != null) {
                MyVoicemailDaemon.mp.release();
                //greetingIsPlaying = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // RecordService may not have started if greeting still playing
        Boolean stopped = context.stopService(new Intent(context, RecordService.class));
        Log.d(TAG, "pickUpHandler: stopService for RecordService returned " + stopped);
        //Utils.pickUpPhone(context);
    }

    /**
     * @param context
     * @param theme
     */
    public CallAlertDialog(Context context, int theme) {
        super(context, theme);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param context
     * @param cancelable
     * @param cancelListener
     */
    public CallAlertDialog(Context context, boolean cancelable,
                           OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        // TODO Auto-generated constructor stub
    }

}
