package com.example.onehandcontroller;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Switch serviceSwitch;
    private Switch padSwitch;
    private Switch rightSwipeSwitch;
    private Switch leftSwipeSwitch;
    private ComponentName runningService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setStatusBar();

        serviceSwitch = findViewById(R.id.serviceSwitch);
        padSwitch = findViewById(R.id.padSwitch);
        rightSwipeSwitch = findViewById(R.id.rightSwipeSwitch);
        leftSwipeSwitch = findViewById(R.id.leftSwipeSwitch);

        if(!isAccessibilityEnable()) {
            setAccessibilityPermission();
        }

        if(isServiceRunning(OneHandService.class)) {
            serviceSwitch.setChecked(true);
        }

        serviceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked) {
                    if(!isAccessibilityEnable()) {
                        setAccessibilityPermission();
                        serviceSwitch.setChecked(false);
                    }
                    else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                        if(Settings.canDrawOverlays(getApplicationContext())) {
                            runningService = startService(new Intent(getApplicationContext(), OneHandService.class));
                        }
                        else {
                            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getApplicationContext().getPackageName())));
                        }
                    }
                }
                else {
                    if(runningService != null) {

                    }
                }
            }
        });
    }

    private void setStatusBar() {
        View view = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (view != null) {
                view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                getWindow().setStatusBarColor(Color.parseColor("#f2f2f2"));
            }
        }
    }

    private boolean isAccessibilityEnable() {
        String prefString = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return prefString != null && prefString.contains(getPackageName() + "/" + OneHandService.class.getName());
    }

    private void setAccessibilityPermission() {
        AlertDialog.Builder gsDialog = new AlertDialog.Builder(this);
        gsDialog.setTitle("접근성 권한 설정");
        gsDialog.setMessage("접근성 권한을 필요로 합니다");
        gsDialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                return;
            }
        }).create().show();
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                runningService = service.service;
                return true;
            }
        }
        return false;
    }
}