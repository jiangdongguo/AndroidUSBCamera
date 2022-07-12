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

public class FpsCounter {
	private int cnt, prevCnt;
	private long startTime, prevTime;
	private float fps, totalFps;
	public FpsCounter() {
		reset();
	}

	public synchronized FpsCounter reset() {
		cnt = prevCnt = 0;
		startTime = prevTime = Time.nanoTime() - 1;
		return this;
	}

	/**
	 * フレームをカウント
	 */
	public synchronized void count() {
		cnt++;
	}

	/**
	 * FPSの値を更新, 1秒程度毎に呼び出す
	 * @return
	 */
	public synchronized FpsCounter update() {
		final long t = Time.nanoTime();
		fps = (cnt - prevCnt) * 1000000000.0f / (t - prevTime);
		prevCnt = cnt;
		prevTime = t;
		totalFps = cnt * 1000000000.0f / (t - startTime);
		return this;
	}

	public synchronized float getFps() {
		return fps;
	}

	public synchronized float getTotalFps() {
		return totalFps;
	}
}
