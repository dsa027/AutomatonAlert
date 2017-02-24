package com.aldersonet.automatonalert.Preferences;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.aldersonet.automatonalert.Activity.FragmentHostActivity;
import com.aldersonet.automatonalert.Activity.FragmentHostActivity.HostFragmentType;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Fragment.RTChooserFragment;
import com.aldersonet.automatonalert.Fragment.VolumeChooserFragment;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Util.Utils;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class RTSettingsFragment extends PreferenceFragment {

	public static final String TAG_RINGTONE_REMINDERS   = "mRingtoneReminders";
	public static final String TAG_DEFAULT_RINGTONE     = "mDefaultRingtone";
	public static final String TAG_DEFAULT_VOLUME       = "mDefaultVolume";
	public static final String TAG_DEFAULT_TEXT_RT      = "mDefaultTextRT";
	public static final String TAG_DEFAULT_NEW_RT       = "mNewRTDefaults";

	PreferenceManager mPrefMgr;
	RingtoneListPreference mOlp;
	Preference mVp;
	Preference mDtp;
	Preference mDnp;
	PreferenceScreen mAnchorScr;

	PreferenceChangeListener mPrefChanged = new PreferenceChangeListener();
	PreferenceClickListener mPrefClicked = new PreferenceClickListener();

	boolean mViewCreated = false;
	FragmentHostActivity.HostFragmentType mFragmentType;

	public VolumeChooserFragment mVolumeChooserFragment;

	private IRTSettingsFragmentListener mListener;

	public static RTSettingsFragment newInstance(HostFragmentType type) {
		RTSettingsFragment fragment = new RTSettingsFragment();
		fragment.mFragmentType = type;

		return fragment;
	}

	public RTSettingsFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mViewCreated = false;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		mPrefMgr = getPreferenceManager();
		// create the preferences screen
		mAnchorScr = mPrefMgr.createPreferenceScreen(getActivity());
		mAnchorScr.setPersistent(false);
		mAnchorScr.setTitle("Contact Ringtone Settings");
		setPreferenceScreen(mAnchorScr);
		// create and set up preferences
		setAllPreference(mAnchorScr);
		mViewCreated = true;

		// GO DIRECTLY TO preference
		if (mFragmentType != null) {
			if (mFragmentType.equals(FragmentHostActivity.HostFragmentType.DEFAULT_RINGTONE)) {
				mOlp.show();

			} else if (mFragmentType.equals(FragmentHostActivity.HostFragmentType.DEFAULT_VOLUME)){
				mVolumeChooserFragment = VolumeChooserFragment.showInstance(
						(AppCompatActivity) getActivity());
			}
		}
	}

//	private String[] getRegisteredAccounts() {
//		ArrayList<NameValueDataDO> specPrefs = NameValueDataDO.get(
//				RTPrefsDO.getDefaultLinkedAccountSpecificPrefix(),
//				true /*startsWith*/,
//				null/*pref value*/);
//		if (specPrefs == null) {
//			specPrefs = new ArrayList<NameValueDataDO>(0);
//		}
//
//		// turn account numbers from specPrefs into account names
//		String[] ret = new String[specPrefs.size()];
//		int i=0;
//		for(NameValueDataDO specPref : specPrefs) {
//			String sAccountId = specPref.getValue();
//			int accountId = -1;
//			try {
//				accountId = Integer.parseInt(sAccountId);
//			} catch (NumberFormatException e) {
//				continue;
//			}
//			AccountDO account = Accounts.get(accountId);
//			if (account != null) {
//				ret[i++] = account.getName();
//			}
//		}
//		if (ret.length > 0) {
//			Arrays.sort(ret);
//		}
//		return ret;
//	}
//
//	private String[] slotAccounts(String[] allAccounts, String[]registeredAccounts) {
//		String[] ret = new String[allAccounts.length];
//		Arrays.fill(ret, null);
//
//		int N=registeredAccounts.length;
//		// place each original Source AccountDO into ret according to
//		// the order of the entries in orderedKeys
//		for (int i=0;i<N;i++) {
//			if (registeredAccounts[i] == null) {
//				continue;
//			}
//			int N2=allAccounts.length;
//			for (int j=0;j<N2;j++) {
//				// see if they both have the same name
//				if (registeredAccounts[i].equals(allAccounts[j])) {
//					ret[j] = registeredAccounts[i];
//					break;
//				}
//			}
//		}
//		return ret;
//	}
//
public RTChooserFragment mRTChooserFragment;

	// system ringtone/ringtone chooser
	class RingtoneListPreference extends ListPreference {
		Uri mCurrentRTUri;
		public RingtoneListPreference(Context context, String currentRTUri) {
			super(context);
			mCurrentRTUri = Uri.parse(currentRTUri == null ? "" : currentRTUri);
		}
		@Override
		protected void onPrepareDialogBuilder(@NotNull Builder builder) {
			super.onPrepareDialogBuilder(builder);
			int idx = findIndexOfValue(getDefaultRingtoneValue());
			final CharSequence[] entries = getEntries();
			final CharSequence[] values = getEntryValues();
			builder.setTitle(
					"Choosing \"Default\" as a ringtone will play this selection.");
			builder.setSingleChoiceItems(
					entries,
					idx,
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					int idx = Utils.getIndexOfEntry(
							"Choose Ringtone", Arrays.copyOf(
									entries, entries.length, String[].class));
					if (which == idx) {
//						enablePreferenceList(false/*enabled*/, false/*bgGone*/);
						mRTChooserFragment = RTChooserFragment.showInstance(
								(AppCompatActivity) getActivity(),
								FragmentTypeRT.SETTINGS,
								RTChooserFragment.getCurrentRTUri(mCurrentRTUri));
						// showInstance will return in this.getActivity()
						// in the fragment interaction listener.  It'll call
						// this.setRingtone().
					}
					else {
						callChangeListener(values[which]);
					}
					dialog.dismiss();
				}
			});
		}
		public void show() {
			showDialog(null);
		}
	}

	private void setAllPreference(PreferenceScreen anchorScr) {
//		PreferenceCategory pc = null;
//		ListPreference lp = null;
//		CheckBoxPreference cbp = null;
//		Preference pref = null;
//		MultiSelectListPreference multi = null;

//		pc = new PreferenceCategory(getActivity());
//		pc.setPersistent(false);
//		pc.setTitle("Ringtone Settings");
//		pc.setLayoutResource(R.layout.preference_category_highlight);
//		anchorScr.addPreference(pc);

		setRingtoneReminders(anchorScr);
		setRingtoneDefaults(anchorScr);
//		setupEmailRingtoneSettings(pc, multi, cbp, anchorScr);
//		setupBlockedSmsMms(pc, cbp, anchorScr);
		setResetErrorMessages(anchorScr);
	}

	private void setRingtoneReminders(PreferenceScreen anchorScr) {
		PreferenceCategory pc = null;
		SwitchPreference sp = null;
		Preference p = null;

		pc = new PreferenceCategory(getActivity());
		pc.setPersistent(false);
		pc.setTitle("Ringtone Reminders");
		pc.setLayoutResource(R.layout.preference_category_regular);
		anchorScr.addPreference(pc);

		sp = new SwitchPreference(getActivity());
		sp.setPersistent(false);
		sp.setTitle("Reminders On/Off");
		sp.setKey(TAG_RINGTONE_REMINDERS);
		sp.setSwitchTextOff("Off");
		sp.setSwitchTextOn("On");
		sp.setChecked(RTPrefsDO.isRemindersOn());
		anchorScr.addPreference(sp);
		sp.setOnPreferenceChangeListener(mPrefChanged);

		p = new Preference(getActivity());
		p.setSummary(Html.fromHtml(
				"<font color=\"#0099cc\">To create a reminder:</font>"
						+ " Choose \"Notification\" when you set up a ringtone."
						+ " You'll get a reminder every 5 minutes."
						+ "<br><font color=\"#0099cc\">To quiet reminders:</font>"
						+ " press or swipe the Text/Email status bar notification."
		));
		anchorScr.addPreference(p);

	}

	private void setRingtoneDefaults(PreferenceScreen anchorScr) {
		PreferenceCategory pc = null;
//		Preference pref = null;

		pc = new PreferenceCategory(getActivity());
		pc.setPersistent(false);
		pc.setTitle("Ringtone Defaults");
		pc.setLayoutResource(R.layout.preference_category_regular);
		anchorScr.addPreference(pc);

		mOlp = new RingtoneListPreference(getActivity(), RTPrefsDO.getDefaultRingtone());
		mOlp.setPersistent(false);
		mOlp.setTitle("Ringtone");
		mOlp.setKey(TAG_DEFAULT_RINGTONE);
		setDefaultRingtoneSummary(mOlp, RTPrefsDO.getDefaultRingtone());
		String[] entries = getDefaultRingtoneEntries();
		mOlp.setEntries(entries);
		mOlp.setEntryValues(R.array.default_ringtone_values);
		mOlp.setValue(getDefaultRingtoneValue());
		mOlp.setOnPreferenceChangeListener(mPrefChanged);
		anchorScr.addPreference(mOlp);

		mVp = new Preference(getActivity());
		mVp.setPersistent(false);
		mVp.setTitle("Volume");
		mVp.setKey(TAG_DEFAULT_VOLUME);
		setDefaultVolumeSummary(mVp, RTPrefsDO.getDefaultVolume());
		mVp.setOnPreferenceClickListener(mPrefClicked);
		anchorScr.addPreference(mVp);

		mDtp = new Preference(getActivity());
		mDtp.setPersistent(false);
		mDtp.setTitle("Global Text Ringtone");
		mDtp.setKey(TAG_DEFAULT_TEXT_RT);
		mDtp.setSummary(
				"Set the ringtone that will play for contacts"
				+ " that haven't been set (those that aren't in the Edit tab)."
		);
		mDtp.setOnPreferenceClickListener(mPrefClicked);
		anchorScr.addPreference(mDtp);

		mDnp = new Preference(getActivity());
		mDnp.setPersistent(false);
		mDnp.setTitle("Initial Values for New Ringtones");
		mDnp.setKey(TAG_DEFAULT_NEW_RT);
		mDnp.setSummary(
				"When adding a new contact ringtone, these will be the initial values.");
		mDnp.setOnPreferenceClickListener(mPrefClicked);
		anchorScr.addPreference(mDnp);
	}

	private String[] getDefaultRingtoneEntries() {
		String[] entries =
				getResources().getStringArray(R.array.default_ringtone_entries);
		int N=entries.length-1; // don't include the last entry, "Choose ringtone"
		for (int i=0;i<N;i++) {
			String[] uriTitle = Utils.getDefaultRingtoneUriAndTitle(
					getActivity(),
					entries[i],
					true/*trimTitleOfDefaultPhrase*/,
					false/*justTheSongName*/);
			entries[i] = uriTitle[1];
		}
		return entries;
	}

	private String getDefaultRingtoneValue() {
		String defaultRT = RTPrefsDO.getDefaultRingtone();
		String[] values =
				getResources().getStringArray(R.array.default_ringtone_values);

		boolean foundIt = false;
		for (String compare : values) {
			if (compare.equals(defaultRT)) {
				foundIt = true;
				break;
			}
		}

		if (foundIt) {
			return defaultRT;
		}
		else {
			return "choose";
		}
	}

//	private void setupEmailRingtoneSettings(
//			PreferenceCategory pc,
//			MultiSelectListPreference multi,
//			CheckBoxPreference cbp,
//			PreferenceScreen anchorScr) {
//
//		pc = new PreferenceCategory(getActivity());
//		pc.setPersistent(false);
//		pc.setTitle("Email Settings");
//		pc.setLayoutResource(R.layout.preference_category_regular);
//		anchorScr.addPreference(pc);
//
//		multi = new MultiSelectListPreference(getActivity());
//		multi.setPersistent(false);
//		multi.setTitle("Default Registered Accounts");
//		multi.setKey("mRegisteredAccounts");
//		multi.setSummary(
//				"These email accounts will be available to new email ringtones.");
//		multi.setDialogTitle("Select Default Email Accounts");
//		setupMultiAccounts(multi);
//		multi.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//			@Override
//			public boolean onPreferenceChange(Preference preference, Object newValue) {
//				HashSet<?> values = (HashSet<?>)newValue;
//				values.remove(null);
//				NameValueDataDO.replaceAllPrefix(
//						RTPrefsDO.getDefaultLinkedAccountSpecificPrefix(),
//						values.toArray(new String[values.size()]));
//				return true;
//			}
//		});
//		anchorScr.addPreference(multi);
//
//		cbp = new CheckBoxPreference(getActivity());
//		cbp.setPersistent(false);
//		cbp.setTitle("Auto-Register New Email Accounts");
//		cbp.setKey("mAutoAddNewAccountsToDefault");
//		cbp.setSummary("When adding a new account in the app, add as a default registered account.");
//		cbp.setChecked(
//				(RTPrefsDO.getAutoAddNewAccountsToDefault().equals("1")) ? true : false);
//		cbp.setOnPreferenceChangeListener(mPollChanged);
//		anchorScr.addPreference(cbp);
//
//		cbp = new CheckBoxPreference(getActivity());
//		cbp.setPersistent(false);
//		cbp.setTitle("Auto-Add To Active Ringtones");
//		cbp.setKey("mAutoAddNewAccountsToActive");
//		cbp.setSummary("Also add it to already active ringtones.");
//		cbp.setChecked(
//				(RTPrefsDO.getAutoAddNewAccountsToActive().equals("1")) ? true : false);
//		cbp.setOnPreferenceChangeListener(mPollChanged);
//		anchorScr.addPreference(cbp);
//	}

//	private void setupBlockedSmsMms(
//			PreferenceCategory pc, CheckBoxPreference cbp, PreferenceScreen anchorScr) {
//
//		pc = new PreferenceCategory(getActivity());
//		pc.setPersistent(false);
//		pc.setTitle("Blocked SMS/MMS");
//		pc.setLayoutResource(R.layout.preference_category_regular);
//		anchorScr.addPreference(pc);
//
//		cbp = new CheckBoxPreference(getActivity());
//		cbp.setPersistent(false);
//		cbp.setTitle("Show SMS/MMS Blocked message");
//		cbp.setKey("mShowToastOnSmsMmsBlock");
//		cbp.setSummary("This will show a brief message when an SMS/MMS is blocked");
//		cbp.setChecked(
//				(RTPrefsDO.getShowToastOnSmsMmsBlock().equals("1")) ? true : false);
//		cbp.setOnPreferenceChangeListener(mPollChanged);
//		anchorScr.addPreference(cbp);
//	}

	private void setResetErrorMessages(PreferenceScreen anchorScr) {
		PreferenceCategory pc = null;
		Preference pref = null;

		pc = new PreferenceCategory(getActivity());
		pc.setPersistent(false);
		pc.setTitle("Reset Error/Warning Messages");
		pc.setLayoutResource(R.layout.preference_category_regular);
		anchorScr.addPreference(pc);

		pref = mPrefMgr.createPreferenceScreen(getActivity());
		pref.setPersistent(false);
		pref.setTitle("Contact Has No Email");
		pref.setKey("RT_ERROR_NO_CONTACT_EMAIL_DONT_SHOW");
		pref.setSummary("Press to see this message again");
		pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				resetErrorAndToast(
						NameValueDataDO.RT_ERROR_NO_CONTACT_EMAIL_DONT_SHOW);
				return false;
			}
		});
		anchorScr.addPreference(pref);

		pref = mPrefMgr.createPreferenceScreen(getActivity());
		pref.setPersistent(false);
		pref.setTitle("Ringtone Is Too Long to Loop");
		pref.setKey("RT_WARNING_LONG_SONG_DURATION_DONT_SHOW");
		pref.setSummary("Press to see this message again");
		pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				resetErrorAndToast(
						NameValueDataDO.RT_WARNING_LONG_SONG_DURATION_DONT_SHOW);
				return false;
			}
		});
		anchorScr.addPreference(pref);

		pref = mPrefMgr.createPreferenceScreen(getActivity());
		pref.setPersistent(false);
		pref.setTitle("Clear Delete Warnings");
		pref.setKey("RT_WARNING_ON_DELETE_DONT_SHOW");
		pref.setSummary("Press to see this message again");
		pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				resetErrorAndNoToast(
						NameValueDataDO.RT_WARNING_ON_DELETE_DONT_SHOW);
				resetErrorAndNoToast(
						NameValueDataDO.RT_WARNING_ON_FREEFORM_DELETE_DONT_SHOW);

				resetErrorAndToast(
						NameValueDataDO.RT_WARNING_ON_ALARM_DELETE_DONT_SHOW);
				return false;
			}
		});
		anchorScr.addPreference(pref);

		pref = mPrefMgr.createPreferenceScreen(getActivity());
		pref.setPersistent(false);
		pref.setTitle("No Email Accounts Are Set Up");
		pref.setKey("RT_ERROR_NO_ACCOUNTS_SET_UP_DONT_SHOW");
		pref.setSummary("Press to see this message again");
		pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				resetErrorAndToast(
						NameValueDataDO.RT_ERROR_NO_ACCOUNTS_SET_UP_DONT_SHOW);
				return false;
			}
		});
		anchorScr.addPreference(pref);

//		pref = mPrefMgr.createPreferenceScreen(getActivity());
//		pref.setPersistent(false);
//		pref.setTitle("Reset 'Blocked SMS/MMS' Warning");
//		pref.setKey("mDontShowWarningBlockedSmsMms");
//		pref.setSummary("Press if you'd like to see this warning message again");
//		pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//			@Override
//			public boolean onPreferenceClick(Preference preference) {
//				NameValueDataDO.set(
//						NameValueDataDO.RT_WARNING_BLOCKED_TEXT_DONT_SHOW,
//						"0");
//				return false;
//			}
//		});
//		anchorScr.addPreference(pref);
	}
//	private void setupMultiAccounts(MultiSelectListPreference multi) {
//		Pair<String[], String[]> accountsPair =
//				Accounts.getAllAccountsNameKeyArrays(AccountEmailDO.ACCOUNT_EMAIL);
//		String[] accounts = accountsPair.first;	// name array
//		String[] registeredAccounts = getRegisteredAccounts();
//		String[] slottedAccounts = slotAccounts(accounts, registeredAccounts);
//		multi.setEntries(accounts);
//		multi.setEntryValues(accounts);
//		HashSet<String> hashSet = new HashSet<String>(accounts.length);
//		Collections.addAll(hashSet, slottedAccounts);
//		multi.setValues(hashSet);
//	}

	private void resetErrorAndToast(String whichOne) {
		resetErrorAndNoToast(whichOne);
		Toast.makeText(getActivity(), "Reset", Toast.LENGTH_SHORT).show();
	}

	private void resetErrorAndNoToast(String whichOne) {
		NameValueDataDO.set(whichOne, "0");
	}

	private void setDefaultRingtoneSummary(Preference preference, final String newVal) {
		String[] uriAndTitle = new String[] { "", "" };
//		String val = "";

		// ACTUAL TRACK
		if (newVal.contains(AutomatonAlert.CONTENT_PREFIX)) {
			uriAndTitle[1] = Utils.getSongName(Uri.parse(newVal));
		}
		// SILENT
		else if (newVal.equalsIgnoreCase(AutomatonAlert.SILENT)) {
			uriAndTitle[1] = AutomatonAlert.SILENT_LABEL;
		}
		// DEFAULT
		else {
			uriAndTitle =
					Utils.getDefaultRingtoneUriAndTitle(
							getActivity(),
							newVal,
							true/*trimTitleOfDefaultPhrase*/,
							false/*justTheSongName*/);
			// came back as default, so just save the content:// string
			if (newVal.equalsIgnoreCase(AutomatonAlert.DEFAULT)) {
				RTPrefsDO.setDefaultRingtone(uriAndTitle[0]);
				RTPrefsDO.save();
			}
		}
		preference.setSummary(
				"This ringtone will be used when selecting \"Default\" in the ringtone chooser. " +
						"\nCurrently: " +	uriAndTitle[1] + ".");
	}

	public void setDefaultVolumeSummary(String vol) {
		setDefaultVolumeSummary(mVp, vol);
	}

	private void setDefaultVolumeSummary(Preference preference, final String newVal) {
		String sVal = newVal;
		String suffix = null;

		// if it's a string, init cap it
		if (Utils.getInt(newVal, -1) == -1) {
			sVal = Utils.initCap(sVal);
			suffix = sVal + " volume";
		}
		else {
			suffix = sVal + "/" + RTPrefsDO.INTERNAL_VOLUME_MAX;
		}

		preference.setSummary(
				"Volume will play at this level when selecting \"Volume Default\""
						+ " in the volume circle. \nCurrently: " + suffix + ".");
	}

	public void enablePreferenceList(boolean enabled, boolean bgGone) {
		if (getView() == null) {
			return;
		}
		View v = getView().findViewById(android.R.id.list);
		if (v != null) {
			v.setEnabled(enabled);
			float alpha = (enabled ? 1F : (bgGone ? .0F : .3F));
			v.setAlpha(alpha);
		}
	}

	class PreferenceClickListener implements OnPreferenceClickListener {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			String key = preference.getKey();
			//
			// Volume Chooser (VolumeChooserFragment)
			if (TAG_DEFAULT_VOLUME.equals(key)) {
				mVolumeChooserFragment = VolumeChooserFragment.showInstance(
						(AppCompatActivity) getActivity());
			}
			// Global Text Ringtone (RTUpdateFragment)
			else if (TAG_DEFAULT_TEXT_RT.equals(key)) {
				Intent intent = new Intent(getActivity(), FragmentHostActivity.class);
				intent.putExtra(
						AutomatonAlert.FRAGMENT_TYPE,
						HostFragmentType.GLOBAL_RINGTONE.name());
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				getActivity().startActivity(intent);
			}
			// Initial Values for New Ringtones (RTUpdateFragment)
			else if (TAG_DEFAULT_NEW_RT.equals(key)) {
				Intent intent = new Intent(getActivity(), FragmentHostActivity.class);
				intent.putExtra(
						AutomatonAlert.FRAGMENT_TYPE,
						HostFragmentType.NEW_RINGTONE_VALUES.name());
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				getActivity().startActivity(intent);
			}

			return true;
		}
	}

	class PreferenceChangeListener implements OnPreferenceChangeListener {
		boolean updatePrefs = false;

		@Override
		public boolean onPreferenceChange(Preference preference, Object newVal) {

			String key = preference.getKey();

			boolean ret = true;

			//
			if (TAG_DEFAULT_RINGTONE.equals(key)) {
				RTPrefsDO.setDefaultRingtone((String)newVal);
				setDefaultRingtoneSummary(preference, newVal.toString());
				updatePrefs = true;
			}

			if (TAG_RINGTONE_REMINDERS.equals(key)) {
				RTPrefsDO.setRemindersOn((Boolean)newVal);
				updatePrefs = true;
			}

			//
//			else if ("mAutoAddNewAccountsToDefault".equals(key)) {
//				RTPrefsDO.setAutoAddNewAccountsToDefault(
//						(((Boolean) newVal) == true) ? "1" : "0");
//				updatePrefs = true;
//			}

			//
//			else if ("mAutoAddNewAccountsToActive".equals(key)) {
//				RTPrefsDO.setAutoAddNewAccountsToActive(
//						(((Boolean) newVal) == true) ? "1" : "0");
//				updatePrefs = true;
//			}

			//
//			else if ("mShowToastOnSmsMmsBlock".equals(key)) {
//				RTPrefsDO.setShowToastOnSmsMmsBlock(
//						(((Boolean)newVal) == true) ? "1" : "0");
//				updatePrefs = true;
//			}

			if (updatePrefs) {
				RTPrefsDO.save();
			}

			return ret;
		}
	}

	public void setRingtone(Uri uri) {
		String newVal = uri.toString();
		RTPrefsDO.setDefaultRingtone(newVal);
		RTPrefsDO.save();
		setDefaultRingtoneSummary(mOlp, newVal);
	}

//	public void setVolume(String newVolume) {
//		RTPrefsDO.setDefaultVolume(newVolume);
//		RTPrefsDO.save();
//		setDefaultVolumeSummary(mVp, newVolume);
//	}

	public void save() {
		RTPrefsDO.save();
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

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (IRTSettingsFragmentListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement IRTSettingsFragmentListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	public interface IRTSettingsFragmentListener {
	}
}
