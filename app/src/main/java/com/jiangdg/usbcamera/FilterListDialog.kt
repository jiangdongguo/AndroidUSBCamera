/*
 * Copyright 2017-2022 Jiangdg
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
package com.jiangdg.usbcamera

import android.app.Activity
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.jiangdg.media.base.BaseDialog
import com.jiangdg.media.render.filter.AbstractFilter

/** Filter list dialog
 *
 * @author Created by jiangdg on 2022/2/8
 */
class FilterListDialog(activity: Activity): BaseDialog(activity, portraitWidthRatio = 1f) {
    private var mRecyclerView: RecyclerView? = null
    private var mFilterTabBtn: TextView? = null
    private var mAnimTabBtn: TextView? = null

    init {
        mRecyclerView = mDialog.findViewById(R.id.filterRv)
        mFilterTabBtn = mDialog.findViewById(R.id.tabFilterBtn)
        mAnimTabBtn = mDialog.findViewById(R.id.tabAnimBtn)
        mDialog.window?.let {
            it.setGravity(Gravity.BOTTOM)
            it.setWindowAnimations(R.style.camera2_anim_down_to_top)

            it.attributes?.run {
                width = WindowManager.LayoutParams.MATCH_PARENT
//                height = (240f / 360f * ScreenUtils.getScreenWidth()).toInt()
                mDialog.window?.attributes = this
            }
        }

        mDialog.window?.setDimAmount(0f)
        setCanceledOnTouchOutside(true)
        setCancelable(true)
    }

    override fun getContentLayoutId(): Int {
        return R.layout.dialog_filters
    }
}

private class FilterListAdapter : BaseQuickAdapter<AbstractFilter, BaseViewHolder>(R.layout.dialog_filter_item) {
    override fun convert(holder: BaseViewHolder, item: AbstractFilter) {

    }
}