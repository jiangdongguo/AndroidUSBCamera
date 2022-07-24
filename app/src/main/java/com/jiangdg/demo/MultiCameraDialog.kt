package com.jiangdg.demo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jiangdg.ausbc.base.BaseBottomDialog
import com.jiangdg.demo.databinding.DialogMultiCamerasBinding

/** Multi Camera Dialog
 *
 * @author Created by jiangdg on 2022/7/23
 */
class MultiCameraDialog : BaseBottomDialog() {
    private lateinit var mViewBinding: DialogMultiCamerasBinding

    override fun initView() {
        mViewBinding.multiCameraDialogHide.setOnClickListener {
            hide()
        }
        childFragmentManager.beginTransaction()
            .add(R.id.multi_camera_dialog_container, DemoMultiCameraFragment())
            .commitAllowingStateLoss()
    }

    override fun initData() {
        setTopOffset(200)
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View? {
        mViewBinding = DialogMultiCamerasBinding.inflate(inflater, container, false)
        return mViewBinding.root
    }

}