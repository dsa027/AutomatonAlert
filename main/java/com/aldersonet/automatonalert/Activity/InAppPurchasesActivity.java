package com.aldersonet.automatonalert.Activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.aldersonet.automatonalert.ActionBar.ActionBarDrawer;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Billing.Util.IabResult;
import com.aldersonet.automatonalert.InAppPurchases.InAppPurchases;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Util.Utils;

import org.jetbrains.annotations.NotNull;

public class InAppPurchasesActivity extends AppCompatActivity
		implements InAppPurchases.IInAppPurchaseListener {

	public static final String TAG = "InAppPurchasesActivity";

	TextView mUpgrade1Purchase;
	TextView mUpgrade2Purchase;

	ActionBarDrawer mActionBarDrawer;

	InAppPurchases mInAppPurchases;

    @Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mActionBarDrawer != null) {
			mActionBarDrawer.getDrawerToggle().onConfigurationChanged(newConfig);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mActionBarDrawer != null) {
			if (mActionBarDrawer.getDrawerToggle().onOptionsItemSelected(item)) {
				return true;
			}
		}

		return super.onOptionsItemSelected(item);
	}

	private void setDrawer() {
		mActionBarDrawer = new ActionBarDrawer(this);
	}

	@Override
	public boolean onKeyUp(int keyCode, @NotNull KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			mActionBarDrawer.openDrawer();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.in_app_purchases);

		Utils.setActionBarCommon(
				getResources(), getSupportActionBar(), "Purchase Options");
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		setDrawer();

		setViewPointers();
		setListeners();
	}

	@Override
	protected void onStart() {
		mInAppPurchases = InAppPurchases.getInstance(this, this);

		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if (mInAppPurchases != null) {
			mInAppPurchases.release();
			mInAppPurchases = null;
		}
		super.onDestroy();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mActionBarDrawer.getDrawerToggle().syncState();
	}

	private void setViewPointers() {
		mUpgrade1Purchase = (TextView)findViewById(R.id.iap_upgrade1_status);
		mUpgrade2Purchase = (TextView)findViewById(R.id.iap_upgrade2_status);
	}

	private void populateInventoryFields() {
		Resources res = getResources();

		if (AutomatonAlert.mDefunctUpgrade) {
			mUpgrade1Purchase.setText(res.getString(R.string.purchased_label));
			mUpgrade1Purchase.setBackgroundResource(0);
		}
		else {
//			mUpgrade1CheckBox.setVisibility(ImageView.INVISIBLE);
//			mUpgrade1Purchase.setText("Purchase");
//			mUpgrade1Purchase.setBackgroundResource(R.drawable.billing_button_frame);
			mUpgrade1Purchase.setText(res.getString(R.string.currently_newline_unavailable_label));
			mUpgrade1Purchase.setBackgroundResource(0);
		}

		if (AutomatonAlert.mIntroductoryUnlimitedUpgrade
				|| AutomatonAlert.hasDevelopersCode()) {
			mUpgrade2Purchase.setText(res.getString(R.string.purchased_label));
			mUpgrade2Purchase.setTextColor(
                    getResources().getColor(android.R.color.holo_blue_dark));
			mUpgrade2Purchase.setTypeface(null, Typeface.BOLD);
			mUpgrade2Purchase.setBackgroundResource(0);
		}
		else {
			mUpgrade2Purchase.setText(res.getString(R.string.purchase_uc_label));
			mUpgrade2Purchase.setTextColor(Color.WHITE);
			mUpgrade2Purchase.setTypeface(null, Typeface.NORMAL);
			mUpgrade2Purchase.setBackgroundResource(R.drawable.billing_button_frame);
		}
	}

	private void makePurchase(String sku) {
		try {
			if (mInAppPurchases == null) {
				throw new IllegalStateException(
						"In-App Purchases is null, can't purchase");
			}
			mInAppPurchases.makePurchase(sku);
		}
		catch (IllegalStateException e) {
			InAppPurchases.showProblemToast(
                    getApplicationContext(), InAppPurchases.PURCHASE_PROBLEM);
			Log.d(
					TAG, ".onClick(): Purchase failed " + sku + "; IllegalStateException: "
							+ e.getMessage());
		}
	}

	private void setListeners() {
		mUpgrade1Purchase.setOnClickListener(new TextView.OnClickListener() {
             @Override
             public void onClick(View v) {
                 checkPattern("1");
                 if (AutomatonAlert.mDefunctUpgrade) {
                 }
//				makePurchase(InAppPurchases.DEFUNCT_UPGRADE_SKU);
             }
         });

		mUpgrade2Purchase.setOnClickListener(new TextView.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPattern("2");
                if (AutomatonAlert.mIntroductoryUnlimitedUpgrade
                        || AutomatonAlert.hasDevelopersCode()) {
                    return;
                }
                makePurchase(InAppPurchases.INTRODUCTORY_UNLIMITED_UPGRADE_SKU);
            }
		});
	}

	// can only be "1" or "2"; a "9" invalidates (for release)
//	private static final String[] mPattern = {"9", "1", "2", "2", "1", "1", "1"};
//	private static int mIdx = -1;

	private void checkPattern(String which) {
//		if (mIdx+1 < mPattern.length) {
//			if (mPattern[mIdx+1].equals(which)) {
//				mIdx++;
//				if (mIdx >= mPattern.length-1) {
//					Toast.makeText(this, "consuming upgrade2", Toast.LENGTH_SHORT).show();
//					mInAppPurchases.consumePurchases();
//				}
//			}
//			else {
//				mIdx = -1;
//			}
//		}
//		else {
//			mIdx = -1;
//		}
	}

	@Override
	public void onInventoryReady(IabResult result) {
		populateInventoryFields();
	}

	@Override
	public void onPurchaseFinished(IabResult result) {
        mInAppPurchases.getPlayInventory();
	}

	@Override
	/* forced hack...helper doesn't call purchases' listener */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		mInAppPurchases.mIabHelper.handleActivityResult(requestCode, resultCode, data);
	}
}
