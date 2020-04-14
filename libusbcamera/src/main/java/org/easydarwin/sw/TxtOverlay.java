package org.easydarwin.sw;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by jiangdg on 2020/1/11.
 */

public class TxtOverlay {

    static {
        System.loadLibrary("TxtOverlay");
    }

    private static TxtOverlay instance;
    private final Context context;
    private long ctx;

    private TxtOverlay(Context context){
        this.context = context;
    }

    public static TxtOverlay getInstance() {
        if(instance == null) {
            throw new IllegalArgumentException("please call install in your application!");
        }
        return instance;
    }

    public static void install(Context context) {
        if(instance == null) {
            instance = new TxtOverlay(context.getApplicationContext());

            File youyuan = context.getFileStreamPath("SIMYOU.ttf");
            if (!youyuan.exists()){
                AssetManager am = context.getAssets();
                try {
                    InputStream is = am.open("zk/SIMYOU.ttf");
                    FileOutputStream os = context.openFileOutput("SIMYOU.ttf", Context.MODE_PRIVATE);
                    byte[] buffer = new byte[1024];
                    int len = 0;
                    while ((len = is.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                    os.close();
                    is.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void init(int width, int height) {
        File youyuan = context.getFileStreamPath("SIMYOU.ttf");
        if (!youyuan.exists()){
            throw new IllegalArgumentException("the font file must be exists,please call install before!");
        }
        ctx = txtOverlayInit(width, height,youyuan.getAbsolutePath());
    }

    public void overlay(byte[] data,
                        String txt) {
//        txt = "drawtext=fontfile="+context.getFileStreamPath("SIMYOU.ttf")+": text='EasyPusher 2017':x=(w-text_w)/2:y=H-60 :fontcolor=white :box=1:boxcolor=0x00000000@0.3";
//        txt = "movie=/sdcard/qrcode.png [logo];[in][logo] "
//                + "overlay=" + 0 + ":" + 0
//                + " [out]";
//        if (ctx == 0) throw new RuntimeException("init should be called at first!");
        if (ctx == 0) return;
        txtOverlay(ctx, data, txt);
    }

    public void release() {
        if (ctx == 0) return;
        txtOverlayRelease(ctx);
        ctx = 0;
    }


    private static native long txtOverlayInit(int width, int height, String fonts);

    private static native void txtOverlay(long ctx, byte[] data, String txt);

    private static native void txtOverlayRelease(long ctx);

}
