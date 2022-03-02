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
#include "proxy_yuv.h"

void yuv420spToNv21(JNIEnv *env, jobject instance, jbyteArray data, jint width, jint height) {
    if(! data || width == 0 || height == 0) {
        LOGE("Parameters error in nv21ToYuv420sp");
        return;
    }
    jbyte *srcData = env->GetByteArrayElements(data, JNI_FALSE);
    jsize srcLen = env->GetArrayLength(data);
    char *dest = (char *)malloc(srcLen);
    yuv420spToNv21Internal((char *)srcData,dest, width, height);
    env->SetByteArrayRegion(data,0,srcLen,(jbyte *)dest);
    env->ReleaseByteArrayElements(data, srcData, 0);
    free(dest);
}

void nv21ToYuv420sp(JNIEnv *env, jobject instance, jbyteArray data, jint width, jint height) {
    if(! data || width == 0 || height == 0) {
        LOGE("Parameters error in nv21ToYuv420sp");
        return;
    }
    jbyte *srcData = env->GetByteArrayElements(data, JNI_FALSE);
    jsize srcLen = env->GetArrayLength(data);
    char *dest = (char *)malloc(srcLen);
    nv21ToYuv420spInternal((char *)srcData,dest, width, height);
    env->SetByteArrayRegion(data,0,srcLen,(jbyte *)dest);
    env->ReleaseByteArrayElements(data, srcData, 0);
    free(dest);
}

void nv21ToYuv420spWithMirror(JNIEnv *env, jobject instance, jbyteArray data, jint width, jint height) {
    if(! data || width == 0 || height == 0) {
        LOGE("Parameters error in nv21ToYuv420spWithMirror");
        return;
    }
    jbyte *srcData = env->GetByteArrayElements(data, JNI_FALSE);
    jsize srcLen = env->GetArrayLength(data);
    char *dest = (char *)malloc(srcLen);
    nv21ToYuv420spWithMirrorInternal((char *)srcData,dest, width, height);
    env->SetByteArrayRegion(data,0,srcLen,(jbyte *)dest);
    env->ReleaseByteArrayElements(data, srcData, 0);
    free(dest);
}

void nv21ToYuv420p(JNIEnv *env, jobject instance, jbyteArray data, jint width, jint height) {
    if(! data || width == 0 || height == 0) {
        LOGE("Parameters error in nv21ToYuv420p");
        return;
    }
    jbyte *srcData = env->GetByteArrayElements(data, JNI_FALSE);
    jsize srcLen = env->GetArrayLength(data);
    char *dest = (char *)malloc(srcLen);
    nv21ToYuv420pInternal((char *)srcData,dest, width, height);
    env->SetByteArrayRegion(data,0,srcLen,(jbyte *)dest);
    env->ReleaseByteArrayElements(data, srcData, 0);
    free(dest);
}

void nv21ToYuv420pWithMirror(JNIEnv *env, jobject instance, jbyteArray data, jint width, jint height) {
    if(! data || width == 0 || height == 0) {
        LOGE("Parameters error in nv21ToYuv420pWithMirror");
        return;
    }
    jbyte *srcData = env->GetByteArrayElements(data, JNI_FALSE);
    jsize srcLen = env->GetArrayLength(data);
    char *dest = (char *)malloc(srcLen);
    nv21ToYuv420pWithMirrorInternal((char *)srcData,dest, width, height);
    env->SetByteArrayRegion(data,0,srcLen,(jbyte *)dest);
    env->ReleaseByteArrayElements(data, srcData, 0);
    free(dest);
}
