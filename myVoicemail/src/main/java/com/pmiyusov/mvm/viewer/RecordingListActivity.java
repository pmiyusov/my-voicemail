package com.pmiyusov.mvm.viewer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import com.pmiyusov.mvm.R;
import com.pmiyusov.mvm.conf.BuildProp;

/**
 * An activity representing a list of Recordings. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link RecordingDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link RecordingListFragment} and the item details (if present) is a
 * {@link RecordingDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link RecordingListFragment.Callbacks} interface to listen for item
 * selections.
 */
public class RecordingListActivity extends FragmentActivity implements
        RecordingListFragment.Callbacks {
    public static Context context;
    public static RecordingListActivity mRecordingListActivity;
    private static final String TAG = "RecordingListActivity";
    static final int VIEW_MESSAGE_REQUEST = 100;  // The request code

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    static String mRecordDirectory = null;
    public static BoxContent mBoxContent = null;
    public static ArrayAdapter<RecordingItem> mBoxContentListAdapter;
    static View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        mRecordingListActivity = this;
        mRecordDirectory = getSharedPreferences("preferences", MODE_PRIVATE).getString("storage_directory",
                getFilesDir() + "/" + BuildProp.STORAGE_DIRECTORY_NAME);

        // getIntent().getStringExtra("storageDirectoryPath");
        mBoxContent = new BoxContent(mRecordDirectory);
        mBoxContentListAdapter = new ArrayAdapter<RecordingItem>(this,
                android.R.layout.simple_list_item_1,
                android.R.id.text1, mBoxContent.ITEMS);

        setContentView(R.layout.activity_recording_list);
        root = findViewById(android.R.id.content).getRootView();
        if (findViewById(R.id.recording_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((RecordingListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.recording_list))
                    .setActivateOnItemClick(true);
        }

        // TODO: If exposing deep links into your app, handle intents here.
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Create voice mail box object from directory containing recordings
        if (mRecordDirectory == null)
            mRecordDirectory = getIntent().getStringExtra("storageDirectoryPath");
        if (mBoxContent == null) {
            mBoxContent = new BoxContent(mRecordDirectory);
            mBoxContentListAdapter = new ArrayAdapter<RecordingItem>(this,
                    android.R.layout.simple_list_item_1,
                    // API level 11 android.R.layout.simple_list_item_activated_1,
                    android.R.id.text1, mBoxContent.ITEMS);
        }

    }

    /**
     * Callback method from {@link RecordingListFragment.Callbacks} indicating
     * that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(RecordingDetailFragment.ARG_ITEM_ID, id);
            RecordingDetailFragment fragment = new RecordingDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.recording_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the player activity
            // for the selected item ID.
            RecordingItem mItem = RecordingListActivity.mBoxContent.ITEM_MAP.get(id);
            if (mItem == null) {
                Log.d(TAG, "Empty mItem,return");
                return;
            }

            Intent i = new Intent(this, VideoPlayerActivity.class);
            i.putExtra("VOICEMAIL_DIRECTORY_PATH", mItem.recordingsDir);
            i.putExtra("VOICEMAIL_FILE_NAME", mItem.fileName);
            startActivityForResult(i, VIEW_MESSAGE_REQUEST);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
        finish();
    }
//	public  void updateRecordList(){
//		Utils.rescan();
//		mBoxContent = new BoxContent(mRecordDirectory);
//		mBoxContentListAdapter.notifyDataSetChanged();
//		Intent returnIntent = getIntent(); //new Intent();
//		 //returnIntent.putExtra("result",result);
//		 setResult(RESULT_OK,returnIntent);     
//		 finish();
//
    // This doesn't work
//		mBoxContent = new BoxContent(mRecordDirectory);
//		mBoxContentListAdapter = new ArrayAdapter<RecordingItem>(getApplicationContext (),
//				android.R.layout.simple_list_item_activated_1,
//				android.R.id.text1, mBoxContent.ITEMS);
//		setContentView(R.layout.activity_recording_list);
//		mBoxContentListAdapter.notifyDataSetChanged();
//	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100) {
            setResult(RESULT_OK);
            finish();
        }
    }

    public void finishFromChild(Activity child) {
        finish();
    }
}
