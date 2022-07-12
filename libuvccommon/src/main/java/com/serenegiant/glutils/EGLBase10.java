package com.serenegiant.glutils;
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

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.GLES10;
import android.opengl.GLES20;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.serenegiant.utils.BuildCheck;

/**
 * EGLレンダリングコンテキストを生成＆使用するためのヘルパークラス
 */
/*package*/ class EGLBase10 extends EGLBase {
//	private static final boolean DEBUG = false;	// FIXME set false on release
	private static final String TAG = "EGLBase10";

    private EGL10 mEgl = null;
	private EGLDisplay mEglDisplay = null;
    private Config mEglConfig = null;
    private int mGlVersion = 2;

	private static final Context EGL_NO_CONTEXT = new Context(EGL10.EGL_NO_CONTEXT);
	@NonNull private Context mContext = EGL_NO_CONTEXT;

	/**
	 * EGLレンダリングコンテキストのホルダークラス
	 */
	public static class Context extends IContext {
		public final EGLContext eglContext;

		private Context(final EGLContext context) {
			eglContext = context;
		}
		
		@Override
		public long getNativeHandle() {
			return 0L;
		}
		
		@Override
		public Object getEGLContext() {
			return eglContext;
		}
	}

	public static class Config extends IConfig {
		public final EGLConfig eglConfig;

		private Config(final EGLConfig eglConfig) {
			this.eglConfig = eglConfig;
		}
	}

	/**
	 * Android4.1.2だとSurfaceを使えない。
	 * SurfaceTexture/SurfaceHolderの場合は内部でSurfaceを生成して使っているにもかかわらず。
	 * SurfaceHolderはインターフェースなのでSurfaceHolderを継承したダミークラスを生成して食わす
	 */
	public static class MySurfaceHolder implements SurfaceHolder {
		private final Surface surface;

		public MySurfaceHolder(final Surface surface) {
			this.surface = surface;
		}
		@Override
		public Surface getSurface() {
			return surface;
		}
		// ここより下はどないでもええ
		@Override
		public void addCallback(final Callback callback) {
		}
		@Override
		public void removeCallback(final Callback callback) {
		}
		@Override
		public boolean isCreating() {
			return false;
		}
		@Override
		public void setType(final int type) {
		}
		@Override
		public void setFixedSize(final int width, final int height) {
		}
		@Override
		public void setSizeFromLayout() {
		}
		@Override
		public void setFormat(final int format) {
		}
		@Override
		public void setKeepScreenOn(final boolean screenOn) {
		}
		@Override
		public Canvas lockCanvas() {
			return null;
		}
		@Override
		public Canvas lockCanvas(final Rect dirty) {
			return null;
		}
		@Override
		public void unlockCanvasAndPost(final Canvas canvas) {
		}
		@Override
		public Rect getSurfaceFrame() {
			return null;
		}
	}

	/**
	 * EGLレンダリングコンテキストに紐付ける描画オブジェクト
	 */
	public static class EglSurface implements IEglSurface {
		private final EGLBase10 mEglBase;
		private EGLSurface mEglSurface = EGL10.EGL_NO_SURFACE;

		/**
		 * Surface(Surface/SurfaceTexture/SurfaceHolder)に関係付けられたEglSurface
		 * @param eglBase
		 * @param surface
		 */
		private EglSurface(final EGLBase10 eglBase, final Object surface)
			throws IllegalArgumentException {

//			if (DEBUG) Log.v(TAG, "EglSurface:");
			mEglBase = eglBase;
			if ((surface instanceof Surface) && !BuildCheck.isAndroid4_2()) {
				// Android4.1.2だとSurfaceを使えない。
				// SurfaceTexture/SurfaceHolderの場合は内部で
				// Surfaceを生成して使っているにもかかわらず。
				// SurfaceHolderはインターフェースなのでSurfaceHolderを
				// 継承したダミークラスを生成して食わす
				mEglSurface = mEglBase.createWindowSurface(
					new MySurfaceHolder((Surface) surface));
			} else if ((surface instanceof Surface)
				|| (surface instanceof SurfaceHolder)
				|| (surface instanceof SurfaceTexture)
				|| (surface instanceof SurfaceView)) {
				mEglSurface = mEglBase.createWindowSurface(surface);
			} else {
				throw new IllegalArgumentException("unsupported surface");
			}
		}

		/**
		 * 指定した大きさを持つオフスクリーンEglSurface(PBuffer)
		 * @param eglBase
		 * @param width
		 * @param height
		 */
		private EglSurface(final EGLBase10 eglBase, final int width, final int height) {
//			if (DEBUG) Log.v(TAG, "EglSurface:");
			mEglBase = eglBase;
			if ((width <= 0) || (height <= 0)) {
				mEglSurface = mEglBase.createOffscreenSurface(1, 1);
			} else {
				mEglSurface = mEglBase.createOffscreenSurface(width, height);
			}
		}

		/**
		 * 指定したEGLSurfaceをカレントの描画Surfaceに設定する
		 * Surface全面に描画できるようにViewportも変更するので必要であればswapの後に変更すること
		 */
		@Override
		public void makeCurrent() {
			mEglBase.makeCurrent(mEglSurface);
			if (mEglBase.getGlVersion() >= 2) {
				GLES20.glViewport(0, 0,
					mEglBase.getSurfaceWidth(mEglSurface), mEglBase.getSurfaceHeight(mEglSurface));
			} else {
				GLES10.glViewport(0, 0,
					mEglBase.getSurfaceWidth(mEglSurface), mEglBase.getSurfaceHeight(mEglSurface));
			}
		}

		/**
		 * 描画を終了してダブルバッファを切り替える
		 */
		@Override
		public void swap() {
			mEglBase.swap(mEglSurface);
		}

		@Override
		public void swap(final long presentationTimeNs) {
			mEglBase.swap(mEglSurface, presentationTimeNs);
		}

		@Override
		public IContext getContext() {
			return mEglBase.getContext();
		}

		public void setPresentationTime(final long presentationTimeNs) {
//			EGLExt.eglPresentationTimeANDROID(mEglBase.mEglDisplay,
// 				mEglSurface, presentationTimeNs);
		}

		/**
		 * EGLSurfaceが有効かどうかを取得
		 * @return
		 */
		@Override
		public boolean isValid() {
			return (mEglSurface != null)
				&& (mEglSurface != EGL10.EGL_NO_SURFACE)
				&& (mEglBase.getSurfaceWidth(mEglSurface) > 0)
				&& (mEglBase.getSurfaceHeight(mEglSurface) > 0);
		}

		/**
		 * 破棄処理
		 */
		@Override
		public void release() {
//			if (DEBUG) Log.v(TAG, "EglSurface:release:");
			mEglBase.makeDefault();
			mEglBase.destroyWindowSurface(mEglSurface);
	        mEglSurface = EGL10.EGL_NO_SURFACE;
		}
	}

	/**
	 * コンストラクタ
	 * @param maxClientVersion
	 * @param sharedContext 共有コンテキストを使用する場合に指定
	 * @param withDepthBuffer
	 * @param isRecordable true MediaCodec等の録画用Surfaceを使用する場合に、
	 * 						EGL_RECORDABLE_ANDROIDフラグ付きでコンフィグする
	 */
	public EGLBase10(final int maxClientVersion,
		final Context sharedContext, final boolean withDepthBuffer,
		final int stencilBits, final boolean isRecordable) {

//		if (DEBUG) Log.v(TAG, "Constructor:");
		init(maxClientVersion, sharedContext, withDepthBuffer, stencilBits, isRecordable);
	}

	/**
	 * 関連するリソースを破棄する
	 */
	@Override
    public void release() {
//		if (DEBUG) Log.v(TAG, "release:");
    	destroyContext();
        mContext = EGL_NO_CONTEXT;
   		if (mEgl == null) return;
   		mEgl.eglMakeCurrent(mEglDisplay,
   			EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
//		mEgl.eglReleaseThread();	// XXX これを入れるとハングアップする機種がある
   		mEgl.eglTerminate(mEglDisplay);
    	mEglDisplay = null;
		mEglConfig = null;
   		mEgl = null;
    }

    /**
     * 指定したSurfaceからEglSurfaceを生成する
     * 生成したEglSurfaceをmakeCurrentした状態で戻る
     * @param nativeWindow Surface/SurfaceTexture/SurfaceHolder
     * @return
     */
	@Override
	public EglSurface createFromSurface(final Object nativeWindow) {
//		if (DEBUG) Log.v(TAG, "createFromSurface:");
		final EglSurface eglSurface = new EglSurface(this, nativeWindow);
		eglSurface.makeCurrent();
		return eglSurface;
	}

	/**
	 * 指定した大きさのオフスクリーンEglSurfaceを生成する
	 * 生成したEglSurfaceをmakeCurrentした状態で戻る
	 * @param width PBufferオフスクリーンのサイズ(0以下はだめ)
	 * @param height
	 * @return
	 */
	@Override
	public EglSurface createOffscreen(final int width, final int height) {
//		if (DEBUG) Log.v(TAG, "createOffscreen:");
		final EglSurface eglSurface = new EglSurface(this, width, height);
		eglSurface.makeCurrent();
		return eglSurface;
	}

	/**
	 * EGLレンダリングコンテキストを取得する
	 * このEGLBaseインスタンスを使って生成したEglSurfaceをmakeCurrentした状態で
	 * eglGetCurrentContextを呼び出すのと一緒
	 * @return
	 */
	@Override
	public Context getContext() {
		return mContext;
	}

	/**
	 * EGLコンフィグを取得する
	 * @return
	 */
	@Override
	public Config getConfig() {
		return mEglConfig;
	}

	/**
	 * EGLレンダリングコンテキストとスレッドの紐付けを解除する
	 */
	@Override
	public void makeDefault() {
//		if (DEBUG) Log.v(TAG, "makeDefault:");
	    if (!mEgl.eglMakeCurrent(mEglDisplay,
	    	EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)) {

			Log.w(TAG, "makeDefault:eglMakeCurrent:err=" + mEgl.eglGetError());
	    }
	}

	/**
	 * eglWaitGLとeglWaitNativeを呼ぶ
	 *
	 * eglWaitGL: コマンドキュー内のコマンドをすべて転送する, GLES20.glFinish()と同様の効果
	 * eglWaitNative: GPU側の描画処理が終了するまで実行をブロックする
	 */
	@Override
	public void sync() {
		mEgl.eglWaitGL();	// GLES20.glFinish()と同様の効果
		mEgl.eglWaitNative(EGL10.EGL_CORE_NATIVE_ENGINE, null);
	}

	/**
	 * GLESに文字列を問い合わせる
	 * @param what
	 * @return
	 */
	@Override
    public String queryString(final int what) {
        return mEgl.eglQueryString(mEglDisplay, what);
    }

	/**
	 * GLESバージョンを取得する
	 * @return 1, 2または3
	 */
	@Override
    public int getGlVersion() {
        return mGlVersion;
    }

	/**
	 * 初期化の下請け
	 * @param maxClientVersion
	 * @param sharedContext
	 * @param withDepthBuffer
	 * @param isRecordable
	 */
	private final void init(final int maxClientVersion,
		@Nullable Context sharedContext,
		final boolean withDepthBuffer, final int stencilBits, final boolean isRecordable) {

//		if (DEBUG) Log.v(TAG, "init:");
		sharedContext = (sharedContext != null) ? sharedContext : EGL_NO_CONTEXT;
		if (mEgl == null) {
			mEgl = (EGL10)EGLContext.getEGL();
	        mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
	        if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
	            throw new RuntimeException("eglGetDisplay failed");
	        }
	        // EGLのバージョンを取得
			final int[] version = new int[2];
	        if (!mEgl.eglInitialize(mEglDisplay, version)) {
	        	mEglDisplay = null;
	            throw new RuntimeException("eglInitialize failed");
	        }
		}
		EGLConfig config;
		if (maxClientVersion >= 3) {
			// GLES3で取得できるかどうか試してみる
			config = getConfig(3, withDepthBuffer, stencilBits, isRecordable);
			if (config != null) {
				final EGLContext context = createContext(sharedContext, config, 3);
				if ((mEgl.eglGetError()) == EGL10.EGL_SUCCESS) {
					// ここは例外生成したくないのでcheckEglErrorの代わりに自前でチェック
					//Log.d(TAG, "Got GLES 3 config");
					mEglConfig = new Config(config);
					mContext = new Context(context);
					mGlVersion = 3;
				}
			}
		}
		// GLES3で取得できなかった時はGLES2を試みる
		if ((maxClientVersion >= 2)
			&& ((mContext == null) || (mContext.eglContext == EGL10.EGL_NO_CONTEXT))) {

            config = getConfig(2, withDepthBuffer, stencilBits, isRecordable);
            if (config == null) {
               	throw new RuntimeException("chooseConfig failed");
            }
            try {
				// create EGL rendering context
				final EGLContext context = createContext(sharedContext, config, 2);
				checkEglError("eglCreateContext");
				mEglConfig = new Config(config);
				mContext = new Context(context);
				mGlVersion = 2;
			} catch (final Exception e) {
				if (isRecordable) {
					config = getConfig(2, withDepthBuffer, stencilBits, false);
					if (config == null) {
						throw new RuntimeException("chooseConfig failed");
					}
					// create EGL rendering context
					final EGLContext context = createContext(sharedContext, config, 2);
					checkEglError("eglCreateContext");
					mEglConfig = new Config(config);
					mContext = new Context(context);
					mGlVersion = 2;
				}
			}
        }
        if ((mContext == null) || (mContext.eglContext == EGL10.EGL_NO_CONTEXT)) {
			config = getConfig(1, withDepthBuffer, stencilBits, isRecordable);
			if (config == null) {
				throw new RuntimeException("chooseConfig failed");
			}
			// create EGL rendering context
			final EGLContext context = createContext(sharedContext, config, 1);
			checkEglError("eglCreateContext");
			mEglConfig = new Config(config);
			mContext = new Context(context);
			mGlVersion = 1;
		}
        // confirm whether the EGL rendering context is successfully created
		final int[] values = new int[1];
		mEgl.eglQueryContext(mEglDisplay,
			mContext.eglContext, EGL_CONTEXT_CLIENT_VERSION, values);
		Log.d(TAG, "EGLContext created, client version " + values[0]);
        makeDefault();
	}

	/**
	 * change context to draw this window surface
	 * @return
	 */
	private final boolean makeCurrent(final EGLSurface surface) {
//		if (DEBUG) Log.v(TAG, "makeCurrent:");
/*		if (mEglDisplay == null) {
            if (DEBUG) Log.d(TAG, "makeCurrent:eglDisplay not initialized");
        } */
        if (surface == null || surface == EGL10.EGL_NO_SURFACE) {
            final int error = mEgl.eglGetError();
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                Log.e(TAG, "makeCurrent:EGL_BAD_NATIVE_WINDOW");
            }
            return false;
        }
        // attach EGL rendering context to specific EGL window surface
        if (!mEgl.eglMakeCurrent(mEglDisplay, surface, surface, mContext.eglContext)) {
			Log.w("TAG", "eglMakeCurrent" + mEgl.eglGetError());
			return false;
        }
        return true;
	}

	private final int swap(final EGLSurface surface) {
//		if (DEBUG) Log.v(TAG, "swap:");
        if (!mEgl.eglSwapBuffers(mEglDisplay, surface)) {
        	final int err = mEgl.eglGetError();
//        	if (DEBUG) Log.w(TAG, "swap:err=" + err);
            return err;
        }
        return EGL10.EGL_SUCCESS;
    }

	/**
	 * swap rendering buffer with presentation time[ns]
	 * presentationTimeNs is ignored on this method
	 * @param surface
	 * @param ignored
	 * @return
	 */
	private final int swap(final EGLSurface surface, final long ignored) {
//		if (DEBUG) Log.v(TAG, "swap:");
//		EGLExt.eglPresentationTimeANDROID(mEglDisplay, surface, presentationTimeNs);
        if (!mEgl.eglSwapBuffers(mEglDisplay, surface)) {
        	final int err = mEgl.eglGetError();
//        	if (DEBUG) Log.w(TAG, "swap:err=" + err);
            return err;
        }
        return EGL10.EGL_SUCCESS;
    }

    private final EGLContext createContext(
    	@NonNull final Context sharedContext,
    	final EGLConfig config, final int version) {

//		if (DEBUG) Log.v(TAG, "createContext:");

        final int[] attrib_list = {
        	EGL_CONTEXT_CLIENT_VERSION, version,
        	EGL10.EGL_NONE
        };
        final EGLContext context = mEgl.eglCreateContext(
        	mEglDisplay, config, sharedContext.eglContext, attrib_list);
//		checkEglError("eglCreateContext");
        return context;
    }

    private final void destroyContext() {
//		if (DEBUG) Log.v(TAG, "destroyContext:");

        if (!mEgl.eglDestroyContext(mEglDisplay, mContext.eglContext)) {
            Log.e("destroyContext", "display:" + mEglDisplay
            	+ " context: " + mContext.eglContext);
            Log.e(TAG, "eglDestroyContext:" + mEgl.eglGetError());
        }
        mContext = EGL_NO_CONTEXT;
    }

	private final int getSurfaceWidth(final EGLSurface surface) {
		final int[] value = new int[1];
		final boolean ret = mEgl.eglQuerySurface(mEglDisplay,
			surface, EGL10.EGL_WIDTH, value);
		if (!ret) value[0] = 0;
		return value[0];
	}

	private final int getSurfaceHeight(final EGLSurface surface) {
		final int[] value = new int[1];
		final boolean ret = mEgl.eglQuerySurface(mEglDisplay,
			surface, EGL10.EGL_HEIGHT, value);
		if (!ret) value[0] = 0;
		return value[0];
	}

	/**
	 * nativeWindow should be one of the SurfaceView, Surface, SurfaceHolder and SurfaceTexture
	 * @param nativeWindow
	 * @return
	 */
    private final EGLSurface createWindowSurface(final Object nativeWindow) {
//		if (DEBUG) Log.v(TAG, "createWindowSurface:nativeWindow=" + nativeWindow);

		final int[] surfaceAttribs = {
            EGL10.EGL_NONE
        };
		EGLSurface result = null;
		try {
			result = mEgl.eglCreateWindowSurface(mEglDisplay,
				mEglConfig.eglConfig, nativeWindow, surfaceAttribs);
            if (result == null || result == EGL10.EGL_NO_SURFACE) {
                final int error = mEgl.eglGetError();
                if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                    Log.e(TAG, "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                }
                throw new RuntimeException("createWindowSurface failed error=" + error);
            }
            makeCurrent(result);
			// 画面サイズ・フォーマットの取得
		} catch (final Exception e) {
			Log.e(TAG, "eglCreateWindowSurface", e);
			throw new IllegalArgumentException(e);
		}
		return result;
	}

    /**
     * Creates an EGL surface associated with an offscreen buffer.
     */
    private final EGLSurface createOffscreenSurface(final int width, final int height) {
//		if (DEBUG) Log.v(TAG, "createOffscreenSurface:");
        final int[] surfaceAttribs = {
                EGL10.EGL_WIDTH, width,
                EGL10.EGL_HEIGHT, height,
                EGL10.EGL_NONE
        };
        mEgl.eglWaitGL();
		EGLSurface result = null;
		try {
			result = mEgl.eglCreatePbufferSurface(mEglDisplay,
				mEglConfig.eglConfig, surfaceAttribs);
	        checkEglError("eglCreatePbufferSurface");
	        if (result == null) {
	            throw new RuntimeException("surface was null");
	        }
		} catch (final IllegalArgumentException e) {
			Log.e(TAG, "createOffscreenSurface", e);
		} catch (final RuntimeException e) {
			Log.e(TAG, "createOffscreenSurface", e);
		}
		return result;
    }

	private final void destroyWindowSurface(EGLSurface surface) {
//		if (DEBUG) Log.v(TAG, "destroySurface:");

        if (surface != EGL10.EGL_NO_SURFACE) {
        	mEgl.eglMakeCurrent(mEglDisplay,
        		EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        	mEgl.eglDestroySurface(mEglDisplay, surface);
        }
        surface = EGL10.EGL_NO_SURFACE;
//		if (DEBUG) Log.v(TAG, "destroySurface:finished");
	}

    private final void checkEglError(final String msg) {
        int error;
        if ((error = mEgl.eglGetError()) != EGL10.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

	private final EGLConfig getConfig(final int version,
		final boolean hasDepthBuffer, final int stencilBits, final boolean isRecordable) {

        int renderableType = EGL_OPENGL_ES2_BIT;
        if (version >= 3) {
            renderableType |= EGL_OPENGL_ES3_BIT_KHR;
        }
//		final int swapBehavior = dirtyRegions ? EGL_SWAP_BEHAVIOR_PRESERVED_BIT : 0;
        final int[] attribList = {
        	EGL10.EGL_RENDERABLE_TYPE, renderableType,
			EGL10.EGL_RED_SIZE, 8,
			EGL10.EGL_GREEN_SIZE, 8,
        	EGL10.EGL_BLUE_SIZE, 8,
        	EGL10.EGL_ALPHA_SIZE, 8,
//        	EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT | swapBehavior,
        	EGL10.EGL_NONE, EGL10.EGL_NONE,	//EGL10.EGL_STENCIL_SIZE, 8,
			// this flag need to recording of MediaCodec
        	EGL10.EGL_NONE, EGL10.EGL_NONE,	// EGL_RECORDABLE_ANDROID, 1,
        	EGL10.EGL_NONE,	EGL10.EGL_NONE,	// with_depth_buffer ? EGL10.EGL_DEPTH_SIZE : EGL10.EGL_NONE,
											// with_depth_buffer ? 16 : 0,
			EGL10.EGL_NONE
        };
        int offset = 10;
        if (stencilBits > 0) {	// ステンシルバッファ(常時未使用)
        	attribList[offset++] = EGL10.EGL_STENCIL_SIZE;
        	attribList[offset++] = 8;
        }
        if (hasDepthBuffer) {	// デプスバッファ
        	attribList[offset++] = EGL10.EGL_DEPTH_SIZE;
        	attribList[offset++] = 16;
        }
        if (isRecordable && BuildCheck.isAndroid4_3()) {
        	// MediaCodecの入力用Surfaceの場合
			// A-1000F(Android4.1.2)はこのフラグをつけるとうまく動かない
        	attribList[offset++] = EGL_RECORDABLE_ANDROID;
        	attribList[offset++] = 1;
        }
        for (int i = attribList.length - 1; i >= offset; i--) {
        	attribList[i] = EGL10.EGL_NONE;
        }
		EGLConfig config = internalGetConfig(attribList);
		if ((config == null) && (version == 2)) {
			if (isRecordable) {
				// EGL_RECORDABLE_ANDROIDをつけると失敗する機種もあるので取り除く
				final int n = attribList.length;
				for (int i = 10; i < n - 1; i += 2) {
					if (attribList[i] == EGL_RECORDABLE_ANDROID) {
						for (int j = i; j < n; j++) {
							attribList[j] = EGL10.EGL_NONE;
						}
						break;
					}
				}
				config = internalGetConfig(attribList);
			}
		}
		if (config == null) {
			Log.w(TAG, "try to fallback to RGB565");
			attribList[3] = 5;
			attribList[5] = 6;
			attribList[7] = 5;
			config = internalGetConfig(attribList);
		}
		return config;
	}

    private EGLConfig internalGetConfig(final int[] attribList) {
		final EGLConfig[] configs = new EGLConfig[1];
		final int[] numConfigs = new int[1];
		if (!mEgl.eglChooseConfig(mEglDisplay,
			attribList, configs, configs.length, numConfigs)) {

			return null;
		}
		return configs[0];
    }
}
