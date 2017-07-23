package com.example.s162132.childapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.AppLaunchChecker;
import android.view.View;

public class FirstOnly extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_only);

        //初回起動のみ
        if(!AppLaunchChecker.hasStartedFromLauncher(this)) {
            System.out.println("called");
            Intent intent0 = new Intent(FirstOnly.this, MainActivity.class);
            startActivity(intent0);
        }

        findViewById(R.id.NextButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent2 = new Intent(FirstOnly.this,FirstOnly2.class);
                startActivity(intent2);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
