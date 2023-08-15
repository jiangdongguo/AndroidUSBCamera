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
package com.jiangdg.utils

import android.content.Context
import android.os.Parcelable
import com.tencent.mmkv.MMKV

/** Global haredPreference wrapper by MMKV
 *
 * @author Created by jiangdg on 2022/3/30
 */
object MMKVUtils {

    private val mKv: MMKV by lazy {
        MMKV.defaultMMKV()
    }

    /**
     * Init MMKV
     *
     * @param context
     */
    fun init(context: Context) {
        MMKV.initialize(context.applicationContext)
    }

    /**
     * save value to SharedPreference
     *
     * @param key key
     * @param value value, such as Int,String,Boolean...etc.
     */
    fun set(key: String, value: Any) {
        when(value) {
            is String -> mKv.encode(key, value)
            is Int -> mKv.encode(key, value)
            is Double -> mKv.encode(key, value)
            is Float -> mKv.encode(key, value)
            is Boolean -> mKv.encode(key, value)
            is Long -> mKv.encode(key, value)
            is ByteArray -> mKv.encode(key, value)
            is Parcelable -> mKv.encode(key, value)
            else -> throw IllegalStateException("Unsupported value type")
        }
    }

    /**
     * Get sharedPreference string value
     *
     * @param key key
     * @param defaultValue default value
     * @return sharedPreference string value
     */
    fun getString(key: String, defaultValue: String?=null): String? {
        return mKv.decodeString(key, defaultValue)
    }

    /**
     * Get sharedPreference Int value
     *
     * @param key key
     * @param defaultValue default value
     * @return sharedPreference Int value
     */
    fun getInt(key: String, defaultValue: Int=0): Int {
        return mKv.decodeInt(key, defaultValue)
    }

    /**
     * Get sharedPreference Long value
     *
     * @param key key
     * @param defaultValue default value
     * @return sharedPreference Long value
     */
    fun getLong(key: String, defaultValue: Long=0L): Long {
        return mKv.decodeLong(key, defaultValue)
    }

    /**
     * Get sharedPreference Double value
     *
     * @param key key
     * @param defaultValue default value
     * @return sharedPreference Double value
     */
    fun getDouble(key: String, defaultValue: Double=0.0): Double {
        return mKv.decodeDouble(key, defaultValue)
    }

    /**
     * Get sharedPreference Float value
     *
     * @param key key
     * @param defaultValue default value
     * @return sharedPreference Float value
     */
    fun getFloat(key: String, defaultValue: Float=0F): Float {
        return mKv.decodeFloat(key, defaultValue)
    }

    /**
     * Get sharedPreference Boolean value
     *
     * @param key key
     * @param defaultValue default value
     * @return sharedPreference Boolean value
     */
    fun getBoolean(key: String, defaultValue: Boolean=false): Boolean {
        return mKv.decodeBool(key, defaultValue)
    }

    /**
     * Get sharedPreference ByteArray value
     *
     * @param key key
     * @param defaultValue default value
     * @return sharedPreference ByteArray value
     */
    fun getByteArray(key: String, defaultValue: ByteArray?=null): ByteArray? {
        return mKv.decodeBytes(key, defaultValue)
    }

    /**
     * Get sharedPreference Parcelable value
     *
     * @param key key
     * @param clz Parcelable class
     * @return sharedPreference Parcelable value
     */
    fun <T: Parcelable> getParcelable(key: String, clz: Class<T>): T? {
        return mKv.decodeParcelable(key, clz)
    }
}
