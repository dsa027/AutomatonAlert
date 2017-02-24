package com.aldersonet.automatonalert.Preferences;

import android.content.Context;
import android.preference.Preference;
import android.support.v4.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import com.aldersonet.automatonalert.R;

public class OverrideVolumePreference extends Preference {
	Context mContext;
	PreferenceFragment mFragment;

	RadioButton mSilent;
	RadioButton mDefault;
	RadioButton mLow;
	RadioButton mMedium;
	RadioButton mHigh;

	RadioButton.OnClickListener mRadioButtonOnClickListener =
			new RadioButton.OnClickListener() {
				@Override
				public void onClick(View v) {
					updateOverride(v);
				}
			};

	public OverrideVolumePreference(Context context, PreferenceFragment fragment) {
		super(context);
		mContext = context;
		mFragment = fragment;
	}

	private void updateOverride(View v) {
		switch(v.getId()) {
			case R.id.ov_silent:
				GeneralPrefsDO.setOverrideVol(GeneralPrefsDO.OverrideVolLevel.SILENT.name());
				break;
			case R.id.ov_default:
				GeneralPrefsDO.setOverrideVol(GeneralPrefsDO.OverrideVolLevel.DEFAULT.name());
				break;
			case R.id.ov_low:
				GeneralPrefsDO.setOverrideVol(GeneralPrefsDO.OverrideVolLevel.LOW.name());
				break;
			case R.id.ov_medium:
				GeneralPrefsDO.setOverrideVol(GeneralPrefsDO.OverrideVolLevel.MED.name());
				break;
			case R.id.ov_high:
				GeneralPrefsDO.setOverrideVol(GeneralPrefsDO.OverrideVolLevel.HI.name());
				break;
		}
		GeneralPrefsDO.save();
		SettingsFragment.updateWidget(mFragment.getActivity());
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		View view = ((LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
				.inflate(R.layout.override_volume, parent, false);

		mSilent = (RadioButton)view.findViewById(R.id.ov_silent);
		mDefault = (RadioButton)view.findViewById(R.id.ov_default);
		mLow = (RadioButton)view.findViewById(R.id.ov_low);
		mMedium = (RadioButton)view.findViewById(R.id.ov_medium);
		mHigh = (RadioButton)view.findViewById(R.id.ov_high);

		mSilent.setOnClickListener(mRadioButtonOnClickListener);
		mDefault.setOnClickListener(mRadioButtonOnClickListener);
		mLow.setOnClickListener(mRadioButtonOnClickListener);
		mMedium.setOnClickListener(mRadioButtonOnClickListener);
		mHigh.setOnClickListener(mRadioButtonOnClickListener);

		String state = GeneralPrefsDO.getOverrideVol();
		if (state.equals(GeneralPrefsDO.OverrideVolLevel.SILENT.name())) {
			mSilent.setChecked(true);
		}
		else if (state.equals(GeneralPrefsDO.OverrideVolLevel.DEFAULT.name())) {
			mDefault.setChecked(true);
		}
		else if (state.equals(GeneralPrefsDO.OverrideVolLevel.LOW.name())) {
			mLow.setChecked(true);
		}
		else if (state.equals(GeneralPrefsDO.OverrideVolLevel.MED.name())) {
			mMedium.setChecked(true);
		}
		else if (state.equals(GeneralPrefsDO.OverrideVolLevel.HI.name())) {
			mHigh.setChecked(true);
		}

		return view;
	}

}
