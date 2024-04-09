/*
 * UVCPreviewJni
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 * Copyright (c) 2024 vschryabets@gmail.com
 *
 * File name: UVCPreview.cpp
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

#include <stdlib.h>
#include "UVCPreviewJni.h"

int UVCPreviewJni::setPreviewDisplay(ANativeWindow *preview_window) {
    pthread_mutex_lock(&preview_mutex);
    {
        if (mPreviewWindow != preview_window) {
            if (mPreviewWindow)
                ANativeWindow_release(mPreviewWindow);
            mPreviewWindow = preview_window;
            if (LIKELY(mPreviewWindow)) {
                ANativeWindow_setBuffersGeometry(mPreviewWindow,
                                                 frameWidth, frameHeight, WINDOW_FORMAT_RGBA_8888);
            }
        }
    }
    pthread_mutex_unlock(&preview_mutex);
    RETURN(0, int);
}

int UVCPreviewJni::setCaptureDisplay(ANativeWindow *capture_window) {
    /*pthread_mutex_lock(&capture_mutex);
    {
        if (isRunning() && isCapturing()) {
            mIsCapturing = false;
            if (mCaptureWindow) {
                pthread_cond_signal(&capture_sync);
                pthread_cond_wait(&capture_sync, &capture_mutex);    // wait finishing capturing
            }
        }
        if (mCaptureWindow != capture_window) {
            // release current Surface if already assigned.
            if (UNLIKELY(mCaptureWindow))
                ANativeWindow_release(mCaptureWindow);
            mCaptureWindow = capture_window;
            // if you use Surface came from MediaCodec#createInputSurface
            // you could not change window format at least when you use
            // ANativeWindow_lock / ANativeWindow_unlockAndPost
            // to write frame data to the Surface...
            // So we need check here.
            if (mCaptureWindow) {
                int32_t window_format = ANativeWindow_getFormat(mCaptureWindow);
                if ((window_format != WINDOW_FORMAT_RGB_565)
                    && (previewFormat == WINDOW_FORMAT_RGB_565)) {
                    LOGE("window format mismatch, cancelled movie capturing.");
                    ANativeWindow_release(mCaptureWindow);
                    mCaptureWindow = NULL;
                }
            }
        }
    }
    pthread_mutex_unlock(&capture_mutex);*/
    return 0;
}

UVCPreviewJni::UVCPreviewJni(uvc_device_handle_t *devh)
        : UVCPreviewBase(devh),
          mPreviewWindow(NULL),
          mCaptureWindow(NULL),
          mFrameCallbackObj(NULL) {

}

UVCPreviewJni::~UVCPreviewJni() {
    if (mPreviewWindow)
        ANativeWindow_release(mPreviewWindow);
    mPreviewWindow = NULL;
    if (mCaptureWindow)
        ANativeWindow_release(mCaptureWindow);
    mCaptureWindow = NULL;
}

int UVCPreviewJni::stopPreview() {
    auto res = UVCPreviewBase::stopPreview();
    clearDisplay();
    // check preview mutex available
    if (pthread_mutex_lock(&preview_mutex) == 0) {
        if (mPreviewWindow) {
            ANativeWindow_release(mPreviewWindow);
            mPreviewWindow = NULL;
        }
        pthread_mutex_unlock(&preview_mutex);
    }
//    if (pthread_mutex_lock(&capture_mutex) == 0) {
//        if (mCaptureWindow) {
//            ANativeWindow_release(mCaptureWindow);
//            mCaptureWindow = NULL;
//        }
//        pthread_mutex_unlock(&capture_mutex);
//    }
    return res;
}

int UVCPreviewJni::setFrameCallback(JNIEnv *env, jobject frame_callback_obj, int pixel_format) {
//    pthread_mutex_lock(&capture_mutex);
//    {
//        if (isRunning() && isCapturing()) {
//            mIsCapturing = false;
//            if (mFrameCallbackObj) {
//                pthread_cond_signal(&capture_sync);
//                pthread_cond_wait(&capture_sync, &capture_mutex);    // wait finishing capturing
//            }
//        }
//        if (!env->IsSameObject(mFrameCallbackObj, frame_callback_obj)) {
//            iframecallback_fields.onFrame = NULL;
//            if (mFrameCallbackObj) {
//                env->DeleteGlobalRef(mFrameCallbackObj);
//            }
//            mFrameCallbackObj = frame_callback_obj;
//            if (frame_callback_obj) {
//                // get method IDs of Java object for callback
//                jclass clazz = env->GetObjectClass(frame_callback_obj);
//                if (LIKELY(clazz)) {
//                    iframecallback_fields.onFrame = env->GetMethodID(clazz,
//                                                                     "onFrame", "(Ljava/nio/ByteBuffer;)V");
//                } else {
//                    LOGW("failed to get object class");
//                }
//                env->ExceptionClear();
//                if (!iframecallback_fields.onFrame) {
//                    LOGE("Can't find IFrameCallback#onFrame");
//                    env->DeleteGlobalRef(frame_callback_obj);
//                    mFrameCallbackObj = frame_callback_obj = NULL;
//                }
//            }
//        }
//        if (frame_callback_obj) {
//            mPixelFormat = pixel_format;
//            callbackPixelFormatChanged();
//        }
//    }
//    pthread_mutex_unlock(&capture_mutex);
    return 0;
}

void UVCPreviewJni::clearDisplay() {
    ANativeWindow_Buffer buffer;
//    pthread_mutex_lock(&capture_mutex);
//    {
//        if (LIKELY(mCaptureWindow)) {
//            if (LIKELY(ANativeWindow_lock(mCaptureWindow, &buffer, NULL) == 0)) {
//                uint8_t *dest = (uint8_t *) buffer.bits;
//                const size_t bytes = buffer.width * PREVIEW_PIXEL_BYTES;
//                const int stride = buffer.stride * PREVIEW_PIXEL_BYTES;
//                for (int i = 0; i < buffer.height; i++) {
//                    memset(dest, 0, bytes);
//                    dest += stride;
//                }
//                ANativeWindow_unlockAndPost(mCaptureWindow);
//            }
//        }
//    }
//    pthread_mutex_unlock(&capture_mutex);
    pthread_mutex_lock(&preview_mutex);
    {
        if (LIKELY(mPreviewWindow)) {
            if (LIKELY(ANativeWindow_lock(mPreviewWindow, &buffer, NULL) == 0)) {
                uint8_t *dest = (uint8_t *) buffer.bits;
                const size_t bytes = buffer.width * PREVIEW_PIXEL_BYTES;
                const int stride = buffer.stride * PREVIEW_PIXEL_BYTES;
                for (int i = 0; i < buffer.height; i++) {
                    memset(dest, 0, bytes);
                    dest += stride;
                }
                ANativeWindow_unlockAndPost(mPreviewWindow);
            }
        }
    }
    pthread_mutex_unlock(&preview_mutex);
}

void UVCPreviewJni::handleFrame(uvc_frame_t *pFrame) {
    if (frameMode) {
        // MJPEG mode
        LOGD("ASD got frame_mjpeg");
        if (LIKELY(pFrame)) {
            auto frame = get_frame(pFrame->width * pFrame->height * 2);
            auto result = uvc_mjpeg2yuyv(pFrame, frame);   // MJPEG => yuyv
            if (LIKELY(!result)) {
                frame = draw_preview_one(frame, &mPreviewWindow, uvc_any2rgbx, 4);
            }
            recycle_frame(frame);
        }
    } else {
        // yuvyv mode
        auto frame = pFrame;
        if (LIKELY(frame)) {
            frame = draw_preview_one(frame, &mPreviewWindow, uvc_any2rgbx, 4);
        }
    }
}

void UVCPreviewJni::onPreviewPrepared(uint16_t frameWidth,
                                      uint16_t frameHeight) {
    pthread_mutex_lock(&preview_mutex);
    if (LIKELY(mPreviewWindow)) {
        ANativeWindow_setBuffersGeometry(mPreviewWindow,
                                         frameWidth, frameHeight, WINDOW_FORMAT_RGBA_8888);
    }
    pthread_mutex_unlock(&preview_mutex);

}

static void
copyFrame(
        const uint8_t *src,
        uint8_t *dest,
        const int width, int height,
        const int stride_src,
        const int stride_dest) {
    const int h8 = height % 8;
    for (int i = 0; i < h8; i++) {
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
    }
    for (int i = 0; i < height; i += 8) {
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
        memcpy(dest, src, width);
        dest += stride_dest;
        src += stride_src;
    }
}

// transfer specific frame data to the Surface(ANativeWindow)
int copyToSurface(uvc_frame_t *frame, ANativeWindow **window) {
    if (LIKELY(*window)) {
        ANativeWindow_Buffer buffer;
        if (LIKELY(ANativeWindow_lock(*window, &buffer, NULL) == 0)) {
            // source = frame data
            const uint8_t *src = (uint8_t *) frame->data;
            const int src_w = frame->width * PREVIEW_PIXEL_BYTES;
            const int src_step = frame->width * PREVIEW_PIXEL_BYTES;
            // destination = Surface(ANativeWindow)
            uint8_t *dest = (uint8_t *) buffer.bits;
            const int dest_w = buffer.width * PREVIEW_PIXEL_BYTES;
            const int dest_step = buffer.stride * PREVIEW_PIXEL_BYTES;
            // use lower transfer bytes
            const int w = src_w < dest_w ? src_w : dest_w;
            // use lower height
            const int h = frame->height < buffer.height ? frame->height : buffer.height;
            // transfer from frame data to the Surface
            copyFrame(src, dest, w, h, src_step, dest_step);
            ANativeWindow_unlockAndPost(*window);
            return 0;
        }
    }
    return -1;
}

// changed to return original frame instead of returning converted frame even if convert_func is not null.
uvc_frame_t *UVCPreviewJni::draw_preview_one(
        uvc_frame_t *frame,
        ANativeWindow **window,
        convFunc_t convert_func,
        int pixcelBytes) {
    int b = 0;
    pthread_mutex_lock(&preview_mutex);
    {
        b = *window != NULL;
    }
    pthread_mutex_unlock(&preview_mutex);
    if (LIKELY(b)) {
        uvc_frame_t *converted;
        if (convert_func) {
            converted = get_frame(frame->width * frame->height * pixcelBytes);
            if LIKELY(converted)
            {
                b = convert_func(frame, converted);
                if (!b) {
                    pthread_mutex_lock(&preview_mutex);
                    copyToSurface(converted, window);
                    pthread_mutex_unlock(&preview_mutex);
                } else {
                    LOGE("failed converting");
                }
                recycle_frame(converted);
            }
        } else {
            pthread_mutex_lock(&preview_mutex);
            copyToSurface(frame, window);
            pthread_mutex_unlock(&preview_mutex);
        }
    }
    return frame; //RETURN(frame, uvc_frame_t *);
}
