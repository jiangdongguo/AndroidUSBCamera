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

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.jiangdg.ausbc.R

import java.text.DecimalFormat

/**
 *
 * @author Created by jiangdg on 2022/2/8
 */
class CircleProgressView : View {
    private var isRecordVideo: Boolean = false
    private var mPaint: Paint? = null
    private var mWidth = 0
    private var mHeight = 0
    private var circleX = 0
    private var circleY = 0
    private var radius = 0
    private var state = 0
    private var mSweepAngle = 1
    private var isOddNumber = true
    private var outsideCircleBgColor = 0
    private var progressArcBgColor = 0
    private var insideCircleBgColor = 0
    private var insideCircleTouchedBgColor = 0
    private var insideRectangleBgColor = 0
    private var tipTextSize = 0f
    private var tipTextColor = 0

    // 进度值
    private var progress = 0
    private var totalSize = 0
    private var isShowTextTip = false
    private var isTouched = false

    // 点击事件回调
    private var listener: OnViewClickListener? = null
    private var isDisabled = false

    constructor(context: Context?) : super(context)

    interface OnViewClickListener {
        fun onViewClick()
    }

    // 点击事件回调
    fun setOnViewClickListener(listener: OnViewClickListener?) {
        this.listener = listener
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        // 获取自定义属性
        val ta: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleProgressView)
        outsideCircleBgColor = ta.getColor(
            R.styleable.CircleProgressView_outsideCircleBgColor,
            resources.getColor(R.color.colorWhite)
        )
        progressArcBgColor = ta.getColor(
            R.styleable.CircleProgressView_progressArcBgColor,
            resources.getColor(R.color.colorGray)
        )
        insideCircleBgColor = ta.getColor(
            R.styleable.CircleProgressView_insideCircleBgColor,
            resources.getColor(R.color.colorRed)
        )
        insideCircleTouchedBgColor = ta.getColor(
            R.styleable.CircleProgressView_insideCircleTouchedBgColor,
            resources.getColor(R.color.colorDeepRed)
        )
        insideRectangleBgColor = ta.getColor(
            R.styleable.CircleProgressView_insideRectangleBgColor,
            resources.getColor(R.color.colorRed)
        )
        tipTextColor = ta.getColor(
            R.styleable.CircleProgressView_tipTextColor,
            resources.getColor(R.color.colorWhite)
        )
        tipTextSize = ta.getDimension(R.styleable.CircleProgressView_tipTextSize, 34F)
        ta.recycle()
        mPaint = Paint()
    }

    fun setConnectState(state: Int) {
        this.state = state
        // 重新绘制View
        this.invalidate()
    }

    fun getConnectState(): Int {
        return state
    }

    fun setProgressVaule(progress: Int) {
        this.progress = progress
        // 重新绘制View
        this.invalidate()
    }

    fun setTotalSize(totalSize: Int) {
        this.totalSize = totalSize
    }

    fun setShowTextTipFlag(isShowTextTip: Boolean) {
        this.isShowTextTip = isShowTextTip
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (listener == null || isDisabled()) return super.onTouchEvent(event)
        when(event.action) {
            MotionEvent.ACTION_DOWN -> {
                isTouched = true
            }
            MotionEvent.ACTION_UP -> {
                isTouched = false
                // 松开手时，处理触摸事件
                listener!!.onViewClick()
            }
            else -> {}
        }
        this.invalidate()
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // 调用setMeasuredDimension
        // 测量View大小
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec))
    }

    private fun measureHeight(widthMeasureSpec: Int): Int {
        var width: Int
        val specMode: Int = MeasureSpec.getMode(widthMeasureSpec)
        val specSize: Int = MeasureSpec.getSize(widthMeasureSpec)
        if (specMode == MeasureSpec.EXACTLY) {
            // 精度模式
            width = specSize
        } else {
            // 默认大小
            width = 200
            // wrap_content
            if (specMode == MeasureSpec.AT_MOST) {
                width = width.coerceAtMost(specSize)
            }
        }
        return width
    }

    private fun measureWidth(heightMeasureSpec: Int): Int {
        var height: Int
        val specMode: Int = MeasureSpec.getMode(heightMeasureSpec)
        val specSize: Int = MeasureSpec.getSize(heightMeasureSpec)
        if (specMode == MeasureSpec.EXACTLY) {
            // 精度模式
            height = specSize
        } else {
            // 默认大小
            height = 200
            // wrap_content
            if (specMode == MeasureSpec.AT_MOST) {
                height = height.coerceAtMost(specSize)
            }
        }
        return height
    }

    private fun isDisabled(): Boolean {
        return isDisabled
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 当View大小变化时，获取其宽高
        mWidth = width
        mHeight = height
        circleX = mWidth / 2
        circleY = mWidth / 2
        radius = mWidth / 2
        // 设置默认状态
        state = STATE_UNDONE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawOutSideCircle(canvas)
        if (STATE_DONE == state) {
            drawInternelRectangle(canvas)
        } else {
            if (isTouched) {
                drawInternalCircle(canvas, insideCircleTouchedBgColor)
            } else {
                drawInternalCircle(canvas, insideCircleBgColor)
            }
            // 绘制弧形进度条
            if (STATE_DOING == state) {
                drawProgressArc(canvas)
            }
        }
        if (isRecordVideo) {
            drawRecordVideoCircle(canvas)
        }
    }

    private fun drawRecordVideoCircle(canvas: Canvas) {
        mPaint?.strokeWidth = 2F
        mPaint?.style = Paint.Style.FILL
        mPaint?.color = resources.getColor(R.color.colorRed)
        mPaint?.isAntiAlias = true
        canvas.drawCircle(
            circleX.toFloat(),
            circleY.toFloat(), (radius - radius * 0.75).toFloat(), mPaint!!
        )
    }

    private fun drawOutSideCircle(canvas: Canvas) {
        mPaint?.strokeWidth = 2.5F
        mPaint?.color = outsideCircleBgColor
        mPaint?.style = Paint.Style.STROKE
        mPaint?.isAntiAlias = true
        canvas.drawColor(Color.TRANSPARENT)
        canvas.drawCircle(circleX.toFloat(), circleY.toFloat(), radius.toFloat() - 5F, mPaint!!)
    }

    private fun drawInternalCircle(canvas: Canvas, colorType: Int) {
        mPaint?.strokeWidth = 2F
        mPaint?.style = Paint.Style.FILL
        mPaint?.color = colorType
        mPaint?.isAntiAlias = true
        canvas.drawCircle(
            circleX.toFloat(),
            circleY.toFloat(), (radius - radius * 0.35).toFloat(), mPaint!!
        )
    }

    private fun drawInternelRectangle(canvas: Canvas) {
        mPaint?.strokeWidth = 2F
        mPaint?.color = insideRectangleBgColor
        mPaint?.isAntiAlias = true
        mPaint?.style = Paint.Style.FILL
        canvas.drawRect(
            (mWidth * 0.3).toFloat(),
            (mWidth * 0.3).toFloat(),
            (mWidth - mWidth * 0.3).toFloat(),
            (mWidth - mWidth * 0.3).toFloat(),
            mPaint!!
        )
    }

    private fun drawProgressArc(canvas: Canvas) {
        mPaint?.strokeWidth = (radius * 0.15).toInt().toFloat()
        mPaint?.style = Paint.Style.STROKE
        mPaint?.isAntiAlias = true
        mPaint?.color = progressArcBgColor
        if (progress >= 0) {
            if (totalSize == 0) return
            canvas.drawArc(
                RectF(
                    (radius * 0.08).toFloat(),
                    (radius * 0.08).toFloat(),
                    2 * radius - (radius * 0.08).toFloat(),
                    2 * radius - (radius * 0.08).toFloat()
                ),
                180F,
                ((DecimalFormat("0.00")
                    .format(progress.toFloat() / totalSize).toFloat() * 360).toInt()).toFloat(),
                false,
                mPaint!!
            )
            if (isShowTextTip) {
                drawTextTip(
                    canvas,
                    (DecimalFormat("0.00")
                        .format(progress.toFloat() / totalSize).toFloat() * 100).toString() + " %"
                )
            }
        } else if (progress == NONE) {
            if (isOddNumber) {
                canvas.drawArc(
                    RectF(
                        (radius * 0.08).toFloat(),
                        (radius * 0.08).toFloat(),
                        2 * radius - (radius * 0.08).toFloat(),
                        2 * radius - (radius * 0.08).toFloat()
                    ), 180F, mSweepAngle.toFloat(), false, mPaint!!
                )
                mSweepAngle++
                if (mSweepAngle >= 360) isOddNumber = false
            } else {
                canvas.drawArc(
                    RectF(
                        (radius * 0.08).toFloat(),
                        (radius * 0.08).toFloat(),
                        2 * radius - (radius * 0.08).toFloat(),
                        2 * radius - (radius * 0.08).toFloat()
                    ), 180F, (-mSweepAngle).toFloat(), false, mPaint!!
                )
                mSweepAngle--
                if (mSweepAngle == 0) isOddNumber = true
            }
            this.postInvalidateDelayed(5)
        }
    }

    private fun drawTextTip(canvas: Canvas, tipText: String) {
        mPaint?.strokeWidth = 2F
        mPaint?.style = Paint.Style.FILL
        mPaint?.isAntiAlias = true
        mPaint?.textSize = tipTextSize
        mPaint?.color = tipTextColor
        //Paint.Align.CENTER , x表示字体中心位置；
        // Paint.Align.LEFT ,x表示文本左边位置；
        mPaint?.textAlign = Paint.Align.CENTER
        val xCenter: Int = measuredHeight / 2
        val yBaseLine: Float =
            ((measuredHeight - mPaint?.fontMetrics!!.bottom + mPaint?.fontMetrics!!.top) / 2
                    - mPaint?.fontMetrics!!.top)
        canvas.drawText(tipText, xCenter.toFloat(), yBaseLine, mPaint!!)
    }

    fun setMode(model: Int) {
        isRecordVideo = model == MODEL_VIDEO
        this.invalidate()
    }

    companion object {
        // 状态正在进行
        private const val STATE_DOING = 0
        // 状态操作完成
        private const val  STATE_DONE = 1
        // 状态操作未完成或初始状态
        private const val  STATE_UNDONE = 2
        private const val  NONE = -1

        private const val MODEL_PICTURE = 0
        private const val MODEL_VIDEO = 1
    }
}