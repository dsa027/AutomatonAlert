package com.aldersonet.automatonalert.Fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.aldersonet.automatonalert.Activity.AccountAddUpdateActivity;
import com.aldersonet.automatonalert.Activity.FragmentHostActivity;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Activity.TakeATourActivity;

import java.lang.ref.WeakReference;

public class TakeATourFragment extends Fragment {
    private Integer mPage = 0;
	Bitmap mBitmap;
	Resources mResources;
	private static final String TAG_PAGE = "mPage";

    public static TakeATourFragment newInstance(int page) {
        TakeATourFragment fragment = new TakeATourFragment();
        Bundle args = new Bundle();
        args.putInt(TAG_PAGE, page);
        fragment.setArguments(args);

        return fragment;
    }
    public TakeATourFragment() {
    }

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
	        mPage = getArguments().getInt(TAG_PAGE);
        }
    }

	private int getPageResource(int page) {
		switch(page) {
			case 0:
				return R.layout.tour_page_1;
			case 1:
				return R.layout.tour_page_2;
			case 2:
				return R.layout.tour_page_3;
			case 3:
				return R.layout.tour_page_4;
			case 4:
				return R.layout.tour_page_5;
		}

		return 0;
	}

	private int getImageResource(int page) {
		switch(page) {
			case 0:
				return R.drawable.tour_contact_new_tab;
			case 1:
				return R.drawable.tour_contact_edit_tab;
			case 2:
				return R.drawable.tour_free_form_list_tab;
			case 3:
				return R.drawable.tour_edit_email_ringtone;
			case 4:
				return R.drawable.tour_action_bar_drawer;
		}

		return 0;
	}

	private void recycleBitmap() {
		if (mBitmap != null) {
			mBitmap.recycle();
		}
	}

	private void showBitmap(View v) {
		if (v != null) {
			ImageView iv = (ImageView)v.findViewById(R.id.image);
			if (iv != null) {
				new BitmapWorkerTask(iv).execute(getImageResource(mPage));
			}
		}
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
	    mResources = getActivity().getResources();
	    // Inflate the layout for this fragment
	    View view = inflater.inflate(getPageResource(mPage), container, false);
		showBitmap(view);
	    setListeners(view);

	    return view;
    }

	class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {
		private final WeakReference<ImageView> imageViewReference;

		public BitmapWorkerTask(ImageView imageView) {
			// Use a WeakReference to ensure the ImageView can be garbage collected
			imageViewReference = new WeakReference<ImageView>(imageView);
		}

		// Decode image in background.
		@Override
		protected Bitmap doInBackground(Integer... params) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Bitmap.Config.RGB_565;
			options.inSampleSize = 2;
			mBitmap = BitmapFactory.decodeResource(
					mResources, getImageResource(mPage), options);

			return mBitmap;
		}

		// Once complete, see if ImageView is still around and set bitmap.
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (imageViewReference != null && bitmap != null) {
				final ImageView imageView = imageViewReference.get();
				if (imageView != null) {
					imageView.setImageBitmap(bitmap);
				}
			}
		}
	}	private void setListeners(View view) {
		// Register New Email Account
		TextView register = (TextView)view.findViewById(R.id.tp4_register_account);
		TextView setGlobal = (TextView)view.findViewById(R.id.tp5_set_global_ringtone);
		TextView exit =  (TextView)view.findViewById(R.id.exit);
		TextView prev =  (TextView)view.findViewById(R.id.prev);
		TextView next =  (TextView)view.findViewById(R.id.next);

		if (register != null) {
			register.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(
							getActivity().getApplicationContext(),
							AccountAddUpdateActivity.class);
					intent.putExtra(AutomatonAlert.M_MODE, AutomatonAlert.ADD);
					intent.setFlags(
							Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
					getActivity().startActivity(intent);
				}
			});
		}

		// Set Global Ringtone
		if (setGlobal != null) {
			setGlobal.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(
							getActivity().getApplicationContext(),
							FragmentHostActivity.class);
					intent.putExtra(
							AutomatonAlert.FRAGMENT_TYPE,
							FragmentHostActivity.HostFragmentType.GLOBAL_RINGTONE.name());
					intent.setFlags(
							Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
					getActivity().startActivity(intent);
				}
			});
		}

		// Exit tour
		if (exit != null) {
			exit.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View v) {
					getActivity().finish();
				}
			});
		}

		// Previous page
		if (prev != null) {
			prev.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View v) {
					TakeATourActivity activity = (TakeATourActivity)getActivity();
					int page = activity.mViewPager.getCurrentItem();
					activity.mViewPager.setCurrentItem(--page);
				}
			});
		}

		// Next page
		if (next != null) {
			next.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View v) {
					TakeATourActivity activity = (TakeATourActivity)getActivity();
					int page = activity.mViewPager.getCurrentItem();
					activity.mViewPager.setCurrentItem(++page);
				}
			});
		}

	}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
	    recycleBitmap();
	    System.gc();
    }

}
