package com.serenegiant.math;
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

import android.graphics.Rect;

/**
 * 四角オブジェクト(2D)
 */
public class RectangleBounds extends BaseBounds {
	/**
	 * 
	 */
	private static final long serialVersionUID = 260429282595037220L;
	public final Vector box = new Vector();	// 高さ(x), 幅(y) 奥行き(z)の1/2
	private final Rect boundsRect = new Rect();
	private final Vector w = new Vector();	// 計算用ワーク
	
	/**
	 * コンストラクタ 中心座標と幅・高さを指定して生成
	 * @param center_x
	 * @param center_y
	 * @param center_z
	 * @param width
	 * @param height
	 * @param depth
	 */
	public RectangleBounds(final float center_x, final float center_y, final float center_z, final float width, final float height, final float depth) {
		position.set(center_x, center_y, center_z);
		box.set(width / 2f, height / 2f, depth / 2f);
		radius = box.len();
	}

	/**
	 * コンストラクタ 中心座標と幅・高さを指定して生成
	 * @param center_x
	 * @param center_y
	 * @param width
	 * @param height
	 */
	public RectangleBounds(final float center_x, final float center_y, final float width, final float height) {
		this(center_x, center_y, 0f, width, height, 0f);
	}
	
	/**
	 * コンストラクタ 中心座標と幅・高さを指定して生成
	 * @param center
	 * @param width
	 * @param height
	 */
	public RectangleBounds(final Vector center, final float width, final float height) {
		this(center.x, center.y, center.z, width, height, 0f);
	}
	
	/**
	 *  コンストラクタ 左下と右上の座標を指定して生成
	 * @param lowerLeft		左下座標
	 * @param upperRight	右上座標
	 */
	public RectangleBounds(final Vector lowerLeft, final Vector upperRight) {
		float a;
		if (lowerLeft.x > upperRight.x) {
			a = lowerLeft.x; lowerLeft.x = upperRight.x; upperRight.x = a;
		}
		if (lowerLeft.y > upperRight.y) {
			a = lowerLeft.y; lowerLeft.y = upperRight.y; upperRight.y = a;
		}
		if (lowerLeft.z > upperRight.z) {
			a = lowerLeft.z; lowerLeft.z = upperRight.z; upperRight.z = a;
		}
		setPosition(
			(upperRight.x - lowerLeft.x) / 2f,
			(upperRight.y - lowerLeft.y) / 2f,
			(upperRight.z - lowerLeft.z) / 2f);
		box.set(position).sub(lowerLeft);
		radius = box.len();
	}
	
	/**
	 * コンストラクタ 外形枠を指定
	 * @param rect
	 */
	public RectangleBounds(final Rect rect) {
		this(rect.centerX(), rect.centerY(), rect.width(), rect.height());
	}

	/**
	 * 指定座標が図形内に存在するかどうか
	 * @return 含まれる場合true
	 */
	@Override
	public boolean ptInBounds(final float x, final float y, final float z) {
		boolean f = ptInBoundsSphere(x, y, z, radius);	// 境界球内にあるかどうか
		if (f) {	// 境界球内に有る時は詳細にチェック
			// この境界図形の中心(position)に対する指定した点(絶対座標ベクトル)の相対ベクトルを計算する
			// 点の相対ベクトルを反対方向に回転させる(この境界図形自体は回転させない)
			w.set(x, y, z).sub(position).rotate(angle, -1f);
			// xz平面への投影図形(四角)内に相対ベクトル(x,0,z)が含まれるかどうか
			float x1 = position.x - box.x;
			float x2 = position.x + box.x;
			float y1 = position.y - box.y;
			float y2 = position.y + box.y;
			float z1 = position.z - box.z;
			float z2 = position.z + box.z;
			f = ((w.x >= x1) && (w.x <= x2)
				&& (w.y >= y1) && (w.y <= y2)
				&& (w.z >= z1) && (w.z <= z2));
		}
		return f;
	}
	
	/**
	 * 外形枠を取得
	 * @return
	 */
	public Rect boundsRect() {
		boundsRect.set(
			(int)(position.x - box.x), (int)(position.y - box.y),
			(int)(position.x + box.x), (int)(position.y + box.y));
		return boundsRect;
	}
	
	/**
	 * 外形枠を取得
	 * @param a スケールファクタ, 1> 拡大, <1 縮小
	 * @return
	 */
	public Rect boundsRect(final float a) {
		boundsRect.set(
			(int)(position.x - box.x * a), (int)(position.y - box.y * a),
			(int)(position.x + box.x * a), (int)(position.y + box.y * a));
		return boundsRect;
	}
}
