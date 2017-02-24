package com.aldersonet.automatonalert.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Filter.FilterItemAccounts;
import com.aldersonet.automatonalert.Filter.FilterItemDO;
import com.aldersonet.automatonalert.Filter.FilterItems;
import com.aldersonet.automatonalert.Preferences.NameValueDataDO;
import com.aldersonet.automatonalert.Preferences.RTPrefsDO;
import com.aldersonet.automatonalert.R;

import java.util.ArrayList;

@SuppressLint("ValidFragment")
public class FreeFormMasterFragment extends Fragment
		implements
		IRTMaster {

	public static final String TAG_FILTER_ITEM_ID = "filterItemId";

	// Free-Form
	Button mFreeFormPhrase;
	TextView mFreeFormHeaderLabel;
	CheckBox mFreeFormAll;
	CheckBox mFreeFormFrom;
	CheckBox mFreeFormTo;
	CheckBox mFreeFormSubject;
	CheckBox mFreeFormCc;
	CheckBox mFreeFormBcc;
	CheckBox mFreeFormSmsMessage;

	View mTopView;
	FilterItemDO mFilterItem;
	int mFilterItemId;
	boolean mFreeFormPhraseInitialized = false;
	SetFreeFormHeaderListener mSetFreeFormHeaderListener;
	RTUpdateFragment mRTListener;

	boolean mIsInitialErrorCheckDone;

    public FreeFormMasterFragment() {
	    mIsInitialErrorCheckDone = false;
    }

	public boolean isInitialErrorCheckDone() {
		return mIsInitialErrorCheckDone;
	}

	public void setInitialErrorCheckDone() {
		mIsInitialErrorCheckDone = true;
	}

    public static FreeFormMasterFragment newInstance(int filterItemId) {
        FreeFormMasterFragment fragment = new FreeFormMasterFragment();

	    Bundle bundle = new Bundle();
	    bundle.putInt(TAG_FILTER_ITEM_ID, filterItemId);
	    fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

	    Bundle bundle = getArguments();
	    mFilterItemId = bundle.getInt(TAG_FILTER_ITEM_ID);
    }

	public void setRTListener(RTUpdateFragment listener) {
		mRTListener = listener;
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View returnView =
		        inflater.inflate(R.layout.free_form_master_fragment, container, false);

	    mTopView = returnView;
	    setViewPointers(returnView);
	    setInitialDbRecs();
	    setViewComponents(returnView);

	    return returnView;
    }

	@Override
	public void setViewPointers(View v) {
		mFreeFormPhrase =  (Button)v.findViewById(R.id.ffaf_phrase);
		mFreeFormHeaderLabel = (TextView)v.findViewById(R.id.ffaf_header_label);
		mFreeFormAll = (CheckBox)v.findViewById(R.id.ffaf_all);
		mFreeFormFrom = (CheckBox)v.findViewById(R.id.ffaf_from);
		mFreeFormTo = (CheckBox)v.findViewById(R.id.ffaf_to);
		mFreeFormSubject = (CheckBox)v.findViewById(R.id.ffaf_subject);
		mFreeFormCc = (CheckBox)v.findViewById(R.id.ffaf_cc);
		mFreeFormBcc = (CheckBox)v.findViewById(R.id.ffaf_bcc);
		mFreeFormSmsMessage = (CheckBox)v.findViewById(R.id.ffaf_sms_message);
	}

	@Override
	public void setInitialDbRecs() {
		mFilterItem = null;
		if (mFilterItemId != -1) {
			mFilterItem = FilterItems.get(mFilterItemId);
			mFilterItemId = mFilterItem.getFilterItemId();
		}
		if (mFilterItem == null) {
			newFilterItem();
		}
	}

	@Override
	public void setViewComponents(View v) {
		setMasterViewToDefaults();
		setViewDefaults();
		setMasterFieldHint();
		setMasterListeners();
	}

	@Override
	public void setMasterViewToDefaults() {
		if (mFreeFormPhrase != null) {
			// only populate with defaults if there's no data in NotificationItemDO
			if (mRTListener.mNotificationItem == null
					|| mRTListener.mNotificationItem.getNotificationItemId() == -1) {
				mFreeFormPhrase.setText(null);
				initializeNewMaster();
			}
		}
		enableDisableMasterFields();
	}

	public void setViewDefaults() {
		// make sure there's an mFilterItem
		if (mFilterItem == null) {
			setInitialDbRecs();
		}
		// set Phrase
		setMasterField(mFilterItem.getPhrase(), null, false);

		setViews();
	}

	private void setViews() {
		// set Headers checkboxes
		for (String header : mFilterItem.getFieldNamesArray()) {
			if (header.equals("*")) {
				mFreeFormAll.setChecked(true);
				unCheckAllButAll();
				break;
			}
			else if (header.equals(mFreeFormFrom.getText().toString())) {
				mFreeFormFrom.setChecked(true);
			}
			else if (header.equals(mFreeFormTo.getText().toString())) {
				mFreeFormTo.setChecked(true);
			}
			else if (header.equals(mFreeFormSubject.getText().toString())) {
				mFreeFormSubject.setChecked(true);
			}
			else if (header.equals(mFreeFormCc.getText().toString())) {
				mFreeFormCc.setChecked(true);
			}
			else if (header.equals(mFreeFormBcc.getText().toString())) {
				mFreeFormBcc.setChecked(true);
			}
			else if (header.equals(AutomatonAlert.SMS_BODY)) {
				mFreeFormSmsMessage.setChecked(true);
			}
		}
	}

	private void unCheckAllButAll() {
		mFreeFormFrom.setChecked(false);
		mFreeFormTo.setChecked(false);
		mFreeFormSubject.setChecked(false);
		mFreeFormCc.setChecked(false);
		mFreeFormBcc.setChecked(false);
		mFreeFormSmsMessage.setChecked(false);
	}

	private boolean areAllCheckedButAll() {
		return mFreeFormFrom.isChecked()
				&& mFreeFormTo.isChecked()
				&& mFreeFormSubject.isChecked()
				&& mFreeFormCc.isChecked()
				&& mFreeFormBcc.isChecked()
				&& mFreeFormSmsMessage.isChecked();

	}

	@Override
	public void setMasterFieldHint() {
		mFreeFormPhrase.setHint("Set Free-Form Phrase");
	}

	@Override
	public void setMasterField(String text, Object obj, boolean skipKeyFieldEqualCheck) {
		// if this isn't the first time
		if (mFreeFormPhraseInitialized &&
				!skipKeyFieldEqualCheck) {
			// return immediately if nothing's changed
			if (!isMasterFieldChanged(text, null)) {
				return;
			}
		}

		if (mFreeFormPhrase != null) {
			updateMasterFieldAndViews(text);
		}

		if (mFreeFormPhraseInitialized) {
			// delete RT
			if (TextUtils.isEmpty(text)) {
				deleteMaster();
				return;////////
			}

			mFilterItem.setPhrase(text);

			saveFilterItem();
		}

		// if we're coming in with a notificationItem already set
		// in mFilterItem, then it needs to show, otherwise, keep
		// it empty
		if (mFreeFormPhraseInitialized
				|| !mFreeFormPhraseInitialized
				&& mFilterItem.getNotificationItemId() != -1) {
			mRTListener.setRingtoneFromNotificationItem(true);
		}

		if (!mFreeFormPhraseInitialized) {
			mFreeFormPhraseInitialized = true;
		}

		// alpha-dim ringtone-required fields if there's no phrase
		enableDisableMasterFields();
	}

	@Override
	public boolean isMasterFieldChanged(String text, Object obj) {
		String textNow = (mFreeFormPhrase == null) ?
				"SomeTextThatIsNotTheSame"
				: mFreeFormPhrase.getText().toString();

		return !textNow.equals(text);
	}

	private void deleteFilterItem() {
		if (mFilterItem != null) {
			mFilterItem.delete();
		}
		newFilterItem();
	}

	@Override
	public void deleteMaster() {
		if (mFilterItem.getFilterItemId() == -1) {
			return;
		}
		deleteFilterItem();
		initializeNewMaster();
		mFilterItem.setPhrase("");
		updateMasterFieldAndViews(mFilterItem.getPhrase());
		enableDisableMasterFields();

		setMasterViewToDefaults();
		setViewComponents(mTopView);
	}

	@Override
	public void updateMasterFieldAndViews(String phrase) {
		mFreeFormPhrase.setText(phrase);
	}

	@Override
	public void setMasterListeners() {
		mFreeFormPhrase.setOnClickListener(new SetFreeFormPhraseListener());

		mSetFreeFormHeaderListener = new SetFreeFormHeaderListener();

		mFreeFormAll.setOnClickListener(mSetFreeFormHeaderListener);
		mFreeFormFrom.setOnClickListener(mSetFreeFormHeaderListener);
		mFreeFormSubject.setOnClickListener(mSetFreeFormHeaderListener);
		mFreeFormTo.setOnClickListener(mSetFreeFormHeaderListener);
		mFreeFormCc.setOnClickListener(mSetFreeFormHeaderListener);
		mFreeFormBcc.setOnClickListener(mSetFreeFormHeaderListener);
		mFreeFormSmsMessage.setOnClickListener(mSetFreeFormHeaderListener);
	}

	@Override
	public void enableDisableMasterFields() {
		if (TextUtils.isEmpty(mFreeFormPhrase.getText().toString())) {
			setAlphaMasterFields(.3f);
			enableDisableMasterFields(false);
		}
		else {
			setAlphaMasterFields(1f);
			enableDisableMasterFields(true);
		}
		mRTListener.showHidePlayFor();
	}

	@Override
	public void enableDisableMasterFields(boolean enable) {
		mFreeFormHeaderLabel.setEnabled(enable);
		mFreeFormAll.setEnabled(enable);
		mFreeFormFrom.setEnabled(enable);
		mFreeFormTo.setEnabled(enable);
		mFreeFormSubject.setEnabled(enable);
		mFreeFormCc.setEnabled(enable);
		mFreeFormBcc.setEnabled(enable);
		mFreeFormSmsMessage.setEnabled(enable);
	}

	@Override
	public void setAlphaMasterFields(float f) {
		mFreeFormHeaderLabel.setAlpha(f);
		mFreeFormAll.setAlpha(f);
		mFreeFormFrom.setAlpha(f);
		mFreeFormTo.setAlpha(f);
		mFreeFormSubject.setAlpha(f);
		mFreeFormCc.setAlpha(f);
		mFreeFormBcc.setAlpha(f);
		mFreeFormSmsMessage.setAlpha(f);
	}

	@Override
	public void initializeNewMaster() {
		mFilterItem.setFieldNames("Subject,Message");
	}

	@Override
	public void saveMasterNotificationItemId(int id) {
		mFilterItem.setNotificationItemId(id);
		saveFilterItem();
	}

	private void saveFilterItem() {
		mFilterItem.save();
		mFilterItemId = mFilterItem.getFilterItemId();

		setListenerFilterItem();
	}

	private void newFilterItem() {
		mFilterItem = new FilterItemDO();
		mFilterItemId = mFilterItem.getFilterItemId();

		setListenerFilterItem();
	}

	public void reGetFilterItem() {
		mFilterItem = FilterItems.get(mFilterItemId);

		setListenerFilterItem();
	}

	private void setListenerFilterItem() {
		mRTListener.reSetFilterItem(mFilterItem);
	}

	@Override
	public boolean clearMaster(Object ... obj) {
		initializeNewMaster();
		setMasterField("", null, true/*skipFreeFormEqualCheck*/);

		return false;
	}

	@Override
	public void addDefaultAccounts() {
		ArrayList<NameValueDataDO> specPrefs =
				NameValueDataDO.get(
						RTPrefsDO.getDefaultLinkedAccountSpecificPrefix(),
						true/*startsWith*/,
						null/*pref value*/);
		FilterItemAccounts.addAll(specPrefs, mFilterItemId);
		// need to refresh mFilterItem here and in Detail (RTUpdateFragment)
		reGetFilterItem();
	}

	@Override
	public Spanned getNoAccountsErrorText() {
		return Html.fromHtml(
				"<i>One or more SMS or Email accounts needs to be chosen. "
						+ "SMS/Email accounts are scanned for the given text. "
						+ "Click the Add/Modify Email Accounts button to add an "
						+ "SMS or Email account.</i>");
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

	class SetFreeFormHeaderListener implements CheckBox.OnClickListener {
		@Override
		public void onClick(View v) {
			CheckBox cb = (CheckBox)v;
			if (cb.isChecked()) {
				// if they're all checked, uncheck
				// them all and check All
				if (cb == mFreeFormAll
						|| areAllCheckedButAll()) {
					unCheckAllButAll();
					mFreeFormAll.setChecked(true);
				}
				else {
					// if All is checked, un-check since
					// we're in individual mode now
					mFreeFormAll.setChecked(false);
					if (cb == mFreeFormSubject) {
						mFreeFormSmsMessage.setChecked(true);
					}
				}
			}

			mFilterItem.setFieldNames(checkBoxesAsAString());
			saveFilterItem();
		}
	}

	private String checkBoxesAsAString() {
		String out = "";

		if (mFreeFormAll.isChecked()) {
			return "*";
		}
		if (mFreeFormFrom.isChecked())          out += mFreeFormFrom.getText().toString() + ",";
		if (mFreeFormTo.isChecked())            out += mFreeFormTo.getText().toString() + ",";
		if (mFreeFormSubject.isChecked())       out += mFreeFormSubject.getText().toString() + ",";
		if (mFreeFormCc.isChecked())            out += mFreeFormCc.getText().toString() + ",";
		if (mFreeFormBcc.isChecked())           out += mFreeFormBcc.getText().toString() + ",";
		if (mFreeFormSmsMessage.isChecked())    out += AutomatonAlert.SMS_BODY + ",";

		if (out.endsWith(",")) {
			out = out.substring(0, out.length() - 1);
		}

		return out;
	}

	FreeFormPhraseDialog mFreeFormPhraseDialog;

	class SetFreeFormPhraseListener implements Button.OnClickListener {
		@Override
		public void onClick(View v) {
			mFreeFormPhraseDialog = FreeFormPhraseDialog.showInstance(
					(AppCompatActivity) getActivity());
		}
	}

}
