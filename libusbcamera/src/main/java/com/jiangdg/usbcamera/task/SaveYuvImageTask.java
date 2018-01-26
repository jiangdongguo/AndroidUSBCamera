package com.jiangdg.usbcamera.task;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;

import com.jiangdg.usbcamera.utils.YUVBean;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**保存YUV格式（NV21）图片
 *
 * Created by jiangdongguo on 2017-12-25下午9:13:01
 */
public class SaveYuvImageTask extends AsyncTask<Void, Void, Void> {
	private static final String TAG = "SaveYuvImageTask";	
	private YUVBean yuvBean;
	private Context mContext;
	//转换结果回调接口
	private OnSaveYuvResultListener mListener;
	
	public interface OnSaveYuvResultListener{
		void onSaveResult(String savePath);
	}

	public SaveYuvImageTask(YUVBean yuvBean, OnSaveYuvResultListener mListener) {
		this.yuvBean = yuvBean;
		this.mListener = mListener;
	}
	
	@Override
	protected Void doInBackground(Void... params) {   
		if (yuvBean == null || yuvBean.getWidth() == 0
				|| yuvBean.getHeight() == 0 || yuvBean.getYuvData() == null) {
			return null;
		}
		saveYuv2Jpeg(yuvBean.getYuvData(),yuvBean.getWidth(),yuvBean.getHeight());
		return null;
	}

	private void saveYuv2Jpeg(byte[] data,int width,int height){
		YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
		ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
		boolean result = yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, bos);
		if(result){
			byte[] buffer = bos.toByteArray();
			Bitmap bmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
	        bmp.recycle();
			String savPath = yuvBean.getPicPath();
			File file = new File(savPath);
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
			try {
				fos.flush();
				fos.close();
				//传递转换结果给调用者
				mListener.onSaveResult(savPath);
			} catch (IOException e) {
				e.printStackTrace();
				mListener.onSaveResult(null);
			}
		}
		try {
			bos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
