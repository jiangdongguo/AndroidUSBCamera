package com.jiangdg.usbcamera.view;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

import com.jiangdg.usbcamera.R;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends BaseActivity implements CameraDialog.CameraDialogParent {
	private static final boolean DEBUG = true;
	private static final String TAG = "MainActivity";
	@BindView(R.id.camera_surface_view)
	public SurfaceView mUVCCameraView;

    private final Object mSync = new Object();
    // USB和USB Camera访问管理类
    private USBMonitor mUSBMonitor;
	private UVCCamera mUVCCamera;

	private Surface mPreviewSurface;
	private boolean isActive, isPreview;

	private final SurfaceHolder.Callback mSurfaceViewCallback = new SurfaceHolder.Callback() {
		@Override
		public void surfaceCreated(final SurfaceHolder holder) {
			if (DEBUG) Log.v(TAG, "surfaceCreated:");
		}

		@Override
		public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
			if ((width == 0) || (height == 0)) return;
			if (DEBUG) Log.v(TAG, "surfaceChanged:");
			mPreviewSurface = holder.getSurface();
			synchronized (mSync) {
				if (isActive && !isPreview && (mUVCCamera != null)) {
					mUVCCamera.setPreviewDisplay(mPreviewSurface);
					mUVCCamera.startPreview();
					isPreview = true;
				}
			}
		}

		@Override
		public void surfaceDestroyed(final SurfaceHolder holder) {
			if (DEBUG) Log.v(TAG, "surfaceDestroyed:");
			synchronized (mSync) {
				if (mUVCCamera != null) {
					mUVCCamera.stopPreview();
				}
				isPreview = false;
			}
			mPreviewSurface = null;
		}
	};

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// 绑定Activity
		ButterKnife.bind(this);
		mUVCCameraView.getHolder().addCallback(mSurfaceViewCallback);

		// 初始化USBMonitor
		// 注册USB设备监听器
		mUSBMonitor = new USBMonitor(this, new OnDeviceConnectListener() {
			@Override
			public void onAttach(final UsbDevice device) {
				if (DEBUG) Log.v(TAG, "onAttach:");
				Toast.makeText(MainActivity.this, "检测到USB设备", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
				if (DEBUG) Log.v(TAG, "onConnect:");
				Toast.makeText(MainActivity.this, "成功连接到USB设备", Toast.LENGTH_SHORT).show();
				synchronized (mSync) {
					if (mUVCCamera != null) {
						mUVCCamera.destroy();
					}
					isActive = isPreview = false;
				}
				queueEvent(new Runnable() {
					@Override
					public void run() {
						synchronized (mSync) {
							final UVCCamera camera = new UVCCamera();
							camera.open(ctrlBlock);
							if (DEBUG) Log.i(TAG, "supportedSize:" + camera.getSupportedSize());
							try {
								camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG);
							} catch (final IllegalArgumentException e) {
								try {
									// fallback to YUV mode
									camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
								} catch (final IllegalArgumentException e1) {
									camera.destroy();
									return;
								}
							}
							mPreviewSurface = mUVCCameraView.getHolder().getSurface();
							if (mPreviewSurface != null) {
								isActive = true;
								camera.setPreviewDisplay(mPreviewSurface);
								camera.startPreview();
								isPreview = true;
							}
							synchronized (mSync) {
								mUVCCamera = camera;
							}
						}
					}
				}, 0);
			}

			@Override
			public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
				if (DEBUG) Log.v(TAG, "onDisconnect:");
				Toast.makeText(MainActivity.this, "与USB设备断开连接", Toast.LENGTH_SHORT).show();
				// XXX you should check whether the comming device equal to camera device that currently using
				queueEvent(new Runnable() {
					@Override
					public void run() {
						synchronized (mSync) {
							if (mUVCCamera != null) {
								mUVCCamera.close();
								if (mPreviewSurface != null) {
									mPreviewSurface.release();
									mPreviewSurface = null;
								}
								isActive = isPreview = false;
							}
						}
					}
				}, 0);
			}

			@Override
			public void onDettach(final UsbDevice device) {
				if (DEBUG) Log.v(TAG, "onDettach:");
				Toast.makeText(MainActivity.this, "未检测到USB设备", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onCancel(final UsbDevice device) {
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (DEBUG) Log.v(TAG, "onStart:");
		synchronized (mSync) {
			// 注册
			if (mUSBMonitor != null) {
				mUSBMonitor.register();
			}
		}
	}

	@Override
	protected void onStop() {
		if (DEBUG) Log.v(TAG, "onStop:");
		synchronized (mSync) {
			// 注销
			if (mUSBMonitor != null) {
				mUSBMonitor.unregister();
			}
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:");
		synchronized (mSync) {
			isActive = isPreview = false;
			if (mUVCCamera != null) {
				mUVCCamera.destroy();
				mUVCCamera = null;
			}
			// 释放资源
			if (mUSBMonitor != null) {
				mUSBMonitor.destroy();
				mUSBMonitor = null;
			}
		}
		mUVCCameraView = null;
		super.onDestroy();
	}

	@OnClick({R.id.camera_surface_view})
	public void onViewClicked(View view){
		int vId= view.getId();
		switch (vId){
			case R.id.camera_surface_view:
				if (mUVCCamera == null) {
					// XXX calling CameraDialog.showDialog is necessary at only first time(only when app has no permission).
					// 当APP访问USB设备没有被授权时，弹出对话框
					CameraDialog.showDialog(MainActivity.this);
				} else {
					synchronized (mSync) {
						mUVCCamera.destroy();
						mUVCCamera = null;
						isActive = isPreview = false;
					}
				}
				break;
		}
	}


	/**
	 * to access from CameraDialog
	 * @return
	 */
	@Override
	public USBMonitor getUSBMonitor() {
		return mUSBMonitor;
	}

	@Override
	public void onDialogResult(boolean canceled) {
		if (canceled) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// FIXME
				}
			}, 0);
		}
	}
}
