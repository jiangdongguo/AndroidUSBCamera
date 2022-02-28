package com.jiangdg.usbcamera

import android.Manifest.permission.*
import android.os.Bundle
import android.os.PowerManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import com.gyf.immersionbar.ImmersionBar
import com.jiangdg.media.utils.ToastUtils
import com.jiangdg.media.utils.Utils
import com.jiangdg.usbcamera.databinding.ActivityMainBinding

/**
 * Demos of camera usage
 *
 * @author Created by jiangdg on 2021/12/27
 */
class MainActivity : AppCompatActivity() {
    private var mWakeLock: PowerManager.WakeLock? = null
    private var immersionBar: ImmersionBar? = null
    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatusBar()
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        replaceDemoFragment(DemoFragment())
//        replaceDemoFragment(GlSurfaceFragment())
    }

    override fun onStart() {
        super.onStart()
        mWakeLock = Utils.wakeLock(this)
    }

    override fun onStop() {
        super.onStop()
        mWakeLock?.apply {
            Utils.wakeUnLock(this)
        }
    }

    private fun replaceDemoFragment(fragment: Fragment) {
        val hasCameraPermission = PermissionChecker.checkSelfPermission(this, CAMERA)
        val hasStoragePermission =
            PermissionChecker.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
        if (hasCameraPermission != PermissionChecker.PERMISSION_GRANTED || hasStoragePermission != PermissionChecker.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA)) {
                ToastUtils.show(R.string.permission_tip)
            }
            ActivityCompat.requestPermissions(
                this,
                arrayOf(CAMERA, WRITE_EXTERNAL_STORAGE, RECORD_AUDIO),
                REQUEST_CAMERA
            )
            return
        }
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commitAllowingStateLoss()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA -> {
                val hasCameraPermission = PermissionChecker.checkSelfPermission(this, CAMERA)
                if (hasCameraPermission == PermissionChecker.PERMISSION_DENIED) {
                    ToastUtils.show(R.string.permission_tip)
                    return
                }
                replaceDemoFragment(DemoFragment())
//                replaceDemoFragment(GlSurfaceFragment())
            }
            REQUEST_STORAGE -> {
                val hasCameraPermission =
                    PermissionChecker.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
                if (hasCameraPermission == PermissionChecker.PERMISSION_DENIED) {
                    ToastUtils.show(R.string.permission_tip)
                    return
                }
                // todo
            }
            else -> {
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        immersionBar= null
    }

    private fun setStatusBar() {
        immersionBar = ImmersionBar.with(this)
            .statusBarDarkFont(false)
            .statusBarColor(R.color.black)
            .navigationBarColor(R.color.black)
            .fitsSystemWindows(true)
            .keyboardEnable(true)
        immersionBar?.init()
    }

    companion object {
        private const val REQUEST_CAMERA = 0
        private const val REQUEST_STORAGE = 1
    }
}