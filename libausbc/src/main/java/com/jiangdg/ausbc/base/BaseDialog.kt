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
package com.jiangdg.ausbc.base

import android.app.Activity
import android.app.Dialog
import android.content.res.Configuration
import android.util.DisplayMetrics
import com.jiangdg.ausbc.R

abstract class BaseDialog(
        activity: Activity,
        portraitWidthRatio: Float = 0.67F,
        landscapeWidthRatio: Float = 0.5F
) : DialogInterface {
    private val mContext: Activity = activity
    protected val mDialog: Dialog = Dialog(mContext, R.style.CommonDialogStyle)

    init {
        mDialog.setContentView(this.getContentLayoutId())
        val orientation = mContext.resources.configuration.orientation
        val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE // 是否横屏
        mDialog.window?.let {
            // dialog的宽度 横屏设置为50% 竖屏设置为80%
            val dm = DisplayMetrics()
            it.windowManager?.defaultDisplay?.run {
                getMetrics(dm)
                val lp = it.attributes
                lp.width = (dm.widthPixels * if (isLandscape) landscapeWidthRatio else portraitWidthRatio).toInt()
                it.attributes = lp
            }
        }
        mDialog.setCanceledOnTouchOutside(false)
    }

    protected abstract fun getContentLayoutId(): Int

    final override fun getDialog(): Dialog = mDialog

    override fun show() {
        getDialog().show()
    }

    override fun dismiss() {
        getDialog().dismiss()
    }

    override fun isShowing(): Boolean {
        return getDialog().isShowing
    }

    override fun setCanceledOnTouchOutside(cancel: Boolean) {
        getDialog().setCanceledOnTouchOutside(cancel)
    }

    override fun setCancelable(flag: Boolean) {
        getDialog().setCancelable(flag)
    }
}
