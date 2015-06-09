package com.example.krishna.simplesyncapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;
import com.dropbox.client2.session.TokenPair;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;

import java.io.File;
import java.util.ArrayList;


public class MainActivity extends Activity implements View.OnClickListener {


    private static final String APP_KEY = "vp7dta2hkqr1vcl";
    private static final String APP_SECRET = "3izzucyk2bbwuey";
    private static final int REQUEST_DIRECTORY = 1002;
    private static final int YOUR_RESULT_CODE = 1003;
    private DropboxAPI<AndroidAuthSession> dropbox;

    private final static String FILE_DIR = "/MySampleFolder/";
    private final static String DROPBOX_NAME = "dropbox_prefs";
    private final static String ACCESS_KEY = APP_KEY;//"YOUR_API_KEY";
    private final static String ACCESS_SECRET = APP_SECRET;//"YOUR_API_SECRET";
    private String syncFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
    private boolean isLoggedIn;
    private Button login;
    private Button uploadFile;
    private TextView tvSyncLocation;
    private ListView lvFileProgress;
    private ArrayList<FileItem> filesList = new ArrayList<>();

    @SuppressWarnings("deprecation")
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

        // Display the proper UI state if logged in or not
        loggedIn(dropbox.getSession().isLinked());

    }

    public void folderPickerDialog() {
        final Intent chooserIntent = new Intent(this, DirectoryChooserActivity.class);

        // Optional: Allow users to create a new directory with a fixed name.
        // Optional: Allow users to create a new directory with a fixed name.
        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_NEW_DIR_NAME,
                "DirChooserSample");
        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_INITIAL_DIRECTORY,
                syncFilePath);

        // REQUEST_DIRECTORY is a constant integer to identify the request, e.g. 0
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

        AndroidAuthSession session = dropbox.getSession();

        boolean flag = session.isLinked();

        Log.e("Dropbox", "isLoggedIn:" + flag);
        Log.e("Dropbox", "IsLoggedSuccessfully:" + session.authenticationSuccessful());

        if (session.authenticationSuccessful()) {
            try {
                session.finishAuthentication();

                TokenPair tokens = session.getAccessTokenPair();
                SharedPreferences prefs = getSharedPreferences(DROPBOX_NAME, 0);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(ACCESS_KEY, tokens.key);
                editor.putString(ACCESS_SECRET, tokens.secret);
                editor.commit();

                loggedIn(true);

            } catch (IllegalStateException e) {
                Toast.makeText(this, "Error during Dropbox authentication",
                        Toast.LENGTH_SHORT).show();
            }
        }



    }

    public void loggedIn(boolean isLogged) {
        this.isLoggedIn = isLogged;
        if (isLogged) {
            login.setText("Logout");
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
                    dropbox.getSession().unlink();
                    loggedIn(false);
                } else {
                    dropbox.getSession().startAuthentication(MainActivity.this);
                }
                break;
            case R.id.upload_file:

                if (isHavingInternetConnection()) {
//                    UploadFileToDropbox upload = new UploadFileToDropbox(this, dropbox, FILE_DIR, filesList, handler);
//                    upload.execute();

                    DownloadFileFromDropbox download = new DownloadFileFromDropbox(this, dropbox, handler);
                    download.execute();
                } else {
                    Toast.makeText(this, "Network connection is required to sync", Toast.LENGTH_SHORT).show();
                }
                break;
//            case R.id.tvDir:
//
//                Uri selectedUri = Uri.parse(Environment.getExternalStorageDirectory() + "/");
//                Intent intent = new Intent(Intent.ACTION_VIEW);
//                intent.setDataAndType(selectedUri, "resource/folder");
//                startActivity(intent);
//                break;
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
                }
                FileItem fileItem = filesList.get(pos);
                fileItem.setProgress(progress);
                Log.d("MainActivity", "handleMessage (Line:248) :" + "pos->" + pos + "  progress:" + progress);
            } else {
                Log.d("MainActivity", "handleMessage (Line:259) :" + " File sync failed");
            }
        }
    };

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

    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        } else {
            return false;
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
}
