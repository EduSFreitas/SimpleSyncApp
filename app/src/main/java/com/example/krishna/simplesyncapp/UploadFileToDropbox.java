package com.example.krishna.simplesyncapp;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

public class UploadFileToDropbox extends AsyncTask<Void, Void, Boolean> {

    public static final String PROGRESS = "Progress";
    public static final String FILE_POS = "filePos";
    public static final String IS_FAILED = "isFailed";
    private DropboxAPI<?> dropbox;
    private String path;
    private Context context;
    private final ArrayList<FileItem> fileItems;
    private Handler handler;

    public UploadFileToDropbox(Context context, DropboxAPI<?> dropbox,
                               String path, ArrayList<FileItem> fileItems, Handler handler) {
        this.context = context;
        this.fileItems = fileItems;
        this.handler = handler;
        this.dropbox = dropbox;
        this.path = path;
    }

    public void setHandler(Handler handler){
        this.handler=handler;
    }

//    @Override
//    protected Boolean doInBackground(Void... params) {
//        final File tempDir = context.getCacheDir();
//        File tempFile;
//        FileWriter fr;
//        try {
//            tempFile = File.createTempFile("file", ".txt", tempDir);
//            fr = new FileWriter(tempFile);
//            fr.write("Test file uploaded using Dropbox API for Android");
//            fr.close();
//
//            FileInputStream fileInputStream = new FileInputStream(tempFile);
//            dropbox.putFile(path + "sample.txt", fileInputStream,
//                    tempFile.length(), null, null);
//            tempFile.delete();
//            return true;
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (DropboxException e) {
//            e.printStackTrace();
//        }
//        return false;
//    }

//    @Override
//    protected Boolean doInBackground(Void... params) {
//
//        File picDir = new File(syncDir);
//
//        Log.d("UploadFileToDropbox", "doInBackground (Line:59) :" + "PICDIR:" + picDir);
//
//        if (picDir.canRead()) {
//            if (picDir.isDirectory()) {
//                uploadFolder(picDir);
//            } else {
//                uploadFile(picDir);
//            }
//        }
//
//        return true;
//    }
//
//    public boolean uploadFolder(File folder) {
//        File[] picFiles = folder.listFiles();
//        for (File file : picFiles) {
//            Log.d("Dropbox", "Checking:" + file.getAbsolutePath());
//            if (file.canRead()) {
//                if (file.isDirectory()) {
//                    return uploadFolder(file);
//                } else {
//                    uploadFile(file);
//                }
//            }
//        }
//        return false;
//    }
//
//    public boolean uploadFile(File file) {
//        FileInputStream fileInputStream = null;
//        try {
//            fileInputStream = new FileInputStream(file);
//            ProgressListener listener = new ProgressListener() {
//                @Override
//                public void onProgress(long l, long l2) {
//                    Log.d("UploadFileToDropbox", "onProgress (Line:94) :" + l + "  -->" + l2);
//                }
//            };
//            dropbox.putFile(path + file.getAbsolutePath(), fileInputStream,
//                    file.length(), null, false, listener);
//            Log.d("DropBox", "Upload completed:" + file.getAbsolutePath());
//            return true;
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (DropboxException e) {
//            e.printStackTrace();
//        }
//
//        Log.d("DropBox", "Upload failed at:" + file.getAbsolutePath());
//        return false;
//    }

    @Override
    protected Boolean doInBackground(Void... params) {

        Log.d("UploadFileToDropbox", "doInBackground (Line:122) :"+"Startedd...."+fileItems);
        for(int i=0; i<fileItems.size(); i++) {
            final FileItem fileItem=fileItems.get(i);
            final int pos=i;

            final File file=new File(fileItem.getFilePath());
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(file);
                ProgressListener listener=new ProgressListener() {
                    @Override
                    public void onProgress(long l, long l2) {
                        long prg=(l*100)/l2;
                        Log.d("UploadFileToDropbox", "onProgress (Line:125) :"+ prg);
                        updateFileProg((int) prg, pos);
                    }
                };
                dropbox.putFile(path + file.getAbsolutePath(), fileInputStream,
                        file.length(), null, false, listener);
                updateFileProg(100, pos);
                Log.d("UploadFileToDropbox", "doInBackground Completed(Line:145) :"+" Pos:"+pos+ "  file:"+fileItem.getFilePath());
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("UploadFileToDropbox", "Failed :"+" File:"+fileItem.getFilePath());
                if(handler!=null) {
                    Message msg = Message.obtain();
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(IS_FAILED, true);
                    bundle.putInt(FILE_POS, pos);
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                }
            }
        }
        return true;
    }

    private void updateFileProg(int prg, int position) {
        if(handler!=null) {
            Message msg = Message.obtain();
            Bundle bundle = new Bundle();
            bundle.putInt(FILE_POS, position);
            bundle.putInt(PROGRESS, (int) prg);
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            Toast.makeText(context, "File Uploaded Successfully!",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "Failed to upload file", Toast.LENGTH_LONG)
                    .show();
        }
        ((DBDownloadService)context).setDownloadOrUploadCompleted(true);
    }
}