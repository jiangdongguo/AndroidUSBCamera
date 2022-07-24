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
                mCameraClient?.requestPermission(device)
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
                    onCameraDisConnected(this)
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
}