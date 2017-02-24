package com.aldersonet.automatonalert.OurDir;

import android.app.Activity;
import android.os.Environment;

import com.aldersonet.automatonalert.AutomatonAlert;

import java.io.File;

public class OurDir {
	public static final String BACKUP_DATABASE_NAME = "contactringtones.db";

	private static File getOldDirectory() {
		return new File(Environment.getExternalStorageDirectory()
				+ AutomatonAlert.mOurOldDirectoryName);
	}

	private static File getOurDirectory() {
		return new File(Environment.getExternalStorageDirectory()
				+ AutomatonAlert.mOurDirectoryName);
	}

	private static boolean haveOldDirectory() {
		return getOldDirectory().exists();
	}

	private static boolean haveOurDirectory() {
		return getOurDirectory().exists();
	}

	public static boolean haveBackupRestoreCapability(Activity activity) {
		return canBackup(activity)
				&& canRestore(activity);

	}

	private static File getData(Activity activity) {
		// programmatically get database path
		String path = Environment.getDataDirectory().getPath();
		path += ("/data/"+activity.getPackageName()+"/databases/");
//						+ AutomatonAlertProvider.DATABASE_NAME);

		return new File(path);
	}

	private static File getSd() {
		String path = OurDir.getOurSdPath();
		if (path != null) {
			return new File(OurDir.getOurSdPath());
		}

		return null;
	}

	public static boolean sdOk() {
		return getSd() != null;
	}

	public static boolean dataOk(Activity activity) {
		return getData(activity) != null;
	}

	public static boolean backupRestoreOk(Activity activity) {
		return dataOk(activity) && sdOk();
	}

	public static boolean canBackup(Activity activity) {
		if (!backupRestoreOk(activity)) {
			return false;
		}
		return getSd().canWrite()
				&& getData(activity).canRead();

	}

	public static boolean canRestore(Activity activity) {
		if (!backupRestoreOk(activity)) {
			return false;
		}

		return getData(activity).canWrite()
				&& getSd().canRead();

	}

	private static boolean makeSureWeHaveAnOurDirectory() {
		// OurDir directory already exists
		if (haveOurDirectory()) {
			return true;
		}

		// if old directory exists, need to rename
		File oldDir = getOldDirectory();
		if (oldDir.exists()) {
			boolean isRenamed =
					oldDir.renameTo(new File(Environment.getExternalStorageDirectory()
							+ AutomatonAlert.mOurDirectoryName));
			if (isRenamed) {
				return true;
			}
		}

		File ourDir = getOurDirectory();
		if (!ourDir.exists()) {
			// return success or utter failure
			return ourDir.mkdir();
		}

		/*ourDir.exists()*/
		return true;
	}

	public static String getOurSdPath() {
		// makeSureWeHaveAnOurDirectory() will create/rename as necessary
		if (!makeSureWeHaveAnOurDirectory()) {
			return null;
		}

		return
				Environment.getExternalStorageDirectory()
						+ AutomatonAlert.mOurDirectoryName;
	}
}
