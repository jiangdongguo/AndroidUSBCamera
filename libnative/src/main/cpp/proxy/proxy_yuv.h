/*
 * Copyright 2017-2022 Jiangdg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Proxy of yuv.
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

void yuv420spToNv21(JNIEnv *env, jobject instance, jbyteArray data, jint width, jint height);
void nv21ToYuv420sp(JNIEnv *env, jobject instance, jbyteArray data, jint width, jint height);
void nv21ToYuv420spWithMirror(JNIEnv *env, jobject instance, jbyteArray data, jint width, jint height);
void nv21ToYuv420p(JNIEnv *env, jobject instance, jbyteArray data, jint width, jint height);
void nv21ToYuv420pWithMirror(JNIEnv *env, jobject instance, jbyteArray data, jint width, jint height);
void nativeRotateNV21(JNIEnv *env, jobject instance, jbyteArray data, jint width, jint height, jint degree);

#ifdef __cplusplus
};
#endif
#endif //ANDROIDUSBCAMERA_PROXY_YUV_H
