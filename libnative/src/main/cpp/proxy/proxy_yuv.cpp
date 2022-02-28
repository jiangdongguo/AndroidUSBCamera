/**
 *  Proxy of yuv
 *
 * @author Created by jiangdg on 2022/2/18
 */

#include "proxy_yuv.h"

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
