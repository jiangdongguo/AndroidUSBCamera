package com.serenegiant.widget;
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

/**
 * コンテンツの拡大縮小方法をセット可能なViewのインターフェース
 */
public interface IScaledView {
	/** アスペクト比を保って最大化 */
	public static final int SCALE_MODE_KEEP_ASPECT = 0;
	/** 画面サイズに合わせて拡大縮小 */
	public static final int SCALE_MODE_STRETCH_TO_FIT = 1;
	/** アスペクト比を保って短辺がフィットするようにCROP_CENTER */
	public static final int SCALE_MODE_CROP = 2;

	/**
	 * 拡大縮小方法をセット
	 * @param scaleMode SCALE_MODE_KEEP_ASPECT, SCALE_MODE_STRETCH, SCALE_MODE_CROP
	 */
	public void setScaleMode(final int scaleMode);
	public int getScaleMode();
}
