package com.jiangdg.usbcamera.application;

import android.app.Application;

import com.jiangdg.usbcamera.utils.CrashHandler;

/**全局类
 *
 * Created by jianddongguo on 2017/7/20.
 */

public class MyApplication extends Application {
    private CrashHandler mCrashHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mCrashHandler = CrashHandler.getInstance();
        mCrashHandler.init(getApplicationContext(), getClass());
        
    }
}
