package com.jiangdg.demo

import android.content.Context
import android.hardware.usb.UsbDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.MultiCameraFragment
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.camera.CameraUVC
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.utils.ToastUtils
import com.jiangdg.demo.databinding.FragmentMultiCameraBinding

/** Multi-road camera demo
 *
 * @author Created by jiangdg on 2022/7/20
 */
class DemoMultiCameraFragment : MultiCameraFragment(), ICameraStateCallBack {
    private lateinit var mAdapter: CameraAdapter
    private lateinit var mViewBinding: FragmentMultiCameraBinding
    private val mCameraList by lazy {
        ArrayList<MultiCameraClient.ICamera>()
    }
    private val mHasRequestPermissionList by lazy {
        ArrayList<MultiCameraClient.ICamera>()
    }

    override fun onCameraAttached(camera: MultiCameraClient.ICamera) {
        mAdapter.data.add(camera)
        mAdapter.notifyItemInserted(mAdapter.data.size - 1)
        mViewBinding.multiCameraTip.visibility = View.GONE
    }

    override fun onCameraDetached(camera: MultiCameraClient.ICamera) {
        mHasRequestPermissionList.remove(camera)
        for ((position, cam) in mAdapter.data.withIndex()) {
            if (cam.getUsbDevice().deviceId == camera.getUsbDevice().deviceId) {
                camera.closeCamera()
                mAdapter.data.removeAt(position)
                mAdapter.notifyItemRemoved(position)
                break
            }
        }
        if (mAdapter.data.isEmpty()) {
            mViewBinding.multiCameraTip.visibility = View.VISIBLE
        }
    }

    override fun generateCamera(ctx: Context, device: UsbDevice): MultiCameraClient.ICamera {
        return CameraUVC(ctx, device)
    }

    override fun onCameraConnected(camera: MultiCameraClient.ICamera) {
        for ((position, cam) in mAdapter.data.withIndex()) {
            if (cam.getUsbDevice().deviceId == camera.getUsbDevice().deviceId) {
                val textureView = mAdapter.getViewByPosition(position, R.id.multi_camera_texture_view)
                cam.openCamera(textureView, getCameraRequest())
                cam.setCameraStateCallBack(this)
                break
            }
        }
        // request permission for other camera
        mAdapter.data.forEach { cam ->
            val device = cam.getUsbDevice()
            if (! hasPermission(device)) {
                mHasRequestPermissionList.add(cam)
                requestPermission(device)
                return@forEach
            }
        }
    }

    override fun onCameraDisConnected(camera: MultiCameraClient.ICamera) {
        camera.closeCamera()
    }


    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {
        if (code == ICameraStateCallBack.State.ERROR) {
            ToastUtils.show(msg ?: "open camera failed.")
        }
        for ((position, cam) in mAdapter.data.withIndex()) {
            if (cam.getUsbDevice().deviceId == self.getUsbDevice().deviceId) {
                mAdapter.notifyItemChanged(position, "switch")
                break
            }
        }
    }


    override fun initView() {
        super.initView()
        openDebug(true)
        mAdapter = CameraAdapter()
        mAdapter.setNewData(mCameraList)
        mAdapter.bindToRecyclerView(mViewBinding.multiCameraRv)
        mViewBinding.multiCameraRv.adapter = mAdapter
        mViewBinding.multiCameraRv.layoutManager = GridLayoutManager(requireContext(), 2)
        mAdapter.setOnItemChildClickListener { adapter, view, position ->
            val camera = adapter.data[position] as MultiCameraClient.ICamera
            when (view.id) {
                R.id.multi_camera_capture_image -> {
                    camera.captureImage(object : ICaptureCallBack {
                        override fun onBegin() {}

                        override fun onError(error: String?) {
                            ToastUtils.show(error ?: "capture image failed")
                        }

                        override fun onComplete(path: String?) {
                            ToastUtils.show(path ?: "capture image success")
                        }
                    })
                }
                R.id.multi_camera_capture_video -> {
                    if (camera.isRecording()) {
                        camera.captureVideoStop()
                        return@setOnItemChildClickListener
                    }
                    camera.captureVideoStart(object : ICaptureCallBack {
                        override fun onBegin() {
                            mAdapter.notifyItemChanged(position, "video")
                        }

                        override fun onError(error: String?) {
                            mAdapter.notifyItemChanged(position, "video")
                            ToastUtils.show(error ?: "capture video failed")
                        }

                        override fun onComplete(path: String?) {
                            mAdapter.notifyItemChanged(position, "video")
                            ToastUtils.show(path ?: "capture video success")
                        }
                    })
                }
                else -> {
                }
            }
        }
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        mViewBinding = FragmentMultiCameraBinding.inflate(inflater, container, false)
        return mViewBinding.root
    }

    private fun getCameraRequest(): CameraRequest {
        return CameraRequest.Builder()
            .setPreviewWidth(640)
            .setPreviewHeight(480)
            .create()
    }

    inner class CameraAdapter :
        BaseQuickAdapter<MultiCameraClient.ICamera, BaseViewHolder>(R.layout.layout_item_camera) {
        override fun convert(helper: BaseViewHolder, camera: MultiCameraClient.ICamera?) {}

        override fun convertPayloads(
            helper: BaseViewHolder,
            camera: MultiCameraClient.ICamera?,
            payloads: MutableList<Any>
        ) {
            camera ?: return
            if (payloads.isEmpty()) {
                return
            }
            helper.setText(R.id.multi_camera_name, camera.getUsbDevice().deviceName)
            helper.addOnClickListener(R.id.multi_camera_capture_video)
            helper.addOnClickListener(R.id.multi_camera_capture_image)
            // local update
            val switchIv = helper.getView<ImageView>(R.id.multi_camera_switch)
            val captureVideoIv = helper.getView<ImageView>(R.id.multi_camera_capture_video)
            if (payloads.find { "switch" == it } != null) {
                if (camera.isCameraOpened()) {
                    switchIv.setImageResource(R.mipmap.ic_switch_on)
                } else {
                    switchIv.setImageResource(R.mipmap.ic_switch_off)
                }
            }
            if (payloads.find { "video" == it } != null) {
                if (camera.isRecording()) {
                    captureVideoIv.setImageResource(R.mipmap.ic_capture_video_on)
                } else {
                    captureVideoIv.setImageResource(R.mipmap.ic_capture_video_off)
                }
            }
        }
    }
}