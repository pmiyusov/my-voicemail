package com.pmiyusov.mvm.viewer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

/**
 * A fragment representing a single Recording detail screen. This fragment is
 * either contained in a {@link RecordingListActivity} in two-pane mode (on
 * tablets) or a {@link RecordingDetailActivity} on handsets.
 */
public class RecordingDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";
    public static final String TAG = "RecordingDetailFragment";
    /**
     * The  content this fragment is presenting.
     */
    private RecordingItem mItem;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RecordingDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the item content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            mItem = RecordingListActivity.mBoxContent.ITEM_MAP.get(getArguments().getString(
                    ARG_ITEM_ID));
            if (mItem == null) {
                Log.d(TAG, "Empty mItem,return");
                return;
            }
            Intent i = new Intent(getActivity(), VideoPlayerActivity.class);
            i.putExtra("VOICEMAIL_DIRECTORY_PATH", mItem.recordingsDir);
            i.putExtra("VOICEMAIL_FILE_NAME", mItem.fileName);
            startActivity(i);

        }
    }
/*
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_recording_detail,
				container, false);
		if (mItem == null) {
			Log.d(TAG, "Empty mItem,return rootView");
			return rootView;			
		}
		Intent i = new Intent(getActivity().getApplicationContext(), VideoPlayerActivity.class);
		i.putExtra("VOICEMAIL_DIRECTORY_PATH", mItem.recordingsDir);
		i.putExtra("VOICEMAIL_FILE_NAME", mItem.fileName);
        startActivity(i);

		// Show the dummy content as text in a TextView.
//		if (mItem != null) {
//			((TextView) rootView.findViewById(R.id.recording_detail))
//					.setText(mItem.toString());
//		}
		return rootView;
	}
*/
}
