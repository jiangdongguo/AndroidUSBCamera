package com.serenegiant.glutils;
/*
 * Copyright 2014 Google Inc. All rights reserved.
 * Modified 2014-2018 t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;
import android.view.MotionEvent;

import java.nio.FloatBuffer;

import static com.serenegiant.glutils.ShaderConst.*;

/**
 * GL program and supporting functions for textured 2D shapes.
 */
public class Texture2dProgram {
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
    private static final String TAG = "Texture2dProgram";

	public enum ProgramType {
		// ここはGL_TEXTURE_2D
        TEXTURE_2D,
//		TEXTURE_SOBEL,		// フラグメントシェーダーがうまく走らなくて止まってしまう
//		TEXTURE_SOBEL2,		// フラグメントシェーダーがうまく走らなくて止まってしまう
        TEXTURE_FILT3x3,
        TEXTURE_CUSTOM,
        // ここから下はGL_TEXTURE_EXTERNAL_OES
		TEXTURE_EXT,
		TEXTURE_EXT_BW,
		TEXTURE_EXT_NIGHT,
		TEXTURE_EXT_CHROMA_KEY,
        TEXTURE_EXT_SQUEEZE,
        TEXTURE_EXT_TWIRL,
        TEXTURE_EXT_TUNNEL,
        TEXTURE_EXT_BULGE,
        TEXTURE_EXT_DENT,
        TEXTURE_EXT_FISHEYE,
        TEXTURE_EXT_STRETCH,
        TEXTURE_EXT_MIRROR,
//		TEXTURE_EXT_SOBEL,		// フラグメントシェーダーがうまく走らなくて止まってしまう
//		TEXTURE_EXT_SOBEL2,		// フラグメントシェーダーがうまく走らなくて止まってしまう
		TEXTURE_EXT_FILT3x3,
    }

	private final Object mSync = new Object();
	private final ProgramType mProgramType;

    private float mTexWidth;
    private float mTexHeight;

    // Handles to the GL program and various components of it.
    private int mProgramHandle;
    private final int muMVPMatrixLoc;	// モデルビュー変換行列
    private final int muTexMatrixLoc;	// テクスチャ行列
	private final int maPositionLoc;	//
	private final int maTextureCoordLoc;//
    private int muKernelLoc;			// カーネル行列(float配列)
    private int muTexOffsetLoc;			// テクスチャオフセット(カーネル行列用)
    private int muColorAdjustLoc;		// 色調整
    private int muTouchPositionLoc;
	private int muFlagsLoc;

    private int mTextureTarget;

	protected boolean mHasKernel2;
	/** Inputs for convolution filter based shaders */
    private final float[] mKernel = new float[KERNEL_SIZE3x3 * 2];
    /** Summed touch event delta */
    private final float[] mSummedTouchPosition = new float[2];
    /** Raw location of last touch event */
    private final float[] mLastTouchPosition = new float[2];
    private float[] mTexOffset;
    private float mColorAdjust;
    private final int[] mFlags = new int[4];

	public Texture2dProgram(final int target, final String fss) {
		this(ProgramType.TEXTURE_CUSTOM, target, VERTEX_SHADER, fss);
	}

	public Texture2dProgram(final int target, final String vss, final String fss) {
		this(ProgramType.TEXTURE_CUSTOM, target, vss, fss);
	}

	public Texture2dProgram(final ProgramType programType) {
		this(programType, 0, null, null);
	}

    /**
     * Prepares the program in the current EGL context.
     */
    protected Texture2dProgram(final ProgramType programType,
    	final int target, final String vss, final String fss) {

		mProgramType = programType;

		float[] kernel = null, kernel2 = null;
		switch (programType) {
			case TEXTURE_2D:
				mTextureTarget = GLES20.GL_TEXTURE_2D;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_2D);
                break;
//			case TEXTURE_SOBEL:
//				mTextureTarget = GLES20.GL_TEXTURE_2D;
//				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_SOBEL);
//				kernel = KERNEL_SOBEL_H;
//				kernel2 = KERNEL_SOBEL_V;
//				break;
//			case TEXTURE_SOBEL2:
//				mTextureTarget = GLES20.GL_TEXTURE_2D;
//				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_SOBEL);
//				kernel = KERNEL_SOBEL2_H;
//				kernel2 = KERNEL_SOBEL2_V;
//				break;
			case TEXTURE_FILT3x3:
				mTextureTarget = GLES20.GL_TEXTURE_2D;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_FILT3x3);
				break;
			case TEXTURE_EXT:
				mTextureTarget = GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT);
				break;
			case TEXTURE_EXT_BW:
				mTextureTarget = GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_BW);
				break;
			case TEXTURE_EXT_NIGHT:
				mTextureTarget = GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_NIGHT);
				break;
			case TEXTURE_EXT_CHROMA_KEY:
				mTextureTarget = GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_CHROMA_KEY);
				break;
			case TEXTURE_EXT_SQUEEZE:
				mTextureTarget = GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_SQUEEZE);
				break;
			case TEXTURE_EXT_TWIRL:
				mTextureTarget = GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_TWIRL);
				break;
			case TEXTURE_EXT_TUNNEL:
				mTextureTarget = GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_TUNNEL);
				break;
			case TEXTURE_EXT_BULGE:
				mTextureTarget = GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_BULGE);
				break;
			case TEXTURE_EXT_FISHEYE:
				mTextureTarget = GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_FISHEYE);
				break;
			case TEXTURE_EXT_DENT:
				mTextureTarget = GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_DENT);
				break;
			case TEXTURE_EXT_MIRROR:
				mTextureTarget = GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_MIRROR);
				break;
			case TEXTURE_EXT_STRETCH:
				mTextureTarget = GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_STRETCH);
				break;
//			case TEXTURE_EXT_SOBEL:
//				mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
//				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_SOBEL);
//				kernel = KERNEL_SOBEL_H;
//				kernel2 = KERNEL_SOBEL_V;
//				break;
//			case TEXTURE_EXT_SOBEL2:
//				mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
//				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_SOBEL);
//				kernel = KERNEL_SOBEL2_H;
//				kernel2 = KERNEL_SOBEL2_V;
//				break;
			case TEXTURE_EXT_FILT3x3:
				mTextureTarget = GL_TEXTURE_EXTERNAL_OES;
				mProgramHandle = GLHelper.loadShader(VERTEX_SHADER, FRAGMENT_SHADER_EXT_FILT3x3);
				break;
			case TEXTURE_CUSTOM:
				switch (target) {
				case GLES20.GL_TEXTURE_2D:
				case GLES11Ext.GL_TEXTURE_EXTERNAL_OES:
					break;
				default:
					throw new IllegalArgumentException(
						"target should be GL_TEXTURE_2D or GL_TEXTURE_EXTERNAL_OES");
				}
				mTextureTarget = target;
				mProgramHandle = GLHelper.loadShader(vss, fss);
				break;
			default:
				throw new RuntimeException("Unhandled type " + programType);
		}
        if (mProgramHandle == 0) {
            throw new RuntimeException("Unable to create program");
        }
        if (DEBUG) Log.d(TAG, "Created program " + mProgramHandle + " (" + programType + ")");

        // get locations of attributes and uniforms
		maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
		GLHelper.checkLocation(maPositionLoc, "aPosition");
		maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
		GLHelper.checkLocation(maTextureCoordLoc, "aTextureCoord");
		muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
		GLHelper.checkLocation(muMVPMatrixLoc, "uMVPMatrix");
		muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
//		GLHelper.checkLocation(muTexMatrixLoc, "uTexMatrix");
        initLocation(kernel, kernel2);

    }

    /**
     * Releases the program.
     */
    public void release() {
        if (DEBUG) Log.d(TAG, "deleting program " + mProgramHandle);
        GLES20.glDeleteProgram(mProgramHandle);
        mProgramHandle = -1;
    }

    /**
     * Returns the program type.
     */
    public ProgramType getProgramType() {
        return mProgramType;
    }

	public int getProgramHandle() {
		return mProgramHandle;
	}

    /**
     * Creates a texture object suitable for use with this program.
     * <p>
     * On exit, the texture will be bound.
     */
    public int createTextureObject() {
        final int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
		GLHelper.checkGlError("glGenTextures");

        final int texId = textures[0];
        GLES20.glBindTexture(mTextureTarget, texId);
		GLHelper.checkGlError("glBindTexture " + texId);

        GLES20.glTexParameterf(mTextureTarget,
        	GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(mTextureTarget,
        	GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(mTextureTarget,
        	GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(mTextureTarget,
        	GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		GLHelper.checkGlError("glTexParameter");

        return texId;
    }

    /**
     * Configures the effect offset
     *
     * This only has an effect for programs that
     * use positional effects like SQUEEZE and MIRROR
     */
    public void handleTouchEvent(final MotionEvent ev){
		synchronized (mSync) {
			if (ev.getAction() == MotionEvent.ACTION_MOVE){
				// A finger is dragging about
				if (mTexHeight != 0 && mTexWidth != 0){
					mSummedTouchPosition[0]
						+= (2 * (ev.getX() - mLastTouchPosition[0])) / mTexWidth;
					mSummedTouchPosition[1]
						+= (2 * (ev.getY() - mLastTouchPosition[1])) / -mTexHeight;
					mLastTouchPosition[0] = ev.getX();
					mLastTouchPosition[1] = ev.getY();
				}
			} else if (ev.getAction() == MotionEvent.ACTION_DOWN){
				// The primary finger has landed
				mLastTouchPosition[0] = ev.getX();
				mLastTouchPosition[1] = ev.getY();
			}
		}
    }

    /**
     * Configures the convolution filter values.
     * This only has an effect for programs that use the
     * FRAGMENT_SHADER_EXT_FILT3x3 Fragment shader.
     *
     * @param values Normalized filter values; must be KERNEL_SIZE3x3 elements.
     */
    public void setKernel(final float[] values, final float colorAdj) {
        if (values.length < KERNEL_SIZE3x3) {
            throw new IllegalArgumentException(
            	"Kernel size is " + values.length + " vs. " + KERNEL_SIZE3x3);
        }
        System.arraycopy(values, 0, mKernel, 0, KERNEL_SIZE3x3);
        mColorAdjust = colorAdj;
    }

	public void setKernel2(final float[] values) {
		synchronized (mSync) {
			mHasKernel2 = values != null && (values.length == KERNEL_SIZE3x3);
			if (mHasKernel2) {
				System.arraycopy(values, 0, mKernel, KERNEL_SIZE3x3, KERNEL_SIZE3x3);
			}
		}
	}

	public void setColorAdjust(final float adjust) {
		synchronized (mSync) {
			mColorAdjust = adjust;
		}
	}

    /**
     * Sets the size of the texture.  This is used to find adjacent texels when filtering.
     */
    public void setTexSize(final int width, final int height) {
        mTexHeight = height;
        mTexWidth = width;
        final float rw = 1.0f / width;
        final float rh = 1.0f / height;

        // Don't need to create a new array here, but it's syntactically convenient.
		synchronized (mSync) {
			mTexOffset = new float[] {
					-rw, -rh,   0f, -rh,    rw, -rh,
					-rw, 0f,    0f, 0f,     rw, 0f,
					-rw, rh,    0f, rh,     rw, rh
			};
		}
    }

	public void setFlags(final int[] flags) {
		final int n = Math.min(4, flags != null ? flags.length : 0);
		if (n > 0) {
			synchronized (mSync) {
				System.arraycopy(flags, 0, mFlags, 0, n);
			}
		}
	}

	public void setFlag(final int index, final int value) {
		if ((index >= 0) && (index < mFlags.length)) {
			synchronized (mSync) {
				mFlags[index] = value;
			}
		}
	}

    /**
     * Issues the draw call.  Does the full setup on every call.
     *
     * @param mvpMatrix The 4x4 projection matrix.
	 * @param mvpMatrixOffset offset of mvpMatrix
     * @param vertexBuffer Buffer with vertex position data.
     * @param firstVertex Index of first vertex to use in vertexBuffer.
     * @param vertexCount Number of vertices in vertexBuffer.
     * @param coordsPerVertex The number of coordinates per vertex (e.g. x,y is 2).
     * @param vertexStride Width, in bytes, of the position data for each vertex (often
     *        vertexCount * sizeof(float)).
     * @param texMatrix A 4x4 transformation matrix for texture coords.
	 * @param texMatrixOffset offset of texMatrix
     * @param texBuffer Buffer with vertex texture data.
     * @param texStride Width, in bytes, of the texture data for each vertex.
     */
    public void draw(final float[] mvpMatrix, final int mvpMatrixOffset,
    	final FloatBuffer vertexBuffer, final int firstVertex,
        final int vertexCount, final int coordsPerVertex, final int vertexStride,
        final float[] texMatrix, final int texMatrixOffset,
        final FloatBuffer texBuffer, final int textureId, final int texStride) {

		GLHelper.checkGlError("draw start");

        // シェーダープログラムを選択
        GLES20.glUseProgram(mProgramHandle);
		GLHelper.checkGlError("glUseProgram");

        // テクスチャを選択
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mTextureTarget, textureId);
		GLHelper.checkGlError("glBindTexture");

		synchronized (mSync) {
			// モデルビュー変換行列をセット
			GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, mvpMatrixOffset);
			GLHelper.checkGlError("glUniformMatrix4fv");

			// テクスチャ変換行列をセット
			if (muTexMatrixLoc >= 0) {
				GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, texMatrixOffset);
				GLHelper.checkGlError("glUniformMatrix4fv");
			}

			// 頂点座標バッファを有効にする("aPosition" vertex attribute)
			GLES20.glEnableVertexAttribArray(maPositionLoc);
			GLHelper.checkGlError("glEnableVertexAttribArray");
			GLES20.glVertexAttribPointer(maPositionLoc, coordsPerVertex,
					GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
			GLHelper.checkGlError("glVertexAttribPointer");

			// テクスチャ座標バッファを有効にする("aTextureCoord" vertex attribute)
			GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
			GLHelper.checkGlError("glEnableVertexAttribArray");
			GLES20.glVertexAttribPointer(maTextureCoordLoc, 2,
					GLES20.GL_FLOAT, false, texStride, texBuffer);
			GLHelper.checkGlError("glVertexAttribPointer");

			// カーネル関数(行列)
			if (muKernelLoc >= 0) {
				if (!mHasKernel2) {
					GLES20.glUniform1fv(muKernelLoc, KERNEL_SIZE3x3, mKernel, 0);
				} else {
					GLES20.glUniform1fv(muKernelLoc, KERNEL_SIZE3x3 * 2, mKernel, 0);
				}
				GLHelper.checkGlError("set kernel");
			}
			// テクセルオフセット
			if ((muTexOffsetLoc >= 0) && (mTexOffset != null)) {
				GLES20.glUniform2fv(muTexOffsetLoc, KERNEL_SIZE3x3, mTexOffset, 0);
			}
			// 色調整オフセット
			if (muColorAdjustLoc >= 0) {
				GLES20.glUniform1f(muColorAdjustLoc, mColorAdjust);
			}
			// タッチ座標
			if (muTouchPositionLoc >= 0){
				GLES20.glUniform2fv(muTouchPositionLoc, 1, mSummedTouchPosition, 0);
			}
			// フラグ
			if (muFlagsLoc >= 0) {
				GLES20.glUniform1iv(muFlagsLoc, 4, mFlags, 0);
			}
		}

		internal_draw(firstVertex, vertexCount);

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES20.glBindTexture(mTextureTarget, 0);
        GLES20.glUseProgram(0);
    }

	protected void initLocation(float[] kernel, float[] kernel2) {
		muKernelLoc = GLES20.glGetUniformLocation(mProgramHandle, "uKernel");
		if (muKernelLoc < 0) {
			// no kernel in this one
			muKernelLoc = -1;
			muTexOffsetLoc = -1;
		} else {
			// has kernel, must also have tex offset and color adj
			muTexOffsetLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexOffset");
			if (muTexOffsetLoc < 0) {
				muTexOffsetLoc = -1;
			}
			// 未使用だと削除されてしまうのでチェックしない
//			GLHelper.checkLocation(muTexOffsetLoc, "uTexOffset");

			// initialize default values
			if (kernel == null) {
				kernel = KERNEL_NULL;
			}
			setKernel(kernel, 0f);
			setTexSize(256, 256);
			}
			if (kernel2 != null) {
				setKernel2(kernel2);
			}

			muColorAdjustLoc = GLES20.glGetUniformLocation(mProgramHandle, "uColorAdjust");
			if (muColorAdjustLoc < 0) {
				muColorAdjustLoc = -1;
			}
			// 未使用だと削除されてしまうのでチェックしない
//			GLHelper.checkLocation(muColorAdjustLoc, "uColorAdjust");

			muTouchPositionLoc = GLES20.glGetUniformLocation(mProgramHandle, "uPosition");
			if (muTouchPositionLoc < 0) {
				// Shader doesn't use position
				muTouchPositionLoc = -1;
			} else {
				// initialize default values
				//handleTouchEvent(new float[]{0f, 0f});
			}
			muFlagsLoc = GLES20.glGetUniformLocation(mProgramHandle, "uFlags");
			if (muFlagsLoc < 0) {
				muFlagsLoc = -1;
			} else {
		}
	}

    protected void internal_draw(final int firstVertex, final int vertexCount) {
		// Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
		GLHelper.checkGlError("glDrawArrays");
	}
}