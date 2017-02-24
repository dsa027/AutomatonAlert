package com.aldersonet.automatonalert.Activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.aldersonet.automatonalert.ActionBar.ActionBarDrawer;
import com.aldersonet.automatonalert.ActionMode.ContactListInfo;
import com.aldersonet.automatonalert.Adapter.ContactCursorAdapter;
import com.aldersonet.automatonalert.Adapter.ContactListAdapterHelper;
import com.aldersonet.automatonalert.Adapter.ContactListArrayAdapter;
import com.aldersonet.automatonalert.Adapter.FreeFormListAdapterHelper;
import com.aldersonet.automatonalert.Adapter.FreeFormListArrayAdapter;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.ContactInfo.ContactInfoDO;
import com.aldersonet.automatonalert.Fragment.ContactActiveFragment;
import com.aldersonet.automatonalert.Fragment.ContactFavoriteFragment;
import com.aldersonet.automatonalert.Fragment.ContactSearchFragment;
import com.aldersonet.automatonalert.Fragment.FreeFormListFragment;
import com.aldersonet.automatonalert.Fragment.IActivityMenuGetter;
import com.aldersonet.automatonalert.Fragment.ICLFragmentController;
import com.aldersonet.automatonalert.Fragment.RTUpdateFragment;
import com.aldersonet.automatonalert.OkCancel.OkCancel;
import com.aldersonet.automatonalert.OkCancel.OkCancelDialog;
import com.aldersonet.automatonalert.Preferences.ContactListSettingsFragment;
import com.aldersonet.automatonalert.Preferences.NameValueDataDO;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.SourceType.SourceTypeDO;
import com.aldersonet.automatonalert.Util.Enums;
import com.aldersonet.automatonalert.Util.Lists;
import com.aldersonet.automatonalert.Util.Utils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

public class ContactFreeFormListActivity extends AppCompatActivity
implements
		IActivityRefresh,
		ICLActivity,
		IActivityMenuGetter,
		ContactActiveFragment.IContactActiveFragmentListener,
		ContactSearchFragment.IContactSearchFragmentListener,
		ContactFavoriteFragment.IContactFavoriteFragmentListener,
		ContactListSettingsFragment.IContactListSettingsFragmentListener,
		FreeFormListFragment.IFreeFormListFragmentListener,
		RTUpdateFragment.IRTUpdateFragmentListener {

	public static final String TAG = "ContactListActivity";

	public enum FragmentTypeCL {
		ACTIVE("Contacts - Edit Ringtones"),
        FAVORITES("Contacts - Favorites"),
        SEARCH("Contacts - New Ringtones"),
        FREEFORM("Free-Form Text/Email Alerts");
        /*SETTINGS*/
        private String title;
        FragmentTypeCL(String title) {
            this.title = title;
        }
        String getTitle() { return title; }
	}

	public static final int NUM_PAGE_VIEWS =
			FragmentTypeCL.values().length;

	public Menu mMenu;
	ViewPager mViewPager;
	ContactListPagerAdapter mPagerAdapter;
	String mFragmentTypeToDisplay;
	List<BaseAdapter> mAdapterList = null;
	@SuppressLint("UseSparseArrays")
//	public static final HashMap<Integer, Fragment> mFragmentList =
//			new HashMap<Integer, Fragment>(NUM_PAGE_VIEWS);

	ActionBarDrawer mActionBarDrawer;

	FragmentManager mFragmentManager;

	Tab mActiveTab;
	Tab mFavoritesTab;
	Tab mSearchTab;
	Tab mFreeFormTab;

	// tab's drawable resources
	static int mActiveRes;
	static int mFavoritesRes;
	static int mSearchRes;
	static int mFreeFormRes;
	static {
		setPortraitTabResources();
	}

	@Override
	public void onTrimMemory(int level) {
		if (level > TRIM_MEMORY_MODERATE) {
			ContactListInfo.killAllBitmaps();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mActionBarDrawer != null) {
			mActionBarDrawer.getDrawerToggle().onConfigurationChanged(newConfig);
		}
	    setTabIconResources();
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
	protected void onStart() {
		super.onStart();
		AutomatonAlert.THIS.mContactFreeFormListActivity = this;

        // internal advertising campaign
        // only show one coming into this activity
        if (!NameValueDataDO.hasBeenAskedToViewTour()) {
            askToViewTour();
            // make sure new users don't see this
            NameValueDataDO.hasBeenShownNewVersionFeatures();
        }
        else if (!NameValueDataDO.hasBeenShownNewVersionFeatures()){
            showNewVersionFeatures();
        }
        else if (Utils.isItTimeToAskUserToRateApp(true/*resetClock*/)) {
            askToRate();
        }
	}

    @Override
	protected void onStop() {
		super.onStop();
		AutomatonAlert.THIS.mContactFreeFormListActivity = null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.contact_list_activity);

		Intent intent = getIntent();
		setFromIntentData(intent);

		mPagerAdapter = new ContactListPagerAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mPagerAdapter);
		mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				// When swiping between pages, select the
				// corresponding tab.
				getSupportActionBar().setSelectedNavigationItem(position);
				stopActionModeAllFragments();
				// show text if the list is empty
				showTextForEmptyFragmentList(position);
				// show this fragment's title
				setActionBarTitle(position);
			}
		});

		// Show the Up button in the action bar.
		setActionBar();
		setDrawer();
		showOpeningFragment();

//        // check app "dangerous" permissions
//        List<String> list = AutomatonAlert.THIS.checkPermissions(AutomatonAlert.CRITICAL_PERMISSIONS);
//        if (list.size() > 0) {
//            Intent permIntent = new Intent(
//                    this.getApplicationContext(), AutomatonAlertActivity.class);
//            startActivityForResult(
//                    permIntent, AutomatonAlertActivity.PERMISSIONS_REQ);
//        }
//        else {
//            initializeApp();
//        }
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == AutomatonAlertActivity.PERMISSIONS_REQ) {
//            initializeApp();
//        }
//    }

//    private void initializeApp() {
//        AutomatonAlert.THIS.initializeApp();
//    }

    private void showOpeningFragment() {
		Enums.getEnum(mFragmentTypeToDisplay, FragmentTypeCL.values(), null);

		// if we were asked to go somewhere specific...
		if (!TextUtils.isEmpty(mFragmentTypeToDisplay)) {
			FragmentTypeCL type =
					Enums.getEnum(mFragmentTypeToDisplay, FragmentTypeCL.values(), null);
			if (type != null) {
				int idx = type.ordinal();
				getSupportActionBar().setSelectedNavigationItem(idx);
				setActionBarTitle(idx);
			}
		}
		else {
			// SEARCH: if there're no ringtones set yet
			// ACTIVE: if there are (PHONE and merge issues aside,
			// both of which resolve themselves eventually)
			TreeSet<HashMap<String, String>> list = ContactInfoDO.getEmptySortedTreeSet();
			// are there any SourceRecs?
			SourceTypeDO.getSourceTypeContacts(
					list, null, true/*justGetOne*/, false/*ignorePhoneType*/, this);
			// if there are, show them, otherwise, show search
			FragmentTypeCL type = (list.size() > 0) ?
					FragmentTypeCL.ACTIVE : FragmentTypeCL.SEARCH;
			int idx = type.ordinal();
			getSupportActionBar().setSelectedNavigationItem(idx);
			setActionBarTitle(idx);
		}
	}

	private void showTextForEmptyFragmentList(int position) {
		Fragment fragment = getFragment(position);
		if (fragment != null) {
			if (fragment instanceof ICLFragmentController) {
				((ICLFragmentController)fragment).showTextForEmptyList();
			}
		}
	}


	private void setActionBarTitle(int position) {
		Log.d(TAG + ".setActionBarTitle(" + position + ")", "setting title");

		ActionBar ab = getSupportActionBar();

        try {
			Utils.setActionBarTitle(ab, FragmentTypeCL.values()[position].getTitle());
            return;
		} catch (ArrayIndexOutOfBoundsException ignored) {}

		// default
		Utils.setActionBarTitle(ab, "Contacts");

		Log.d(TAG + ".setActionBarTitle(" + position + ")", "default title set");
	}


	//////////////////////////////
	// IContactListFragment
	//////////////////////////////
	@Override
	public boolean isThisFragmentShowingNow(FragmentTypeCL inFragmentType) {
		if (inFragmentType == null) return false;

//		if (BuildConfig.DEBUG) {
//			boolean equal = mViewPager.getCurrentItem() == inFragmentType.ordinal();
//			Log.d(TAG + ".isThisFragmentShowingNow()",
//					"shown[" + Enums.getEnum(
//							mViewPager.getCurrentItem(), FragmentTypeCL.values(), null)
//					+ "]==?("+equal+") [" + inFragmentType + "]");
//		}

		return mViewPager.getCurrentItem() == inFragmentType.ordinal();
	}

	public FragmentTypeCL getFragmentType(int ordinal, Enum def) {
		return (FragmentTypeCL)Enums.getEnum(ordinal, FragmentTypeCL.values(), def);
	}

	private Fragment getFragmentInView() {
		Fragment fragment = getFragment(mViewPager.getCurrentItem());

//		if (BuildConfig.DEBUG) {
//    		if (fragment != null) {
//				FragmentTypeCL fragmentType = null;
//				if (fragment instanceof ContactSearchFragment) {
//					fragmentType = FragmentTypeCL.SEARCH;
//				} else if (fragment instanceof ContactFavoriteFragment) {
//					fragmentType = FragmentTypeCL.FAVORITES;
//				} else if (fragment instanceof FreeFormListFragment) {
//					fragmentType = FragmentTypeCL.FREEFORM;
//				} else {
//					fragmentType = FragmentTypeCL.ACTIVE;
//				}
//				Log.d(TAG + ".getFragmentInView()", "in view[" + fragmentType + "]");
//			}
//		}

		return fragment;

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.contact_list_action_bar_menu, menu);
	    mMenu = menu;
	    // put ProgressBar on the end
	    menu.getItem(0).setVisible(false);
		return true;
	}

	public static boolean refreshFragment(Fragment fragment) {
		boolean ret = false;

		if (fragment != null) {
			if (fragment instanceof ICLFragmentController) {
				((ICLFragmentController)fragment).refreshData(false/*forced*/);
				ret = true;
			}
		}
		return ret;
	}

	@Override
	public void refreshFragments(boolean[] doIts, FragmentTypeCL[] types) {
		FragmentTypeCL[] good = getDoIts(doIts, types);
		List<Fragment> list = getFragments();

		if (list != null) {
			for (Fragment fragment : list) {
				if (fragment instanceof ICLFragmentController) {
					if (isFragmentInList(fragment, good)) {
						final Fragment fFragment = fragment;
						fragment.getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((ICLFragmentController) fFragment).refreshData(false/*forced*/);
                            }
						});
					}
				}
			}
		}
	}

	@Override
	public void notifyAdapters(boolean[] doIts, FragmentTypeCL[] types) {
		FragmentTypeCL[] good = getDoIts(doIts, types);
		List<Fragment> list = getFragments();

		if (list != null) {
			for (Fragment fragment : list) {
				if (fragment instanceof ICLFragmentController) {
					if (isFragmentInList(fragment, good)) {
                        if (((ICLFragmentController) fragment).getAdapter() != null) {
                            ((ICLFragmentController) fragment).getAdapter().notifyDataSetChanged();
                        }
					}
				}
			}
		}
	}

	private void refreshAllFragments() {
		List<Fragment> list = getFragments();
		if (list != null) {
			for (Fragment fragment : list) {
				if (fragment instanceof ICLFragmentController) {
					((ICLFragmentController) fragment).refreshData(true/*forced*/);
				}
			}
		}
	}

	//////////////////////////////////////////////////////
	public List<Fragment> getFragments() {
		if (mFragmentManager == null) {
			mFragmentManager = getSupportFragmentManager();
		}

		return mFragmentManager.getFragments();
	}

	public Fragment getFragment(int ordinal) {
		FragmentTypeCL type = getFragmentType(ordinal, null);

		return (type == null) ? null : getFragment(type);
	}

	public Fragment getFragment(FragmentTypeCL type) {
		List<Fragment> list = getFragments();
		Log.d(TAG + "getFragment(" + type + ")", ".....");

		if (list != null) {
			Log.d(TAG + "getFragment(" + type + ")", "list.size[" + list.size() + "]");
			for (Fragment fragment : list) {
				FragmentTypeCL fragmentType =
						((ICLFragmentController) fragment).getFragmentType();
				if (fragmentType.equals(type)) {
					// ASSUMING there's only one
					Log.d(TAG + "getFragment(" + type + ")", "found it");
					return fragment;
				}
			}
		}

		Log.d(TAG + "getFragment(" + type + ")", "not found, returning null");
		return null;
	}

	public int getFragmentIndexOf(Fragment getThis) {
		List<Fragment> list = getFragments();
		if (list != null) {
			int N = list.size();
			for (int i = 0; i < N; i++) {
				if (list.get(i).equals(getThis)) {
					return i;
				}
			}
		}

		return -1;
	}

	private boolean isFragmentInList(Fragment fragment, FragmentTypeCL[] types) {
		if (fragment instanceof ICLFragmentController) {
			FragmentTypeCL target = ((ICLFragmentController) fragment).getFragmentType();
			for (FragmentTypeCL type : types) {
				if (type.equals(target)) {
					return true;
				}
			}
		}

		return false;
	}

	private FragmentTypeCL[] getDoIts(final boolean[] doIts, final FragmentTypeCL[] types) {
		ArrayList<FragmentTypeCL> list = new ArrayList<>(types.length);
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

		return list.toArray(new FragmentTypeCL[list.size()]);

	}

	//////////////////////////////////////////////////////

	private void setFromIntentData(Intent intent) {
		mFragmentTypeToDisplay = intent.getStringExtra(
				AutomatonAlert.FRAGMENT_TYPE);
	}

	public class ContactListPagerAdapter extends FragmentPagerAdapter {
		ContactListPagerAdapter(FragmentManager fm) {
			super(fm);
		}
		@Override
		public Fragment getItem(int position) {
			switch(getFragmentType(position, FragmentTypeCL.ACTIVE)) {
				case SEARCH:
					return ContactSearchFragment.newInstance();
				case FAVORITES:
					return ContactFavoriteFragment.newInstance();
				case FREEFORM:
					return FreeFormListFragment.newInstance();
				default:
					return ContactActiveFragment.newInstance();
			}
		}
		@Override
		public int getCount() {
			return NUM_PAGE_VIEWS;
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

	private void setTabIconResources() {
		Resources res = getResources();
		Configuration c = res.getConfiguration();

		if (c.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			setLandscapeTabResources();
		}
		else {
			setPortraitTabResources();
		}
		setTabIcons(res);
	}

	private static void setLandscapeTabResources() {
		mActiveRes = R.drawable.ic_action_edit_dark_64;
		mFavoritesRes = R.drawable.ic_menu_star_holo_dark_48_to_64;
		mSearchRes = R.drawable.ic_action_new_dark_64;
		mFreeFormRes = R.drawable.free_form_icon_holo_dark_64;
	}

	private static void setPortraitTabResources() {
		mActiveRes = R.drawable.ic_action_edit_64;
		mFavoritesRes = R.drawable.ic_menu_star_holo_light_48_to_64;
		mSearchRes = R.drawable.ic_action_new_light_64;
		mFreeFormRes = R.drawable.free_form_icon_holo_light_64;
	}

	private void setTabIcons(Resources res) {
		mActiveTab.setIcon(res.getDrawable(mActiveRes));
		mFavoritesTab.setIcon(res.getDrawable(mFavoritesRes));
		mSearchTab.setIcon(res.getDrawable(mSearchRes));
		mFreeFormTab.setIcon(res.getDrawable(mFreeFormRes));
	}

	private void setActionBar() {

		ActionBar ab = getSupportActionBar();
		if (ab != null) {
			Resources res = getResources();
			Utils.setActionBarCommon(res, ab, "Contacts");

			mActiveTab = ab.newTab()
					.setTabListener(new TabListener<ContactActiveFragment>(
							this,
							FragmentTypeCL.ACTIVE.name(),
							mViewPager));
			mFavoritesTab = ab.newTab()
					.setTabListener(new TabListener<ContactFavoriteFragment>(
							this,
							FragmentTypeCL.FAVORITES.name(),
							mViewPager));
			mSearchTab = ab.newTab()
					.setTabListener(new TabListener<ContactSearchFragment>(
							this,
							FragmentTypeCL.SEARCH.name(),
							mViewPager));
			mFreeFormTab = ab.newTab()
					.setTabListener(new TabListener<FreeFormListFragment>(
							this,
							FragmentTypeCL.FREEFORM.name(),
							mViewPager));

			setTabIconResources();

			ab.addTab(mActiveTab);
			ab.addTab(mFavoritesTab);
			ab.addTab(mSearchTab);
			ab.addTab(mFreeFormTab);
		}
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

	private void askToViewTour() {
		String title = "Take a Quick Tour?";
		String message =
				"<font color=\"#0099cc\">"
						+ "Would you like take a quick tour of Contact Ringtones?</font>";


		OkCancel okCancel = new OkCancel() {
			@Override
			protected void ok(DialogInterface dialog) {
				Intent intent = new Intent(
						getApplicationContext(), TakeATourActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}

			@Override
			protected void cancel(DialogInterface dialog) {
				Toast.makeText(
						getApplicationContext(),
						"You can always view the tour from the Help screen",
						Toast.LENGTH_SHORT)
				.show();
			}
		};

		Utils.okCancelDialogNative(
				this,
				title,
				message,
				okCancel);
	}

	private void showNewVersionFeatures() {
		Resources res = getResources();

		String title = res.getString(
				R.string.tour_page_free_form_list_description_title);
		String detail = res.getString(
				R.string.tour_page_free_form_list_description_detail);
		detail = detail.replace("\n\n", "<br>");
//		String purchaseText =
//				(AutomatonAlert.mIntroductoryUnlimitedUpgrade
//				 || AutomatonAlert.mHasDevelopersCode) ?
//						"<br>"
//						: "(requires in-app purchase)<br><br>";

		String message =
				"<font color=\"#0099cc\">"
						+ "New Feature!<br><br>"
						+ "--<b>Reminders</b>--<br>"
//						+ purchaseText
//						+ title + ":<br>"
						+ detail
						+ "</font>";

		OkCancel okCancel = new OkCancel() {
			@Override
			protected  void ok(DialogInterface dialog) {
//				getSupportActionBar().setSelectedNavigationItem(
//						FragmentTypeCL.FREEFORM.ordinal());
			}

			@Override
			protected  void cancel(DialogInterface dialog) {
			}
		};

		OkCancelDialog okCancelDialog = OkCancelDialog.showInstance(
				this,
				message,
				null,
				"",
				"Ok",
				OkCancelDialog.CancelButton.LEFT,
				OkCancelDialog.EWI.INFO
		);
		okCancelDialog.setOkCancel(okCancel);
	}

	private OkCancel getAskToRateOkCancel() {
		return new OkCancel() {
			@Override
			protected  void ok(DialogInterface dialog) {
				if (OkCancelDialog.isCheckBoxChecked(mRateAppOkCancel)) {
					NameValueDataDO.dontShowUserToRateAppEverAgain();
				}

				Intent intent = new Intent(
						Intent.ACTION_VIEW,
						Uri.parse("https://play.google.com/store/search?q=aldersonet"));
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
			@Override
			protected  void cancel(DialogInterface dialog) {
				if (OkCancelDialog.isCheckBoxChecked(mRateAppOkCancel)) {
					NameValueDataDO.dontShowUserToRateAppEverAgain();
				}
			}
		};

	}

	OkCancelDialog mRateAppOkCancel;

	private void askToRate() {
		String message =
				"If you haven't already, please rate this app!<br><br> If you can't give"
						+ " the app 5 stars, please first send us an email at<br>"
						+ " <font color='red'><small>"
				        + AutomatonAlert.TECH_EMAIL_ADDR
				        + "</small></font>"
						+ "<br> to tell us what we can fix!<br><br>"
						+ " Thank you.";
		OkCancel okCancel = getAskToRateOkCancel();

		mRateAppOkCancel = OkCancelDialog.showInstance(
				this,
				message,
				"Don't show this again",
				"Cancel",
				"Rate",
				OkCancelDialog.CancelButton.LEFT,
				OkCancelDialog.EWI.INFO
		);
		mRateAppOkCancel.setOkCancel(okCancel);

	}

	private FragmentTypeCL getFragmentTypeFromAdapter(Fragment fragment) {
		if (fragment instanceof ICLFragmentController) {
			return ((ICLFragmentController)fragment).getFragmentType();
		}
		return null;
	}

	private Fragment getAdapterFragment(BaseAdapter adapter) {
		if (adapter instanceof ContactListArrayAdapter) {
			return ((ContactListArrayAdapter)adapter).getFragment();
		}
		else if (adapter instanceof ContactCursorAdapter) {
			return ((ContactCursorAdapter)adapter).getFragment();
		}
		else if (adapter instanceof FreeFormListArrayAdapter) {
			return ((FreeFormListArrayAdapter)adapter).getFragment();
		}
		return null;
	}

	/* Need a common base for Helper to genericize this mHelper access */
	private void stopAdaptersActionMode(BaseAdapter adapter) {
		if (adapter instanceof ContactListArrayAdapter) {
			ContactListAdapterHelper helper =
					((ContactListArrayAdapter)adapter).mHelper;
			if (helper != null) {
				if (helper.mActionMode != null) {
					helper.mActionMode.finish();
					helper.mActionMode = null;
				}
			}
		}
		else if (adapter instanceof ContactCursorAdapter) {
			ContactListAdapterHelper helper =
					((ContactCursorAdapter)adapter).mHelper;
			if (helper != null) {
				if (helper.mActionMode != null) {
					helper.mActionMode.finish();
					helper.mActionMode = null;
				}
			}
		}
		else if (adapter instanceof FreeFormListArrayAdapter) {
			FreeFormListAdapterHelper helper =
					((FreeFormListArrayAdapter)adapter).mHelper;
			if (helper != null) {
				if (helper.mActionMode != null) {
					helper.mActionMode.finish();
					helper.mActionMode = null;
				}
			}
		}
	}

	@Override
	public void stopActionModeAllFragments() {
		if (mAdapterList != null) {
			for (BaseAdapter adapter : mAdapterList) {
				if (adapter != null) {
					stopAdaptersActionMode(adapter);
				}
			}
		}
	}
	@Override
	public BaseAdapter setFragmentAdapterHandle(BaseAdapter adapter) {
		if (adapter == null) {
			return null;
		}
		if (mAdapterList == null) {
			mAdapterList =
					new Lists<BaseAdapter>().fill(null, FragmentTypeCL.values().length);
		}
		Fragment fragment = getAdapterFragment(adapter);
		if (fragment != null) {
			FragmentTypeCL type = getFragmentTypeFromAdapter(fragment);
			if (type != null) {
				int idx = type.ordinal();
				mAdapterList.set(idx, adapter);
			}
		}

		return adapter;
	}
	@Override
	public String getLookupKeyCallback() {
		return null;
	}

	@Override
	public List<BaseAdapter> getAdapterList() {
		return mAdapterList;
	}

	@Override
	public ActionBar getTheActionBar() {
		return getSupportActionBar();
	}

	@Override
	public Menu getMenu() {
		return mMenu;
	}

}
