package com.serenegiant.graphics;
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

import android.graphics.Path;

/**
 * 三角形シェイプ
 */
public class TriangleShape extends PathShape {

	/**
	 * デフォルトの三角形を生成
	 */
	public TriangleShape() {
		this(0, 0, 0, 1, 1, 1);
	}

	/**
	 * 座標を指定して三角形を生成。
	 * (x0,y0)-(x1,y1)-(x2,y2)の６点の値を使う
	 * ここでの値はShape内描画座標系でShapeの描画内容の位置関係を示すだけなので
	 * 実際の表示サイズとは異なる。
	 * @param pointes
	 */
	public TriangleShape(final float[] pointes) {
		this(pointes[0], pointes[1], pointes[2], pointes[3], pointes[4], pointes[5]);
	}

	/**
	 * 座標を指定して三角形を生成
	 * (x0,y0)-(x1,y1)-(x2,y2)-(x0,y0)を結ぶ三角形を生成する
	 * ここでの値はShape内描画座標系でShapeの描画内容の位置関係を示すだけなので
	 * 実際の表示サイズとは異なる。
	 * @param x0
	 * @param y0
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	public TriangleShape(final float x0, final float y0, final float x1, final float y1, final float x2, final float y2) {
		super(null, delta(x0, x1, x2), delta(y0, y1, y2));
		final float minx = min(x0, x1, x2);
		final float miny = min(y0, y1, y2);
		final Path path = new Path();
		path.moveTo(x0 - minx,  y0 - miny);
		path.lineTo(x1 - minx,  y1 - miny);
		path.lineTo(x2 - minx,  y2 - miny);
		path.close();
		setPath(path);
	}

	/**
	 * 与えられた値の中から最小の値を返す
	 * @param v0
	 * @param v1
	 * @param v2
	 * @return
	 */
	private static final float min(final float v0, final float v1, final float v2) {
		return Math.min(Math.min(v0, v1), v2);
	}

	/**
	 * 与えられた値の中から最大の値を返す
	 * @param v0
	 * @param v1
	 * @param v2
	 * @return
	 */
	private static final float max(final float v0, final float v1, final float v2) {
		return Math.max(Math.max(v0, v1), v2);
	}

	/**
	 * 与えられた値から最大と最小の差を返す
	 * @param v0
	 * @param v1
	 * @param v2
	 * @return
	 */
	private static final float delta(final float v0, final float v1, final float v2) {
		return (max(v0, v1, v2) - min(v0, v1, v2));
	}

}
