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
package com.jiangdg.media.utils

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.ChecksSdkIntAtLeast
import java.io.*

/** Media utils
 *
 * @author Created by jiangdg on 2022/2/23
 */
object MediaUtils {

    fun readRawTextFile(context: Context, rawId: Int): String {
        val inputStream = context.resources.openRawResource(rawId)
        val br = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        val sb = StringBuilder()
        try {
            while (br.readLine().also { line = it } != null) {
                sb.append(line)
                sb.append("\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.e("MediaUtils", "open raw file failed!", e)
        }
        try {
            br.close()
        } catch (e: IOException) {
            e.printStackTrace()
            Logger.e("MediaUtils", "close raw file failed!", e)
        }
        return sb.toString()
    }

    fun findRecentMedia(context: Context): String? {
        val imagePath = findRecentMedia(context, true)
        val videoPath = findRecentMedia(context, false)
        if (imagePath == null) {
            return videoPath
        }
        if (videoPath == null) {
            return imagePath
        }
        val imageFile = File(imagePath)
        val videoFile = File(videoPath)
        if (imageFile.lastModified() >= videoFile.lastModified()) {
            return imagePath
        }
        return videoPath
    }

    fun findRecentMedia(context: Context, isImage: Boolean): String? {
        val uri: Uri
        val sortOrder: String
        val columnName: String
        val projection = if (isImage) {
            columnName = MediaStore.Images.Media.DATA
            sortOrder = MediaStore.Images.ImageColumns._ID + " DESC"
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            arrayOf(
                MediaStore.Images.Media.DATA, MediaStore.Images.ImageColumns.DATE_ADDED,
                MediaStore.Images.Media.SIZE, MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.WIDTH, MediaStore.Images.Media.HEIGHT
            )
        } else {
            columnName = MediaStore.Video.Media.DATA
            sortOrder = MediaStore.Video.VideoColumns._ID + " DESC"
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            arrayOf(
                MediaStore.Video.Media.DATA, MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.VideoColumns.DATE_ADDED, MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.MIME_TYPE, MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.WIDTH, MediaStore.Video.Media.HEIGHT
            )
        }
        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            sortOrder
        )?.apply {
            if (count < 1) {
                close()
                return null
            }
            while (moveToNext()) {
                val data = getString(getColumnIndexOrThrow(columnName))
                val file = File(data)
                if (file.exists()) {
                    close()
                    return file.path
                }
            }
        }.also {
            it?.close()
        }
        return null
    }

    fun saveYuv2Jpeg(path: String, data: ByteArray, width: Int, height: Int): Boolean {
        val yuvImage = YuvImage(data, ImageFormat.NV21, width, height, null)
        val bos = ByteArrayOutputStream(data.size)
        var result = yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, bos)
        if (! result) {
            return false
        }
        val buffer = bos.toByteArray()
        val file = File(path)
        val fos: FileOutputStream?
        try {
            fos = FileOutputStream(file)
            fos.write(buffer)
            fos.close()
        } catch (e: FileNotFoundException) {
            result = false
            e.printStackTrace()
        } catch (e: IOException) {
            result = false
            e.printStackTrace()
        } finally {
            try {
                bos.close()
            } catch (e: IOException) {
                result = false
                e.printStackTrace()
            }
        }
        return result
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    fun isAboveQ(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
}
