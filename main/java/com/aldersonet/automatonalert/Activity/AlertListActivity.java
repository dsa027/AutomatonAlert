package com.aldersonet.automatonalert.Activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Adapter;
import android.widget.BaseAdapter;

import com.aldersonet.automatonalert.Adapter.AlertListArrayAdapter;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Fragment.AlertListFragment;
import com.aldersonet.automatonalert.Fragment.AlertListFragment.Mode;
import com.aldersonet.automatonalert.Fragment.IALActivityController;
import com.aldersonet.automatonalert.Fragment.IALFragmentController;
import com.aldersonet.automatonalert.Fragment.IActivityMenuGetter;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.ActionBar.ActionBarDrawer;
import com.aldersonet.automatonalert.Util.Utils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AlertListActivity extends AppCompatActivity
		implements
		IALActivityController,
		IActivityMenuGetter,
		AlertListFragment.IAlertListFragmentListener {

	public enum FragmentTypeAL {
		NEW("Alert Inbox"),
        SAVED("Saved"),
        TRASH("Trash"),
        ALARMS("Reminders"),
        SNOOZED("Snoozed"),
        REPEATS("Repeating"),
        SETTINGS("Settings");

        private String title;
        FragmentTypeAL(String title) {
            this.title = title;
        }
        String getTitle() { return title; }
	}
	public static final FragmentTypeAL[] mAlertsTabs = {
			FragmentTypeAL.NEW,
			FragmentTypeAL.SAVED,
			FragmentTypeAL.TRASH,
			FragmentTypeAL.SETTINGS
	};
	public static final FragmentTypeAL[] mAlarmsTabs = {
		FragmentTypeAL.ALARMS,
		FragmentTypeAL.SNOOZED,
		FragmentTypeAL.REPEATS,
		FragmentTypeAL.SETTINGS
	};
	public AlertListFragment.Mode mMode;

	public static final int ALARM_SET_REQCODE_BASE = 2346;
	public static final int ALARM_SET_REQCODE_MULTIPLIER = 1000;

	CharSequence ACTIVITY_TITLE = "Alerts";

	ActionBar mActionBar;
	public Menu mMenu;
	String mFragmentTypeForInitialDisplay;
	ArrayList<BaseAdapter> mAdapterList = null;
	ViewPager mViewPager;
	AlertListPagerAdapter mPagerAdapter;
	FragmentManager mFragmentManager;

	ActionBarDrawer mActionBarDrawer;

	private int navIndexToALIndex() {
		// the page we're on
		int navIdx = mActionBar.getSelectedNavigationIndex();

		// ALERTS
		if (mMode.equals(Mode.ALERT)) {
			return mAlertsTabs[navIdx].ordinal();
		}
		// ALARMS
		else {
			return mAlarmsTabs[navIdx].ordinal();
		}
	}

	////////////////////////
	// IAlertListFragment
	////////////////////////
	@Override
	public ArrayList<BaseAdapter> getAdapterList() {
		return mAdapterList;
	}
	@Override
	public ActionBar getTheActionBar() {
		return mActionBar;
	}

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
		refreshAllFragments();

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
	protected void onResume() {
		stopActionModeAllFragments();
		super.onResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.action_bar_refresh, menu);
		mMenu = menu;
		// put ProgressBar on the end
		menu.getItem(0).setVisible(false);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//davedel -- disallow access
		finish();
		//davedel

		supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.alert_list_activity_fragment);
//		mFragmentList.clear();
		Intent intent = getIntent();
		setDataFromIntent(intent);

		// ViewPager and its adapters use support library
		// fragments, so use getSupportFragmentManager.
		mPagerAdapter = new AlertListPagerAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mPagerAdapter);
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				// When swiping between pages, select the
				// corresponding tab.
				getSupportActionBar().setSelectedNavigationItem(position);
				// change title to reflect the new page
				if (mAdapterList != null) {
					setActionBarTitle(mAdapterList.get(position));
				}
				// cancel ActionMode (multiselect)
				stopActionModeAllFragments();
			}
		});

		// Show the Up button in the action bar.
		setActionBar();
		setDrawer();

		// go to the tab as passed in Intent
		int i = -1;
		FragmentTypeAL[] values =
				mMode.equals(Mode.ALERT) ? mAlertsTabs : mAlarmsTabs;
		for (FragmentTypeAL s : values) {
			++i;
			if (mFragmentTypeForInitialDisplay.equals(s.name())) {
				getSupportActionBar().setSelectedNavigationItem(i);
				break;
			}
		}
	}

	private void setDataFromIntent(Intent intent) {
		mFragmentTypeForInitialDisplay =
				intent.getStringExtra(AutomatonAlert.FRAGMENT_TYPE);
		if (mFragmentTypeForInitialDisplay == null) {
			mFragmentTypeForInitialDisplay = FragmentTypeAL.NEW.name();
		}
		if (mFragmentTypeForInitialDisplay.equals(FragmentTypeAL.ALARMS.name())) {
			intent.putExtra(AutomatonAlert.MODE, Mode.ALARM.name());
		}

		// SHOW Alerts only or Alarms only
		String mode = intent.getStringExtra(AutomatonAlert.MODE);
		try {
			mMode = Mode.valueOf(mode);
		} catch (NullPointerException npe) {
			mMode = Mode.ALERT;
		} catch (IllegalArgumentException iae) {
			mMode = Mode.ALERT;
		}
	}

	private BaseAdapter getFragmentsAdapter(int reqCode) {
		int idx = decodeRequestCode(reqCode).first;
		if (idx < 0
				|| idx >= mAdapterList.size()) {
			return null;
		}
		return mAdapterList.get(idx);
	}

	@Override
	public int encodeRequestCode(int originalNumber, FragmentTypeAL type) {
		int base = AlertListActivity.ALARM_SET_REQCODE_BASE;
		int multiplied = (type.ordinal()+1/*can't be 0*/)
				* AlertListActivity.ALARM_SET_REQCODE_MULTIPLIER;
		return base + multiplied + originalNumber;
	}

	@Override
	public Pair<Integer, Integer> decodeRequestCode(int reqCode) {
		int minusBase = reqCode - ALARM_SET_REQCODE_BASE;
		int indexToType = minusBase / ALARM_SET_REQCODE_MULTIPLIER - 1 /*back to ordinal*/;
		int originalReqCode = minusBase % ALARM_SET_REQCODE_MULTIPLIER;
		return new Pair<Integer, Integer>(indexToType, originalReqCode);
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
//		stopActionModeAllFragments();
		// exclusively for SetAlertActivity for the moment...
		// requestCode is encoded to reflect which adapter
		// did the startActivityForResult()
		BaseAdapter adapter = getFragmentsAdapter(requestCode);
		if (adapter != null) {
			if (adapter instanceof AlertListArrayAdapter) {
				AlertListArrayAdapter aAdapter = (AlertListArrayAdapter)adapter;
				aAdapter.replaceShowListAlertItem(requestCode);
			}
		}
	}

	public class AlertListPagerAdapter extends FragmentPagerAdapter {

		public AlertListPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			Fragment fragment = null;
			String sourceTypeName = null;

			if (mMode.equals(Mode.ALERT)) {
				sourceTypeName = mAlertsTabs[position].name();
				switch(position) {
				case 0:	// new
				case 1:	// saved
				case 2: // trash
					fragment = AlertListFragment.newInstance(sourceTypeName);
					break;
				case 3: // Settings
				default:
					fragment = AlertListFragment.newInstance(sourceTypeName);
					break;
				}
			}
			else {
				sourceTypeName = mAlarmsTabs[position].name();
				switch(position) {
				case 0: // alarm
				case 1: // snoozed
				case 2: // repeat
					fragment = AlertListFragment.newInstance(sourceTypeName);
					break;
				case 3: // Settings
				default:
					fragment = AlertListFragment.newInstance("SETTINGS");
					break;
				}
			}

//			mFragmentList.put(position, fragment);
			return fragment;
		}

		@Override
		public int getCount() {
			if (mMode.equals(Mode.ALERT)) {
				return mAlertsTabs.length;
			}
			else {
				return mAlarmsTabs.length;
			}
//	        return NUM_PAGE_VIEWS;
		}

	}

	@Override
	public boolean onKeyUp(int keyCode, @NotNull KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			mActionBarDrawer.openDrawer();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	private ActionBar setActionBar() {
		mActionBar = getSupportActionBar();
		if (mActionBar != null) {
			Resources res = getResources();

			Utils.setActionBarCommon(
					res,
					mActionBar,
					mMode.equals(Mode.ALERT) ? "Alerts":"Alarms");

			Tab tNew = mActionBar.newTab()
					.setIcon(res.getDrawable(R.drawable.ic_inbox_cc_findicons_com_439129))
					.setTabListener(new TabListener<AlertListFragment>(
							this,
							FragmentTypeAL.NEW.name(),
							mViewPager));
			Tab tSaved = mActionBar.newTab()
					.setIcon(res.getDrawable(R.drawable.ic_menu_save_app_blue))
					.setTabListener(new TabListener<AlertListFragment>(
							this,
							FragmentTypeAL.SAVED.name(),
							mViewPager));
			Tab tTrash = mActionBar.newTab()
					.setIcon(res.getDrawable(R.drawable.delete_bin_app_blue))
					.setTabListener(new TabListener<AlertListFragment>(
							this,
							FragmentTypeAL.TRASH.name(),
							mViewPager));
			Tab tAlarm = mActionBar.newTab()
					.setIcon(res.getDrawable(R.drawable.ic_alarm_clock_app_blue))
					.setTabListener(new TabListener<AlertListFragment>(
							this,
							FragmentTypeAL.ALARMS.name(),
							mViewPager));
			Tab tSnoozed = mActionBar.newTab()
					.setIcon(res.getDrawable(R.drawable.ic_snooze))
					.setTabListener(new TabListener<AlertListFragment>(
							this,
							FragmentTypeAL.SNOOZED.name(),
							mViewPager));
			Tab tRepeat = mActionBar.newTab()
					.setIcon(res.getDrawable(R.drawable.ic_repeat))
					.setTabListener(new TabListener<AlertListFragment>(
							this,
							FragmentTypeAL.REPEATS.name(),
							mViewPager));
			Tab tSettings = mActionBar.newTab()
					.setIcon(res.getDrawable(R.drawable.settings))
					.setTabListener(new TabListener<AlertListFragment>(
							this,
							FragmentTypeAL.SETTINGS.name(),
							mViewPager));

			if (mMode.equals(Mode.ALERT)) {
				mActionBar.addTab(tNew);
				mActionBar.addTab(tSaved);
				mActionBar.addTab(tTrash);
			}
			else {
				mActionBar.addTab(tAlarm);
				mActionBar.addTab(tSnoozed);
				mActionBar.addTab(tRepeat);
			}
			mActionBar.addTab(tSettings);
		}
		return mActionBar;
	}

	public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
		private final ViewPager mPager;

		public TabListener(Activity activity, String tag, ViewPager pager) {
			mPager = pager;
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			mPager.setCurrentItem(tab.getPosition());
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}

	}

	private Fragment getFragment(BaseAdapter adapter) {
		if (adapter instanceof AlertListArrayAdapter) {
			return ((AlertListArrayAdapter)adapter).mFragment;
		}
		return null;
	}

	private FragmentTypeAL getFragmentType(BaseAdapter adapter) {
		Fragment fragment = getFragment(adapter);
		if (fragment != null) {
			if (fragment instanceof IALFragmentController) {
				return ((IALFragmentController)fragment).getFragmentType();
			}
		}
		return null;
	}

	/////////////////////////////
	// AlertListFragment.FreeFormListFragmentListener
	/////////////////////////////
	@Override
	public BaseAdapter setFragmentAdapterList(BaseAdapter adapter) {
		if (adapter == null) {
			return null;
		}
		// if there isn't one, make it
		if (mAdapterList == null) {
			int size = FragmentTypeAL.values().length;
			mAdapterList = new ArrayList<BaseAdapter>(size);
			for (int i=0;i<size;i++) {
				// get it sized right
				mAdapterList.add(null);
			}
		}
		FragmentTypeAL type = getFragmentType(adapter);

		if (type != null) {
			int idx = type.ordinal();
			mAdapterList.set(idx, adapter);
		}

		return adapter;
	}

	private String getTitle(FragmentTypeAL type) {
        if (type == null) return "Alerts";

        return type.getTitle();
	}

	private String getActionBarTitle(Adapter adapter) {
		if (adapter != null) {
			String name = getTitle(getFragmentType((BaseAdapter)adapter));
			int count = adapter.getCount();
			return name + " (" + count + ")";
		}
		return ACTIVITY_TITLE + "";
	}

	//////////////////////////
	// IAlertListFragmentListener
	// IAlertListActivityController
	//////////////////////////
	@Override
	public void setActionBarTitle(BaseAdapter adapter) {
		if (adapter != null) {
			Utils.setActionBarTitle(mActionBar, getActionBarTitle(adapter));
		}
		else if (mAdapterList != null) {
			int idx = navIndexToALIndex();//mActionBar.getSelectedNavigationIndex();
			if (idx >= 0
					&& idx < mAdapterList.size()) {
				BaseAdapter foundAdapter = mAdapterList.get(idx);
				if (foundAdapter != null) {
					Utils.setActionBarTitle(
							mActionBar, getActionBarTitle(foundAdapter));
				}
			}
		}
	}

	public List<Fragment> getFragments() {
		if (mFragmentManager == null) {
			mFragmentManager = getSupportFragmentManager();
		}

		return mFragmentManager.getFragments();
	}

	private boolean isFragmentInList(Fragment fragment, FragmentTypeAL[] types) {
		if (fragment instanceof IALFragmentController) {
			FragmentTypeAL target = ((IALFragmentController) fragment).getFragmentType();
			for (FragmentTypeAL type : types) {
				if (type.equals(target)) {
					return true;
				}
			}
		}

		return false;
	}

	private FragmentTypeAL[] getDoIts(final boolean[] doIts, final FragmentTypeAL[] types) {
		ArrayList<FragmentTypeAL> list = new ArrayList<>(types.length);
		boolean[] lDoIts = doIts;
		// if arrays are of different size, refresh all types[]
		int N = types.length;
		if (N != doIts.length) {
			lDoIts = Arrays.copyOf(doIts, N);
			Arrays.fill(lDoIts, true);
		}

		// add if doIt
		for (int i = 0; i < N; i++) {
			if (lDoIts[i]) {
				list.add(types[i]);
			}
		}

		return list.toArray(new FragmentTypeAL[list.size()]);

	}

	///////////////////////
	// IAlertListActivityController
	///////////////////////
	@Override
	public void refreshFragments(boolean[] doIts, FragmentTypeAL[] types) {
		FragmentTypeAL[] good = getDoIts(doIts, types);
		List<Fragment> list = getFragments();

		if (list != null) {
			for (Fragment fragment : list) {
				if (fragment instanceof IALFragmentController) {
					if (isFragmentInList(fragment, good)) {
						((IALFragmentController) fragment).refreshData();
					}
				}
			}
		}
	}

	private void refreshAllFragments() {
		List<Fragment> list = getFragments();
		if (list != null) {
			for (Fragment fragment : list) {
				if (fragment instanceof IALFragmentController) {
					((IALFragmentController) fragment).refreshData();
				}
			}
		}
	}

	////////////////////////////
	////////////////////////////
	private ActionMode getActionMode(BaseAdapter adapter) {
		if (adapter instanceof AlertListArrayAdapter) {
			return ((AlertListArrayAdapter)adapter).mActionMode;
		}
		return null;
	}

	////////////////////////
	// IAlertListActivityController
	////////////////////////
	@Override
	public void stopActionModeAllFragments() {
		if (mAdapterList != null) {
			for (BaseAdapter adapter : mAdapterList) {
				if (adapter != null) {
					ActionMode actionMode = getActionMode(adapter);
					if (actionMode != null) {
						actionMode.finish();
					}
				}
			}
		}
	}

	//////////////////////////////
	// IActivityMenuAccessor
	//////////////////////////////
	@Override
	public Menu getMenu() {
		return mMenu;
	}

}
