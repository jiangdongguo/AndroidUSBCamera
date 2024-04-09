/*
 * UVCCameraConfig.h
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2024 vshcryabets@gmail.com
 *
 * File name: UVCCamera.h
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
#pragma once

#include <stdint.h>

typedef struct control_value {
	int res;	// unused
	int min;
	int max;
	int def;
	int current;
} control_value_t;

typedef uvc_error_t (*paramget_func_i8)(uvc_device_handle_t *devh, int8_t *value, enum uvc_req_code req_code);
typedef uvc_error_t (*paramget_func_i16)(uvc_device_handle_t *devh, int16_t *value, enum uvc_req_code req_code);
typedef uvc_error_t (*paramget_func_i32)(uvc_device_handle_t *devh, int32_t *value, enum uvc_req_code req_code);
typedef uvc_error_t (*paramget_func_u8)(uvc_device_handle_t *devh, uint8_t *value, enum uvc_req_code req_code);
typedef uvc_error_t (*paramget_func_u16)(uvc_device_handle_t *devh, uint16_t *value, enum uvc_req_code req_code);
typedef uvc_error_t (*paramget_func_u32)(uvc_device_handle_t *devh, uint32_t *value, enum uvc_req_code req_code);
typedef uvc_error_t (*paramget_func_u8u8)(uvc_device_handle_t *devh, uint8_t *value1, uint8_t *value2, enum uvc_req_code req_code);
typedef uvc_error_t (*paramget_func_i8u8)(uvc_device_handle_t *devh, int8_t *value1, uint8_t *value2, enum uvc_req_code req_code);
typedef uvc_error_t (*paramget_func_i8u8u8)(uvc_device_handle_t *devh, int8_t *value1, uint8_t *value2, uint8_t *value3, enum uvc_req_code req_code);
typedef uvc_error_t (*paramget_func_i32i32)(uvc_device_handle_t *devh, int32_t *value1, int32_t *value2, enum uvc_req_code req_code);

typedef uvc_error_t (*paramset_func_i8)(uvc_device_handle_t *devh, int8_t value);
typedef uvc_error_t (*paramset_func_i16)(uvc_device_handle_t *devh, int16_t value);
typedef uvc_error_t (*paramset_func_i32)(uvc_device_handle_t *devh, int32_t value);
typedef uvc_error_t (*paramset_func_u8)(uvc_device_handle_t *devh, uint8_t value);
typedef uvc_error_t (*paramset_func_u16)(uvc_device_handle_t *devh, uint16_t value);
typedef uvc_error_t (*paramset_func_u32)(uvc_device_handle_t *devh, uint32_t value);
typedef uvc_error_t (*paramset_func_u8u8)(uvc_device_handle_t *devh, uint8_t value1, uint8_t value2);
typedef uvc_error_t (*paramset_func_i8u8)(uvc_device_handle_t *devh, int8_t value1, uint8_t value2);
typedef uvc_error_t (*paramset_func_i8u8u8)(uvc_device_handle_t *devh, int8_t value1, uint8_t value2, uint8_t value3);
typedef uvc_error_t (*paramset_func_i32i32)(uvc_device_handle_t *devh, int32_t value1, int32_t value2);


class UVCCameraAdjustments {
private:
    uvc_device_handle_t *mDeviceHandle;

    control_value_t mScanningMode;
    control_value_t mExposureMode;
    control_value_t mExposurePriority;
    control_value_t mExposureAbs;
    control_value_t mAutoFocus;
    control_value_t mAutoWhiteBlance;
    control_value_t mAutoWhiteBlanceCompo;
    control_value_t mWhiteBlance;
    control_value_t mWhiteBlanceCompo;
    control_value_t mBacklightComp;
    control_value_t mBrightness;
    control_value_t mContrast;
    control_value_t mAutoContrast;
    control_value_t mSharpness;
    control_value_t mGain;
    control_value_t mGamma;
    control_value_t mSaturation;
    control_value_t mHue;
    control_value_t mAutoHue;
    control_value_t mZoom;
    control_value_t mZoomRel;
    control_value_t mFocus;
    control_value_t mFocusRel;
    control_value_t mFocusSimple;
    control_value_t mIris;
    control_value_t mIrisRel;
    control_value_t mPan;
    control_value_t mTilt;
    control_value_t mRoll;
    control_value_t mPanRel;
    control_value_t mTiltRel;
    control_value_t mRollRel;
    control_value_t mPrivacy;
    control_value_t mPowerlineFrequency;
    control_value_t mMultiplier;
    control_value_t mMultiplierLimit;
    control_value_t mAnalogVideoStandard;
    control_value_t mAnalogVideoLockState;
private:
    int internalSetCtrlValue(int32_t value, paramset_func_u16 set_func);
    int internalSetCtrlValue(control_value_t &values, int8_t value,
                             paramget_func_i8 get_func, paramset_func_i8 set_func);
    int internalSetCtrlValue(control_value_t &values, uint8_t value,
                             paramget_func_u8 get_func, paramset_func_u8 set_func);
    int internalSetCtrlValue(control_value_t &values, uint8_t value1, uint8_t value2,
                             paramget_func_u8u8 get_func, paramset_func_u8u8 set_func);
    int internalSetCtrlValue(control_value_t &values, int8_t value1, uint8_t value2,
                             paramget_func_i8u8 get_func, paramset_func_i8u8 set_func);
    int internalSetCtrlValue(control_value_t &values, int8_t value1, uint8_t value2, uint8_t value3,
                             paramget_func_i8u8u8 get_func, paramset_func_i8u8u8 set_func);
    int internalSetCtrlValue(control_value_t &values, int16_t value,
                             paramget_func_i16 get_func, paramset_func_i16 set_func);
    int internalSetCtrlValue(control_value_t &values, uint16_t value,
                             paramget_func_u16 get_func, paramset_func_u16 set_func);
    int internalSetCtrlValue(control_value_t &values, int32_t value,
                             paramget_func_i32 get_func, paramset_func_i32 set_func);
    int internalSetCtrlValue(control_value_t &values, uint32_t value,
                             paramget_func_u32 get_func, paramset_func_u32 set_func);
public:
    uint64_t mCtrlSupports = 0;
    uint64_t mPUSupports = 0;

    UVCCameraAdjustments(uvc_device_handle_t *deviceHandle);

    void clearCameraParams();
    int updatePanLimit(int &min, int &max, int &def);
    int setPan(int pan);
    int getPan();

    int updateTiltLimit(int &min, int &max, int &def);
    int setTilt(int tilt);
    int getTilt();

    int updateRollLimit(int &min, int &max, int &def);
    int setRoll(int roll);
    int getRoll();

    int updateScanningModeLimit(int &min, int &max, int &def);
    int setScanningMode(int mode);
    int getScanningMode();

    int updateExposureModeLimit(int &min, int &max, int &def);
    int setExposureMode(int mode);
    int getExposureMode();

    int updateExposurePriorityLimit(int &min, int &max, int &def);
    int setExposurePriority(int priority);
    int getExposurePriority();

    int updateExposureLimit(int &min, int &max, int &def);
    int setExposure(int ae_abs);
    int getExposure();

    int updateExposureRelLimit(int &min, int &max, int &def);
    int setExposureRel(int ae_rel);
    int getExposureRel();

    int updateAutoFocusLimit(int &min, int &max, int &def);
    int setAutoFocus(bool autoFocus);
    bool getAutoFocus();

    int updateFocusLimit(int &min, int &max, int &def);
    int setFocus(int focus);
    int getFocus();

    int updateFocusRelLimit(int &min, int &max, int &def);
    int setFocusRel(int focus);
    int getFocusRel();

    int updateIrisLimit(int &min, int &max, int &def);
    int setIris(int iris);
    int getIris();

    int updateIrisRelLimit(int &min, int &max, int &def);
    int setIrisRel(int iris);
    int getIrisRel();

    int updatePanRelLimit(int &min, int &max, int &def);
    int setPanRel(int pan_rel);
    int getPanRel();

    int updateTiltRelLimit(int &min, int &max, int &def);
    int setTiltRel(int tilt_rel);
    int getTiltRel();

    int updateRollRelLimit(int &min, int &max, int &def);
    int setRollRel(int roll_rel);
    int getRollRel();

    int updatePrivacyLimit(int &min, int &max, int &def);
    int setPrivacy(int privacy);
    int getPrivacy();

    int updateAutoWhiteBlanceLimit(int &min, int &max, int &def);
    int setAutoWhiteBlance(bool autoWhiteBlance);
    bool getAutoWhiteBlance();

    int updateAutoWhiteBlanceCompoLimit(int &min, int &max, int &def);
    int setAutoWhiteBlanceCompo(bool autoWhiteBlanceCompo);
    bool getAutoWhiteBlanceCompo();

    int updateWhiteBlanceLimit(int &min, int &max, int &def);
    int setWhiteBlance(int temp);
    int getWhiteBlance();

    int updateWhiteBlanceCompoLimit(int &min, int &max, int &def);
    int setWhiteBlanceCompo(int white_blance_compo);
    int getWhiteBlanceCompo();

    int updateBacklightCompLimit(int &min, int &max, int &def);
    int setBacklightComp(int backlight);
    int getBacklightComp();

    int updateBrightnessLimit(int &min, int &max, int &def);
    int setBrightness(int brightness);
    int getBrightness();

    int updateContrastLimit(int &min, int &max, int &def);
    int setContrast(uint16_t contrast);
    int getContrast();

    int updateAutoContrastLimit(int &min, int &max, int &def);
    int setAutoContrast(bool autoFocus);
    bool getAutoContrast();

    int updateSharpnessLimit(int &min, int &max, int &def);
    int setSharpness(int sharpness);
    int getSharpness();

    int updateGainLimit(int &min, int &max, int &def);
    int setGain(int gain);
    int getGain();

    int updateGammaLimit(int &min, int &max, int &def);
    int setGamma(int gamma);
    int getGamma();

    int updateSaturationLimit(int &min, int &max, int &def);
    int setSaturation(int saturation);
    int getSaturation();

    int updateHueLimit(int &min, int &max, int &def);
    int setHue(int hue);
    int getHue();

    int updateAutoHueLimit(int &min, int &max, int &def);
    int setAutoHue(bool autoFocus);
    bool getAutoHue();

    int updatePowerlineFrequencyLimit(int &min, int &max, int &def);
    int setPowerlineFrequency(int frequency);
    int getPowerlineFrequency();

    int sendCommand(int command);

    int updateZoomLimit(int &min, int &max, int &def);
    int setZoom(int zoom);
    int getZoom();

    int updateZoomRelLimit(int &min, int &max, int &def);
    int setZoomRel(int zoom);
    int getZoomRel();

    int updateDigitalMultiplierLimit(int &min, int &max, int &def);
    int setDigitalMultiplier(int multiplier);
    int getDigitalMultiplier();

    int updateDigitalMultiplierLimitLimit(int &min, int &max, int &def);
    int setDigitalMultiplierLimit(int multiplier_limit);
    int getDigitalMultiplierLimit();

    int updateAnalogVideoStandardLimit(int &min, int &max, int &def);
    int setAnalogVideoStandard(int standard);
    int getAnalogVideoStandard();

    int updateAnalogVideoLockStateLimit(int &min, int &max, int &def);
    int setAnalogVideoLockState(int status);
    int getAnalogVideoLockState();

    /*	int updateFocusSimpleLimit(int &min, int &max, int &def);
	int setFocusSimple(int focus);
	int getFocusSimple(); */
};
