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

import android.graphics.SurfaceTexture;
import android.view.Surface;

public interface IRenderer extends IRendererCommon {

	/**
	 * 関係するすべてのリソースを開放する。再利用できない
	 */
	public void release();

	/**
	 * 描画先のSurfaceをセット
	 * @param surface
	 */
	public void setSurface(final Surface surface);

	/**
	 * 描画先のSurfaceをセット
	 * @param surface
	 */
	public void setSurface(final SurfaceTexture surface);

	/**
	 * Surfaceサイズを変更
	 * @param width
	 * @param height
	 */
	public void resize(final int width, final int height);

	/**
	 * 描画要求
	 * @param args
	 */
	public void requestRender(final Object... args);
}
