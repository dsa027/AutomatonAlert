package com.aldersonet.automatonalert.Activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.Alert.AlertItems;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Fragment.RTChooserFragment;
import com.aldersonet.automatonalert.Fragment.RTUpdateFragment;
import com.aldersonet.automatonalert.Fragment.SetAlarmMasterFragment;
import com.aldersonet.automatonalert.Fragment.VolumeChooserFragment;
import com.aldersonet.automatonalert.Preferences.RTPrefsDO;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.ActionBar.ActionBarDrawer;
import com.aldersonet.automatonalert.Picker.DatePickerTimePicker;
import com.aldersonet.automatonalert.Util.Utils;

import org.jetbrains.annotations.NotNull;

public class SetAlertActivity extends AppCompatActivity
implements
		RTUpdateFragment.IRTUpdateFragmentListener,
		RTChooserFragment.IRTChooserFragmentListener {

	public static enum Mode {
		ALARM,						// setting based on an AlertItemDO
		ALERT						// setting based on FilterItemDO
	}

	public static final int MAX_VOLUME = RTPrefsDO.UI_VOLUME_MAX;

	private static String mRTChooser = RTChooserFragment.class.toString();
	private static String mVolChooser = VolumeChooserFragment.class.toString();

	String mLookupKey;
    String mDisplayName;
    AlertItemDO mAlertItem;
    RTUpdateFragment mRTUpdateFragment;
	SetAlarmMasterFragment mSetAlarmMasterFragment;
	private DatePickerTimePicker mPicker;

	ActionBarDrawer mActionBarDrawer;

    @Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mActionBarDrawer != null) {
			mActionBarDrawer.getDrawerToggle().onConfigurationChanged(newConfig);
		}
		setActionBar();
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	for (int i=0; i<menu.size(); i++) {
    		MenuItem mi = menu.getItem(i);
    		String title = mi.getTitle().toString();
    		Spannable newTitle = new SpannableString(title);
    		newTitle.setSpan(
    				new ForegroundColorSpan(Color.BLUE),
    				0,
    				newTitle.length(),
    				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    		mi.setTitle(newTitle);
    	}
    	return true;
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//davedel -- disallow access
		finish();
		//davedel

		// fragment_host_activity has "top" and "middle"
		// views for our two fragments
		setContentView(R.layout.fragment_host_activity);
		Intent intent = getIntent();
		setDataFromIntent(intent);
		addFragment();
		// Show the Up button in the action bar.
		setActionBar();
		setDrawer();
	}

	private void addFragment() {
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();

		mRTUpdateFragment =
				RTUpdateFragment.newInstance(
						RTUpdateFragment.Mode.ALARM,
						FragmentTypeRT.TEXT,
						"Set Alarm",
						//TODO: fixed this because someone's accessing it but
						//TODO: what exactly does this show?
						(mAlertItem == null ? -1 : mAlertItem.getAlertItemId()),
						-1);

		mSetAlarmMasterFragment =
				SetAlarmMasterFragment.newInstance(
						mAlertItem.getAlertItemId());
		mSetAlarmMasterFragment.setRTListener(mRTUpdateFragment);

		mRTUpdateFragment.setMaster(mSetAlarmMasterFragment);

		// order matters. need to getInstance RTUpdateFragment
		// args before SetAlarmRTUpdateFragment accesses them
		ft.add(
				R.id.fha_middle_frame,
				mRTUpdateFragment,
				RTUpdateFragment.class.getName());
		ft.add(
				R.id.fha_top_frame,
				mSetAlarmMasterFragment,
				SetAlarmMasterFragment.class.getName());
		ft.commit();
	}

	private void setAlertItem(Intent intent) {
		int alertItemId = intent.getIntExtra(
				AlertItemDO.TAG_ALERT_ITEM_ID, -1);
		if (alertItemId != -1) {
			mAlertItem = AlertItems.get(alertItemId);
		}
	}

	public void setAlertItem(AlertItemDO alertItem) {
		mAlertItem = alertItem;
	}

	private void setLookupKey(AlertItemDO alertItem) {
		if (alertItem != null) {
			mLookupKey = alertItem.getKvRawDetails().get(Contacts.LOOKUP_KEY);
		}
		else {
			mLookupKey = "";
		}
	}

	private void setDisplayName(AlertItemDO alertItem) {
		if (alertItem != null) {
			mDisplayName = alertItem.getKvRawDetails().get(Contacts.DISPLAY_NAME);
		}
		else {
			mDisplayName = "";
		}
	}

	private void setDataFromIntent(Intent intent) {
		setAlertItem(intent);
		setLookupKey(mAlertItem);
		setDisplayName(mAlertItem);
	}

	private String getActionBarTitle() {
		String title = "";
		if (mAlertItem != null) {
			if (AlertItemDO.isSmsMms(mAlertItem.getKvRawDetails())) {
				title = mAlertItem.getKvRawDetails().get(AutomatonAlert.SMS_BODY);
			}
			else {
				title = mAlertItem.getKvRawDetails().get(AutomatonAlert.SUBJECT);
			}
		}
		if (!TextUtils.isEmpty(title)) {
			title = mDisplayName + " - " + title;
		}
		else {
			title = mDisplayName;
		}
		return title;
	}

	private ActionBar setActionBar() {
		String title = getActionBarTitle();
		ActionBar ab = getSupportActionBar();
		if (ab != null) {
			Resources res = getResources();
			Utils.setActionBarCommon(res, ab, title);
			ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		}
		return ab;
	}

	public static boolean showRequireOnlyFutureAlarmsDialog(Activity activity) {
		// don't show after this time
		AlertDialog.Builder builder =
				new AlertDialog.Builder(activity)
					.setTitle("Reminder Problem")
					.setMessage("Reminders need to be set for a future date and time.")
					.setPositiveButton(
							AutomatonAlert.OK_LABEL, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
								}
							});
		AlertDialog alertDialog = builder.create();
		alertDialog.show();

		return false;
	}

//	@Override
//	@Deprecated
//	protected void onPrepareDialog(int id, @NotNull Dialog dialog, Bundle args) {
//		super.onPrepareDialog(id, dialog, args);
//
//		// if mAlertItem.getDateRemind == null, use System.currTimeInMillis
//		Calendar cal = Calendar.getInstance();
//		Date origDate = Utils.getDateRemindOrNull(mAlertItem);
//		Date newDate = origDate;
//		if (origDate == null) {
//			newDate = new Date(System.currentTimeMillis());
//			// origDate is used in comparison to see if the date has changed.
//			// null needs to be before any date that could be entered
//			origDate = new Date(0);
//		}
//		cal.setTime(newDate);
//
//		if (dialog instanceof DatePickerDialog) {
//			((DatePickerDialog)dialog).updateDate(
//					cal.get(Calendar.YEAR),
//					cal.get(Calendar.MONTH),
//					cal.get(Calendar.DAY_OF_MONTH));
//			mPicker.setCompareToDate(origDate.getTime());
//		}
//		else if (dialog instanceof TimePickerDialog) {
//			((TimePickerDialog)dialog).updateTime(
//					cal.get(Calendar.HOUR_OF_DAY),
//					cal.get(Calendar.MINUTE));
//			mPicker.setCompareToDate(origDate.getTime());
//		}
//	}

//	@Override
//	protected Dialog onCreateDialog(int id) {
//		return getDateOrTimePicker().getDialog(id, Utils.getDateRemindOrNull(mAlertItem));
//	}
//
	private Fragment getChooserFragment(String tag) {
    	return getSupportFragmentManager().findFragmentByTag(tag);
	}

	private boolean endChooserIfAny() {
		boolean chooserEnded = false;

    	if ((getChooserFragment(mRTChooser)) != null) {
    		endFragmentInteraction(mRTChooser);
    		chooserEnded = true;
    	}
    	if (getChooserFragment(mVolChooser) != null) {
    		endFragmentInteraction(mVolChooser);
    		chooserEnded = true;
    	}
    	return chooserEnded;
	}

	@Override
	public boolean onKeyUp(int keyCode, @NotNull KeyEvent event) {
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
	public void updateRingtone(
			Dialog dialog, Fragment fragment,
			String sourceType, String song, Uri uri) {

		// send data back to the fragment
		if (fragment != null) {
			if (fragment instanceof RTUpdateFragment) {
				RTUpdateFragment ruf = (RTUpdateFragment)fragment;
				ruf.setRingtone(song, uri, false);
			}
		}
		if (dialog != null) {
			dialog.dismiss();
		}
//		endFragmentInteraction(RTChooserFragment.class.toString());
	}
}
