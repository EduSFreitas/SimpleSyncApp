package com.example.krishna.simplesyncapp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;

import java.io.File;
import java.util.ArrayList;


public class DropboxActivity extends Activity implements View.OnClickListener, ServiceConnection {


    private static final int REQUEST_DIRECTORY = 1002;
    private static final int YOUR_RESULT_CODE = 1003;

    private String syncFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
    private boolean isLoggedIn;
    private Button login;
    private Button uploadFile;
    private TextView tvSyncLocation;
    private ListView lvFileProgress;
    private ArrayList<FileItem> filesList = new ArrayList<>();
    private DBDownloadService dbService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        login = (Button) findViewById(R.id.dropbox_login);
        login.setOnClickListener(this);
        uploadFile = (Button) findViewById(R.id.upload_file);
        uploadFile.setOnClickListener(this);
        tvSyncLocation = (TextView) findViewById(R.id.tvDir);
        tvSyncLocation.setText(syncFilePath);
        tvSyncLocation.setOnClickListener(this);
        lvFileProgress = (ListView) findViewById(R.id.lv_files);

        FileProgressAdapter adapter = new FileProgressAdapter(this, getListOfFiles(syncFilePath));
        lvFileProgress.setAdapter(adapter);

        tvSyncLocation.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                folderPickerDialog();
                return true;
            }
        });

        Intent intent = new Intent(this, DBDownloadService.class);
        startService(intent);

        Intent bindIntent = new Intent(this, DBDownloadService.class);
        bindService(bindIntent, this, BIND_AUTO_CREATE);
    }

    public void folderPickerDialog() {
        final Intent chooserIntent = new Intent(this, DirectoryChooserActivity.class);

        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_NEW_DIR_NAME,
                "DirChooserSample");
        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_INITIAL_DIRECTORY,
                syncFilePath);

        startActivityForResult(chooserIntent, REQUEST_DIRECTORY);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == REQUEST_DIRECTORY) {
            if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                syncFilePath = data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
                tvSyncLocation.setText(syncFilePath);
            } else {
                // Nothing selected
            }
        } else if (requestCode == YOUR_RESULT_CODE) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                String FilePath = getRealPathFromURI(uri);

                Toast.makeText(this, "FIle:" + FilePath, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dbService != null) {
            isLoggedIn=dbService.isLoggedIn();
            loggedIn(isLoggedIn);
            dbService.setTransactionSuccessful();
        }
    }

    private void loggedIn(boolean isLogged) {
        this.isLoggedIn = isLogged;

        Log.d("DropboxActivity", "loggedIn (Line:119) :"+isLogged);
        if (isLogged) {
            login.setText("Logout");
            boolean isEnable= dbService.isDownloadingOrUploadingCompleted();
            Log.d("DropboxActivity", "loggedIn (Line:125) :"+"Is enabled"+isEnable);
            uploadFile.setEnabled(true);
        } else {
            login.setText("Login");
            uploadFile.setEnabled(false);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.dropbox_login:
                if (isLoggedIn) {
                    dbService.logout();
                    loggedIn(false);
                } else {
                    dbService.login();
                }
                break;
            case R.id.upload_file:
                if(dbService.isDownloadingOrUploadingCompleted()) {
                    dbService.uploadFiles(filesList, handler);
                }else{
                    Toast.makeText(this, " Dropbox uploading or downloading is in progress", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor == null) return null;
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    public ArrayList<FileItem> getListOfFiles(String path) {

        filesList.clear();
        File file = new File(path);
        if (file.canRead()) {
            if (file.isDirectory()) {
                getFilesFromFolder(file);
            } else {
                filesList.add(new FileItem(path));
            }
        }
        return filesList;
    }

    public void getFilesFromFolder(File folder) {
        File[] picFiles = folder.listFiles();
        for (File file : picFiles) {
            if (file.canRead()) {
                if (file.isDirectory()) {
                    getFilesFromFolder(file);
                } else {
                    filesList.add(new FileItem(file.getAbsolutePath()));
                }
            }
        }
    }

    public View getViewByPosition(int position) {
        final int firstListItemPosition = lvFileProgress.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + lvFileProgress.getChildCount() - 1;
        if (position < firstListItemPosition || position > lastListItemPosition) {
            // return filesListview.getAdapter().getView(position, filesListview.getChildAt(position), filesListview);
            return null;
        } else {
            final int childIndex = position - firstListItemPosition;
            return lvFileProgress.getChildAt(childIndex);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        DBDownloadService.MyBinder myBinder = (DBDownloadService.MyBinder) service;
        dbService = ((DBDownloadService.MyBinder) service).getService();

        dbService.updateHandler(handler);

        Log.d("DropboxActivity", "onServiceConnected (Line:199) :");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        dbService = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        dbService.updateHandler(null);
        handler = null;
        unbindService(this);
    }

    private android.os.Handler handler = new android.os.Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Bundle bundle = msg.getData();

            boolean isFailed = bundle.getBoolean(UploadFileToDropbox.IS_FAILED);
            int pos = bundle.getInt(UploadFileToDropbox.FILE_POS);

            if (!isFailed) {
                int progress = bundle.getInt(UploadFileToDropbox.PROGRESS);
                View view = getViewByPosition(pos);
                if (view != null) {
                    FileProgressAdapter.ViewHolder holder = (FileProgressAdapter.ViewHolder) view.getTag();
                    holder.pgFile.setProgress(progress);
                    Log.d("DropboxActivity", "handleMessage (Line:232) :" + "view updated");
                    tvSyncLocation.setText(syncFilePath + "  " + progress);
                } else {
                    Log.d("DropboxActivity", "handleMessage (Line:233) :" + "View is null");
                }
                FileItem fileItem = filesList.get(pos);
                fileItem.setProgress(progress);
                Log.d("MainActivity", "handleMessage (Line:248) :" + "pos->" + pos + "  progress:" + progress);
            } else {
                Log.d("MainActivity", "handleMessage (Line:259) :" + " File sync failed");
            }
        }
    };
}
