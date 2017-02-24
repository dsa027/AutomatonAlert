package com.aldersonet.automatonalert.Activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.aldersonet.automatonalert.ActionBar.ActionBarDrawer;
import com.aldersonet.automatonalert.ActionMode.ContactListInfo;
import com.aldersonet.automatonalert.Activity.ContactFreeFormListActivity.FragmentTypeCL;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Fragment.ContactActiveFragment;
import com.aldersonet.automatonalert.Fragment.RTChooserFragment;
import com.aldersonet.automatonalert.Fragment.RTUpdateFragment;
import com.aldersonet.automatonalert.Fragment.VolumeChooserFragment;
import com.aldersonet.automatonalert.Preferences.RTSettingsFragment;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Util.Utils;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;

public class RTUpdateActivity extends AppCompatActivity
implements
		IActivityRefresh,
		RTUpdateFragment.IRTUpdateFragmentListener,
		RTChooserFragment.IRTChooserFragmentListener,
		RTSettingsFragment.IRTSettingsFragmentListener {

	public static final String TAG                  = "RTUpdateActivity";
	public static final String TAG_FRAGMENT_TYPE    = "sourceType";


	public enum FragmentTypeRT {
		TEXT, PHONE, EMAIL, SETTINGS
	}

	public static final String DEFAULT_TEXT_RT    = "DefaultTextRT";
	public static final String DEFAULT_NEW_RT     = "DefaultNewRT";
	public static final String DEFAULT_RINGTONE   = "DefaultRingtone";
	public static final String DEFAULT_VOLUME     = "DefaultVolume";
	public static final String GENERAL_SETTINGS   = "GeneralSettings";
	public static final String RINGTONE_SETTINGS  = "RingtoneSettings";
	public static final String FREEFORM_FRAGMENT  = "FreeFormRTUpdateFragment";
	public static final String UNKNOWN_FRAGMENT   = "UnknownFragmentName";
	private static final String RT_CHOOSER        = RTChooserFragment.class.toString();
	private static final String VOL_CHOOSER       = VolumeChooserFragment.class.toString();
	public static final int NUM_PAGE_VIEWS        = FragmentTypeRT.values().length;

    String mLookupKey;
    String mDisplayName;
    String mFragmentTypeToDisplay;
	ViewPager mViewPager;
	RingtoneUpdatePagerAdapter mPagerAdapter;
	@SuppressLint("UseSparseArrays")
	public static final HashMap<Integer, Fragment> mFragmentList =
			new HashMap<Integer, Fragment>(NUM_PAGE_VIEWS);
	ContactListInfo mContactListInfo;

    ActionBarDrawer mActionBarDrawer;

	public boolean[] mIsInitialErrorCheckDone =
			new boolean[NUM_PAGE_VIEWS];

	Tab mTextTab;
	Tab mPhoneTab;
	Tab mEmailTab;
	Tab mSettingsTab;

	{
		Arrays.fill(mIsInitialErrorCheckDone, false);
	}


    @Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mActionBarDrawer != null) {
			mActionBarDrawer.getDrawerToggle().onConfigurationChanged(newConfig);
		}
	    setTabIcons();
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

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mActionBarDrawer.getDrawerToggle().syncState();
	}

	private void setDrawer() {
		mActionBarDrawer = new ActionBarDrawer(this);
	}

	public boolean isThisFragmentShowingNow(FragmentTypeRT inFragmentType) {
		if (inFragmentType == null) return false;

//		if (BuildConfig.DEBUG) {
//			boolean equal = mViewPager.getCurrentItem() == inFragmentType.ordinal();
//			Log.d(TAG + ".isThisFragmentShowingNow()",
//					"shown[" + Enums.getEnum(
//							mViewPager.getCurrentItem(), FragmentTypeRT.values(), null)
//					+ "]==?("+equal+") [" + inFragmentType + "]");
//		}

		return mViewPager.getCurrentItem() == inFragmentType.ordinal();
	}

	public Fragment getFragmentInView() {
		Fragment fragment = null;
		int idx = mViewPager.getCurrentItem();

		if (mFragmentList.containsKey(idx)) {
			fragment = mFragmentList.get(idx);

//			if (BuildConfig.DEBUG) {
//				FragmentTypeRT fragmentType = null;
//				if (fragment instanceof RTSettingsFragment) {
//					fragmentType = FragmentTypeRT.SETTINGS;
//				} else {
//					fragmentType = ((RTUpdateFragment) fragment).mFragmentType;
//				}
//				Log.d(TAG + ".getFragmentInView()",
//						"in view[" + fragmentType + "]");
//			}
		}

		return fragment;
	}

	@Override
	protected void onStop() {
		super.onStop();
//		mFragmentList.clear();
//		mRTDefaultTextFragment = null;
//		mRTDefaultNewFragment = null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rt_update_activity);
		mFragmentList.clear();
		Intent intent = getIntent();
		setDataFromIntent(intent);

		// ViewPager and its adapters use support library
		// fragments, so use getSupportFragmentManager.
		mPagerAdapter = new RingtoneUpdatePagerAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mPagerAdapter);
//		mViewPager.setOffscreenPageLimit(0);
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				// When swiping between pages, select the
				// corresponding tab.
				getSupportActionBar().setSelectedNavigationItem(position);
				// Because the fragment has already done onStart() and onResume()
				// (because ViewPager caches pages), we need to force an error
				// check when each page is shown.
				initialFragmentCheck(position);
				// get rid of any chooser that's showing
				endChooserIfAny();
				// set ActionBar title for ViewPager fragments
				setActionBarTitle(position);
			}
		});

		// Show the Up button in the action bar.
		setActionBar();
		setDrawer();

		// go to the tab as passed in Intent
		int i = -1;
		for (FragmentTypeRT s : FragmentTypeRT.values()) {
			++i;
			if (mFragmentTypeToDisplay.equals(s.name())) {
				getSupportActionBar().setSelectedNavigationItem(i);
				initialFragmentCheck(i);
				break;
			}
		}
	}

	private void setActionBarTitle(int position) {
		ActionBar ab = getSupportActionBar();
		Fragment fragment = mFragmentList.get(position);

		if (fragment != null) {
			if (fragment instanceof RTUpdateFragment) {
				if (isThisFragmentShowingNow(((RTUpdateFragment)fragment).mFragmentType)) {
					Utils.setActionBarTitle(ab, mDisplayName);
				}
			}
			else if (fragment instanceof RTSettingsFragment) {
				if (isThisFragmentShowingNow(FragmentTypeRT.SETTINGS)) {
					Utils.setActionBarTitle(ab, "General Ringtone Settings");
				}
			}
		}
	}

	private void initialFragmentCheck(int position) {
		// if the fragment at position is not RTUpdateSettings,
		// check for errors in the fragment here since it may have
		// already done onResume() while it wasn't actually on screen
		Fragment fragment = mFragmentList.get(position);
		if (fragment != null
				&& fragment instanceof RTUpdateFragment) {
			// easy access
			RTUpdateFragment rtFragment = (RTUpdateFragment)fragment;
			// do error check if we haven't yet
			Log.d(TAG + ".checkFragmentForErrorsSince...()", "calling initialChecks()");
			rtFragment.initialChecks();
		}
	}

	private void setDataFromIntent(Intent intent) {
		mLookupKey = intent.getStringExtra(Contacts.LOOKUP_KEY);
		mDisplayName = intent.getStringExtra(Contacts.DISPLAY_NAME);

		mFragmentTypeToDisplay = intent.getStringExtra(TAG_FRAGMENT_TYPE);
		if (mFragmentTypeToDisplay == null) {
			mFragmentTypeToDisplay = FragmentTypeRT.TEXT.name();
		}

		if (mLookupKey == null) {
			finish();
			mLookupKey = "";
		}
		if (mDisplayName == null) {
			mDisplayName = "<unknown>";
		}

		mContactListInfo = ContactListInfo.getContacts().get(mLookupKey);
		if (mContactListInfo == null) {
			mContactListInfo = new ContactListInfo(mLookupKey, mDisplayName, false, null);
		}
	}

	/* get all fragments this Activity is managing and
	 send back the list
	  */
	public static HashMap<Integer, Fragment> getFragmentList() {
		// getInstance with FragmentTypeRT fragments
		@SuppressLint("UseSparseArrays")
		HashMap<Integer, Fragment> fragmentList =
				new HashMap<Integer, Fragment>(mFragmentList);

		// add Global Text fragment
		if (AutomatonAlert.THIS.mRTDefaultTextFragment != null) {
			fragmentList.put(9999, AutomatonAlert.THIS.mRTDefaultTextFragment);
		}
		// add Default for New fragment
		if (AutomatonAlert.THIS.mRTDefaultNewFragment != null) {
			fragmentList.put(9998, AutomatonAlert.THIS.mRTDefaultNewFragment);
		}
		// add FreeForm
		if (AutomatonAlert.THIS.mRTFreeFormFragment != null) {
			fragmentList.put(9997, AutomatonAlert.THIS.mRTFreeFormFragment);
		}

		return fragmentList;
	}

	public class RingtoneUpdatePagerAdapter extends FragmentPagerAdapter {

	    public RingtoneUpdatePagerAdapter(FragmentManager fm) {
	        super(fm);
	    }

		@Override
	    public Fragment getItem(int position) {
			Fragment fragment = null;
			FragmentTypeRT sourceType =
					FragmentTypeRT.values()[position];

			switch(position) {
			case 0:	// SMS/MMS
			case 1:	// Call
			case 2: // Email
				fragment =
						RTUpdateFragment.newInstance(
								RTUpdateFragment.Mode.RINGTONE,
								sourceType,
								mDisplayName,
								-1,
								-1);
				break;
			case 3: // Settings
			default:
				fragment = RTSettingsFragment.newInstance(null);
				break;
			}

			mFragmentList.put(position, fragment);
			return fragment;
	    }

		@Override
	    public int getCount() {
	        return NUM_PAGE_VIEWS;
	    }

	}

	private void setTabIcons() {
		Resources res = getResources();

		int textIcon = (mContactListInfo.hasText()) ?
				R.drawable.android_messages_blue_64
				: R.drawable.android_messages_grey_64;
		int phoneIcon = (mContactListInfo.hasPhone()) ?
				R.drawable.android_phone_blue_64
				: R.drawable.android_phone_grey_64;
		int emailIcon = (mContactListInfo.hasEmail()) ?
				R.drawable.android_email_blue_blue_64
				: R.drawable.android_email_blue_grey_64;

		mTextTab.setIcon(res.getDrawable(textIcon));
		mPhoneTab.setIcon(res.getDrawable(phoneIcon));
		mEmailTab.setIcon(res.getDrawable(emailIcon));
		mSettingsTab.setIcon(res.getDrawable(R.drawable.ic_action_settings));
	}

	private ActionBar setActionBar() {
		ActionBar ab = getSupportActionBar();
		if (ab != null) {
			Resources res = getResources();

			Utils.setActionBarCommon(res, ab, mDisplayName);

			mTextTab = ab.newTab()
					.setTabListener(new TabListener<RTUpdateFragment>(
							this,
							FragmentTypeRT.TEXT.name(),
							mViewPager));
			mPhoneTab = ab.newTab()
					.setTabListener(new TabListener<RTUpdateFragment>(
							this,
							FragmentTypeRT.PHONE.name(),
							mViewPager));
			mEmailTab = ab.newTab()
					.setTabListener(new TabListener<RTUpdateFragment>(
							this,
							FragmentTypeRT.EMAIL.name(),
							mViewPager));
			mSettingsTab = ab.newTab()
					.setTabListener(new TabListener<RTSettingsFragment>(
							this,
							FragmentTypeRT.SETTINGS.name(),
							mViewPager));

			setTabIcons();

			ab.addTab(mTextTab);
			ab.addTab(mPhoneTab);
			ab.addTab(mEmailTab);
			ab.addTab(mSettingsTab);
		}
		return ab;
	}

	public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
	    private final ViewPager mPager;

	    public TabListener(Activity activity, String tag, ViewPager pager) {
	        mPager = pager;
	    }

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction arg1) {
	        mPager.setCurrentItem(tab.getPosition());
		}

		@Override
		public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
		}

		@Override
		public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
		}

	}

	private boolean endChooserIfAny() {
		boolean fragmentEnded = false;

		// re-enable the preference list
		RTSettingsFragment fragment =
				(RTSettingsFragment)mFragmentList.get(
						FragmentTypeRT.SETTINGS.ordinal());
		if (fragment != null) {
			fragment.enablePreferenceList(true/*enabled*/, false/*bgGone*/);
		}

		// end all this Activity's fragments
    	if ((getChooserFragment(RT_CHOOSER)) != null) {
    		endFragmentInteraction(RT_CHOOSER);
    		fragmentEnded = true;
    	}
    	if (getChooserFragment(VOL_CHOOSER) != null) {
    		endFragmentInteraction(VOL_CHOOSER);
    		fragmentEnded = true;
    	}
    	if (getChooserFragment(DEFAULT_TEXT_RT) != null) {
    		endFragmentInteraction(DEFAULT_TEXT_RT);
    		fragmentEnded = true;
    	}
    	if (getChooserFragment(DEFAULT_NEW_RT) != null) {
    		endFragmentInteraction(DEFAULT_NEW_RT);
    		fragmentEnded = true;
    	}
    	if (getChooserFragment(DEFAULT_RINGTONE) != null) {
    		endFragmentInteraction(DEFAULT_RINGTONE);
    		fragmentEnded = true;
    	}
    	if (getChooserFragment(DEFAULT_VOLUME) != null) {
    		endFragmentInteraction(DEFAULT_VOLUME);
    		fragmentEnded = true;
    	}
    	if (getChooserFragment(GENERAL_SETTINGS) != null) {
    		endFragmentInteraction(GENERAL_SETTINGS);
    		fragmentEnded = true;
    	}
    	if (getChooserFragment(RINGTONE_SETTINGS) != null) {
    		endFragmentInteraction(RINGTONE_SETTINGS);
    		fragmentEnded = true;
    	}
    	if (getChooserFragment(UNKNOWN_FRAGMENT) != null) {
    		endFragmentInteraction(UNKNOWN_FRAGMENT);
    		fragmentEnded = true;
    	}

    	return fragmentEnded;
	}

	private Fragment getChooserFragment(String tag) {

		return getSupportFragmentManager().findFragmentByTag(tag);
	}

	private void endFragmentInteraction(String name) {

		// get rid of the chooser
		findViewById(R.id.fru_list_frame_background).setVisibility(FrameLayout.GONE);

		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		Fragment chooserFragment = fm.findFragmentByTag(name);
		ft.remove(chooserFragment);
		fm.popBackStack();
		ft.commit();
	}

	@Override
	public boolean onKeyUp(int keyCode, @NotNull KeyEvent event) {
		Intent intent = getIntent();
		intent.putExtra(Contacts.LOOKUP_KEY, mLookupKey);
		setResult(ContactActiveFragment.RT_REQUEST_CODE, intent);

		// if RTChooserFragment is attached, get rid of it
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	    	if (endChooserIfAny()) {
	    		return false;
	    	}
	    }
	    else if (keyCode == KeyEvent.KEYCODE_MENU) {
			mActionBarDrawer.openDrawer();
			return true;
		}
	    // otherwise pass through
		super.onKeyUp(keyCode, event);
		return true;
	}

	@Override
	public String getLookupKeyCallback() {
		return mLookupKey;
	}

	@Override
	public void updateRingtone(
			Dialog dialog, Fragment fragment,
			String sourceType, String song, Uri uri) {

		// send data back to the fragment
		if (fragment != null) {
			if (fragment instanceof RTUpdateFragment) {
				RTUpdateFragment ruf = (RTUpdateFragment)fragment;
				ruf.setRingtone(song, uri, false);
			}
			else if (fragment instanceof RTSettingsFragment) {
				RTSettingsFragment rsf = (RTSettingsFragment)fragment;
				rsf.setRingtone(uri);
				rsf.enablePreferenceList(true/*enabled*/, false/*bgGone*/);
			}
		}
		if (dialog != null) {
			dialog.dismiss();
		}
	}

	public void refreshFragments(boolean[] doIts, FragmentTypeCL[] types) {
		// nothing to refresh!
	}
}
