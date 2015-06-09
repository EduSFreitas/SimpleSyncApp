package com.example.krishna.simplesyncapp;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;
import com.dropbox.client2.session.TokenPair;

import java.util.ArrayList;

public class DBDownloadService extends Service {

    private static final String APP_KEY = "vp7dta2hkqr1vcl";
    private static final String APP_SECRET = "3izzucyk2bbwuey";
    private DropboxAPI<AndroidAuthSession> dropbox;

    private final static String FILE_DIR = "/MySampleFolder/";
    private final static String DROPBOX_NAME = "dropbox_prefs";
    private final static String ACCESS_KEY = APP_KEY;//"YOUR_API_KEY";
    private final static String ACCESS_SECRET = APP_SECRET;//"YOUR_API_SECRET";
    private String syncFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
    private ArrayList<FileItem> filesList = new ArrayList<>();
    private boolean isCompleted=true;
    private boolean isUploading;

    private final IBinder mBinder = new MyBinder();
    private DownloadFileFromDropbox download;
    private UploadFileToDropbox upload;

    public DBDownloadService() {
    }

    public boolean isDownloadingOrUploadingCompleted() {
        return isCompleted;
    }

    public void updateHandler(Handler handler) {
        Log.d("DBDownloadService", "updateHandler (Line:50) :");

        if (!isCompleted) {
            if (isUploading) {
                if (upload != null) {
                    upload.setHandler(handler);
                    Log.d("DBDownloadService", "updateHandler (Line:52) :" + "Updated handler on upload");
                }
            } else {
                if(download!=null) {
                    download.setHandler(handler);
                    Log.d("DBDownloadService", "updateHandler (Line:52) :" + "Updated handler on download");
                }
            }
        }
    }

    public void setDownloadOrUploadCompleted(boolean isCompleted){
        this.isCompleted=isCompleted;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class MyBinder extends Binder {
        DBDownloadService getService() {
            return DBDownloadService.this;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate() {
        super.onCreate();

        AndroidAuthSession session;
        AppKeyPair pair = new AppKeyPair(ACCESS_KEY, ACCESS_SECRET);

        SharedPreferences prefs = getSharedPreferences(DROPBOX_NAME, 0);
        String key = prefs.getString(ACCESS_KEY, null);
        String secret = prefs.getString(ACCESS_SECRET, null);

        if (key != null && secret != null) {
            AccessTokenPair token = new AccessTokenPair(key, secret);
            session = new AndroidAuthSession(pair, Session.AccessType.APP_FOLDER, token);
        } else {
            session = new AndroidAuthSession(pair, Session.AccessType.APP_FOLDER);
        }

        dropbox = new DropboxAPI<AndroidAuthSession>(session);
    }

    public boolean isLoggedIn() {
        // Display the proper UI state if logged in or not
        return dropbox.getSession().isLinked();
    }

    @SuppressWarnings("deprecation")
    public void login() {
        dropbox.getSession().startAuthentication(this);
    }

    public void logout() {
        dropbox.getSession().unlink();
    }

    public void setTransactionSuccessful() {
        AndroidAuthSession session = dropbox.getSession();

        if (session.authenticationSuccessful()) {
            try {
                session.finishAuthentication();

                TokenPair tokens = session.getAccessTokenPair();
                SharedPreferences prefs = getSharedPreferences(DROPBOX_NAME, 0);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(ACCESS_KEY, tokens.key);
                editor.putString(ACCESS_SECRET, tokens.secret);
                editor.commit();

            } catch (IllegalStateException e) {
                Toast.makeText(this, "Error during Dropbox authentication", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public boolean isHavingInternetConnection() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        //For 3G check
        boolean is3g = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                .isConnectedOrConnecting();
        //For WiFi Check
        boolean isWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                .isConnectedOrConnecting();

        return (is3g || isWifi);
    }

    public void uploadFiles(ArrayList<FileItem> files, Handler handler) {
        if (isHavingInternetConnection()) {
            this.filesList.clear();
            this.filesList.addAll(files);
            setDownloadOrUploadCompleted(false);
            isUploading = true;
            upload = new UploadFileToDropbox(this, dropbox, FILE_DIR, this.filesList, handler);
            upload.execute();
        } else {
            Toast.makeText(this, "Network connection is required to sync", Toast.LENGTH_SHORT).show();
        }
    }

    public void downloadFiles(Handler handler) {
        if (isHavingInternetConnection()) {
            download = new DownloadFileFromDropbox(this, dropbox, handler);
            download.execute();
            setDownloadOrUploadCompleted(false);
            isUploading = true;
        } else {
            Toast.makeText(this, "Network connection is required to sync", Toast.LENGTH_SHORT).show();
        }
    }
}
