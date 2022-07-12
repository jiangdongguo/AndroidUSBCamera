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

import android.opengl.Matrix;

import java.io.Serializable;
import java.util.Locale;

public class Vector implements Serializable, Cloneable {
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 1620440892067002860L;

	public static final float TO_RADIAN = (float)(Math.PI / 180.0f);
	public static final float TO_DEGREE = (float)(180.0f / Math.PI);

	public static final Vector zeroVector = new Vector();
	public static final Vector normVector = new Vector(1,1,1);

	private static final float[] matrix = new float[16];
	private static final float[] inVec = new float[4];
	private static final float[] outVec = new float[4];

	public float x, y, z;

	public Vector() {
	}

	public Vector(final float x, final float y) {
		this(x, y, 0.0f);
	}

	public Vector(final Vector v) {
		this(v.x, v.y, v.z);
	}

	public Vector(final float x, final float y, final float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static Vector vector(final float x, final float y, final float z) {
		return new Vector(x, y, z);
	}

	public static Vector vector(final Vector v) {
		return new Vector(v.x, v.y, v.z);
	}

	public Vector clone() throws CloneNotSupportedException {
		final Vector result = (Vector)super.clone();
		return result;
	}

	/**
	 * ベクトルの各成分に指定したスカラ値をセット
	 * @param scalar
	 * @return
	 */
	public Vector clear(final float scalar) {
		x = y = z = scalar;
		return this;
	}

	/**
	 * ベクトルの各成分に指定したスカラ値をセット, zは0
	 * @param x
	 * @param y
	 * @return
	 */
	public Vector set(final float x, final float y) {
		return set(x, y, 0.0f);
	}

	/**
	 * ベクトルに指定したベクトルをセット v = v'
	 * @param v
	 * @return
	 */
	public Vector set(final Vector v) {
		return set(v.x, v.y, v.z);
	}

	/**
	 * ベクトルに指定したベクトルをセット(スケール変換あり) v' = v * a
	 * @param v
	 * @param a
	 * @return
	 */
	public Vector set(final Vector v, final float a) {
		return set(v.x, v.y, v.z, a);
	}

	/**
	 * ベクトルの各成分に指定したスカラ値をセット v = (x,y,z)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public Vector set(final float x, final float y, final float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	/**
	 * ベクトルの各成分に指定したスカラ値をセット(スケール変換有り) v = (x,y,z) * a
	 * @param x
	 * @param y
	 * @param z
	 * @param a
	 * @return
	 */
	public Vector set(final float x, final float y, final float z, final float a) {
		this.x = x * a;
		this.y = y * a;
		this.z = z * a;
		return this;
	}

	/**
	 * x成分値を取得
	 * @return
	 */
	public float x() {
		return x;
	}

	/**
	 * x成分値をセット
	 * @param x
	 */
	public void x(final float x) {
		this.x = x;
	}

	/**
	 * y成分値を取得
	 * @return
	 */
	public float y() {
		return y;
	}

	/**
	 * y成分値をセット
	 * @param y
	 */
	public void y(final float y) {
		this.y = y;
	}

	/**
	 * z成分値を取得
	 * @return
	 */
	public float z() {
		return z;
	}

	/**
	 * z成分値をセット
	 * @param z
	 */
	public void z(final float z) {
		this.z = z;
	}

	/**
	 * ベクトルに加算 v = v + (x,y,0)
	 * @param x
	 * @param y
	 * @return
	 */
	public Vector add(final float x, final float y) {
		return add(x, y, 0.0f);
	}

	/**
	 * ベクトルを加算 v = v + (x,y,z)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public Vector add(final float x, final float y, final float z) {
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}

	/**
	 * ベクトルを加算(スケール変換有り)v = v + (x,y,z)*a
	 * @param x
	 * @param y
	 * @param z
	 * @param a
	 * @return
	 */
	public Vector add(final float x, final float y, final float z, final float a) {
		this.x += x * a;
		this.y += y * a;
		this.z += z * a;
		return this;
	}

	/**
	 * ベクトルを加算 v = v + v'
	 * @param v
	 * @return
	 */
	public Vector add(final Vector v) {
		return add(v.x, v.y, v.z);
	}

	/**
	 * ベクトルを加算(スケール変換有り) v = v + v' * a
	 * @param v
	 * @param a
	 * @return
	 */
	public Vector add(final Vector v, final float a) {
		return add(v.x, v.y, v.z, a);
	}

	/**
	 * ベクトルを減算 v = v - (x,y,0)
	 * @param x
	 * @param y
	 * @return
	 */
	public Vector sub(final float x, final float y) {
		return add(-x, -y, 0.0f);
	}

	/**
	 * ベクトルを減算 v = v - v'
	 * @param v
	 * @return
	 */
	public Vector sub(final Vector v) {
		return add(-v.x, -v.y, -v.z);
	}

	public Vector sub(final Vector v, final float a) {
		return add(-v.x, -v.y, -v.z, a);
	}

	/**
	 * ベクトルを減算 v = v - (x,y,z)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public Vector sub(final float x, final float y, final float z) {
		return add(-x, -y, -z);
	}

	/**
	 * ベクトルを減算(スケール変換有り)v = v - (x,y,z)*a
	 * @param x
	 * @param y
	 * @param z
	 * @param a
	 * @return
	 */
	public Vector sub(final float x, final float y, final float z, final float a) {
		return add(-x, -y, -z, a);
	}

	/**
	 * ベクトルの各成分同士の掛け算(内積・外積じゃないよ)
	 * @param other
	 * @return
	 */
	public Vector mult(final Vector other) {
		this.x *= other.x;
		this.y *= other.y;
		this.z *= other.z;
		return this;
	}

	/**
	 * ベクトルの各成分にスカラ値をかけ算
	 */
	public Vector mult(final float scale) {
		this.x *= scale;
		this.y *= scale;
		this.z *= scale;
		return this;
	}

	/**
	 * ベクトルの各成分にスカラ値をかけ算
	 */
	public Vector mult(final float x_scale, final float y_scale) {
		this.x *= x_scale;
		this.y *= y_scale;
		return this;
	}

	/**
	 * ベクトルの各成分にスカラ値をかけ算
	 */
	public Vector mult(final float x_scale, final float y_scale, final float z_scale) {
		this.x *= x_scale;
		this.y *= y_scale;
		this.z *= z_scale;
		return this;
	}

	/**
	 * ベクトルの各成分同士の割り算(内積・外積じゃないよ)
	 * @param other
	 * @return
	 */
	public Vector div(final Vector other) {
		this.x /= other.x;
		this.y /= other.y;
		this.z /= other.z;
		return this;
	}
	/**
	 * ベクトルの各成分をスカラ値で割り算
	 */
	public Vector div(final float scale) {
		this.x /= scale;
		this.y /= scale;
		this.z /= scale;
		return this;
	}

	/**
	 * ベクトルの各成分をスカラ値で割り算
	 */
	public Vector div(final float x_scale, final float y_scale) {
		this.x /= x_scale;
		this.y /= y_scale;
		return this;
	}

	/**
	 * ベクトルの各成分をスカラ値で割り算
	 */
	public Vector div(final float x_scale, final float y_scale, final float z_scale) {
		this.x /= x_scale;
		this.y /= y_scale;
		this.z /= z_scale;
		return this;
	}

	/**
	 * ベクトルの各成分をスカラ値で剰余計算
	 */
	public Vector mod(final float scalar) {
		this.x %= scalar;
		this.y %= scalar;
		this.z %= scalar;
		return this;
	}

	/**
	 * x,y,zがそれぞれ角度(degree)であるとみなしてラジアンに変換する
	 * @return
	 */
	public Vector toRadian() {
		return mult(TO_RADIAN);
	}

	/**
	 * x,y,zがそれぞれラジアンであるとみなして角度に変換する
	 * @return
	 */
	public Vector toDegree() {
		return mult(TO_DEGREE);
	}

	/**
	 * x,y,z各成分を指定した値[-scalar, +scalar]に収まるように制限する
	 * @param scalar
	 * @return
	 */
	public Vector limit(final float scalar) {
		x = x >= scalar ? scalar : (x < -scalar ? -scalar : x);
		y = y >= scalar ? scalar : (y < -scalar ? -scalar : y);
		z = z >= scalar ? scalar : (z < -scalar ? -scalar : z);
		while (x >= scalar) x -= scalar;
		while (x < -scalar) x += scalar;
		while (y >= scalar) y -= scalar;
		while (y < -scalar) y += scalar;
		while (z >= scalar) z -= scalar;
		while (z < -scalar) z += scalar;
		return this;
	}

	/**
	 * x,y,z各成分を指定した値[lower, upper]に収まるように制限する
	 * @param lower
	 * @param upper
	 * @return
	 */
	public Vector limit(final float lower, final float upper) {
		x = x >= upper ? upper : (x < lower ? lower : x);
		y = y >= upper ? upper : (y < lower ? lower : y);
		z = z >= upper ? upper : (z < lower ? lower : z);
		return this;
	}

	/**
	 * ベクトルの長さを取得
	 */
	public float len() {
		return (float) Math.sqrt(x * x + y * y + z * z);
	}

	/**
	 * ベクトルの長さの２乗を取得
	 */
	public float lenSquared() {
		return x * x + y * y + z * z;
	}

	/**
	 * ベクトルを正規化(長さを1にする)
	 */
	public Vector normalize() {
		final float len = len();
		if (len != 0) {
			this.x /= len;
			this.y /= len;
			this.z /= len;
		}
		return this;
	}

	/**
	 * ベクトルの内積を取得(dotProductと同じ)
	 * 標準化ベクトルv2を含む直線にベクトルv1を真っ直ぐ下ろした（正射影した）時の長さ
	 */
	public float dot(final Vector v) {
		return x * v.x + y * v.y + z * v.z;
	}

	/**
	 * ベクトルの内積を取得(dotと同じ)
	 * 標準化ベクトルv2を含む直線にベクトルv1を真っ直ぐ下ろした（正射影した）時の長さ
	 */
	public float dotProduct(final Vector v) {
		return x * v.x + y * v.y + z * v.z;
	}

	/**
	 * ベクトルの内積を取得(dotProductと同じ)
	 */
	public float dot(final float x, final float y, final float z) {
		return this.x * x + this.y * y + this.z * z;
	}

	/**
	 * ベクトルの内積を取得(dotと同じ)
	 */
	public float dotProduct(final float x, final float y, final float z) {
		return this.x * x + this.y * y + this.z * z;
	}

	/**
	 * ベクトルの外積を計算(2D, crossProduct2と同じ)
	 * v1×v2= x1*y2-x2*y1 = |v1||v2|sin(θ)
	 */
	public float cross2(final Vector v) {
		return x * v.y - v.x * y;
	}

	/**
	 * ベクトルの外積を計算(2D, cross2と同じ)
	 * v1×v2= x1*y2-x2*y1 = |v1||v2|sin(θ)
	 */
	public float crossProduct2(final Vector v) {
		return x * v.y - v.x * y;
	}

	/*
	 * ベクトルの外積を計算(3D, crossProductと同じ)
	 * v1×v2= (y1*z2-z1*y2, z1*x2-x1*z2, x1*y2-y1*x2) = (x3, y3, z3) =  v3
	 * 2つのベクトルに垂直な方向を向いた大きさが|v1||v2|sinθのベクトル
	 */
	public Vector cross(final Vector v) {
		return crossProduct(this, this, v);
	}

	/*
	 * ベクトルの外積を計算(3D, crossと同じ)
	 * v1×v2= (y1*z2-z1*y2, z1*x2-x1*z2, x1*y2-y1*x2) = (x3, y3, z3) =  v3
	 * 2つのベクトルに垂直な方向を向いた大きさが|v1||v2|sinθのベクトル
	 */
	public Vector crossProduct(final Vector v) {
		return crossProduct(this, this, v);
	}

	/**
	 * ベクトルの外積を計算(3D, crossProductと同じ)
	 */
	public static Vector cross(final Vector v3, final Vector v1, final Vector v2) {
		final float x3 = v1.y * v2.z - v1.z * v2.y;
		final float y3 = v1.z * v2.x - v1.x * v2.z;
		final float z3 = v1.x * v2.y - v1.y * v2.x;
		v3.x = x3; v3.y = y3; v3.z = z3;
		return v3;
	}

	/**
	 * ベクトルの外積を計算(3D, crossと同じ)
	 */
	public static Vector crossProduct(final Vector v3, final Vector v1, final Vector v2) {
		final float x3 = v1.y * v2.z - v1.z * v2.y;
		final float y3 = v1.z * v2.x - v1.x * v2.z;
		final float z3 = v1.x * v2.y - v1.y * v2.x;
		v3.x = x3; v3.y = y3; v3.z = z3;
		return v3;
	}

	/**
	 * XY平面上でベクトルとX軸の角度を取得
	 */
	public float angleXY() {
		float angle = (float) Math.atan2(y, x) * TO_DEGREE;
		if (angle < 0)
			angle += 360;
		return angle;
	}

	/**
	 * XZ平面上でベクトルとX軸の角度を取得
	 */
	public float angleXZ() {
		float angle = (float) Math.atan2(z, x) * TO_DEGREE;
		if (angle < 0)
			angle += 360;
		return angle;
	}

	/**
	 * YZ平面上でベクトルとY軸の角度を取得
	 */
	public float angleYZ() {
		float angle = (float) Math.atan2(z, y) * TO_DEGREE;
		if (angle < 0)
			angle += 360;
		return angle;
	}

	/**
	 * ベクトル間の角度を取得
	 * ベクトル１ Za=(X1,Y1,Z1)、ベクトル２ Zb=(X2,Y2,Z2)、求める角φとすると、
	 * cos φ ＝ Za・Zb / (|Za| |Zb|)
	 *  =(X1X2+Y1Y2+Z1Z2) / √{(X1^2 + Y1^2 + Z1^2)(X2^2 + Y2^2 + Z2^2)}
	 *  上式のアークコサイン(cos^-1)を取ればOK。
	 * @param v
	 * @return
	 */
	public float getAngle(final Vector v) {
		final double cos = dotProduct(v) / (float) Math.sqrt(lenSquared() * v.lenSquared());
		return (float) Math.acos(cos) * TO_DEGREE;
	}

	// Z軸周りに(XY平面上で)ベクトルを指定した角度[度]回転させる
	public Vector rotateXY(final float angle) {
		final float rad = angle * TO_RADIAN;
		final float cos = (float) Math.cos(rad);
		final float sin = (float) Math.sin(rad);

		final float newX = x * cos - y * sin;
		final float newY = x * sin + y * cos;

		x = newX;
		y = newY;

		return this;
	}

	/**
	 * Y軸周りに(XZ平面上で)ベクトルを指定した角度[度]回転させる
	 */
	public Vector rotateXZ(final float angle) {
		final float rad = angle * TO_RADIAN;
		final float cos = (float) Math.cos(rad);
		final float sin = (float) Math.sin(rad);

		final float newX = x * cos - z * sin;
		final float newZ = x * sin + z * cos;

		x = newX;
		z = newZ;

		return this;
	}

	/**
	 * X軸周りに(YZ平面上で)ベクトルを指定した角度[度]回転させる
	 */
	public Vector rotateYZ(final float angle) {
		final float rad = angle * TO_RADIAN;
		final float cos = (float) Math.cos(rad);
		final float sin = (float) Math.sin(rad);

		final float newY = y * cos - z * sin;
		final float newZ = y * sin + z * cos;

		y = newY;
		z = newZ;

		return this;
	}

	/**
	 * ベクトルを回転(スレッドセーフではない)
	 * x軸：(1,0,0), y軸(0,1,0), z軸(0,0,1)
	 * @param angle [度]
	 * @param axisX
	 * @param axisY
	 * @param axisZ
	 * @return
	 */
	public Vector rotate(final float angle, final float axisX, final float axisY, final float axisZ) {
		inVec[0] = x;
		inVec[1] = y;
		inVec[2] = z;
		inVec[3] = 1;
		Matrix.setIdentityM(matrix, 0);
		Matrix.rotateM(matrix, 0, angle, axisX, axisY, axisZ);
		Matrix.multiplyMV(outVec, 0, matrix, 0, inVec, 0);
		x = outVec[0];
		y = outVec[1];
		z = outVec[2];
		return this;
	}

	/**
	 * ベクトルを回転(スレッドセーフではない)
	 * @param angleX [度]
	 * @param angleY [度]
	 * @param angleZ [度]
	 * @return
	 */
	public Vector rotate(final float angleX, final float angleY, final float angleZ) {
		return rotate(this, angleX, angleY, angleZ);
	}

	/**
	 * ベクトルを回転(スレッドセーフではない)
	 * @param v 回転させるベクトル
	 * @param angleX [度]
	 * @param angleY [度]
	 * @param angleZ [度]
	 * @return
	 */
	public static Vector rotate(final Vector v, final float angleX, final float angleY, final float angleZ) {
		inVec[0] = v.x;
		inVec[1] = v.y;
		inVec[2] = v.z;
		inVec[3] = 1;
		Matrix.setIdentityM(matrix, 0);
		if (angleX != 0)
			Matrix.rotateM(matrix, 0, angleX, 1f, 0f, 0f);
		if (angleY != 0)
			Matrix.rotateM(matrix, 0, angleY, 0f, 1f, 0f);
		if (angleZ != 0)
			Matrix.rotateM(matrix, 0, angleZ, 0f, 0f, 1f);
		Matrix.multiplyMV(outVec, 0, matrix, 0, inVec, 0);
		v.x = outVec[0];
		v.y = outVec[1];
		v.z = outVec[2];
		return v;
	}

	/**
	 * ベクトル配列内の各ベクトルを回転(スレッドセーフでは無い)
	 * @param v ベクトル配列
	 * @param angleX [度]
	 * @param angleY [度]
	 * @param angleZ [度]
	 * @return
	 */
	public static Vector[] rotate(final Vector[] v, final float angleX, final float angleY, final float angleZ) {
		Matrix.setIdentityM(matrix, 0);
		if (angleX != 0)
			Matrix.rotateM(matrix, 0, angleX, 1f, 0f, 0f);
		if (angleY != 0)
			Matrix.rotateM(matrix, 0, angleY, 0f, 1f, 0f);
		if (angleZ != 0)
			Matrix.rotateM(matrix, 0, angleZ, 0f, 0f, 1f);
		final int n = (v != null) ? v.length : 0;
		for (int i = 0; i < n; i++) {
			if (v[i] == null) continue;
			inVec[0] = v[i].x;
			inVec[1] = v[i].y;
			inVec[2] = v[i].z;
			inVec[3] = 1;
			Matrix.multiplyMV(outVec, 0, matrix, 0, inVec, 0);
			v[i].x = outVec[0];
			v[i].y = outVec[1];
			v[i].z = outVec[2];
		}
		return v;
	}

	/**
	 * ベクトルを回転(スレッドセーフではない) v = v#rot(angle * a)
	 * @param angle 回転角, 各成分は [度]
	 * @param a　スケール変換
	 * @return
	 */
	public Vector rotate(final Vector angle, final float a) {
		rotate(angle.x * a, angle.y * a, angle.z * a);
		return this;
	}

	public Vector rotate(final Vector angle) {
		return rotate(angle.x, angle.y, angle.z);
	}

	/**
	 * 逆回転(スレッドセーフではない)
	 * @param angleX
	 * @param angleY
	 * @param angleZ
	 * @return
	 */
	public Vector rotate_inv(final float angleX, final float angleY, final float angleZ) {
		inVec[0] = x;
		inVec[1] = y;
		inVec[2] = z;
		inVec[3] = 1;
		Matrix.setIdentityM(matrix, 0);
		if (angleZ != 0)
			Matrix.rotateM(matrix, 0, angleZ, 0f, 0f, 1f);
		if (angleY != 0)
			Matrix.rotateM(matrix, 0, angleY, 0f, 1f, 0f);
		if (angleX != 0)
			Matrix.rotateM(matrix, 0, angleX, 1f, 0f, 0f);
		Matrix.multiplyMV(outVec, 0, matrix, 0, inVec, 0);
		x = outVec[0];
		y = outVec[1];
		z = outVec[2];
		return this;
	}

	/**
	 * 逆回転(スレッドセーフではない)
	 * @param angle
	 * @param a
	 * @return
	 */
	public Vector rotate_inv(final Vector angle, final float a) {
		rotate_inv(angle.x * a, angle.y * a, angle.z * a);
		return this;
	}

	/**
	 * 逆回転(スレッドセーフではない)
	 * @param angle
	 * @return
	 */
	public Vector rotate_inv(final Vector angle) {
		rotate_inv(angle, -1f);
		return this;
	}

	/**
	 * クオータニオンとして取得(4番目の成分は1)
	 * @return
	 */
	public float[] getQuat() {
		final float[] q = new float[4];
		q[0] = x;
		q[1] = y;
		q[2] = z;
		q[3] = 1;
		return q;
	}

	/**
	 * クオータニオンをセット(4番目の成分は無視される)
	 * @param q
	 * @return
	 */
	public Vector setQuat(final float[] q) {
		x = q[0];
		y = q[1];
		z = q[2];
		return this;
	}

	/**
	 * ベクトル間の距離を取得する
	 */
	public float distance(final Vector v) {
		return distance(v.x, v.y, v.z);
	}

	/**
	 * ベクトル間の距離を取得する
	 * @param x
	 * @param y
	 * @return
	 */
	public float distance(final float x, final float y) {
		return distance(x, y, this.z);
	}

	/**
	 * ベクトル間の距離を取得する
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public float distance(final float x, final float y, final float z) {
		return (float) Math.sqrt(distSquared(x, y, z));
	}

	/**
	 * ベクトル間の距離の2乗を取得する
	 * @param v
	 * @return
	 */
	public float distSquared(final Vector v) {
		return distSquared(v.x, v.y, v.z);
	}

	/**
	 * ベクトル間の距離の2乗を取得する
	 * @param x
	 * @param y
	 * @return
	 */
	public float distSquared(final float x, final float y) {
		return distSquared(x, y, this.z);
	}

	/**
	 * ベクトル間の距離の2乗を取得する
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public float distSquared(final float x, final float y, final float z) {
		final float dx = this.x - x;
		final float dy = this.y - y;
		final float dz = this.z - z;
		return dx * dx + dy * dy + dz * dz;
	}

	/**
	 * ベクトルの各成分を交換
	 */
	public Vector swap(final Vector v) {
		float w = x; x = v.x; v.x = w;
		w = y; y = v.y; v.y = w;
		w = z; z = v.z; v.z = w;
		return this;
	}

	/**
	 * x成分とy成分を交換
	 */
	public Vector swapXY() {
		final float w = x; x = y; y = w;
		return this;
	}

	/**
	 * 2つのベクトルで示す点を通る直線の傾きを取得(2D)
	 */
	public float slope(final Vector v) {
		if (v.x != x)
			return (v.y - y) / (v.x - x);
		else
			return (v.y - y >= 0 ? Float.MAX_VALUE : Float.MIN_VALUE);
	}

	/**
	 * 原点とこのベクトルが示す点を通る直線の傾きを取得(2D)
	 * @return
	 */
	public float slope() {
		if (x != 0)
			return y / x;
		else
			return (y >= 0 ? Float.MAX_VALUE : Float.MIN_VALUE);
	}

	/**
	 * 各成分を負なら-1.0f, ゼロなら0.0f, 正なら1.0fにする
	 * @return
	 */
	public Vector sign() {
		x = Math.signum(x);
		y = Math.signum(y);
		z = Math.signum(z);
		return this;
	}

	@Override
	public String toString() {
		return String.format(Locale.US, "(%f,%f,%f)", x, y, z);
	}

	public String toString(String fmt) {
		return String.format(Locale.US, fmt, x, y, z);
	}
}
