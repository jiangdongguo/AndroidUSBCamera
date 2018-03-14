package com.serenegiant.usb.encoder.biz;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

/**
 * 对YUV视频流进行编码
 * Created by jiangdongguo on 2017/5/6.
 */

@SuppressWarnings("deprecation")
public class H264EncodeConsumer extends Thread {
    private static final boolean DEBUG = false;
    private static final String TAG = "H264EncodeConsumer";
    private static final String MIME_TYPE = "video/avc";
    // 间隔1s插入一帧关键帧
    private static final int FRAME_INTERVAL = 1;
    // 绑定编码器缓存区超时时间为10s
    private static final int TIMES_OUT = 10000;
    // 硬编码器
    private MediaCodec mMediaCodec;
    private int mColorFormat;
    private boolean isExit = false;
    private boolean isEncoderStart = false;

    private MediaFormat mFormat;
    private static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test2.h264";
    private BufferedOutputStream outputStream;
    final int millisPerframe = 1000 / 20;
    long lastPush = 0;
    private OnH264EncodeResultListener listener;
    private int mWidth;
    private int mHeight;
    private MediaFormat newFormat;
    private WeakReference<Mp4MediaMuxer> mMuxerRef;
    private boolean isAddKeyFrame = false;

    public interface OnH264EncodeResultListener {
        void onEncodeResult(byte[] data, int offset,
                            int length, long timestamp);
    }

    public void setOnH264EncodeResultListener(OnH264EncodeResultListener listener) {
        this.listener = listener;
    }

    public H264EncodeConsumer(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    public synchronized void setTmpuMuxer(Mp4MediaMuxer mMuxer) {
        this.mMuxerRef = new WeakReference<>(mMuxer);
        Mp4MediaMuxer muxer = mMuxerRef.get();
        if (muxer != null && newFormat != null) {
            muxer.addTrack(newFormat, true);
        }
    }

    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;

    public void setRawYuv(byte[] yuvData, int width, int height) {
        if (!isEncoderStart)
            return;
        if (mWidth != width || mHeight != height) {
            mWidth = width;
            mHeight = height;
            return;
        }
        try {
            if (lastPush == 0) {
                lastPush = System.currentTimeMillis();
            }
            long time = System.currentTimeMillis() - lastPush;
            if (time >= 0) {
                time = millisPerframe - time;
                if (time > 0)
                    Thread.sleep(time / 2);
            }
            // 将数据写入编码器

            feedMediaCodecData(nv12ToNV21(yuvData, mWidth, mHeight));

            if (time > 0)
                Thread.sleep(time / 2);
            lastPush = System.currentTimeMillis();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private void feedMediaCodecData(byte[] data) {
        if (!isEncoderStart)
            return;
        int bufferIndex = -1;
        try {
            bufferIndex = mMediaCodec.dequeueInputBuffer(0);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        if (bufferIndex >= 0) {
            ByteBuffer buffer;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                buffer = mMediaCodec.getInputBuffer(bufferIndex);
            } else {
                buffer = inputBuffers[bufferIndex];
            }
            buffer.clear();
            buffer.put(data);
            buffer.clear();
            mMediaCodec.queueInputBuffer(bufferIndex, 0, data.length, System.nanoTime() / 1000, MediaCodec.BUFFER_FLAG_KEY_FRAME);
        }
    }

    public void exit() {
        isExit = true;
    }

    @SuppressLint("WrongConstant")
    @Override
    public void run() {
        if (!isEncoderStart) {
            startMediaCodec();
        }
        // 休眠200ms，等待音频线程开启
        // 否则视频第一秒会卡住
        try {
            Thread.sleep(200);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        // 如果编码器没有启动或者没有图像数据，线程阻塞等待
        while (!isExit) {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = 0;
            byte[] mPpsSps = new byte[0];
            byte[] h264 = new byte[mWidth * mHeight];
            do {
                outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
                if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    outputBuffers = mMediaCodec.getOutputBuffers();
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    synchronized (H264EncodeConsumer.this) {
                        newFormat = mMediaCodec.getOutputFormat();
                        if (mMuxerRef != null) {
                            Mp4MediaMuxer muxer = mMuxerRef.get();
                            if (muxer != null) {
                                muxer.addTrack(newFormat, true);
                            }
                        }
                    }
                } else if (outputBufferIndex < 0) {
                    // let's ignore it
                } else {
                    ByteBuffer outputBuffer;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
                    } else {
                        outputBuffer = outputBuffers[outputBufferIndex];
                    }
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                    boolean sync = false;
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {// sps
                        sync = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0;
                        if (!sync) {
                            byte[] temp = new byte[bufferInfo.size];
                            outputBuffer.get(temp);
                            mPpsSps = temp;
                            mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                            continue;
                        } else {
                            mPpsSps = new byte[0];
                        }
                    }
                    sync |= (bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0;
                    int len = mPpsSps.length + bufferInfo.size;
                    if (len > h264.length) {
                        h264 = new byte[len];
                    }
                    if (sync) {
                        System.arraycopy(mPpsSps, 0, h264, 0, mPpsSps.length);
                        outputBuffer.get(h264, mPpsSps.length, bufferInfo.size);
                        if (listener != null) {
                            listener.onEncodeResult(h264, 0, mPpsSps.length + bufferInfo.size, bufferInfo.presentationTimeUs / 1000);
                        }

                        // 添加视频流到混合器
                        if (mMuxerRef != null) {
                            Mp4MediaMuxer muxer = mMuxerRef.get();
                            if (muxer != null) {
                                muxer.pumpStream(outputBuffer, bufferInfo, true);
                            }
                            isAddKeyFrame = true;
                        }
                        if (DEBUG)
                            Log.i(TAG, "关键帧 h264.length = " + h264.length + ";mPpsSps.length=" + mPpsSps.length
                                    + "  bufferInfo.size = " + bufferInfo.size);
                    } else {
                        outputBuffer.get(h264, 0, bufferInfo.size);
                        if (listener != null) {
                            listener.onEncodeResult(h264, 0, bufferInfo.size, bufferInfo.presentationTimeUs / 1000);
                        }
                        // 添加视频流到混合器
                        if (isAddKeyFrame && mMuxerRef != null) {
                            Mp4MediaMuxer muxer = mMuxerRef.get();
                            if (muxer != null) {
                                muxer.pumpStream(outputBuffer, bufferInfo, true);
                            }
                        }
                        if (DEBUG)
                            Log.i(TAG, "普通帧 h264.length = " + h264.length + "  bufferInfo.size = " + bufferInfo.size);
                    }
                    mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                }
            } while (!isExit && isEncoderStart);
        }
        stopMediaCodec();
    }

    private void startMediaCodec() {
        final MediaCodecInfo videoCodecInfo = selectVideoCodec(MIME_TYPE);
        if (videoCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }

        final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);
        format.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate());
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        try {
            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();


        isEncoderStart = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP + 1) {
            inputBuffers = outputBuffers = null;
        } else {
            inputBuffers = mMediaCodec.getInputBuffers();
            outputBuffers = mMediaCodec.getOutputBuffers();
        }

        Bundle params = new Bundle();
        params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mMediaCodec.setParameters(params);
        }
    }

    private void stopMediaCodec() {
        isEncoderStart = false;
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            Log.d(TAG, "关闭视频编码器");
        }
    }

    private static final int FRAME_RATE = 15;
    private static final float BPP = 0.50f;

    private int calcBitRate() {
        final int bitrate = (int) (BPP * FRAME_RATE * mWidth * mHeight);
        Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
        return bitrate;
    }

    /**
     * select the first codec that match a specific MIME type
     *
     * @param mimeType
     * @return null if no codec matched
     */
    @SuppressWarnings("deprecation")
    protected final MediaCodecInfo selectVideoCodec(final String mimeType) {

        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {    // skipp decoder
                continue;
            }
            // select first codec that match a specific MIME type and color format
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    final int format = selectColorFormat(codecInfo, mimeType);
                    if (format > 0) {
                        mColorFormat = format;
                        return codecInfo;
                    }
                }
            }
        }
        return null;
    }

    /**
     * select color format available on specific codec and we can use.
     *
     * @return 0 if no colorFormat is matched
     */
    protected static final int selectColorFormat(final MediaCodecInfo codecInfo, final String mimeType) {
        int result = 0;
        final MediaCodecInfo.CodecCapabilities caps;
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            caps = codecInfo.getCapabilitiesForType(mimeType);
        } finally {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }
        int colorFormat;
        for (int i = 0; i < caps.colorFormats.length; i++) {
            colorFormat = caps.colorFormats[i];
            if (isRecognizedViewoFormat(colorFormat)) {
                if (result == 0)
                    result = colorFormat;
                break;
            }
        }
        if (result == 0)
            Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return result;
    }

    /**
     * color formats that we can use in this class
     */
    protected static int[] recognizedFormats;

    static {
        recognizedFormats = new int[]{
//                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar,
//                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
//                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
                MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,
        };
    }

    private static final boolean isRecognizedViewoFormat(final int colorFormat) {
        final int n = recognizedFormats != null ? recognizedFormats.length : 0;
        for (int i = 0; i < n; i++) {
            if (recognizedFormats[i] == colorFormat) {
                return true;
            }
        }
        return false;
    }


    private byte[] nv21ToI420(byte[] data, int width, int height) {
        byte[] ret = new byte[width * height * 3 / 2];
        int total = width * height;

        ByteBuffer bufferY = ByteBuffer.wrap(ret, 0, total);         // I420的Y分量
        ByteBuffer bufferU = ByteBuffer.wrap(ret, total, total / 4); // I420的U分量
        ByteBuffer bufferV = ByteBuffer.wrap(ret, total + total / 4, total / 4); // I420的V分量
        // NV21 YYYYYYYY VUVU
        bufferY.put(data, 0, total);
        for (int i = total; i < data.length; i += 2) {
            bufferV.put(data[i]);
            bufferU.put(data[i + 1]);
        }

        return ret;
    }

    private byte[] nv12ToI420(byte[] data, int width, int height) {
        byte[] ret = new byte[width * height * 3 / 2];
        int total = width * height;

        ByteBuffer bufferY = ByteBuffer.wrap(ret, 0, total);         // I420的Y分量
        ByteBuffer bufferU = ByteBuffer.wrap(ret, total, total / 4); // I420的U分量
        ByteBuffer bufferV = ByteBuffer.wrap(ret, total + total / 4, total / 4); // I420的V分量

        // NV12 YYYYYYYY UVUV
        bufferY.put(data, 0, total);
        for (int i = total; i < data.length; i += 2) {
            bufferU.put(data[i]);
            bufferV.put(data[i + 1]);
        }
        return ret;
    }

    private byte[] nv12ToNv21(byte[] data, int width, int height) {
        byte[] ret = new byte[width * height * 3 / 2];
        int total = width * height;

        ByteBuffer bufferY = ByteBuffer.wrap(ret, 0, total);         // I420的Y分量
        ByteBuffer bufferU = ByteBuffer.wrap(ret, total, total / 4); // I420的U分量
        ByteBuffer bufferV = ByteBuffer.wrap(ret, total + total / 4, total / 4); // I420的V分量

        // NV12 YYYYYYYY UVUV
        bufferY.put(data, 0, total);
        for (int i = total; i < data.length; i += 2) {
            bufferU.put(data[i]);
            bufferV.put(data[i + 1]);
        }
        return ret;
    }

    // YYYYYYYY UVUV(nv21)--> YYYYYYYY VUVU(nv12)
    private byte[] nv21ToNV12(byte[] nv21, int width, int height) {
        byte[] ret = new byte[width * height * 3 /2];
        int framesize = width * height;
        int i = 0, j = 0;
        // 拷贝Y分量
        System.arraycopy(nv21, 0,ret , 0, framesize);
        // 拷贝UV分量
        for (j = framesize; j < nv21.length; j += 2) {
            ret[j+1] = nv21[j+1];
            ret[j] = nv21[j];
        }
        return ret;
    }

    // YYYYYYYY UVUV(nv12)--> YYYYYYYY VUVU(nv21)
    private byte[] nv12ToNV21(byte[] nv12, int width, int height) {
        byte[] ret = new byte[width * height * 3 /2];
        int framesize = width * height;
        int i = 0, j = 0;
        // 拷贝Y分量
        System.arraycopy(nv12, 0,ret , 0, framesize);
        // 拷贝UV分量
        for (j = framesize; j < nv12.length; j += 2) {
            ret[j] = nv12[j+1];
            ret[j+1] = nv12[j];
        }
        return ret;
    }

}
