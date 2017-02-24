package com.aldersonet.automatonalert.Fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aldersonet.automatonalert.Account.AccountDO;
import com.aldersonet.automatonalert.Account.Accounts;
import com.aldersonet.automatonalert.Activity.AccountAddUpdateActivity;
import com.aldersonet.automatonalert.Activity.AlarmVisualActivity;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.Activity.SetAlertActivity;
import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.Alert.AlertItems;
import com.aldersonet.automatonalert.Alert.NotificationItemDO;
import com.aldersonet.automatonalert.Alert.NotificationItems;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Email.AccountEmailDO;
import com.aldersonet.automatonalert.Filter.FilterItemAccountDO;
import com.aldersonet.automatonalert.Filter.FilterItemAccounts;
import com.aldersonet.automatonalert.Filter.FilterItemDO;
import com.aldersonet.automatonalert.Filter.FilterItems;
import com.aldersonet.automatonalert.Fragment.VolumeChooserFragment.VolumeTypes;
import com.aldersonet.automatonalert.OkCancel.OkCancel;
import com.aldersonet.automatonalert.OkCancel.OkCancelDialog;
import com.aldersonet.automatonalert.OkCancel.OkCancelDialog.EWI;
import com.aldersonet.automatonalert.Picker.DatePickerTimePicker;
import com.aldersonet.automatonalert.Preferences.GeneralPrefsDO;
import com.aldersonet.automatonalert.Preferences.GeneralPrefsDO.OverrideVolLevel;
import com.aldersonet.automatonalert.Preferences.NameValueDataDO;
import com.aldersonet.automatonalert.Preferences.RTPrefsDO;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Service.AutomatonAlertService;
import com.aldersonet.automatonalert.SourceAccount.SourceAccountDO;
import com.aldersonet.automatonalert.SourceType.SourceTypeDO;
import com.aldersonet.automatonalert.Util.EmailAccountDialog;
import com.aldersonet.automatonalert.Util.Enums;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RTUpdateFragment extends Fragment {

	public static final String TAG = "RTUpdateFragment";

	public enum Mode {
		RINGTONE,           // just a ringtone
		ALARM,              // Set alarm (regular/repeating)
		FREEFORM,           // Free-form email alert
		DEFAULT_TEXT_RT,    // Default text ringtone for contacts not set
		DEFAULT_NEW_RT,     // Default new values for a ringtone
		UNKNOWN
	}
	public static final String TESTIT = "Test!";
	public static final String STOPIT = "Stop";

	public static final String TAG_DATA_SAVED = "saved";
	public static final long LONGEST_RT_BEFORE_WARNING = 4999; // if (len > WARNING)

	private IRTUpdateFragmentListener mActivityListener;
	IRTMaster mMaster;

	public Mode mMode;
	AlertItemDO mAlertItem;
	String mLookupKey;
	String mDisplayName;
	String mPreviousActionBarTitle;
	//error fields
	boolean mHasErrorNoEmailAccountsBeenShown;
	boolean mHasErrorNoContactEmailBeenShown;
	boolean mHasWarningSongDurationBeenShown;
	boolean mHasInfoMessageBlockedMessageBeenShown;

	public FragmentTypeRT mFragmentType;
	OkCancelDialog mOkCancelDialog;

	// Ringtone
	View mTopView;
	TextView mRingtone;
	TextView mRingtoneHeader;
	View mInitValuesSpacer;
	Button mAccounts;
	TextView mAccountList;
	Spinner mSilentModeSpinner;
	TextView mSilentModeHeader;
	Spinner mPlayForSpinner;
	TextView mPlayForHeader;
	Spinner mVibrateModeSpinner;
	TextView mVibrateModeHeader;
	Spinner mNotificationSpinner;
	TextView mNotificationHeader;
	Spinner mLedModeSpinner;
	TextView mLedModeHeader;
	ImageButton mVolDial;
	ImageButton mClearButton;
	public Button mTestButton;

	Uri mRingtoneUri;
	NotificationItemDO mNotificationItem;
	SourceTypeDO mSourceType;
	FilterItemDO mFilterItem;
	int mFilterItemId;
	Intent mIntent;
	long mSongDuration;
	boolean mInitializing;

	boolean mContactHasEmail = false;
	boolean mRingtoneInitialized = false;
	boolean mInitializingSilentModeSpinner = false;
	boolean mInitializingPlayForSpinner = false;
	boolean mInitializingVibrateModeSpinner = false;
	boolean mInitializingNotificationSpinner = false;
	boolean mInitializingLedModeSpinner = false;

	public static RTUpdateFragment newInstance(
			Mode mode, FragmentTypeRT fragmentType,
			String displayName, int alertItemId, int filterItemId) {

		RTUpdateFragment fragment = new RTUpdateFragment();
		putArgs(fragment, fragmentType, mode, displayName, alertItemId, filterItemId);

		return fragment;
	}

	private static void putArgs(Fragment fragment, FragmentTypeRT fragmentType,
			Mode mode, String displayName, int alertItemId, int filterItemId) {

		Bundle args = new Bundle();
		args.putString(
				AutomatonAlert.FRAGMENT_TYPE,
				fragmentType == null ?
						FragmentTypeRT.SETTINGS.name() : fragmentType.name());
		args.putString(
				AutomatonAlert.M_MODE,
				mode == null ?
						Mode.UNKNOWN.name() : mode.name());
		args.putString(Contacts.DISPLAY_NAME, displayName);
		args.putInt(AlertItemDO.TAG_ALERT_ITEM_ID, alertItemId);
		args.putInt(FilterItemDO.TAG_FILTER_ITEM_ID, filterItemId);
		fragment.setArguments(args);
	}

	public void setMaster(IRTMaster controller) {
		mMaster = controller;
	}

	public static Mode getMode(String mode) {
		try {
			return Mode.valueOf(mode);
		} catch (NullPointerException npe) {
			return null;
		} catch (IllegalArgumentException iae) {
			return null;
		}
	}

	private RTUpdateActivity getRTActivityOrNull() {
		if (getActivity() instanceof RTUpdateActivity) {
			return (RTUpdateActivity)getActivity();
		}

		return null;
	}

	private boolean isInitialErrorCheckDone() {
		if (mMaster != null) {
			return mMaster.isInitialErrorCheckDone();
		}
		else {
			RTUpdateActivity activity = getRTActivityOrNull();
			if (activity == null) {
				return false;
			}
			int idx = mFragmentType.ordinal();
			return activity.mIsInitialErrorCheckDone[idx];
		}
	}

	private void setInitialErrorCheckDone() {
		if (mMaster != null) {
			mMaster.setInitialErrorCheckDone();
		}
		else {
			RTUpdateActivity activity = getRTActivityOrNull();
			if (activity == null) {
				return;
			}
			int idx = mFragmentType.ordinal();
			activity.mIsInitialErrorCheckDone[idx] = true;
		}
	}

	public void initialChecks() {
		// no error checking for SETTINGS
		if (mFragmentType.equals(FragmentTypeRT.SETTINGS)) {
			return;
		}

		if (isInitialErrorCheckDone()) {
			return;
		}

		// do initial check if we haven't already.
		// make sure we're showing before checking for errors
		// see if our Fragment is actually showing. If not, return
		RTUpdateActivity activity = getRTActivityOrNull();
		if (mMode.equals(Mode.FREEFORM)
				|| (activity != null
					&& activity.isThisFragmentShowingNow(mFragmentType))) {
			Log.d(
					TAG, ".initialChecks(): " +
					"about to checkErrors for ["
							+ (mMode.equals(Mode.FREEFORM) ? Mode.FREEFORM.name() : mFragmentType)
							+ "]");

			checkForErrorsAndWarnings(false/*checkSongDuration*/);
			setInitialErrorCheckDone();
		}
		else {
			Log.d(
					TAG, ".initialChecks(): " +
					"not showing, no errorChk ["
							+ (mMode.equals(Mode.FREEFORM) ? Mode.FREEFORM.name() : mFragmentType)
							+ "]");
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		showEmailAccounts();
		Log.d(TAG, ".onResume(): calling initialChecks()");
		initialChecks();
//		checkForErrorsAndWarningsSetAlarm();
	}

	private void checkForErrorsAndWarnings(boolean checkSongDuration) {
		if (mFragmentType.equals(FragmentTypeRT.EMAIL)) {
			checkForEmailErrors();
		}
		else {
			if (isTextMessageBlocked()) {
				showInfoMessageBlocked();
			}
		}

		// if song duration > x seconds, see if user is asking to loop
		// audio. if so, check if TEXT or EMAIL. If so, warn that looping
		// will go on for n hours/minutes/seconds.
		if (checkSongDuration) {
			if (!mFragmentType.equals(FragmentTypeRT.PHONE)
					&& mSongDuration > LONGEST_RT_BEFORE_WARNING) {
				checkForSongDurationWarning(-1);
			}
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		resetTextAndNewRTTitle();
	}

	private void resetTextAndNewRTTitle() {
		// if we changed the title, set it back
		if (mMode.equals(Mode.DEFAULT_TEXT_RT)
				|| mMode.equals(Mode.DEFAULT_NEW_RT)) {
			if (getActivity() instanceof RTUpdateActivity) {
				// reset ActionBar title
				RTUpdateActivity activity = (RTUpdateActivity)getActivity();
				Utils.setActionBarTitle(
						activity.getSupportActionBar(),
						mPreviousActionBarTitle);
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set return data
		mIntent = getActivity().getIntent();
		mIntent.setData(Uri.parse(""));

		initFields();
		getArgs();

		// we can do the title here (instead of in Activity)
		// because these fragments are created and
		// destroyed outside the ViewPager (which caches fragments)
		setTextAndNewRTTitle();
	}

	private void setTextAndNewRTTitle() {
		// Change actionBar title to "Default Text RT"
		if (mMode.equals(Mode.DEFAULT_TEXT_RT)
				|| mMode.equals(Mode.DEFAULT_NEW_RT)) {
			if (getActivity() instanceof RTUpdateActivity) {
				ActionBar ab = ((RTUpdateActivity)getActivity()).getSupportActionBar();
				mPreviousActionBarTitle = ab.getTitle().toString();
				Utils.setActionBarTitle(
						((RTUpdateActivity)getActivity()).getSupportActionBar(),
						mDisplayName);
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	private void initFields() {
		mFragmentType = FragmentTypeRT.SETTINGS;
		mMode = Mode.UNKNOWN;
		mDisplayName = "<unknown>";
		mAlertItem = null;
		mFilterItem = null;
		mHasErrorNoEmailAccountsBeenShown = false;
		mHasErrorNoContactEmailBeenShown = false;
		mHasWarningSongDurationBeenShown = false;
		mHasInfoMessageBlockedMessageBeenShown = false;
	}

	private void getArgs() {
		Bundle args = getArguments();
		if (args != null) {
			// get args
			mMode = getMode(args.getString(AutomatonAlert.M_MODE, Mode.UNKNOWN.name()));

			switch (mMode) {
				case ALARM:
					mFragmentType = FragmentTypeRT.TEXT;
					break;
				case FREEFORM:
					mFragmentType = FragmentTypeRT.EMAIL;
					mFilterItemId = args.getInt(FilterItemDO.TAG_FILTER_ITEM_ID, -1);
					break;
				default:
					mFragmentType =
							Enums.getEnum(
									args.getString(
											AutomatonAlert.FRAGMENT_TYPE,
											FragmentTypeRT.SETTINGS.name()),
									FragmentTypeRT.values(),
									null
							);
					break;
			}
			mDisplayName = args.getString(Contacts.DISPLAY_NAME, "<unknown>");
			int alertItemId = args.getInt(AlertItemDO.TAG_ALERT_ITEM_ID, -1);
			if (alertItemId != -1) {
				mAlertItem = AlertItems.get(alertItemId);
			}
		}
	}

	public void reSetFilterItem(FilterItemDO filterItem) {
		mFilterItem = filterItem;
		mFilterItemId = filterItem == null ? -1 : filterItem.getFilterItemId();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopTestSound();
	}

	private boolean contactHasEmail() {
		if (mLookupKey == null) {
			mLookupKey = getLookupKey();
		}
		if (!TextUtils.isEmpty(mLookupKey)) {
			ArrayList<String> emails =
					Utils.getEmailAddresses(
							getActivity(), mLookupKey, 1);
			if (emails.size() > 0) {
				return true;
			}
		}
		return false;
	}

	private OkCancel getEmailErrorOkCancel() {
		return new OkCancel() {
			// IGNORE
			@Override
			protected  void ok(DialogInterface dialog) {
				OkCancelDialog.saveOkCancelCheckBox(
						mOkCancelDialog,
						NameValueDataDO.RT_ERROR_NO_CONTACT_EMAIL_DONT_SHOW);
			}

			// ADD EMAIL
			@Override
			protected  void cancel(DialogInterface dialog) {
				String id = mLookupKey;
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_EDIT);
				intent.setData(Uri.parse(Contacts.CONTENT_LOOKUP_URI + "/" + id));
				getActivity().startActivityForResult(intent, 1);

				OkCancelDialog.saveOkCancelCheckBox(
						mOkCancelDialog,
						NameValueDataDO.RT_ERROR_NO_CONTACT_EMAIL_DONT_SHOW);
			}
		};
	}

	private void checkForEmailErrors() {
		// see if our Fragment is actually showing. If not, return
		if (getActivity() instanceof RTUpdateActivity) {
			if (!(((RTUpdateActivity)getActivity()).isThisFragmentShowingNow(mFragmentType))) {
				return;
			}
		}

		//
		showErrorNoEmailAccounts(processEmailAccounts());

		/////////////////////////////
		// no need to check if contact has email for FREEFORM
		/////////////////////////////
		if (mMode.equals(Mode.FREEFORM)) {
			return;
		}

		OkCancelDialog.UserActionOnChecked skipDialog =
				OkCancelDialog.isSkipDialog(
						NameValueDataDO.RT_ERROR_NO_CONTACT_EMAIL_DONT_SHOW);

		// don't do anything ok()/cancel() if not shown
		if (!skipDialog.equals(OkCancelDialog.UserActionOnChecked.DONT_SKIP)) {
			return;
		}

		// show the message if there's no email for the contact.
		// if there's no error, make sure mRingtone attributes are correct
		mContactHasEmail = contactHasEmail();
		if (!mContactHasEmail) {
			showErrorNoContactEmail(false/*dontShow*/);
		}

	}

	public void showErrorNoContactEmail(boolean skipDialog) {
		stopTestSound();

		if (mHasErrorNoContactEmailBeenShown) {
			return;
		}
		mHasErrorNoContactEmailBeenShown = true;

		OkCancel okCancel = getEmailErrorOkCancel();

//		boolean skipDialog =
//				isSkipDialog(
//						NameValueDataDO.RT_ERROR_NO_CONTACT_EMAIL_DONT_SHOW);

		if (!skipDialog) {
			mOkCancelDialog = OkCancelDialog.showInstance(
					(AppCompatActivity) getActivity(),
					"Contact <b>" + mDisplayName + "</b>"
							+ " has no email."
							+ " <b>Go to Contacts and add an email?</b>"
							+ "<br><br>This ringtone won't work unless"
							+ " the contact has an email address.",
					"", //"Don't show this message again for any contact",
					"Add Email",
					"Ignore This Time",
					OkCancelDialog.CancelButton.RIGHT,
					EWI.ERROR
			);
			mOkCancelDialog.setOkCancel(okCancel);
		}
		else {
			okCancel.doRightButtonPressed();
		}
	}

	private OkCancel getBlockedMessageOkCancel() {
		return new OkCancel() {
			// OK - do nothing
			@Override
			public void ok(DialogInterface dialog) {
				OkCancelDialog.saveOkCancelCheckBox(
						mOkCancelDialog,
						NameValueDataDO.RT_WARNING_BLOCKED_MESSAGE_DONT_SHOW);
			}

			// CANCEL More Info //
			@Override
			public void cancel(DialogInterface dialog) {
				OkCancelDialog.saveOkCancelCheckBox(
						mOkCancelDialog,
						NameValueDataDO.RT_WARNING_BLOCKED_MESSAGE_DONT_SHOW);
			}
		};
	}

	private void showInfoMessageBlocked() {
		stopTestSound();

		if (mHasInfoMessageBlockedMessageBeenShown) {
			return;
		}
		mHasInfoMessageBlockedMessageBeenShown = true;

		OkCancel okCancel = getBlockedMessageOkCancel();

		OkCancelDialog.UserActionOnChecked skipDialog =
				OkCancelDialog.isSkipDialog(
						NameValueDataDO.RT_WARNING_BLOCKED_MESSAGE_DONT_SHOW);

		if (skipDialog.equals(OkCancelDialog.UserActionOnChecked.DONT_SKIP)) {
			mOkCancelDialog = OkCancelDialog.showInstance(
					(AppCompatActivity) getActivity(),
					"The <b>Block SMS/MMS</b> setting is <b>experimental</b>"
							+ " and may not work on your phone.</b>",
					"Don't show this message again",
					"More Info",
					AutomatonAlert.OK_LABEL,
					OkCancelDialog.CancelButton.RIGHT,
					EWI.INFO
			);
			mOkCancelDialog.setOkCancel(okCancel);
		}
		else {
			okCancel.doUserActionOkOrCancel(skipDialog);
		}
	}

	private OkCancel getLongSongDurationOkCancel() {
		return new OkCancel() {
			// OK - do nothing
			@Override
			protected  void ok(DialogInterface dialog) {
				OkCancelDialog.saveOkCancelCheckBox(
						mOkCancelDialog,
						NameValueDataDO.RT_WARNING_LONG_SONG_DURATION_DONT_SHOW);
			}

			// CANCEL //
			@Override
			protected  void cancel(DialogInterface dialog) {
				//
			}
		};
	}

	private void showWarningLongSongDuration() {
		stopTestSound();

//		if (mHasWarningSongDurationBeenShown) {
//			return;
//		}
		mHasWarningSongDurationBeenShown = true;

		OkCancel okCancel = getLongSongDurationOkCancel();

		OkCancelDialog.UserActionOnChecked skipDialog =
				OkCancelDialog.isSkipDialog(
						NameValueDataDO.RT_WARNING_LONG_SONG_DURATION_DONT_SHOW);

		if (skipDialog.equals(OkCancelDialog.UserActionOnChecked.DONT_SKIP)) {
			mOkCancelDialog = OkCancelDialog.showInstance(
					(AppCompatActivity) getActivity(),
					"This ringtone will play for <b>"
							+ Utils.translateMillis(mSongDuration,
							false/*shortTrans*/)
							+ "</b> every time because it's set to loop, start to end. "
							+ "Change <b>Play For</b> to limit the ringtone time. "
							+ "<br><br><i>To silence long ringtones 1) use"
							+ " the widget, 2) go to Menu->Settings->"
							+ "Override Volume or 3) adjust the system alert volume.</i>",
					"Don't show this message again",
					"",
					AutomatonAlert.OK_LABEL,
					OkCancelDialog.CancelButton.LEFT,
					EWI.INFO
			);
			mOkCancelDialog.setOkCancel(okCancel);
		}
		else {
			okCancel.doUserActionOkOrCancel(skipDialog);
		}
	}

	private void stopTestSound() {
		if (mTestButton.getText().toString().equals(STOPIT)) {
			mTestButton.performClick();
		}
	}

	private boolean isNewNotificationItem() {
		return mNotificationItem == null
				|| mNotificationItem.getNotificationItemId() == -1;
	}

	@Override
	public View onCreateView(
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View returnView = null;

		// Inflate the layout for this fragment
		returnView = inflater.inflate(
				R.layout.rt_update_fragment, container, false);

		mTopView = returnView;
		setViewPointers(returnView);
		mLookupKey = getLookupKey();
		setInitialDbRecs();
		setDefaultTextAndNewViews();
		setInitialRingtoneUri();
		setViewComponents(returnView);
		return returnView;
	}

	private void setInitialDbRecs() {
		switch (mMode) {
			case DEFAULT_TEXT_RT:
				getRTDefaultTextNotificationItem();
				break;
			case DEFAULT_NEW_RT:
				getRTDefaultNewNotificationItem();
				break;
			case FREEFORM:
				getNotificationItemAndFilterItem(false/*getANewOne*/);
				break;
			default:
				getNotificationItemAndSourceType(false/*getANewOne*/);
				break;
		}
	}

	private void setDefaultTextAndNewViews() {
		switch (mMode) {
			case DEFAULT_TEXT_RT:
				break;
			case DEFAULT_NEW_RT:
				mInitValuesSpacer.setVisibility(View.VISIBLE);
				mRingtone.setVisibility(Button.GONE);
				mTestButton.setVisibility(Button.GONE);
				break;
			default:
				break;
		}
	}

	private void setInitialRingtoneUri() {
		String path = "";
		if (mNotificationItem != null) {
			if (mNotificationItem.getSoundPath() != null) {
				path = mNotificationItem.getSoundPath();
			}
		}
		mRingtoneUri = Uri.parse(path);
	}

	RTChooserFragment mRTChooserFragment;

	private void showRingtoneChooser() {
		stopTestSound();
		if (mRingtoneUri == null) mRingtoneUri = Uri.parse("");
		mRTChooserFragment = RTChooserFragment.showInstance(
				(AppCompatActivity) getActivity(),
				mFragmentType,
				mRingtoneUri);
	}

	private void checkForSongDurationWarning(int position) {
		if (mInitializing) {
			return;
		}
		int pos = (position != -1) ? position : mPlayForSpinner.getSelectedItemPosition();
		if (pos >= 0) {
			String stopAfter =
					getActivity().getResources().getStringArray(
							R.array.stop_sound_after_values)[pos];
			if (stopAfter.charAt(0) == '-') {
				// looping
				if (!mMode.equals(Mode.ALARM)) {
					if (mFragmentType.equals(FragmentTypeRT.TEXT)
							|| mFragmentType.equals(FragmentTypeRT.EMAIL)) {
						showWarningLongSongDuration();
					}
				}
			}
		}
	}

	private boolean isRingtoneChanged(String text, Uri uri) {
		String textNow = (mRingtone == null) ?
				"{}{}SomeTextThatIsNotTheSame<><>"
				: mRingtone.getText().toString();

		Uri uriNow = (mNotificationItem == null) ?
				Uri.parse(null) : Uri.parse(mNotificationItem.getSoundPath());

		return !(textNow.equals(text)
				&& uriNow.equals(uri));
	}

	private void setRingtoneText(String text, Uri uri) {
		// if it's "Default" and it's been through the
		// naming process, leave it alone
		if (text == null) {
			text = "";
		}
		if (uri == null) {
			uri = Uri.parse("");
		}
		if (text.toLowerCase().startsWith("default - ")) {
			mRingtone.setText(text);
		}
		else {
			Pair<Uri, String> uriAndSongName =
					getSpecificSongNameIfDefault(getActivity(), uri);
			mRingtoneUri = uriAndSongName.first;
			mRingtone.setText(uriAndSongName.second);
		}
	}

	public void setRingtone(String text, Uri uri, boolean skipRTEqualCheck) {
		// if this isn't the first time
		if (mRingtoneInitialized
				&& !skipRTEqualCheck) {
			// return immediately if nothing's changed
			if (!isRingtoneChanged(text, uri)) {
				return;
			}
		}

		if (uri == null
				|| TextUtils.isEmpty(uri.toString())) {
			mSongDuration = 0;
		}
		else {
			mSongDuration = Utils.getSongDuration(getActivity(), text, uri);
		}

		if (mRingtone != null) {
			setRingtoneText(text, uri);
			if (mMode.equals(Mode.ALARM)
					|| mMode.equals(Mode.FREEFORM)) {
				enableDisableRingtone(text);
			}
		}

		// Overlay not set for phone, so alpha-dim the Test! button
		// if there's no ringtone and clear button if no fields set
		if (mFragmentType.equals(FragmentTypeRT.PHONE)) {
			setTestAndClearButtonsEnabledDisabled(text);
		}

		if (mRingtoneInitialized) {
			// delete RT
			if (TextUtils.isEmpty(text)) {
				deleteRT();
				return;////////
			}

			// store data in NotificationItem, save phone info to Contacts.
			// save the pair SourceTypeDO, NotificationItemDO
			mRingtoneUri = (uri == null) ? Uri.parse("") : uri;

			if (mNotificationItem != null) {
				mNotificationItem.setSoundPath(mRingtoneUri.toString());
				mNotificationItem.setActive(true);
			}
			// if new, need to add registered account defaults from RTPrefsDO.
			// returns with false if AdVersion and too many active contacts.
			// if so, resets ringtone and ringtoneUri
			boolean unRestricted =
					saveDbRecs(isNewNotificationItem());
			if (!unRestricted) {
				// essentially clear/trash/delete ringtone
				mRingtone.setText("");
				mRingtoneUri = null;
			}
			else {
				if (mFragmentType.equals(FragmentTypeRT.PHONE)) {
					savePhone(mRingtoneUri);
				}
			}

			// DO ERROR CHECK? //
			boolean doErrorCheck = false;
			if (mMode.equals(Mode.FREEFORM)) {
				doErrorCheck = true;
			}
			else if (getActivity() instanceof RTUpdateActivity){
				if (((RTUpdateActivity)getActivity()).isThisFragmentShowingNow(mFragmentType)) {
					doErrorCheck = true;
				}
			}
			if (doErrorCheck) {
				Log.d(TAG + ".setRingtone()", "for [" + mFragmentType + "], checkErrors");
				checkForErrorsAndWarnings(true/*checkSongDuration*/);
			}
			else {
				Log.d(TAG + ".setRingtone()",
						"for [" + mFragmentType + "], not showing, not checkErrors");
			}
		}

		if (!mRingtoneInitialized) {
			mRingtoneInitialized = true;
		}

		// alpha-dim ringtone-required fields if there's no ringtone.
		// only sets non-phone.
		enableDisableNonPhoneFields();
		setTabIcon();
		showEmailAccounts();
	}

	private void setTestAndClearButtonsEnabledDisabled(String text) {
		boolean resetClear = false;
		if (TextUtils.isEmpty(text)) {
			mTestButton.setAlpha(.5f);
			mTestButton.setEnabled(false);
			String[] resOut = getResources().getStringArray(getSilentModeValues());
			int position = mSilentModeSpinner.getSelectedItemPosition();
			String val = (position < 0 || position > resOut.length) ?
					"" : resOut[position];
			if (val.equals("0")) {
				mClearButton.setAlpha(.5f);
				mClearButton.setEnabled(false);
			}
			else {
				resetClear = true;
			}
		}
		else {
			mTestButton.setAlpha(1f);
			mTestButton.setEnabled(true);
			resetClear = true;
		}
		if (resetClear) {
			mClearButton.setAlpha(1f);
			mClearButton.setEnabled(true);
		}
	}

	private void deleteRT() {
		if (mFragmentType.equals(FragmentTypeRT.PHONE)) {
			deletePhoneRT();
		}
		else {
			deleteAllRT();
		}
		setViewDefaults();
	}

	private void deletePhoneRT() {
		// PHONE ringtone and silent mode are independent
		// of this app (in contacts db)
		// don't delete everything if one is set
		if (mNotificationItem.getSilentMode().equals("1")) { // send to VM
			// just update ringtone
			Utils.updatePhoneRTVM(getActivity(), mLookupKey, "", null);
			mNotificationItem.setSoundPath("");
			saveDbRecs(false/*isANewAccount*/);
		}
		else {
			deleteAllRT();
		}
		setViewDefaults();
	}

	private void deleteAllRT() {
		// reset PHONE contact fields
		if (mFragmentType.equals(FragmentTypeRT.PHONE)) {
			Utils.updatePhoneRTVM(getActivity(), mLookupKey, "", "0");
		}
		switch (mMode) {
			case ALARM:
				// delete only NotificationItem
				mNotificationItem.delete();
				break;
			case FREEFORM:
				// delete NotificationItem, FilterItem, FilterItemAccount
				Utils.deleteFilterItemRT(mNotificationItem, mFilterItem);
				break;
			default:
				// deletes NotificationItem, SourceType, SourceAccount
				Utils.deleteRegularRT(mLookupKey, mFragmentType);
				break;
		}
		getNotificationItemAndSourceType(true/*getANewOne*/);
		setViewComponents(mTopView);
	}

	private void enableDisableRingtone(String text) {
		if (TextUtils.isEmpty(text)) {
			mRingtone.setEnabled(false);
			mRingtone.setAlpha(.3F);
			if (mRingtoneHeader != null) {
				mRingtoneHeader.setAlpha(.3F);
			}
		}
		else {
			mRingtone.setEnabled(true);
			mRingtone.setAlpha(1F);
			if (mRingtoneHeader != null) {
				mRingtoneHeader.setAlpha(1F);
			}
		}
	}

	private void enableDisableNonRingtoneFields(boolean enable) {
		mAccounts.setEnabled(enable);
		mSilentModeHeader.setEnabled(enable);
		mSilentModeSpinner.setEnabled(enable);
		mPlayForHeader.setEnabled(enable);
		mPlayForSpinner.setEnabled(enable);
		mVibrateModeHeader.setEnabled(enable);
		mVibrateModeSpinner.setEnabled(enable);
		mNotificationHeader.setEnabled(enable);
		mNotificationSpinner.setEnabled(enable);
		mLedModeHeader.setEnabled(enable);
		mLedModeSpinner.setEnabled(enable);
		mClearButton.setEnabled(enable);
		mVolDial.setEnabled(enable);
		mTestButton.setEnabled(enable);
		mAccountList.setEnabled(enable);

		// make trash invisible always in these cases
		if (mMode.equals(Mode.DEFAULT_TEXT_RT)
				|| mMode.equals(Mode.DEFAULT_NEW_RT)) {
			mClearButton.setVisibility(Button.INVISIBLE);
			mClearButton.setEnabled(false);
		}
	}

	private void setAlphaNonRingtoneFields(float f) {
		mAccounts.setAlpha(f);
		mSilentModeHeader.setAlpha(f);
		mSilentModeSpinner.setAlpha(f);
		mPlayForHeader.setAlpha(f);
		mPlayForSpinner.setAlpha(f);
		mVibrateModeHeader.setAlpha(f);
		mVibrateModeSpinner.setAlpha(f);
		mNotificationHeader.setAlpha(f);
		mNotificationSpinner.setAlpha(f);
		mLedModeHeader.setAlpha(f);
		mLedModeSpinner.setAlpha(f);
		mClearButton.setAlpha(f);
		mVolDial.setAlpha(f);
		mTestButton.setAlpha(f);
		mAccountList.setAlpha(f);
	}

	private boolean isTextMessageBlocked() {
		// TODO: blocked
//		if (mFragmentType.equals(FragmentTypeRT.TEXT)) {
//			if (mRingtone.getText().toString().toLowerCase()
//							.equalsIgnoreCase(AutomatonAlert.BLOCK_SMS_MMS_LABEL)) {
//				return true;
//			}
//		}
		return false;
	}

	void showHidePlayFor() {
		if (mMode.equals(Mode.ALARM)) {
			if (mNotificationItem.isNoAlertScreen()) {
				mPlayForHeader.setVisibility(TextView.VISIBLE);
				mPlayForSpinner.setVisibility(Spinner.VISIBLE);
			}
			else {
				mPlayForHeader.setVisibility(TextView.GONE);
				mPlayForSpinner.setVisibility(Spinner.GONE);
			}
		}
	}

	private void enableDisableNonPhoneFields() {
		if (!mFragmentType.equals(FragmentTypeRT.PHONE)) {
			if (TextUtils.isEmpty(mRingtone.getText().toString())
					|| isTextMessageBlocked()) {
				if (TextUtils.isEmpty(mRingtone.getText().toString())) {
					setAlphaNonRingtoneFields(.3f);
				}
				else {
					showInfoMessageBlocked();
				}
				enableDisableNonRingtoneFields(false);
			}
			else {
				setAlphaNonRingtoneFields(1f);
				enableDisableNonRingtoneFields(true);
				// special case: led/light
				boolean show = mNotificationItem.isShowInNotificationBar();
				dimLedModeSpinnerOnNoNotification(show);
			}
		}
	}

	private void setTabIcon() {
		// set alarm or default text
		if (mMode.equals(Mode.ALARM)
				|| mMode.equals(Mode.FREEFORM)
				|| mMode.equals(Mode.DEFAULT_TEXT_RT)
				|| mMode.equals(Mode.DEFAULT_NEW_RT)) {
			return;
		}

		// get our tab and put grey or color icon there
		Fragment fragment =
				RTUpdateActivity.mFragmentList.get(mFragmentType.ordinal());
		if (fragment == null) {
			return;
		}
		ActionBar ab = ((AppCompatActivity)fragment.getActivity()).getSupportActionBar();
		Tab tab = ab.getTabAt(mFragmentType.ordinal());

		if (mFragmentType.equals(FragmentTypeRT.TEXT)) {
			if (TextUtils.isEmpty(mRingtone.getText().toString())) {
				tab.setIcon(R.drawable.android_messages_grey_64);
			}
			else {
				tab.setIcon(R.drawable.android_messages_blue_64);
			}
		}
		else if (mFragmentType.equals(FragmentTypeRT.PHONE)) {
			if (TextUtils.isEmpty(mRingtone.getText().toString()) &&
					mSilentModeSpinner.getSelectedItemPosition() == 0) {
				tab.setIcon(R.drawable.android_phone_grey_64);
			}
			else {
				tab.setIcon(R.drawable.android_phone_blue_64);
			}
		}
		else if (mFragmentType.equals(FragmentTypeRT.EMAIL)) {
			if (TextUtils.isEmpty(mRingtone.getText().toString())) {
				tab.setIcon(R.drawable.android_email_blue_grey_64);
			}
			else {
				tab.setIcon(R.drawable.android_email_blue_blue_64);
			}
		}
	}

	private void setViewDefaults() {
		if (mRingtone != null) {
			// only populate with defaults if there's no data in NotificationItemDO
			if (mNotificationItem.getNotificationItemId() == -1) {
				mRingtone.setText(null);
				if (mSilentModeSpinner != null) {
					initializeNewRT();
					// adjust new volume to RTPrefsDO.mDefaultNewVolume
					int vol = RTPrefsDO.getDefaultNewVolume();
					mVolDial.setTag(vol - 1);
					setVolumeDrawable(mVolDial);
				}
			}
		}
		enableDisableNonPhoneFields();
		setTabIcon();
	}

	private void savePhone(Uri uri) {
		Utils.updatePhoneRTVM(
				getActivity().getApplicationContext(),
				mLookupKey,
				uri.toString(),
				null);
	}

	private void getSourceType() {
		if (!mMode.equals(Mode.ALARM)
				&& !mMode.equals(Mode.FREEFORM)) {
			// if there's a lookupKey, get SourceTypeDO
			if (!TextUtils.isEmpty(mLookupKey)) {
				mSourceType = SourceTypeDO.get(mLookupKey, mFragmentType.name());
			}
			// get a new SourceTypeDO
			if (mSourceType == null) {
				mSourceType = new SourceTypeDO(mLookupKey, mFragmentType.name());
			}
		}
	}

	private void getFilterItem() {
		// wrong call
		if (mMode != Mode.FREEFORM) {
			return;
		}

		mFilterItem = null;
		if (mFilterItemId != -1) {
			mFilterItem = FilterItems.get(mFilterItemId);
		}
		if (mFilterItem == null) {
			mFilterItem = new FilterItemDO();
		}
	}

	private void getNotificationItem(boolean getANewOne) {
		// if successful, get notificationItem
		switch (mMode) {
			case ALARM:
				if (mAlertItem.getNotificationItemId() != -1
						&& !getANewOne) {
					mNotificationItem =
							NotificationItems.get(mAlertItem.getNotificationItemId());
				}
				break;
			case FREEFORM:
				if (mFilterItem.getNotificationItemId() != -1
						&& !getANewOne) {
					mNotificationItem =
							NotificationItems.get(mFilterItem.getNotificationItemId());
				}
				break;
			default:
				if (mSourceType != null
						&& mSourceType.getNotificationItemId() != -1) {
					mNotificationItem =
							NotificationItems.get(mSourceType.getNotificationItemId());
				}
				break;
		}
		if (mNotificationItem == null
				|| getANewOne) {
			mNotificationItem = new NotificationItemDO();
			initializeNewRT();
		}
		// For PHONE, use data in Contacts to populate notificationItem
		if (mFragmentType.equals(FragmentTypeRT.PHONE)) {
			Pair<String, String> pair =
					Utils.getContactPhoneRTVM(getActivity(), mLookupKey);
			String path = pair.first;
			String mode = pair.second;
			mNotificationItem.setSoundPath((path == null ? "" : path));
			mNotificationItem.setSilentMode((mode == null ? "0" : mode));
			mNotificationItem.setNoAlertScreen(true);
		}
	}

	private void getNotificationItemAndSourceType(boolean getANewOne) {
		mNotificationItem = null;
		mSourceType = null;
		getSourceType();
		getNotificationItem(getANewOne);
	}

	private void getNotificationItemAndFilterItem(boolean getANewOne) {
		mNotificationItem = null;
		mFilterItem = null;
		getFilterItem();
		getNotificationItem(getANewOne);
	}

	private void setViewPointers(View v) {
		mRingtone = (TextView)v.findViewById(R.id.ru_ringtone);
		mRingtoneHeader = (TextView)v.findViewById(R.id.rt_separator_header);
		mInitValuesSpacer = v.findViewById(R.id.ru_init_values_spacer);
		mAccounts = (Button)v.findViewById(R.id.ru_accounts);
		mVolDial = (ImageButton)v.findViewById(R.id.ru_volume_dial);
		mSilentModeSpinner = (Spinner)v.findViewById(R.id.ru_spinner_silent_mode);
		mSilentModeHeader = (TextView)v.findViewById(R.id.ru_silent_mode_header);
		mPlayForSpinner = (Spinner)v.findViewById(R.id.ru_spinner_play_for);
		mPlayForHeader = (TextView)v.findViewById(R.id.ru_play_for_header);
		mVibrateModeSpinner = (Spinner)v.findViewById(R.id.ru_spinner_vibrate);
		mVibrateModeHeader = (TextView)v.findViewById(R.id.ru_vibrate_mode_header);
		mNotificationSpinner = (Spinner)v.findViewById(R.id.ru_spinner_notification);
		mNotificationHeader = (TextView)v.findViewById(R.id.ru_notification_header);
		mLedModeSpinner = (Spinner)v.findViewById(R.id.ru_spinner_led);
		mLedModeHeader = (TextView)v.findViewById(R.id.ru_led_mode_header);
		mClearButton = (ImageButton)v.findViewById(R.id.ru_clear);
		mTestButton = (Button)v.findViewById(R.id.rutest);
		mAccountList = (TextView)v.findViewById(R.id.ru_account_list);
	}

	private void setPhoneSilentModeHeaderAndView() {
		mSilentModeHeader.setText("Send Calls");
		if (mNotificationItem.getSilentMode().equals("0")) {
			mSilentModeHeader.setTextColor(Color.BLACK);
		}
		else {
			int color = getResources().getColor(R.color.native_dark_blue);
			mSilentModeHeader.setTextColor(color);
		}
	}

	private Spanned getNoAccountsErrorText() {
		if (mMaster != null) {
			return mMaster.getNoAccountsErrorText();
		}
		return Html.fromHtml(
				"<i>One or more email account needs to be chosen. Email"
						+ " accounts are scanned for email <b>from</b> this contact."
						+ " Click the Add/Modify Email Accounts button"
						+ " to add an account.</i>");
	}

	public void showEmailAccounts() {
		if (!mFragmentType.equals(FragmentTypeRT.EMAIL)) {
			return;
		}

		mAccounts.setTextColor(
				getResources().getColor(android.R.color.holo_blue_dark));

		ArrayList<AccountDO> accounts = null;
		Object[] list = null;

		if (mMode.equals(Mode.FREEFORM)) {
			accounts = Accounts.get();
			List<FilterItemAccountDO> items =
				FilterItemAccounts.getFilterItemId(mFilterItemId);
			list = items.toArray();
		}
		else {
			accounts = Accounts.getByAccountType(AccountEmailDO.ACCOUNT_EMAIL);
			List<SourceAccountDO> sources =
					SourceAccountDO.getSourceTypeId(mSourceType.getSourceTypeId());
			list = sources.toArray();
		}

		// no accounts?
		if (accounts.size() <= 0) {
			mAccounts.setTextColor(
					getResources().getColor(android.R.color.holo_red_dark));
			mAccountList.setText(getNoAccountsErrorText());
			return;/////////////////////
		}

		// none set up yet?
		if (list.length <= 0) {
			mAccountList.setText(getNoAccountsErrorText());
		}

		// show accounts
		else {
			String s = "";
			for (Object acct : list) {
				AccountDO account = Accounts.get(getAccountId(acct));
				if (account != null) {
					s += account.getName() + "<br>";
				}
			}
			mAccountList.setText(Html.fromHtml(s));
		}
	}

	private int getAccountId(Object acct) {
		if (acct instanceof SourceAccountDO) {
			return ((SourceAccountDO)acct).getAccountId();
		}
		else if (acct instanceof FilterItemAccountDO) {
			return ((FilterItemAccountDO)acct).getAccountId();
		}

		return -1;
	}

	void setRingtoneFromNotificationItem(boolean createDefaultOnEmpty) {
		// guard against null
		if (mNotificationItem.getSoundPath() == null) {
			mNotificationItem.setSoundPath("");
		}
		// get song uri
		Uri uri = Uri.parse(mNotificationItem.getSoundPath());
		// if uri is empty, make it "Default"
		if (createDefaultOnEmpty
				&& TextUtils.isEmpty(mNotificationItem.getSoundPath())) {
			uri = Uri.parse(AutomatonAlert.DEFAULT_LABEL);
		}
		// get uri and songName for "Default" or other uri
		Pair<Uri, String> uriAndSongName =
				getSpecificSongNameIfDefault(getActivity(), uri);
		mRingtoneUri = uriAndSongName.first;
		// setRingtone()
		uri = uriAndSongName.first;
		String songName = uriAndSongName.second;
		setRingtone(songName, uri, false);
	}

	private static boolean isDefaultSongName(String nameIn) {
		String songName = nameIn.toLowerCase();
		if (songName.equals(AutomatonAlert.DEFAULT)
				|| songName.equals(VolumeTypes.ringtone.name())
				|| songName.equals(VolumeTypes.alarm.name())
				|| songName.equals(VolumeTypes.notification.name())) {
			return true;
		}

		return false;
	}

	public static Pair<Uri, String> getSpecificSongNameIfDefault(
			Context context, Uri uri) {

		String songName = uri.toString();

		if (isDefaultSongName(songName)) {
			Pair<Uri, String> uriAndTitle =
					getDefaultRingtoneUriAndTitle(context, songName);
			uri = uriAndTitle.first;
			songName = AutomatonAlert.DEFAULT_LABEL + " - " + uriAndTitle.second;
		}
		else {
			songName = Utils.getSongName(uri);
			if (uri.toString().equals(RTPrefsDO.getDefaultRingtone())) {
				songName = AutomatonAlert.DEFAULT_LABEL + " - " + songName;
			}
		}

		return new Pair<Uri, String>(uri, songName);
	}

	private void setViewComponents(View v) {
		mInitializing = true;
		setViewDefaults();

		if (mFragmentType.equals(FragmentTypeRT.PHONE)) {
			setNonPhoneComponentsInvisible(v);
			setPhoneSilentModeHeaderAndView();
		}
		else if (mFragmentType.equals(FragmentTypeRT.EMAIL)) {
			mAccounts.setVisibility(Button.VISIBLE);
		}
		else {
			mAccounts.setVisibility(Button.GONE);
			LayoutParams lp = (LayoutParams)mRingtone.getLayoutParams();
			lp.weight = -1;
			mRingtone.setLayoutParams(lp);
		}

		// set spinners before ringtone because
		// setRingtoneFrom... does setRingtone which
		// checks for errors
		setVolDial();
		setSpinnersAndButtons();

		// Ringtone
		setRingtoneHint();
		setRingtoneFromNotificationItem(false/*createDefaultOnEmpty*/);

		// ringtone
		mRingtone.setOnClickListener(new RingtoneListener());
		// accounts
		mAccounts.setOnClickListener(new AccountsButtonListener());

		mInitializing = false;
	}

	private void setRingtoneHint() {
		// tolower
		String type = "";
		switch (mMode) {
			case ALARM:
				type = Mode.ALARM.name().toLowerCase();
				break;
			case FREEFORM:
				type = Mode.FREEFORM.name().toLowerCase();
				break;
			default:
				type = mFragmentType.name().toLowerCase();
				break;
		}
		// init upper
		type =
				type.substring(0, 1).toUpperCase(Locale.getDefault())
				+ type.substring(1);

		mRingtone.setHint("Choose " + type + " Ringtone");
	}

	private void setVolDial() {
		// volume
		mVolDial.setTag(mNotificationItem.getVolumeLevel() - 1);
		VolumeDialOnClickListener vList = new VolumeDialOnClickListener();
		setVolumeDrawable(mVolDial);
		mVolDial.setOnClickListener(vList);
		mVolDial.setOnLongClickListener(new VolumeDialOnLongClickListener());
	}

	private void setSpinnersAndButtons() {
		setSilentModeSpinner();
		setPlayForSpinner();
		setVibrateModeSpinner();
		setNotificationSpinner();
		setLedModeSpinner();
		setClearButton();
		setTestButton();
	}

	class RingtoneListener implements TextView.OnClickListener {
		@Override
		public void onClick(View v) {
			showRingtoneChooser();
		}
	}

	private EmailAccountDialog processEmailAccounts() {
		Pair<String[], String[]> accountsPair = null;
		int accountType = -1;
		if (mMode.equals(Mode.FREEFORM)) {
			accountType = AccountDO.ACCOUNT_GENERIC;
		}
		else {
			accountType = AccountEmailDO.ACCOUNT_EMAIL;
		}
		accountsPair = Accounts.getAllAccountsNameKeyArrays(accountType);

		String[] nameList = accountsPair.first;
		String[] keyList = accountsPair.second;

		return new EmailAccountDialog(
				this, nameList, keyList, mMode, mSourceType, mFilterItem, mDisplayName);
	}

	private OkCancel getNoAccountsOkCancel() {
		return new OkCancel() {
			// CLEAR //
			@Override
			protected  void ok(DialogInterface dialog) {
				Intent intent = new Intent(
						getActivity().getApplicationContext(),
						AccountAddUpdateActivity.class);
				intent.putExtra(AutomatonAlert.M_MODE, AutomatonAlert.ADD);
				getActivity().startActivity(intent);

				OkCancelDialog.saveOkCancelCheckBox(
						mOkCancelDialog,
						NameValueDataDO.RT_ERROR_NO_ACCOUNTS_SET_UP_DONT_SHOW);
			}

			// CANCEL //
			@Override
			protected  void cancel(DialogInterface dialog) {
				//
			}
		};
	}

	private void showNoAccountsOkCancel() {
		stopTestSound();

		if (mHasErrorNoEmailAccountsBeenShown) {
			return;
		}
		mHasErrorNoEmailAccountsBeenShown = true;

		OkCancel okCancel = getNoAccountsOkCancel();

		OkCancelDialog.UserActionOnChecked skipDialog =
				OkCancelDialog.isSkipDialog(
						NameValueDataDO.RT_ERROR_NO_ACCOUNTS_SET_UP_DONT_SHOW);

		if (skipDialog.equals(OkCancelDialog.UserActionOnChecked.DONT_SKIP)) {
			mOkCancelDialog = OkCancelDialog.showInstance(
					(AppCompatActivity) getActivity(),
					"<b>One or more email account needs to be registered.</b><br><br>"
							+ "<b>Tip:</b> Each account will be scanned at intervals"
							+ " for email <b>from</b> this contact.",
					"Don't show this message again",
					AutomatonAlert.CANCEL_LABEL,
					"Add Account",
					OkCancelDialog.CancelButton.LEFT,
					EWI.WARNING
			);
			mOkCancelDialog.setOkCancel(okCancel);
		}
		else {
			okCancel.doUserActionOkOrCancel(skipDialog);
		}
	}

	private boolean showErrorNoEmailAccounts(EmailAccountDialog emailDialog) {
		// if there are no accounts and user hasn't
		// requested to not see the error, show it
		// and leave
		if (emailDialog.mAllAccountKeys == null
				|| emailDialog.mAllAccountKeys.length == 0) {
			NameValueDataDO nv =
					NameValueDataDO.get(
							NameValueDataDO.RT_ERROR_NO_ACCOUNTS_SET_UP_DONT_SHOW,
							null);
			if (nv == null
					|| nv.getValue().equals("0")) {
				showNoAccountsOkCancel();
				return true;
			}
		}
		return false;
	}

	private void showEmailAccountDialog(EmailAccountDialog emailDialog) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("emailDialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		emailDialog.show(ft, "emailDialog");
	}

	class AccountsButtonListener implements Button.OnClickListener {
		@Override
		public void onClick(View v) {
			if (TextUtils.isEmpty(mRingtone.getText().toString())) {
				Toast toast = Toast.makeText(
						getActivity(),
						"Please select a ringtone before specifying accounts",
						Toast.LENGTH_SHORT);
				toast.show();
				return;
			}
			EmailAccountDialog emailDialog = processEmailAccounts();
//			if (showErrorNoEmailAccounts(emailDialog)) {
//				return;
//			}
			showEmailAccountDialog(emailDialog);
		}
	}

	public void setTestButtonText(String text) {
		mTestButton.setText(text);
	}

	private void setTestButton() {
		mTestButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String sText = mTestButton.getText().toString();

				// STOP
				if (sText.equals(STOPIT)) {
					setTestButtonText(TESTIT);
					final Intent intent = new Intent(
							getActivity().getApplicationContext(),
							AutomatonAlertService.class);
					intent.putExtra(AutomatonAlert.ACTION,
							AutomatonAlert.ALERT_TURN_OFF_ALL_NOTIFICATION_ITEM_SOUNDS);
					getActivity().startService(intent);
				}
				// TEST_LABEL
				else {
					Intent intent = setTestIntentForAlarm();
					intent.putExtra(Contacts.DISPLAY_NAME, mDisplayName);
					intent.setFlags(0);
					getActivity().sendBroadcast(intent);

					// if volume isn't silent, play ringtone
					// and set button to "Stop"
					if (!warnedIfSilentOrPaused()) {
						setTestButtonText(STOPIT);
					}
				}
			}
		});
	}

	private boolean warnedIfSilentOrPaused() {
		String toastText = "";
		if (GeneralPrefsDO.isPauseAlertsAlarms()) {
			toastText = "Alerts/Alarms are paused; Un-pause"
					+ " in Widget or Settings to test.";
		}
		else if (GeneralPrefsDO.getOverrideVol().equals(
				OverrideVolLevel.SILENT.name())) {

			toastText = "Alerts/Alarms are silent; Change volume"
					+ " in Widget or Settings to test sound.";
		}
		if (!(toastText.equals(""))) {
			Toast.makeText(
					getActivity(), toastText, Toast.LENGTH_LONG).show();
			return true;
		}
		else {
			return false;
		}
	}

	private Intent setTestIntentForAlarm() {
		int alertItemId = -1;
		if (mAlertItem != null) {
			alertItemId = mAlertItem.getAlertItemId();
		}
		Intent intent =
				AlertItemDO.setAlarmAlertIntent(
						alertItemId,
						mNotificationItem.getNotificationItemId(),
						mFragmentType,
						-1L, -1L, -1L);
		// our unique stuff
		intent.putExtra(
				RTUpdateActivity.TAG_FRAGMENT_TYPE, mFragmentType.name());
		// it's only a test
		intent.setData(Uri.parse(
				AlarmVisualActivity.TEST_ALARM
				+ "|"
				+ AlarmVisualActivity.TEST_ALARM));

		return intent;
	}

	String setSpinnerSelection(Spinner spinner, String value, int sRArray) {
		String[] array = getResources().getStringArray(sRArray);
		return setSpinnerSelection(spinner, value, array);
	}
	private String setSpinnerSelection(Spinner spinner, String value, String[] array) {
		if (value == null) {
			value = array[0];
		}
		int idx = Utils.getIndexOfEntry(value, array);
		if (idx >= 0) {
			spinner.setSelection(idx);
		}
		else {
			spinner.setSelection(0);
			value = array[0];
		}
		return value;
	}

	private int getSilentModeEntries() {
		if (mFragmentType.equals(FragmentTypeRT.PHONE)) {
			return R.array.ringtone_silent_mode_phone;
		}
		else {
			return R.array.ringtone_silent_mode_other;
		}
	}

	private int getSilentModeValues() {
		if (mFragmentType.equals(FragmentTypeRT.PHONE)) {
			return R.array.ringtone_silent_mode_phone_values;
		}
		else {
			return R.array.ringtone_silent_mode_other_values;
		}
	}

	// this one is different in that the spinner entries/values
	// depend on whether this fragment is PHONE or other
	private void setSilentModeSpinner() {
		int resIn = getSilentModeEntries();
		int resOut = getSilentModeValues();

		// set adapter
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				getActivity(),
				resIn,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSilentModeSpinner.setAdapter(adapter);

		mInitializingSilentModeSpinner = true;
		setSpinnerSelection(
				mSilentModeSpinner,
				mNotificationItem.getSilentMode(),
				resOut);

		mSilentModeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				// alpha-dim clearButton if needed
				if (mFragmentType.equals(FragmentTypeRT.PHONE)) {
					setTestAndClearButtonsEnabledDisabled(
							mRingtone.getText().toString());
				}
				Resources res = getResources();
				int resOut = getSilentModeValues();
				String val = res.getStringArray(resOut)[position];
				mNotificationItem.setSilentMode(val);
				if (!mInitializingSilentModeSpinner) {
					if (mFragmentType.equals(FragmentTypeRT.PHONE)) {
						Utils.updatePhoneRTVM(getActivity(), mLookupKey, null, val);
					}
					saveDbRecs(false/*isANewAccount*/);
				}
				mInitializingSilentModeSpinner = false;
				if (mFragmentType.equals(FragmentTypeRT.PHONE)) {
					setPhoneSilentModeHeaderAndView();
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}

	private void setPlayForSpinner() {
		mInitializingPlayForSpinner = true;
		setSpinnerSelection(
				mPlayForSpinner,
				"" + mNotificationItem.getPlayFor(),
				R.array.stop_sound_after_values);

		mPlayForSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(
					AdapterView<?> parent, View view, int position, long id) {
				if (!mInitializingPlayForSpinner) {
					String sound = getResources().getStringArray(
							R.array.stop_sound_after_values)[position];
					playForSongDurationWarningCheck(sound, position);
					mNotificationItem.setPlayFor(sound);
					saveDbRecs(false/*isANewAccount*/);
				}
				mInitializingPlayForSpinner = false;
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}

	private long playForSongDurationWarningCheck(String playFor, int position) {
		long l = Utils.getLong(playFor, 2000);
		if (l < 0
				&& mNotificationItem.getPlayFor() >= 0) {
			if (!mFragmentType.equals(FragmentTypeRT.PHONE)
					&& mSongDuration > LONGEST_RT_BEFORE_WARNING) {
				checkForSongDurationWarning(position);
			}
		}

		return l;
	}

	private void setVibrateModeSpinner() {
		mInitializingVibrateModeSpinner = true;
		setSpinnerSelection(
				mVibrateModeSpinner,
				"" + mNotificationItem.getVibrateMode(),
				R.array.ringtone_vibrate_mode_values);

		mVibrateModeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				String s = getResources().getStringArray(
						R.array.ringtone_vibrate_mode_values)[position];
				mNotificationItem.setVibrateMode(s);
				if (!mInitializingVibrateModeSpinner) {
					saveDbRecs(false/*isANewAccount*/);
				}
				mInitializingVibrateModeSpinner = false;
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}

	private void setNotificationSpinner() {
		mInitializingNotificationSpinner = true;
		dimLedModeSpinnerOnNoNotification(mNotificationItem.isShowInNotificationBar());
		setSpinnerSelection(
				mNotificationSpinner,
				mNotificationItem.isShowInNotificationBar() ? "1" : "0",
				R.array.ringtone_notification_values);
		mNotificationSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				String value = getResources().getStringArray(
						R.array.ringtone_notification_values)[position];
				boolean booleanValue = value.equals("1");
				mNotificationItem.setShowInNotificationBar(booleanValue);
				if (!mInitializingNotificationSpinner) {
					saveDbRecs(false/*isANewAccount*/);
				}
				mInitializingNotificationSpinner = false;
				dimLedModeSpinnerOnNoNotification(booleanValue);
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}

	private void dimLedModeSpinnerOnNoNotification(boolean val) {
		// if we're already dimmed because of no ringtone, don't double-dim
		if (TextUtils.isEmpty(mRingtone.getText().toString())) {
			return;//////
		}
		// if dimmed because on "Block SMS/MMS"...
		if (isTextMessageBlocked()) {
			return;//////
		}
		if (!val) {
			mLedModeHeader.setAlpha(.5f);
			mLedModeSpinner.setEnabled(false);
		}
		else {
			mLedModeHeader.setAlpha(1f);
			mLedModeSpinner.setEnabled(true);
		}
	}

	private void setLedModeSpinner() {
		mInitializingLedModeSpinner = true;
		setSpinnerSelection(
				mLedModeSpinner,
				mNotificationItem.getLedMode(),
				R.array.ringtone_led_values);

		mLedModeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				String s = getResources().getStringArray(
						R.array.ringtone_led_values)[position];
				mNotificationItem.setLedMode(s);
				if (!mInitializingLedModeSpinner) {
					saveDbRecs(false/*isANewAccount*/);
				}
				mInitializingLedModeSpinner = false;
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}

	private String getDeleteWarningValue() {
		switch(mMode) {
			case FREEFORM:
				return NameValueDataDO.RT_WARNING_ON_FREEFORM_DELETE_DONT_SHOW;
			case ALARM:
				return NameValueDataDO.RT_WARNING_ON_ALARM_DELETE_DONT_SHOW;
			default:
				return NameValueDataDO.RT_WARNING_ON_DELETE_DONT_SHOW;
		}
	}

	private OkCancel getClearOkCancel(final String warningValue) {
		return new OkCancel() {
			// CLEAR //
			@Override
			protected  void ok(DialogInterface dialog) {
				switch (mMode) {
					case ALARM:
						mMaster.clearMaster(
								DatePickerTimePicker.CLEAR_DIALOG_ID, true/*deleteRT*/);
						break;
					case FREEFORM:
						mMaster.clearMaster();
						clearRT();
						break;
					default:
						clearRT();
						break;
				}
				OkCancelDialog.saveOkCancelCheckBox(
						mOkCancelDialog,
						warningValue);

				if (mMode.equals(Mode.FREEFORM)) {
					getActivity().finish();
					getActivity().overridePendingTransition(0, 0);
				}
			}

			// CANCEL CLEAR //
			@Override
			protected  void cancel(DialogInterface dialog) {
				//
			}
		};
	}

	private void showClearOkCancel() {
		final String warningValue = getDeleteWarningValue();

		stopTestSound();

		OkCancel okCancel = getClearOkCancel(warningValue);

		OkCancelDialog.UserActionOnChecked skipDialog =
				OkCancelDialog.isSkipDialog(warningValue);

		if (skipDialog.equals(OkCancelDialog.UserActionOnChecked.DONT_SKIP)) {
			String kind = "alarm";
			switch(mMode) {
				case ALARM:
					kind = VolumeTypes.alarm.name();
					break;
				case FREEFORM:
					kind = "free-form alert";
					break;
				default:
					kind = VolumeTypes.ringtone.name();
					break;
			}
			mOkCancelDialog = OkCancelDialog.showInstance(
					(AppCompatActivity) getActivity(),
					"Do you want to delete this " + kind + "?",
					"Don't show this message again",
					AutomatonAlert.CANCEL_LABEL,
					AutomatonAlert.OK_LABEL,
					OkCancelDialog.CancelButton.LEFT,
					EWI.WARNING
			);
			mOkCancelDialog.setOkCancel(okCancel);
		}
		else {
			okCancel.doUserActionOkOrCancel(skipDialog);
		}
	}

	public void setClearButton() {
		mClearButton.setOnClickListener(new ImageView.OnClickListener() {
			@Override
			public void onClick(View v) {
				showClearOkCancel();
			}
		});
	}

	private class VolumeDialOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (setVolumeDrawable(v) != 0) {
				saveDbRecs(false/*isANewAccount*/);
			}
		}
	}

	private int setVolumeDrawable(View v) {

		int newR = 0;
		int idx = -1;
		Object o = v.getTag();
		if (o == null) {
			o = RTPrefsDO.UI_VOLUME_DEFAULT_INDEX;
			v.setTag(o);
		}
		if (o instanceof Integer) {//.getClass().equals(Integer.class)) {
			idx = (Integer)o;
			if (idx > RTPrefsDO.UI_VOLUME_DEFAULT_INDEX) {
				idx = RTPrefsDO.UI_VOLUME_DEFAULT_INDEX;
			}
			switch (idx) {
			case 0:
				newR = R.drawable.volume_circle_1_of_7_with_text;
				break;
			case 1:
				newR = R.drawable.volume_circle_2_of_7_with_text;
				break;
			case 2:
				newR = R.drawable.volume_circle_3_of_7_with_text;
				break;
			case 3:
				newR = R.drawable.volume_circle_4_of_7_with_text;
				break;
			case 4:
				newR = R.drawable.volume_circle_5_of_7_with_text;
				break;
			case 5:
				newR = R.drawable.volume_circle_6_of_7_with_text;
				break;
			case 6:
				newR = R.drawable.volume_circle_7_of_7_with_text;
				break;
			case 7:
				newR = R.drawable.volume_circle_default_with_text;
				break;
			default:
				newR = R.drawable.volume_circle_0_of_7_with_text;
				break;
			}
		}
		if (newR != 0) {
			int level = ++idx%(RTPrefsDO.UI_VOLUME_DEFAULT_INDEX +1);
			v.setTag(level);
			v.setBackgroundResource(newR);
			mNotificationItem.setVolumeLevel(level);
		}
		return newR;
	}

	private class VolumeDialOnLongClickListener implements View.OnLongClickListener {
		@Override
		public boolean onLongClick(View v) {
			v.setTag(RTPrefsDO.UI_VOLUME_DEFAULT_INDEX);
			v.setBackgroundResource(R.drawable.volume_circle_default_with_text);
			mNotificationItem.setVolumeLevel(RTPrefsDO.UI_VOLUME_DEFAULT_INDEX);
			saveDbRecs(false/*isANewAccount*/);
			return true;
		}
	}

	private void setNonPhoneComponentsInvisible(View v) {
		mLedModeSpinner.setVisibility(Spinner.GONE);
		mNotificationSpinner.setVisibility(Spinner.GONE);
		mPlayForSpinner.setVisibility(Spinner.GONE);
		mVibrateModeSpinner.setVisibility(Spinner.GONE);
		mVolDial.setVisibility(Spinner.GONE);
		v.findViewById(R.id.ru_led_mode_header).setVisibility(TextView.GONE);
		v.findViewById(R.id.ru_notification_header).setVisibility(TextView.GONE);
		v.findViewById(R.id.ru_play_for_header).setVisibility(TextView.GONE);
		v.findViewById(R.id.ru_vibrate_mode_header).setVisibility(TextView.GONE);
		mAccounts.setVisibility(Button.GONE);

		// need to anchor account list below the silentMode spinner
		RelativeLayout rl = ((RelativeLayout)v.findViewById(R.id.ru_bottom));
		RelativeLayout.LayoutParams lp =
				(RelativeLayout.LayoutParams)rl.getLayoutParams();
		lp.addRule(RelativeLayout.BELOW, R.id.ru_spinner_silent_mode);
		rl.setLayoutParams(lp);
	}

	private void addDefaultSourceTypeAccounts() {
		ArrayList<NameValueDataDO> specPrefs =
				NameValueDataDO.get(
				RTPrefsDO.getDefaultLinkedAccountSpecificPrefix(),
				true/*startsWith*/,
				null/*pref value*/);
		SourceAccountDO.addAll(specPrefs, mSourceType.getSourceTypeId());
	}

	private static Pair<Uri, String>getDefaultRingtoneUriAndTitle(
			Context context, String rt) {

		Uri uri = Uri.parse(AutomatonAlert.DEFAULT_LABEL);
		String title = AutomatonAlert.DEFAULT_LABEL;

		// "Default" to "alarm", "ringtone", "notification"
		if (rt.toLowerCase().equals(AutomatonAlert.DEFAULT)) {
			rt = RTPrefsDO.getDefaultRingtone();
		}

		boolean isDefault = false;
		for (VolumeTypes def : VolumeTypes.values()) {
			if (rt.equals(def.name())) {
				isDefault = true;
				break;
			}
		}
		if (isDefault) {
			// get URI for default ringtone
			String[] uriAndTitle =
					Utils.getDefaultRingtoneUriAndTitle(
							context,
							rt,
							true/*trimTitleOfDefault*/,
							true/*justTheSongName*/);

			// found
			if (uriAndTitle.length == 2) {
				uri = Uri.parse(uriAndTitle[0]);
				title = uriAndTitle[1];
			}
			// not found
			else {
				uri = Uri.parse(rt);
				title = rt;
			}
		}
		// not default
		else {
			uri = Uri.parse(RTPrefsDO.getDefaultRingtone());
			title = Utils.getSongName(uri);
		}

		return new Pair<Uri, String>(uri, title);
	}

	private void getRTDefaultTextNotificationItem() {
		mNotificationItem = new NotificationItemDO();

		mNotificationItem.setSoundPath(RTPrefsDO.getDefaultTextRingtone());
		mNotificationItem.setPlayFor(RTPrefsDO.getDefaultTextPlayFor());
		mNotificationItem.setVibrateMode(RTPrefsDO.getDefaultTextVibrateMode());
		mNotificationItem.setSilentMode(RTPrefsDO.getDefaultTextSilentMode());
		mNotificationItem.setLedMode(RTPrefsDO.getDefaultTextLight());
		mNotificationItem.setVolumeLevel(RTPrefsDO.getDefaultTextVolume());
		mNotificationItem.setShowInNotificationBar(RTPrefsDO.isDefaultTextNotification());
		mNotificationItem.setNoAlertScreen(true);
		mNotificationItem.save();

		mVolDial.setTag(mNotificationItem.getVolumeLevel() - 1);
		setVolumeDrawable(mVolDial);

	}

	private void getRTDefaultNewNotificationItem() {
		mNotificationItem = new NotificationItemDO();

		mNotificationItem.setSoundPath(AutomatonAlert.DEFAULT_LABEL);
		mNotificationItem.setPlayFor(RTPrefsDO.getDefaultNewPlayFor());
		mNotificationItem.setVibrateMode(RTPrefsDO.getDefaultNewVibrateMode());
		mNotificationItem.setSilentMode(RTPrefsDO.getDefaultNewSilentMode());
		mNotificationItem.setLedMode(RTPrefsDO.getDefaultNewLight());
		mNotificationItem.setVolumeLevel(RTPrefsDO.getDefaultNewVolume());
		mNotificationItem.setShowInNotificationBar(RTPrefsDO.isDefaultNewNotification());
		mNotificationItem.setNoAlertScreen(true);
		mNotificationItem.save();

		mVolDial.setTag(mNotificationItem.getVolumeLevel() - 1);
		setVolumeDrawable(mVolDial);

	}

	private void saveRTDefaultTextNotificationItem() {
		RTPrefsDO.setDefaultTextRingtone(mNotificationItem.getSoundPath());
		RTPrefsDO.setDefaultTextPlayFor((int) mNotificationItem.getPlayFor());
		RTPrefsDO.setDefaultTextVibrateMode(mNotificationItem.getVibrateMode());
		RTPrefsDO.setDefaultTextSilentMode(mNotificationItem.getSilentMode());
		RTPrefsDO.setDefaultTextLight(mNotificationItem.getLedMode());
		RTPrefsDO.setDefaultTextVolume(mNotificationItem.getVolumeLevel());
		RTPrefsDO.setDefaultTextNotification(mNotificationItem.isShowInNotificationBar());
		RTPrefsDO.save();
	}

	private void saveRTDefaultNewNotificationItem() {
		RTPrefsDO.setDefaultNewPlayFor((int) mNotificationItem.getPlayFor());
		RTPrefsDO.setDefaultNewVibrateMode(mNotificationItem.getVibrateMode());
		RTPrefsDO.setDefaultNewSilentMode(mNotificationItem.getSilentMode());
		RTPrefsDO.setDefaultNewLight(mNotificationItem.getLedMode());
		RTPrefsDO.setDefaultNewVolume(mNotificationItem.getVolumeLevel());
		RTPrefsDO.setDefaultNewNotification(mNotificationItem.isShowInNotificationBar());
		RTPrefsDO.save();
	}

	private boolean saveDbRecs(boolean isANewAccount) {
		if (isANewAccount
				&& !mMode.equals(Mode.FREEFORM)
				&& !mMode.equals(Mode.ALARM)
				&& Utils.inAppUpgradeIsRTContactsAtLimit(getActivity(), mLookupKey)) {
			return false;
		}

		if (mMode.equals(Mode.DEFAULT_TEXT_RT)) {
			saveRTDefaultTextNotificationItem();
			mNotificationItem.save();
			return true;
		}
		if (mMode.equals(Mode.DEFAULT_NEW_RT)) {
			saveRTDefaultNewNotificationItem();
			mNotificationItem.save();
			return true;
		}

		// no alert screen (really just pertinent when
		// hitting the Test! button)
		if (mFragmentType.equals(FragmentTypeRT.PHONE)) {
			mNotificationItem.setNoAlertScreen(true);
		}

		mNotificationItem.save();

		// need an alert item for ALARM type
		switch (mMode) {

			case ALARM:

			case FREEFORM:
				mMaster.saveMasterNotificationItemId(
						mNotificationItem.getNotificationItemId());
				mMaster.addDefaultAccounts();
				break;

			default:
				boolean saveSourceType = false;
				if (mSourceType == null) {
					mSourceType = new SourceTypeDO(mLookupKey, mFragmentType.name());
					saveSourceType = true;
				}
				if (mSourceType.getNotificationItemId() !=
						mNotificationItem.getNotificationItemId()) {
					mSourceType.setNotificationItemId(mNotificationItem.getNotificationItemId());
					saveSourceType = true;
				}
				if (saveSourceType) {
					mSourceType.save();
				}
				// if EMAIL, do setup after above are saved
				if (mSourceType.getSourceType().equals(FragmentTypeRT.EMAIL.name())
						&& isANewAccount) {
					addDefaultSourceTypeAccounts();
				}
				break;
		}

		if (mIntent == null) {
			mIntent = new Intent(getActivity().getIntent());
		}
		mIntent.setData(Uri.parse(TAG_DATA_SAVED));
		getActivity().setResult(Activity.RESULT_OK, mIntent);

		return true;
	}

	public String getLookupKey() {
		if (mActivityListener != null) {
			return mActivityListener.getLookupKeyCallback();
		}
		return null;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mActivityListener = (IRTUpdateFragmentListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement IRTUpdateFragmentListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mActivityListener = null;
		// the following 2 types are TEXT, so check for them
		// first so that we don't null TEXT instead of these
		if (((AutomatonAlert.THIS.mRTDefaultTextFragment == this))) {
			AutomatonAlert.THIS.mRTDefaultTextFragment = null;
		}
		else if (((AutomatonAlert.THIS.mRTDefaultNewFragment == this))) {
			AutomatonAlert.THIS.mRTDefaultNewFragment = null;
		}
		else if (((AutomatonAlert.THIS.mRTFreeFormFragment == this))) {
			AutomatonAlert.THIS.mRTFreeFormFragment = null;
		}
		else if (RTUpdateActivity.mFragmentList.get(mFragmentType.ordinal()) != null) {
			RTUpdateActivity.mFragmentList.put(mFragmentType.ordinal(), null);
		}
	}

	private void initializeNewRT() {
		// silent mode
		if (mFragmentType.equals(FragmentTypeRT.PHONE)) {
			mNotificationItem.setSilentMode("0");	// sendToVM: false
		}
		else {
			mNotificationItem.setSilentMode(RTPrefsDO.getDefaultNewSilentMode());
		}
		mNotificationItem.setPlayFor(RTPrefsDO.getDefaultNewPlayFor());
		mNotificationItem.setVibrateMode(RTPrefsDO.getDefaultNewVibrateMode());
		mNotificationItem.setShowInNotificationBar(RTPrefsDO.isDefaultNewNotification());
		mNotificationItem.setLedMode(RTPrefsDO.getDefaultNewLight());
		mNotificationItem.setVolumeLevel(RTPrefsDO.getDefaultNewVolume());
		if (!mMode.equals(Mode.ALARM)) {
			mNotificationItem.setNoAlertScreen(true);
		}

		mNotificationItem.setActive(false);
	}

	public interface IRTUpdateFragmentListener {
		String getLookupKeyCallback();
	}

	void clearRT() {
		// clear out view values
		mSilentModeSpinner.setSelection(0);
		// true = skipInitCheck so we don't get
		// ignored if there's no ringtone already
		setRingtone("", Uri.parse(""), true);

		if (getActivity() instanceof SetAlertActivity) {
			((SetAlertActivity)getActivity()).setAlertItem(null);
		}

		initializeNewRT();
	}

	// called only for SetAlarm...
	void saveNotificationItem() {
		if (mNotificationItem != null
				&& mNotificationItem.isDirty) {
			mNotificationItem.save();
		}
	}
}
