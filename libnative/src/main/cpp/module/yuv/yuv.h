/**
 * yuv handle header
 *
 * @author Created by jiangdg on 2022/2/18
 */

#ifndef ANDROIDUSBCAMERA_YUV_H
#define ANDROIDUSBCAMERA_YUV_H

#ifdef __cplusplus
extern "C" {
#endif
#include <cstring>

void *yuv420spToNv21Internal(char* srcData, char* destData, int width, int height);
void *nv21ToYuv420spInternal(char* srcData, char* destData, int width, int height);
void *nv21ToYuv420spWithMirrorInternal(char* srcData, char* destData, int width, int height);
void *nv21ToYuv420pInternal(char* srcData, char* destData, int width, int height);
void *nv21ToYuv420pWithMirrorInternal(char* srcData, char* destData, int width, int height);

#ifdef __cplusplus
};
#endif
#endif //ANDROIDUSBCAMERA_YUV_H
