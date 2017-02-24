package com.aldersonet.automatonalert.Activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.ActionBar.ActionBarDrawer;
import com.aldersonet.automatonalert.Util.Utils;

import org.jetbrains.annotations.NotNull;

public class HelpActivity extends AppCompatActivity {

	ActionBarDrawer mActionBarDrawer;

	TextView mTour;
	TextView mAbout;
	TextView mLicenses;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
		Utils.setActionBarCommon(getResources(), getSupportActionBar(), "Help");
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		setDrawer();
		setViewPointers();
		setListeners();
	}

	private void setViewPointers() {
		mTour = (TextView)findViewById(R.id.qh_walkthrough);
		mAbout = (TextView)findViewById(R.id.qh_about);
		mLicenses = (TextView)findViewById(R.id.qh_licenses);
	}

	private void setListeners() {
		mTour.setOnClickListener(mTourListener);
		mAbout.setOnClickListener(mAboutListener);
		mLicenses.setOnClickListener(mLicensesListener);
	}

	TextView.OnClickListener mTourListener =
			new TextView.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(
							getApplicationContext(), TakeATourActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
				}
			};

	TextView.OnClickListener mAboutListener =
			new TextView.OnClickListener() {
				@Override
				public void onClick(View v) {
					Utils.showOkCancelAbout(HelpActivity.this);
				}
			};

	TextView.OnClickListener mLicensesListener =
			new TextView.OnClickListener() {
				@Override
				public void onClick(View v) {
					Utils.showOkCancelLicenses(HelpActivity.this);
				}
			};

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
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mActionBarDrawer.getDrawerToggle().syncState();
	}

	@Override
	public boolean onKeyUp(int keyCode, @NotNull KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			mActionBarDrawer.openDrawer();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	private void setDrawer() {
		mActionBarDrawer = new ActionBarDrawer(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
		System.gc();
	}
}
