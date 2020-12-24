package com.example.webserver;

import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.webserver.WebServer.HttpSrv;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WifiManager wifiMgr1 = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo1 = wifiMgr1.getConnectionInfo();
        int ip = wifiInfo1.getIpAddress();
        String ipAddress = Formatter.formatIpAddress(ip);
        TextView text = (TextView) findViewById(R.id.textView);
        text.setText(ipAddress);
        HttpSrv web =new HttpSrv(getApplicationContext());
        web.Start("8266");
    }

    public void onClick(View v) {
        WifiManager wifiMgr1 = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo1 = wifiMgr1.getConnectionInfo();
        int ip = wifiInfo1.getIpAddress();
        // ------------------------------------------------
        String ipAddress = Formatter.formatIpAddress(ip);
        TextView text = (TextView) findViewById(R.id.textView);
        text.setText(ipAddress);
        // ------------------------------------------------
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + ipAddress + ":9090"));
        startActivity(browserIntent);
    }

}