package com.aldersonet.automatonalert.InAppPurchases;

import android.app.Activity;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Billing.Util.IabHelper;
import com.aldersonet.automatonalert.Billing.Util.IabResult;
import com.aldersonet.automatonalert.Billing.Util.Inventory;
import com.aldersonet.automatonalert.Billing.Util.Purchase;
import com.aldersonet.automatonalert.Billing.Verify;

public class InAppPurchases {

    public static final String TAG = "InAppPurchases";

    private static final String DEFUNCT_UPGRADE_SKU =
            "initial_upgrade_add_contacts_and_emails";
    public static final String INTRODUCTORY_UNLIMITED_UPGRADE_SKU =
            "second_upgrade_unlimited_contacts_and_emails";

    public static final String PURCHASE_PROBLEM =
            "There was a problem with your purchase, please try again later";
    private static final String SETUP_PROBLEM =
            "There was a server communication problem, please try again later";
    private static final String PURCHASE_SUCCESSFUL = "Item purchased";

    public static final int FREE_VERSION_NUM_ACCOUNTS_ALLOWED = 1;
    public static final int FREE_VERSION_NUM_ACTIVE_CONTACTS_ALLOWED = 3;
    public static final int FREE_VERSION_NUM_FILTER_ITEMS_ALLOWED = 2;

    public static final int DEFUNCT_UPGRADE_VERSION_NUM_ACCOUNTS_ALLOWED = 5;
    public static final int DEFUNCT_UPGRADE_VERSION_NUM_ACTIVE_CONTACTS_ALLOWED = 25;
    public static final int DEFUNCT_UPGRADE_VERSION_NUM_FILTER_ITEMS_ALLOWED = 25;

    private static final int DEFUNCT_UPGRADE_UPGRADE1_REQ_CODE = 10001;
    private static final int INTRODUCTORY_UNLIMITED_UPGRADE_REQ_CODE = 10002;

    public IabHelper mIabHelper;
    private Inventory mInventory;

    private Context mContext;
    private IInAppPurchaseListener mListener;
    private boolean mConsume;

    public static InAppPurchases getInstance(Context context, IInAppPurchaseListener listener) {
        return new InAppPurchases(context, listener);
    }

    private InAppPurchases(Context context, IInAppPurchaseListener listener) {
        mContext = context;
        mListener = listener;

        getInventory();
    }

    private void getInventory() {
        // don't crash if we can't get Play Services Inventory
        try {
            if (mIabHelper == null) {
                setPlayServices();
            }
        } catch (Exception e) {
            mInventory = null;
        }
    }

    private int getReqCode(String sku) {
        if (sku.equals(DEFUNCT_UPGRADE_SKU)) {
            return DEFUNCT_UPGRADE_UPGRADE1_REQ_CODE;
        }
        if (sku.equals(INTRODUCTORY_UNLIMITED_UPGRADE_SKU)) {
            return INTRODUCTORY_UNLIMITED_UPGRADE_REQ_CODE;
        }

        return 1;
    }

    private String getUniqueData(String sku) {
        if (sku.equals(DEFUNCT_UPGRADE_SKU)) {
            return Settings.Secure.ANDROID_ID + DEFUNCT_UPGRADE_SKU;
        }
        if (sku.equals(INTRODUCTORY_UNLIMITED_UPGRADE_SKU)) {
            return Settings.Secure.ANDROID_ID + INTRODUCTORY_UNLIMITED_UPGRADE_SKU;
        }

        return "I screwed up somewhere";
    }

    public boolean makePurchase(String sku) {
        if (mListener == null) {
            showProblemToast(mContext, PURCHASE_PROBLEM);
            Log.e(
                    TAG, ".makePurchase("+sku+"): Listener is null");
            return false;
        }

        try {
            mIabHelper.launchPurchaseFlow(
                    (Activity)mListener,
                    sku,
                    getReqCode(sku),
                    mPurchaseFinishedListener,
                    getUniqueData(sku));
        } catch (IabHelper.IabAsyncInProgressException e) {
            showProblemToast(mContext, PURCHASE_PROBLEM);
            Log.e(
                    TAG, ".makePurchase("+sku+")" + "Unable to launchPurchaseFlow()");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener =
            new IabHelper.OnIabPurchaseFinishedListener() {

                @Override
                public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
                    boolean ok = false;

                    if (result.isFailure()) {
                        Log.d(
                                TAG, ".setPlayServices(): " +
                                "Failed with result[" + result.getResponse() + "]: "
                                        + result.getMessage());
                        ok = false;
                    }
                    else if (purchase.getSku().equals(DEFUNCT_UPGRADE_SKU)) {
                        if ((ok = checkPurchase(purchase))) {
                            AutomatonAlert.mDefunctUpgrade = true;
                        }
                    }
                    else if (purchase.getSku().equals(INTRODUCTORY_UNLIMITED_UPGRADE_SKU)) {
                        if ((ok = checkPurchase(purchase))) {
                            AutomatonAlert.mIntroductoryUnlimitedUpgrade = true;
                        }
                    }

                    if (!ok) {
                        showProblemToast(mContext, PURCHASE_PROBLEM);
                    }
                    else {
                        showSuccessToast(mContext);
                    }

                    if (mListener != null) {
                        mListener.onPurchaseFinished(result);
                    }
                }
            };

    public static void showProblemToast(Context context, String problem) {
        Toast.makeText(context, problem, Toast.LENGTH_SHORT).show();
    }

    private static void showSuccessToast(Context context) {
        Toast.makeText(context, PURCHASE_SUCCESSFUL, Toast.LENGTH_SHORT).show();
    }

    private boolean checkPurchase(Purchase purchase) {
        if (purchase.getSku().equals(DEFUNCT_UPGRADE_SKU)) {
            if (!purchase.getDeveloperPayload().equals(
                    Settings.Secure.ANDROID_ID + DEFUNCT_UPGRADE_SKU)) {
                return false;
            }
        }
        else if (purchase.getSku().equals(INTRODUCTORY_UNLIMITED_UPGRADE_SKU)) {
            if (!purchase.getDeveloperPayload().equals(
                    Settings.Secure.ANDROID_ID + INTRODUCTORY_UNLIMITED_UPGRADE_SKU)) {
                return false;
            }
        }

        return true;
    }

    private void setPlayServices() {
        // Google Play Services In-App Billing
        mIabHelper = new IabHelper(mContext, Verify.get());

        mIabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (result.isSuccess()) {
                    getPlayInventory();
                } else {
                    mInventory = null;
                    showProblemToast(mContext, SETUP_PROBLEM);
                    Log.e(
                            TAG, ".setPlayServices(): " +
                                    "Failed with result[" + result.getResponse() + "]: "
                                    + result.getMessage());
                }
            }
        });
    }

    private IabHelper.QueryInventoryFinishedListener mGetPlayInventoryListener =
            new IabHelper.QueryInventoryFinishedListener() {
                @Override
                public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                    AutomatonAlert.mDefunctUpgrade = false;
                    AutomatonAlert.mIntroductoryUnlimitedUpgrade = false;

                    if (result.isSuccess()) {
                        mInventory = inv;

                        Purchase upgrade1 = mInventory.getPurchase(DEFUNCT_UPGRADE_SKU);
                        if (upgrade1 != null) {
                            AutomatonAlert.mDefunctUpgrade = true;
                        }
                        Purchase upgrade2 = mInventory.getPurchase(INTRODUCTORY_UNLIMITED_UPGRADE_SKU);
                        if (upgrade2 != null) {
                            AutomatonAlert.mIntroductoryUnlimitedUpgrade = true;
                        }
                    }
                    else {
                        mInventory = null;
                    }

                    if (mListener != null) {
                        mListener.onInventoryReady(result);
                    }
                }
            };

    public void getPlayInventory() {
        try {
            mIabHelper.queryInventoryAsync(mGetPlayInventoryListener);
        } catch (IllegalStateException | IabHelper.IabAsyncInProgressException e) {
            e.printStackTrace();
        }
    }

    public void release() {
        if (mIabHelper != null) {
            try {
                mIabHelper.dispose();
                // catch "Service not registered" exception
            } catch (IllegalArgumentException | IabHelper.IabAsyncInProgressException e) {
                e.printStackTrace();
            }
        }
    }

    public interface IInAppPurchaseListener {
        void onInventoryReady(IabResult result);
        void onPurchaseFinished(IabResult result);
    }
}
