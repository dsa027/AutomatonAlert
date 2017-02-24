package com.aldersonet.automatonalert.Activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.aldersonet.automatonalert.Fragment.TakeATourFragment;
import com.aldersonet.automatonalert.R;


public class TakeATourActivity extends AppCompatActivity {
	public static final int NUM_PAGE_VIEWS = 5;
	public ViewPager mViewPager;
	TourPagerAdapter mPagerAdapter;

    @SuppressLint("AppCompatMethod")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.take_a_tour_activity);

	    mPagerAdapter = new TourPagerAdapter(getSupportFragmentManager());
	    mViewPager = (ViewPager) findViewById(R.id.pager);
	    mViewPager.setAdapter(mPagerAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	    return false;
    }
	public class TourPagerAdapter extends FragmentStatePagerAdapter {

		public TourPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			return TakeATourFragment.newInstance(position);
		}

		@Override
		public int getCount() {
			return NUM_PAGE_VIEWS;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		System.gc();
	}
}
