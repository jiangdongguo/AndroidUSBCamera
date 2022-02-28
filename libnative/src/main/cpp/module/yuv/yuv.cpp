/**
 *  yuv handle
 *
 * @author Created by jiangdg on 2022/2/18
 */

#include "yuv.h"

void *nv21ToYuv420spInternal(char* srcData, char* destData, int width, int height) {
    int yLength = width * height;
    int uLength = yLength / 4;
    memcpy(destData,srcData,yLength);
    for(int i=0; i<yLength/4; i++) {
        destData[yLength + 2 * i] = srcData[yLength + 2*i+1];
        destData[yLength + 2*i+1] = srcData[yLength + 2*i];
    }
    return nullptr;
}

void *nv21ToYuv420spWithMirrorInternal(char* srcData, char* destData, int width, int height) {
    return nullptr;
}

void *nv21ToYuv420pInternal(char* srcData, char* destData, int width, int height) {
    int yLength = width * height;
    int uLength = yLength / 4;
    memcpy(destData,srcData,yLength);
    for(int i=0; i<yLength/4; i++) {
        destData[yLength + i] = srcData[yLength + 2*i + 1];
        destData[yLength + uLength + i] = srcData[yLength + 2*i];
    }
    return nullptr;
}

void *nv21ToYuv420pWithMirrorInternal(char* srcData, char* destData, int width, int height) {
    return nullptr;
}
