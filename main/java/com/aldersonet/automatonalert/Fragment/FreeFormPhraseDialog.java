package com.aldersonet.automatonalert.Fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import com.aldersonet.automatonalert.R;

import java.util.List;

public class FreeFormPhraseDialog extends DialogFragment {

	TextView mDescription;
	EditText mPhrase;
	TextView mOk;

	String mInPhrase;

	AppCompatActivity mActivity;
	FreeFormMasterFragment mFragment;

	/* this is "newInstance()" + show() */
	public static FreeFormPhraseDialog showInstance(
			AppCompatActivity activity) {

		final FreeFormPhraseDialog dialog = new FreeFormPhraseDialog();
		final FragmentManager fm = activity.getSupportFragmentManager();

		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dialog.show(fm, "FreeFormPhraseDialog");
			}
		});

		return dialog;
	}

	public FreeFormPhraseDialog() {
		super();
	}

	private FreeFormMasterFragment findOurFragment() {
		if (mActivity != null) {
			List<Fragment> list = mActivity.getSupportFragmentManager().getFragments();
			for (Fragment fragment : list) {
				if (fragment instanceof FreeFormMasterFragment) {
					FreeFormMasterFragment ffFragment = (FreeFormMasterFragment)fragment;
					if (ffFragment.mFreeFormPhraseDialog != null
							&& ffFragment.mFreeFormPhraseDialog == this) {
						return ffFragment;
					}
				}
			}
		}

		return null;
	}

	@Override
	public void onAttach(Activity activity) {
		if (activity instanceof AppCompatActivity) {
			mActivity = (AppCompatActivity)activity;
			mFragment = findOurFragment();
			if (mFragment != null
					&& mFragment.mFreeFormPhrase != null) {
				mInPhrase = mFragment.mFreeFormPhrase.getText().toString();
			}
		}
		super.onAttach(activity);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

		return dialog;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		final Dialog dialog = getDialog();
		dialog.setCanceledOnTouchOutside(false);

		Resources res = mActivity.getResources();

		View view = inflater.inflate(R.layout.free_form_phrase_dialog, container, false);

		mDescription = (TextView)view.findViewById(R.id.ffpd_simple_regex_description);
		mPhrase = (EditText)view.findViewById(R.id.ffpd_phrase);
		mOk = (TextView)view.findViewById(R.id.ffpd_ok);

		mPhrase.setText(mInPhrase);
		mDescription.setText(Html.fromHtml(res.getString(R.string.simple_regex_description)));
		mOk.setOnClickListener(new TextView.OnClickListener() {
			@Override
			public void onClick(View v) {
				FreeFormPhraseDialog.this.dismiss();
			}
		});

		return view;
	}

	@Override
	public void onDetach() {
		super.onDetach();

		if (mFragment != null) {
			mFragment.mFreeFormPhraseDialog = null;
		}
	}

	@Override
	public void onStop() {
		super.onStop();

		String phrase = mPhrase.getText().toString();

		if (mFragment != null) {
			mFragment.setMasterField(phrase, null, true);
		}
	}
}
