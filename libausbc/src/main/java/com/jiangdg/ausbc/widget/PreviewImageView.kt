/*
 * Copyright 2017-2023 Jiangdg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jiangdg.ausbc.widget

import android.animation.*
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageView
import com.jiangdg.ausbc.utils.Logger

/**
 * author: jiangdg
 * date: 2020/11/4 3:16 PM
 * description: Preview ImageView
 */
class PreviewImageView: AppCompatImageView {

    private var isNewImageLoading: Boolean = false
    private var mProgressAnim: ValueAnimator? = null
    private lateinit var mProgressPath: Path
    private lateinit var mProgressPathMeasure: PathMeasure
    private var mProgressDstPath: Path? = null

    private var mBreathAnimation: ObjectAnimator? = null
    private val mPaint = Paint()
    private var progress = 0f
    private var mListener: OnLoadingFinishListener? = null
    private var mTheme = Theme.LIGHT

    private val mSrcRadii =  FloatArray(8)
    private val mBorderRadii = FloatArray(8)

    private val mBorderRectF: RectF = RectF()
    private val mSrcRectF: RectF = RectF()
    private var mTmpPath: Path = Path()
    private val mClipPath: Path = Path()
    private lateinit var mXfermode: PorterDuffXfermode
    private val cornerRadius = dp2px(5f)
    private val borderWidth = dp2px(1f)

    var canShowImageBorder = false
        set(value) {
            field = value
            postInvalidate()
        }

    enum class Theme {
        LIGHT, DARK
    }

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
            context!!,
            attrs,
            defStyleAttr
    ) {
        init()
    }

    interface OnLoadingFinishListener {
        fun onLoadingFinish()
    }

    private fun init() {
        // 设置PorterDuff模式
        mXfermode = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
            PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        } else {
            PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
        // 计算圆角半径
        for (i in mBorderRadii.indices) {
            mBorderRadii[i] = cornerRadius
            mSrcRadii[i] = cornerRadius - borderWidth / 2.0f
        }
    }

    private fun initBorderPath(w: Int, h: Int) {
        // 计算外边框的RectF
        mBorderRectF.set(borderWidth/2.0f, borderWidth/2.0f, w - borderWidth / 2.0f, h - borderWidth / 2.0f)
        // 计算图片原始区域的RectF
        mSrcRectF.set(0.0f, 0.0f, w.toFloat(), h.toFloat())
    }

    private fun initProgressPath(w: Int, h: Int) {
        val radius = cornerRadius   // 圆角半径
        val p = borderWidth / 2.0f     // 边距
        val rectFLeftTop = RectF(p, p, (p + 2 * radius), (p + 2 * radius))
        val rectFRightTop = RectF((w - p - 2 * radius), p, (w - p), (2 * radius + p))
        val rectFLeftBottom = RectF(p, (h - p - 2 * radius), (p + 2 * radius), (h - p))
        val rectFRightBottom = RectF((w - p - 2 * radius), (h - p - 2 * radius), (w - p), (h - p))
        mProgressPath = Path().apply {
            moveTo(w / 2f - p, p)
            // 右上角
            lineTo(w - radius - p, p)
            arcTo(rectFRightTop, 270f, 90f)
            // 右下角
            lineTo(w - p, h - p - radius)
            arcTo(rectFRightBottom, 0f, 90f)
            // 左下角
            lineTo(p + radius, h - p)
            arcTo(rectFLeftBottom, 90f, 90f)
            // 左上角
            lineTo(p, p + radius)
            arcTo(rectFLeftTop, 180f, 90f)
            lineTo(w / 2f - p, p)
        }
        mProgressDstPath = Path()
        mProgressPathMeasure = PathMeasure(mProgressPath, true)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initProgressPath(w, h)
        initBorderPath(w, h)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDraw(canvas: Canvas?) {
        try {
            canvas?.saveLayer(mSrcRectF, null)
            // 缩小画布
//            val sx = 1.0f * (width - borderWidth) / width
//            val sy  = 1.0f * (height - borderWidth) / height
//            canvas?.scale(sx, sy, width / 2.0f, height / 2.0f)

            // 绘制原图
            super.onDraw(canvas)

            // 裁剪，得到圆角原图
            mPaint.reset()
            mPaint.isAntiAlias = true
            mPaint.style = Paint.Style.FILL
            mPaint.color = Color.parseColor("#FFFFFF")
            mPaint.xfermode = mXfermode
            mClipPath.reset()
            mClipPath.addRoundRect(mSrcRectF, mSrcRadii, Path.Direction.CCW)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                canvas?.drawPath(mClipPath, mPaint)
            } else {
                mTmpPath.reset()
                mTmpPath.addRect(mSrcRectF, Path.Direction.CCW)
                mTmpPath.op(mClipPath, Path.Op.DIFFERENCE)
                canvas?.drawPath(mTmpPath, mPaint)
            }
            mPaint.xfermode = null
            canvas?.restore()

            // 绘制边框
            drawBorders(canvas)
            // 绘制进度条
            drawBorderProgress(canvas)
        } catch (e: Exception) {
            Logger.e(TAG, "draw preview image view failed", e)
            e.printStackTrace()
        }
    }

    private fun drawBorders(canvas: Canvas?) {
        if (mTheme == Theme.LIGHT || !canShowImageBorder) return
        mClipPath.reset()
        mPaint.isAntiAlias = true
        mPaint.strokeWidth = borderWidth
        mPaint.color = Color.parseColor("#FFFFFF")
        mPaint.style = Paint.Style.STROKE
        mClipPath.addRoundRect(mBorderRectF, mBorderRadii, Path.Direction.CCW)
        canvas?.drawPath(mClipPath, mPaint)
    }

    private fun drawBorderProgress(canvas: Canvas?) {
        mPaint.reset()
        mPaint.isAntiAlias = true
        mPaint.strokeWidth = borderWidth
        mPaint.style = Paint.Style.STROKE
        mPaint.color = Color.parseColor("#2E5BFF")

        mProgressDstPath?.let {
            if (! it.isEmpty) {
                canvas?.drawPath(it, mPaint)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelAnimation()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        drawable?.let {
            if (isAnimationRunning()) {
                mProgressAnim?.cancel()
                mProgressAnim = null
            }
            if (isNewImageLoading || getProgress() != 0.0f) {//解决setImageDrawable多次調用會闪动
                if ((mBreathAnimation == null || mBreathAnimation?.isRunning == false)) {
                    showBreathAnimation()
                }
            }
        }
    }

    fun cancelAnimation() {
        mBreathAnimation?.cancel()
        mBreathAnimation = null
        mProgressAnim?.cancel()
        mProgressAnim = null
        setProgress(0.0f)
        isNewImageLoading = false
        mListener?.onLoadingFinish()
    }

    fun setNewImageFlag(isNewImage: Boolean) {
        this.isNewImageLoading = isNewImage
    }

    fun showImageLoadProgress(isShowFakeProgress: Boolean = true) {
        if (isAnimationRunning()) {
            return
        }
        if (! isShowFakeProgress) {
            showBreathAnimation()
            return
        }
        // 假进度
        mProgressAnim = ValueAnimator.ofFloat(0.0f, 0.7f)
        mProgressAnim?.interpolator = LinearInterpolator()
        mProgressAnim?.duration = 3000
        mProgressAnim?.addUpdateListener {
            val value = it.animatedValue
            if (value is Float) {
                setProgress(value)
            }
        }
        mProgressAnim?.start()
    }

    fun setOnLoadingFinishListener(listener: OnLoadingFinishListener?) {
        this.mListener = listener
    }

    fun setTheme(theme: Theme) {
        this.mTheme = theme
    }

    private fun showBreathAnimation() {
        if (isAnimationRunning()) {
            return
        }
        val scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.2f, 1.0f)
        val scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.2f, 1.0f)
        val progress = PropertyValuesHolder.ofFloat("progress", getProgress(), 1.0f)
        mBreathAnimation = ObjectAnimator.ofPropertyValuesHolder(this,  scaleX,  scaleY, progress).apply {
            addListener(object :AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    isNewImageLoading = false
                    setProgress(0.0f)
                    mBreathAnimation = null
                    mListener?.onLoadingFinish()
                }
            })
            duration = 150
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun isAnimationRunning() =  mProgressAnim?.isRunning ==true || mBreathAnimation?.isRunning == true

    private fun setProgress(progress: Float) {
        this.progress = progress
        updateBorderProgress()
    }

    private fun getProgress(): Float {
        return progress
    }

    private fun updateBorderProgress() {
        mProgressDstPath?.let {
            it.reset()
            mProgressPathMeasure.getSegment(0f,
                    mProgressPathMeasure.length * progress, it, true)
            invalidate()
        }
    }

    private fun dp2px(dpValue: Float): Float {
        val scale = resources.displayMetrics.density
        return dpValue * scale + 0.5f
    }

    companion object {
        private const val TAG = "PreviewImageView"
    }
}