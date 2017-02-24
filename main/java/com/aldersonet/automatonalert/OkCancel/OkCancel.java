package com.aldersonet.automatonalert.OkCancel;

import android.content.DialogInterface;
import com.aldersonet.automatonalert.OkCancel.OkCancelDialog.UserActionOnChecked;

public class OkCancel {

	public static final boolean mLeftEqualsCancelHARDCODED = true;

	public UserActionOnChecked mLastButtonPressed;

	protected void ok(DialogInterface dialog) {
	}

	protected void cancel(DialogInterface dialog) {
	}

	public void doUserActionOkOrCancel(UserActionOnChecked userAction) {
		// LEFT
		switch(userAction) {
			case LEFT:
				doLeftButtonPressed();
				break;
			case RIGHT:
				doRightButtonPressed();
				break;
			case NEITHER:
				break;
			case DONT_SKIP:
				break;
		}
	}

	public void doLeftButtonPressed(DialogInterface dialog) {
		mLastButtonPressed = UserActionOnChecked.LEFT;
		if (mLeftEqualsCancelHARDCODED) {
			// LEFT/CANCEL
			cancel(dialog);
		}
		else {
			// LEFT/OK
			ok(dialog);
		}
	}
	public void doLeftButtonPressed() {
		doLeftButtonPressed(null);
	}

	public void doRightButtonPressed(DialogInterface dialog) {
		mLastButtonPressed = UserActionOnChecked.RIGHT;
		if (mLeftEqualsCancelHARDCODED) {
			// RIGHT/OK
			ok(dialog);
		}
		else {
			// RIGHT/CANCEL
			cancel(dialog);
		}
	}
	public void doRightButtonPressed() {
		doRightButtonPressed(null);
	}
}
