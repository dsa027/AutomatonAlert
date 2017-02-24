package com.aldersonet.automatonalert.Preferences;

import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.aldersonet.automatonalert.R;

public class ContactListSettingsFragment extends PreferenceFragment {

	public static final String TAG_SHOW_TEXT = "mShowText";
	public static final String TAG_SHOW_PHONE = "mShowPhone";
	public static final String TAG_SHOW_EMAIL = "mShowEmail";


	PreferenceManager mPrefMgr = null;

	ContactListPreferenceChangeListener mContactListPrefChanged =
			new ContactListPreferenceChangeListener();

	private IContactListSettingsFragmentListener mListener;

	public static ContactListSettingsFragment newInstance() {
		return new ContactListSettingsFragment();
	}

	public ContactListSettingsFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		mPrefMgr = getPreferenceManager();
		// create the preferences screen
		PreferenceScreen anchorScr = mPrefMgr.createPreferenceScreen(getActivity());
		anchorScr.setPersistent(false);
		anchorScr.setTitle("Contact List Settings");
		setPreferenceScreen(anchorScr);

		// create and set up preferences
		setAllPreference(anchorScr);
	}

	private void setAllPreference(PreferenceScreen anchorScr) {
		PreferenceCategory pc = null;
		CheckBoxPreference cbp = null;
//		String[] entries = null;
//		String[] values = null;
//		ListPreference lp = null;
//		Preference pref = null;
//		MultiSelectListPreference multi = null;

		pc = new PreferenceCategory(getActivity());
		pc.setPersistent(false);
		pc.setTitle("Contact List Settings");
		pc.setLayoutResource(R.layout.preference_category_highlight);
		anchorScr.addPreference(pc);

		pc = new PreferenceCategory(getActivity());
		pc.setPersistent(false);
		pc.setTitle("Show Tabs/Icons:");
		pc.setLayoutResource(R.layout.preference_category_regular);
		anchorScr.addPreference(pc);

		cbp = new CheckBoxPreference(getActivity());
		cbp.setPersistent(false);
		cbp.setTitle("Text/SMS/MMS");
		cbp.setKey(TAG_SHOW_TEXT);
		cbp.setChecked(ContactListPrefsDO.isShowText());
		anchorScr.addPreference(cbp);
		cbp.setOnPreferenceChangeListener(mContactListPrefChanged);

		cbp = new CheckBoxPreference(getActivity());
		cbp.setPersistent(false);
		cbp.setTitle("Phone");
		cbp.setKey(TAG_SHOW_PHONE);
		cbp.setChecked(ContactListPrefsDO.isShowPhone());
		anchorScr.addPreference(cbp);
		cbp.setOnPreferenceChangeListener(mContactListPrefChanged);

		cbp = new CheckBoxPreference(getActivity());
		cbp.setPersistent(false);
		cbp.setTitle("Email");
		cbp.setKey(TAG_SHOW_EMAIL);
		cbp.setChecked(ContactListPrefsDO.isShowEmail());
		anchorScr.addPreference(cbp);
		cbp.setOnPreferenceChangeListener(mContactListPrefChanged);

//		pc = new PreferenceCategory(getActivity());
//		pc.setPersistent(false);
//		pc.setTitle("Reset Error Messages");
//		pc.setLayoutResource(R.layout.preference_category_regular);
//		anchorScr.addPreference(pc);
//
//		pref = mPrefMgr.createPreferenceScreen(getActivity());
//		pref.setPersistent(false);
//		pref.setTitle("Reset Error Messages");
//		pref.setKey("mDontShowErrors");
//		anchorScr.addPreference(pref);
//		pref.setSummary("Press error messages again");
//		pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//			@Override
//			public boolean onPreferenceClick(Preference preference) {
//				return false;
//			}
//		});
	}

	class ContactListPreferenceChangeListener implements OnPreferenceChangeListener {

		boolean updatePrefs = false;

		@Override
		public boolean onPreferenceChange(Preference preference, Object newVal) {

			String key = preference.getKey();

			boolean ret = true;

			//
			if (TAG_SHOW_TEXT.equals(key)) {
				ContactListPrefsDO.setShowText((Boolean)newVal);
				updatePrefs = true;
			}
			else if (TAG_SHOW_PHONE.equals(key)) {
				ContactListPrefsDO.setShowPhone((Boolean)newVal);
				updatePrefs = true;
			}
			else if (TAG_SHOW_EMAIL.equals(key)) {
				ContactListPrefsDO.setShowEmail((Boolean)newVal);
				updatePrefs = true;
			}

			// make sure there's at least one tab/icon shown
			if (!ContactListPrefsDO.isShowText()
					&& !ContactListPrefsDO.isShowPhone()
					&& !ContactListPrefsDO.isShowEmail()) {
				Toast.makeText(
						getActivity(),
						"You must show at least one tab/icon.",
						Toast.LENGTH_SHORT).show();
//				ContactListPrefsDO.setShowText(true);
//				updatePrefs = true;
				ret = false;
			}

			if (updatePrefs) {
				ContactListPrefsDO.save();
			}

			return ret;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.custom_prefs_list,
				container, false);

	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (IContactListSettingsFragmentListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement IContactListSettingsFragmentListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	public interface IContactListSettingsFragmentListener {
	}

}
