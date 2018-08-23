package com.pmiyusov.mvm.viewer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.pmiyusov.mvm.Log;


/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class BoxContent {
	public static String recordingsDir = null;	
	public  List<RecordingItem> ITEMS;
	public  Map<String, RecordingItem> ITEM_MAP;
	Context context ;
	private static final String TAG = "BoxContent";

	 BoxContent(String recordingsPath) {
		 context = RecordingListActivity.context;
		 recordingsDir = recordingsPath;
	/**
	 * An array of sample (dummy) items.
	 */
	ITEMS = new ArrayList<RecordingItem>();

	/**
	 * A map of sample (dummy) items, by ID.
	 */
	ITEM_MAP = new HashMap<String, RecordingItem>();
		// Add 3 sample items.
//		addItem(new RecordingItem("1", "Item 1"));
//		addItem(new RecordingItem("2", "Item 2"));
//		addItem(new RecordingItem("3", "Item 3"));
        File dir = new File(recordingsDir);
        if (!dir.exists()) {
            try {
                dir.mkdirs();
            } catch (Exception e) {
                Log.e(TAG, " Can't create directory " + dir + ": " + e);
                Toast t = Toast.makeText(context, TAG + "Can't create directory  " + dir  + "Is SDCARD installed?",
                		Toast.LENGTH_LONG);
                t.show();
                return;
            }
        } else {
            if (!dir.canWrite()) {
                Log.e(TAG, "No write permission for directory: " + dir);
                Toast t = Toast.makeText(context, TAG+" No write permission for the directory " + dir, Toast.LENGTH_LONG);
                t.show();
                return;
            }
        }
////////////// get file list sorted by date reversed //////////////////////////
        File[] files = dir.listFiles();
        if(files.length > 0)
        	Arrays.sort(files, new Comparator<File>(){
        		public int compare(File f1, File f2)
        		{
        			return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
        		} });
        String[] dlist = new String[files.length];
        int i=0;
        for (File file: files){
        	dlist[i++] = new String(file.getName());
        }
/////////////////////////////////////////////////////////////
        // String[] dlist = dir.list();

        for (i=0; i<dlist.length; i++) {
        	RecordingItem newItem = new RecordingItem(Integer.toString(i+3), recordingsDir, dlist[i]);
        	if(!newItem.number.matches("INVALID")){
        		addItem(newItem);
        	}
        }
	}

	private  void addItem(RecordingItem item) {
		ITEMS.add(item);
		ITEM_MAP.put(item.id, item);
	}
}
	/**
	 * An item representing a piece of content.
	 */
 class RecordingItem {
		private static final String TAG = "BoxContent";
		public  String recordingsDir = null;	
	 // recording meta data encoded in filename
	 // number-month-day-hour-minute-second-duration.extension
		public String id = "?";
		public String fileName = "?";
		public String number = "?";
		public String month = "?";
		public String day = "?";
		public String hour = "?";
		public String minute = "?";
		public String second = "?";
		public String duration = "?";
		public String extension = "?";

		public  RecordingItem(String id, String recordingsPath, String fileName) {
			this.id = id;
			recordingsDir = recordingsPath;
			this.fileName = fileName;
			String meta[] = fileName.split("-");
			if(meta.length != 7){
				number = "INVALID";
				month = fileName; //to show in listing
				return;
			}
			number = formatPhoneNumber(meta[0]);
			month = meta[1];
			day = meta[2];
			hour = meta[3];
			minute = meta[4];
			second = meta[5];
			try {
			duration = meta[6].substring(0, meta[6].indexOf("."));
			extension = meta[6].substring(meta[6].indexOf("."),meta[6].length());
			} catch (Exception e){
				Log.e(TAG, "Error parsing filename: "+fileName);
				duration = "0";
				extension = "bad";
			}
			return;
			
		}
		public String formatPhoneNumber(String rawNumber){
			int l = rawNumber.length();
			if(l<10)
				return rawNumber;
			String fmtNum  = 
					rawNumber.substring(0,l-10) + "("
				+	rawNumber.substring(l-10,l-7) + ")"
				+	rawNumber.substring(l-7,l-4) + "-"					
				+	rawNumber.substring(l-4,l);
			return fmtNum;
		}
		@Override
		public String toString() {
			return number + " " + month + "/" + day + " " + hour + ":" + minute + " " + duration;
		}
}
