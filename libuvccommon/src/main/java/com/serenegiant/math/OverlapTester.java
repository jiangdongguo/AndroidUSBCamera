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
 * オブジェクトの衝突・重なりチェック
 */
public class OverlapTester {
	public static boolean check(final BaseBounds b1, final BaseBounds b2) {
		final float distance = b1.position.distSquared(b2.position);
		final float radiusSum = b1.radius + b2.radius;
		return (distance <= radiusSum * radiusSum);
	}
	
	public static boolean check(final CircleBounds c1, final CircleBounds c2) {
		final float distance = c1.position.distSquared(c2.position);
		final float radiusSum = c1.radius + c2.radius;
		return (distance <= radiusSum * radiusSum);
	}
	
	private static final Vector r1L = new Vector();
	private static final Vector r2L = new Vector();
	public static boolean check(final RectangleBounds r1, final RectangleBounds r2) {
		r1L.set(r1.position).sub(r1.box);
		final float r1x = r1.box.x * 2;
		final float r1y = r1.box.y * 2;
		final float r1z = r1.box.z * 2;
		r2L.set(r2.position).sub(r2.box);
		final float r2x = r2.box.x * 2;
		final float r2y = r2.box.y * 2;
		final float r2z = r2.box.z * 2;
		
		return ((r1L.x < r2L.x + r2x)
		 && (r1L.x + r1x > r2L.x)
		 && (r1L.y < r2L.y + r2y)
		 && (r1L.y + r1y > r2L.y)
		 && (r1L.z < r2L.z + r2z)
		 && (r1L.z + r1z > r2L.z));
	}
	
	public static boolean check(final CircleBounds c, final RectangleBounds r) {
		float cx = c.position.x;
		float cy = c.position.y;
		float cz = c.position.z;
		r1L.set(r.position).sub(r.box);
		final float rx = r.box.x * 2;
		final float ry = r.box.y * 2;
		final float rz = r.box.z * 2;
		
		if (c.position.x < r1L.x) {
			cx = r1L.x;
		} else if (c.position.x > r1L.x + rx) {
			cx = r1L.x + rx;
		}
		
		if (c.position.y < r1L.y) {
			cy = r1L.y;
		} else if (c.position.y > r1L.y + ry) {
			cy = r1L.y + ry;
		}

		if (c.position.z < r1L.z) {
			cz = r1L.z;
		} else if (c.position.z > r1L.z + rz) {
			cz = r1L.z + rz;
		}

		return (c.position.distSquared(cx, cy, cz) < c.radius * c.radius);
	}
	
	public static boolean check(final CircleBounds c, final Vector p) {
		return (c.position.distSquared(p) < c.radius * c.radius);
	}
	
	public static boolean check(final CircleBounds c, final float x, final float y, final float z) {
		return (c.position.distSquared(x, y, z) < c.radius * c.radius);
	}

	public static boolean check(final CircleBounds c, final float x, final float y) {
		return (c.position.distSquared(x, y) < c.radius * c.radius);
	}
	
	public static boolean check(final RectangleBounds r, final Vector p) {
		return check(r, p.x, p.y, p.z);
	}

	public static boolean check(final RectangleBounds r, final float x, final float y) {
		return check(r, x, y, 0f);
	}
	
	public static boolean check(final RectangleBounds r, final float x, final float y, final float z) {
		r1L.set(r.position).sub(r.box);
		return ((r1L.x <= x) && (r1L.x + r.box.x * 2 >= x)
				&& (r1L.y <= y) && (r1L.y + r.box.y * 2 >= y)
				&& (r1L.z <= z) && (r1L.z + r.box.z * 2 >= z));
	}
	
	public static boolean check(final SphereBounds s1, final SphereBounds s2) {
		final float distance = s1.position.distance(s2.position);
		return (distance <= s1.radius + s2.radius);
	}
	
	public static boolean check(final SphereBounds s, final Vector pos) {
		return (s.position.distSquared(pos) < s.radius * s.radius);
	}
	
	public static boolean check(final SphereBounds s, final float x, final float y, final float z) {
		return (s.position.distance(x, y, z) < s.radius * s.radius);
	}
}
