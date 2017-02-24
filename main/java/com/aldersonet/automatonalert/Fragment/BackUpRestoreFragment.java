package com.aldersonet.automatonalert.Fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.ActionBar.ProgressBar;
import com.aldersonet.automatonalert.BackupRestore.BackupRestore;
import com.aldersonet.automatonalert.OkCancel.OkCancel;
import com.aldersonet.automatonalert.OkCancel.OkCancelDialog;
import com.aldersonet.automatonalert.BackupRestore.RestoreListView;

import java.io.File;
import java.util.ArrayList;

public class BackUpRestoreFragment extends Fragment
		implements IProgressBarListener {

    private IBackUpRestoreFragmentListener mListener;
	ProgressBar mProgressBar = ProgressBar.getInstance();
	long mProgressBarStartKey = Integer.MAX_VALUE;

	BackupRestore mBackupRestore;
	ArrayList<File> mBackupFiles;

	RestoreListView mRestoreList;
	TextView mRestore;
	ImageView mDelete;
	TextView mBackup;
	TextView mListEmpty;
	File mSelected;

	RestoreItemOnClickListener mRestoreItemOnClickListener;
	BackupOnClickListener mBackupOnClickListener;
	DeleteItemOnClickListener mDeleteItemOnClickListener;
	BackupRestoreListener mBackupRestoreListeners;
	SelectItemOnClickListener mSelectItemOnClickListener;

	ArrayAdapter<File> mAdapter;

    public static BackUpRestoreFragment newInstance() {
	    return new BackUpRestoreFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

	@Override
	public void onStart() {
		super.onStart();
		mProgressBar = ProgressBar.getInstance();
	}

	@Override
	public void onStop() {
		super.onStop();
		if (mProgressBar != null) {
			mProgressBar.stop(mProgressBarStartKey);
			mProgressBar = null;
		}
	}

	public void refresh() {
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void startProgressBar() {
		mProgressBarStartKey = ProgressBar.startProgressBar(
				mProgressBar,
				mProgressBarStartKey,
				new ProgressBar.StartObject(
						(AppCompatActivity) getActivity(),
						null,//(BaseAdapter)this,
						null/* adapterList */
				));
	}

	public void stopProgressBar() {
		mProgressBarStartKey =
				ProgressBar.stopProgressBar(mProgressBar, mProgressBarStartKey);
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.back_up_restore_fragment, container, false);

	    setViewComponents(v);

	    mBackupRestore = new BackupRestore(getActivity(), mBackupRestoreListeners);
	    mBackupFiles = BackupRestore.getBackupFiles();
	    if (mBackupFiles == null) {
		    mBackupFiles = new ArrayList<File>();
		    OkCancelDialog okCancelDialog = OkCancelDialog.showInstance(
				    (AppCompatActivity)getActivity(),
				    "Sorry, unable to backup or restore.<br><br>"
				    + "There's no SD Card or the app can't access the SD Card.",
		            "",
				    "",
				    "Ok",
				    OkCancelDialog.CancelButton.LEFT,
				    OkCancelDialog.EWI.ERROR
			);
		    okCancelDialog.setOkCancel(
				    new OkCancel() {
					    @Override
					    protected void ok(DialogInterface dialog) {
						    getActivity().finish();
					    }
					    @Override
					    protected void cancel(DialogInterface dialog) {
						    getActivity().finish();
					    }
				    }
		    );
	    }

	    setAdapter();

	    return v;
    }

	private Runnable getShowSelectedRunnable() {
		return new Runnable() {
			@Override
			public void run() {
				showSelected();
			}
		};
	}

	private void setAdapter() {
		mAdapter = new RestoreListAdapter(
				getActivity(), R.layout.back_up_restore_textview, mBackupFiles);
		mRestoreList.setAdapter(mAdapter);
		showEmptyList();
	}

	private void showEmptyList() {
		if (mBackupFiles.size() <= 0) {
			mListEmpty.setVisibility(TextView.VISIBLE);
			mListEmpty.setText("There are no backups");
		}
		else {
			mListEmpty.setVisibility(TextView.GONE);
		}
	}

	class RestoreListAdapter extends ArrayAdapter<File> {
		int mResource;
		ArrayList<File> mList;

		public RestoreListAdapter(Context context, int resource, ArrayList<File> list) {
			super(context, resource, list);
			mResource = resource;
			mList = list;
			enableRestoreActions(false);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;

			if (convertView == null) {
				convertView = ((LayoutInflater) getActivity()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
						.inflate(mResource, null);

				viewHolder = new ViewHolder();
				setViewHolder(viewHolder, convertView);
				setRestoreItemListeners(viewHolder);
				convertView.setTag(viewHolder);
			}
			else {
				viewHolder = (ViewHolder)convertView.getTag();
			}

			File file = mList.get(position);
			viewHolder.mFile.setTag(file);
			viewHolder.mFile.setText(file.getName());

//			boolean selected = file == mSelected;
//			select(convertView, viewHolder.mFile, selected/*select*/);

			return convertView;
		}

		@Override
		public void notifyDataSetChanged() {
			startProgressBar();
			super.notifyDataSetChanged();
			mList.clear();
			ArrayList<File> list = BackupRestore.getBackupFiles();
			if (list != null) {
				mList.addAll(list);
			}
			showEmptyList();
			stopProgressBar();
		}
	}

	class BackupRestoreListener implements BackupRestore.IBackupRestoreListener {
		@Override
		public void onBackupComplete(final String name) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mAdapter.notifyDataSetChanged();
					OkCancelDialog okCancelDialog = OkCancelDialog.showInstance(
							(AppCompatActivity)getActivity(),
							"Success! Your backup has completed.<br><br>"
									+ "The backup file is located on the SD Card:<br><br>"
									+ "'" + name + "'",
							"",
							"",
							"Ok",
							OkCancelDialog.CancelButton.LEFT,
							OkCancelDialog.EWI.INFO
					);
					okCancelDialog.setOkCancel(new OkCancel());
				}
			});
		}

		@Override
		public void onBackupError(final int error) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					int fce = error & BackupRestore.BACKUP_FILE_COPY_EXCEPTION;
					if (fce != 0) {
						OkCancelDialog okCancelDialog = OkCancelDialog.showInstance(
								(AppCompatActivity) getActivity(),
								"ERROR: Unfortunately, your backup failed, the app was"
										+ " unable to copy data to the SD Card.<br><br>"
										+ " The backup was not created.",
								"",
								"",
								"Ok",
								OkCancelDialog.CancelButton.LEFT,
								OkCancelDialog.EWI.ERROR
						);
						okCancelDialog.setOkCancel(new OkCancel());
					}
				}
			});
		}

		@Override
		public void onRestoreComplete() {
			OkCancelDialog okCancelDialog = OkCancelDialog.showInstance(
					(AppCompatActivity) getActivity(),
					"That was quick!<br><br>"
							+ "App data has been restored and is"
							+ " ready for use.",
					null,
					"",
					"Ok",
					OkCancelDialog.CancelButton.LEFT,
					OkCancelDialog.EWI.INFO
			);
			okCancelDialog.setOkCancel(new OkCancel());
		}

		@Override
		public void onRestoreError(int error, String backupDbPath) {
			int nad = error & BackupRestore.RESTORE_NOT_A_DATABASE;
			if (nad != 0) {
				showInvalidDb(backupDbPath);
			}
			int fce = error & BackupRestore.RESTORE_FILE_COPY_EXCEPTION;
			if (fce != 0) {
				showBackupFileCopyException(backupDbPath);
			}
		}
	}

	private void showInvalidDb(String path) {
		OkCancelDialog okCancelDialog = OkCancelDialog.showInstance(
				(AppCompatActivity)getActivity(),
				"ERROR: This backup file doesn't contain app data.<br>"
						+ "Restore did not complete.<br><br>"
						+ "File: '" + path + "'",
				"",
				"",
				"Ok",
				OkCancelDialog.CancelButton.LEFT,
				OkCancelDialog.EWI.ERROR
		);
		okCancelDialog.setOkCancel(new OkCancel());
	}

	private void showBackupFileCopyException(String path) {
		OkCancelDialog okCancelDialog = OkCancelDialog.showInstance(
				(AppCompatActivity)getActivity(),
				"ERROR: Unable to create app data from the backup file.<br>"
						+ "Restore did not complete.<br><br>"
						+ "File: '" + path + "'",
				"",
				"",
				"Ok",
				OkCancelDialog.CancelButton.LEFT,
				OkCancelDialog.EWI.ERROR
		);
		okCancelDialog.setOkCancel(new OkCancel());
	}

	class RestoreItemOnClickListener implements  TextView.OnClickListener {
		@Override
		public void onClick(View v) {
			OkCancelDialog okCancelDialog = OkCancelDialog.showInstance(
					(AppCompatActivity)getActivity(),
					" WARNING: All current data will be"
							+ " <font color='red'>overwritten</font>.<br><br>"
							+ "Are you sure you want to restore?<br><br>"
							+ "If you aren't sure, <font color='red'>cancel"
							+ " now and do a BACKUP</font> first!",
					"",
					"Cancel",
					"Ok,<br>Restore",
					OkCancelDialog.CancelButton.LEFT,
					OkCancelDialog.EWI.WARNING
			);
			okCancelDialog.setOkCancel(
					new OkCancel() {
						@Override
						protected void ok(DialogInterface dialog) {
							mBackupRestore.restoreDb(mSelected);
						}
						@Override
						protected void cancel(DialogInterface dialog) {
						}
					}
			);
		}
	}

	class BackupOnClickListener implements TextView.OnClickListener {
		@Override
		public void onClick(View v) {
			mBackupRestore.backupDb();
		}
	}

	class DeleteItemOnClickListener implements ImageView.OnClickListener {
		@Override
		public void onClick(View v) {
			OkCancelDialog okCancelDialog = OkCancelDialog.showInstance(
					(AppCompatActivity)getActivity(),
					" WARNING: You are about to permanently delete a backup.<br><br>"
							+ "Are you sure?",
					"",
					"Cancel",
					"Ok,<br>Delete",
					OkCancelDialog.CancelButton.LEFT,
					OkCancelDialog.EWI.WARNING
			);
			okCancelDialog.setOkCancel(
					new OkCancel() {
						@Override
						protected void ok(DialogInterface dialog) {
							if (mSelected != null) {
								mSelected.delete();
								getNextSelection();
								mAdapter.notifyDataSetChanged();
							}
						}
						@Override
						protected void cancel(DialogInterface dialog) {
						}
					}
			);
		}

		private void getNextSelection() {
			if (mSelected == null) {
				return;
			}

			// find mSelected in mBackupFiles
			int idx = mBackupFiles.indexOf(mSelected);
			mSelected = null;
			if (idx != -1) {
				// remove from mBackupFiles. Idx now points
				// to next (need to mod first tho)
				mBackupFiles.remove(idx);
				// get next
				int num = mBackupFiles.size();
				if (num == 0) {
					return;
				}
				if (idx >= num) {
					idx = num - 1;
				}
				mSelected = mBackupFiles.get(idx);
			}
		}

	}

	class SelectItemOnClickListener implements TextView.OnClickListener {
		@Override
		public void onClick(View v) {
			ViewHolder vh = (ViewHolder) v.getTag();
			File file = (vh == null) ? null : (File) vh.mFile.getTag();
			if (file == null) {
				return;
			}

			if (mSelected == file) {
				mSelected = null;
			} else {
				mSelected = file;
			}

			enableRestoreActions(mSelected != null);
			showSelected();
		}
	}

	private void showSelected() {
		int N=mRestoreList.getChildCount();
		for (int i=0;i<N;i++) {
			LinearLayout ll = (LinearLayout)mRestoreList.getChildAt(i);
			ViewHolder vh = (ViewHolder)ll.getTag();
			if (vh != null) {
				File file = (File)vh.mFile.getTag();
				boolean selected = (file.equals(mSelected));
				select(ll, vh.mFile, selected);
			}
		}
	}

	private void select(View v, TextView tv, boolean select) {
		LinearLayout ll = (LinearLayout)v;
		if (select) {
			ll.setBackgroundColor(Color.parseColor("#c2c2c2"));
			tv.setTextColor(Color.WHITE);
		}
		else {
			ll.setBackgroundColor(Color.WHITE);
			tv.setTextColor(Color.BLACK);
		}
	}

	private void enableRestoreActions(boolean enable) {
		mRestore.setEnabled(enable);
		mDelete.setEnabled(enable);
		if (enable) {
			mRestore.setAlpha(1F);
			mDelete.setAlpha(1F);
		}
		else {
			mRestore.setAlpha(0.3F);
			mDelete.setAlpha(0.3F);
		}
	}

	private void setViewComponents(View v) {
		setViewPointers(v);
		mRestoreList.setRunnable(getShowSelectedRunnable());
		setListeners();
	}

	private void setViewPointers(View v) {
		mRestoreList = (RestoreListView) v.findViewById(R.id.burf_restore_list);
		mRestore = (TextView) v.findViewById(R.id.burf_restore);
		mBackup = (TextView) v.findViewById(R.id.burf_backup);
		mDelete = (ImageView) v.findViewById(R.id.burf_delete_restore_file);
		mListEmpty = (TextView) v.findViewById(R.id.burf_list_empty);
	}

	public static class ViewHolder {
		LinearLayout mTopView;
		TextView mFile;
	}

	private void setViewHolder(ViewHolder vh, View v) {
		vh.mTopView = (LinearLayout) v.findViewById(R.id.burt_top_view);
		vh.mFile = (TextView)v.findViewById(R.id.burt_file);
	}

	private void setListeners() {
		mBackupRestoreListeners = new BackupRestoreListener();
		mRestoreItemOnClickListener = new RestoreItemOnClickListener();
		mBackupOnClickListener = new BackupOnClickListener();
		mDeleteItemOnClickListener = new DeleteItemOnClickListener();
		mSelectItemOnClickListener = new SelectItemOnClickListener();

		mBackup.setOnClickListener(mBackupOnClickListener);
		mRestore.setOnClickListener(mRestoreItemOnClickListener);
		mDelete.setOnClickListener(mDeleteItemOnClickListener);
	}

	private void setRestoreItemListeners(ViewHolder vh) {
		vh.mTopView.setOnClickListener(mSelectItemOnClickListener);
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (IBackUpRestoreFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement IBackUpRestoreFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface IBackUpRestoreFragmentListener {
    }
}
