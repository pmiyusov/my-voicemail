package com.pmiyusov.mvm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.AutomaticGainControl;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.pmiyusov.mvm.R;
import com.pmiyusov.mvm.SettingsActivity.*;
import com.pmiyusov.mvm.utils.*;

public class RecordGreetingActivity extends Activity implements OnClickListener {

	RecordAudio recordTask;
	PlayAudio playTask;
	static long timeStartMs = 0;
	static long timeCurrentMs = 0;
	Button recordingButton,  playbackButton, saveXButton, saveToFileButton, doneButton;
	TextView statusText;
	TextView greetingFileName;
	File recordingFile;
	String rawPcmPath;
	String newGreetingPath;
	boolean isRecording = false;
	boolean isPlaying = false;

	int frequency = 8000;
	// int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	int bitsPerSample = 16;
	String  recordingiDirPath;
	File recordingiDir;				;
	String  greetingDirPath;
	String appDataDir;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_record_greeting);

		statusText = (TextView) this.findViewById(R.id.StatusTextView);
		greetingFileName = (TextView) this.findViewById(R.id.GreetingFileName);
		greetingFileName.setVisibility(View.INVISIBLE);
		recordingButton = (Button) this
				.findViewById(R.id.RecordingButton);
		playbackButton = (Button) this
				.findViewById(R.id.PlaybackButton);
		saveXButton = (Button) this
				.findViewById(R.id.SaveXButton);
		saveToFileButton = (Button) this
				.findViewById(R.id.SaveToFileButton);
		doneButton = (Button) this
				.findViewById(R.id.DoneButton);

		recordingButton.setOnClickListener(this);
		playbackButton.setOnClickListener(this);
		saveXButton.setOnClickListener(this);
		saveToFileButton.setOnClickListener(this);
		doneButton.setOnClickListener(this);
		
		recordingButton.setEnabled(true);
		playbackButton.setEnabled(false);
		saveXButton.setEnabled(false);
		saveToFileButton.setEnabled(false);
		
		saveToFileButton.setVisibility(View.GONE);
		doneButton.setEnabled(true);
		
		recordingiDirPath = System.getProperty ("java.io.tmpdir");
		recordingiDir = new File(recordingiDirPath);
		recordingiDir.mkdirs();
		PackageManager m = getPackageManager();
		String s = getPackageName();
		try {
		    PackageInfo p = m.getPackageInfo(s, 0);
		    appDataDir = p.applicationInfo.dataDir;
		} catch (NameNotFoundException e) {
		    Log.e("myVoicemail", "Error Package name not found ", e);
		}
		// greetingDirPath = appDataDir + "/greetings/";
		greetingDirPath = getSharedPreferences("preferences", MODE_PRIVATE).getString("storage_directory", null)
				+ "/greetings/";
		File greetingDir = 		new File(greetingDirPath);
		greetingDir.mkdirs();
	}

	public void onClick(View v) {
		if (v == recordingButton) {
			recordingButtonPressed();
		} else if (v == playbackButton) {
			playbackButtonPressed();
		} else if (v == saveXButton) {
			saveXButtonPressed();
		} else if (v == saveToFileButton) {
			saveToFileButtonPressed();
		} else if (v == doneButton) {
			doneButtonPressed();
		}
	}
	public void recordingButtonPressed(){
		if(isRecording){  // stop
			isRecording = false;
			recordingButton.setText("Start Recording");
			playbackButton.setText("Start Playback");
			playbackButton.setEnabled(true);
			recordingButton.setEnabled(true);
			doneButton.setEnabled(true);

		}
		else {  // start
			recordingButton.setText("Stop Recording");
			recordingButton.setEnabled(true);
			playbackButton.setEnabled(false);
			doneButton.setEnabled(false);
			recordTask = new RecordAudio();
			recordTask.execute();
			
		}
	}
	public void playbackButtonPressed(){
		if(isPlaying){  // stop
			isPlaying = false;
			playbackButton.setText("Start Playback");
			playbackButton.setEnabled(true);
			recordingButton.setEnabled(true);
			doneButton.setEnabled(true);
		}
		else {  // start
			playbackButton.setText("Stop Playback");
			playbackButton.setEnabled(true);
			recordingButton.setEnabled(false);
			doneButton.setEnabled(false);

			playTask = new PlayAudio();
			playTask.execute();
			
		}
	}
	
	private class PlayAudio extends AsyncTask<Void, Integer, String> {
		AudioTrack audioTrack = null;
		@Override
		protected		
		 void onPreExecute (){
			audioTrack = null;			 
		 }
		@Override
		protected String doInBackground(Void... params) {
			isPlaying = true;

			int bufferSize = AudioTrack.getMinBufferSize(frequency,
					AudioFormat.CHANNEL_OUT_MONO, audioEncoding);
			short[] audiodata = new short[bufferSize / 4];

			try {
				DataInputStream dis = new DataInputStream(
						new BufferedInputStream(new FileInputStream(
								recordingFile)));

				audioTrack = new AudioTrack(
						AudioManager.STREAM_MUSIC, frequency,
						AudioFormat.CHANNEL_OUT_MONO, audioEncoding, bufferSize,
						AudioTrack.MODE_STREAM);

				audioTrack.play();
				int secs=0, secsOld =0;
				int totalSamples = 0;
				timeStartMs = System.currentTimeMillis ();
				timeCurrentMs = timeStartMs;
				
				while (isPlaying && dis.available() > 0) {
					int i = 0;
					while (dis.available() > 0 && i < audiodata.length) {
						audiodata[i] = dis.readShort();
						i++;
					}
					audioTrack.write(audiodata, 0, audiodata.length);
					totalSamples += i;
					timeCurrentMs = System.currentTimeMillis ();					
					secs = (int)((timeCurrentMs-timeStartMs)/1000); 
					if(secs != secsOld)
						publishProgress(Integer.valueOf(secs));
					secsOld = secs;

				}
				dis.close();
			} catch (Throwable t) {
				Log.e("AudioTrack", "Playback Failed");
				t.printStackTrace();
			}

			audioTrack.release();

			return null;
		}
		protected void onProgressUpdate(Integer... progress) {
			statusText.setText(progress[0].toString() + " sec");
		}	
		protected void onPostExecute(String result) {
			playbackButton.setText("Start Playback");
			playbackButton.setEnabled(true);
			recordingButton.setEnabled(true);
			doneButton.setEnabled(true);
			
		}
	}

	private class RecordAudio extends AsyncTask<Void, Integer, String> {
		AudioRecord audioRecord = null;
		@Override
		protected		
		 void onPreExecute (){
			audioRecord = null;			 
		 }
		@Override
		protected String doInBackground(Void... params) {
			isRecording = true;
			try {
				recordingFile = File.createTempFile("recording", ".pcm", null);
			} catch (IOException e) {
				throw new RuntimeException("Couldn't create file recording.pcm in "+System.getProperty ("java.io.tmpdir"), e);
			}

			try {
				DataOutputStream dos = new DataOutputStream(
						new BufferedOutputStream(new FileOutputStream(
								recordingFile)));

				int bufferSizeInBytes = AudioRecord.getMinBufferSize(frequency,
						AudioFormat.CHANNEL_IN_MONO, audioEncoding);
				int bufferSizeInShorts = bufferSizeInBytes/2;
				audioRecord = new AudioRecord(
						MediaRecorder.AudioSource.MIC, frequency,
						AudioFormat.CHANNEL_IN_MONO, audioEncoding, bufferSizeInBytes);
//				if(AutomaticGainControl.isAvailable()){
//					// LoudnessEnhancer API 19   // TODO					
//					// AutomaticGainControl agc = (AutomaticGainControl) new AudioEffect();
//					// agc.setEnabled(true);
//					int micSessionId = audioRecord.getAudioSessionId();
//					AutomaticGainControl.create(micSessionId);
//				}

				short[] buffer = new short[bufferSizeInShorts];
				audioRecord.startRecording();

				int secs=0, secsOld =0;
				long totalSamples = 0;
				timeStartMs = System.currentTimeMillis ();
				timeCurrentMs = timeStartMs;
				while (isRecording) {
					int numberOfShorts = audioRecord.read(buffer, 0,
							bufferSizeInShorts);
					for (int i = 0; i < numberOfShorts; i++) {
						dos.writeShort(buffer[i]);
					}
					totalSamples += numberOfShorts;
					timeCurrentMs = System.currentTimeMillis ();					
					secs = (int)((timeCurrentMs-timeStartMs)/1000); 
					if(secs != secsOld)
						publishProgress(Integer.valueOf(secs));
					secsOld = secs;
				}

				audioRecord.stop();
				dos.close();
				audioRecord.release();
			} catch (Throwable t) {
				Log.e("AudioRecord", "Recording Failed");
				t.printStackTrace();
				audioRecord.release();
				return null;
			}

			return recordingFile.getAbsolutePath();
		}

		protected void onProgressUpdate(Integer... progress) {
			statusText.setText(progress[0].toString() + " sec");
		}

		protected void onPostExecute(String result) {
			recordingButton.setEnabled(true);
			playbackButton.setEnabled(true);
			if(result != null) {
				rawPcmPath = new String(result);				
				saveXButton.setEnabled(true);
				//imm.toggleSoftInputFromWindow(linearLayout.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);

			}
		}
	}
	public void saveXButtonPressed(){
		greetingFileName.setVisibility(View.VISIBLE);
		saveXButton.setVisibility(View.GONE);
		saveXButton.setEnabled(false);
		saveToFileButton.setVisibility(View.VISIBLE);
		saveToFileButton.setEnabled(true);
		InputMethodManager imm =(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput (findViewById(R.id.GreetingFileName), InputMethodManager.SHOW_FORCED);
		
	}	
	public void saveToFileButtonPressed(){
		Editable edt = ((EditText)findViewById(R.id.GreetingFileName)).getText();
		String savePath = greetingDirPath + edt.toString();
		//  /data/data/com.pmiyusov.mvm/files/voicemail/greetings/newz_greeting.wav
		Toast.makeText(this, "Saving file : "+savePath, Toast.LENGTH_LONG).show();
		Utils.writeWavFromPcm(this, rawPcmPath, savePath, frequency,16);
		newGreetingPath = savePath;		
		// greetingFileName.setVisibility(View.INVISIBLE);
		doneButton.setEnabled(true);
		// InputMethodManager imm =(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		// imm.hideSoftInputFromInputMethod (IBinder token, int flags)
		InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(findViewById(R.id.GreetingFileName).getWindowToken(), 0);
	}	
	public void doneButtonPressed(){
		setGreetingPrefs("3", newGreetingPath, newGreetingPath);
		finish();
	}
	public  void setGreetingPrefs(String  type, String path, String summary){
		getSharedPreferences("preferences", MODE_PRIVATE).edit()
		.putString("greeting_type",type)
		.putString("greeting_file_path",path)
		.commit();
		getSharedPreferences(getPackageName() +"_preferences", MODE_PRIVATE).edit()
		.putString("greeting_type",type)
		.putString("greeting_file_path",path)
		.commit();
		// findPreference("greeting_type").setSummary(summary);
	}

}
