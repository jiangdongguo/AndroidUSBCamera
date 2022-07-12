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

/**
 * 円(2D)/球(3D)オブジェクト
 */
public class CircleBounds extends BaseBounds {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6571630061846420508L;

	public CircleBounds(final float x, final float y, final float z, final float radius) {
		position.set(x, y, z);
		this.radius = radius;
	}

	public CircleBounds(final float x, final float y, final float radius) {
		this(x, y, 0f, radius);
	}
	
	public CircleBounds(final Vector v, final float radius) {
		this(v.x, v.y, 0f, radius);
	}
	
	@Override
	public boolean ptInBounds(final float x, final float y, final float z) {
		return ptInBoundsSphere(x, y, z, radius);
	}
}
