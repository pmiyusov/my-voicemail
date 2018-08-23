package com.pmiyusov.mvm;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.Exception;
import java.util.Date;
import java.text.SimpleDateFormat;
import android.os.SystemClock;
import android.os.Environment;
//import android.os.Binder;
import android.os.IBinder;
import android.app.Service;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.view.KeyEvent;
import android.widget.Toast;
// import android.util.Log;
import com.pmiyusov.mvm.Log;
import com.pmiyusov.mvm.R;
import com.pmiyusov.conf.BuildProp;

public class RecordService extends Service implements
		MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener {
	private static final String TAG = "myVoicemail";
	private static final int RECORDING_NOTIFICATION_ID = 1;

	public static MediaRecorder recorder = null;
	public static boolean isRecording = false;
	private static File recording = null;
	static RecordInfo lastRecord = null;
	private static String incomingNumber = null;
	int delayRing = 0;
	FileOutputStream fos = null;
	SharedPreferences sharedPrefs;
	void init(){
		recorder = null;
		isRecording = false;
		recording = null;
		lastRecord = null;
		incomingNumber = null;		
	}
	private String makeRecordFileName(SharedPreferences sharedPrefs, String number){
		String storageDirectoryPath = null;
		storageDirectoryPath = getSharedPreferences("preferences", MODE_PRIVATE).getString("storage_directory", null);
        File dir = new File(storageDirectoryPath);
        // test dir for existence and writeability
        if (!dir.exists()) {
            try {
                dir.mkdirs();
            } catch (Exception e) {
                Log.e(TAG, " Can't create directory " + dir + ": " + e);
                Toast t = Toast.makeText(getApplicationContext(), TAG + "Can't create directory  " + dir  + e, Toast.LENGTH_LONG);
                t.show();
                return null;
            }
        } else {
            if (!dir.canWrite()) {
                Log.e(TAG, "No write permission for directory: " + dir);
                Toast t = Toast.makeText(getApplicationContext(), TAG+" No write permission for the directory " + dir, Toast.LENGTH_LONG);
                t.show();
                return null;
            }
        }

		// create filename based on call data
		String prefix = number;
//		SimpleDateFormat sdf = new SimpleDateFormat("-MMddHHmm");
		SimpleDateFormat sdf = new SimpleDateFormat("-MM-dd-HH-mm-ss");
		prefix = prefix + sdf.format(new Date());

		// add info to file name about what audio channel we were recording
//		int audiosource = Integer
//				.parseInt(prefs.getString("audio_source", "1"));
//		prefix += "-" + audiosource ;

		// create suffix based on format
		String suffix = "";
		int audioformat = Integer
				.parseInt(sharedPrefs.getString("audio_format", "1"));
		switch (audioformat) {
		case MediaRecorder.OutputFormat.THREE_GPP:
			suffix = ".3gp";
			break;
		case MediaRecorder.OutputFormat.MPEG_4:
			suffix = ".mpg";
			break;
		case MediaRecorder.OutputFormat.AMR_WB:
		case MediaRecorder.OutputFormat.RAW_AMR:
			suffix = ".amr";
			break;
		case MediaRecorder.OutputFormat.AAC_ADTS :
			suffix = ".aac";
			break;
		}
		String name = prefix + suffix;
		return name;
		
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, " onCreate() ");
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if (isRecording) {
			Log.d(TAG, " onStartCommand called while isRecording");
			stopSelf(); // TODO can it be another instance?
			return;
		}
		init();
		lastRecord = new RecordInfo();
		Context c = getApplicationContext();
		sharedPrefs= this.getSharedPreferences("preferences", MODE_PRIVATE);
		// TOFIX SharedPreferences prefs = SettingsActivity.sharedPrefs
		int audiosource = Integer
				.parseInt(sharedPrefs.getString("audio_source", "1"));
		int audioformat = Integer
				.parseInt(sharedPrefs.getString("audio_format", "1"));
		String incomingNumber =  "Unknown";
		if(intent.hasExtra("extra_incoming_number"))
		incomingNumber = intent
				.getStringExtra("extra_incoming_number");
		// .getStringExtra(MyVoicemailDaemon.EXTRA_INCOMING_NUMBER); // NullPointerException
		int maxDurationMs = 1000*Integer
				.parseInt(sharedPrefs.getString("max_duration_sec", Integer.toString(com.pmiyusov.conf.BuildProp.MAX_DURATION_SEC)));
		if(com.pmiyusov.conf.BuildProp.EVALUATION){
			maxDurationMs = 1000*com.pmiyusov.conf.BuildProp.MAX_DURATION_SEC;
		}
		String fileName = makeRecordFileName(sharedPrefs, incomingNumber);
		// getFilesDir () = /data/data/com.pmiyusov.mvm/files/
//		String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + DEFAULT_STORAGE_DIRECTORY + "/" + fileName;
		String filePath = getFilesDir () + "/" + fileName;
		File file = new File(filePath);
		file.setReadable(true, false);
		file.setWritable(true, false);
		try {
			fos = new FileOutputStream(filePath);
		} catch (Exception e) {
			Log.e(TAG,
					"Error opening  output file  "
							+ filePath  + e);
			Toast t = Toast.makeText(getApplicationContext(),
					"Error opening  output file  "
							+ filePath  + e,
					Toast.LENGTH_LONG);
			t.show();
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		recorder = new MediaRecorder();
		Log.d(TAG, "onStart created new MediaRecorder object" + recorder);

		Log.d(TAG, " Configure MediaRecorder with audiosource: "
				+ audiosource + " audioformat: " + audioformat);
		try {
			Log.d(TAG, "set audiosource " + audiosource);
			recorder.setAudioSource(audiosource);
			Log.d(TAG, "set setOutputFormat " + audioformat);
			// AudioFormat.ENCODING_PCM_16BIT
			recorder.setOutputFormat(audioformat);
			Log.d(TAG, "set encoder DEFAULT");
			recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT); 
			Log.d(TAG, "set sampling rate 8000");
			recorder.setAudioSamplingRate(8000);
			recorder.setOutputFile(fos.getFD());
			recorder.setMaxDuration(maxDurationMs); 
			// recorder.setMaxFileSize(bytesMax); //1024*1024); // 1KB

			recorder.setOnInfoListener(this);
			recorder.setOnErrorListener(this);

			try {
				Log.d(TAG, " recorder.prepare()");
				recorder.prepare();
			} catch (java.io.IOException e) {
				Log.e(TAG,
						" onStart() IOException attempting recorder.prepare()\n"
								+ e);
				Toast t = Toast.makeText(getApplicationContext(),
						"Error on prepare recording: " + e,
						Toast.LENGTH_LONG);
				t.show();
				e.printStackTrace();
				return; // return 0; //START_STICKY;
			}
			Log.d(TAG, "recorder.prepare done");
			lastRecord.recordFilePath = filePath;
			lastRecord.source = audiosource;
			lastRecord.format = audioformat;
			lastRecord.timeStartMs = System.currentTimeMillis();
			lastRecord.number = incomingNumber;
			recorder.start();
			isRecording = true;
			Log.d(TAG, "recorder.start() returned");
			updateNotification(true);
		} catch (Exception e) {
			Toast t = Toast.makeText(getApplicationContext(),
					"Error when starting recording: " + e,
					Toast.LENGTH_LONG);
			t.show();

			Log.e(TAG, " onStart caught unexpected exception");
			e.printStackTrace();
		}

		return; // return 0; //return START_STICKY;
	}

	@Override
	public void onDestroy() {
		// TODO will be called after unbinded !!!
		super.onDestroy();
		Log.d(TAG, " onDestroy with recorder = " + recorder);
		if (null != recorder) {
			Log.i(TAG, " onDestroy calling recorder.release()");
			recorder.release();
			isRecording = false;
		}

		updateNotification(false);
	}

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	// public class LocalBinder extends Binder {
	// RecordService getService() {
	// return RecordService.this;
	// }
	// }

	// private final IBinder mBinder = new LocalBinder();

	// methods to handle binding the service

	@Override
	public IBinder onBind(Intent intent) {
		return null; // mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return false;
	}

	@Override
	public void onRebind(Intent intent) {
	}
	// private void updateNotification(Boolean status) {return;}
	private void updateNotification(Boolean status) {
		Context c = getApplicationContext();
		sharedPrefs= this.getSharedPreferences("preferences", MODE_PRIVATE);

		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

		if (status) {
			int icon = R.drawable.tape;
			CharSequence tickerText = "Recording Message ..."; // from "+incomingNumber;
			long when = System.currentTimeMillis();

			Notification notification = new Notification(icon, tickerText, when);

			Context context = getApplicationContext();
			CharSequence contentTitle = "Status";
			CharSequence contentText = "Recording Voice Message ";
			Intent notificationIntent = new Intent(this, RecordService.class);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					notificationIntent, 0);
			// setLatestEventInfo deprecated from API23
			// notification.setLatestEventInfo(context, contentTitle, contentText,
			//		contentIntent);
			mNotificationManager
					.notify(RECORDING_NOTIFICATION_ID, notification);
		} else {
			mNotificationManager.cancel(RECORDING_NOTIFICATION_ID);
		}
	}

	private void saveNewVoicemail(RecordInfo ri) throws Exception {
		Intent i = new Intent(this, AddVoicemailHeadlessActivity.class);
		i.putExtra("recordFilePath", ri.recordFilePath);
		i. putExtra("number", ri.number);
		i.putExtra("timeEndMs", ri.timeEndMs); 
		i.putExtra("durationSec", ri.durationSec);
		i.putExtra("format", ri.format);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(i);
	}

	// MediaRecorder.OnInfoListener
	@Override
	public void onInfo(MediaRecorder mr, int what, int extra) {
		Log.d(TAG,
				" MediaRecorder onInfo callback with what: "
						+ what + " extra: " + extra);
		if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED){
			Log.d(TAG,"MEDIA_RECORDER_INFO_MAX_DURATION_REACHED");
//			MyVoicemailDaemon.playResource(AudioManager.STREAM_VOICE_CALL, R.raw.end_message_beep, false, false);			
//			stopRecorder();
			// isRecording = false;
			MyVoicemailDaemon.getInstance().hangUpPhone(getApplicationContext());

		}
	}

	// MediaRecorder.OnErrorListener
	@Override
	public void onError(MediaRecorder mr, int what, int extra) {
		Log.e(TAG,
				" MediaRecorder onError callback with what: "
						+ what + " extra: " + extra);
		// 
		stopRecorder();
		stopSelf();
	}

	/*
The state machine of the media_recorder.
	    MEDIA_RECORDER_ERROR                 =      0,
	    // Recorder was just created.
	    MEDIA_RECORDER_IDLE                  = 1 << 0,
	    MEDIA_RECORDER_INITIALIZED           = 1 << 1,
	    MEDIA_RECORDER_DATASOURCE_CONFIGURED = 1 << 2,
	    // Recorder is ready to start.
	    MEDIA_RECORDER_PREPARED              = 1 << 3,
	    MEDIA_RECORDER_RECORDING             = 1 << 4,
*/
	
	public static void stopRecorder() {
		Log.d(TAG, "stopRecorder()");
		if (recorder == null) {
			Log.d("Recorder", "stopRecorder: null");
			isRecording = false;
			return;
		}
		try {
			if (isRecording) {
				recorder.stop();
				isRecording = false;
				lastRecord.timeEndMs = System.currentTimeMillis();
				lastRecord.durationSec = (lastRecord.timeEndMs - lastRecord.timeStartMs) / 1000;
				// don't trust emulator timer if(lastRecord.durationSec >0)
				lastRecord.valid = true;
			}
			// recorder = null;
		} catch (Exception e) {
			Log.d(TAG, "recorder.stop() Exception");
			e.printStackTrace();
			isRecording = false;
			// recorder.reset();
			if (lastRecord != null)
				if (lastRecord.recordFilePath != null)
					new File(lastRecord.recordFilePath).delete();
			// recorder = null;
		}
	}

	class RecordInfo {
		boolean valid = false;
		long timeStartMs = 0;
		long timeEndMs = 0;
		String number = null;
		String recordFilePath=null;
		int source=0;
		int format=0;
		long durationSec=0;

		RecordInfo() {
			valid = false;
			number = "";
			timeStartMs = System.currentTimeMillis();
			timeEndMs = timeStartMs;
			durationSec = 0;
			recordFilePath = null;
			source = 0;
			format = 0;
		}
	}

}
