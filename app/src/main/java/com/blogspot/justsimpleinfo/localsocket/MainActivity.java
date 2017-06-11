package com.blogspot.justsimpleinfo.localsocket;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    final static String WIFI_AND_POWER_LOCK_TAG = "MyLock";
    Button mServerBtn;
    Button mClientButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mServerBtn = (Button) this.findViewById(R.id.server_btn);
        mServerBtn.setOnClickListener(this);

        mClientButton = (Button) this.findViewById(R.id.client_btn);
        mClientButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

        int id = view.getId();
        switch (id){
            case R.id.client_btn:
                Intent clientIntent = new Intent(MainActivity.this,ClientActivity.class);
                startActivity(clientIntent);
                break;

            case R.id.server_btn:


                Intent serverIntent = new Intent(MainActivity.this,ServerActivity.class);
                startActivity(serverIntent);

                break;

            default:

        }

    }
}
