package com.jiangdg.ausbc.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/** RecyclerView.ItemDecoration
 *
 * @author Created by jiangdg on 2022/7/24
 */
class SpaceItemDecoration(private val space: Int): RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.left = space;
        outRect.bottom = space;
        if (parent.getChildLayoutPosition(view) %3==0) {
            outRect.left = 0
        }
    }
}