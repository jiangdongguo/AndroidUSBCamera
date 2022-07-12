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

/**
 * 二等辺三角形シェイプ
 */
public class IsoscelesTriangleShape extends TriangleShape {

	/**
	 * 高さと幅を指定して二等辺三角形Shapeを生成する
	 * ここでの高さと幅はShape内描画座標系でShapeの描画内容の位置関係を示すだけなので
	 * 実際の表示サイズとは異なる。単に高さと幅の比率だけ指定するだけでよい
	 * @param height
	 * @param width
	 */
	public IsoscelesTriangleShape(final float height, final float width) {
		super(new float[] {0, height, width, height, width / 2, 0});
	}

}
