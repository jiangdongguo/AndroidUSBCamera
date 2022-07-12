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

public class RingBounds extends CylinderBounds {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5157039256747626240L;
	protected float height;				// 高さの1/2
	protected float inner_r;			// 内円柱の半径

	public RingBounds(final float x, final float y, final float z, final float height, final float outer, final float inner) {
		super(x, y, z, height, outer);
		this.inner_r = inner;	// 底円の半径
	}

	public RingBounds(final Vector center, final float height, final float outer, final float inner) {
		this(center.x, center.y, center.z, height, outer, inner);
	}

	@Override
	public boolean ptInBounds(final float x, final float y, final float z) {
		boolean f = super.ptInBounds(x, y, z);		// 境界球と外円柱のチェック
		if (f) {
			f = !ptInCylinder(x, y, z, inner_r);	// 内円柱のチェック
		}
		return f;
	}

}
