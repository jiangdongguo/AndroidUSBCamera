package com.jiangdg.uac;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.jiangdg.usb.USBMonitor;

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
        if (thread.mCallBackList.contains(callBack)) {
            return;
        }
        thread.mCallBackList.add(callBack);
    }

    public void removeDataCallBack(UACAudioCallBack callBack) {
        final AudioThread thread = mThreadWf.get();
        if (callBack == null || isReleased()) {
            return;
        }
        if (! thread.mCallBackList.contains(callBack)) {
            return;
        }
        thread.mCallBackList.remove(callBack);
    }

    @Override
    public void handleMessage(Message msg) {
        final AudioThread thread = mThreadWf.get();
        if (thread == null) {
            Log.w(TAG, "handleMessage: err, thread is null");
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
        private final static long TIMES_OUT_MS = 1500;
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
            synchronized (mSync) {
                if (isRecording()) {
                    return;
                }
                mUACAudio = new UACAudio();
                mUACAudio.init(mCtrlBlock);
                mUACAudio.addAudioCallBack(this::callOnDataCallBack);
                mSync.notifyAll();
            }
            Log.i(TAG, "handleInitAudioRecord");
        }

        public void handleStartRecording() {
            if (mUACAudio == null) {
                Log.e(TAG, "handleStartRecording failed, you should call initAudioRecord first");
                return;
            }
            mUACAudio.startRecording();
            Log.i(TAG, "handleStartRecording");
        }

        public void handleStopRecording() {
            if (mUACAudio == null) {
                Log.e(TAG, "handleStopRecording failed, you should call initAudioRecord first");
                return;
            }
            mUACAudio.stopRecording();
            Log.i(TAG, "handleStopRecording");
        }

        public void handleReleaseAudioRecord() {
            handleStopRecording();
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
            Log.i(TAG, "handleReleaseAudioRecord");
        }

        public int getSampleRate() {
            synchronized (mSync) {
                if (mUACAudio == null) {
                    try {
                        mSync.wait(TIMES_OUT_MS);
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
            Log.i(TAG, "Audio thread start");
            Looper.prepare();
            UACAudioHandler uacHandler = null;
            try {
                Constructor<UACAudioHandler> constructor = mHandlerClass.getDeclaredConstructor(AudioThread.class);
                uacHandler = constructor.newInstance(this);
            } catch (Exception e) {
                Log.e(TAG, "UACAudioHandler new failed, " + e.getMessage());
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
            Log.i(TAG, "Audio thread stop");
        }

        public UACAudioHandler getHandler() {
            Log.i(TAG, "getHandler: ");
            synchronized (mSync) {
                if (mUACHandler == null) {
                    try {
                        mSync.wait(TIMES_OUT_MS);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "getHandler: failed, " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return mUACHandler;
        }

        private void callOnDataCallBack(byte[] pcm) {
            for (UACAudioCallBack callBack: mCallBackList) {
                try {
                    callBack.pcmData(pcm);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
