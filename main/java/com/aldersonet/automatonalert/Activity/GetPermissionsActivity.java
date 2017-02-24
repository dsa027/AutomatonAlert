package com.aldersonet.automatonalert.Activity;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AlertDialog;
import android.view.Window;

import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.R;

import java.util.List;

public class GetPermissionsActivity
        extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    public static final int PERMISSIONS_REQ = 1001;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.get_permissions_activity);
        //davedel
//        AutomatonAlert.populateAppData();
        //davedel
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                    @NonNull int[] grantResults) {

        boolean ok = true;

        if (requestCode == PERMISSIONS_REQ) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    if (permissions[i].equals(Manifest.permission.READ_CONTACTS) ||
                            permissions[i].equals(Manifest.permission.READ_SMS)) {
                        ok = false;
                        acceptAppCrash();
                        break;
                    }
                }
            }
        }
        if (ok) {
            startMainAppActivity(this);
        }
    }

    public static void startMainAppActivity(Activity activity) {
        activity.finish();
        AutomatonAlert.THIS.initializeApp();
        Intent intent = new Intent(activity, ContactFreeFormListActivity.class);
        activity.startActivity(intent);
    }

    private void acceptAppCrash() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(
                getString(R.string.need_permissions) +
                        "\n\n\nSorry, gotta crash now. Start the app to retry."
                );
        builder.setCancelable(true);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setResult(PERMISSIONS_REQ);
                finish();
                Object o = null;
                o.toString();
            }
        });
        builder.create().show();
    }



    private void getPermissions() {
        List<String> list =
                AutomatonAlert.THIS.checkPermissions(
                        AutomatonAlert.CRITICAL_PERMISSIONS);
        if (list.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    list.toArray(new String[list.size()]),
                    PERMISSIONS_REQ);
        }
        else {
            startMainAppActivity(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        getPermissions();
    }

    @Override
	protected void onPause() {
		super.onPause();
	}

}
