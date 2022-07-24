package com.jiangdg.ausbc.base

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jiangdg.ausbc.R

/** Base bottom sheet dialog
 *
 * @author Created by jiangdg on 2022/7/23
 */
abstract class BaseBottomDialog: BottomSheetDialogFragment()  {
    private var mDismissListener: OnDismissListener? = null
    private var mTopOffset: Int = 0
    private var mBehavior: BottomSheetBehavior<FrameLayout>? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        context ?: return super.onCreateDialog(savedInstanceState)
        return BottomSheetDialog(requireContext(), R.style.TransparentBottomSheetStyle);
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return getRootView(inflater, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initData()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        val sheetDialog = dialog as BottomSheetDialog
        sheetDialog.delegate.findViewById<FrameLayout>(R.id.design_bottom_sheet)?.apply {
            background = ColorDrawable(Color.TRANSPARENT)
            val params = layoutParams as CoordinatorLayout.LayoutParams
            params.height = getPeekHeight()
            layoutParams = params
            mBehavior = BottomSheetBehavior.from(this)
            mBehavior?.peekHeight = getPeekHeight()
            mBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun getPeekHeight(): Int {
        (context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.let { wm ->
            val point = Point()
            wm.defaultDisplay.getSize(point)
            return point.y - mTopOffset
        }
        return WindowManager.LayoutParams.MATCH_PARENT
    }

    fun hide() {
        mBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
    }

    fun setTopOffset(offset: Int) {
        mTopOffset = offset
    }

    fun setOnDismissListener(listener: OnDismissListener) {
        this.mDismissListener = listener
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        mDismissListener?.onDismiss()
    }

    protected abstract fun initView()
    protected abstract fun initData()
    protected abstract fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View?

    interface OnDismissListener {
        fun onDismiss()
    }
}