package com.jiangdg.usbcamera;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

/**
 * 通用对话框
 * <p>
 * Created by jianddongguo on 2017/11/12.
 */

public class AlertCustomDialog {

    public static interface OnMySelectedListener {
        void onItemSelected(int postion);
    }

    public static void createSimpleListDialog(Context mContext, String title, List<String> dataList, final OnMySelectedListener listener) {
        View rootView = LayoutInflater.from(mContext).inflate(R.layout.dialog_list_layout, null);
        ListView mListView = (ListView) rootView.findViewById(R.id.dialog_list);
//        TextView mTitle = (TextView) rootView.findViewById(R.id.dialog_list_title);
//        mTitle.setText(title);
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(mContext);
        mBuilder.setView(rootView);
        final AlertDialog mDialog = mBuilder.create();
        mDialog.show();
        ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(mContext, R.layout.dialog_list_item_layout, R.id.dialog_list_item_content, dataList);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                listener.onItemSelected(position);
                mDialog.dismiss();
            }
        });
    }
}
