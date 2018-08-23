package com.pmiyusov.purchaseupgrade;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.*;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.pmiyusov.purchaseupgrade.util.IabHelper;
import com.pmiyusov.purchaseupgrade.util.IabResult;
import com.pmiyusov.purchaseupgrade.util.Inventory;
import com.pmiyusov.purchaseupgrade.util.Purchase;

public class IaUpgradeHelper extends IabHelper {
	static final String base64EncodedPublicKey ="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi0RkPzeTT++3HAo7Hmhh7Djo3bfywRVHlJomSHoZZdPfVMp5OFeieoGP+3mgwJbSutpo3WkhUnZgJBkGU0VrktBBJcu0V2tZcZ9iPXyj3PnuRMeN6TgUnWlKQw1CAu2iZS3Gda/L19ZC49HB2zoXSEPGvcEiRIGzFHf6hz7c8EhFgBn7mNITTzogeD0T6B2hkkel0+FJAmS4U4CM/Z2CPfg6haw2mkndqnT7LOXIp31s2I+4uUoMwB0gNvI3oUU9s8jh+Q4MQddrDEa890wEUa/QKkgyQgQ+5Ro2JgMS28MQ6woidlSYNjkTpxalbK4zxO1tdc0dAVrke6jgu5d1TwIDAQAB";
	//	static final String base64EncodedPublicKey ="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjo5X/B9CAF+bQ+CEMxzi6XgBAXSpbzN9t9Er+ObNgWp+xcZqLzxMeuWsI8SwHE+K9dTTxU8x/iLE7M3hHw1/xj+S5WLuCt855RTowTS2yjbGnkvv8VU0ta2obmS/zvTeF+UVtdv+0luhHpjUI4DxxEmVhA1OklaCvS3AAVFOBhZ6Ln9pLZ1nO3NABnaXpqJkejwKXeevo2MMnzmeTYQXOM3xp8bNQSmfWLANizjmZhuVBWKA1pMhq+HHRbxTghJm9bdfjzflbpV8DgidQurthzxrRKjovItX88Lq9SxIUS+3pSGQw1vvAt/pycxoP2iD+EXICHqapgla9E4qiti3pQIDAQAB";
//	static  String PACKAGE = "com.pmiyusov.purchaseupgrade";
	static final String TAG = "IaUpgradeHelper";

	// Does the user have the premium upgrade?
	public static boolean mIsPremium = false;

	// SKUs for our products: the premium upgrade (non-consumable) and gas (consumable)
	//    static final String SKU_PREMIUM = "android.test.purchased";
	public static final String SKU_PREMIUM = "premium";

	// (arbitrary) request code for the purchase flow
	public static final int RC_REQUEST = 1955;
	Context mContext;
	// The helper object
	IabHelper mHelper;
	UiCallBackListener mUiListener = null;
	public IaUpgradeHelper(Context ctx) {
		super(ctx, getBase64EncodedPublicKey());
		mContext = ctx;
		mHelper = this;
	}


	public void startSetup(final OnIabSetupFinishedListener listener) {
		// TODO Auto-generated method stub
		super.startSetup(listener);

	}
	public void launchPurchaseFlow(Activity act,String  payload){
		super.launchPurchaseFlow(act, IaUpgradeHelper.SKU_PREMIUM,IaUpgradeHelper. RC_REQUEST, 
            mPurchaseFinishedListener, payload);
	}
	@Override
	public void launchPurchaseFlow(Activity act, String sku, String itemType,
			int requestCode, OnIabPurchaseFinishedListener listener,
			String extraData) {
		// TODO Auto-generated method stub
		super.launchPurchaseFlow(act, sku, itemType, requestCode, listener, extraData);
	}

	@Override
	public boolean handleActivityResult(int requestCode, int resultCode,
			Intent data) {
		// TODO Auto-generated method stub
		return super.handleActivityResult(requestCode, resultCode, data);
	}

	public static String getBase64EncodedPublicKey() {
		return base64EncodedPublicKey;
	}

//	public static void setBase64EncodedPublicKey(String base64EncodedPublicKey) {
//		IaUpgradeHelper.base64EncodedPublicKey = base64EncodedPublicKey;
//	}

	public static boolean isPremium() {
		return mIsPremium;
	}
//	public interface OnIabSetupFinishedListener {
//	        /**
//	         * Called to notify that setup is complete.
//	         *
//	         * @param result The result of the setup process.
//	         */
//	        public void onIabSetupFinished(IabResult result);
//	    }

	public static void setPremium(boolean IsPremium) {
		mIsPremium = IsPremium;
	}

	IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result,
				Inventory inventory) {
			Log.d(TAG, "Query inventory finished.");
			if (result.isFailure()) {
				Toast.makeText(mContext, "Unable to update inventory. " +
						"Check your network connection ",Toast.LENGTH_LONG).show();
				// complain("Unable to update inventory. Check your network connection ");
				///complain("Failed to query inventory: " + result);
			} else {

				Log.d(TAG, "Query inventory was successful.");

				/*
				 * Check for items we own. Notice that for each purchase, we
				 * check the developer payload to see if it's correct! See
				 * verifyDeveloperPayload().
				 */

				// Do we have the premium upgrade?
				Purchase premiumPurchase = inventory
						.getPurchase(IaUpgradeHelper.SKU_PREMIUM);
				setPremium(premiumPurchase != null
						&& verifyDeveloperPayload(premiumPurchase));
				Log.d(TAG, "User is "
						+ (mIsPremium ? "PREMIUM" : "NOT PREMIUM"));
				mUiListener.saveData();
			}
			Log.d(TAG, "Initial inventory query finished; enabling main UI.");
			mUiListener.updateUi(result);
			mUiListener.setWaitScreen(false);
		}
	};

	// Callback for when a purchase is finished
	IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			Log.d(TAG, "Purchase finished: " + result + ", purchase: "
					+ purchase);
			if (result.isFailure()) {
				complain("Error purchasing: " + result);
			} else if (!verifyDeveloperPayload(purchase)) {
				complain("Error purchasing. Authenticity verification failed.");
			} else if (purchase.getSku().equals(IaUpgradeHelper.SKU_PREMIUM)) {
				Log.d(TAG, "Purchase successful.");
				// bought the premium upgrade!
				Log.d(TAG, "Purchase is premium upgrade. Congratulating user.");
				alert("Thank you for upgrading to premium!");
				setPremium(true);
			} else {
				// not supported
				Log.d(TAG, "Unknown SKU " + purchase.getSku());
				alert("Unknown SKU " + purchase.getSku());
			}
			mUiListener.updateUi(result);
			mUiListener.setWaitScreen(false);
		}
	};

	public void onCreate(UiCallBackListener uiCallBackListener){
		mUiListener = uiCallBackListener;
		mUiListener.updateUi();
		// Start setup. This is asynchronous and the specified listener
		// will be called once setup completes.
		Log.d(TAG, "Starting setup.");
		
		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				Log.d(TAG, "Setup finished.");

				if (!result.isSuccess()) {
					// Oh noes, there was a problem.
					complain("Problem setting up in-app billing: " + result);
					return;
				}

				// Hooray, IAB is fully set up. Now, let's get an inventory of stuff we own.
				Log.d(TAG, "Setup successful. Querying inventory.");
				mHelper.queryInventoryAsync(mGotInventoryListener);
			}
		});
		
	}
	public void onDestroy() {
		// very important:
		Log.d(TAG, "Destroying helper.");
		if (mHelper != null) mHelper.dispose();
		mHelper = null;
	}
	void complain(String message) {
		Log.e(TAG, " Error: " + message);
		alert("Alert: " + message);
	}

	void alert(String message) {
		AlertDialog.Builder bld = new AlertDialog.Builder(mContext);
		bld.setMessage(message);
		bld.setNeutralButton("OK", null);
		Log.d(TAG, "Showing alert dialog: " + message);
		bld.create().show();
	}
	/** Verifies the developer payload of a purchase. */
	boolean verifyDeveloperPayload(Purchase p) {
		String payload = p.getDeveloperPayload();

		/*
		 * TODO: verify that the developer payload of the purchase is correct. It will be
		 * the same one that you sent when initiating the purchase.
		 * 
		 * WARNING: Locally generating a random string when starting a purchase and 
		 * verifying it here might seem like a good approach, but this will fail in the 
		 * case where the user purchases an item on one device and then uses your app on 
		 * a different device, because on the other device you will not have access to the
		 * random string you originally generated.
		 *
		 * So a good developer payload has these characteristics:
		 * 
		 * 1. If two different users purchase an item, the payload is different between them,
		 *    so that one user's purchase can't be replayed to another user.
		 * 
		 * 2. The payload must be such that you can verify it even when the app wasn't the
		 *    one who initiated the purchase flow (so that items purchased by the user on 
		 *    one device work on other devices owned by the user).
		 * 
		 * Using your own server to store and verify developer payloads across app
		 * installations is recommended.
		 */

		return true;
	}
    public interface UiCallBackListener {
        /**
         * Called to execute in UI thread.
         *
         * @param result The result of the setup process.
         */
    	public void  loadData();
    	public void  saveData();
    	public void  updateUi(IabResult result);
    	public void  updateUi();
    	public void  setWaitScreen(boolean oN);
        // public void onIabSetupFinished(IabResult result);
    }

}


/* base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY
 * (that you got from the Google Play developer console). This is not your
 * developer public key, it's the *app-specific* public key.
 *
 * Instead of just storing the entire literal string here embedded in the
 * program,  construct the key at runtime from pieces or
 * use bit manipulation (for example, XOR with some other string) to hide
 * the actual key.  The key itself is not secret information, but we don't
 * want to make it easy for an attacker to replace the public key with one
 * of their own and then fake messages from the server.
 */
