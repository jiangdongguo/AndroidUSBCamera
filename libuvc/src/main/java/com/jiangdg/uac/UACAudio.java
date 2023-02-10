package com.jiangdg.uac;

import android.text.TextUtils;
import android.util.Log;

import com.jiangdg.usb.USBMonitor;

/** usb audio engine
 *
 * @author Created by jiangdg on 2022/9/8
 */
public class UACAudio {
    private static final String TAG = "UACAudio";
    private static final String DEFAULT_USBFS = "/dev/bus/usb";
    /**
     * called by native for pcm data callback
     */
    public UACAudioCallBack mAudioCallBack;
    private AudioStatus mStatus = AudioStatus.RELEASED;
    /**
     * called by native as file handle
     */
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
            Log.w(TAG, "initAudio: err = " + e.getMessage());
        }
        mStatus = result<0 ? AudioStatus.ERROR : AudioStatus.CREATED;
        Log.i(TAG, "initAudio: " + result+ ", mNativePtr = " +mNativePtr);
    }

    public void startRecording() {
        if (mStatus == AudioStatus.RELEASED) {
            Log.e(TAG, "startRecording failed: init status error");
            return;
        }
        if (nativeStartRecord(mNativePtr) < 0) {
            Log.e(TAG, "startRecording: start failed");
            mStatus = AudioStatus.ERROR;
            return;
        }
        mStatus = AudioStatus.RUNNING;
        Log.i(TAG, "startRecording: success");
    }

    public void stopRecording() {
        if (mStatus != AudioStatus.RUNNING) {
            Log.e(TAG, "stopRecording: not in running");
            return;
        }
        if (nativeStopRecord(mNativePtr) < 0) {
            Log.e(TAG, "stopRecording: stop failed.");
            return;
        }
        mStatus = AudioStatus.STOPPED;
        Log.i(TAG, "stopRecording: success");
    }

    public void release() {
        nativeRelease(mNativePtr);
        mStatus = AudioStatus.RELEASED;
        Log.i(TAG, "release: success");
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
            Log.w(TAG, "failed to get USBFS path, try to use default path:" + name);
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
