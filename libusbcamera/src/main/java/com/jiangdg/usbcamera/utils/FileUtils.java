package com.jiangdg.usbcamera.utils;

import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * Created by jiangdongguo on 2017/10/18.
 */

public class FileUtils {

    private static BufferedOutputStream outputStream;
    public static String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator;

    public static void createfile(String path){
        File file = new File(path);
        if(file.exists()){
            file.delete();
        }
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void releaseFile(){
        try {
            if(outputStream != null) {
                outputStream.flush();
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void putFileStream(byte[] data,int offset,int length){
        if(outputStream != null) {
            try {
                outputStream.write(data,offset,length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void putFileStream(byte[] data){
        if(outputStream != null) {
            try {
                outputStream.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
