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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.jiangdg.utils.R
import java.lang.IllegalArgumentException

/**GlideImageLoader by glide
 *
 * @param target imageview owner
 *
 * @author Created by jiangdg on 2022/3/16
 */
class GlideLoader<T>(target: T) : ILoader<ImageView> {
    private var mRequestManager: RequestManager? = null

    init {
        mRequestManager = when (target) {
            is Fragment -> Glide.with(target)
            is FragmentActivity -> Glide.with(target)
            is Activity -> Glide.with(target)
            is Context -> Glide.with(target)
            is View -> Glide.with(target)
            else -> throw IllegalArgumentException()
        }
    }

    override fun load(imageView: ImageView, url: String?, placeHolder: Int) {
        val centerCrop: Transformation<Bitmap> = CenterCrop()
        mRequestManager!!.load(url).optionalTransform(centerCrop)
            .optionalTransform(WebpDrawable::class.java, WebpDrawableTransformation(centerCrop))
            .placeholder(placeHolder)
            .into(imageView)
    }

    override fun load(imageView: ImageView, url: String?) {
        val centerCrop: Transformation<Bitmap> = CenterCrop()
        mRequestManager!!.load(url).optionalTransform(centerCrop)
            .optionalTransform(WebpDrawable::class.java, WebpDrawableTransformation(centerCrop))
            .placeholder(R.drawable.imageloader_default_cover_bg)
            .into(imageView)
    }

    override fun load(imageView: ImageView, resId: Int) {
        val centerCrop: Transformation<Bitmap> = CenterCrop()
        mRequestManager!!.load(resId).optionalTransform(centerCrop)
            .optionalTransform(WebpDrawable::class.java, WebpDrawableTransformation(centerCrop))
            .placeholder(R.drawable.imageloader_default_cover_bg)
            .into(imageView)
    }

    override fun load(
        imageView: ImageView,
        url: String?,
        placeHolder: Int,
        bitmapTransformation: BitmapTransformation?
    ) {
        mRequestManager!!.load(url).optionalTransform(bitmapTransformation!!)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(bitmapTransformation)
            )
            .placeholder(placeHolder).into(imageView)
    }

    @SuppressLint("CheckResult")
    override fun loadRounded(imageView: ImageView, url: String?, placeHolder: Int, radius: Float) {
        RequestOptions().apply {
            if (radius >= 0) {
                transform(CenterCrop(), RoundedCorners(dp2px(imageView.context, radius)))
            } else {
                transform(RoundedCorners(dp2px(imageView.context, radius)))
            }
        }.also { options ->
            mRequestManager!!.load(url)
                .placeholder(placeHolder)
                .apply(options)
                .into(imageView)
        }
    }

    @SuppressLint("CheckResult")
    override fun loadRounded(
        imageView: ImageView,
        url: String?,
        placeHolder: Drawable?,
        radius: Float
    ) {
        RequestOptions().apply {
            if (radius >= 0) {
                transform(CenterCrop(), RoundedCorners(dp2px(imageView.context, radius)))
            } else {
                transform(RoundedCorners(dp2px(imageView.context, radius)))
            }
        }.also { options ->
            mRequestManager!!.load(url)
                .placeholder(placeHolder)
                .apply(options)
                .into(imageView)
        }
    }

    override fun loadRounded(imageView: ImageView, url: String?, radius: Float) {
        loadRounded(imageView, url, R.drawable.imageloader_default_cover_bg, radius)
    }

    override fun loadCircle(imageView: ImageView, url: String?, placeHolder: Int) {
        mRequestManager?.apply {
            this.load(url)
                .placeholder(placeHolder)
                .apply(RequestOptions.bitmapTransform(CircleCrop()))
                .into(imageView)
        }
    }

    override fun loadCircle(imageView: ImageView, url: String?) {
        mRequestManager?.apply {
            this.load(url)
                .placeholder(R.drawable.imageloader_default_cover_bg)
                .apply(RequestOptions.bitmapTransform(CircleCrop()))
                .into(imageView)
        }
    }

    override fun loadCircle(imageView: ImageView, resId: Int, placeHolder: Int) {
        mRequestManager?.apply {
            this.load(resId)
                .placeholder(placeHolder)
                .apply(RequestOptions.bitmapTransform(CircleCrop()))
                .into(imageView)
        }
    }

    override fun loadCircle(imageView: ImageView, resId: Int) {
        mRequestManager?.apply {
            this.load(resId)
                .placeholder(R.drawable.imageloader_default_cover_bg)
                .apply(RequestOptions.bitmapTransform(CircleCrop()))
                .into(imageView)
        }
    }

    override fun loadAsBitmap(
        url: String?,
        width: Int,
        height: Int,
        listener: ILoader.OnLoadedResultListener
    ) {
        mRequestManager?.apply {
            this.asBitmap()
                .centerCrop()
                .load(url)
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        listener.onLoadedFailed(e)
                        return true
                    }

                    override fun onResourceReady(
                        resource: Bitmap?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        listener.onLoadedSuccess(resource)
                        return true
                    }

                })
                .submit(width, height)
        }
    }

    private fun dp2px(context: Context, dpValue: Float): Int {
        val scale: Float = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
}