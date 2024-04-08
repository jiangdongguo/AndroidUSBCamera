/*
 * Copyright 2024 vschryabets@gmail.com
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
package com.vshcryabets.activity

import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.jiangdg.demo.MainActivity
import com.vshcryabets.screens.DeviceListScreen
import com.vshcryabets.screens.DeviceListViewModel
import com.vshcryabets.screens.DeviceListViewModelFactory
import kotlinx.coroutines.launch


class DevicesActivity : ComponentActivity() {

    lateinit var viewModel: DeviceListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(
            this, DeviceListViewModelFactory(
                usbManager = applicationContext.getSystemService(USB_SERVICE) as UsbManager
            )
        )
            .get(DeviceListViewModel::class.java)
        setContent {
            DeviceListScreen.ScreenContent(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.begin()

        lifecycleScope.launch {
            viewModel.state.collect {
                if (it.openPreviewDeviceId != null) {
                    viewModel.onPreviewOpened()
                    val intent = MainActivity.newInstance(applicationContext, it.openPreviewDeviceId)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onPause() {
        viewModel.stop()
        super.onPause()
    }

}