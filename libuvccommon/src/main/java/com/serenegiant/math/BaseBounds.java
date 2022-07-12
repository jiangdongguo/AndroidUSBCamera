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

import java.io.Serializable;

public abstract class BaseBounds implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5504958491886331189L;
	public final Vector position = new Vector();
	public final Vector angle = new Vector();
	public float radius;

	public BaseBounds() {
	}
	
	public BaseBounds(final BaseBounds src) {
		set(src);
	}
	
	public BaseBounds(final float center_x, final float center_y, final float radius) {
		position.set(center_x, center_y);
		this.radius = radius;
	}
	
	public BaseBounds(final float center_x, final float center_y, final float center_z, final float radius) {
		position.set(center_x, center_y, center_z);
		this.radius = radius;
	}
	
	public BaseBounds set(final BaseBounds src) {
		position.set(src.position);
		angle.set(src.angle);
		radius = src.radius;
		return this;
	}
	
	// 境界球内かどうかをチェック
	protected boolean ptInBoundsSphere(final float x, final float y, final float z, final float r) {
		return position.distSquared(x, y, z) < r * r;
	}
	
	public boolean ptInBounds(final float x, final float y) {
		return ptInBounds(x, y, position.z);
	}
	
	public boolean ptInBounds(final Vector other) {
		return ptInBounds(other.x, other.y, other.z);
	}

	public abstract boolean ptInBounds(final float x, final float y, final float z);
	
	public BaseBounds move(final float offset_x, final float offset_y) {
		position.add(offset_x, offset_y);
		return this;
	}
	
	public BaseBounds move(final float offset_x, final float offset_y, final float offset_z) {
		position.add(offset_x, offset_y, offset_z);
		return this;
	}
	
	public BaseBounds move(final Vector offset) {
		position.add(offset);
		return this;
	}
	
	public BaseBounds setPosition(final Vector pos) {
		position.set(pos);
		return this;
	}
	
	public BaseBounds setPosition(final float x, final float y) {
		position.set(x, y);
		return this;
	}

	public BaseBounds setPosition(final float x, final float y, final float z) {
		position.set(x, y, z);
		return this;
	}
	
	public void centerX(final float x) {
		position.x = x;
	}

	public float centerX() {
		return position.x;
	}

	public void centerY(final float y) {
		position.y = y;
	}

	public float centerY() {
		return position.y;
	}

	public void centerZ(final float z) {
		position.z = z;
	}

	public float centerZ() {
		return position.z;
	}

	public void rotate(final Vector angle) {
		angle.set(angle.x, angle.y, angle.z);
	}

	public void rotate(final float x, final float y, final float z) {
		angle.set(x, y, z);
	}

	public void rotateX(final float angle) {
		this.angle.x = angle;
	}

	public void rotateY(final float angle) {
		this.angle.y = angle;
	}

	public void rotateZ(final float angle) {
		this.angle.z = angle;
	}

}
