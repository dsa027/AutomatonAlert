package com.aldersonet.automatonalert.Activity;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.aldersonet.automatonalert.ActionBar.ActionBarDrawer;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Filter.FilterItemDO;
import com.aldersonet.automatonalert.Fragment.BackUpRestoreFragment;
import com.aldersonet.automatonalert.Fragment.FreeFormMasterFragment;
import com.aldersonet.automatonalert.Fragment.RTChooserFragment;
import com.aldersonet.automatonalert.Fragment.RTUpdateFragment;
import com.aldersonet.automatonalert.Preferences.RTSettingsFragment;
import com.aldersonet.automatonalert.Preferences.SettingsFragment;
import com.aldersonet.automatonalert.Preferences.SettingsFragment.ShowMode;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Util.Utils;

public class FragmentHostActivity extends AppCompatActivity
implements
		RTUpdateFragment.IRTUpdateFragmentListener,
		RTSettingsFragment.IRTSettingsFragmentListener,
		RTChooserFragment.IRTChooserFragmentListener,
		BackUpRestoreFragment.IBackUpRestoreFragmentListener {

    private static final String TAG = "FragmentHostActivity";

	public enum HostFragmentType {
		GLOBAL_RINGTONE,
		NEW_RINGTONE_VALUES,
		DEFAULT_RINGTONE,
		DEFAULT_VOLUME,
		GENERAL_SETTINGS,
		RINGTONE_SETTINGS,
		FREEFORM,
		BACKUP_RESTORE
	}


    private HostFragmentType mFragmentType;

    private static final String ACTON_BAR_TITLE = "Settings";

    private ActionBarDrawer mActionBarDrawer;
    private Fragment mFragment;
    private Intent mActivityIntent;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_host_activity);
		mActivityIntent = getIntent();

		setFragmentType();
		setActionBar();
		setDrawer();

//		if (savedInstanceState != null) {
//			getAllFromBundle(savedInstanceState);
//		}

		showFragment();
	}

	private void setFragmentType() {
		String sFragmentType = null;

		if (mActivityIntent != null
				&& mActivityIntent.getExtras() != null) {
			Bundle bundle = mActivityIntent.getExtras();
			for (String s : bundle.keySet()) {
				Log.d(TAG, ".setFragmentType(): " + s + "[" + bundle.get(s) + "]");
			}
			sFragmentType =
					mActivityIntent.getStringExtra(AutomatonAlert.FRAGMENT_TYPE);
		}

		if (sFragmentType == null) {
			mFragmentType = HostFragmentType.GENERAL_SETTINGS;
		}

		try {
			mFragmentType = HostFragmentType.valueOf(sFragmentType);
		} catch (IllegalArgumentException | NullPointerException ignored) {}

    }

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mActionBarDrawer != null) {
			mActionBarDrawer.getDrawerToggle().onConfigurationChanged(newConfig);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.contact_list_action_bar_menu, menu);
		// put ProgressBar on the end
		menu.getItem(0).setVisible(true);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mActionBarDrawer != null) {
			if (mActionBarDrawer.getDrawerToggle().onOptionsItemSelected(item)) {
				return true;
			}
		}
		if (item.getItemId() == R.id.refresh) {
			Fragment fragment =
					getSupportFragmentManager().findFragmentByTag(
							HostFragmentType.BACKUP_RESTORE.name());
			if (fragment != null
					&& fragment instanceof BackUpRestoreFragment) {
				BackUpRestoreFragment brFragment = (BackUpRestoreFragment)fragment;
				brFragment.refresh();
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

//	private Bundle getAllFromBundle(final Bundle bundle) {
//		return bundle;
//	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			mActionBarDrawer.openDrawer();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    boolean ret = super.onKeyDown(keyCode, event);
		boolean finish = false;

		// if we're in SettingsFragment, process back as
		// a logical back (not backStack cuz it ain't there)
		// take us back one logical screen
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			switch (mFragmentType) {
				case GENERAL_SETTINGS:
					ret = processGeneralSettingsKeycodeBack();
					finish = true;
					break;
				case GLOBAL_RINGTONE:
					break;
				case RINGTONE_SETTINGS:
					break;
			}
	    }
		if (finish) {
			finish();
		}
		return ret;
	}

	private boolean processGeneralSettingsKeycodeBack() {
		ShowMode showMode = ((SettingsFragment) mFragment).getmShowMode();

		// from ACCOUNT to ACCOUNT_LIST
		if (showMode.equals(ShowMode.ACCOUNT)) {
			SettingsFragment.callSelf(
					getApplicationContext(), ShowMode.ACCOUNT_LIST, null);
			return false;
		}
		// from ACCOUNT_LIST or DEVELOPER to GENERAL
		else if (showMode.equals(ShowMode.ACCOUNT_LIST)
				|| showMode.equals(ShowMode.DEVELOPER)) {
			SettingsFragment.callSelf(
					getApplicationContext(), ShowMode.GENERAL, null);
			return false;
		}
		return true;
	}

	private void setActionBar() {
		ActionBar ab = getSupportActionBar();
		if (ab != null) {
			Utils.setActionBarCommon(getResources(), ab, ACTON_BAR_TITLE);
			ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		}
	}

    private void showFragmentGlobalRingtone() {
        mFragment = RTUpdateFragment.newInstance(
                RTUpdateFragment.Mode.DEFAULT_TEXT_RT,
                FragmentTypeRT.TEXT,
                "",
                -1,
                -1);
        AutomatonAlert.THIS.mRTDefaultTextFragment =
                (RTUpdateFragment)mFragment;
        Utils.setActionBarTitle(
                getSupportActionBar(), "Global Text Ringtone");
    }

    private void showFragmentNewRingtoneValues() {
        mFragment = RTUpdateFragment.newInstance(
                RTUpdateFragment.Mode.DEFAULT_NEW_RT,
                FragmentTypeRT.TEXT,
                "",
                -1,
                -1);
        AutomatonAlert.THIS.mRTDefaultNewFragment =
                (RTUpdateFragment)mFragment;
        Utils.setActionBarTitle(
                getSupportActionBar(), "Initial Values for New Ringtones");
    }

    private void showFragmentDefaultRingtone() {
        mFragment = RTSettingsFragment.newInstance(
                HostFragmentType.DEFAULT_RINGTONE);
        Utils.setActionBarTitle(
                getSupportActionBar(), "General Ringtone Settings");
    }

    private void showFragmentDefaultVolume() {
        mFragment = RTSettingsFragment.newInstance(
                HostFragmentType.DEFAULT_VOLUME);
        Utils.setActionBarTitle(
                getSupportActionBar(), "General Ringtone Settings");
    }

    private void showFragmentGeneralSettings() {
        String sShowMode = mActivityIntent.getStringExtra(SettingsFragment.TAG_SHOW_MODE);
        String sKey = mActivityIntent.getStringExtra(AutomatonAlert.ACCOUNT_KEY);
        ShowMode showMode = SettingsFragment.getShowMode(sShowMode);
        mFragment = SettingsFragment.newInstance(showMode, sKey);
        Utils.setActionBarTitle(
                getSupportActionBar(), "Settings");
    }

    private void showFragmentRingtoneSettings() {
        mFragment = RTSettingsFragment.newInstance(null);
        Utils.setActionBarTitle(
                getSupportActionBar(), "General Ringtone Settings");
    }

    private void showFragmentFreeform(FragmentTransaction ft) {
        mFragment = null;
        int filterItemId =
                mActivityIntent.getIntExtra(
                        FilterItemDO.TAG_FILTER_ITEM_ID, -1);
        // DETAIL: RTUpdateFragment
        RTUpdateFragment rtUpdateFragment =
                RTUpdateFragment.newInstance(
                        RTUpdateFragment.Mode.FREEFORM,
                        FragmentTypeRT.EMAIL,
                        "",
                        -1,
                        filterItemId);
        // MASTER: FreeFormMasterFragment
        FreeFormMasterFragment freeFormMasterFragment =
                FreeFormMasterFragment.newInstance(filterItemId);

        freeFormMasterFragment.setRTListener(rtUpdateFragment);
        rtUpdateFragment.setMaster(freeFormMasterFragment);

        // order matters. need to getInstance RTUpdateFragment
        // args before FreeFormRTUpdateFragment accesses them
        ft.add(
                R.id.fha_middle_frame,
                rtUpdateFragment,
                getFragmentName(2));
        ft.add(
                R.id.fha_top_frame,
                freeFormMasterFragment,
                getFragmentName(1));
        ft.commit();

        AutomatonAlert.THIS.mRTFreeFormFragment = rtUpdateFragment;
        Utils.setActionBarTitle(
                getSupportActionBar(), "Free-Form Text/Email Alert");
    }

    private void showFragmentBackupRestore() {
        mFragment = BackUpRestoreFragment.newInstance();
        Utils.setActionBarTitle(
                getSupportActionBar(), "Backup/Restore");
    }

	private void showFragment() {
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();

		switch (mFragmentType) {
			case GLOBAL_RINGTONE:
                showFragmentGlobalRingtone();
				break;

			case NEW_RINGTONE_VALUES:
                showFragmentNewRingtoneValues();
				break;

			case DEFAULT_RINGTONE:
                showFragmentDefaultRingtone();
				break;

			case DEFAULT_VOLUME:
                showFragmentDefaultVolume();
				break;

			case GENERAL_SETTINGS:
                showFragmentGeneralSettings();
				break;

			case RINGTONE_SETTINGS:
                showFragmentRingtoneSettings();
				break;

			case FREEFORM:
                showFragmentFreeform(ft);
				break;

			case BACKUP_RESTORE:
                showFragmentBackupRestore();
				break;
		}

		if (mFragment != null) {
			ft.add(
					R.id.fha_top_layout,
					mFragment,
					getFragmentName(0));
			ft.commit();
		}
	}

	private String getFragmentName(int topMiddleBottom) {
		if (mFragmentType == null) {
			return "FragmentTypeIsNull";
		}
		switch (mFragmentType) {
			case GLOBAL_RINGTONE:
				return RTUpdateActivity.DEFAULT_TEXT_RT;
			case DEFAULT_RINGTONE:
				return RTUpdateActivity.DEFAULT_RINGTONE;
			case DEFAULT_VOLUME:
				return RTUpdateActivity.DEFAULT_VOLUME;
			case GENERAL_SETTINGS:
				return RTUpdateActivity.GENERAL_SETTINGS;
			case RINGTONE_SETTINGS:
				return RTUpdateActivity.RINGTONE_SETTINGS;
			case FREEFORM:
				if (topMiddleBottom == 1) {
					return RTUpdateActivity.FREEFORM_FRAGMENT;
				}
				else {
					return RTUpdateActivity.DEFAULT_RINGTONE;
				}
			case BACKUP_RESTORE:
				return mFragmentType.name();

			default:
				return RTUpdateActivity.UNKNOWN_FRAGMENT;
		}
	}

	@Override
	public String getLookupKeyCallback() {
		return null;
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
}

