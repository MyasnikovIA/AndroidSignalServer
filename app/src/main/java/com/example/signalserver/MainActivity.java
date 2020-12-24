package com.example.signalserver;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.signalserver.WebServer.HttpSrv;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView text = (TextView) findViewById(R.id.textView);

        // Разблокировать экран
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
            KeyguardManager.KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
            lock.disableKeyguard();
        // ---------------------
        boolean onConnect = false;
        String ipAddress = "";
        WifiConfiguration wifiConfig = new WifiConfiguration();
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        wifiConfig.SSID = String.format("\"%s\"", "ELTEX-87A2"); // Имя WIFI точки доступа
        wifiConfig.preSharedKey = String.format("\"%s\"", "XXXXXXXXX"); // Пароль для полдключения к точки доступа
        wifiManager.disconnect();
        int netId = wifiManager.addNetwork(wifiConfig);
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
        while (onConnect == false) {
            // Подключение к WIFI точке деоступа
            // wifiConfig.SSID = String.format("\"%s\"", "a616mm");
            // ------------------------------------------
            WifiInfo info = wifiManager.getConnectionInfo();
            int ip = info.getIpAddress();
            if (ip != 0) {
                ipAddress = Formatter.formatIpAddress(ip);
                text.setText(ipAddress);
                text.append("\nSSID WIFI: ");
                text.append(info.getSSID());
                text.append("\nMAC DEVICE: ");
                text.append(info.getMacAddress());
                onConnect = true;
            } else {
                text.append("\n.");
            }
            pause(3000);
        }


        HttpSrv web =new HttpSrv(getApplicationContext());
        web.Start("8266");
    }
    public static void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            System.err.format("IOException: %s%n", e);
        }
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
