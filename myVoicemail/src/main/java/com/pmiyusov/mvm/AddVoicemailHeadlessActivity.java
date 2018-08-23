package com.pmiyusov.mvm;

// import com.pmiyusov.mvm.voicemail.common.logging.Logger;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
// import android.util.Log;
import com.pmiyusov.conf.BuildProp;
import com.pmiyusov.mvm.Log;
import com.pmiyusov.mvm.R;
import com.pmiyusov.mvm.RecordService.RecordInfo;
import com.pmiyusov.mvm.utils.*;
import com.pmiyusov.mvm.voicemail.common.core.Voicemail;
import com.pmiyusov.mvm.voicemail.common.core.VoicemailImpl;
import com.pmiyusov.mvm.voicemail.common.core.VoicemailProviderHelper;
import com.pmiyusov.mvm.voicemail.common.core.VoicemailProviderHelpers;
import com.pmiyusov.mvm.voicemail.common.utils.CloseUtils;

import android.util.Pair;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A simple activity that stores user entered voicemail data into voicemail
 * content provider. To be used as a test voicemail source.
 */
public class AddVoicemailHeadlessActivity extends Activity {
	private static final String TAG = "AddVoicemailHeadlessActivity";
	private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy h:mm");
	// private static final Logger logger = Logger
	// .getLogger(AddVoicemailHeadlessActivity.class);

	// private static final SimpleDateFormat DATE_FORMATTER = new
	// SimpleDateFormat(
	// "dd/MM/yyyy h:mm");
	/**
	 * This is created in {@link #onCreate(Bundle)}, and needs to be released in
	 * {@link #onDestroy()}.
	 */
	private VoicemailProviderHelper mVoicemailProviderHelper;
	private Uri mRecordingUri;
	String sender;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mVoicemailProviderHelper = VoicemailProviderHelpers
				.createPackageScopedVoicemailProvider(this);
	}

	public void onStart() {
		Log.d(TAG, " onStart");
		super.onStart();
		storeVoicemail();
	}

	private void storeVoicemail() {

		try {
			Pair<Voicemail, Uri> newVoicemail = new Pair<Voicemail, Uri>(
					buildVoicemailObject(), mRecordingUri);
			new InsertVoicemailTask().execute(newVoicemail);
		} catch (Exception e) {
			handleError(e);
		}
	}

	private Voicemail buildVoicemailObject() throws ParseException {
		// TODO RecordInfo copy constructor 
//		RecordInfo ri = RecordService.lastRecord;
//		long time = ri.timeEndMs;
//		long duration = ri.durationSec;
//		String sender = new String(ri.number);
//		String recordFilePath = ri.recordFilePath;
		String recordFilePath = getIntent().getStringExtra("recordFilePath");
		Uri uri = Uri.fromFile(new File(recordFilePath));
		mRecordingUri = uri;
		int format = getIntent().getIntExtra("format", 3); // RAW_AMR
		long duration = getIntent().getLongExtra("durationSec", 0);
		long timeStamp = new Date().getTime();
		// long timeStamp = getIntent().getLongExtra("timeEndMs", 0);
		sender = getIntent().getStringExtra("number");
		// String mimeType = "audio/3gpp";
		String sourcePackageName = getPackageName();
		return VoicemailImpl.createForInsertion(timeStamp, sender)
				.setDuration(duration).setSourcePackage(sourcePackageName)
				.build();
	}

	private void handleError(Exception e) {
		Log.d(TAG, e.getMessage());
		e.printStackTrace();
		// mDialogHelper.showErrorMessageDialog(R.string.voicemail_store_error,
		// e);
	}

	/**
	 * An async task that inserts a new voicemail record using a background
	 * thread. The tasks accepts a pair of voicemail object and the recording
	 * Uri as the param. The result returned is the error exception, if any,
	 * encountered during the operation.
	 */
	private class InsertVoicemailTask extends
			AsyncTask<Pair<Voicemail, Uri>, Void, Exception> {
		@Override
		protected Exception doInBackground(Pair<Voicemail, Uri>... params) {
			if (getSharedPreferences("preferences", MODE_PRIVATE).getString("storage_viewer", "sdcard").equals("sdcard")) 
				// if (getSharedPreferences("preferences", MODE_PRIVATE).getBoolean("use_external_storage", true) == true) 
				return null; // not using content provider, goto onPostExecute()

			if (params.length > 0) {
				try {
					insertVoicemail(params[0].first, params[0].second);
				} catch (Exception e) {
					return e;
				}
			}
			return null;
		}

		private void insertVoicemail(Voicemail voicemail, Uri recordingUri)
				throws Exception {
			File recordingFile = new File(recordingUri.getPath());
			if (recordingFile.length() < 200) { // TODO detect faulty record
				Log.d(TAG, " Faulty record" + recordingUri.getPath());
				Log.d(TAG, " Not saving message.");
				return;
			}
			InputStream inputAudioStream = recordingUri == null ? null
					: getContentResolver().openInputStream(recordingUri);
			Uri newVoicemailUri = mVoicemailProviderHelper.insert(voicemail);
			Log.i(TAG, "Inserted new voicemail URI: " + newVoicemailUri);
			String mimeType = getContentResolver().getType(recordingUri);
			// TODO FIXME!
			if (mimeType == null) {
				mimeType = "vnd.android.cursor.item/voicemail"; //"audio/3gpp";
			}
			if (inputAudioStream != null) {
				try {
					mVoicemailProviderHelper.setVoicemailContent(
							newVoicemailUri, inputAudioStream, mimeType);
				} finally {
					CloseUtils.closeQuietly(inputAudioStream);
				}
			}
		}

		@Override
		protected void onPostExecute(Exception error) {
			Log.d(TAG, " onPostExecute");
			if (getSharedPreferences("preferences", MODE_PRIVATE).getString("storage_viewer", "sdcard").equals("sdcard")){ 
			// if (getSharedPreferences("preferences", MODE_PRIVATE).getBoolean("use_external_storage", true) == true) {
					saveRecordingToSdcard(RecordService.lastRecord);
					Toast t = Toast.makeText(getApplicationContext(),
							"Message Saved ", Toast.LENGTH_LONG);
					t.show();
			}
			Utils.postNotification(getBaseContext(), getString(R.string.you_have_new_voice_mail), "From "+ sender,  com.pmiyusov.mvm.viewer.RecordingListActivity.class);
			File file = new File(RecordService.lastRecord.recordFilePath);
			boolean deleted = file.delete();
			if (deleted == false)
				Log.e(TAG, "Failed to delete file "
						+ RecordService.lastRecord.recordFilePath);

			mediaRescan();
			// now we can stop service and lose RecordService.lastRecord			
			MyVoicemailDaemon.getInstance().stopRecordService();
			finish();
		}

		void mediaRescan() {
			String voicemailStoreDir = Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/" + BuildProp.STORAGE_DIRECTORY_NAME;
			String[] filesAndDirs = { voicemailStoreDir };
			MediaScannerConnection.scanFile(getApplicationContext(), filesAndDirs,
					new String[] { "*/*" } /* mimeType */,
					new MediaScannerConnection.OnScanCompletedListener() {
						@Override
						public void onScanCompleted(String path, Uri uri) {
							// TODO Auto-generated method stub
							Log.d(TAG, " Media Scan Completed");

						}
					});
		}
	}

	public void fileCopy(String srcPath, String dstDir) throws IOException {
		File src = new File(srcPath);
		String filename = Utils.safeSubstring(srcPath, srcPath.lastIndexOf("/") + 1);
//		String filename = srcPath.substring(srcPath.lastIndexOf("/") + 1);
		String dstPath = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/" + dstDir + "/" + filename;
		File dst = new File(dstPath);
		dst.setWritable(true, false);
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}
	public String extendFileName(String src, String addition){
		// insert some addition chars to filename before extention
		
		String beforeExt = Utils.safeSubstring(src, 0, src.lastIndexOf('.'));
//		String beforeExt = src.substring(0, src.lastIndexOf('.'));
		String ext = Utils.safeSubstring(src, src.lastIndexOf('.')+1, src.length());
//		String ext = src.substring(src.lastIndexOf('.')+1, src.length());
		String extendedFileName = beforeExt + "-" + addition + "sec" + "." + ext;
		return extendedFileName;
	}

	public void saveRecordingToSdcard(RecordInfo ri) {
		File src = new File(ri.recordFilePath);
		String srcFileName = Utils.safeSubstring(ri.recordFilePath, ri.recordFilePath
				.lastIndexOf("/") + 1);
		String dstDirPath = getSharedPreferences("preferences", MODE_PRIVATE).getString("storage_directory", null);
		String dstFilePath = dstDirPath + "/"
				+ extendFileName(srcFileName, Integer.toString((int) ri.durationSec));
		try {
			File dst = new File(dstFilePath);
			dst.setWritable(true, false);
			InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dst);

			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		} catch (IOException e) {
			Toast t = Toast.makeText(
					getApplicationContext(),
					TAG + "Error saving file "
							+ RecordService.lastRecord.recordFilePath
							+ " in directory "
							+ getSharedPreferences("preferences", MODE_PRIVATE).getString("storage_directory", 
									getFilesDir()+"/"+BuildProp.STORAGE_DIRECTORY_NAME)
							+ e.toString(), Toast.LENGTH_LONG);
			t.show();
			Log.e(TAG, "Error saving file "
					+ RecordService.lastRecord.recordFilePath
					+ " in directory "
					+ getSharedPreferences("preferences", MODE_PRIVATE).getString("storage_directory", 
				getFilesDir()+"/"+BuildProp.STORAGE_DIRECTORY_NAME));
		}
	}

	}
