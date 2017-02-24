package com.aldersonet.automatonalert.Preferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.aldersonet.automatonalert.Account.AccountDO;
import com.aldersonet.automatonalert.Account.Accounts;
import com.aldersonet.automatonalert.Activity.AccountAddUpdateActivity;
import com.aldersonet.automatonalert.Activity.FragmentHostActivity;
import com.aldersonet.automatonalert.Activity.FragmentHostActivity.HostFragmentType;
import com.aldersonet.automatonalert.Activity.GmailActivity;
import com.aldersonet.automatonalert.Activity.HelpActivity;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent.ApiSubType;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent.ApiType;
import com.aldersonet.automatonalert.Alert.NotificationItemDO;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Email.AccountEmailDO;
import com.aldersonet.automatonalert.Email.EmailGet;
import com.aldersonet.automatonalert.Filter.FilterItemDO;
import com.aldersonet.automatonalert.Filter.FilterItems;
import com.aldersonet.automatonalert.OkCancel.OkCancel;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Receiver.AlertReceiver;
import com.aldersonet.automatonalert.SMS.AccountSmsDO;
import com.aldersonet.automatonalert.Service.AutomatonAlertService;
import com.aldersonet.automatonalert.SourceAccount.SourceAccountDO;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.ArrayList;
import java.util.Collections;

public class SettingsFragment extends PreferenceFragment {

	// relevant:
	// Two general modes:
	// 1. No specific intent: Full set of preferences
	// 2. Intent with getData() = account.getKey(): account-specific preferences

	// Full set of preferences:
	//
	// Preferences are built:
	// Screen 1. preferences.xml
	// Screen 2. List of accounts (built in code below)
	// Screen 3. One screen per account of account-specific preferences
	//           top: account_preferences.xml
	//			 bottom: set of search phrases (built in code)

	public static final String TAG_SHOW_MODE            = "mShowMode";
	public static final String TAG_ACCOUNT_KEY          = "mAccountKey";
	public static final String TAG_DEBUG_MODE           = "mDebugMode";
	public static final String TAG_FOREGROUND           = "mForeground";
	public static final String TAG_IMAP_RETRIES         = "mImapMaxRetries";
	public static final String TAG_GC_POLL_INTERVAL     = "mGCPollInterval";
	public static final String TAG_POLL                 = "mPoll";
	public static final String TAG_SYSTEM_ON            = "mSystemOn";
	public static final String TAG_RINGTONE_SETTINGS    = "mRingtoneSettings";
	public static final String TAG_OVERRIDE_VOLUME      = "mOverrideVolume";
	public static final String TAG_QUIET_TIME           = "mQuietTime";

	public enum ShowMode {
		GENERAL,				// general preference screen
		DEVELOPER,				// developer preference screen
		ACCOUNT_LIST,			// list of accounts
		ACCOUNT					// an account's preference screen
	}

	// AccountDO-specific preferences
	// Screen 3, above

	public static final int TIME_START_DIALOG_ID = 9669;
	public static final int TIME_END_DIALOG_ID = 6996;

	private final static String ACCOUNTS_PREF_BUTTON_ID 		= "accounts";
	private final static String SINGLE_ACCOUNT_PREF_BUTTON_ID	= "account";
	private final static String ADD_NEW_EMAIL_PREF_BUTTON_ID	= "addNewEmailAccount";
	private final static String DEVELOPER_PREF_BUTTON_ID		= "developerPreferences";
	private final static String HELP_PREF_BUTTON_ID				= "helpPreferences";

	public final static String ACCOUNTS_LABEL					= "Edit Email Account(s)";
	public final static String ADD_NEW_EMAIL_ACCOUNT_LABEL		= "Register New Email Account";
	public final static String DELETE_ACCOUNT_LABEL 			= "Delete This Account";
	public final static String UPDATE_ACCOUNT_LABEL 			= "Change Password/Server";
	public final static String CHECK_ACCOUNT_NOW_LABEL 			= "Scan For Messages Now";
	public final static String RESET_MESSAGE_SEARCH_TO_1_LABEL 	= "Reset to Scan All";
	public final static String RESET_MESSAGE_SEARCH_TO_TOP_LABEL= "Reset to Scan New";
	public final static String HELP_LABEL 						= "Help";
	public final static String ABOUT_LABEL 						= "About " + AutomatonAlert.mAppTitle;
	public final static String CHECK_FOR_NEW_MESSAGES_LABEL		= "Scan for Messages Interval";

	PollChangeListener mPollChanged;
	PreferenceClickListener mPrefClicked;
	GeneralPreferenceChangeListener mGeneralPrefChanged;
	DeveloperPreferenceChangeListener mDeveloperPrefChanged;
	OtherPreferenceClickListener mOtherPrefClicked;

	ShowMode mShowMode = ShowMode.GENERAL;
	String mAccountToShowKey;

	PreferenceManager mPm;
	PreferenceScreen mAnchorPs;

	// convenience for when a preference is changed
	// this class contains pointers to all account
	// preferences
	AccountDO mAccount;

	int mOrder;

	QuietTimePreference mQuietTimePreference;
	OverrideVolumePreference mOverrideVolumePreference;

	public static SettingsFragment newInstance(ShowMode showMode, String accountKey) {
		SettingsFragment fragment = new SettingsFragment();

		Bundle bundle = new Bundle();
		bundle.putString(TAG_SHOW_MODE, showMode.name());
		bundle.putString(TAG_ACCOUNT_KEY, accountKey);
		fragment.setArguments(bundle);

		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle bundle = getArguments();
		mShowMode = getShowMode(bundle.getString(TAG_SHOW_MODE));
		mAccountToShowKey = bundle.getString(TAG_ACCOUNT_KEY, "");

		String title = null;
		AppCompatActivity activity = (AppCompatActivity)getActivity();
		ActionBar actionBar = activity.getSupportActionBar();

		switch(mShowMode) {
			case GENERAL:
				title = "General Settings";
				break;
			case DEVELOPER:
				title = "Developer Settings";
				break;
			case ACCOUNT_LIST:
				title = "Account List";
				break;
			case ACCOUNT:
				title = "Update Account";
				break;
			default:
				title = "General Settings";
				break;
		}
		Utils.setActionBarTitle(actionBar, title);
	}

	@Override
	public View onCreateView(
			LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		// Inflate the layout for this fragment
		return inflater.inflate(
				R.layout.custom_prefs_list,
				container,
				false);
	}

	public ShowMode getmShowMode() {
		return mShowMode;
	}

	private void showAccountPreferences() {
		mOrder = 0;

		mAnchorPs = mPm.createPreferenceScreen(getActivity());
		mAnchorPs.setPersistent(false);

		mAccount = null;

		mAccount = Accounts.get(mAccountToShowKey);
		if (mAccount == null) {
			return;
		}

		int idx = mAccountToShowKey.indexOf("|");
		if (idx < 0) {
			mAnchorPs.setTitle(mAccountToShowKey + " preferences");
		}
		else {
			mAnchorPs.setTitle(
					mAccountToShowKey.substring(0, idx)	+ " preferences");
		}

		setPreferenceScreen(mAnchorPs);

		if (mAccount.mAccountType == AccountEmailDO.ACCOUNT_EMAIL) {
			showAccountEmailPreferences(mAnchorPs);
		}
	}

	private void showAccountList() {

		mAnchorPs = mPm.createPreferenceScreen(getActivity());
		mAnchorPs.setPersistent(false);
		mAnchorPs.setTitle(ACCOUNTS_LABEL);

		setPreferenceScreen(mAnchorPs);

		// for each account
		mOrder = 0;

		PreferenceCategory pc = new PreferenceCategory(getActivity());
		pc.setPersistent(false);
		pc.setOrder(mOrder++);
		pc.setTitle("Registered Accounts");
		pc.setLayoutResource(R.layout.preference_category_highlight);
		mAnchorPs.addPreference(pc);

		ArrayList<AccountDO> accounts = Accounts.get();

		Collections.sort(accounts, new Accounts.DefaultComparator<AccountDO>());

		for (final AccountDO account : accounts) {

			if (account.mAccountType == AccountSmsDO.ACCOUNT_SMS) {
				continue;
			}

			// screen for each account that contains groupings of search string preferences
			PreferenceScreen accountPs = mPm.createPreferenceScreen(getActivity());

			accountPs.setTitle(account.getName());
			accountPs.setOrder(mOrder++);
			accountPs.setPersistent(false);
			accountPs.setKey(
					SINGLE_ACCOUNT_PREF_BUTTON_ID
					+ "|"
					+ account.getKey());

			mAnchorPs.addPreference(accountPs);
			accountPs.setOnPreferenceClickListener(new AccountButtonClickListener());
		}

	}

	private void showDeveloperPreferences() {

		mOrder = 0;

		mAnchorPs = mPm.createPreferenceScreen(getActivity());
		mAnchorPs.setPersistent(false);
		mAnchorPs.setTitle("Developer Options");
		setPreferenceScreen(mAnchorPs);

		// Debug mode
		CheckBoxPreference cbp = new CheckBoxPreference(getActivity());
		cbp.setPersistent(false);
		cbp.setOrder(mOrder++);
		cbp.setTitle("Debug Mode");
		cbp.setKey(TAG_DEBUG_MODE);
		mAnchorPs.addPreference(cbp);
		cbp.setChecked(GeneralPrefsDO.isDebugMode());
		cbp.setSummary(
				"Show extra information like alert timestamps"
				+ " and polling alarms that would not otherwise be shown.");
		cbp.setOnPreferenceChangeListener(mDeveloperPrefChanged);

		// Foreground mode
		cbp = new CheckBoxPreference(getActivity());
		cbp.setPersistent(false);
		cbp.setOrder(mOrder++);
		cbp.setTitle("Foreground Service");
		cbp.setKey(TAG_FOREGROUND);
		mAnchorPs.addPreference(cbp);
		cbp.setChecked(GeneralPrefsDO.isForeground());
		cbp.setSummary(
				"Make the service a foreground service.  Somewhat"
				+ " more responsive with a slight impact on the battery.");
		cbp.setOnPreferenceChangeListener(mDeveloperPrefChanged);

		// iMap Retries
		ListPreference lp = new ListPreference(getActivity());
		lp.setPersistent(false);
		lp.setOrder(mOrder++);
		lp.setTitle("IMAP Retries");
		lp.setKey(TAG_IMAP_RETRIES);
		mAnchorPs.addPreference(lp);
		lp.setEntries(new CharSequence[] { "1", "3", "5", "10" });
		lp.setEntryValues(new CharSequence[] { "1", "3", "5", "10" });
		lp.setSummary(getImapRetriesSummary("" + GeneralPrefsDO.getImapMaxRetries()));
		lp.setOnPreferenceChangeListener(mDeveloperPrefChanged);

		// Ping on no Poll
		lp = new ListPreference(getActivity());
		lp.setPersistent(false);
		lp.setOrder(mOrder++);
		lp.setTitle("Ping on no Poll");
		lp.setKey(TAG_GC_POLL_INTERVAL);
		mAnchorPs.addPreference(lp);
		String[] entries = getResources().getStringArray(R.array.alarm_snooze_entries);
		String[] values = getResources().getStringArray(R.array.alarm_snooze_values);
		lp.setValue("" + GeneralPrefsDO.getGCPollInterval());
		lp.setSummary(getGCPollIntervalSummary(
				Utils.translateEntriesValues(
						"" + GeneralPrefsDO.getGCPollInterval(),
						values,
						entries)));
		lp.setEntries(entries);
		lp.setEntryValues(values);
		lp.setOnPreferenceChangeListener(mDeveloperPrefChanged);
	}

	private void showGeneralPreferences() {

		mOrder = 0;
		SwitchPreference sp;

		final boolean thereAreAccounts = Accounts.get().size() > 1;

		mAnchorPs = mPm.createPreferenceScreen(getActivity());
		mAnchorPs.setPersistent(false);
		mAnchorPs.setTitle(AutomatonAlert.mAppTitle + "Settings");
		mAnchorPs.setKey("Settings");
		mAnchorPs.setPersistent(false);
		setPreferenceScreen(mAnchorPs);

		sp = new SwitchPreference(getActivity());
		sp.setPersistent(false);
		sp.setOrder(mOrder++);
		sp.setTitle("Ringtones On/Off");
		sp.setKey(TAG_SYSTEM_ON);
		sp.setSwitchTextOff("Off");
		sp.setSwitchTextOn("On");
		sp.setChecked(GeneralPrefsDO.isSystemOn());
		mAnchorPs.addPreference(sp);
		sp.setOnPreferenceChangeListener(mGeneralPrefChanged);

		// Ringtone settings
		PreferenceScreen rtPs = mPm.createPreferenceScreen(getActivity());
		rtPs.setPersistent(false);
		rtPs.setOrder(mOrder++);
		rtPs.setTitle("Ringtone Settings");
		rtPs.setKey(TAG_RINGTONE_SETTINGS);
		mAnchorPs.addPreference(rtPs);
		rtPs.setOnPreferenceClickListener(
				new RingtoneSettingsButtonClickListener());

		// Edit email accounts
		if (thereAreAccounts) {
			PreferenceScreen editPs = mPm.createPreferenceScreen(getActivity());
			editPs.setPersistent(false);
			editPs.setOrder(mOrder++);
			editPs.setTitle(ACCOUNTS_LABEL);
			editPs.setKey(ACCOUNTS_PREF_BUTTON_ID);
			mAnchorPs.addPreference(editPs);
			editPs.setOnPreferenceClickListener(new AccountsButtonClickListener());
		}

		// Register new email account
		PreferenceScreen newPs = mPm.createPreferenceScreen(getActivity());
		newPs.setPersistent(false);
		newPs.setOrder(mOrder++);
		newPs.setTitle(ADD_NEW_EMAIL_ACCOUNT_LABEL);
		newPs.setKey(ADD_NEW_EMAIL_PREF_BUTTON_ID);
		mAnchorPs.addPreference(newPs);
		newPs.setOnPreferenceClickListener(new AddEmailButtonClickListener());

		//////////////
		// Alert/Alarm (also via Widget) control
		//////////////
		PreferenceCategory pc = new PreferenceCategory(getActivity());
		pc.setPersistent(false);
		pc.setOrder(mOrder++);
		pc.setTitle("Override All Ringtone's Volumes");
		pc.setKey("WidgetControl");
		pc.setLayoutResource(R.layout.preference_category_regular);
		mAnchorPs.addPreference(pc);

		mOverrideVolumePreference = new OverrideVolumePreference(getActivity(), this);
		mOverrideVolumePreference.setPersistent(false);
		mOverrideVolumePreference.setOrder(mOrder++);
		mOverrideVolumePreference.setKey(TAG_OVERRIDE_VOLUME);
		mAnchorPs.addPreference(mOverrideVolumePreference);

		//////////////
		// Quiet Time category title
		//////////////
		pc = new PreferenceCategory(getActivity());
		pc.setPersistent(false);
		pc.setOrder(mOrder++);
		pc.setTitle("Set Quiet Time");
		pc.setLayoutResource(R.layout.preference_category_regular);
		mAnchorPs.addPreference(pc);

		// Quiet Time
		mQuietTimePreference = new QuietTimePreference(getActivity(), this);
		mQuietTimePreference.setPersistent(false);
		mQuietTimePreference.setOrder(mOrder++);
		mQuietTimePreference.setKey(TAG_QUIET_TIME);
		mAnchorPs.addPreference(mQuietTimePreference);

		pc = new PreferenceCategory(getActivity());
		pc.setPersistent(false);
		pc.setOrder(mOrder++);
		pc.setTitle("Help");
		pc.setLayoutResource(R.layout.preference_category_regular);
		mAnchorPs.addPreference(pc);

		// "Help" button
		PreferenceScreen helpPs = mPm.createPreferenceScreen(getActivity());
		helpPs.setPersistent(false);
		helpPs.setOrder(mOrder++);
		helpPs.setTitle(HELP_LABEL);
		helpPs.setKey(HELP_PREF_BUTTON_ID);
		mAnchorPs.addPreference(helpPs);
		helpPs.setOnPreferenceClickListener(mOtherPrefClicked);
	}

	public static void callSelf(
			Context context, ShowMode mode, Pair<String, String> extra) {

		Intent intent = new Intent(context, FragmentHostActivity.class);
		intent.putExtra(TAG_SHOW_MODE, mode.name());
		intent.putExtra(
				AutomatonAlert.FRAGMENT_TYPE,
				HostFragmentType.GENERAL_SETTINGS.name());
		if (extra != null) {
			intent.putExtra(extra.first, extra.second);
		}
		intent.setFlags(
				  Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(intent);
	}

	class OtherPreferenceClickListener implements OnPreferenceClickListener {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			if (preference.getKey().equals(DEVELOPER_PREF_BUTTON_ID)) {
				getActivity().finish();
				callSelf(
						getActivity().getApplicationContext(),
						ShowMode.DEVELOPER,
						null);
			}
			else if (preference.getKey().equals(HELP_PREF_BUTTON_ID)) {
				Intent intent = new Intent(
						getActivity(), HelpActivity.class);
				startActivity(intent);
			}
			return true;
		}
	}

	class AccountButtonClickListener implements OnPreferenceClickListener {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			getActivity().finish();

			String key = preference.getKey().replace(SINGLE_ACCOUNT_PREF_BUTTON_ID + "|", "");
			Pair<String, String> pair =
					new Pair<String, String>(AutomatonAlert.ACCOUNT_KEY, key);

			callSelf(
					getActivity().getApplicationContext(), ShowMode.ACCOUNT, pair);

			return true;
		}
	}

	class AccountsButtonClickListener implements OnPreferenceClickListener {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			getActivity().finish();
			callSelf(
					getActivity().getApplicationContext(),
					ShowMode.ACCOUNT_LIST,
					null);

			return true;
		}
	}

	class AddEmailButtonClickListener implements OnPreferenceClickListener {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			if (Utils.inAppUpgradeCheckAccountsAtLimit(getActivity())) {
				return true;
			}
//			getActivity().finish();
            //davedel
            final Intent intent = new Intent(
                    getActivity(),
                    GmailActivity.class);
            startActivity(intent);
//			final Intent intent = new Intent(
//					getActivity(),
//					AccountAddUpdateActivity.class);
//			intent.putExtra(AutomatonAlert.M_MODE, AutomatonAlert.ADD);
//			startActivity(intent);
            //davedel

			return true;
		}
	}

	class RingtoneSettingsButtonClickListener implements OnPreferenceClickListener {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			Intent intent = new Intent(
					getActivity(), FragmentHostActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra(
					AutomatonAlert.FRAGMENT_TYPE,
					HostFragmentType.RINGTONE_SETTINGS.name());
			startActivity(intent);

			return true;
		}
	}

	class GeneralPreferenceChangeListener implements OnPreferenceChangeListener {
		boolean updateWidget = false;

		@Override
		public boolean onPreferenceChange(Preference preference, Object newVal) {
			boolean changed = false;
			String key = preference.getKey();

			if (TAG_SYSTEM_ON.equals(key)) {
				GeneralPrefsDO.setSystemOn((Boolean)newVal);
				changed = true;
			}

			if (changed) {
				GeneralPrefsDO.save();

				if (updateWidget) {
					updateWidget(getActivity());
				}
			}

			return true;
		}
	}

	public static void updateWidget(Activity activity) {
		Intent intent = new Intent(
				activity,
				AlertReceiver.class);
		intent.setAction(AutomatonAlert.WIDGET_UPDATE_2X1);
		intent.setFlags(0);
		activity.sendBroadcast(intent);
	}

	private String getCheckedSummary() {
		String lastChecked = "";
		if (mAccount != null
				&& mAccount instanceof AccountEmailDO) {
			long last = ((AccountEmailDO)mAccount).getLastChecked();
			if (last != 0
					&& last != -1) {
				lastChecked = Utils.toLocaleString(last);
			}
		}

		return "Tap to immediately scan this email account."
				+ (lastChecked.equals("") ?
						""
						:
						"  " + "\nLast checked " + lastChecked
				+ ". \n(Leave and return to see an updated date/time.)");

	}

	private String getImapRetriesSummary(String value) {


		return (value + ".  Number of times an IMAP command will be"
				+ " retried when that command fails.");
	}

	private String getGCPollIntervalSummary(String value) {

		return (value + ".  Garbage Collection will run at this rate.");
	}

	class DeveloperPreferenceChangeListener implements OnPreferenceChangeListener {

		@Override
		public boolean onPreferenceChange(Preference preference, Object newVal) {

			String key = preference.getKey();

			if (TAG_DEBUG_MODE.equals(key)) {
				GeneralPrefsDO.setDebugMode((Boolean)newVal);
				AutomatonAlert.DEBUG = GeneralPrefsDO.isDebugMode();
			}

			else if (TAG_FOREGROUND.equals(key)) {
				GeneralPrefsDO.setForeground((Boolean)newVal);

				Intent intent = new Intent(
						getActivity(),
						AlertReceiver.class);
				intent.setAction(AutomatonAlert.ACTION_FOREGROUND_BACKGROUND);
				intent.putExtra(TAG_FOREGROUND, GeneralPrefsDO.isForeground());
				intent.setFlags(0);
				getActivity().sendBroadcast(intent);

			}

			else if (TAG_IMAP_RETRIES.equals(key)) {
				GeneralPrefsDO.setImapMaxRetries(Utils.getInt((String) newVal, 1));
				preference.setSummary(getImapRetriesSummary(
						"" + GeneralPrefsDO.getImapMaxRetries()));
			}

			else if (TAG_GC_POLL_INTERVAL.equals(key)) {
				GeneralPrefsDO.setGCPollInterval(
						Utils.getInt((String) newVal, (int)GeneralPrefsDO.DEFAULT_GC_POLL));
				Utils.setGCPollAlarm(getActivity());
				preference.setSummary(getGCPollIntervalSummary(
						Utils.translateEntriesValues(
								"" + GeneralPrefsDO.getGCPollInterval(),
								getResources().getStringArray(
										R.array.alarm_snooze_values),
								getResources().getStringArray(
										R.array.alarm_snooze_entries))));
			}

			GeneralPrefsDO.save();

			return true;
		}
	}

	public static ShowMode getShowMode(String mode) {
		ShowMode showMode = ShowMode.GENERAL;

		try {
			showMode = ShowMode.valueOf(mode);
		} catch (IllegalArgumentException ignored) {
		} catch (NullPointerException ignored) {
		}

		return showMode;
	}

	@Override
	public void onStart() {
		super.onStart();

//		AutomatonAlert.populateAppData();

		mAnchorPs = null;
		mPm = getPreferenceManager();

		mPollChanged = new PollChangeListener();
		mPrefClicked = new PreferenceClickListener();
		mGeneralPrefChanged = new GeneralPreferenceChangeListener();
		mDeveloperPrefChanged = new DeveloperPreferenceChangeListener();
		mOtherPrefClicked = new OtherPreferenceClickListener();

		// 1. if showing only one account's preferences,
		//			the starting point is an account's preferences (not in xml)
		// 2. if showing list of accounts, findPreference(TAG_ACCOUNT_KEY) is where we start
		// 3. if showing all preferences, preferences.xml is the starting point
		if (mShowMode.equals(ShowMode.ACCOUNT)) {
			showAccountPreferences();
		}
		else if (mShowMode.equals(ShowMode.ACCOUNT_LIST)) {
			showAccountList();
		}
		else if (mShowMode.equals(ShowMode.DEVELOPER)){
			showDeveloperPreferences();
		}
		else {
			showGeneralPreferences();
		}
	}

	private void showAccountEmailPreferences(PreferenceScreen accountPs) {

		PreferenceCategory pc = new PreferenceCategory(getActivity());
		pc.setPersistent(false);
		ListPreference lp  = null;
		mOrder = 100;

		pc = new PreferenceCategory(getActivity());
		pc.setTitle(mAccount.getName());
		pc.setPersistent(false);
		pc.setOrder(mOrder++);
		pc.setLayoutResource(R.layout.preference_category_regular);
		accountPs.addPreference(pc);

		// message check interval
		lp = new ListPreference(getActivity());
		lp.setPersistent(false);
		lp.setOrder(mOrder++);
		lp.setTitle(Html.fromHtml(
				"<font color=\"#0099cc\">" + CHECK_FOR_NEW_MESSAGES_LABEL + "</font>"));
		lp.setKey(TAG_POLL);
		accountPs.addPreference(lp);
		lp.setValue("" + ((AccountEmailDO)mAccount).getPoll());
		lp.setSummary(getIntervalSummary(((AccountEmailDO) mAccount).getPoll()));
		lp.setEntries(R.array.account_settings_check_frequency_entries);
		lp.setEntryValues(R.array.account_settings_check_frequency_values);
		lp.setOnPreferenceChangeListener(mPollChanged);

		// check account now
		Preference cAps = mPm.createPreferenceScreen(getActivity());
		cAps.setPersistent(false);
		cAps.setOrder(mOrder++);
		cAps.setTitle(CHECK_ACCOUNT_NOW_LABEL);
		cAps.setKey(mAccount.getKey());
		accountPs.addPreference(cAps);
		cAps.setSummary(getCheckedSummary());
		cAps.setOnPreferenceClickListener(mPrefClicked);

		// check entire inbox next time
		Preference r1Aps = mPm.createPreferenceScreen(getActivity());
		r1Aps.setPersistent(false);
		r1Aps.setOrder(mOrder++);
		r1Aps.setTitle(RESET_MESSAGE_SEARCH_TO_1_LABEL);
		r1Aps.setKey(mAccount.getKey());
		accountPs.addPreference(r1Aps);
		r1Aps.setSummary("On the next scan, look at all messages in the Inbox.");
		r1Aps.setOnPreferenceClickListener(mPrefClicked);

		// check entire inbox next time
		Preference rtAps = mPm.createPreferenceScreen(getActivity());
		rtAps.setPersistent(false);
		rtAps.setOrder(mOrder++);
		rtAps.setTitle(RESET_MESSAGE_SEARCH_TO_TOP_LABEL);
		rtAps.setKey(mAccount.getKey());
		accountPs.addPreference(rtAps);
		rtAps.setSummary(
				"On next scan, look at only messages that are newer"
				+ " than what is currently in the Inbox.");
		rtAps.setOnPreferenceClickListener(mPrefClicked);

		// delete account
		PreferenceScreen dAps = mPm.createPreferenceScreen(getActivity());
		dAps.setPersistent(false);
		dAps.setOrder(mOrder++);
		accountPs.addPreference(dAps);
		dAps.setTitle(DELETE_ACCOUNT_LABEL);
		dAps.setKey(mAccount.getKey());
		dAps.setOnPreferenceClickListener(mPrefClicked);

		// update account
		PreferenceScreen uAps = mPm.createPreferenceScreen(getActivity());
		uAps.setPersistent(false);
		uAps.setOrder(mOrder++);
		accountPs.addPreference(uAps);
		uAps.setTitle(UPDATE_ACCOUNT_LABEL);
		uAps.setKey(mAccount.getKey());
		uAps.setOnPreferenceClickListener(mPrefClicked);
	}

	private String getIntervalSummary(int poll) {
		return
				"Sets the interval at which this email account is scanned for contacts."
				+ "\nCurrently: "
				+ convertPoll(poll);
	}

	private class PollChangeListener implements OnPreferenceChangeListener {

		@Override
		public boolean onPreferenceChange(Preference preference, Object newVal) {
			int iTime = Utils.getInt(newVal.toString(), -1);
			boolean checkEmailImmediately = false;
			// if it was "never/manual" and now it's not,
			// check email right away
			if (((AccountEmailDO) mAccount).getPoll() <= 0
					&& iTime > 0) {
				checkEmailImmediately = true;
			}
			// if it had a poll need to cancel previous alarm
			else if (((AccountEmailDO) mAccount).getPoll() > 0) {
				AutomatonAlert.getAPIs()
						.findCancelRemovePendingIntentsPostAlarms(
								AlarmPendingIntent.ApiType.EMAIL_POLL,
								ApiSubType.NONE,
								mAccount.mAccountId,
								-1,
								-1);
			}

			// set the value in the Account and save it
			preference.setSummary(getIntervalSummary(iTime));
			((AccountEmailDO) mAccount).setPoll(iTime);
			mAccount.save();

			Utils.setGCPollAlarm(getActivity());

			// send message to our server to reschedule
			Intent intent = new Intent();
			intent.putExtra(
					AutomatonAlert.ACCOUNT_KEY, mAccount.getKey());

			if (checkEmailImmediately) {
				intent.setAction(AutomatonAlert.ACTION_EMAIL_CHECK_MAIL);
				intent.setFlags(0);
				getActivity().sendBroadcast(intent);
			}

			// if it's changed to a value other than "never", reschedule check
			if (iTime > 0) {
				intent.setClass(
						getActivity(),
						AlertReceiver.class);
				intent.setAction(AutomatonAlert.ACTION_EMAIL_RESCHEDULE_POLL);
				intent.setFlags(0);
				getActivity().sendBroadcast(intent);
			}

			return true;
		}

	}

	private class PreferenceClickListener implements OnPreferenceClickListener {
		@Override
		public boolean onPreferenceClick(final Preference preference) {
			if (preference.getTitle().equals(DELETE_ACCOUNT_LABEL)) {
				deleteAccountDialog(preference);
				return true;
			}
			else if (preference.getTitle().equals(UPDATE_ACCOUNT_LABEL)) {
				callUpdateAccount(preference);
				return true;
			}
			else if (preference.getTitle().equals(CHECK_ACCOUNT_NOW_LABEL)) {
				callCheckAccountNow(preference);
				return true;
			}
			else if (preference.getTitle().equals(RESET_MESSAGE_SEARCH_TO_1_LABEL)) {
				resetAccountTo_1(preference.getKey());
				return true;
			}
			else if (preference.getTitle().equals(RESET_MESSAGE_SEARCH_TO_TOP_LABEL)) {
				resetAccountToTop(preference.getKey(), true/*toastIt*/);
				return true;
			}
			return false;
		}
	}

	private void callCheckAccountNow(Preference preference) {
		Intent intent = new Intent(
				getActivity(),
				AlertReceiver.class);
		intent.setAction(AutomatonAlert.ACTION_EMAIL_CHECK_MAIL);
		intent.putExtra(AutomatonAlert.ACCOUNT_KEY, preference.getKey());
		// check even if background data is off since this is a
		// foreground request
		intent.putExtra(AutomatonAlertService.TAG_IS_BACKGROUND_DATA_OK, true);
		intent.setFlags(0);
		getActivity().sendBroadcast(intent);
		Toast.makeText(
				getActivity(),
				"Scanning for email...",
				Toast.LENGTH_SHORT).show();
	}

	private void callUpdateAccount(Preference preference) {
		final Intent intent = new Intent(
				getActivity(),
				AccountAddUpdateActivity.class);
		intent.putExtra(AutomatonAlert.M_MODE, AutomatonAlert.UPDATE);
		intent.putExtra(AutomatonAlert.ACCOUNT_KEY, preference.getKey());
		startActivity(intent);
	}

	private void resetAccountTo_1(final String key) {
		Utils.okCancelDialogNative(
				getActivity(),
				"Are you sure you want to reset and search the"
						+ " entire INBOX?  If you have a large"
						+ " number of messages, the next scan"
						+ " could be very time-consuming.",
				new OkCancel() {
					@Override
					protected  void ok(DialogInterface dialog) {
						ArrayList<AccountDO> accounts = Accounts.get();
						for (final AccountDO account : accounts) {
							if (account.getKey().equalsIgnoreCase(key)) {
								((AccountEmailDO)account).setLatestUidProcessed("1");
								account.save();
								break;
							}
						}
						Toast.makeText(
								getActivity(),
								"Reset to all messages",
								Toast.LENGTH_SHORT).show();
					}
					@Override
					protected  void cancel(DialogInterface dialog) {
					}
				});
	}

	public static OkCancel getOkCancelResetAccountToTop(
			final String key, final boolean toastIt) {

		return new OkCancel() {
			@Override
			protected  void ok(DialogInterface dialog) {
				AccountDO targetAccount = Accounts.get(key);
				if (targetAccount != null) {
					String uid = findAccountTop((AccountEmailDO)targetAccount);
					if (uid != null) {
						((AccountEmailDO)targetAccount).setLatestUidProcessed(uid);
						targetAccount.save();
					}
					if (toastIt) {
						Toast.makeText(
								AutomatonAlert.THIS.getApplicationContext(),
								"Reset to new messages",
								Toast.LENGTH_SHORT).show();
					}
				}
			}
			@Override
			protected  void cancel(DialogInterface dialog) {
			}
		};
	}

	private void resetAccountToTop(final String key, boolean toastIt) {
		Utils.okCancelDialogNative(
				getActivity(),
				"Are you sure you want to reset?  Please note,"
				+ " resetting may take a moment.",
				getOkCancelResetAccountToTop(key, toastIt));
	}

//	private static Pattern mRegExCROrLF = Pattern.compile("\\r|\\n");

	private static String findAccountTop(AccountEmailDO account) {
		// not updating, don't need to tryAcquire()
		EmailGet mailHandle = AutomatonAlert.getMailGetSemaphores().get(
				account.getKey()).mMailHandle;

		return mailHandle.getTopUid();
	}

	private void deleteAccountDialog(final Preference preference) {
		String accountName = preference.getKey();
		if (accountName.contains("|")) {
			accountName = accountName.substring(0, accountName.indexOf("|"));
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage("Are you sure you want to delete " + accountName + "?")
		.setCancelable(false)
		.setPositiveButton("Yes, Delete", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				AccountDO account =
						Accounts.get(preference.getKey());
				if (account != null) {
					// delete all SourceAccounts with this accountId
					SourceAccountDO.delete(account.mAccountId);
					// delete this account from default accounts in SpecPref
					ArrayList<NameValueDataDO> specPrefs =
							NameValueDataDO.get(
									RTPrefsDO.getDefaultLinkedAccountSpecificPrefix(),
									true/*startsWith*/,
									""+account.getAccountId());
					for (NameValueDataDO specPref : specPrefs) {
						specPref.delete();
					}
					// cancel all alarms associate with this accountId
					AutomatonAlert.getAPIs()
							.findCancelRemovePendingIntentsPostAlarms(
									ApiType.EMAIL_POLL,
									ApiSubType.NONE,
									account.mAccountId,
									-1,
									-1);
					// delete the account
					account.delete();	// get rid of account in shared preferences
					dialog.dismiss();

					getActivity().finish();
					// land on account list screen
					callSelf(
							getActivity().getApplicationContext(),
							ShowMode.ACCOUNT_LIST,
							null);
				}

			}
		})
		.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		(builder.create()).show();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		AccountDO account = null;

		// coming back from SetAlertActivity with a NotificationItemId
		if (requestCode == 969) {
			String accountKey = intent.getStringExtra(AutomatonAlert.ACCOUNT_KEY);
			int nId = intent.getIntExtra(
					NotificationItemDO.TAG_NOTIFICATION_ITEM_ID, -1);
			int fId = intent.getIntExtra(FilterItemDO.TAG_FILTER_ITEM_ID, -1);
			if (accountKey != null
					&& nId >= 0
					&& fId >= 0) {
				account = Accounts.get(accountKey);
				if (account != null){
					FilterItemDO filterItem = FilterItems.get(fId);
					if (filterItem != null) {
						filterItem.setNotificationItemId(nId);
						filterItem.save();
					}
				}
			}
			getActivity().finish();
			callSelf(
					getActivity().getApplicationContext(),
					ShowMode.ACCOUNT,
					new Pair<String, String>(AutomatonAlert.ACCOUNT_KEY, accountKey));
		}

	}

	private String convertPoll(int milliseconds) {
		if (milliseconds < 0) {
			return AccountDO.ACCOUNT_CHECK_NEVER;
		}

		int time;
		if ((time = (milliseconds / 1000 / 60)) < 60) {
			return time + (time == 1 ? " minute" : " minutes");
		}

		time /= 60;
		return time + (time == 1 ? " hour" : " hours");

	}
}
