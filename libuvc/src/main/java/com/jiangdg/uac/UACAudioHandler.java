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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.jiangdg.usb.USBMonitor;
import com.jiangdg.utils.XLogWrapper;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/** UAC thread handler
 *
 * @author Created by jiangdg on 2022/9/9
 */
public class UACAudioHandler extends Handler {
    private static final String TAG = "UACAudioHandler";
    private static final int MSG_INIT = 0x00;
    private static final int MSG_START = 0x01;
    private static final int MSG_STOP= 0x02;
    private static final int MSG_RELEASE = 0x03;
    private volatile boolean mIsReleased;
    private final WeakReference<AudioThread> mThreadWf;

    public static UACAudioHandler createHandler(USBMonitor.UsbControlBlock ctrlBlock) {
        AudioThread audioThread = new AudioThread(UACAudioHandler.class, ctrlBlock);
        audioThread.start();
        return audioThread.getHandler();
    }

    private UACAudioHandler(AudioThread thread) {
        this.mThreadWf = new WeakReference<>(thread);
    }

    public void initAudioRecord() {
        checkReleased();
        obtainMessage(MSG_INIT).sendToTarget();
    }

    public void startRecording() {
        checkReleased();
        obtainMessage(MSG_START).sendToTarget();
    }

    public void stopRecording() {
        checkReleased();
        obtainMessage(MSG_STOP).sendToTarget();
    }

    public void releaseAudioRecord() {
        checkReleased();
        obtainMessage(MSG_RELEASE).sendToTarget();
    }

    public boolean isRecording() {
        final AudioThread thread = mThreadWf.get();
        if (thread==null || isReleased()) {
            return false;
        }
        return thread.isRecording();
    }

    public UACAudio.AudioStatus getAudioStatus() {
        final AudioThread thread = mThreadWf.get();
        if (isReleased()) {
            return UACAudio.AudioStatus.ERROR;
        }
        return thread.getAudioStatus();
    }

    public int getSampleRate() {
        final AudioThread thread = mThreadWf.get();
        if (isReleased()) {
            return -1;
        }
        return thread.getSampleRate();
    }

    public int getBitResolution() {
        final AudioThread thread = mThreadWf.get();
        if (isReleased()) {
            return -1;
        }
        return thread.getBitResolution();
    }

    public int getChannelCount() {
        final AudioThread thread = mThreadWf.get();
        if (isReleased()) {
            return -1;
        }
        return thread.getChannelCount();
    }

    public void addDataCallBack(UACAudioCallBack callBack) {
        final AudioThread thread = mThreadWf.get();
        if (callBack == null || isReleased()) {
            return;
        }
        thread.mCallBackList.add(callBack);
    }

    public void removeDataCallBack(UACAudioCallBack callBack) {
        final AudioThread thread = mThreadWf.get();
        if (callBack == null || isReleased()) {
            return;
        }
        thread.mCallBackList.remove(callBack);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        final AudioThread thread = mThreadWf.get();
        if (thread == null) {
            XLogWrapper.w(TAG, "handleMessage: err, thread is null");
            return;
        }
        switch (msg.what) {
            case MSG_INIT:
                thread.handleInitAudioRecord();
                break;
            case MSG_START:
                thread.handleStartRecording();
                break;
            case MSG_STOP:
                thread.handleStopRecording();
                break;
            case MSG_RELEASE:
                thread.handleReleaseAudioRecord();
                break;
            default:
                break;
        }
    }

    public boolean isReleased() {
        return mIsReleased || mThreadWf.get()==null;
    }

    private void checkReleased() {
        if (isReleased()) {
            throw new IllegalStateException("uac handler already been released");
        }
    }

    /**
     * uac thread
     */
    public static  class AudioThread extends Thread {
        private final static String TAG = "AudioThread";
        private final static long TIMES_OUT_MS = 3000;
        private final Object mSync = new Object();
        private final USBMonitor.UsbControlBlock mCtrlBlock;
        private final Class<UACAudioHandler> mHandlerClass;
        private UACAudioHandler mUACHandler;
        private UACAudio mUACAudio;

        private final Set<UACAudioCallBack> mCallBackList = new CopyOnWriteArraySet<>();

        public AudioThread(Class<UACAudioHandler> handlerClass, USBMonitor.UsbControlBlock ctrlBlock) {
            this.mCtrlBlock = ctrlBlock;
            this.mHandlerClass = handlerClass;
        }

        public void handleInitAudioRecord() {
            XLogWrapper.i(TAG, "handleInitAudioRecord:");
            mUACAudio = new UACAudio();
            mUACAudio.init(mCtrlBlock);
            mUACAudio.addAudioCallBack(this::callOnDataCallBack);
            synchronized (mSync) {
                mSync.notifyAll();
            }
            XLogWrapper.i(TAG, "handleInitAudioRecord: Finished");
        }

        public void handleStartRecording() {
            if (mUACAudio == null) {
                XLogWrapper.e(TAG, "handleStartRecording failed, you should call initAudioRecord first");
                return;
            }
            mUACAudio.startRecording();
            XLogWrapper.i(TAG, "handleStartRecording");
        }

        public void handleStopRecording() {
            if (mUACAudio == null) {
                XLogWrapper.e(TAG, "handleStopRecording failed, you should call initAudioRecord first");
                return;
            }
            mUACAudio.stopRecording();
            XLogWrapper.i(TAG, "handleStopRecording");
        }

        public void handleReleaseAudioRecord() {
            if (mUACAudio != null) {
                mUACAudio.release();
                mUACAudio = null;
            }
            mCallBackList.clear();
            if (mUACHandler != null) {
                mUACHandler.mIsReleased = true;
            }
            Looper looper = Looper.myLooper();
            if (looper != null) {
                looper.quit();
            }
            XLogWrapper.i(TAG, "handleReleaseAudioRecord");
        }

        public int getSampleRate() {
            synchronized (mSync) {
                if (mUACAudio == null) {
                    try {
                        mSync.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return mUACAudio != null ? mUACAudio.getSampleRate() : -1;
                }
                return mUACAudio.getSampleRate();
            }
        }

        public int getChannelCount() {
            synchronized (mSync) {
                if (mUACAudio == null) {
                    try {
                        mSync.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return mUACAudio != null ? mUACAudio.getChannelCount() : -1;
                }
                return mUACAudio.getChannelCount();
            }
        }

        public int getBitResolution() {
            synchronized (mSync) {
                if (mUACAudio == null) {
                    try {
                        mSync.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return mUACAudio != null ? mUACAudio.getBitResolution() : -1;
                }
                return mUACAudio.getBitResolution();
            }
        }

        public UACAudio.AudioStatus getAudioStatus() {
            synchronized (mSync) {
                if (mUACAudio == null) {
                    try {
                        mSync.wait(TIMES_OUT_MS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return mUACAudio != null ? mUACAudio.getAudioStatus() : UACAudio.AudioStatus.ERROR;
                }
                return mUACAudio.getAudioStatus();
            }
        }

        public boolean isRecording() {
            synchronized (mSync) {
                if (mUACAudio == null) {
                    try {
                        mSync.wait(TIMES_OUT_MS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return mUACAudio != null && mUACAudio.isRecording();
                }
                return mUACAudio.isRecording();
            }
        }

        @Override
        public void run() {
            XLogWrapper.i(TAG, "Audio thread start");
            Looper.prepare();
            UACAudioHandler uacHandler = null;
            try {
                Constructor<UACAudioHandler> constructor = mHandlerClass.getDeclaredConstructor(AudioThread.class);
                uacHandler = constructor.newInstance(this);
            } catch (Exception e) {
                XLogWrapper.e(TAG, "UACAudioHandler new failed, " + e.getMessage());
            }
            if (uacHandler != null) {
                synchronized (mSync) {
                    mUACHandler = uacHandler;
                    mSync.notifyAll();
                }
                Looper.loop();
                if (mUACHandler != null) {
                    mUACHandler.mIsReleased = true;
                }
            }
            mCallBackList.clear();
            synchronized (mSync) {
                mUACHandler = null;
                mSync.notifyAll();
            }
            XLogWrapper.i(TAG, "Audio thread stop");
        }

        public UACAudioHandler getHandler() {
            synchronized (mSync) {
                if (mUACHandler == null) {
                    try {
                        mSync.wait(TIMES_OUT_MS);
                    } catch (InterruptedException e) {
                        XLogWrapper.e(TAG, "getHandler: failed, " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            XLogWrapper.i(TAG, "getHandler: " + mUACHandler);
            return mUACHandler;
        }

        private void callOnDataCallBack(byte[] pcm) {
            for (UACAudioCallBack callBack: mCallBackList) {
                try {
                    callBack.pcmData(pcm);
                } catch (Exception e) {
                    mCallBackList.remove(callBack);
                }
            }
        }
    }
}
