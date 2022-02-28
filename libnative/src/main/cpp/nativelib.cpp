#include <jni.h>
#include <string>
#include "utils/logger.h"
#include "proxy/proxy_yuv.h"

#define NUM_METHODS(x) ((int)(sizeof(x)/ sizeof(x[0])))
JavaVM *globalJvm;
const char * yuvClsPath = "com/jiangdg/natives/YUVUtils";

static JNINativeMethod g_yuv_methods[] = {
        {"nv21ToYuv420sp", "([BII)V", (void *)nv21ToYuv420sp},
        {"nv21ToYuv420spWithMirror", "([BII)V", (void *)nv21ToYuv420spWithMirror},
        {"nv21ToYuv420p", "([BII)V", (void *)nv21ToYuv420p},
        {"nv21ToYuv420pWithMirror", "([BII)V", (void *)nv21ToYuv420pWithMirror},
};

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* jvm, void* reserved) {
    globalJvm = jvm;

    // 获取jvm中的JNIEnv实例对象
    JNIEnv *env;
    if(JNI_OK != jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_4)) {
        LOGE("Get JNIEnv failed");
        return JNI_ERR;
    }
    // 注册所有native类的方法
    jclass yuvLcs = env->FindClass(yuvClsPath);
    int ret = env->RegisterNatives(yuvLcs, g_yuv_methods, NUM_METHODS(g_yuv_methods));
    if( ret < 0) {
        LOG_E("Register src.yuv natives failed, ret = %d", ret);
        return JNI_ERR;
    }
    LOGI("JNI_OnLoad success!");
    return JNI_VERSION_1_4;
}

extern "C"
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* jvm, void* reserved) {
    if(jvm) {
        jvm->DestroyJavaVM();
    }
    globalJvm = nullptr;
    LOGI("JNI_OnUnload success!");
}