package com.pmiyusov.mvm;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
// import android.util.Log;
import com.pmiyusov.mvm.MyVoicemailDaemon;
import com.pmiyusov.mvm.Log;
import com.pmiyusov.mvm.R;
import com.pmiyusov.mvm.viewer.*;
import com.pmiyusov.mvm.viewer.RecordingListActivity;
import com.pmiyusov.conf.BuildProp;
import com.pmiyusov.purchaseupgrade.IaUpgradeHelper;
import com.pmiyusov.purchaseupgrade.IaUpgradeHelper.UiCallBackListener;
import com.pmiyusov.purchaseupgrade.util.IabResult;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
//import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import android.app.Fragment;
import java.util.List;
//import com.ipaulpro.afilechooser.
import com.ipaulpro.afilechooser.utils.FileUtils;
import com.ipaulpro.afilechooser.FileChooserActivity;

//import group.pals.android.lib.ui.filechooser.io.localfile.LocalFile;
//import group.pals.android.lib.ui.filechooser.FileChooserActivity;
//import group.pals.android.lib.ui.filechooser.services.LocalFileProvider;
//import group.pals.android.lib.ui.filechooser.services.IFileProvider;
import 	android.support.v4.content.LocalBroadcastManager;
/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity implements UiCallBackListener{
	/**
	 * Determines whether to always show the simplified settings UI, where
	 * settings are presented in a single list. When false, settings are shown
	 * as a master/detail two-pane view on tablets. When true, a single pane is
	 * shown on tablets.
	 */
	static final int LIST_MESSAGES_REQUEST = 1;  // The request code
	static final int RECORD_GREETING_REQUEST = 2;
//	static final int PREMIUM_UPGRADE_REQUEST = 3;
	private static final String TAG = "SettingsActivity";
	// Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + DEFAULT_STORAGE_DIRECTORY;
	private static final boolean ALWAYS_SIMPLE_PREFS = true;
	private static final int REQUEST_CODE_VIEW_VOICEMAIL = 1;
	private static final int _ReqChooseFile = 101;
	static Object myLock = new Object();
	static boolean fileSelected = false;
	static Button buttonStartViewer;
	static Button buttonPlayGeeting;
	static Button buttonRecordGreeting;
	static Button buttonPremiumUpgrade;
	
	private static LocalBroadcastManager mLBM;
	private boolean onStartup = true;
	BuildProp buildProp;
	SharedPreferences sharedPrefs = null;
    // The helper object
    IaUpgradeHelper mIaUpgradeHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		buildProp = new BuildProp(getApplicationContext());
		String storageDirectoryPath = null;
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setTitle(getTitle() + " " +com.pmiyusov.conf.BuildProp.VERSION_NAME);
		if(com.pmiyusov.conf.BuildProp.EVALUATION){
			setTitle(getTitle() + " " + getString (R.string.evaluation));
		}
		onStartup = true;
		//	    String state = Environment.getExternalStorageState();
		//	    if ((Environment.MEDIA_MOUNTED.equals(state) &&
		//	    !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))) {
		//	    	storageDirectoryPath = 
		//	    			Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + com.pmiyusov.conf.BuildProp.STORAGE_DIRECTORY_NAME;	    	
		//	    } else {
		//	    	storageDirectoryPath = "/data/local/tmp" + "/" + com.pmiyusov.conf.BuildProp.STORAGE_DIRECTORY_NAME;   // TOFIX HARDCODED
		//	    }
		//
		//		// Default is packageName_preferences.xml
		sharedPrefs= this.getSharedPreferences("preferences", MODE_PRIVATE);
		//	    if(!sharedPrefs.contains("storage_directory"))
		//	    	sharedPrefs.edit().putString("storage_directory", storageDirectoryPath).commit();
		// alternative file
		// sharedPrefs = getSharedPreferences(SHARED_PREFS_FILE_NAME, MODE_MULTI_PROCESS);

		// enable HW Volume Up/Down
		setVolumeControlStream (AudioManager.STREAM_MUSIC);

		// write default values from xml to a file in shared_prefs own directory
		// TOFIX do we need xml to file?
		// PreferenceManager.setDefaultValues(getApplicationContext(),
		// 		R.xml.pref_enable_voicemail, true);

		// PreferenceManager.setDefaultValues(Context context, int resourceId,
		// boolean readAgain);
		ListView v = getListView();

		// Create button to start viewer	
		buttonStartViewer = new Button(getApplicationContext());
		buttonStartViewer.setWidth(20); // FIXME Good looking button
		buttonStartViewer.setText(R.string.view_messages);
		buttonStartViewer.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Intent i = new Intent("com.pmiyusov.mvm.ACTION_VIEW_VOICEMAL_LIST");
				Intent i = new Intent(getApplicationContext(), com.pmiyusov.mvm.viewer.RecordingListActivity.class);
				startActivityForResult(i, LIST_MESSAGES_REQUEST);
			}
		});
		v.addFooterView(buttonStartViewer);

		// Create button to play back greeting	
		buttonPlayGeeting = new Button(getApplicationContext());
		buttonPlayGeeting.setWidth(20); // FIXME Good looking button
		buttonPlayGeeting.setText(R.string.play_greeting);
		buttonPlayGeeting.setOnClickListener(new OnClickListener() {
			Boolean greetingIsPlaying = false;

			@Override
			public void onClick(View v) {
				// If playback in progress then stop it				
				if(MyVoicemailDaemon.playbackCompleted == false){
					PlayTrack.stopPlayingTrack();
//					if(MyVoicemailDaemon.audioTrack != null){
//						try {
//							MyVoicemailDaemon.audioTrack.stop();
//							MyVoicemailDaemon.playbackCompleted = true;
//							MyVoicemailDaemon.audioTrack.release();
//							greetingIsPlaying = false;
//						} catch (Exception e){
//							Log.d(TAG, "audioTrack Error"+e);
//							//return;						
//						}				
//					}
					if(MyVoicemailDaemon.mp != null){
						try {
							MyVoicemailDaemon.mp.release();
							MyVoicemailDaemon.playbackCompleted =	true;
						}catch (IllegalStateException e){
							Log.d(TAG, "MediaPlayer Error"+e);
						}
					}
					
					return;
				}
									
				// No playback in progress - Play greeting on speaker
				MyVoicemailDaemon.getInstance() .playGreeting(AudioManager.STREAM_MUSIC, true);
			}
		});
		v.addFooterView(buttonPlayGeeting);
		
		// Create button to record greeting	
		buttonRecordGreeting = new Button(getApplicationContext());
		buttonRecordGreeting.setWidth(20); // FIXME Good looking button
		buttonRecordGreeting.setText(R.string.record_new_greeting);
		buttonRecordGreeting.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getApplicationContext(), com.pmiyusov.mvm.RecordGreetingActivity.class);
				startActivityForResult(i, RECORD_GREETING_REQUEST);
			}
		});
		v.addFooterView(buttonRecordGreeting);
		// Create button to record greeting	
		buttonPremiumUpgrade = new Button(getApplicationContext());
		buttonPremiumUpgrade.setWidth(20); // FIXME Good looking button
		buttonPremiumUpgrade.setText(R.string.get_full_version);
		buttonPremiumUpgrade.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onUpgradeAppButtonClicked(v);
				}
		});
		v.addFooterView(buttonPremiumUpgrade);

		
	}

//	public void selectFile() {
//		if (onStartup == true) {
//			; // TODO read persistant value of customGreetingFilePath
//			return;
//		}
//		Intent intent = new Intent(MyVoicemailDaemon.context, FileChooserActivity.class);
//		// intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		/*
//		 * by default, if not specified, default rootpath is sdcard, if sdcard
//		 * is not available, "/" will be used
//		 */
//		String greetingsDirPath = getSharedPreferences("preferences", MODE_PRIVATE).getString("storage_directory", null)
//				+ "/greetings";
//
//		intent.putExtra(FileChooserActivity._Rootpath,
//				(Parcelable) new LocalFile(greetingsDirPath));
//
//		intent.putExtra(FileChooserActivity._RegexFilenameFilter,
//			"(?si).*\\.(wav|pcm|xyz)$"); // only .wav is can be played in-call by mediaPlayer (based on test)
//		// "(?si).*\\.(mp3|wav|3gp|3gpp|aac|m4a|amr)$");
//		startActivityForResult(intent, _ReqChooseFile);
//	}
	////////////////
	public void selectFile() {
		if (onStartup == true) {
			; // TODO read persistant value of customGreetingFilePath
			return;
		}

		// Use the GET_CONTENT intent from the utility class
		Intent target = FileUtils.createGetContentIntent();
		// Create the chooser Intent
		Intent intent = Intent.createChooser(
				target, "Select File");
		try {
			startActivityForResult(intent, _ReqChooseFile);
		} catch (ActivityNotFoundException e) {
			// The reason for the existence of aFileChooser
		}
	}

	///////////////

	public void closeSettings() {
		moveTaskToBack(true);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onPostCreate");
		super.onPostCreate(savedInstanceState);

		setupSimplePreferencesScreen();
		iapOnCreate();
		onStartup = false;
	}

	/**
	 * Shows the simplified settings UI if the device configuration if the
	 * device configuration dictates that a simplified, single-pane UI should be
	 * shown.
	 */
	private void setupSimplePreferencesScreen() {
		if (!isSimplePreferences(this)) {
			return;
		}
		Log.d(TAG, "setupSimplePreferencesScreen");
		// In the simplified UI, fragments are not used at all and we instead
		// use the older PreferenceActivity APIs.

		// Add 'Voicemail' preferences, and a corresponding header.
		addPreferencesFromResource(buildProp.PREFS_RESOURCE_ID);
		// Uncomment if needed
		//		PreferenceCategory preferenceHeader = new PreferenceCategory(this);
		//		getPreferenceScreen().addPreference(preferenceHeader);
		//		if(com.pmiyusov.conf.BuildProp.EVALUATION)
		//			preferenceHeader.setTitle(R.string.pref_header_settings_eval);
		//		else
		//			preferenceHeader.setTitle(R.string.pref_header_settings_full);

		// Bind the summaries of  preferences to
		// their values. When their values change, their summaries are updated
		// to reflect the new value, per the Android Design guidelines.

		bindPreferenceSummaryToValue(findPreference("greeting_type"));
		bindPreferenceSummaryToValue(findPreference("greeting_use_speakerphone"));
		bindPreferenceSummaryToValue(findPreference("recording_use_speakerphone"));
		bindPreferenceSummaryToValue(findPreference("delay_ring"));
		bindPreferenceSummaryToValue(findPreference("enable_voicemail"));
		bindPreferenceSummaryToValue(findPreference("audio_source"));
		bindPreferenceSummaryToValue(findPreference("audio_format"));
		//bindPreferenceSummaryToValue(findPreference("use_external_storage"));
		bindPreferenceSummaryToValue(findPreference("max_duration_sec"));
		bindPreferenceSummaryToValue(findPreference("greeting_volume"));
		bindPreferenceSummaryToValue(findPreference("storage_directory"));
		bindPreferenceSummaryToValue(findPreference("greeting_stream"));
		bindPreferenceSummaryToValue(findPreference("storage_directory"));
		bindPreferenceSummaryToValue(findPreference("storage_viewer"));
		bindPreferenceSummaryToValue(findPreference("greeting_audio_mode"));
		bindPreferenceSummaryToValue(findPreference("recorder_audio_mode"));
		if(com.pmiyusov.conf.BuildProp.EVALUATION){
			Preference mds = findPreference("max_duration_sec");
			mds.setSummary(getString(R.string.limited_to)+" "+com.pmiyusov.conf.BuildProp.MAX_DURATION_SEC + getString(R.string.seconds));
			mds.setEnabled(false); // fixed value from xml
		}

	}

	/** {@inheritDoc} */
	@Override
	public boolean onIsMultiPane() {
		return isXLargeTablet(this) && !isSimplePreferences(this);
	}

	/**
	 * Helper method to determine if the device has an extra-large screen. For
	 * example, 10" tablets are extra-large.
	 */
	private static boolean isXLargeTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	/**
	 * Determines whether the simplified settings UI should be shown. This is
	 * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
	 * doesn't have newer APIs like {@link PreferenceFragment}, or the device
	 * doesn't have an extra-large screen. In these cases, a single-pane
	 * "simplified" settings UI should be shown.
	 */
	private static boolean isSimplePreferences(Context context) {
		return ALWAYS_SIMPLE_PREFS
				|| Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
				|| !isXLargeTablet(context);
	}

	/** {@inheritDoc} */
	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void onBuildHeaders(List<Header> target) {
		if (!isSimplePreferences(this)) {
			loadHeadersFromResource(R.xml.pref_headers, target);
		}
	}

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private  Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();
			Log.d(TAG, "onPreferenceChange preference: "+preference.toString()+" value: " + stringValue);

			if (preference instanceof ListPreference) {
				ListPreference listPreference = (ListPreference) preference;
				if(preference.getKey().equals("greeting_type")){
					if(stringValue.equals(com.pmiyusov.conf.BuildProp.GREETING_TYPE_CUSTOM)){  // GREETING_TYPE_CUSTOM
						selectFile();
						// if(customGreetingFilePath != null){
						// Path is set asynchronously so it is not set yet
						// return false; // don't change it here
					}
				} 
				getSharedPreferences("preferences", MODE_PRIVATE).edit()
				.putString(preference.getKey(), stringValue)
				.commit();

				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				int index = listPreference.findIndexOfValue(stringValue);
				// Set the summary to reflect the new value.
				preference
				.setSummary(index >= 0 ? listPreference.getEntries()[index]
						: null);
			} else if (preference instanceof EditTextPreference) {
				if(preference.getKey().equals("greeting_volume")){
					String greetingVolumestr = stringValue;
					int greetingVolume;
					try{
						greetingVolume = Integer.parseInt(greetingVolumestr);
					}
					catch(NumberFormatException e){
						Log.e(TAG, "Error parsing Integer "+greetingVolumestr+". Setting 2");
						return false; // don't change
					}
					if(greetingVolume < 0)
						return false; // don't change	
					if(greetingVolume > 10)
						return false; // don't change
				}
				if(preference.getKey().equals("max_duration_sec")){
					String maxDurationStr = stringValue;
					int maxDuration;
					try{
						maxDuration = Integer.parseInt(maxDurationStr);
					}
					catch(NumberFormatException e){
						Log.e(TAG, "Error parsing Integer "+maxDurationStr+". Not changing");
						return false; // don't change
					}
					if(maxDuration < 1)
						return false; // don't change	
				}
				preference.setSummary(stringValue);
				getSharedPreferences("preferences", MODE_PRIVATE).edit()
				.putString(preference.getKey(), stringValue)
				.commit();
			} else if (preference instanceof CheckBoxPreference) {
				// preference.setSummary(stringValue);
				Boolean boolVal = Boolean.valueOf(stringValue);
				getSharedPreferences("preferences", MODE_PRIVATE).edit()
				.putBoolean(preference.getKey(), boolVal)
				.commit();
			}

			else {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				// TODO Handle CheckBox prefs summary
				preference.setSummary(stringValue); // ???
			}
			// TODO preferences have changed - Restart Services?
			return true;

		}
	};

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 * 
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private  void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		String stringValue = preference.toString();
		String mKey = preference.getKey();
		Context c = preference.getContext();
		Log.d(TAG, "bindPreferenceSummaryToValue  " + "Key=" + mKey + " value="
				+ stringValue);

		preference
		.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
		// Trigger the listener immediately with the preferences default file
		// TODO change to preference.getPersistedBoolean(false);
		if (preference instanceof CheckBoxPreference) {
			Boolean  boolPref = preference.getSharedPreferences().getBoolean(mKey, false);
			sBindPreferenceSummaryToValueListener.onPreferenceChange(
					preference, boolPref);
		} else 
			if (preference instanceof ListPreference) {
				String stringPref = preference.getSharedPreferences().getString(mKey, "undefined");
				sBindPreferenceSummaryToValueListener.onPreferenceChange(
						preference, stringPref);
			}
			else if (preference instanceof EditTextPreference) {
				String stringPref = preference.getSharedPreferences().getString(mKey, "Not defined, tap to enter");			
				sBindPreferenceSummaryToValueListener.onPreferenceChange(
						preference, stringPref);

			}
	}

	/**
	 * This fragment shows general preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	// @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public class GeneralPreferenceFragment extends Fragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			// addPreferencesFromResource(R.xml.pref_general);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			// bindPreferenceSummaryToValue(findPreference("example_text"));
			// bindPreferenceSummaryToValue(findPreference("example_list"));
		}
	}

	/**
	 * This fragment shows Enable Voicemail preference
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public  class VoicemailPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(com.pmiyusov.conf.BuildProp.PREFS_RESOURCE_ID);
			bindPreferenceSummaryToValue(findPreference("enable_voicemail"));
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		iapOnActivityResult(requestCode, resultCode, data);
		String tStr = null;
		switch (requestCode) {
		case LIST_MESSAGES_REQUEST:
			if (resultCode == RESULT_OK) {  // restart anyway
				Intent i = new Intent(MyVoicemailDaemon.context, com.pmiyusov.mvm.viewer.RecordingListActivity.class);
				i.putExtra("storageDirectoryPath", MyVoicemailDaemon.storageDirectoryPath);
				startActivityForResult(i, LIST_MESSAGES_REQUEST);
			}
			break;
		case RECORD_GREETING_REQUEST:
			
			break;
	//////////////////////////////////////////
			case _ReqChooseFile:
				// If the file selection was successful
				if (resultCode == RESULT_OK) {
					String path = "default";
					if (data != null) {
						// Get the URI of the selected file
						final Uri uri = data.getData();
						android.util.Log.i(TAG, "Uri = " + uri.toString());
						try {
							// Get the file path from the URI
							path = FileUtils.getPath(this, uri);
							Toast.makeText(this,
									"File Selected: " + path, Toast.LENGTH_LONG).show();
							android.util.Log.i("FileSelector", "File Selected: " + path);
						} catch (Exception e) {
							android.util.Log.e("FileSelector", "File select error", e);
						}
					}
					fileSelected = true;
					setGreetingPrefs("3", path, path);
				} else {
					fileSelected = false;
					// roll back to default if path was not set before
					if(tStr == null) {
						setGreetingPrefs("1", null, "Default");
					}

				}
				break;
		}
	}
//
//			case _ReqChooseFile:
//			if (resultCode == RESULT_OK) {
//				/*
//				 * you can use two flags included in data
//				 */
//				IFileProvider.FilterMode filterMode = (IFileProvider.FilterMode) data
//						.getSerializableExtra(FileChooserActivity._FilterMode);
//				boolean saveDialog = data.getBooleanExtra(
//						FileChooserActivity._SaveDialog, false);
//
//				/*
//				 * a list of files will always return, if selection mode is
//				 * single, the list contains one file
//				 */
//				List<LocalFile> files = (List<LocalFile>) data
//						.getSerializableExtra(FileChooserActivity._Results);
//				for (LocalFile f : files) {
//					tStr = new String(f.getAbsolutePath());
//				}
//				fileSelected = true;
//				setGreetingPrefs("3", tStr, tStr);
//			} else {
//				fileSelected = false;
//				// roll back to default if path was not set before
//				if(tStr == null) {
//					setGreetingPrefs("1", null, "Default");
//				}
//			}
//
//			break;
//			*/ /////////////////////////////////////
	public  void setGreetingPrefs(String  type, String path, String summary){
		getSharedPreferences("preferences", MODE_PRIVATE).edit()
		.putString("greeting_type",type)
		.putString("greeting_file_path",path)
		.commit();
		getSharedPreferences(getPackageName() +"_preferences", MODE_PRIVATE).edit()
		.putString("greeting_type",type)
		.putString("greeting_file_path",path)
		.commit();
		findPreference("greeting_type").setSummary(summary);
	}
///////////////////////////////////////////////////////////////////////////
//	In app purchase 
//  Object declaration in UI activity
//    IaUpgradeHelper mIaUpgradeHelper;
// 
//  In app purchase related functions
//	
    public void iapOnCreate() {
    // Create the helper, passing it our context 
    // public key to verify signatures with is defined in helper
        Log.d(TAG, "Creating IaUpgradeHelper.");
        mIaUpgradeHelper = new IaUpgradeHelper(this);
        mIaUpgradeHelper.onCreate(this);       
    // enable debug logging (for a production application, you should set this to false).
        mIaUpgradeHelper.enableDebugLogging(com.pmiyusov.conf.BuildProp.DEBUG);
    }
    protected void iapOnActivityResult(int requestCode, int resultCode, Intent data) {
        // Pass on the activity result to the helper for handling
        if (mIaUpgradeHelper.handleActivityResult(requestCode, resultCode, data)) {
        	Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }
    // Listener that's called when we finish querying the items and subscriptions we own
    // User clicked the "Upgrade to Premium" button.
    public void onUpgradeAppButtonClicked(View arg0) {
    	Log.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");
    	setWaitScreen(true);

    	/* TODO: for security, generate your payload here for verification. See the comments on 
    	 *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use 
    	 *        an empty string, but on a production app you should carefully generate this. */
    	String payload = ""; 
    	try {
    		mIaUpgradeHelper.launchPurchaseFlow(this, payload);
    	}
    	catch (IllegalStateException e) {
    		Toast t = Toast.makeText(getApplicationContext(), 
    	      "Can't start Purchase  because another async operation is in progress." +
    	      "Check your internet connection", Toast.LENGTH_LONG);
    		t.show();
    	}    	
    }
    // updates UI to reflect model
    public void updateUi() {
    	setTitle(R.string.app_name);
    	setTitle(getTitle() + " " +com.pmiyusov.conf.BuildProp.VERSION_NAME);
    	if(mIaUpgradeHelper.isPremium()==false){
			setTitle(getTitle() + " " + getString (R.string.evaluation));
		}
    	else {
    		SharedPreferences sp = getPreferences(MODE_PRIVATE);
    		String maxDurationString = 
    		   sp.getString("max_duration_sec", String.valueOf(com.pmiyusov.conf.BuildProp.MAX_DURATION_SEC));
    		//sp.edit().
    		Preference md =findPreference("max_duration_sec");
    		md.setSummary(maxDurationString);
    		md.setEnabled(true);
    	}   		
    	// "Upgrade" button is only visible if the user is not premium
        buttonPremiumUpgrade.setVisibility(mIaUpgradeHelper.isPremium() ? View.GONE : View.VISIBLE);
    }

    // Enables or disables the "please wait" screen.
    public void    setWaitScreen(boolean set) {
//        findViewById(R.id.screen_main).setVisibility(set ? View.GONE : View.VISIBLE);
 //       findViewById(R.id.screen_wait).setVisibility(set ? View.VISIBLE : View.GONE);
    }


    public void   saveData() {
        
        /*
         * WARNING: on a real application, we recommend you save data in a secure way to
         * prevent tampering. For simplicity in this sample, we simply store the data using a
         * SharedPreferences.
         */
        
        SharedPreferences.Editor spe = getPreferences(MODE_PRIVATE).edit();
        spe.putBoolean("full", mIaUpgradeHelper.isPremium());
        spe.commit();
        Log.d(TAG, "Saved data: full = " + String.valueOf(mIaUpgradeHelper.isPremium()));
        BuildProp.EVALUATION = mIaUpgradeHelper.isPremium()? false: true;
    }
    // to retrieve premium flag stored locally
    public void   loadData() {
        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        mIaUpgradeHelper.setPremium(sp.getBoolean("full", false));
        Log.d(TAG, "Loaded data: full = " + String.valueOf(mIaUpgradeHelper.isPremium()));
    }
    // updates UI to reflect model
	public void updateUi(IabResult result) {
		if (result.isSuccess())
			saveData();
		loadData();
		updateUi();
	}

}