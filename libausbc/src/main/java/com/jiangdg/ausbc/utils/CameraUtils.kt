package com.jiangdg.ausbc.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import com.jiangdg.ausbc.R
import com.jiangdg.usb.DeviceFilter

/** Camera tools
 *
 * @author Created by jiangdg on 2022/7/19
 */
object CameraUtils {

    /**
     * check is usb camera
     *
     * @param device see [UsbDevice]
     * @return true usb camera
     */
    fun isUsbCamera(device: UsbDevice?): Boolean {
        return when (device?.deviceClass) {
            UsbConstants.USB_CLASS_VIDEO -> {
                true
            }
            UsbConstants.USB_CLASS_MISC -> {
                var isVideo = false
                for (i in 0 until device.interfaceCount) {
                    val cls = device.getInterface(i).interfaceClass
                    if (cls == UsbConstants.USB_CLASS_VIDEO) {
                        isVideo = true
                        break
                    }
                }
                isVideo
            }
            else -> {
                false
            }
        }
    }

    /**
     * Is camera contains mic
     *
     * @param device usb device
     * @return true contains
     */
    fun isCameraContainsMic(device: UsbDevice?): Boolean {
//        device ?: return false
//        var hasMic = false
//        for (i in 0 until device.interfaceCount) {
//            val cls = device.getInterface(i).interfaceClass
//            if (cls == UsbConstants.USB_CLASS_AUDIO) {
//                hasMic = true
//                break
//            }
//        }
//        return hasMic
        return false
    }

    /**
     * Filter needed usb device by according to filter regular
     *
     * @param context context
     * @param usbDevice see [UsbDevice]
     * @return true find success
     */
    fun isFilterDevice(context: Context?, usbDevice: UsbDevice?): Boolean {
        return DeviceFilter.getDeviceFilters(context, R.xml.default_device_filter)
            .find { devFilter ->
                devFilter.mProductId == usbDevice?.productId && devFilter.mVendorId == usbDevice.vendorId
            }.let { dev ->
                dev != null
            }
    }

    fun hasAudioPermission(ctx: Context): Boolean{
        val locPermission = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
        return locPermission == PackageManager.PERMISSION_GRANTED
    }

    fun hasStoragePermission(ctx: Context): Boolean{
        val locPermission = ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return locPermission == PackageManager.PERMISSION_GRANTED
    }

    fun hasCameraPermission(ctx: Context): Boolean{
        val locPermission = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
        return locPermission == PackageManager.PERMISSION_GRANTED
    }

    fun hasUVCCamera(context: Context): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val usbDevices = usbManager.deviceList
        for (device in usbDevices.values) {
            if (isUsbCamera(device) || isFilterDevice(context, device)) {
                return true
            }
        }
        return false
    }
}