package com.example.signalserver;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.signalserver.WebServer.HttpSrv;

import java.util.List;

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

        // Разблокировать экран
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
        lock.disableKeyguard();
        // ---------------------


        // Подключение к WIFI точке деоступа
            WifiConfiguration wifiConfig = new WifiConfiguration();
            // wifiConfig.SSID = String.format("\"%s\"", "a616mm");
            wifiConfig.SSID = String.format("\"%s\"", "ELTEX-87A2"); // Имя WIFI точки доступа
            wifiConfig.preSharedKey = String.format("\"%s\"", "XXXXXXXXXX"); // Пароль для полдключения к точки доступа
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            int netId = wifiManager.addNetwork(wifiConfig);
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();
        // ------------------------------------------


        WifiInfo info = wifiManager.getConnectionInfo ();
        text.append("\nSSID WIFI: ");
        text.append(info.getSSID());
        text.append("\nMAC DEVICE: ");
        text.append(info.getMacAddress());

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
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + ipAddress + ":8266"));
        startActivity(browserIntent);
    }

}
