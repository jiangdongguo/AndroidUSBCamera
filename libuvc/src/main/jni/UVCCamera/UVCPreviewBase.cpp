/*
 * UVCCamera
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
#include <linux/time.h>
#include <unistd.h>
#include "utilbase.h"
#include "UVCPreviewBase.h"
#include "libuvc_internal.h"
#include <mutex>

#define LOCAL_DEBUG 1

struct timespec ts;
struct timeval tv;

UVCPreviewBase::UVCPreviewBase(uvc_device_handle_t *devh)
        : mDeviceHandle(devh),
          requestWidth(DEFAULT_PREVIEW_WIDTH),
          requestHeight(DEFAULT_PREVIEW_HEIGHT),
          requestMinFps(DEFAULT_PREVIEW_FPS_MIN),
          requestMaxFps(DEFAULT_PREVIEW_FPS_MAX),
          requestMode(DEFAULT_PREVIEW_MODE),
          requestBandwidth(DEFAULT_BANDWIDTH),
          frameWidth(DEFAULT_PREVIEW_WIDTH),
          frameHeight(DEFAULT_PREVIEW_HEIGHT),
          frameBytes(DEFAULT_PREVIEW_WIDTH * DEFAULT_PREVIEW_HEIGHT * 2),    // YUYV
          frameMode(0),
          previewBytes(DEFAULT_PREVIEW_WIDTH * DEFAULT_PREVIEW_HEIGHT * PREVIEW_PIXEL_BYTES),
          mIsRunning(false),
//          mIsCapturing(false),
//          captureQueu(NULL),
          mFrameCallbackFunc(NULL),
          callbackPixelBytes(2) {
    // 初始化并关联 capture_clock_attr
    //pthread_condattr_init(&capture_clock_attr);
    //pthread_condattr_setclock(&capture_clock_attr, CLOCK_MONOTONIC);
//    pthread_cond_init(&capture_sync, NULL);
//    pthread_mutex_init(&capture_mutex, NULL);
    pthread_mutex_init(&pool_mutex, NULL);
}

UVCPreviewBase::~UVCPreviewBase() {
    clearPreviewFramesQueue();
//    clearCaptureFrame();
    clear_pool();
    pthread_mutex_lock(&preview_mutex);
    pthread_mutex_destroy(&preview_mutex);
    pthread_cond_destroy(&preview_sync);
//    pthread_mutex_lock(&capture_mutex);
//    pthread_mutex_destroy(&capture_mutex);
//    pthread_cond_destroy(&capture_sync);
    // 释放 capture_clock_aatr
    // pthread_condattr_destroy(&capture_clock_attr);
    pthread_mutex_destroy(&pool_mutex);

}

/**
 * get uvc_frame_t from frame pool
 * if pool is empty, create new frame
 * this function does not confirm the frame size
 * and you may need to confirm the size
 */
uvc_frame_t *UVCPreviewBase::get_frame(size_t data_bytes) {
    uvc_frame_t *frame = NULL;
    pthread_mutex_lock(&pool_mutex);
    {
        if (!mFramePool.empty()) {
            frame = mFramePool.back();
            mFramePool.pop_back();
        }
    }
    pthread_mutex_unlock(&pool_mutex);
    if (UNLIKELY(!frame)) {
        allocatedFramesCounter++;
//        LOGI("ASD allocate new frame %d", allocatedFramesCounter);
        frame = uvc_allocate_frame(data_bytes);
    }
    return frame;
}

void UVCPreviewBase::recycle_frame(uvc_frame_t *frame) {
    pthread_mutex_lock(&pool_mutex);
    if (LIKELY(mFramePool.size() < FRAME_POOL_SZ)) {
        mFramePool.push_back(frame);
//        LOGI("ASD recycle_frame queue %d", mFramePool.size());
        frame = nullptr;
    }
    pthread_mutex_unlock(&pool_mutex);
    if (UNLIKELY(frame)) {
        uvc_free_frame(frame);
    }
}

void UVCPreviewBase::clear_pool() {
    pthread_mutex_lock(&pool_mutex);
    {
        for (const auto &frame: mFramePool)
            uvc_free_frame(frame);
        mFramePool.clear();
    }
    pthread_mutex_unlock(&pool_mutex);
}

inline const bool UVCPreviewBase::isRunning() const { return mIsRunning; }

int UVCPreviewBase::setPreviewSize(int width, int height, int min_fps, int max_fps, int mode, float bandwidth) {
    int result = 0;
    if ((requestWidth != width) || (requestHeight != height) || (requestMode != mode)) {
        requestWidth = width;
        requestHeight = height;
        requestMinFps = min_fps;
        requestMaxFps = max_fps;
        requestMode = mode;
        requestBandwidth = bandwidth;

        uvc_stream_ctrl_t ctrl;
        result = uvc_get_stream_ctrl_format_size_fps(mDeviceHandle, &ctrl,
                                                     !requestMode ? UVC_FRAME_FORMAT_YUYV : UVC_FRAME_FORMAT_MJPEG,
                                                     requestWidth, requestHeight, requestMinFps, requestMaxFps);
    }

    RETURN(result, int);
}

void UVCPreviewBase::callbackPixelFormatChanged() {
    mFrameCallbackFunc = NULL;
    const size_t sz = requestWidth * requestHeight;
    switch (mPixelFormat) {
        case PIXEL_FORMAT_RAW:
            LOGI("PIXEL_FORMAT_RAW:");
            callbackPixelBytes = sz * 2;
            break;
        case PIXEL_FORMAT_YUV:
            LOGI("PIXEL_FORMAT_YUV:");
            callbackPixelBytes = sz * 2;
            break;
        case PIXEL_FORMAT_RGB565:
            LOGI("PIXEL_FORMAT_RGB565:");
            mFrameCallbackFunc = uvc_any2rgb565;
            callbackPixelBytes = sz * 2;
            break;
        case PIXEL_FORMAT_RGBX:
            LOGI("PIXEL_FORMAT_RGBX:");
            mFrameCallbackFunc = uvc_any2rgbx;
            callbackPixelBytes = sz * 4;
            break;
        case PIXEL_FORMAT_YUV20SP:
            LOGI("PIXEL_FORMAT_YUV20SP:");
            mFrameCallbackFunc = uvc_yuyv2iyuv420SP;
            callbackPixelBytes = (sz * 3) / 2;
            break;
        case PIXEL_FORMAT_NV21:
            LOGI("PIXEL_FORMAT_NV21:");
            mFrameCallbackFunc = uvc_yuyv2yuv420SP;
            callbackPixelBytes = (sz * 3) / 2;
            break;
    }
}

int UVCPreviewBase::startPreview() {
    int result = EXIT_FAILURE;
    if (!isRunning()) {
        mIsRunning = true;
        pthread_mutex_lock(&preview_mutex);
        {
            result = pthread_create(&preview_thread, NULL, preview_thread_func, (void *) this);
        }
        pthread_mutex_unlock(&preview_mutex);
        if (UNLIKELY(result != EXIT_SUCCESS)) {
            LOGW("UVCCamera::window does not exist/already running/could not create thread etc.");
            mIsRunning = false;
            pthread_mutex_lock(&preview_mutex);
            {
                pthread_cond_signal(&preview_sync);
            }
            pthread_mutex_unlock(&preview_mutex);
        }
    }
    RETURN(result, int);
}

int UVCPreviewBase::stopPreview() {
    bool b = isRunning();
    if (LIKELY(b)) {
        mIsRunning = false;
        pthread_cond_signal(&preview_sync);
        // jiangdg:fix stopview crash
        // because of capture_thread may null when called do_preview()
//        if (mHasCapturing) {
//            pthread_cond_signal(&capture_sync);
//            if (capture_thread && pthread_join(capture_thread, NULL) != EXIT_SUCCESS) {
//                LOGW("UVCPreview::terminate capture thread: pthread_join failed");
//            }
//        }
        if (preview_thread && pthread_join(preview_thread, NULL) != EXIT_SUCCESS) {
            LOGW("UVCPreview::terminate preview thread: pthread_join failed");
        }
    }
//    mHasCapturing = false;
    clearPreviewFramesQueue();
//    clearCaptureFrame();
    return 0;
}

//**********************************************************************
//
//**********************************************************************
void UVCPreviewBase::uvc_preview_frame_callback(uvc_frame_t *frame, void *vptr_args) {
    UVCPreviewBase *preview = reinterpret_cast<UVCPreviewBase *>(vptr_args);
    if UNLIKELY(!preview->isRunning() || !frame || !frame->frame_format || !frame->data || !frame->data_bytes) return;
    if (UNLIKELY(
            ((frame->frame_format != UVC_FRAME_FORMAT_MJPEG) && (frame->actual_bytes < preview->frameBytes))
            || (frame->width != preview->frameWidth) || (frame->height != preview->frameHeight))) {

#if LOCAL_DEBUG
        LOGD("broken frame!:format=%d,actual_bytes=%d/%d(%d,%d/%d,%d)",
            frame->frame_format, frame->actual_bytes, preview->frameBytes,
            frame->width, frame->height, preview->frameWidth, preview->frameHeight);
#endif
        return;
    }
    if (LIKELY(preview->isRunning())) {
        uvc_frame_t *copy = preview->get_frame(frame->data_bytes);
        if (UNLIKELY(!copy)) {
#if LOCAL_DEBUG
            LOGE("uvc_callback:unable to allocate duplicate frame!");
#endif
            return;
        }
        uvc_error_t ret = uvc_duplicate_frame(frame, copy);
        if (UNLIKELY(ret)) {
            preview->recycle_frame(copy);
            return;
        }
        preview->addPreviewFrame(copy);
    }
}

void UVCPreviewBase::addPreviewFrame(uvc_frame_t *frame) {
    pthread_mutex_lock(&preview_mutex);
    if (isRunning() && (previewFrames.size() < MAX_FRAME)) {
        previewFrames.push_back(frame);
        frame = NULL;
        pthread_cond_signal(&preview_sync);
    }
    pthread_mutex_unlock(&preview_mutex);
    if (frame) {
        recycle_frame(frame);
    }
}

uvc_frame_t *UVCPreviewBase::waitPreviewFrame() {
    uvc_frame_t *frame = NULL;
    pthread_mutex_lock(&preview_mutex);
    {
        if (!previewFrames.size()) {
            pthread_cond_wait(&preview_sync, &preview_mutex);
        }
        if (LIKELY(isRunning() && !previewFrames.empty())) {
            frame = previewFrames.front();
            previewFrames.pop_front();
        }
    }
    pthread_mutex_unlock(&preview_mutex);
    return frame;
}

void UVCPreviewBase::clearPreviewFramesQueue() {
    pthread_mutex_lock(&preview_mutex);
    {
        for (const auto &frame: previewFrames) {
            recycle_frame(frame);
        }
        previewFrames.clear();
    }
    pthread_mutex_unlock(&preview_mutex);
}

void *UVCPreviewBase::preview_thread_func(void *vptr_args) {
    int result;
    UVCPreviewBase *preview = reinterpret_cast<UVCPreviewBase *>(vptr_args);
    if (LIKELY(preview)) {
        uvc_stream_ctrl_t ctrl;
        result = preview->prepare_preview(&ctrl);
        if (LIKELY(!result)) {
            preview->do_preview(&ctrl);
        }
    }
    pthread_exit(NULL);
}

int UVCPreviewBase::prepare_preview(uvc_stream_ctrl_t *ctrl) {
    uvc_error_t result = uvc_get_stream_ctrl_format_size_fps(mDeviceHandle, ctrl,
                                                             !requestMode ? UVC_FRAME_FORMAT_YUYV
                                                                          : UVC_FRAME_FORMAT_MJPEG,
                                                             requestWidth,
                                                             requestHeight,
                                                             requestMinFps,
                                                             requestMaxFps);
    if (LIKELY(!result)) {
        uvc_frame_desc_t *frame_desc;
        result = uvc_get_frame_desc(mDeviceHandle, ctrl, &frame_desc);
        if (LIKELY(!result)) {
            frameWidth = frame_desc->wWidth;
            frameHeight = frame_desc->wHeight;
            LOGI("frameSize=(%d,%d)@%s", frameWidth, frameHeight, (!requestMode ? "YUYV" : "MJPEG"));
            onPreviewPrepared(frameWidth, frameHeight);
        } else {
            frameWidth = requestWidth;
            frameHeight = requestHeight;
        }
        frameMode = requestMode;
        frameBytes = frameWidth * frameHeight * (!requestMode ? 2 : 4);
        previewBytes = frameWidth * frameHeight * PREVIEW_PIXEL_BYTES;
    } else {
        LOGE("could not negotiate with camera:err=%d", result);
    }
    RETURN(result, int);
}

void UVCPreviewBase::do_preview(uvc_stream_ctrl_t *ctrl) {
    uvc_error_t result = uvc_start_streaming_bandwidth(
            mDeviceHandle,
            ctrl,
            uvc_preview_frame_callback,
            (void *) this, requestBandwidth, 0);
    if (LIKELY(!result)) {
        clearPreviewFramesQueue();
        for (; LIKELY(isRunning());) {
            auto frame = waitPreviewFrame();
            handleFrame(frame);
            recycle_frame(frame);
        }
        uvc_stop_streaming(mDeviceHandle);
    } else {
        uvc_perror(result, "failed start_streaming");
    }
}


//void UVCPreviewBase::addCaptureFrame(uvc_frame_t *frame) {
//    pthread_mutex_lock(&capture_mutex);
//    if (LIKELY(isRunning())) {
//        // keep only latest one
//        if (captureQueu) {
//            recycle_frame(captureQueu);
//        }
//        captureQueu = frame;
//        pthread_cond_broadcast(&capture_sync);
//    } else {
//        // Add this can solve native leak
//        recycle_frame(frame);
//    }
//    pthread_mutex_unlock(&capture_mutex);
//}
//
///**
// * get frame data for capturing, if not exist, block and wait
// */
//uvc_frame_t *UVCPreviewBase::waitCaptureFrame() {
//    uvc_frame_t *frame = NULL;
//    pthread_mutex_lock(&capture_mutex);
//    {
//        if (!captureQueu) {
//            //  这里有阻塞的情况，替换成 pthread_cond_timedwait 方法，设置相对的超时时间为 1s
//            //pthread_cond_wait(&capture_sync, &capture_mutex);
//            /*struct timespec tv;
//            clock_gettime(CLOCK_MONOTONIC, &tv);
//            tv.tv_sec += 1;
//            pthread_cond_timedwait(&capture_sync, &capture_mutex,&tv);*/
//            ts.tv_sec = 0;
//            ts.tv_nsec = 0;
//
//#if _POSIX_TIMERS > 0
//            clock_gettime(CLOCK_REALTIME, &ts);
//#else
//            gettimeofday(&tv, NULL);
//            ts.tv_sec = tv.tv_sec;
//            ts.tv_nsec = tv.tv_usec * 1000;
//#endif
//            ts.tv_sec += 1;
//            ts.tv_nsec += 0;
//            pthread_cond_timedwait(&capture_sync, &capture_mutex, &ts);
//        }
//        if (LIKELY(isRunning() && captureQueu)) {
//            frame = captureQueu;
//            captureQueu = NULL;
//        }
//    }
//    pthread_mutex_unlock(&capture_mutex);
//    return frame;
//}
//
///**
// * clear drame data for capturing
// */
//void UVCPreviewBase::clearCaptureFrame() {
//    pthread_mutex_lock(&capture_mutex);
//    {
//        if (captureQueu)
//            recycle_frame(captureQueu);
//        captureQueu = NULL;
//    }
//    pthread_mutex_unlock(&capture_mutex);
//}


///**
// * the actual function for capturing
// */
//void UVCPreviewBase::do_capture(JNIEnv *env) {
//    clearCaptureFrame();
//    callbackPixelFormatChanged();
//    for (; isRunning();) {
//        mIsCapturing = true;
//        if (mCaptureWindow) {
//            do_capture_surface(env);
//        } else {
//            do_capture_idle_loop(env);
//        }
//        pthread_cond_broadcast(&capture_sync);
//    }    // end of for (; isRunning() ;)
//}
//
//void UVCPreviewBase::do_capture_idle_loop(JNIEnv *env) {
//    for (; isRunning() && isCapturing();) {
//        do_capture_callback(env, waitCaptureFrame());
//    }
//}
//
///**
// * write frame data to Surface for capturing
// */
//void UVCPreviewBase::do_capture_surface(JNIEnv *env) {
//    uvc_frame_t *frame = NULL;
//    uvc_frame_t *converted = NULL;
//    char *local_picture_path;
//
//    for (; isRunning() && isCapturing();) {
//        frame = waitCaptureFrame();
//        if (LIKELY(frame)) {
//            // frame data is always YUYV format.
//            if LIKELY(isCapturing()) {
//                if (UNLIKELY(!converted)) {
//                    converted = get_frame(previewBytes);
//                }
//                if (LIKELY(converted)) {
//                    int b = uvc_any2rgbx(frame, converted);
//                    if (!b) {
//                        if (LIKELY(mCaptureWindow)) {
//                            copyToSurface(converted, &mCaptureWindow);
//                        }
//                    }
//                }
//            }
//            do_capture_callback(env, frame);
//        }
//    }
//    if (converted) {
//        recycle_frame(converted);
//    }
//    if (mCaptureWindow) {
//        ANativeWindow_release(mCaptureWindow);
//        mCaptureWindow = NULL;
//    }
//}
//
///**
//* call IFrameCallback#onFrame if needs
// */
//void UVCPreviewBase::do_capture_callback(JNIEnv *env, uvc_frame_t *frame) {
//    if (LIKELY(frame)) {
//        uvc_frame_t *callback_frame = frame;
//        if (mFrameCallbackObj) {
//            if (mFrameCallbackFunc) {
//                callback_frame = get_frame(callbackPixelBytes);
//                if (LIKELY(callback_frame)) {
//                    int b = mFrameCallbackFunc(frame, callback_frame);
//                    recycle_frame(frame);
//                    if (UNLIKELY(b)) {
//                        LOGW("failed to convert for callback frame");
//                        goto SKIP;
//                    }
//                } else {
//                    LOGW("failed to allocate for callback frame");
//                    callback_frame = frame;
//                    goto SKIP;
//                }
//            }
//            jobject buf = env->NewDirectByteBuffer(callback_frame->data, callbackPixelBytes);
//            if (iframecallback_fields.onFrame) {
//                env->CallVoidMethod(mFrameCallbackObj, iframecallback_fields.onFrame, buf);
//            }
//            env->ExceptionClear();
//            env->DeleteLocalRef(buf);
//        }
//        SKIP:
//        recycle_frame(callback_frame);
//    }
//
//}
