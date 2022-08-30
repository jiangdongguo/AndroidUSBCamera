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
package com.jiangdg.demo

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.jiangdg.ausbc.base.BaseDialog
import com.jiangdg.ausbc.render.effect.bean.CameraEffect
import com.jiangdg.utils.MMKVUtils
import com.jiangdg.ausbc.utils.Utils
import com.jiangdg.utils.imageloader.ImageLoaders

/** Effect list dialog
 *
 * @author Created by jiangdg on 2022/2/8
 */
@SuppressLint("NotifyDataSetChanged")
class EffectListDialog(activity: Activity) : BaseDialog(activity, portraitWidthRatio = 1f),
    View.OnClickListener {
    private var mListener: OnEffectClickListener? = null
    private var mAdapter: EffectListAdapter? = null
    private var mRecyclerView: RecyclerView? = null
    private var mFilterTabBtn: TextView? = null
    private var mAnimTabBtn: TextView? = null
    private var mEffectList: ArrayList<CameraEffect> = ArrayList()
    private val mEffectMap = HashMap<Int, List<CameraEffect>>()

    override fun getContentLayoutId(): Int = R.layout.dialog_effects

    init {
        mDialog.window?.let {
            it.setGravity(Gravity.BOTTOM)
            it.setWindowAnimations(R.style.camera2_anim_down_to_top)

            it.attributes?.run {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = (200f / 360f * Utils.getScreenWidth(activity)).toInt()
                mDialog.window?.attributes = this
            }
        }

        mDialog.window?.setDimAmount(0f)
        setCanceledOnTouchOutside(true)
        setCancelable(true)
        // recycler view
        mAdapter = EffectListAdapter().apply {
            setOnItemChildClickListener { _, _, position ->
                data.getOrNull(position)?.let {
                    if (getCurrEffect()?.id == it.id) {
                        return@setOnItemChildClickListener
                    }
                    setCurrEffect(it)
                    mListener?.onEffectClick(it)
                    notifyDataSetChanged()
                }
            }
        }
        mRecyclerView = mDialog.findViewById(R.id.filterRv)
        mFilterTabBtn = mDialog.findViewById(R.id.tabFilterBtn)
        mAnimTabBtn = mDialog.findViewById(R.id.tabAnimBtn)
        mAnimTabBtn?.setOnClickListener(this)
        mFilterTabBtn?.setOnClickListener(this)
        mRecyclerView?.layoutManager =
            LinearLayoutManager(mDialog.context, LinearLayoutManager.HORIZONTAL, false)
        mRecyclerView?.adapter = mAdapter
    }

    fun setData(list: List<CameraEffect>, listener: OnEffectClickListener) {
        mListener = listener
        mEffectList.clear()
        mEffectList.addAll(list)
        initEffectData()
        initEffectTabs()
    }

    private fun getCurFilterId() = MMKVUtils.getInt(KEY_FILTER, CameraEffect.ID_NONE_FILTER)

    private fun getCurAnimationId() = MMKVUtils.getInt(KEY_ANIMATION, CameraEffect.ID_NONE_ANIMATION)

    private fun initEffectTabs() {
        getCurAnimationId().let { curAnimId ->
            if (curAnimId != CameraEffect.ID_NONE_ANIMATION) {
                mAnimTabBtn?.typeface = Typeface.DEFAULT_BOLD
                mAnimTabBtn?.setTextColor(getDialog().context.resources.getColor(R.color.black))
                mAnimTabBtn?.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    0,
                    R.drawable.ic_tab_line_blue
                )
                mFilterTabBtn?.typeface = Typeface.DEFAULT
                mFilterTabBtn?.setTextColor(getDialog().context.resources.getColor(R.color.common_a8_black))
                mFilterTabBtn?.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    0,
                    R.drawable.ic_tab_line_white
                )
                mAdapter?.setNewData(mEffectMap[CameraEffect.CLASSIFY_ID_ANIMATION])
                mAdapter?.setCurrEffect(mAdapter?.data?.find { it.id == curAnimId })
                return
            }
        }
        val curFilterId = getCurFilterId()
        mFilterTabBtn?.typeface = Typeface.DEFAULT_BOLD
        mFilterTabBtn?.setTextColor(getDialog().context.resources.getColor(R.color.black))
        mFilterTabBtn?.setCompoundDrawablesWithIntrinsicBounds(
            0,
            0,
            0,
            R.drawable.ic_tab_line_blue
        )
        mAnimTabBtn?.typeface = Typeface.DEFAULT
        mAnimTabBtn?.setTextColor(getDialog().context.resources.getColor(R.color.common_a8_black))
        mAnimTabBtn?.setCompoundDrawablesWithIntrinsicBounds(
            0,
            0,
            0,
            R.drawable.ic_tab_line_white
        )
        mAdapter?.setNewData(mEffectMap[CameraEffect.CLASSIFY_ID_FILTER])
        mAdapter?.setCurrEffect(mAdapter?.data?.find { it.id == curFilterId })
    }

    private fun initEffectData() {
        // filter list
        mEffectList.filter {
            it.classifyId == CameraEffect.CLASSIFY_ID_FILTER
        }.let {
            val list = ArrayList<CameraEffect>().apply {
                addAll(it)
            }
            mEffectMap[CameraEffect.CLASSIFY_ID_FILTER] = list
        }
        // animation list
        mEffectList.filter {
            it.classifyId == CameraEffect.CLASSIFY_ID_ANIMATION
        }.let {
            val list = ArrayList<CameraEffect>().apply {
                addAll(it)
            }
            mEffectMap[CameraEffect.CLASSIFY_ID_ANIMATION] = list
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tabFilterBtn -> {
                mFilterTabBtn?.typeface = Typeface.DEFAULT_BOLD
                mFilterTabBtn?.setTextColor(getDialog().context.resources.getColor(R.color.black))
                mFilterTabBtn?.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    0,
                    R.drawable.ic_tab_line_blue
                )
                mAnimTabBtn?.typeface = Typeface.DEFAULT
                mAnimTabBtn?.setTextColor(getDialog().context.resources.getColor(R.color.common_a8_black))
                mAnimTabBtn?.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    0,
                    R.drawable.ic_tab_line_white
                )
                mAdapter?.setNewData(mEffectMap[CameraEffect.CLASSIFY_ID_FILTER])
                mAdapter?.setCurrEffect(mAdapter?.data?.find { it.id == getCurFilterId() })
            }
            R.id.tabAnimBtn -> {
                mAnimTabBtn?.typeface = Typeface.DEFAULT_BOLD
                mAnimTabBtn?.setTextColor(getDialog().context.resources.getColor(R.color.black))
                mAnimTabBtn?.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    0,
                    R.drawable.ic_tab_line_blue
                )
                mFilterTabBtn?.typeface = Typeface.DEFAULT
                mFilterTabBtn?.setTextColor(getDialog().context.resources.getColor(R.color.common_a8_black))
                mFilterTabBtn?.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    0,
                    R.drawable.ic_tab_line_white
                )
                mAdapter?.setNewData(mEffectMap[CameraEffect.CLASSIFY_ID_ANIMATION])
                mAdapter?.setCurrEffect(mAdapter?.data?.find { it.id == getCurAnimationId() })
            }
            else -> {
            }
        }
    }

    interface OnEffectClickListener {
        fun onEffectClick(effect: CameraEffect)
    }

    companion object {
        const val KEY_FILTER = "filter"
        const val KEY_ANIMATION = "animation"
    }
}

private class EffectListAdapter :
    BaseQuickAdapter<CameraEffect, BaseViewHolder>(R.layout.dialog_effect_item) {

    private var mCurrEffect: CameraEffect? = null

    fun setCurrEffect(effect: CameraEffect?) {
        val oldPosition = getPosition(mCurrEffect)
        mCurrEffect = effect
        val newPosition = getPosition(effect)
        if (oldPosition != newPosition) {
            if (oldPosition != -1) {
                notifyItemChanged(oldPosition)
            }
            if (newPosition != -1) {
                notifyItemChanged(newPosition)
            }
        }
    }

    fun getCurrEffect(): CameraEffect? = mCurrEffect

    private fun getPosition(cameraEffect: CameraEffect?): Int {
        var position = -1
        cameraEffect ?: return position
        data.forEachIndexed { index, filter ->
            if (filter.id == cameraEffect.id) {
                position = index
                return@forEachIndexed
            }
        }
        return position
    }

    override fun convert(helper: BaseViewHolder, item: CameraEffect) {
        helper.setText(R.id.effectName, item.name)
        helper.getView<ImageView>(R.id.effectIv).also {
            item.coverResId?.apply {
                ImageLoaders.of(mContext).loadCircle(it, this, R.drawable.effect_none)
                return@also
            }
            ImageLoaders.of(mContext).loadCircle(it, item.coverUrl, R.drawable.effect_none)
        }
        helper.addOnClickListener(R.id.effectIv)
        // update check status
        (mCurrEffect?.id == item.id).let { isCheck ->
            val textColor = if (isCheck) 0xFF2E5BFF else 0xFF232325
            helper.setTextColor(R.id.effectName, textColor.toInt())
            helper.setVisible(R.id.effectCheckIv, isCheck)
        }

    }
}