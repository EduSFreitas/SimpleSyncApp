package com.example.krishna.simplesyncapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by krishna on 8/6/15.
 */
public class FileProgressAdapter extends BaseAdapter {

    private ArrayList<FileItem> listFiles;
    private final LayoutInflater inflater;

    public FileProgressAdapter(Context context, ArrayList<FileItem> listFiles){

        inflater = LayoutInflater.from(context);
        this.listFiles = listFiles;
    }

    @Override
    public int getCount() {
        return listFiles.size();
    }

    @Override
    public FileItem getItem(int position) {
        return listFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if(convertView==null){
            convertView=inflater.inflate(R.layout.list_file_item, null);
            viewHolder=new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }else {
            viewHolder= (ViewHolder) convertView.getTag();
        }

        viewHolder.updateView(getItem(position));

        return convertView;
    }

    public static class ViewHolder{
        TextView tvFile;
        ProgressBar pgFile;

        public ViewHolder(View view){
            tvFile= (TextView) view.findViewById(R.id.tv_file);
            pgFile= (ProgressBar) view.findViewById(R.id.pg_file);
        }

        public void updateView(FileItem item){
            tvFile.setText(item.getFilePath());
            pgFile.setProgress(item.getProgress());
        }
    }
}
