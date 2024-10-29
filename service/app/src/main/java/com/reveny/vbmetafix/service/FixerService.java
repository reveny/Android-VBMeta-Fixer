package com.reveny.vbmetafix.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.reveny.vbmetafix.service.keyattestation.Entry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FixerService extends Service {
    private static final String TAG = "FixerService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service Created");
    }

    public void writeBootHashToFile(Context context, String bootHash) {
        @SuppressLint("SdCardPath") File file = new File("/data/data/com.reveny.vbmetafix.service/cache/boot.hash");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bootHash.getBytes());
            Log.d(TAG, "Boot hash written to " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Failed to write boot hash", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service Started");

        String bootHash = Entry.run();
        Log.e(TAG, "Boot hash: " + bootHash);

        writeBootHashToFile(this, bootHash);

        stopForeground(true);
        stopSelf();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service Destroyed");
    }
}
