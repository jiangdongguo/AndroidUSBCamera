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

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment

/**
 * Image loaders
 *
 * @author Created by jiangdg on 2022/3/16
 */
object ImageLoaders {
    /**
     * create a glide image loader
     *
     * @param fragment target is fragment
     * @return [GlideLoader]
     */
    fun of(fragment: Fragment): ILoader<ImageView> = GlideLoader(fragment)

    /**
     * create a glide image loader
     *
     * @param activity target is activity
     * @return [GlideLoader]
     */
    fun of(activity: Activity): ILoader<ImageView> = GlideLoader(activity)

    /**
     * create a glide image loader
     *
     * @param context target is context
     * @return [GlideLoader]
     */
    fun of(context: Context): ILoader<ImageView> = GlideLoader(context)

    /**
     * create a glide image loader
     *
     * @param view target is view
     * @return [GlideLoader]
     */
    fun of(view: View): ILoader<ImageView> = GlideLoader(view)
}