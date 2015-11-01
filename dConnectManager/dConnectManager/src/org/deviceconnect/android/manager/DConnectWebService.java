/*
 DConnectWebService.java
 Copyright (c) 2014 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.manager;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;

import org.deviceconnect.android.manager.setting.SettingActivity;
import org.deviceconnect.android.manager.util.DConnectUtil;
import org.deviceconnect.server.DConnectServer;
import org.deviceconnect.server.DConnectServerConfig;
import org.deviceconnect.server.nanohttpd.DConnectServerNanoHttpd;

import java.util.logging.Logger;

/**
 * Webサーバ用のサービス.
 * @author NTT DOCOMO, INC.
 */
public class DConnectWebService extends Service {
    /** ロガー. */
    protected final Logger mLogger = Logger.getLogger("dconnect.manager");

    /** Notification Id. */
    private static final int ONGOING_NOTIFICATION_ID = 8080;

    /** Webサーバ. */
    private DConnectServer mWebServer;

    /** DConnectの設定. */
    private DConnectSettings mSettings;

    @Override
    public IBinder onBind(final Intent intent) {
        return (IBinder) mBinder;
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSettings = DConnectSettings.getInstance();
        mSettings.load(this);
    }

    @Override
    public void onDestroy() {
        stopWebServer();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        return START_STICKY;
    }

    /**
     * Webサーバを起動する.
     */
    private synchronized void startWebServer() {
        if (mWebServer == null) {
            DConnectServerConfig.Builder builder = new DConnectServerConfig.Builder();
            builder.port(mSettings.getWebPort())
                    .documentRootPath(mSettings.getDocumentRootPath());

            mLogger.fine("Web Server was Started.");
            mLogger.fine("Host: " + mSettings.getHost());
            mLogger.fine("Port: " + mSettings.getWebPort());
            mLogger.fine("Document Root: " + mSettings.getDocumentRootPath());

            mWebServer = new DConnectServerNanoHttpd(builder.build(), this);
            mWebServer.start();
            showNotification();
        }
    }

    /**
     * Webサーバを停止する.
     */
    private synchronized void stopWebServer() {
        if (mWebServer != null) {
            mWebServer.shutdown();
            mWebServer = null;
            hideNotification();
        }
        mLogger.fine("Web Server was Stopped.");
    }

    /**
     * サービスをフォアグランドに設定する。
     */
    private void showNotification() {
        Intent notificationIntent = new Intent(getApplicationContext(), SettingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, notificationIntent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setContentIntent(pendingIntent);
        builder.setTicker(getString(R.string.service_web_server));
        builder.setContentTitle(getString(R.string.service_web_server));
        builder.setContentText(DConnectUtil.getIPAddress(this) + ":" + mSettings.getWebPort());
        builder.setSmallIcon(R.drawable.icon);

        startForeground(ONGOING_NOTIFICATION_ID, builder.build());
    }

    /**
     * フォアグランドを停止する。
     */
    private void hideNotification() {
        stopForeground(true);
    }

    private final IDConnectWebService mBinder = new IDConnectWebService.Stub()  {
        @Override
        public IBinder asBinder() {
            return null;
        }

        @Override
        public boolean isRunning() throws RemoteException {
            return mWebServer != null;
        }

        @Override
        public void start() throws RemoteException {
            startWebServer();
        }

        @Override
        public void stop() throws RemoteException {
            stopWebServer();
        }
    };
}
