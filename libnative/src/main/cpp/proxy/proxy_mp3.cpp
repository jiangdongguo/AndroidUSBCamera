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
 * Proxy of mp3.
 *
 * @author Created by jiangdg on 2022/2/18
 */
#include "proxy_mp3.h"
#include "../module/mp3/mp3.h"
#include "../utils/logger.h"

void lameInit(JNIEnv *env, jobject instance, jint inSampleRate, jint outChannel, jint outSampleRate,
              jint outBitRate, jint quality) {
    lameInitInternal(inSampleRate, outChannel, outSampleRate, outBitRate, quality);
}

int lameEncode(JNIEnv *env, jobject instance, jshortArray leftBuf_, jshortArray rightBuf_,
                jint sampleRate, jbyteArray mp3Buf_) {
    jint ret = -1;
    if(leftBuf_ == nullptr || mp3Buf_ == nullptr){
        LOGI("data can't be null");
        return ret;
    }
    jshort *leftBuf;
    jshort *rightBuf = nullptr;
    leftBuf = env->GetShortArrayElements(leftBuf_, nullptr);
    if(rightBuf_ != nullptr){
        rightBuf = env->GetShortArrayElements(rightBuf_, nullptr);
    }
    jbyte *mp3Buf = env->GetByteArrayElements(mp3Buf_, nullptr);
    jsize readSizes = env->GetArrayLength(mp3Buf_);
    ret = lameEncodeInternal(leftBuf, rightBuf, sampleRate, reinterpret_cast<unsigned char *>(mp3Buf), readSizes);
    env->ReleaseShortArrayElements(leftBuf_, leftBuf, 0);
    if(rightBuf_ != nullptr){
        env->ReleaseShortArrayElements(rightBuf_, rightBuf, 0);
    }
    env->ReleaseByteArrayElements(mp3Buf_, mp3Buf, 0);
    return ret;
}

jint lameFlush(JNIEnv *env, jobject instance, jbyteArray mp3buf_) {
    jbyte *mp3buf = env->GetByteArrayElements(mp3buf_, nullptr);
    jsize len = env->GetArrayLength(mp3buf_);
    jint ret = lameFlushInternal(reinterpret_cast<unsigned char *>(mp3buf), len);
    env->ReleaseByteArrayElements(mp3buf_, mp3buf, 0);
    return ret;
}

void lameClose(JNIEnv *env, jobject instance) {
    lameCloseInternal();
}
