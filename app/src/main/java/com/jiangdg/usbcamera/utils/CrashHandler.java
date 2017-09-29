package com.jiangdg.usbcamera.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * UncaughtException处理类,当程序发生Uncaught异常的时候,有该类来接管程序,并记录发送错误报告.
 * 
 * @author user
 * 
 */
public class CrashHandler implements UncaughtExceptionHandler {

	public static final String TAG = "CrashHandler";

	public static final String PROGRAM_BROKEN_ACTION = "com.teligen.wccp.PROGRAM_BROKEN";

	// 系统默认的UncaughtException处理类
	private UncaughtExceptionHandler mDefaultHandler;
	// CrashHandler实例
	private static CrashHandler instance = new CrashHandler();
	// 程序的Context对象
	private Context mContext;
	// 程序的主Activity的class
	private Class<?> mainActivityClass;
	// 用来存储设备信息和异常信息
	private Map<String, String> infos = new HashMap<String, String>();

	/** 保证只有一个CrashHandler实例 */
	private CrashHandler() {
	}

	/** 获取CrashHandler实例 ,单例模式 */
	public static CrashHandler getInstance() {
		return instance;
	}

	public void init(Context context, Class<?> activityClass) {
		mContext = context;
		this.setMainActivityClass(activityClass);
		// 获取系统默认的UncaughtException处理器
		mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
		// 设置该CrashHandler为程序的默认处理器
		Thread.setDefaultUncaughtExceptionHandler(this);
	}

	/**
	 * 当UncaughtException发生时会转入该函数来处理
	 */
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		if (!handleException(ex) && mDefaultHandler != null) {
			// 如果用户没有处理则让系统默认的异常处理器来处理
			mDefaultHandler.uncaughtException(thread, ex);
		} else {
			//重启应用，释放资源
			System.out.println("uncaughtException--->" + ex.getMessage());
//			Log.e(TAG, ex.getMessage());
			logError(ex);
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
//				Log.e("debug", "error：", e);
			}
//			AppManagerUtils.removeAllActivities();
//			AppManagerUtils.restartApp(mContext,mContext.getPackageName());
//			AppManagerUtils.releaseAppResource();
		}
	}

	/**
	 * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
	 * 
	 * @param ex
	 * @return true:如果处理了该异常信息;否则返回false.
	 */
	private boolean handleException(Throwable ex) {
		if (ex == null) {
			return false;
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				Toast.makeText(mContext.getApplicationContext(),
						"程序异常退出，即将重启...", Toast.LENGTH_LONG).show();
				Looper.loop();
			}
		}).start();
		// 收集设备参数信息
		collectDeviceInfo(mContext.getApplicationContext());
		// 保存日志文件
		logError(ex);
		return true;
	}

	/**
	 * 收集设备参数信息
	 * 
	 * @param ctx
	 */
	public void collectDeviceInfo(Context ctx) {
		try {
			PackageManager pm = ctx.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(),
					PackageManager.GET_ACTIVITIES);
			if (pi != null) {
				String versionName = pi.versionName == null ? "null"
						: pi.versionName;
				String versionCode = pi.versionCode + "";
				infos.put("versionName", versionName);
				infos.put("versionCode", versionCode);
			}
		} catch (NameNotFoundException e) {
//			Log.e(TAG, "收集包信息出现错误", e);
		}
		Field[] fields = Build.class.getDeclaredFields();
		for (Field field : fields) {
			try {
				field.setAccessible(true);
				infos.put(field.getName(), field.get(null).toString());
			} catch (Exception e) {
//				Log.e(TAG, "收集系统信息出现错误", e);
			}
		}
	}

	/**
	 * 保存错误信息到文件中
	 * 
	 * @param ex
	 * @return 返回文件名称,便于将文件传送到服务器
	 */
	private void logError(Throwable ex) {

		StringBuffer sb = new StringBuffer();
//		for (Map.Entry<String, String> entry : infos.entrySet()) {
//			String key = entry.getKey();
//			String value = entry.getValue();
//			sb.append(key + "=" + value + "\n");
//		}
		int num = ex.getStackTrace().length;
		for (int i=0;i<num;i++){
			sb.append(ex.getStackTrace()[i].toString());
			sb.append("\n");
		}

		File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
				+File.separator+System.currentTimeMillis()+".txt");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			fos.write((sb.toString()+"异常："+ex.getLocalizedMessage()).getBytes());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			try {
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
//		Log.e(TAG, "出现未捕捉异常，程序异常退出！", ex);
	}

	public Class<?> getMainActivityClass() {
		return mainActivityClass;
	}

	public void setMainActivityClass(Class<?> mainActivityClass) {
		this.mainActivityClass = mainActivityClass;
	}
}
