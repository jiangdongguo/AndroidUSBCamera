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
 */
package com.jiangdg.uac;

import android.text.TextUtils;
import com.jiangdg.usb.USBMonitor;
import com.jiangdg.utils.XLogWrapper;

/** usb audio engine
 *
 * @author Created by jiangdg on 2022/9/8
 */
public class UACAudio {
    private static final String TAG = "UACAudio";
    private static final String DEFAULT_USBFS = "/dev/bus/usb";
    // called by native
    public UACAudioCallBack mAudioCallBack;
    private AudioStatus mStatus;
    // called by native
    private long mNativePtr;

    static {
        System.loadLibrary("UACAudio");
    }

    public void init(USBMonitor.UsbControlBlock ctrlBlock) {
        int result = -1;
        try {
            result = nativeInit(ctrlBlock.getVenderId(), ctrlBlock.getProductId(),
                    ctrlBlock.getBusNum(),
                    ctrlBlock.getDevNum(),
                    ctrlBlock.getFileDescriptor(),
                    getUSBFSName(ctrlBlock));
        } catch (final Exception e) {
            XLogWrapper.w(TAG, "initAudio: err = " + e.getMessage());
        }
        mStatus = result<0 ? AudioStatus.ERROR : AudioStatus.CREATED;
        XLogWrapper.i(TAG, "initAudio: " + result+ ", mNativePtr = " +mNativePtr);
    }

    public void startRecording() {
        if (mStatus != AudioStatus.CREATED) {
            XLogWrapper.e(TAG, "startRecording failed: init status error");
            return;
        }
        if (nativeStartRecord(mNativePtr) < 0) {
            XLogWrapper.e(TAG, "startRecording: start failed");
            mStatus = AudioStatus.ERROR;
            return;
        }
        mStatus = AudioStatus.RUNNING;
        XLogWrapper.i(TAG, "startRecording: success");
    }

    public void stopRecording() {
        if (mStatus != AudioStatus.RUNNING) {
            XLogWrapper.e(TAG, "stopRecording: not in running");
            return;
        }
        if (nativeStopRecord(mNativePtr) < 0) {
            XLogWrapper.e(TAG, "stopRecording: stop failed.");
            return;
        }
        mStatus = AudioStatus.STOPPED;
        XLogWrapper.i(TAG, "stopRecording: success");
    }

    public void release() {
        nativeRelease(mNativePtr);
        mStatus = AudioStatus.RELEASED;
        XLogWrapper.i(TAG, "release: success");
    }

    public int getSampleRate() {
        return nativeGetSampleRate(mNativePtr);
    }

    public int getChannelCount() {
        return nativeGetChannelCount(mNativePtr);
    }

    public int getBitResolution() {
        return nativeGetBitResolution(mNativePtr);
    }

    public AudioStatus getAudioStatus() {
        return mStatus;
    }

    public boolean isRecording() {
        return nativeGetRecordingState(mNativePtr);
    }

    public void addAudioCallBack(UACAudioCallBack callBack) {
        this.mAudioCallBack = callBack;
    }

    /** Pcm data callback, called by native
     *
     * @param data pcm data
     */
    public void pcmData(byte[] data) {
        if (mAudioCallBack != null) {
            mAudioCallBack.pcmData(data);
        }
    }

    private String getUSBFSName(final USBMonitor.UsbControlBlock ctrlBlock) {
        String result = null;
        final String name = ctrlBlock.getDeviceName();
        final String[] v = !TextUtils.isEmpty(name) ? name.split("/") : null;
        if ((v != null) && (v.length > 2)) {
            final StringBuilder sb = new StringBuilder(v[0]);
            for (int i = 1; i < v.length - 2; i++)
                sb.append("/").append(v[i]);
            result = sb.toString();
        }
        if (TextUtils.isEmpty(result)) {
            XLogWrapper.w(TAG, "failed to get USBFS path, try to use default path:" + name);
            result = DEFAULT_USBFS;
        }
        return result;
    }

    private native int nativeInit(int vid, int pid, int busnum, int devaddr, int fd, String usbfs);

    private native void nativeRelease(long id);

    private native int nativeStartRecord(long id);

    private native int nativeStopRecord(long id);

    private native boolean nativeGetRecordingState(long id);

    private native int nativeGetSampleRate(long id);

    private native int nativeGetChannelCount(long id);

    private native int nativeGetBitResolution(long id);

    public enum AudioStatus {
        CREATED,
        RUNNING,
        STOPPED,
        RELEASED,
        ERROR
    }
}
