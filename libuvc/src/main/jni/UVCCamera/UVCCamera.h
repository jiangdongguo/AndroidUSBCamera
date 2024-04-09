/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 * File name: UVCCamera.h
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 * Files in the jni/libjpeg, jni/libusb, jin/libuvc, jni/rapidjson folder may have a different license, see the respective files.
*/

#pragma once

#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <android/native_window.h>
#include "UVCStatusCallback.h"
#include "UVCButtonCallback.h"
#include "UVCCameraAdjustments.h"
#include "UVCPreviewJni.h"
#include <memory>
#include <chrono>

class UVCCamera {
private:
	char *mUsbFs;
	uvc_context_t *mContext;
	int mFd;
	uvc_device_t *mDevice;
	uvc_device_handle_t *mDeviceHandle;
	UVCStatusCallback *mStatusCallback;
	UVCButtonCallback *mButtonCallback;
    std::shared_ptr<UVCPreviewJni> mPreviewOld;
    std::shared_ptr<UVCCameraAdjustments> mCameraConfig;
	void clearCameraParams();
public:
	UVCCamera();
	~UVCCamera();

	int connect(int vid, int pid, int fd, int busnum, int devaddr, const char *usbfs);
	int release();

	int setStatusCallback(JNIEnv *env, jobject status_callback_obj);
	int setButtonCallback(JNIEnv *env, jobject button_callback_obj);

	char *getSupportedSize();
	int setFrameCallback(JNIEnv *env, jobject frame_callback_obj, int pixel_format);

	int getCtrlSupports(uint64_t *supports);
	int getProcSupports(uint64_t *supports);

    std::shared_ptr<UVCPreviewJni> getPreviewOldObject();
    std::shared_ptr<UVCCameraAdjustments> getAdjustments();
};
