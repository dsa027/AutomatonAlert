package com.aldersonet.automatonalert.BackupRestore;

import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.Log;

import com.aldersonet.automatonalert.ActionBar.ActionBarDrawer;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.Alert.NotificationItemDO;
import com.aldersonet.automatonalert.Alert.NotificationItems;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.OkCancel.OkCancel;
import com.aldersonet.automatonalert.OkCancel.OkCancelDialog;
import com.aldersonet.automatonalert.OurDir.OurDir;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import com.aldersonet.automatonalert.SourceType.SourceTypeDO;
import com.aldersonet.automatonalert.Util.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class BackupRestore {
	public static final String TAG = "BackupRestore";

	public static final int RESTORE_FILE_COPY_EXCEPTION = 1;
	public static final int RESTORE_NOT_A_DATABASE      = 2;
	public static final int BACKUP_FILE_COPY_EXCEPTION  = 4;

	static File mApplicationDbFile;
	AutomatonAlertProvider mProvider;
	Context mContext;
	IBackupRestoreListener mListener;

	public BackupRestore(Context context, IBackupRestoreListener listener) {
		mContext = context;
		mApplicationDbFile = getDatabaseFile();
		mProvider = getAutomatonAlertProvider();
		mListener = listener;
	}

	public void backupDb() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				// EXPORT Database
				final String name = exportDb();

				// CALLBACK
				if (name != null) {
					onBackupComplete(name);
				}
				else {
					onBackupError(BACKUP_FILE_COPY_EXCEPTION);
				}
			}
		}).start();
	}

	public void restoreDb(final File backupFile) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				// RESTORE
				restore(backupFile);
			}
		}).start();
	}

	private File getDatabaseFile() {
		return mContext.getDatabasePath(AutomatonAlertProvider.DATABASE_NAME);
	}

	private AutomatonAlertProvider getAutomatonAlertProvider() {
		ContentProviderClient cpc = AutomatonAlert.getProvider();
		if (cpc != null) {
			return (AutomatonAlertProvider) cpc.getLocalContentProvider();
		}

		return null;
	}

	private static String exportDb() {
		Calendar cal = Calendar.getInstance(Locale.getDefault());
		String backupDBPath =
				OurDir.getOurSdPath()
						+ OurDir.BACKUP_DATABASE_NAME
						+ DateFormat.format(".yyMMdd_kkmmss", cal);

		try {
			File sd = Environment.getExternalStorageDirectory();
			if (sd.canWrite()) {
				File currentDB = mApplicationDbFile;
				File backupDB = new File(backupDBPath);

				if (currentDB.exists()) {
					FileChannel src = new FileInputStream(currentDB).getChannel();
					FileChannel dst = new FileOutputStream(backupDB).getChannel();
					dst.transferFrom(src, 0, src.size());
					src.close();
					dst.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return backupDBPath;
	}

	private boolean validateDb(String path, AutomatonAlertProvider provider) {
		AutomatonAlertProvider.AutomatonAlertDBOpenHelper fromHelper =
				new AutomatonAlertProvider.AutomatonAlertDBOpenHelper(
						mContext, path, true/*testingValidity*/);

		boolean isOk = false;

		Cursor cursor = null;
		try {
			cursor = provider.query(
					fromHelper,
					AutomatonAlertProvider.CONTACT_RT_PREFS_TABLE_URI,
					null, null, null, null);
			if (cursor.moveToFirst()) {
				isOk = true;
			}
		} catch (Exception e) {
			//
		}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		fromHelper.close();

		return isOk;
	}

	public static ArrayList<File> getBackupFiles() {
		// get files from our directory
		ArrayList<File> validFiles = new ArrayList<File>();
		if (OurDir.getOurSdPath() == null) {
			return null;
		}
		File dir = new File(OurDir.getOurSdPath());
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isFile()
					&& file.getName().startsWith(OurDir.BACKUP_DATABASE_NAME)) {
				validFiles.add(file);
			}
		}

		if (validFiles.size() > 0) {
			Collections.sort(validFiles, new Comparator<File>() {
				@Override
				public int compare(File lhs, File rhs) {
					if (lhs == null) {
						if (rhs == null) {
							return 0;
						}
						return -1;
					}
					if (rhs == null) {
						return 1;
					}

					return lhs.getName().compareTo(rhs.getName());
				}
			});
		}

		return validFiles;
	}

	public void onRestoreComplete() {
		Log.d(TAG + ".onRestoreComplete()", "");
		if (mListener != null) {
			mListener.onRestoreComplete();
		}
	}

	public void onRestoreError(int error, String backupDbPath) {
		Log.d(TAG + ".onRestoreComplete()", "");
		if (mListener != null) {
			mListener.onRestoreError(error, backupDbPath);
		}
	}

	public void onBackupComplete(String name) {
		Log.d(TAG + ".onBackupComplete()", "");
		if (mListener != null) {
			mListener.onBackupComplete(name);
		}
	}

	public void onBackupError(int error) {
		Log.d(TAG + ".onRestoreComplete()", "");
		if (mListener != null) {
			mListener.onBackupError(error);
		}
	}

	private void restore(File restoreFile) {
		int error = 0;

		if (restoreFile == null) {
			error |= RESTORE_NOT_A_DATABASE;
			// CALLBACK
			onRestoreError(error, "<<null>>");
			return;
		}
		final String backupDbPath = restoreFile.getAbsolutePath();

		// close current database
		mProvider.mDBOpenHelper.close();

		// FILESYSTEM FILE COPY of database
		try {
			File sd = Environment.getExternalStorageDirectory();
			if (sd.canWrite()) {
				if (!validateDb(backupDbPath, mProvider)) {
					error |= RESTORE_NOT_A_DATABASE;
				}
				else {
					File backupDb = new File(backupDbPath);
					File currentDb = mApplicationDbFile;

					if (backupDb.exists()) {
						FileChannel src = new FileInputStream(backupDb).getChannel();
						FileChannel dst = new FileOutputStream(currentDb).getChannel();
						dst.transferFrom(src, 0, src.size());
						src.close();
						dst.close();
					}
				}
			}
		} catch (Exception e) {
			error |= RESTORE_FILE_COPY_EXCEPTION;
			e.printStackTrace();
		}

		// open newly restored app database
		mProvider.resetDbOpenHelper();

		if (error == 0) {
			// UPGRADE DB if needed
			int fromVer = mProvider.mDBOpenHelper.getDataBaseVersion();
			int toVer = AutomatonAlertProvider.DATABASE_VERSION;
			if (fromVer < toVer) {
				SQLiteDatabase fromDatabase = mProvider.mDBOpenHelper.getWritableDatabase();
				mProvider.mDBOpenHelper.onUpgrade(fromDatabase, fromVer, toVer);
			}
			// make sure in-memory data is refreshed
            //davedel
//			AutomatonAlert.populateAppData();
            //davedel
			// make sure any data that's maintained externally
			// is updated from our app
			updateExternalData();
			// CALLBACK
			onRestoreComplete();
		}
		else {
			// CALLBACK
			onRestoreError(error, backupDbPath);
		}
	}

	private void updateExternalData() {
		// SOURCETYPE -> CONTACTS
		ArrayList<SourceTypeDO> sources = SourceTypeDO.get(FragmentTypeRT.PHONE);
		for (SourceTypeDO source : sources) {
			int id = source.getNotificationItemId();
			if (id != -1) {
				NotificationItemDO notificationItem = NotificationItems.get(id);
				if (notificationItem != null) {
					Utils.updatePhoneRTVM(
							mContext,
							source.getLookupKey(),
							notificationItem.getSoundPath(),
							notificationItem.getSilentMode());
				}
			}
		}
		// CONTACTS -> SOURCETYPE
		Utils.makeSureAllPhoneRTVMHaveSourceType(mContext);
	}

	public static void showNoBackupRestoreCapabilityWarning(
			final Activity activity, final ActionBarDrawer.Rec rec) {

        // check for access problem
		String s = "";
		boolean canBackup = OurDir.canBackup(activity);
		boolean canRestore = OurDir.canRestore(activity);
		if (!canBackup) {
			s = "Backup";
		}
		if (!canRestore) {
			if (s.equals("")) {
				s = "Restore";
			}
			else {
				s += "/Restore";
			}
		}
		OkCancelDialog okCancelDialog = OkCancelDialog.showInstance(
				(AppCompatActivity) activity,
                "WARNING: This device is reporting that it can't " + s + "."
                        + (!canRestore ?
                        " If you press Try Anyway now and then Restore,"
                                + " you may lose data."
                        : ""),
				"",
				"Cancel",
				"Try<br>Anyway",
				OkCancelDialog.CancelButton.LEFT,
				OkCancelDialog.EWI.INFO
		);
		okCancelDialog.setOkCancel(
				new OkCancel() {
					@Override
					protected  void ok(DialogInterface dialog) {
                        // we'll make them try again instead of going there
						Intent intent =
								ActionBarDrawer.getDrawerIntent(
										activity, rec.mClz, rec.mArg);
						if (intent != null) {
							activity.startActivity(intent);
						}
					}
					@Override
					protected  void cancel(DialogInterface dialog) {
					}
				}
		);
	}

	public interface IBackupRestoreListener {
		void onBackupComplete(String name);
		void onBackupError(int error);
		void onRestoreComplete();
		void onRestoreError(int error, String backupDbPath);
	}
}
