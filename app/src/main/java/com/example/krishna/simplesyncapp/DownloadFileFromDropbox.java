package com.example.krishna.simplesyncapp;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by krishna on 8/6/15.
 */
public class DownloadFileFromDropbox extends AsyncTask<Void, Void, Void> {


    private Context context;
    private DropboxAPI<?> dropbox;
    private Handler handler;

    public DownloadFileFromDropbox(Context context, DropboxAPI<?> dropbox, Handler handler) {

        this.context = context;
        this.dropbox = dropbox;
        this.handler = handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    protected Void doInBackground(Void... params) {

        String path = "/";
        getListOfFiles(path);

        if (filesList.size() != 0) {
            boolean isDownloaded = downloadFile(filesList.get(0));
            Log.d("DownloadFileFromDropbox", "getListOfFiles (Line:77) :" + " isDownloaded:" + isDownloaded);
        } else {
            Log.d("DownloadFileFromDropbox", "doInBackground (Line:42) :" + " Downloaded files list is empty");
        }

        return null;
    }

    private ArrayList<String> filesList = new ArrayList<>();

    public ArrayList<String> getListOfFiles(String path) {

        filesList.clear();
        DropboxAPI.Entry entries = null;
        try {
            entries = dropbox.metadata(path, 100, null, true, null);
            for (DropboxAPI.Entry e : entries.contents) {
                if (!e.isDeleted) {
                    Log.i("Is Folder", "DBox" + String.valueOf(e.isDir));
                    Log.i("Item Name", "DBox" + e.fileName());

                    if (e.isDir) {
                        getFilesFromFolder(e.path);
                    } else {
                        filesList.add(e.path);
                    }
                }
            }
        } catch (DropboxException e) {
            e.printStackTrace();
        }

        Log.d("DownloadFileFromDropbox", "getListOfFiles (Line:82) :" + filesList);

        return filesList;
    }

    public void getFilesFromFolder(String path) {

        DropboxAPI.Entry entries = null;
        try {
            entries = dropbox.metadata(path, 100, null, true, null);
            for (DropboxAPI.Entry e : entries.contents) {
                if (!e.isDeleted) {
                    if (e.isDir) {
                        getFilesFromFolder(e.path);
                    } else {
                        filesList.add(e.path);
                    }
                }
            }
        } catch (DropboxException e) {
            e.printStackTrace();
        }
    }

    public boolean downloadFile(String dbFilePath) {

        File destFile = new File(Environment.getExternalStorageDirectory() + "/dropbox/", dbFilePath);
        Log.d("DownloadFileFromDropbox", "downloadFile (Line:124) :" + "Started..." + destFile.getAbsolutePath());

        destFile.getParentFile().mkdirs();

        FileOutputStream mFos = null;
        try {
            mFos = new FileOutputStream(destFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            closeStream(mFos);
            return false;
        }

        try {
            dropbox.getFile(dbFilePath, null, mFos, null);
            closeStream(mFos);
        } catch (DropboxException e) {
            e.printStackTrace();
            closeStream(mFos);
            return false;
        }

        Log.d("DownloadFileFromDropbox", "downloadFile (Line:137) :" + "Successfully --> " + dbFilePath);
        return true;
    }

    private void closeStream(FileOutputStream mFos) {
        if (mFos != null) {
            try {
                mFos.close();
            } catch (IOException ioe) {
            }
        }
    }
}
