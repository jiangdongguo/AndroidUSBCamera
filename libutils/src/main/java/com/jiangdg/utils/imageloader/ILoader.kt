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
package com.jiangdg.utils.imageloader

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation

/**
 * Image loader
 *
 * @param T image view
 * @author Created by jiangdg on 2022/3/16
 */
interface ILoader<T> {
    /**
     * Load image from url
     *
     * @param imageView image view
     * @param url image uri
     * @param placeHolder place holder
     */
    fun load(imageView: T, url: String?, placeHolder: Int)

    /**
     * Load image from url width default place holder
     *
     * @param imageView image view
     * @param url image url
     */
    fun load(imageView: T, url: String?)

    /**
     * Load image from resource id
     *
     * @param imageView image view
     * @param resId resource id
     */
    fun load(imageView: T, resId: Int)

    /**
     * Load image from url with transform
     *
     * @param imageView image view
     * @param url image url
     * @param placeHolder place holder
     * @param bitmapTransformation transformation
     */
    fun load(
        imageView: T,
        url: String?,
        placeHolder: Int,
        bitmapTransformation: BitmapTransformation?
    )

    /**
     * Load rounded from url
     *
     * @param imageView image view
     * @param url image url
     * @param placeHolder resource id of place holder
     * @param radius radius of rounded image
     */
    fun loadRounded(imageView: T, url: String?, placeHolder: Int, radius: Float)

    /**
     * Load rounded from url
     *
     * @param imageView image view
     * @param url image url
     * @param placeHolder drawable type of place holder
     * @param radius radius of rounded image
     */
    fun loadRounded(imageView: T, url: String?, placeHolder: Drawable?, radius: Float)

    /**
     * Load rounded from url
     *
     * @param imageView image view
     * @param url image url
     * @param radius radius of rounded image
     */
    fun loadRounded(imageView: T, url: String?, radius: Float)

    /**
     * Load circle from url
     *
     * @param imageView image view
     * @param url image url
     * @param placeHolder resource id of place holder
     */
    fun loadCircle(imageView: T, url: String?, placeHolder: Int)

    /**
     * Load circle from url
     *
     * @param imageView image view
     * @param url image url
     */
    fun loadCircle(imageView: T, url: String?)

    /**
     * Load circle from url
     *
     * @param imageView image view
     * @param resId image resId
     * @param placeHolder resource id of place holder
     */
    fun loadCircle(imageView: T, resId: Int, placeHolder: Int)

    /**
     * Load circle from resource id
     *
     * @param imageView image view
     * @param resId image resId
     */
    fun loadCircle(imageView: T, resId: Int)

    fun loadAsBitmap(url: String?, width: Int, height: Int, listener: OnLoadedResultListener)

    interface OnLoadedResultListener {
        fun onLoadedSuccess(bitmap: Bitmap?)
        fun onLoadedFailed(error: Exception?)
    }
}