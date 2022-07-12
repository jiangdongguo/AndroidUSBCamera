package com.serenegiant.utils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2018 saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

public class GyroHelper {
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = GyroHelper.class.getSimpleName();

	private static final int[] SENSOR_TYPES = {
		Sensor.TYPE_MAGNETIC_FIELD,
		Sensor.TYPE_GRAVITY,
		Sensor.TYPE_ACCELEROMETER,
		Sensor.TYPE_GYROSCOPE,
	};

	private final Object mSync = new Object();
	private final WeakReference<Context> mWeakContext;
	private SensorManager mSensorManager;
	private boolean mRegistered;
	private int mRotation;									// 画面の向き
	private final Object mSensorSync = new Object();		// センサー値同期用
	private final float[] mMagnetValues = new float[3];		// 磁気[μT]
	private final float[] mGravityValues = new float[3];	// 重力[m/s^2]
	private final float[] mAzimuthValues = new float[3];	// 方位[-180,+180][度]
	private final float[] mAccelValues = new float[3];		// 加速度[m/s^2]
	private final float[] mGyroValues = new float[3];		// ジャイロ[radian/s]

	public GyroHelper(@NonNull final Context context) {
		mWeakContext = new WeakReference<Context>(context);
		synchronized (mSync) {
			mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		}
	}

	public void release() {
		synchronized (mSync) {
			mSensorManager = null;
		}
	}

	/**
	 * 磁気センサー・加速度センサー等を読み取り開始
	 */
	public void start() {
		synchronized (mSync) {
			final Context context = mWeakContext.get();
			if ((mSensorManager == null) || (context == null)) {
				throw new IllegalStateException("already released");
			}

			for (int i = 0; i < 3; i++) {
				mMagnetValues[i] = mGravityValues[i] = mAzimuthValues[i] = 0;
				mAccelValues[i] = mGyroValues[i] = 0;
			}
			// 重力センサーがあればそれを使う。なければ加速度センサーで代用する
			boolean hasGravity = false;
			mRegistered = true;
			for (final int sensor_type : SENSOR_TYPES) {
				final List<Sensor> sensors = mSensorManager.getSensorList(sensor_type);
				if ((sensors != null) && (sensors.size() > 0)) {
					if (sensor_type == Sensor.TYPE_GRAVITY) {
						Log.i(TAG, "hasGravity");
						hasGravity = true;
					}
					if (!hasGravity || (sensor_type != Sensor.TYPE_ACCELEROMETER)) {
						mSensorManager.registerListener(mSensorEventListener, sensors.get(0),
							SensorManager.SENSOR_DELAY_GAME);
					}
				} else {
					Log.i(TAG, String.format("no sensor for sensor type %d", sensor_type));
				}
			}
		}
	}

	/**
	 * 磁気センサー・加速度センサー等からの読み取り終了
	 */
	public void stop() {
		synchronized (mSync) {
			if (mRegistered && (mSensorManager != null)) {
				try {
					mSensorManager.unregisterListener(mSensorEventListener);
				} catch (final Exception e) {
					// ignore
				}
			}
			mRegistered = false;
		}
	}

	public void setScreenRotation(final int rotation) {
		mRotation = rotation;
	}

	/**
	 * 方位角を取得
	 * @return
	 */
	public float getAzimuth() {
		return mAzimuthValues[0];
	}

	/**
	 * 左右への傾きを取得, XXX 前後と左右が入れ替わってるかも
	 * @return
	 */
	public float getPan() {
		return mAzimuthValues[1];
	}

	/**
	 * 前後の傾きを取得, XXX 前後と左右が入れ替わってるかも
	 * @return
	 */
	public float getTilt() {
		return mAzimuthValues[2];
	}

	private final SensorEventListener mSensorEventListener = new SensorEventListener() {
		/**
		 * ハイパスフィルターを通して値をセットする
		 * @param values 値を保持するためのfloat配列
		 * @param new_values 新しい値を渡すためのfloat配列
		 * @param alpha フィルター定数(alpha=t/(t+dt)
		 */
		private void highPassFilter(final float[] values, final float[] new_values, final float alpha) {
			values[0] = alpha * values[0] + (1 - alpha) * new_values[0];
			values[1] = alpha * values[1] + (1 - alpha) * new_values[1];
			values[2] = alpha * values[2] + (1 - alpha) * new_values[2];
		}

		private final float[] outR = new float[16];
		private final float[] outR2 = new float[16];
		private void getOrientation(final float[] rotateMatrix, final float[] result) {

			switch (mRotation) {
			case Surface.ROTATION_0:
				SensorManager.getOrientation(rotateMatrix, result);
				return;
			case Surface.ROTATION_90:
				SensorManager.remapCoordinateSystem(
					rotateMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, outR);
				break;
			case Surface.ROTATION_180:
				SensorManager.remapCoordinateSystem(
					rotateMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, outR2);
				SensorManager.remapCoordinateSystem(
					outR2, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, outR);
				break;
			case Surface.ROTATION_270:
				SensorManager.remapCoordinateSystem(
					outR, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_MINUS_X, outR);
				break;
			}
			SensorManager.getOrientation(outR, result);
		}

		private static final float TO_DEGREE = (float)(180 / Math.PI);
		private final float[] mRotateMatrix = new float[16];			// 回転行列
		private final float[] mInclinationMatrix = new float[16];    	// 傾斜行列

		/**
		 * センサーの値が変化した時のコールバック
		 * @param event
		 */
		@Override
		public void onSensorChanged(final SensorEvent event) {
			if (DEBUG) Log.v(TAG, "onSensorChanged:" + event);
			final float[] values = event.values;
			final int type = event.sensor.getType();
			switch (type) {
			case Sensor.TYPE_MAGNETIC_FIELD:	// 磁気センサー
				synchronized (mSensorSync) {
					// ハイパスフィルターを通して取得
					// alpha=t/(t+dt), dt≒20msec@SENSOR_DELAY_GAME, tはローパスフィルタの時定数(t=80)
					highPassFilter(mMagnetValues, values, 0.8f);
					System.arraycopy(values, 0, mMagnetValues, 0, 3);
					// 磁気センサーの値と重力センサーの値から方位を計算
					SensorManager.getRotationMatrix(mRotateMatrix, mInclinationMatrix, mGravityValues, mMagnetValues);
					getOrientation(mRotateMatrix, mAzimuthValues);
					mAzimuthValues[0] *= TO_DEGREE;
					mAzimuthValues[1] *= TO_DEGREE;
					mAzimuthValues[2] *= TO_DEGREE;
				}
				break;
			case Sensor.TYPE_GRAVITY:			// 重力センサー
				synchronized (mSensorSync) {
					System.arraycopy(values, 0, mGravityValues, 0, 3);
				}
				break;
			case Sensor.TYPE_ACCELEROMETER:		// 加速度センサー
				synchronized (mSensorSync) {
					System.arraycopy(values, 0, mAccelValues, 0, 3);
					System.arraycopy(values, 0, mGravityValues, 0, 3);	// 重力センサーが無い時は加速度センサーで代用
				}
				break;
			case Sensor.TYPE_GYROSCOPE:			// ジャイロセンサー
				synchronized (mSensorSync) {
					System.arraycopy(values, 0, mGyroValues, 0, 3);
				}
				break;
			default:
				if (DEBUG) Log.v(TAG, "onSensorChanged:" + String.format(Locale.US, "その他%d(%f,%f,%f)", type, values[0], values[1], values[2]));
				break;
			}
		}

		/**
		 * センサーの精度が変更された時のコールバック
		 * @param sensor
		 * @param accuracy
		 */
		@Override
		public void onAccuracyChanged(final Sensor sensor, int accuracy) {
		}
	};

}
