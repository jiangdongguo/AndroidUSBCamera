package com.serenegiant.glutils;
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

import android.annotation.SuppressLint;
import android.opengl.GLES20;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import static com.serenegiant.glutils.ShaderConst.*;

/**
 * GL_TEXTURE_EXTERNAL_OESテクスチャを受け取ってSurfaceへ分配描画するクラス
 * RendererHolderにフラグメントシェーダーでのフィルター処理を追加
 * ...カラーマトリックスを掛けるほうがいいかなぁ
 * ...色はuniform変数で渡す方がいいかも
 */
public class EffectRendererHolder extends AbstractRendererHolder {
//	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = EffectRendererHolder.class.getSimpleName();

	private static final int MAX_PARAM_NUM = 18;

	public static final int EFFECT_NON = 0;
	public static final int EFFECT_GRAY = 1;
	public static final int EFFECT_GRAY_REVERSE = 2;
	public static final int EFFECT_BIN = 3;
	public static final int EFFECT_BIN_YELLOW = 4;
	public static final int EFFECT_BIN_GREEN = 5;
	public static final int EFFECT_BIN_REVERSE = 6;
	public static final int EFFECT_BIN_REVERSE_YELLOW = 7;
	public static final int EFFECT_BIN_REVERSE_GREEN = 8;
	/**
	 * 赤色黄色を強調
	 * setParamsはfloat[12] {
	 *    0.17f, 0.85f,		// 赤色&黄色の色相下側閾値, 上側閾値
	 *    0.50f, 1.0f,		// 強調する彩度下限, 上限
	 *    0.40f, 1.0f,		// 強調する明度下限, 上限
	 *    1.0f, 1.0f, 5.0f,	// 強調時のファクター(H, S, Vの順) 明度(x5.0) = 1.0
	 *    1.0f, 0.5f, 0.8f,	// 通常時のファクター(H, S, Vの順) 彩度(x0.5)と明度(x0.8)を少し落とす
	 * }
	 */
	public static final int EFFECT_EMPHASIZE_RED_YELLOW = 9;
	/**
	 * 赤色黄色と白を強調
	 * setParamsはfloat[12] {
	 *    0.17f, 0.85f,		// 赤色&黄色の色相下側閾値, 上側閾値
	 *    0.50f, 1.0f,		// 強調する彩度下限, 上限
	 *    0.40f, 1.0f,		// 強調する明度下限, 上限
	 *    1.0f, 1.0f, 5.0f,	// 強調時のファクター(H, S, Vの順) 明度(x5.0) = 1.0
	 *    1.0f, 0.5f, 0.8f,	// 通常時のファクター(H, S, Vの順) 彩度(x0.5)と明度(x0.8)を少し落とす
	 * 白のパラメータは今はなし
	 */
	public static final int EFFECT_EMPHASIZE_RED_YELLOW_WHITE = 10;
	/**
	 * 黄色と白を強調
	 * setParamsはfloat[12] {
	 *    0.17f, 0.85f,		// 赤色&黄色の色相下側閾値, 上側閾値 FIXME 未調整
	 *    0.50f, 1.0f,		// 強調する彩度下限, 上限
	 *    0.40f, 1.0f,		// 強調する明度下限, 上限
	 *    1.0f, 1.0f, 5.0f,	// 強調時のファクター(H, S, Vの順) 明度(x5.0) = 1.0
	 *    1.0f, 0.5f, 0.8f,	// 通常時のファクター(H, S, Vの順) 彩度(x0.5)と明度(x0.8)を少し落とす
	 * 白のパラメータは今はなし
	 */
	public static final int EFFECT_EMPHASIZE_YELLOW_WHITE = 11;
	/** 内蔵映像効果の数 */
	public static final int EFFECT_NUM = 12;

	/**
	 * グレースケール変換のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_GRAY_BASE = SHADER_VERSION +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"const vec3 conv = vec3(0.3, 0.59, 0.11);\n" +
		"void main() {\n" +
		"    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
		"    float color = dot(tc.rgb, conv);\n" +
		"    vec3 cl3 = vec3(color, color, color);\n" +
		"    gl_FragColor = vec4(cl3, 1.0);\n" +
	"}\n";

	private static final String FRAGMENT_SHADER_GRAY_OES
		= String.format(FRAGMENT_SHADER_GRAY_BASE, HEADER_OES, SAMPLER_OES);

	/**
	 * 白黒反転したグレースケール変換のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_GRAY_REVERSE_BASE = SHADER_VERSION +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"const vec3 conv = vec3(0.3, 0.59, 0.11);\n" +
		"void main() {\n" +
		"    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
		"    float color = dot(tc.rgb, conv);\n" +
		"    vec3 cl3 = vec3(color, color, color);\n" +
		"    gl_FragColor = vec4(clamp(vec3(1.0, 1.0, 1.0) - cl3, 0.0, 1.0), 1.0);\n" +
	"}\n";

	private static final String FRAGMENT_SHADER_GRAY_REVERSE_OES
		= String.format(FRAGMENT_SHADER_GRAY_REVERSE_BASE, HEADER_OES, SAMPLER_OES);

	/**
	 * 2値化のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)、
	 * 変換後の明るい部分用の色を指定するための文字列(R, G, Bの順)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_BIN_BASE = SHADER_VERSION +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"const vec3 conv = vec3(0.3, 0.59, 0.11);\n" +
		"const vec3 cl = vec3(%s);\n" +
		"void main() {\n" +
		"    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
		"    float color = dot(tc.rgb, conv);\n" +
		"    vec3 bin = step(0.3, vec3(color, color, color));\n" +
		"    gl_FragColor = vec4(cl * bin, 1.0);\n" +
	"}\n";

	private static final String FRAGMENT_SHADER_BIN_OES
		= String.format(FRAGMENT_SHADER_BIN_BASE, HEADER_OES, SAMPLER_OES, "1.0, 1.0, 1.0");

	private static final String FRAGMENT_SHADER_BIN_YELLOW_OES
		= String.format(FRAGMENT_SHADER_BIN_BASE, HEADER_OES, SAMPLER_OES, "1.0, 1.0, 0.0");

	private static final String FRAGMENT_SHADER_BIN_GREEN_OES
		= String.format(FRAGMENT_SHADER_BIN_BASE, HEADER_OES, SAMPLER_OES, "0.0, 1.0, 0.0");

	/**
	 * 反転した2値化のためのフラグメントシェーダーのベース文字列
	 * header(HEADER_OESかHEADER_2D)とサンプラーの種類文字列(SAMPLER_OESかSAMPLER_2D)、
	 * 変換後の明るい部分用の色を指定するための文字列(R, G, Bの順)を渡すこと
	 */
	private static final String FRAGMENT_SHADER_BIN_REVERSE_BASE = SHADER_VERSION +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"const vec3 conv = vec3(0.3, 0.59, 0.11);\n" +
		"const vec3 cl = vec3(%s);\n" +
		"void main() {\n" +
		"    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
		"    float color = dot(tc.rgb, conv);\n" +
		"    vec3 bin = step(0.3, vec3(color, color, color));\n" +
		"    gl_FragColor = vec4(cl * (vec3(1.0, 1.0, 1.0) - bin), 1.0);\n" +
	"}\n";

	private static final String FRAGMENT_SHADER_BIN_REVERSE_OES
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE, HEADER_OES, SAMPLER_OES, "1.0, 1.0, 1.0");

	private static final String FRAGMENT_SHADER_BIN_REVERSE_YELLOW_OES
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE, HEADER_OES, SAMPLER_OES, "1.0, 1.0, 0.0");

	private static final String FRAGMENT_SHADER_BIN_REVERSE_GREEN_OES
		= String.format(FRAGMENT_SHADER_BIN_REVERSE_BASE, HEADER_OES, SAMPLER_OES, "0.0, 1.0, 0.0");

	/**
	 * 赤と黄色を強調するためのフラグメントシェーダーのベース文字列
	 */
	private static final String FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_BASE = SHADER_VERSION +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"uniform float uParams[" + MAX_PARAM_NUM + "];\n" +
		FUNC_RGB2HSV +
		FUNC_HSV2RGB +
		"void main() {\n" +
		"    vec3 hsv = rgb2hsv(texture2D(sTexture, vTextureCoord).rgb);\n" +	// RGBをHSVに変換
		"    if ( ((hsv.g >= uParams[2]) && (hsv.g <= uParams[3]))\n" +			// s
		"        && ((hsv.b >= uParams[4]) && (hsv.b <= uParams[5]))\n" +		// v
		"        && ((hsv.r <= uParams[0]) || (hsv.r >= uParams[1])) ) {\n" +	// h
		"        hsv = hsv * vec3(uParams[6], uParams[7], uParams[8]);\n" +		// 赤色と黄色の範囲
		"    } else {\n" +
		"        hsv = hsv * vec3(uParams[9], uParams[10], uParams[11]);\n" +	// それ以外なら
		"    }\n" +
		"    gl_FragColor = vec4(hsv2rgb(clamp(hsv, 0.0, 1.0)), 1.0);\n" +		// HSVをRGBに戻す
	"}\n";

	private static final String FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_OES
		= String.format(FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_BASE, HEADER_OES, SAMPLER_OES);

	/**
	 * 赤と黄色と白色を強調するためのフラグメントシェーダーのベース文字列
	 */
	private static final String FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_BASE = SHADER_VERSION +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"uniform float uParams[" + MAX_PARAM_NUM + "];\n" +
		FUNC_RGB2HSV +
		FUNC_HSV2RGB +
		"void main() {\n" +
		"    vec3 hsv = rgb2hsv(texture2D(sTexture, vTextureCoord).rgb);\n" +	// RGBをHSVに変換
		"    if ( ((hsv.g >= uParams[2]) && (hsv.g <= uParams[3]))\n" +			// s
		"        && ((hsv.b >= uParams[4]) && (hsv.b <= uParams[5]))\n" +		// v
		"        && ((hsv.r <= uParams[0]) || (hsv.r >= uParams[1])) ) {\n" +	// h
		"        hsv = hsv * vec3(uParams[6], uParams[7], uParams[8]);\n" +		// 赤色と黄色の範囲
		"    } else if ((hsv.g < uParams[12]) && (hsv.b < uParams[13])) {\n" +	// 彩度が一定以下, 明度が一定以下なら
		"        hsv = hsv * vec3(1.0, 0.0, 2.0);\n" +							// 色相そのまま, 彩度0, 明度x2
		"    } else {\n" +
		"        hsv = hsv * vec3(uParams[9], uParams[10], uParams[11]);\n" +	// それ以外なら
		"    }\n" +
		"    gl_FragColor = vec4(hsv2rgb(clamp(hsv, 0.0, 1.0)), 1.0);\n" +		// HSVをRGBに戻す
	"}\n";

	private static final String FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_OES
		= String.format(FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_BASE, HEADER_OES, SAMPLER_OES);

	/**
	 * 黄色と白を強調するためのフラグメントシェーダーのベース文字列
	 * 今はFRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_BASEと同じ(違うパラメータ渡せば良いだけなので)
	 */
	private static final String FRAGMENT_SHADER_EMPHASIZE_YELLOW_WHITE_BASE = SHADER_VERSION +
		"%s" +
		"precision mediump float;\n" +
		"varying vec2 vTextureCoord;\n" +
		"uniform %s sTexture;\n" +
		"uniform float uParams[" + MAX_PARAM_NUM + "];\n" +
		FUNC_RGB2HSV +
		FUNC_HSV2RGB +
		"void main() {\n" +
		"    vec3 rgb = texture2D(sTexture, vTextureCoord).rgb;\n" +			// RGB
		"    vec3 hsv = rgb2hsv(rgb);\n" +										// RGBをHSVに変換
		"    if (   ((hsv.r >= uParams[0]) && (hsv.r <= uParams[1]))\n" +		// h
		"        && ((hsv.g >= uParams[2]) && (hsv.g <= uParams[3]))\n" +		// s
		"        && ((hsv.b >= uParams[4]) && (hsv.b <= uParams[5])) ) {\n" +	// v
		"        hsv = hsv * vec3(uParams[6], uParams[7], uParams[8]);\n" +		// 黄色の範囲
		"    } else if ((hsv.g < uParams[12]) && (hsv.b > uParams[13])) {\n" +	// 彩度が一定以下, 明度が一定以上なら
		"        hsv = hsv * vec3(1.0, 0.0, 2.0);\n" +							// 色相そのまま, 彩度0, 明度x2
		"    } else {\n" +
		"        hsv = hsv * vec3(uParams[9], uParams[10], uParams[11]);\n" +	// それ以外なら
		"    }\n" +
		"    gl_FragColor = vec4(hsv2rgb(clamp(hsv, 0.0, 1.0)), 1.0);\n" +		// HSVをRGBに戻す
	"}\n";

	private static final String FRAGMENT_SHADER_EMPHASIZE_YELLOW_WHITE_OES
		= String.format(FRAGMENT_SHADER_EMPHASIZE_YELLOW_WHITE_BASE, HEADER_OES, SAMPLER_OES);

	public EffectRendererHolder(final int width, final int height,
		@Nullable final RenderHolderCallback callback) {

		this(width, height,
			3, null, EglTask.EGL_FLAG_RECORDABLE,
			callback);
	}

	public EffectRendererHolder(final int width, final int height,
		final int maxClientVersion, final EGLBase.IContext sharedContext, final int flags,
		@Nullable final RenderHolderCallback callback) {

		super(width, height,
			maxClientVersion, sharedContext, flags,
			callback);
	}

	@Override
	@NonNull
	protected RendererTask createRendererTask(final int width, final int height,
		final int maxClientVersion, final EGLBase.IContext sharedContext, final int flags) {
		return new MyRendererTask(this, width, height,
			maxClientVersion, sharedContext, flags);
	}

//================================================================================
// クラス固有publicメソッド
//================================================================================
	
	/**
	 * 映像効果をセット
	 * 継承して独自の映像効果を追加する時はEFFECT_NUMよりも大きい値を使うこと
	 * @param effect
	 */
	public void changeEffect(final int effect) {
		((MyRendererTask)mRendererTask).changeEffect(effect);
	}
	
	/**
	 * 現在の映像効果番号を取得
	 * @return
	 */
	public int getCurrentEffect() {
		return ((MyRendererTask)mRendererTask).mEffect;
	}

	/**
	 * 現在選択中の映像フィルタにパラメータ配列をセット
	 * 現在対応しているのは色強調用の映像効果のみ(n=12以上必要)
	 * @param params
	 */
	public void setParams(@NonNull final float[] params) {
		((MyRendererTask)mRendererTask).setParams(-1, params);
	}

	/**
	 * 指定した映像フィルタにパラメータ配列をセット
	 * 現在対応しているのは色強調用の映像効果のみ(n=12以上必要)
	 * @param effect EFFECT_NONより大きいこと
	 * @param params
	 * @throws IllegalArgumentException effectが範囲外ならIllegalArgumentException生成
	 */
	public void setParams(final int effect, @NonNull final float[] params)
		throws IllegalArgumentException {

		if (effect > EFFECT_NON) {
			((MyRendererTask)mRendererTask).setParams(effect, params);
		} else {
			throw new IllegalArgumentException("invalid effect number:" + effect);
		}
	}
	
	/**
	 * 内蔵映像効果以外のeffectを指定したときの処理
	 * 描画用のワーカースレッド上で呼び出される
	 * このクラスでは無変換のフラグメントシェーダーを適用する
	 * @param effect
	 * @param drawer GLDrawer2Dインスタンス
	 */
	protected void handleDefaultEffect(final int effect,
		@NonNull final IDrawer2dES2 drawer) {

		if (drawer instanceof GLDrawer2D) {
			((GLDrawer2D) drawer).resetShader();
		}
	}
	
//================================================================================
// 実装
//================================================================================
	private static final int REQUEST_CHANGE_EFFECT = 100;
	private static final int REQUEST_SET_PARAMS = 101;

	/**
	 * ワーカースレッド上でOpenGL|ESを用いてマスター映像を分配描画するためのインナークラス
	 */
	protected static final class MyRendererTask extends RendererTask {

		private final SparseArray<float[]> mParams = new SparseArray<float[]>();
		private int muParamsLoc;
		private float[] mCurrentParams;
		private int mEffect;

		public MyRendererTask(final EffectRendererHolder parent,
			final int width, final int height) {

			super(parent, width, height);
		}

		public MyRendererTask(@NonNull final AbstractRendererHolder parent,
			final int width, final int height,
			final int maxClientVersion,
			final EGLBase.IContext sharedContext, final int flags) {
			
			super(parent, width, height, maxClientVersion, sharedContext, flags);
		}

		/**
		 * ワーカースレッド開始時の処理(ここはワーカースレッド上)
		 */
		@SuppressLint("NewApi")
		@Override
		protected void internalOnStart() {
//			if (DEBUG) Log.v(TAG, "onStart:");
			super.internalOnStart();
			mParams.clear();
			mParams.put(EFFECT_EMPHASIZE_RED_YELLOW, new float[] {
				0.17f, 0.85f,		// 赤色&黄色の色相下側閾値, 上側閾値
				0.50f, 1.0f,		// 強調する彩度下限, 上限
				0.40f, 1.0f,		// 強調する明度下限, 上限
				1.0f, 1.0f, 5.0f,	// 強調時のファクター(H, S, Vの順) 明度(x5.0) = 1.0
				1.0f, 1.0f, 1.0f,	// 通常時のファクター(H, S, Vの順)
			});
			mParams.put(EFFECT_EMPHASIZE_RED_YELLOW_WHITE, new float[] {
				0.17f, 0.85f,		// 赤色&黄色の色相下側閾値, 上側閾値
				0.50f, 1.0f,		// 強調する彩度下限, 上限
				0.40f, 1.0f,		// 強調する明度下限, 上限
				1.0f, 1.0f, 5.0f,	// 強調時のファクター(H, S, Vの順) 明度(x5.0) = 1.0
				1.0f, 1.0f, 1.0f,	// 通常時のファクター(H, S, Vの順)
			});
			mParams.put(EFFECT_EMPHASIZE_YELLOW_WHITE, new float[] {
				0.10f, 0.19f,			// 黄色の色相h下側閾値, 上側閾値
				0.30f, 1.00f,			// 強調する彩度s下限, 上限
				0.30f, 1.00f,			// 強調する明度v下限, 上限
				1.00f, 1.00f, 5.00f,	// 強調時のファクター(H, S, Vの順) 明度(x5.0) = 1.0
				1.00f, 0.80f, 0.80f,	// 通常時のファクター(H, S, Vの順) 彩度(x0.8)と明度(x0.8)を少し落とす
				0.15f, 0.40f,			// 白強調時の彩度上限, 白強調時の明度下限
				0, 0, 0, 0,				// ダミー
			});
			mEffect = EFFECT_NON;
			handleChangeEffect(EFFECT_NON);
//			if (DEBUG) Log.v(TAG, "onStart:finished");
		}

		@Override
		protected Object processRequest(final int request,
			final int arg1, final int arg2, final Object obj) {

			Object result = null;
			switch (request) {
			case REQUEST_CHANGE_EFFECT:
				handleChangeEffect(arg1);
				break;
			case REQUEST_SET_PARAMS:
				handleSetParam(arg1, (float[])obj);
				break;
			default:
				result = super.processRequest(request, arg1, arg2, obj);
				break;
			}
			return result;
		}

		public void changeEffect(final int effect) {
			checkFinished();
			if (mEffect != effect) {
				offer(REQUEST_CHANGE_EFFECT, effect);
			}
		}

		public void setParams(final int effect, @NonNull final float[] params) {
			checkFinished();
			offer(REQUEST_SET_PARAMS, effect, 0, params);
		}

//================================================================================
// ワーカースレッド上での処理
//================================================================================
		/**
		 * 映像効果を変更
		 * @param effect
		 */
		protected void handleChangeEffect(final int effect) {
			mEffect = effect;
			switch (effect) {
			case EFFECT_NON:
				mDrawer.updateShader(FRAGMENT_SHADER_SIMPLE_OES);
				break;
			case EFFECT_GRAY:
				mDrawer.updateShader(FRAGMENT_SHADER_GRAY_OES);
				break;
			case EFFECT_GRAY_REVERSE:
				mDrawer.updateShader(FRAGMENT_SHADER_GRAY_REVERSE_OES);
				break;
			case EFFECT_BIN:
				mDrawer.updateShader(FRAGMENT_SHADER_BIN_OES);
				break;
			case EFFECT_BIN_YELLOW:
				mDrawer.updateShader(FRAGMENT_SHADER_BIN_YELLOW_OES);
				break;
			case EFFECT_BIN_GREEN:
				mDrawer.updateShader(FRAGMENT_SHADER_BIN_GREEN_OES);
				break;
			case EFFECT_BIN_REVERSE:
				mDrawer.updateShader(FRAGMENT_SHADER_BIN_REVERSE_OES);
				break;
			case EFFECT_BIN_REVERSE_YELLOW:
				mDrawer.updateShader(FRAGMENT_SHADER_BIN_REVERSE_YELLOW_OES);
				break;
			case EFFECT_BIN_REVERSE_GREEN:
				mDrawer.updateShader(FRAGMENT_SHADER_BIN_REVERSE_GREEN_OES);
				break;
			case EFFECT_EMPHASIZE_RED_YELLOW:
				mDrawer.updateShader(FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_OES);
				break;
			case EFFECT_EMPHASIZE_RED_YELLOW_WHITE:
				mDrawer.updateShader(FRAGMENT_SHADER_EMPHASIZE_RED_YELLOW_WHITE_OES);
				break;
			case EFFECT_EMPHASIZE_YELLOW_WHITE:
				mDrawer.updateShader(FRAGMENT_SHADER_EMPHASIZE_YELLOW_WHITE_OES);
				break;
			default:
				try {
					((EffectRendererHolder)getParent())
						.handleDefaultEffect(effect, mDrawer);
				} catch (final Exception e) {
					mDrawer.resetShader();
					Log.w(TAG, e);
				}
				break;
			}
			muParamsLoc = mDrawer.glGetUniformLocation("uParams");
			mCurrentParams = mParams.get(effect);
			updateParams();
		}
		
		/**
		 * 映像効果用のパラメーターをセット
		 * @param effect
		 * @param params
		 */
		private void handleSetParam(final int effect, @NonNull final float[] params) {
			if ((effect < EFFECT_NON) || (mEffect == effect)) {
				mCurrentParams = params;
				mParams.put(mEffect, params);
				updateParams();
			} else {
				mParams.put(effect, params);
			}
		}
		
		/**
		 * 映像効果用のパラメータをGPUへ適用
		 */
		private void updateParams() {
			final int n = Math.min(mCurrentParams != null
				? mCurrentParams.length : 0, MAX_PARAM_NUM);
			if ((muParamsLoc >= 0) && (n > 0)) {
				mDrawer.glUseProgram();
				GLES20.glUniform1fv(muParamsLoc, n, mCurrentParams, 0);
			}
		}

	}

}
