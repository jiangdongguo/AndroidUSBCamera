package com.serenegiant.glutils.es1;
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

import javax.microedition.khronos.opengles.GL10;

import com.serenegiant.utils.BuildCheck;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.opengl.GLES10;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

/**
 * OpenGL|ES用のヘルパークラス
 */
public final class GLHelper {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = "GLHelper";

	/**
	 * OpenGL|ESのエラーをチェックしてlogCatに出力する
	 * @param op
	 */
    public static void checkGlError(final String op) {
        final int error = GLES10.glGetError();
        if (error != GLES10.GL_NO_ERROR) {
            final String msg = op + ": glError 0x" + Integer.toHexString(error);
			Log.e(TAG, msg);
            new Throwable(msg).printStackTrace();
//         	if (DEBUG) {
//	            throw new RuntimeException(msg);
//       	}
        }
    }

	/**
	 * OpenGL|ESのエラーをチェックしてlogCatに出力する
	 * @param gl
	 * @param op
	 */
	public static void checkGlError(final GL10 gl, final String op) {
		final int error = gl.glGetError();
		if (error != GL10.GL_NO_ERROR) {
			final String msg = op + ": glError 0x" + Integer.toHexString(error);
			Log.e(TAG, msg);
			new Throwable(msg).printStackTrace();
//         	if (DEBUG) {
//	            throw new RuntimeException(msg);
//       	}
		}
	}

	/**
	 * テクスチャ名を生成, テクスチャユニットはGL_TEXTURE0, クランプ方法はGL_CLAMP_TO_EDGE
	 * @param texTarget
	 * @param filter_param テクスチャの補完方法を指定, min/mag共に同じ値になる, GL_LINEARとかGL_NEAREST
	 * @return
	 */
	public static int initTex(final int texTarget, final int filter_param) {
		return initTex(texTarget, GLES10.GL_TEXTURE0, filter_param, filter_param, GLES10.GL_CLAMP_TO_EDGE);
	}

	/**
	 * テクスチャ名を生成
	 * @param texTarget
	 * @param texUnit テクスチャユニット, GL_TEXTURE0...GL_TEXTURE31
	 * @param min_filter テクスチャの補間方法を指定, GL_LINEARとかGL_NEAREST
	 * @param mag_filter テクスチャの補間方法を指定, GL_LINEARとかGL_NEAREST
	 * @param wrap テクスチャのクランプ方法, GL_CLAMP_TO_EDGE
	 * @return
	 */
	public static int initTex(final int texTarget, final int texUnit, final int min_filter, final int mag_filter, final int wrap) {
//		if (DEBUG) Log.v(TAG, "initTex:target=" + texTarget);
		final int[] tex = new int[1];
		GLES10.glActiveTexture(texUnit);
		GLES10.glGenTextures(1, tex, 0);
		GLES10.glBindTexture(texTarget, tex[0]);
		GLES10.glTexParameterx(texTarget, GLES10.GL_TEXTURE_WRAP_S, wrap);
		GLES10.glTexParameterx(texTarget, GLES10.GL_TEXTURE_WRAP_T, wrap);
		GLES10.glTexParameterx(texTarget, GLES10.GL_TEXTURE_MIN_FILTER, min_filter);
		GLES10.glTexParameterx(texTarget, GLES10.GL_TEXTURE_MAG_FILTER, mag_filter);
		return tex[0];
	}

	/**
	 * テクスチャ名を生成(GL_TEXTURE0のみ)
	 * @param gl
	 * @param texTarget
	 * @param filter_param テクスチャの補間方法を指定 GL_LINEARとかGL_NEAREST
	 * @return
	 */
	public static int initTex(final GL10 gl, final int texTarget, final int filter_param) {
//		if (DEBUG) Log.v(TAG, "initTex:target=" + texTarget);
		final int[] tex = new int[1];
		gl.glActiveTexture(GL10.GL_TEXTURE0);
		gl.glGenTextures(1, tex, 0);
		gl.glBindTexture(texTarget, tex[0]);
		gl.glTexParameterx(texTarget, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
		gl.glTexParameterx(texTarget, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
		gl.glTexParameterx(texTarget, GL10.GL_TEXTURE_MIN_FILTER, filter_param);
		gl.glTexParameterx(texTarget, GL10.GL_TEXTURE_MAG_FILTER, filter_param);
		return tex[0];
	}

	/**
	 * delete specific texture
	 */
	public static void deleteTex(final int hTex) {
//		if (DEBUG) Log.v(TAG, "deleteTex:");
		final int[] tex = new int[] {hTex};
		GLES10.glDeleteTextures(1, tex, 0);
	}

	/**
	 * delete specific texture
	 */
	public static void deleteTex(final GL10 gl, final int hTex) {
//		if (DEBUG) Log.v(TAG, "deleteTex:");
		final int[] tex = new int[] {hTex};
		gl.glDeleteTextures(1, tex, 0);
	}

	public static int loadTextureFromResource(final Context context, final int resId) {
		// Create an empty, mutable bitmap
		final Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
		// get a canvas to paint over the bitmap
		final Canvas canvas = new Canvas(bitmap);
		canvas.drawARGB(0,0,255,0);

		// get a background image from resources
		// note the image format must match the bitmap format
		final Drawable background = context.getResources().getDrawable(resId);
		background.setBounds(0, 0, 256, 256);
		background.draw(canvas); // draw the background to our bitmap

		final int[] textures = new int[1];

		//Generate one texture pointer...
		GLES10.glGenTextures(1, textures, 0);
		//...and bind it to our array
		GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textures[0]);

		//Create Nearest Filtered Texture
		GLES10.glTexParameterx(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST);
		GLES10.glTexParameterx(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);

		//Different possible texture parameters, e.g. GLES10.GL_CLAMP_TO_EDGE
		GLES10.glTexParameterx(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_WRAP_S, GLES10.GL_REPEAT);
		GLES10.glTexParameterx(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_WRAP_T, GLES10.GL_REPEAT);

		//Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
		GLUtils.texImage2D(GLES10.GL_TEXTURE_2D, 0, bitmap, 0);
		//Clean up
		bitmap.recycle();

		return textures[0];
	}

	public static int createTextureWithTextContent (final String text) {
		// Create an empty, mutable bitmap
		final Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
		// get a canvas to paint over the bitmap
		final Canvas canvas = new Canvas(bitmap);
		canvas.drawARGB(0,0,255,0);

		// Draw the text
		final Paint textPaint = new Paint();
		textPaint.setTextSize(32);
		textPaint.setAntiAlias(true);
		textPaint.setARGB(0xff, 0xff, 0xff, 0xff);
		// draw the text centered
		canvas.drawText(text, 16, 112, textPaint);

		final int texture = initTex(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE0, GLES10.GL_NEAREST, GLES10.GL_LINEAR, GLES10.GL_REPEAT);

		// Alpha blending
		// GLES10.glEnable(GLES10.GL_BLEND);
		// GLES10.glBlendFunc(GLES10.GL_SRC_ALPHA, GLES10.GL_ONE_MINUS_SRC_ALPHA);

		// Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
		GLUtils.texImage2D(GLES10.GL_TEXTURE_2D, 0, bitmap, 0);
		// Clean up
		bitmap.recycle();

		return texture;
	}

	/**
	 * Checks to see if the location we obtained is valid.  GLES returns -1 if a label
	 * could not be found, but does not set the GL error.
	 * <p>
	 * Throws a RuntimeException if the location is invalid.
	 */
	public static void checkLocation(final int location, final String label) {
		if (location < 0) {
			throw new RuntimeException("Unable to locate '" + label + "' in program");
		}
	}

	/**
	 * Writes GL version info to the log.
	 */
	@SuppressLint("InlinedApi")
	public static void logVersionInfo() {
		Log.i(TAG, "vendor  : " + GLES10.glGetString(GLES10.GL_VENDOR));
		Log.i(TAG, "renderer: " + GLES10.glGetString(GLES10.GL_RENDERER));
		Log.i(TAG, "version : " + GLES10.glGetString(GLES10.GL_VERSION));

		if (BuildCheck.isAndroid4_3()) {
			final int[] values = new int[1];
			GLES30.glGetIntegerv(GLES30.GL_MAJOR_VERSION, values, 0);
			final int majorVersion = values[0];
			GLES30.glGetIntegerv(GLES30.GL_MINOR_VERSION, values, 0);
			final int minorVersion = values[0];
			if (GLES30.glGetError() == GLES30.GL_NO_ERROR) {
				Log.i(TAG, "version: " + majorVersion + "." + minorVersion);
			}
		}
	}

// came from GLU
	/**
	 * Return an error string from a GL or GLU error code.
	 *
	 * @param error - a GL or GLU error code.
	 * @return the error string for the input error code, or NULL if the input
	 *         was not a valid GL or GLU error code.
	 */
	public static String gluErrorString(final int error) {
		switch (error) {
		case GLES10.GL_NO_ERROR:
			return "no error";
		case GLES10.GL_INVALID_ENUM:
			return "invalid enum";
		case GLES10.GL_INVALID_VALUE:
			return "invalid value";
		case GLES10.GL_INVALID_OPERATION:
			return "invalid operation";
		case GLES10.GL_STACK_OVERFLOW:
			return "stack overflow";
		case GLES10.GL_STACK_UNDERFLOW:
			return "stack underflow";
		case GLES10.GL_OUT_OF_MEMORY:
			return "out of memory";
		default:
			return null;
		}
	}

	/**
	 * Define a viewing transformation in terms of an eye point, a center of
	 * view, and an up vector.
	 *
	 * @param eyeX eye point X
	 * @param eyeY eye point Y
	 * @param eyeZ eye point Z
	 * @param centerX center of view X
	 * @param centerY center of view Y
	 * @param centerZ center of view Z
	 * @param upX up vector X
	 * @param upY up vector Y
	 * @param upZ up vector Z
	 */
	public static void gluLookAt(final float eyeX, final float eyeY, final float eyeZ,
		final float centerX, final float centerY, final float centerZ,
		final float upX, final float upY, final float upZ) {

		final float[] scratch = sScratch;
		synchronized (scratch) {
			Matrix.setLookAtM(scratch, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ,
			upX, upY, upZ);
			GLES10.glMultMatrixf(scratch, 0);
		}
	}

	/**
	 * Set up a 2D orthographic projection matrix
	 *
	 * @param left
	 * @param right
	 * @param bottom
	 * @param top
	 */
	public static void gluOrtho2D(final float left, final float right,
		final float bottom, final float top) {
		GLES10.glOrthof(left, right, bottom, top, -1.0f, 1.0f);
	}

	/**
	 * Set up a perspective projection matrix
	 *
	 * @param fovy specifies the field of view angle, in degrees, in the Y
	 *        direction.
	 * @param aspect specifies the aspect ration that determins the field of
	 *        view in the x direction. The aspect ratio is the ratio of x
	 *        (width) to y (height).
	 * @param zNear specifies the distance from the viewer to the near clipping
	 *        plane (always positive).
	 * @param zFar specifies the distance from the viewer to the far clipping
	 *        plane (always positive).
	 */
	public static void gluPerspective(final float fovy, final float aspect,
		final float zNear, final float zFar) {

		final float top = zNear * (float) Math.tan(fovy * (Math.PI / 360.0));
		final float bottom = -top;
		final float left = bottom * aspect;
		final float right = top * aspect;
		GLES10.glFrustumf(left, right, bottom, top, zNear, zFar);
	}

	/**
	 * Map object coordinates into window coordinates. gluProject transforms the
	 * specified object coordinates into window coordinates using model, proj,
	 * and view. The result is stored in win.
	 * <p>
	 * Note that you can use the OES_matrix_get extension, if present, to get
	 * the current modelView and projection matrices.
	 *
	 * @param objX object coordinates X
	 * @param objY object coordinates Y
	 * @param objZ object coordinates Z
	 * @param model the current modelview matrix
	 * @param modelOffset the offset into the model array where the modelview
	 *        maxtrix data starts.
	 * @param project the current projection matrix
	 * @param projectOffset the offset into the project array where the project
	 *        matrix data starts.
	 * @param view the current view, {x, y, width, height}
	 * @param viewOffset the offset into the view array where the view vector
	 *        data starts.
	 * @param win the output vector {winX, winY, winZ}, that returns the
	 *        computed window coordinates.
	 * @param winOffset the offset into the win array where the win vector data
	 *        starts.
	 * @return A return value of GL_TRUE indicates success, a return value of
	 *         GL_FALSE indicates failure.
	 */
	public static int gluProject(final float objX, final float objY, final float objZ,
		final float[] model, final int modelOffset, final float[] project, final int projectOffset,
		final int[] view, final int viewOffset, final float[] win, final int winOffset) {

		final float[] scratch = sScratch;
		synchronized (scratch) {
			final int M_OFFSET = 0; // 0..15
			final int V_OFFSET = 16; // 16..19
			final int V2_OFFSET = 20; // 20..23
			Matrix.multiplyMM(scratch, M_OFFSET, project, projectOffset, model, modelOffset);

			scratch[V_OFFSET + 0] = objX;
			scratch[V_OFFSET + 1] = objY;
			scratch[V_OFFSET + 2] = objZ;
			scratch[V_OFFSET + 3] = 1.0f;

			Matrix.multiplyMV(scratch, V2_OFFSET, scratch, M_OFFSET, scratch, V_OFFSET);

			final float w = scratch[V2_OFFSET + 3];
			if (w == 0.0f) {
				return GLES10.GL_FALSE;
			}

			final float rw = 1.0f / w;

			win[winOffset] = view[viewOffset]
				+ view[viewOffset + 2]
				* (scratch[V2_OFFSET + 0] * rw + 1.0f)
				* 0.5f;
			win[winOffset + 1] =
				view[viewOffset + 1] + view[viewOffset + 3]
				* (scratch[V2_OFFSET + 1] * rw + 1.0f) * 0.5f;
			win[winOffset + 2] = (scratch[V2_OFFSET + 2] * rw + 1.0f) * 0.5f;
		}

		return GL10.GL_TRUE;
	}

	/**
	 * Map window coordinates to object coordinates. gluUnProject maps the
	 * specified window coordinates into object coordinates using model, proj,
	 * and view. The result is stored in obj.
	 * <p>
	 * Note that you can use the OES_matrix_get extension, if present, to get
	 * the current modelView and projection matrices.
	 *
	 * @param winX window coordinates X
	 * @param winY window coordinates Y
	 * @param winZ window coordinates Z
	 * @param model the current modelview matrix
	 * @param modelOffset the offset into the model array where the modelview
	 *        maxtrix data starts.
	 * @param project the current projection matrix
	 * @param projectOffset the offset into the project array where the project
	 *        matrix data starts.
	 * @param view the current view, {x, y, width, height}
	 * @param viewOffset the offset into the view array where the view vector
	 *        data starts.
	 * @param obj the output vector {objX, objY, objZ}, that returns the
	 *        computed object coordinates.
	 * @param objOffset the offset into the obj array where the obj vector data
	 *        starts.
	 * @return A return value of GL10.GL_TRUE indicates success, a return value
	 *         of GL10.GL_FALSE indicates failure.
	 */
	public static int gluUnProject(final float winX, final float winY, final float winZ,
		final float[] model, final int modelOffset, final float[] project, final int projectOffset,
		final int[] view, final int viewOffset, final float[] obj, final int objOffset) {

		final float[] scratch = sScratch;
		synchronized (scratch) {
			final int PM_OFFSET = 0; // 0..15
			final int INVPM_OFFSET = 16; // 16..31
			final int V_OFFSET = 0; // 0..3 Reuses PM_OFFSET space
			Matrix.multiplyMM(scratch, PM_OFFSET, project, projectOffset, model, modelOffset);

			if (!Matrix.invertM(scratch, INVPM_OFFSET, scratch, PM_OFFSET)) {
				return GL10.GL_FALSE;
			}

			scratch[V_OFFSET + 0] =
				2.0f * (winX - view[viewOffset + 0]) / view[viewOffset + 2]
				- 1.0f;
			scratch[V_OFFSET + 1] =
				2.0f * (winY - view[viewOffset + 1]) / view[viewOffset + 3]
				- 1.0f;
			scratch[V_OFFSET + 2] = 2.0f * winZ - 1.0f;
			scratch[V_OFFSET + 3] = 1.0f;

			Matrix.multiplyMV(obj, objOffset, scratch, INVPM_OFFSET, scratch, V_OFFSET);
		}

		return GL10.GL_TRUE;
	}

	private static final float[] sScratch = new float[32];
}
