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

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.jiangdg.ausbc.R
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/** Custom capture view
 *
 * @author Created by jiangdg on 2022/2/9
 */
class CaptureMediaView : View {
    private var mAnimator: ObjectAnimator? = null

    enum class CaptureVideoState {
        DOING, UNDO, PAUSE
    }

    enum class CaptureMode {
        MODE_CAPTURE_PIC, MODE_CAPTURE_VIDEO, MODE_CAPTURE_AUDIO
    }

    enum class CaptureViewTheme {
        THEME_BLUE, THEME_WHITE
    }

    private lateinit var mPaint: Paint
    private var mWidth = 0
    private var mHeight = 0
    private var circleX = 0
    private var circleY = 0
    private var radius = 0
    private var mCaptureVideoState: CaptureVideoState? = null
    private var mCaptureModel: CaptureMode? = null
    private var mCaptureViewTheme: CaptureViewTheme? = null
    private var mCaptureVideoDuration = 0
    private var mCaptureVideoProgress = 0
    private var internalCirclePercent = 0f
    private var listener: OnViewClickListener? = null
    private var mFirstDraw = true

    interface OnViewClickListener {
        fun onViewClick(mode: CaptureMode?)
    }

    fun setOnViewClickListener(listener: OnViewClickListener?) {
        this.listener = listener
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        mPaint = Paint()
        mPaint.isAntiAlias = true
        mCaptureVideoState = CaptureVideoState.UNDO
        mCaptureModel = CaptureMode.MODE_CAPTURE_PIC
        mCaptureViewTheme = CaptureViewTheme.THEME_BLUE
        mCaptureVideoProgress = 0
        internalCirclePercent = 1.0f
        mCaptureVideoDuration = 60
    }

    /**
     * 设置视频录制进度
     *
     * @param progress 进度值，<=mCaptureVideoDuration
     */
    fun setCaptureVideoProgress(progress: Int) {
        mCaptureVideoProgress = progress
        invalidate()
    }

    /**
     * 设置视频录制总时长
     *
     * @param duration 总时长，单位为秒
     * 默认录制60s
     */
    fun setCaptureVideoDuration(duration: Int) {
        mCaptureVideoDuration = duration
    }

    /**
     * 设置拍摄模式
     *
     * @param model 拍照 or 录像
     */
    fun setCaptureMode(model: CaptureMode?) {
        mCaptureModel = model
        invalidate()
    }

    /**
     * 设置按钮风格
     *
     * @param theme 风格，目前支持蓝色系和白色系两种
     */
    fun setCaptureViewTheme(theme: CaptureViewTheme?) {
        mCaptureViewTheme = theme
    }

    /**
     * 设置录制状态
     *
     * @param state 开始录制 or 停止录制
     */
    fun setCaptureVideoState(state: CaptureVideoState?) {
        mCaptureVideoState = state
        invalidate()
    }

    /**
     * 拍照时内部圆形缩放比例，仅供做动画使用
     *
     * @param internalCirclePercent 缩放比例，0f~1.0f（1.0f表示正常大小)
     */
    private fun setInternalCirclePercent(internalCirclePercent: Float) {
        this.internalCirclePercent = internalCirclePercent
        invalidate()
    }

    private fun getInternalCirclePercent(): Float {
        return internalCirclePercent
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            if (listener != null) {
                showClickAnimation()
                listener!!.onViewClick(mCaptureModel)
            }
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec))
    }

    private fun measureHeight(widthMeasureSpec: Int): Int {
        var measureW: Int
        val specMode = MeasureSpec.getMode(widthMeasureSpec)
        val specSize = MeasureSpec.getSize(widthMeasureSpec)
        if (specMode == MeasureSpec.EXACTLY) {
            // 精度模式
            measureW = specSize
        } else {
            // 默认大小
            measureW = dp2px(80f)
            // wrap_content
            if (specMode == MeasureSpec.AT_MOST) {
                measureW = measureW.coerceAtMost(specSize)
            }
        }
        return measureW
    }

    private fun measureWidth(heightMeasureSpec: Int): Int {
        var measureH: Int
        val specMode = MeasureSpec.getMode(heightMeasureSpec)
        val specSize = MeasureSpec.getSize(heightMeasureSpec)
        if (specMode == MeasureSpec.EXACTLY) {
            // 精度模式
            measureH = specSize
        } else {
            // 默认大小
            measureH = dp2px(80f)
            // wrap_content
            if (specMode == MeasureSpec.AT_MOST) {
                measureH = measureH.coerceAtMost(specSize)
            }
        }
        return measureH
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 当View大小变化时，获取其宽高
        mWidth = width
        mHeight = height
        circleX = mWidth / 2
        circleY = mHeight / 2
        radius = mWidth / 2
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mCaptureModel == CaptureMode.MODE_CAPTURE_VIDEO || mCaptureModel == CaptureMode.MODE_CAPTURE_AUDIO) {
            drawCaptureVideo(canvas)
        } else {
            drawCapturePicture(canvas)
        }
        if (mFirstDraw) {
            mFirstDraw = false
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mAnimator != null && mAnimator!!.isRunning) {
            mAnimator!!.cancel()
            mAnimator = null
        }
    }

    private fun showClickAnimation() {
        if (mCaptureModel == CaptureMode.MODE_CAPTURE_PIC) {
            mAnimator = ObjectAnimator.ofFloat(this, "internalCirclePercent", 1.0f, 0.85f, 1.0f)
            mAnimator?.duration = 150
            mAnimator?.start()
        }
    }

    private fun drawCapturePicture(canvas: Canvas) {
        if (mCaptureViewTheme == CaptureViewTheme.THEME_BLUE) {
            mPaint.style = Paint.Style.FILL
            mPaint.color = Color.WHITE
            canvas.drawCircle(circleX.toFloat(), circleY.toFloat(), radius.toFloat(), mPaint)
            // 绘制内部圆形
            mPaint.style = Paint.Style.FILL
            mPaint.strokeWidth = 2f
            mPaint.color = Color.parseColor("#2E5BFF")
            canvas.drawCircle(
                circleX.toFloat(), circleY.toFloat(),
                (radius - radius * 0.2).toFloat() * internalCirclePercent, mPaint
            )
            // 绘制外部圆环
            mPaint.strokeWidth = (radius * 0.1).toFloat()
            mPaint.style = Paint.Style.STROKE
            mPaint.color = Color.parseColor("#2E5BFF")
            canvas.drawCircle(
                circleX.toFloat(), circleY.toFloat(),
                (radius - radius * 0.05).toFloat(), mPaint
            )
        } else {
            mPaint.style = Paint.Style.FILL
            mPaint.color = Color.TRANSPARENT
            canvas.drawCircle(circleX.toFloat(), circleY.toFloat(), radius.toFloat(), mPaint)
            // 绘制内部圆形
            mPaint.style = Paint.Style.FILL
            mPaint.strokeWidth = 2f
            mPaint.color = Color.WHITE
            canvas.drawCircle(
                circleX.toFloat(), circleY.toFloat(),
                (radius - radius * 0.2).toFloat() * internalCirclePercent, mPaint
            )
            // 绘制外部圆环
            mPaint.strokeWidth = (radius * 0.1).toFloat()
            mPaint.style = Paint.Style.STROKE
            mPaint.color = Color.WHITE
            canvas.drawCircle(
                circleX.toFloat(), circleY.toFloat(),
                (radius - radius * 0.05).toFloat(), mPaint
            )
        }
    }

    private fun drawCaptureVideo(canvas: Canvas) {
        when (mCaptureVideoState) {
            CaptureVideoState.DOING -> {
                drawCaptureVideoDoingState(canvas)
            }
            CaptureVideoState.PAUSE -> {
                drawCaptureVideoPauseState(canvas)
            }
            else -> {
                drawCaptureVideoUndoState(canvas)
            }
        }
    }

    private fun drawCaptureVideoDoingState(canvas: Canvas) {
        mPaint.style = Paint.Style.FILL
        mPaint.color = resources.getColor(R.color.common_30_black)
        canvas.drawCircle(circleX.toFloat(), circleY.toFloat(), radius.toFloat(), mPaint)

        // 绘制内部白色圆形
        mPaint.style = Paint.Style.FILL
        mPaint.color = Color.WHITE
        canvas.drawCircle(circleX.toFloat(), circleY.toFloat(), radius * 0.575f, mPaint)

        // 绘制内部暂停的两条竖线
        mPaint.style = Paint.Style.FILL
        mPaint.strokeWidth = 2f
        mPaint.color = Color.parseColor("#FF0000")
        canvas.drawRoundRect(
            RectF(
                (mWidth * 0.4125).toFloat(), (mWidth * 0.3875).toFloat(),
                (mWidth * 0.4625).toFloat(), (mWidth * 0.6125).toFloat()
            ), 8f, 8f, mPaint
        )
        canvas.drawRoundRect(
            RectF(
                (mWidth * 0.5375).toFloat(), (mWidth * 0.3875).toFloat(),
                (mWidth * 0.5875).toFloat(), (mWidth * 0.6125).toFloat()
            ), 8f, 8f, mPaint
        )

        // 绘制外部进度条
        // 圆角线
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.strokeWidth = (radius * 0.08).toFloat()
        mPaint.style = Paint.Style.STROKE
        mPaint.color = Color.parseColor("#2E5BFF")
        val rectF = RectF(
            (radius * 0.03).toFloat(), (radius * 0.03).toFloat(),
            2 * radius - (radius * 0.03).toFloat(), 2 * radius - (radius * 0.03).toFloat()
        )
        val dfs = DecimalFormatSymbols()
        dfs.decimalSeparator = '.'
        val format = DecimalFormat("0.00")
        format.decimalFormatSymbols = dfs
        val result =
            format.format((mCaptureVideoProgress.toFloat() / mCaptureVideoDuration).toDouble())
        canvas.drawArc(rectF, 270f, (result.toFloat() * 360), false, mPaint)
    }

    private fun drawCaptureVideoPauseState(canvas: Canvas) {
        mPaint.style = Paint.Style.FILL
        mPaint.color = resources.getColor(R.color.common_30_black)
        canvas.drawCircle(circleX.toFloat(), circleY.toFloat(), radius.toFloat(), mPaint)

        // 绘制内部白色圆形
        mPaint.style = Paint.Style.FILL
        mPaint.color = Color.WHITE
        canvas.drawCircle(circleX.toFloat(), circleY.toFloat(), radius * 0.6f, mPaint)

        // 绘制外部进度条
        // 圆角线
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.strokeWidth = (radius * 0.08).toFloat()
        mPaint.style = Paint.Style.STROKE
        mPaint.color = Color.parseColor("#2E5BFF")
        val rectF = RectF(
            (radius * 0.03).toFloat(), (radius * 0.03).toFloat(),
            2 * radius - (radius * 0.03).toFloat(), 2 * radius - (radius * 0.03).toFloat()
        )
        val dfs = DecimalFormatSymbols()
        dfs.decimalSeparator = '.'
        val format = DecimalFormat("0.00")
        format.decimalFormatSymbols = dfs
        val result =
            format.format((mCaptureVideoProgress.toFloat() / mCaptureVideoDuration).toDouble())
        canvas.drawArc(rectF, 270f, (result.toFloat() * 360), false, mPaint)
    }

    private fun drawCaptureVideoUndoState(canvas: Canvas) {
        mCaptureVideoProgress = 0
        mPaint.style = Paint.Style.FILL
        mPaint.color = Color.TRANSPARENT
        canvas.drawCircle(circleX.toFloat(), circleY.toFloat(), radius.toFloat(), mPaint)
        // 绘制内部圆形
        mPaint.strokeWidth = 2f
        mPaint.style = Paint.Style.FILL
        mPaint.color = Color.WHITE
        canvas.drawCircle(
            circleX.toFloat(), circleY.toFloat(),
            (radius - radius * 0.3).toFloat(), mPaint
        )
        // 绘制发散线段
        canvas.save()
        mPaint.strokeWidth = dp2px(2f).toFloat()
        mPaint.style = Paint.Style.FILL
        mPaint.color = Color.WHITE
        val startX = mWidth.toFloat() / 2
        val startY = (mHeight / 2 - radius).toFloat()
        val count = 30
        for (i in 0 until count) {
            canvas.drawLine(startX, startY, startX, startY + 10, mPaint)
            canvas.rotate(360.toFloat() / count, width.toFloat() / 2, height.toFloat() / 2)
        }
        canvas.restore()
    }

    private fun dp2px(dpValue: Float): Int {
        val scale = resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
}