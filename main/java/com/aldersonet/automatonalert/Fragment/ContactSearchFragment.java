package com.aldersonet.automatonalert.Fragment;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.aldersonet.automatonalert.ActionMode.ContactListInfo;
import com.aldersonet.automatonalert.Activity.ContactFreeFormListActivity;
import com.aldersonet.automatonalert.Activity.ContactFreeFormListActivity.FragmentTypeCL;
import com.aldersonet.automatonalert.Activity.ICLActivity;
import com.aldersonet.automatonalert.Adapter.ContactCursorAdapter;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Util.Utils;

public class ContactSearchFragment extends ListFragment
    	implements
			TextWatcher,
			LoaderManager.LoaderCallbacks<Cursor>,
		ICLFragmentController {

	public static final String TAG = "ContactSearchFragment";

	private IContactSearchFragmentListener mActivityListener;

	public FragmentTypeCL mFragmentType = FragmentTypeCL.SEARCH;
	public ContactCursorAdapter mAdapter;
	ListView mListView;
	String mCurFilter;
	TextView mEmptyListText;

	int mScrollPos = -1;
	int mScrollTop = -1;

	public static ContactSearchFragment newInstance() {
		return new ContactSearchFragment();
	}

	@Override
	public ListView getListView() {
		return mListView;
	}

	@Override
	public void stopActionModeAllFragments() {
		if (getActivity() instanceof ContactFreeFormListActivity) {
			((ICLActivity)getActivity()).stopActionModeAllFragments();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		stopActionModeAllFragments();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		refreshData(false/*forced*/);
	}

	private static int mLayout = R.layout.contact_search_contact_list_fragment;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View returnView = inflater.inflate(mLayout, container, false);
		setViewComponents(returnView);
		return returnView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
		EditText et = (EditText)getActivity().findViewById(R.id.csl_search);
		if (et != null) {
			et.addTextChangedListener(this);
		}
	}

	@Override
	public void refreshData(boolean forced) {
		Log.d(TAG + ".refreshData()", "refreshing");

		if (mListView != null) {
			Pair<Integer, Integer> pos = Utils.getScrollPosition(mListView);
			mScrollPos = pos.first;
			mScrollTop = pos.second;
		}

		try {
			ContactCursorAdapter adapter = new ContactCursorAdapter(
					getActivity().getApplicationContext(),
					null,
					(AppCompatActivity) getActivity(),
					this,
					forced);

			setListAdapter(adapter);
			mAdapter = adapter;

		} catch (NullPointerException e) {
			return;
		}

		getLoaderManager().initLoader(0, null, this);

		// scroll to...
		if (mScrollPos != -1
				&& mListView != null) {
			mListView.setSelectionFromTop(mScrollPos, mScrollTop);
		}
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		String sString = s.toString();
	    mCurFilter = TextUtils.isEmpty(sString) ? null : sString;
	    getLoaderManager().restartLoader(0, null, this);
	}

	// These are the Contacts rows that we will retrieve.
	static final String[] CONTACTS_PROJECTION = new String[] {
	    Contacts._ID,
	    Contacts.LOOKUP_KEY,
	    Contacts.DISPLAY_NAME
	};

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Uri baseUri;
	    if (mCurFilter != null) {
	        baseUri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI,
	                Uri.encode(mCurFilter));
	    } else {
	        baseUri = Contacts.CONTENT_URI;
	    }

	    return new CursorLoader(getActivity(), baseUri,
	            CONTACTS_PROJECTION, null, null,
	            Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
	    mAdapter.swapCursor(data);
		sendAdapterToActivity();
		// don't show empty list message if the
		// user is searching for something
		if (TextUtils.isEmpty(mCurFilter)) {
			showTextForEmptyList();
		}
	}

	@Override
	public void showTextForEmptyList() {
		if (mEmptyListText == null) {
			return;
		}
		ContactListGenericBase.showHideTextForEmptyList(
				(ICLActivity) getActivity(),
				(mAdapter == null ? 0 : mAdapter.getCount()),
				mFragmentType,
				mEmptyListText,
				R.string.contact_search_empty_list,
				ContactListGenericBase.getIntentShowContacts());
	}

	public void onLoaderReset(Loader<Cursor> loader) {
	    mAdapter.swapCursor(null);
	}

	private void setViewComponents(View v) {
		mListView = (ListView)v.findViewById(android.R.id.list);
		mEmptyListText =
				(TextView)v.findViewById(R.id.cl_empty_list_text);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (mAdapter == null) {
			refreshData(false/*forced*/);
		}
		else {
			ContactListInfo.refreshRow(data, mAdapter);
		}
	}

	@Override
	public String getInfo() {
		return null;
	}

	@Override
	public BaseAdapter sendAdapterToActivity() {
		if (mActivityListener != null) {
			return mActivityListener.setFragmentAdapterHandle(mAdapter);
		}
		return null;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mActivityListener = (IContactSearchFragmentListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement IContactSearchFragmentListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mActivityListener = null;
		mAdapter = null;
	}

	@Override
	public void afterTextChanged(Editable s) {
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	public interface IContactSearchFragmentListener {
		public BaseAdapter setFragmentAdapterHandle(BaseAdapter listAdapter);
	}

	@Override
	public FragmentTypeCL getFragmentType() {
		return mFragmentType;
	}

	@Override
	public BaseAdapter getAdapter() {
		return mAdapter;
	}
}
