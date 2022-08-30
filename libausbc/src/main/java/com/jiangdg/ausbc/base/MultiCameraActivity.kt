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
package com.jiangdg.ausbc.base

import android.hardware.usb.UsbDevice
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack
import com.serenegiant.usb.USBMonitor

/** Multi-road camera activity
 *
 * @author Created by jiangdg on 2022/7/20
 */
abstract class MultiCameraActivity: BaseActivity() {
    private var mCameraClient: MultiCameraClient? = null
    private val mCameraMap = hashMapOf<Int, MultiCameraClient.Camera>()

    override fun initData() {
        mCameraClient = MultiCameraClient(this, object : IDeviceConnectCallBack {
            override fun onAttachDev(device: UsbDevice?) {
                device ?: return
                MultiCameraClient.Camera(this@MultiCameraActivity, device).apply {
                    mCameraMap[device.deviceId] = this
                    onCameraAttached(this)
                }
                if (isAutoRequestPermission()) {
                    mCameraClient?.requestPermission(device)
                }
            }

            override fun onDetachDec(device: UsbDevice?) {
                mCameraMap.remove(device?.deviceId)?.apply {
                    setUsbControlBlock(null)
                    onCameraDetached(this)
                }
            }

            override fun onConnectDev(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                device ?: return
                ctrlBlock ?: return
                mCameraMap[device.deviceId]?.apply {
                    setUsbControlBlock(ctrlBlock)
                    onCameraConnected(this)
                }
            }

            override fun onDisConnectDec(
                device: UsbDevice?,
                ctrlBlock: USBMonitor.UsbControlBlock?
            ) {
                mCameraMap[device?.deviceId]?.apply {
                    onCameraDisConnected(this)
                }
            }

            override fun onCancelDev(device: UsbDevice?) {
                mCameraMap[device?.deviceId]?.apply {
                    onCameraDisConnected(this)
                }
            }
        })
        mCameraClient?.register()
    }

    override fun clear() {
        mCameraMap.values.forEach {
            it.closeCamera()
        }
        mCameraMap.clear()
        mCameraClient?.unRegister()
        mCameraClient?.destroy()
        mCameraClient = null
    }

    /**
     * On camera connected
     *
     * @param camera see [MultiCameraClient.Camera]
     */
    protected abstract fun onCameraConnected(camera: MultiCameraClient.Camera)

    /**
     * On camera disconnected
     *
     * @param camera see [MultiCameraClient.Camera]
     */
    protected abstract fun onCameraDisConnected(camera: MultiCameraClient.Camera)

    /**
     * On camera attached
     *
     * @param camera see [MultiCameraClient.Camera]
     */
    protected abstract fun onCameraAttached(camera: MultiCameraClient.Camera)

    /**
     * On camera detached
     *
     * @param camera see [MultiCameraClient.Camera]
     */
    protected abstract fun onCameraDetached(camera: MultiCameraClient.Camera)

    /**
     * Get current connected cameras
     */
    protected fun getCameraMap() = mCameraMap

    /**
     * Get all usb device list
     */
    protected fun getDeviceList() = mCameraClient?.getDeviceList()

    /**
     * Get camera client
     */
    protected fun getCameraClient() = mCameraClient

    /**
     * Is auto request permission
     * default is true
     */
    protected fun isAutoRequestPermission() = true

    /**
     * Request permission
     *
     * @param device see [UsbDevice]
     */
    protected fun requestPermission(device: UsbDevice?) {
        mCameraClient?.requestPermission(device)
    }

    /**
     * Has permission
     *
     * @param device see [UsbDevice]
     */
    protected fun hasPermission(device: UsbDevice?) = mCameraClient?.hasPermission(device) == true

    protected fun openDebug(debug: Boolean) {
        mCameraClient?.openDebug(debug)
    }
}