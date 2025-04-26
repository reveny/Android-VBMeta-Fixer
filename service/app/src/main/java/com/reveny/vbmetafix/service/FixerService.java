package com.reveny.vbmetafix.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import com.reveny.vbmetafix.service.keyattestation.Entry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FixerService extends Service {
    private static final String TAG = "FixerService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "fixer_service_channel";
    private HandlerThread handlerThread;
    private Handler handler;

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service Created");
        
        createNotificationChannel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }

        handlerThread = new HandlerThread("FixerServiceThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Fixer Service Channel",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Used for running VBMeta fixing operations");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("VBMeta Service")
                .setContentText("Processing boot hash...")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .build();
        } else {
            return new Notification.Builder(this)
                .setContentTitle("VBMeta Service")
                .setContentText("Processing boot hash...")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setPriority(Notification.PRIORITY_LOW)
                .build();
        }
    }

    public void writeBootHashToFile(String bootHash) {
        @SuppressLint("SdCardPath") File file = new File("/data/data/com.reveny.vbmetafix.service/cache/boot.hash");
        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                Log.e(TAG, "Failed to create directory: " + dir.getAbsolutePath());
            }
        }
        
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
        
        handler.post(() -> {
            try {
                String bootHash = Entry.run();
                Log.d(TAG, "Boot hash: " + bootHash);
                writeBootHashToFile(bootHash);
            } catch (Exception e) {
                Log.e(TAG, "Error processing boot hash", e);
            } finally {
                stopSelf();
            }
        });

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (handlerThread != null) {
            handlerThread.quitSafely();
        }
        super.onDestroy();
        Log.d(TAG, "Service Destroyed");
    }
}