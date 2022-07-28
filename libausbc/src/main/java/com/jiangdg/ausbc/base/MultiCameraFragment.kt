package com.jiangdg.ausbc.base

import android.hardware.usb.UsbDevice
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack
import com.serenegiant.usb.USBMonitor

/** Multi-road camera fragment
 *
 * @author Created by jiangdg on 2022/7/20
 */
abstract class MultiCameraFragment: BaseFragment() {
    private var mCameraClient: MultiCameraClient? = null
    private val mCameraMap = hashMapOf<Int, MultiCameraClient.Camera>()

    override fun initData() {
        mCameraClient = MultiCameraClient(requireContext(), object : IDeviceConnectCallBack {
            override fun onAttachDev(device: UsbDevice?) {
                device ?: return
                context?.let {
                    if (mCameraMap.containsKey(device.deviceId)) {
                        return
                    }
                    MultiCameraClient.Camera(it, device).apply {
                        mCameraMap[device.deviceId] = this
                        onCameraAttached(this)
                    }
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
                context ?: return
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