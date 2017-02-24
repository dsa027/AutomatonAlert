package com.aldersonet.automatonalert.OkCancel;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aldersonet.automatonalert.Preferences.NameValueDataDO;
import com.aldersonet.automatonalert.R;

public class OkCancelDialog extends DialogFragment {
	public static final String TAG_MESSAGE = "message";
	public static final String TAG_CHECKBOX_TEXT = "checkBoxText";
	public static final String TAG_LEFT_BUTTON_TEXT = "leftButtonText";
	public static final String TAG_RIGHT_BUTTON_TEXT = "rightButtonText";
	public static final String TAG_CANCEL_BUTTON = "cancelButton";
	public static final String TAG_EWI = "ewi";

	public enum EWI {
		ERROR, WARNING, INFO
	}

	public enum CancelButton {
		LEFT, RIGHT, NEITHER
	}

	public enum UserActionOnChecked {
		LEFT, RIGHT, NEITHER, DONT_SKIP
	}

	private OkCancel mOkCancel;
	private String mMessage;
	private String mCheckBoxText;
	private String mLeftViewText;       // OkCancel.cancel()
	private String mRightViewText;      // OkCancel.ok()
	private EWI mEWI;
	private CancelButton mCancelButton;

	TextView mErrorMessageView;
	CheckBox mCheckBoxView;
	TextView mCheckBoxTextView;
	Button mRightButton;
	LinearLayout mRightContainer;
	Button mLeftButton;
	LinearLayout mLeftContainer;
	LinearLayout mTopView;
	View mButtonTopView;
	LinearLayout mTagLineView;
	RelativeLayout mCheckBoxLayoutView;

	/* this is "newInstance()" + show() */
	public static OkCancelDialog showInstance(
			AppCompatActivity activity,
			final String message, final String checkBoxText,
			final String leftButtonText, final String rightButtonText,
			CancelButton cancelButton, EWI ewi) {

		if (activity == null) {
			return null;
		}

		final OkCancelDialog dialog = new OkCancelDialog();
		final FragmentManager fm = activity.getSupportFragmentManager();

		dialog.setArgs(
				dialog, message, checkBoxText, leftButtonText,
				rightButtonText, cancelButton, ewi);

		activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.show(fm, "OkCancelDialog"+message+leftButtonText+rightButtonText);
            }
        });

		return dialog;
	}

	private void setFont() {
		mErrorMessageView.setTextSize(20F);
	}

	public void setArgs(DialogFragment dialog, String message,
			String checkBoxText, String leftButtonText,
			String rightButtonText, CancelButton cancelButton, EWI ewi) {

		Bundle bundle = new Bundle();
		bundle.putString(TAG_MESSAGE, message);
		bundle.putString(TAG_CHECKBOX_TEXT, checkBoxText);
		bundle.putString(TAG_LEFT_BUTTON_TEXT, leftButtonText);
		bundle.putString(TAG_RIGHT_BUTTON_TEXT, rightButtonText);
		bundle.putString(TAG_CANCEL_BUTTON, cancelButton.name());
		bundle.putString(TAG_EWI, ewi.name());

		dialog.setArguments(bundle);
	}

	private void getArgs() {
		Bundle bundle = getArguments();

		mMessage = bundle.getString(TAG_MESSAGE, "");
		mCheckBoxText = bundle.getString(TAG_CHECKBOX_TEXT, null);
		mLeftViewText = bundle.getString(TAG_LEFT_BUTTON_TEXT, "Ok");
		mRightViewText = bundle.getString(TAG_RIGHT_BUTTON_TEXT, "Ok");

		try {
			mCancelButton =
					CancelButton.valueOf(bundle.getString(TAG_CANCEL_BUTTON));
		} catch (IllegalArgumentException e) {
			mCancelButton = CancelButton.NEITHER;
		}

		try {
			mEWI =
					EWI.valueOf(bundle.getString(TAG_EWI));
		} catch (IllegalArgumentException e) {
			mEWI = EWI.INFO;
		}

		// empty until setOkCancel() is called
		if (mOkCancel == null) {
			mOkCancel = new OkCancel();
		}
	}

	public void setOkCancel(OkCancel okCancel) {
		mOkCancel = okCancel;
	}

	public OkCancelDialog() {
		super();
	}

	private int getEWIBackground() {
		switch(mEWI) {
		case ERROR:
			return android.R.color.holo_red_dark;
		case WARNING:
			return android.R.color.holo_orange_dark;
		case INFO:
			return R.color.native_dark_blue;
		default:
			return R.color.native_dark_blue;
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

		getArgs();
		if (mCancelButton == null
				|| mCancelButton.equals(CancelButton.NEITHER)) {
			setCancelable(false);
		}

		return dialog;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		final Dialog dialog = getDialog();
		dialog.setCanceledOnTouchOutside(false);

		View view = inflater.inflate(
						R.layout.ok_cancel_checkbox_custom_dialog, container, false);

		mErrorMessageView = (TextView)view.findViewById(R.id.occcd_error_text);
		mCheckBoxView = (CheckBox)view.findViewById(R.id.occcd_error_dont_show_again);
		mCheckBoxTextView = (TextView)view.findViewById(R.id.occcd_error_dont_show_again_text);
		mLeftButton = (Button)view.findViewById(R.id.occcd_error_left_button);
		mLeftContainer = (LinearLayout)view.findViewById(R.id.occcd_error_left_button_layout);
		mRightButton = (Button)view.findViewById(R.id.occcd_error_right_button);
		mRightContainer = (LinearLayout)view.findViewById(R.id.occcd_error_right_button_layout);
		mTopView = (LinearLayout)view.findViewById(R.id.occcd_error_top_layout);
		mButtonTopView = view.findViewById(R.id.occcd_button_separator_top);
		mTagLineView = (LinearLayout)view.findViewById(R.id.occcd_tag_line_layout);
		mCheckBoxLayoutView = (RelativeLayout)view.findViewById(R.id.occcd_checkbox_layout);

		mErrorMessageView.setMovementMethod(new ScrollingMovementMethod());
		mErrorMessageView.setScrollbarFadingEnabled(false);

		mErrorMessageView.setText(Html.fromHtml(mMessage));

		// disappear 'em if there's no button text
		mLeftButton.setText(Html.fromHtml(mLeftViewText));
		mLeftContainer.setVisibility(mLeftViewText.equals("") ?
				ViewGroup.GONE : ViewGroup.VISIBLE);

		// disappear 'em if there's no button text
		mRightButton.setText(Html.fromHtml(mRightViewText));
		mRightContainer.setVisibility(mRightViewText.equals("") ?
				ViewGroup.GONE : ViewGroup.VISIBLE);

		// set background according to INFO, ERROR, WARNING
		mTopView.setBackgroundResource(getEWIBackground());
		mButtonTopView.setBackgroundResource(getEWIBackground());

		// we may not want the checkBox
		if (TextUtils.isEmpty(mCheckBoxText)) {
			mCheckBoxLayoutView.setVisibility(CheckBox.GONE);
			if (mCheckBoxText == null) {
				mTagLineView.setVisibility(LinearLayout.VISIBLE);
				setFont();
			}
			else {
				mTagLineView.setVisibility(LinearLayout.GONE);
			}
		}
		else {
			mCheckBoxTextView.setText(Html.fromHtml(mCheckBoxText));
			mCheckBoxView.setVisibility(CheckBox.VISIBLE);
			mTagLineView.setVisibility(LinearLayout.GONE);
		}

		mLeftButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				mOkCancel.doLeftButtonPressed();
				dialog.dismiss();
			}
		});
		mRightButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				mOkCancel.doRightButtonPressed();
				dialog.dismiss();
			}
		});

		mCheckBoxTextView.setOnClickListener(new TextView.OnClickListener() {
			@Override
			public void onClick(View v) {
				mCheckBoxView.setChecked(!mCheckBoxView.isChecked());
			}
		});

		return view;
	}

	public static UserActionOnChecked isSkipDialog(String settingName) {
		// get the setting from the db
		NameValueDataDO skipDialog = NameValueDataDO.get(settingName, null);

		// mistake
		if (skipDialog == null) {
			return UserActionOnChecked.DONT_SKIP;
		}

		// old values were "0":"1".
		// user will have to choose again
		if (skipDialog.getValue().equals("1")) {
			skipDialog.delete();
			return UserActionOnChecked.DONT_SKIP;
		}

		// return db value LEFT, RIGHT, NEITHER, DONT_SKIP
		try {
			return UserActionOnChecked.valueOf(skipDialog.getValue());

		} catch (IllegalArgumentException ignored) {
			return UserActionOnChecked.DONT_SKIP;
		}
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		doBackArrowCancel();
	}

	private void doBackArrowCancel() {
		// LEFT
		if (mCancelButton.equals(CancelButton.LEFT)) {
			mOkCancel.doLeftButtonPressed();
		}
		// RIGHT
		else {
			mOkCancel.doRightButtonPressed();
		}
	}

	public static boolean isCheckBoxChecked(OkCancelDialog okCancelDialog) {
		if (okCancelDialog == null) {
			return false;
		}

		View v = okCancelDialog.getView();
		if (v != null) {
			CheckBox cb = (CheckBox) v.findViewById(R.id.occcd_error_dont_show_again);
			return cb.isChecked();
		}

		return false;
	}

	private static CheckBox getCheckBox(OkCancelDialog dialog) {
		if (dialog == null) {
			return null;
		}
		View v = dialog.getView();
		if (v == null) {
			return null;
		}

		CheckBox cb = (CheckBox)v.findViewById(R.id.occcd_error_dont_show_again);

		if (cb == null
				|| !cb.isChecked()) {
			return null;
		}

		return cb;
	}

	public static void saveOkCancelCheckBox(OkCancelDialog dialog, String settingName) {
		CheckBox cb = getCheckBox(dialog);
		if (cb == null) {
			return;
		}
		UserActionOnChecked lastButton =
				(dialog.mOkCancel.mLastButtonPressed == null) ?
						  UserActionOnChecked.DONT_SKIP
						: dialog.mOkCancel.mLastButtonPressed;

		// DB NameValue
		NameValueDataDO nv = NameValueDataDO.get(settingName, null);
		// make sure we have a NameValueDataDO
		if (nv == null) {
			// set it to opposite of checkBox so it'll get saved
			nv = new NameValueDataDO(settingName, lastButton.name());
		}
		else {
			nv.setValue(lastButton.name());
		}
		nv.save();
	}
}
