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
 * 球オブジェクト(3D)
 */
public class SphereBounds extends CircleBounds {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5374122610666117206L;

	public SphereBounds(final float x, final float y, final float z, final float radius) {
		super(x, y, z, radius);
	}

	public SphereBounds(final float x, final float y, final float radius) {
		super(x, y, radius);
	}

	public SphereBounds(final Vector v, final float radius) {
		super(v, radius);
	}
	
}
