package com.example.signalserver.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.signalserver.MainActivity;

public class OnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            //  запуск сервиса
            context.startService(new Intent(context, ServiceExample.class));
            //  Intent i = new Intent(context, MainActivity.class);
            //  i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //  context.startActivity(i);
            //  Toast.makeText(context, "Boot Completed", Toast.LENGTH_SHORT).show();
        }
    }


}
