package com.example.krishna.simplesyncapp;

import com.dropbox.client2.DropboxAPI;

import java.util.ArrayList;

/**
 * Created by krishna on 9/6/15.
 */
public interface GetFilesListener {

    void getFiles(ArrayList<DropboxAPI.Entry> dbFileDetals);
}
