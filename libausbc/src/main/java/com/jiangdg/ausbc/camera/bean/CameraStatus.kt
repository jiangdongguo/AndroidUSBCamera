package com.jiangdg.ausbc.camera.bean

import androidx.annotation.Keep

/** Camera status
 *
 * @author Created by jiangdg on 2022/4/5
 */
@kotlin.Deprecated("Deprecated since version 3.3.0")
@Keep
data class CameraStatus(val code: Int, val message: String? = null) {
    companion object {
        const val START = 1
        const val STOP = 2
        const val ERROR = -1
        const val ERROR_PREVIEW_SIZE = -2
    }
}
