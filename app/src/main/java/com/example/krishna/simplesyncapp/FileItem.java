package com.example.krishna.simplesyncapp;

/**
 * Created by krishna on 8/6/15.
 */
public class FileItem {

    private String filePath;
    private int progress;

    public FileItem(String filePath) {
        this.filePath = filePath;
    }

    public int getProgress() {
        return progress;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    @Override
    public String toString() {
        return "filePath='" + filePath ;
    }
}
