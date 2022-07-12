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

import androidx.annotation.Nullable;

import com.serenegiant.utils.MessageTask;

public abstract class EglTask extends MessageTask {
//	private static final boolean DEBUG = false;
//	private static final String TAG = "EglTask";

	public static final int EGL_FLAG_DEPTH_BUFFER = 0x01;
	public static final int EGL_FLAG_RECORDABLE = 0x02;
	public static final int EGL_FLAG_STENCIL_1BIT = 0x04;
//	public static final int EGL_FLAG_STENCIL_2BIT = 0x08;
//	public static final int EGL_FLAG_STENCIL_4BIT = 0x10;
	public static final int EGL_FLAG_STENCIL_8BIT = 0x20;

	private EGLBase mEgl = null;
	private EGLBase.IEglSurface mEglHolder;

	public EglTask(final EGLBase.IContext sharedContext, final int flags) {
//		if (DEBUG) Log.i(TAG, "shared_context=" + shared_context);
		init(flags, 3, sharedContext);
	}

	public EglTask(final int maxClientVersion,
		final EGLBase.IContext sharedContext, final int flags) {

//		if (DEBUG) Log.i(TAG, "shared_context=" + shared_context);
		init(flags, maxClientVersion, sharedContext);
	}

	/**
	 * @param flags
	 * @param maxClientVersion
	 * @param sharedContext
	 */
	@Override
	protected void onInit(final int flags,
		final int maxClientVersion, final Object sharedContext) {

		if ((sharedContext == null)
			|| (sharedContext instanceof EGLBase.IContext)) {

			final int stencilBits
				= (flags & EGL_FLAG_STENCIL_1BIT) == EGL_FLAG_STENCIL_1BIT ? 1
					: ((flags & EGL_FLAG_STENCIL_8BIT) == EGL_FLAG_STENCIL_8BIT ? 8 : 0);
			mEgl = EGLBase.createFrom(maxClientVersion, (EGLBase.IContext)sharedContext,
				(flags & EGL_FLAG_DEPTH_BUFFER) == EGL_FLAG_DEPTH_BUFFER,
				stencilBits,
				(flags & EGL_FLAG_RECORDABLE) == EGL_FLAG_RECORDABLE);
		}
		if (mEgl == null) {
			callOnError(new RuntimeException("failed to create EglCore"));
			releaseSelf();
		} else {
			mEglHolder = mEgl.createOffscreen(1, 1);
			mEglHolder.makeCurrent();
		}
	}

	@Override
	protected Request takeRequest() throws InterruptedException {
		final Request result = super.takeRequest();
		mEglHolder.makeCurrent();
		return result;
	}

	@Override
	protected void onBeforeStop() {
		mEglHolder.makeCurrent();
	}

	@Override
	protected void onRelease() {
		mEglHolder.release();
		mEgl.release();
	}

	protected EGLBase getEgl() {
		return mEgl;
	}

	protected EGLBase.IContext getEGLContext() {
		return mEgl.getContext();
	}

	protected EGLBase.IConfig getConfig() {
		return mEgl.getConfig();
	}

	@Nullable
	protected EGLBase.IContext getContext() {
		return mEgl != null ? mEgl.getContext() : null;
	}

	protected void makeCurrent() {
		mEglHolder.makeCurrent();
	}

	protected boolean isGLES3() {
		return (mEgl != null) && (mEgl.getGlVersion() > 2);
	}
}
