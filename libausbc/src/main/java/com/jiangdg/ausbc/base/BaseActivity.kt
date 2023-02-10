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
package com.jiangdg.ausbc.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.jiangdg.ausbc.utils.ActivityStackUtils

/** Base Activity
 *  Extended from AppCompatActivity which implemented LifecycleOwner.
 *
 * @author Created by jiangdg on 2022/1/28
 */
abstract class BaseActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getRootView(layoutInflater))
        initView()
        initData()
        ActivityStackUtils.pushActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        clear()
        ActivityStackUtils.removeActivity(this)
    }

    protected abstract fun getRootView(layoutInflater: LayoutInflater): View?
    protected open fun initView() {}
    protected open fun initData() {}
    protected open fun clear() {}
}