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
package com.jiangdg.ausbc.utils

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.ChecksSdkIntAtLeast
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

/** Media utils
 *
 * @author Created by jiangdg on 2022/2/23
 */
object MediaUtils {

    private const val TAG = "MediaUtils"

    private val AUDIO_SAMPLING_RATES = intArrayOf(
        96000,  // 0
        88200,  // 1
        64000,  // 2
        48000,  // 3
        44100,  // 4
        32000,  // 5
        24000,  // 6
        22050,  // 7
        16000,  // 8
        12000,  // 9
        11025,  // 10
        8000,  // 11
        7350,  // 12
        -1,  // 13
        -1,  // 14
        -1
    )

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
            Logger.e(TAG, "open raw file failed!", e)
        }
        try {
            br.close()
        } catch (e: IOException) {
            e.printStackTrace()
            Logger.e(TAG, "close raw file failed!", e)
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
        val yuvImage = try {
            YuvImage(data, ImageFormat.NV21, width, height, null)
        } catch (e: Exception) {
            Logger.e(TAG, "create YuvImage failed.", e)
            null
        } ?: return false
        val bos = ByteArrayOutputStream(data.size)
        var result = try {
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, bos)
        } catch (e: Exception) {
            Logger.e(TAG, "compressToJpeg failed.", e)
            false
        }
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
        } catch (e: IOException) {
            Logger.e(TAG, "saveYuv2Jpeg failed.", e)
            result = false
            e.printStackTrace()
        } finally {
            try {
                bos.close()
            } catch (e: IOException) {
                result = false
                Logger.e(TAG, "saveYuv2Jpeg failed.", e)
                e.printStackTrace()
            }
        }
        return result
    }

    fun transformYuv2Jpeg(data: ByteArray, width: Int, height: Int): ByteArray? {
        val yuvImage = try {
            YuvImage(data, ImageFormat.NV21, width, height, null)
        } catch (e: Exception) {
            Logger.e(TAG, "create YuvImage failed.", e)
            null
        } ?: return null
        val bos = ByteArrayOutputStream(data.size)
        return try {
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, bos)
            bos.toByteArray()
        } catch (e: Exception) {
            Logger.e(TAG, "compressToJpeg failed.", e)
            null
        }
    }

    fun transferByte2Short(data: ByteArray, readBytes: Int): ShortArray {
        // byte[] to short[], the length of the array is reduced by half
        val shortLen = readBytes / 2
        // Assemble byte[] numbers as ByteBuffer buffers
        val byteBuffer: ByteBuffer = ByteBuffer.wrap(data, 0, readBytes)
        // Convert ByteBuffer to little endian and get shortBuffer
        val shortBuffer: ShortBuffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val shortData = ShortArray(shortLen)
        shortBuffer.get(shortData, 0, shortLen)
        return shortData
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    fun isAboveQ(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
    fun addADTStoPacket(packet: ByteArray, packetLen: Int, sampleRate: Int) {
        val packetWithAdts = ByteArray(packetLen + 7)
        packetWithAdts[0] = 0xFF.toByte()
        packetWithAdts[1] = 0xF1.toByte()
        packetWithAdts[2] = (((2 - 1 shl 6) + (AUDIO_SAMPLING_RATES.indexOf(sampleRate) shl 2) + (1 shr 2)).toByte())
        packetWithAdts[3] = ((1 and 3 shl 6) + (packetLen shr 11)).toByte()
        packetWithAdts[4] = (packetLen and 0x7FF shr 3).toByte()
        packetWithAdts[5] = ((packetLen and 7 shl 5) + 0x1F).toByte()
        packetWithAdts[6] = 0xFC.toByte()
        System.arraycopy(packet, 0, packetWithAdts, 7, packetLen)
    }

}
