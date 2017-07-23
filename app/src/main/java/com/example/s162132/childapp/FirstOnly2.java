package com.example.s162132.childapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.AppLaunchChecker;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

public class FirstOnly2 extends AppCompatActivity {

    private SharedPreferences pref3;
    private CheckBox checkBox;
    private WifiManager wifi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_only2);

        //初回起動のみ
        if(AppLaunchChecker.hasStartedFromLauncher(this)) {
            Intent intent0 = new Intent(FirstOnly2.this, MainActivity.class);
            startActivity(intent0);
        } else {
            WifiSettingFragment wifiSettingFragment = new WifiSettingFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.container2, wifiSettingFragment);
            transaction.commit();
        }

        wifi = (WifiManager) getSystemService(WIFI_SERVICE);

        pref3 = getSharedPreferences("SkyWayId", Context.MODE_PRIVATE);

        findViewById(R.id.NextButton2).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        EditText text = (EditText) findViewById(R.id.Number);

                        String s = text.getText().toString();
                        SharedPreferences.Editor e = pref3.edit();
                        e.putString("Key", s);
                        e.commit();

                        Intent intent3 = new Intent(FirstOnly2.this, MainActivity.class);
                        startActivity(intent3);
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
