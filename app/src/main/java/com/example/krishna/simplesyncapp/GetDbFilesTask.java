package com.example.krishna.simplesyncapp;

import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;

import java.util.ArrayList;

/**
 * Created by krishna on 9/6/15.
 */
public class GetDbFilesTask extends AsyncTask<Void, Void, Void> {

    private String dbRootPath;
    private DropboxAPI<?> dropbox;
    private GetFilesListener listener;
    private boolean isAllFiles;
    private boolean isRequiredFolders;

    /**
     * Instantiates a new Get db files task.
     *
     * @param dbRootPath the db root path
     * @param dropbox the dropbox
     * @param listener the listener
     * @param isAllFiles the is all files
     * @param isRequiredFolders the is required folders
     */
    public GetDbFilesTask(String dbRootPath, DropboxAPI<?> dropbox, GetFilesListener listener, boolean isAllFiles, boolean isRequiredFolders){
        this.dbRootPath = dbRootPath;
        this.dropbox = dropbox;
        this.listener = listener;
        this.isAllFiles = isAllFiles;
        this.isRequiredFolders = isRequiredFolders;
    }

    @Override
    protected Void doInBackground(Void... params) {

        getListOfFiles(dbRootPath);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if(listener!=null) {
            listener.getFiles(filesList);
            Log.d("GetDbFilesTask", "onPostExecute (Line:40) :" + "  DBFIlesSize: " + filesList.size());
        }
    }

    private ArrayList<DropboxAPI.Entry> filesList = new ArrayList<>();

    /**
     * Gets list of files.
     *
     * @param path the path
     * @return the list of files
     */
    public ArrayList<DropboxAPI.Entry> getListOfFiles(String path) {

        filesList.clear();
        DropboxAPI.Entry entries = null;
        try {
            entries = dropbox.metadata(path, 100, null, true, null);
            for (DropboxAPI.Entry e : entries.contents) {
                if (!e.isDeleted) {
                    Log.i("Is Folder", "DBox" + String.valueOf(e.isDir));
                    Log.i("Item Name", "DBox" + e.fileName());

                    if (e.isDir) {
                        if(isRequiredFolders){
                            filesList.add(e);
                        }
                        if(isAllFiles) {
                            getFilesFromFolder(e.path);
                        }
                    } else {
                        filesList.add(e);
                    }
                }
            }
        } catch (DropboxException e) {
            e.printStackTrace();
        }

        Log.d("DownloadFileFromDropbox", "getListOfFiles (Line:82) :" + filesList);

        return filesList;
    }

    /**
     * Gets files from folder.
     *
     * @param path the path
     */
    public void getFilesFromFolder(String path) {

        DropboxAPI.Entry entries = null;
        try {
            entries = dropbox.metadata(path, 100, null, true, null);
            for (DropboxAPI.Entry e : entries.contents) {
                if (!e.isDeleted) {
                    if (e.isDir) {
                        if(isRequiredFolders){
                            filesList.add(e);
                        }
                        if(isAllFiles) {
                            getFilesFromFolder(e.path);
                        }
                    } else {
                        filesList.add(e);
                    }
                }
            }
        } catch (DropboxException e) {
            e.printStackTrace();
        }
    }

}

/**
 * The type Sample.
 */
class Sample{
    /**
     * Gets name.
     *
     * @return the name
     */
    public int getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(int name) {
        this.name = name;
    }

    private int name;
}
