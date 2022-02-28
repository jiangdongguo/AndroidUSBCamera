/**
 *  Proxy of yuv
 *
 * @author Created by jiangdg on 2022/2/18
 */
#ifndef ANDROIDUSBCAMERA_PROXY_YUV_H
#define ANDROIDUSBCAMERA_PROXY_YUV_H
#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>
#include <cstdlib>
#include "../module/yuv/yuv.h"
#include "../utils/logger.h"

void nv21ToYuv420sp(JNIEnv *env, jobject instance, jbyteArray data, jint width, jint height);
void nv21ToYuv420spWithMirror(JNIEnv *env, jobject instance, jbyteArray data, jint width, jint height);
void nv21ToYuv420p(JNIEnv *env, jobject instance, jbyteArray data, jint width, jint height);
void nv21ToYuv420pWithMirror(JNIEnv *env, jobject instance, jbyteArray data, jint width, jint height);

#ifdef __cplusplus
};
#endif
#endif //ANDROIDUSBCAMERA_PROXY_YUV_H
