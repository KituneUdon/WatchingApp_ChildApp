package com.example.s162132.childapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;

public class WiFiSettingActivity extends AppCompatActivity {

    private SharedPreferences pref3;
    private CheckBox checkBox;
    private WifiManager wifi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_setting);

        wifi = (WifiManager) getSystemService(WIFI_SERVICE);

        pref3 = getSharedPreferences("SkyWayId", Context.MODE_PRIVATE);

        WifiSettingFragment wifiSettingFragment = new WifiSettingFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.container, wifiSettingFragment);
        transaction.commit();

        findViewById(R.id.NextButton4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}
