package com.aldersonet.automatonalert.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.aldersonet.automatonalert.AutomatonAlert;

import java.util.List;

public class InitialDeciderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // just leave if permissions ok
        if (!isNeedPermissions()) {
            GetPermissionsActivity.startMainAppActivity(this);
        }
        else {
            getPermissionsActivity();
        }
    }

    private boolean isNeedPermissions() {
        // ask for all permissions
        List<String> list =
                AutomatonAlert.THIS.checkPermissions(
                        AutomatonAlert.CRITICAL_PERMISSIONS);
        return list.size() > 0;
    }

    private void getPermissionsActivity() {
        finish();
        Intent intent = new Intent(this, GetPermissionsActivity.class);
        startActivity(intent);
    }
}
