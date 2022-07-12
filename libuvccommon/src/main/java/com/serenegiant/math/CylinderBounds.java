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

public class CylinderBounds extends BaseBounds {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2875851852923460432L;
	protected float height;		// 高さの1/2
	protected float outer_r;	// 外円柱の半径
	private final Vector w1 = new Vector();
	private final Vector w2 = new Vector();
	
	// コンストラクタ
	public CylinderBounds(final float x, final float y, final float z, final float height, final float radius) {
		position.set(x, y, z);
		this.radius = (float)Math.sqrt(radius * radius + height * height / 4);	// 境界球の半径
		this.outer_r = radius;	// 底円の半径
		this.height = height / 2;
	}

	// コンストラクタ
	public CylinderBounds(final Vector center, final float height, final float radius) {
		this(center.x, center.y, center.z, height, radius);
	}

	protected boolean ptInCylinder(final float x, final float y, final float z, final float r) {
		boolean f = false;
		// この境界図形の中心(position)に対する指定した点(絶対座標ベクトル)の相対ベクトルを計算する
		// 点の相対ベクトルを反対方向に回転させる(この境界図形自体は回転させない)
		w1.set(x, y, z).sub(position).rotate(angle, -1f);
		// xz平面への投影図形(丸)内に相対ベクトル(x,0,z)が含まれるかどうか
		w2.set(w1); w2.y = 0;	// 高さは別途
		if (w2.distSquared(position.x, 0, position.z) < r * r) {
			float x1 = position.x - r;
			float x2 = position.x + r;
			float y1 = position.y - height;
			float y2 = position.y + height;
			f = ((w1.x >= x1) && (w1.x <= x2) && (w1.y >= y1) && (w1.y <= y2));
		}
		return f;
	}

	@Override
	public boolean ptInBounds(final float x, final float y, final float z) {
		boolean f = ptInBoundsSphere(x, y, z, radius);	// 境界球内にあるかどうか
		if (f) {	// 境界球内に有る時は詳細にチェック
			f = ptInCylinder(x, y, z, outer_r);
		}
		return f;
	}

}
