package com.jiangdg.utils;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.elvishew.xlog.LogConfiguration;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;
import com.elvishew.xlog.flattener.PatternFlattener;
import com.elvishew.xlog.printer.AndroidPrinter;
import com.elvishew.xlog.printer.file.FilePrinter;
import com.elvishew.xlog.printer.file.naming.FileNameGenerator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * X Log wrapper
 *
 * @author Created by jiangdg on 2022/7/19
 */
public class XLogWrapper {
    private static final String TAG = "AUSBC";
    private static final String FLATTERER = "{d yyyy-MM-dd HH:mm:ss.SSS} {l}/{t}: {m}";
    private static boolean mHasInit;

    public static void init(Application application, String folderPath) {
        AndroidPrinter androidPrinter = new AndroidPrinter(true);
        LogConfiguration config = new LogConfiguration.Builder().logLevel(LogLevel.ALL)
                .tag(TAG)
                .disableStackTrace().build();
        String path = TextUtils.isEmpty(folderPath) ? application.getExternalFilesDir(null).getPath() : folderPath;
        path = TextUtils.isEmpty(path) ? application.getFilesDir().getPath(): path;
        FilePrinter filePrinter = new FilePrinter.Builder(path)
                .fileNameGenerator(new MyFileNameGenerator(application))
                .flattener(new MyFlatterer(FLATTERER))
                .build();
        XLog.init(config, androidPrinter, filePrinter);
        mHasInit = true;
    }

    public static void v(String tag, String msg) {
        if (mHasInit) {
            XLog.v( "[" + tag + "]  " +msg);
            return;
        }
        Log.v(tag,  "" +msg);
    }

    public static void i(String tag, String msg) {
        if (mHasInit) {
            XLog.i( "[" + tag + "]  " +msg);
            return;
        }
        Log.i(tag,  "" +msg);
    }

    public static void d(String tag, String msg) {
        if (mHasInit) {
            XLog.d( "[" + tag + "]  " +msg);
            return;
        }
        Log.d(tag,  "" +msg);
    }

    public static void w(String tag, String msg) {
        if (mHasInit) {
            XLog.w( "[" + tag + "]  " +msg);
            return;
        }
        Log.w(tag,  "" +msg);
    }

    public static void w(String tag, String msg, Throwable throwable) {
        if (mHasInit) {
            XLog.w( "[" + tag + "]  " +msg, throwable);
            return;
        }
        Log.w(tag,  msg, throwable);
    }

    public static void w(String tag, Throwable throwable) {
        if (mHasInit) {
            XLog.w("[" + tag, throwable);
            return;
        }
        Log.w(tag,"" , throwable);
    }

    public static void e(String tag, String msg) {
        if (mHasInit) {
            XLog.e("[" + tag + "]  " +msg);
            return;
        }
        Log.e(tag,  "" +msg);
    }

    public static void e(String tag, String msg, Throwable throwable) {
        if (mHasInit) {
            XLog.e("[" + tag + "]  " +msg, throwable);
            return;
        }
        Log.e(tag,  "" +msg, throwable);
    }

    static class MyFileNameGenerator implements FileNameGenerator {
        private final Context mCtx;

        public MyFileNameGenerator(Context context) {
            this.mCtx = context;
        }

        private final ThreadLocal<SimpleDateFormat> mLocalDateFormat = new ThreadLocal<SimpleDateFormat>() {
            @NonNull
            @Override
            protected SimpleDateFormat initialValue() {
                return new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            }
        };

        @Override
        public boolean isFileNameChangeable() {
            return true;
        }

        @Override
        public String generateFileName(int logLevel, long timestamp) {
            SimpleDateFormat sdf = mLocalDateFormat.get();
            sdf.setTimeZone(TimeZone.getDefault());
            String dateStr = sdf.format(new Date(timestamp));
            return "AUSBC_v" + getVerName() + "_" + dateStr + ".log";
        }

        private String getVerName() {
            String verName = "";
            try {
                verName = mCtx.getPackageManager().
                        getPackageInfo(mCtx.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return verName;
        }
    }

    static class MyFlatterer extends PatternFlattener {
        public MyFlatterer(String pattern) {
            super(pattern);
        }
    }
}
