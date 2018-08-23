/**
 * 
 */
package com.pmiyusov.conf;

// import 	android.content.pm.PackageManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;

import android.content.pm.PackageManager.NameNotFoundException;
import 	android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;

import com.pmiyusov.mvm.Log;
/**
 * @author paul
 *
 */
public class BuildProp extends ContextWrapper{
	/* XXX
	 * Build images for  combination of:
	 * E - EVALUATION
	 * D - DEBUG
	 * P - PROGUARD no DEBUG
	 * Manifest:
	 * android:debuggable="false"
	 * project.properties
	 *     uncomment proguard.config for RELEASE
	 */
	static final String		TAG = "myVoicemail";
	public static boolean	RELEASE = true;
	public static boolean	EVALUATION = true;
	public static boolean	DEBUG =  true;
	public static final int		MAX_DURATION_SEC = 15;
	public static final String STORAGE_DIRECTORY_NAME = "voicemail";
	public static final String GREETING_TYPE_DEFAULT = "1";
	public static final String GREETING_TYPE_ALTERNATIVE = "2";
	public static final String GREETING_TYPE_CUSTOM = "3";

	public static String VERSION_NAME = "Not Set";
	public static int VERSION_CODE = 0;
	public static int PREFS_RESOURCE_ID = 0;
	public static String PREFS_RESOURCE_NAME = null;
	public static String PACKAGE_NAME = null;
	static String buildPropPath = "/system/build.prop";
	public static String ro_board_platform = "";
	public static ArrayList<String> buldPropLines = null;
	Context context;
	static String[] platformsExcluded = {"msm\\S*", "pdp\\S+"};
	static String[] platformsSupported = {"exynos3*", "exynos4*", "exynos5*"};
	public BuildProp(Context c)  {
		super(c);
		PACKAGE_NAME = getPackageName();
		try {
			VERSION_NAME = getPackageManager().getPackageInfo(PACKAGE_NAME, 0).versionName;
			VERSION_CODE = getPackageManager().getPackageInfo(PACKAGE_NAME, 0).versionCode;	
		} catch (NameNotFoundException e) {
			VERSION_NAME = "Version Name NOT FOUND";
			VERSION_CODE = -1;	
			e.printStackTrace();
		}
		PREFS_RESOURCE_NAME = "pref_myvoicemail_default";
		if(Build.MANUFACTURER.contains("samsung"))
			PREFS_RESOURCE_NAME = "pref_myvoicemail_samsung";	    	 
		PREFS_RESOURCE_ID = getResources().getIdentifier(PREFS_RESOURCE_NAME,
				"xml", PACKAGE_NAME);
		Log.e(TAG, "VERSION_CODE:"+VERSION_CODE);
		Log.e(TAG, "VERSION_NAME:"+VERSION_NAME);
		Log.e(TAG, "RELEASE:"+RELEASE);
		Log.e(TAG, "EVALUATION:"+EVALUATION);
		Log.e(TAG, "PACKAGE_NAME:"+PACKAGE_NAME);
		Log.e(TAG, "DEBUG:"+DEBUG);
		scanBuildProps();
		Log.e(TAG, "Platform "+ro_board_platform+ (isDeviceSupported()?" Is supported":" Is NOT Supported"));
	}
	// collect strings from build.prop file in buldPropLines
	// extract platform name
	void scanBuildProps(){
	    buldPropLines = new ArrayList<String>();
		File buildPropFile = new File(buildPropPath);
	    
		Scanner scan = null;
		try {
			scan = new Scanner(buildPropFile);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "File "+ buildPropPath + "not found!");
			ro_board_platform = "unknown";
			return;
		}
	    
	    while (scan.hasNext()) {
	        String strLine = scan.nextLine();
	        buldPropLines.add(strLine);
	    }
		scan.close();
	    for( String line: buldPropLines ){
	    	if(line.contains("ro.board.platform")){
	    		ro_board_platform = line.split("=")[1].trim();
		    	Log.e(TAG, "Platform = " + ro_board_platform);
	    	}
	    }
		
	}
	// check detected platform against exclusion list
	public boolean isDeviceSupported(){
		for( String line: platformsExcluded ){
			if(ro_board_platform.matches(line)){
				return false;
			}
		}
		return true;
	}
}

