package com.example.signalserver.services;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.text.format.Formatter;
import android.widget.Toast;

import com.example.signalserver.WebServer.HttpSrv;

public class ServiceExample extends Service {
    private HttpSrv web ;
    @Override
    public void onCreate() {
        super.onCreate();

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
        lock.disableKeyguard();


        boolean onConnect = false;
        String ipAddress = "";
         /*
        // Подключение к WIFI точке деоступа
        WifiConfiguration wifiConfig = new WifiConfiguration();
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        // wifiConfig.SSID = String.format("\"%s\"", "a616mm");
        wifiConfig.SSID = String.format("\"%s\"", "ELTEX-87A2"); // Имя WIFI точки доступа
        wifiConfig.preSharedKey = String.format("\"%s\"", "XXXXXXXXXX"); // Пароль для полдключения к точки доступа

        wifiManager.disconnect();
        int netId = wifiManager.addNetwork(wifiConfig);
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
        // ------------------------------------------
        while (onConnect == false) {
            WifiInfo info = wifiManager.getConnectionInfo();
            int ip = info.getIpAddress();
            if (ip != 0) {
                ipAddress = Formatter.formatIpAddress(ip);
                Toast.makeText(getApplicationContext(), "IP "+ipAddress, Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), "SSID "+info.getSSID(), Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), "MAC "+info.getMacAddress(), Toast.LENGTH_SHORT).show();
                onConnect = true;
            }
            pause(3000); // пауза 3 секунды
        }
        */
        web = new HttpSrv(getApplicationContext());
        web.Start("8266");
        Toast.makeText(getApplicationContext(), "Boot Signal Server", Toast.LENGTH_SHORT).show();
    }


    public static void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            System.err.format("IOException: %s%n", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        web.Stop();
    }
}
