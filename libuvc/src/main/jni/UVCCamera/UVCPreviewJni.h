/*
 * UVCPreviewJni
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 * Copyright (c) 2024 vshcryabets@gmail.com
 *
 * File name: UVCPreview.h
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

#include "UVCPreviewBase.h"
#include <android/native_window.h>

class UVCPreviewJni: public UVCPreviewBase {
private:
    ANativeWindow *mPreviewWindow;
    ANativeWindow *mCaptureWindow;
    jobject mFrameCallbackObj;
private:
    void clearDisplay();
    uvc_frame_t *draw_preview_one(uvc_frame_t *frame, ANativeWindow **window, convFunc_t func, int pixelBytes);
protected:
    virtual void handleFrame(uvc_frame_t *pFrame);
    virtual void onPreviewPrepared(uint16_t frameWidth, uint16_t  frameHeight);
public:
    UVCPreviewJni(uvc_device_handle_t *devh);
    ~UVCPreviewJni();

    int setPreviewDisplay(ANativeWindow *preview_window);
    int setCaptureDisplay(ANativeWindow *capture_window);
    virtual int stopPreview();
    int setFrameCallback(JNIEnv *env, jobject frame_callback_obj, int pixel_format);
};