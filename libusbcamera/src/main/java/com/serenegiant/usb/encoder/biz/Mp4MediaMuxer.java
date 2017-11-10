package com.serenegiant.usb.encoder.biz;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**Mp4封装混合器
 *
 * Created by jianddongguo on 2017/7/28.
 */

public class Mp4MediaMuxer {
    private static final boolean VERBOSE = false;
    private static final String TAG = Mp4MediaMuxer.class.getSimpleName();
    private String mFilePath;
    private MediaMuxer mMuxer;
    private long durationMillis;
    private int index = 0;
    private int mVideoTrackIndex = -1;
    private int mAudioTrackIndex = -1;
    private long mBeginMillis;
    private MediaFormat mVideoFormat;
    private MediaFormat mAudioFormat;
    private boolean isVoiceClose;

    // 文件路径；文件时长
    public Mp4MediaMuxer(String path, long durationMillis,boolean isVoiceClose) {
        String mFilePath;
        this.isVoiceClose = isVoiceClose;
        this.durationMillis = durationMillis;
        if(durationMillis != 0) {
            mFilePath = path + "-" + index++ + ".mp4";
        }else{
            mFilePath = path+".mp4";
        }
        Object mux = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mux = new MediaMuxer(mFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mMuxer = (MediaMuxer) mux;
        }
    }

    public synchronized void addTrack(MediaFormat format, boolean isVideo) {
        // now that we have the Magic Goodies, start the muxer
        if ((!isVoiceClose && mAudioTrackIndex != -1) && mVideoTrackIndex != -1)
            throw new RuntimeException("already add all tracks");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            int track = mMuxer.addTrack(format);
            if (VERBOSE)
                Log.i(TAG, String.format("addTrack %s result %d", isVideo ? "video" : "audio", track));
            if (isVideo) {
                mVideoFormat = format;
                mVideoTrackIndex = track;
                // 当音频轨道添加
                // 或者开启静音就start
                if (isVoiceClose || mAudioTrackIndex != -1) {
                    if (VERBOSE)
                        Log.i(TAG, "both audio and video added,and muxer is started");
                    mMuxer.start();
                    mBeginMillis = System.currentTimeMillis();
                }
            } else {
                mAudioFormat = format;
                mAudioTrackIndex = track;
                if (mVideoTrackIndex != -1) {
                    mMuxer.start();
                    mBeginMillis = System.currentTimeMillis();
                }
            }
        }
    }

    public synchronized void pumpStream(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo, boolean isVideo) {
        if ((!isVoiceClose && mAudioTrackIndex == -1) || mVideoTrackIndex == -1) {
//            Log.i(TAG, String.format("pumpStream [%s] but muxer is not start.ignore..", isVideo ? "video" : "audio"));
            return;
        }
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // The codec config data was pulled out and fed to the muxer when we got
            // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
        } else if (bufferInfo.size != 0) {
            if (isVideo && mVideoTrackIndex == -1) {
                throw new RuntimeException("muxer hasn't started");
            }

            // adjust the ByteBuffer values to match BufferInfo (not needed?)
            outputBuffer.position(bufferInfo.offset);
            outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mMuxer.writeSampleData(isVideo ? mVideoTrackIndex : mAudioTrackIndex, outputBuffer, bufferInfo);
            }
//            if (VERBOSE)
//                Log.d(TAG, String.format("sent %s [" + bufferInfo.size + "] with timestamp:[%d] to muxer", isVideo ? "video" : "audio", bufferInfo.presentationTimeUs / 1000));
        }

        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//            if (VERBOSE)
//                Log.i(TAG, "BUFFER_FLAG_END_OF_STREAM received");
        }

        if (durationMillis!=0 && System.currentTimeMillis() - mBeginMillis >= durationMillis) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//                if (VERBOSE)
//                    Log.i(TAG, String.format("record file reach expiration.create new file:" + index));
                mMuxer.stop();
                mMuxer.release();
                mMuxer = null;
                mVideoTrackIndex = mAudioTrackIndex = -1;
                try {
                    mMuxer = new MediaMuxer(mFilePath + "-" + ++index + ".mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    addTrack(mVideoFormat, true);
                    addTrack(mAudioFormat, false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (mMuxer != null) {
                //(!isVoiceClose&&mAudioTrackIndex != -1)
                if (mVideoTrackIndex  != -1) {
                    if (VERBOSE)
                        Log.i(TAG, String.format("muxer is started. now it will be stoped."));
                    try {
                        mMuxer.stop();
                        mMuxer.release();
                    } catch (IllegalStateException ex) {
                        ex.printStackTrace();
                    }

                    if (System.currentTimeMillis() - mBeginMillis <= 1500){
                        new File(mFilePath + "-" + index + ".mp4").delete();
                    }
                    mAudioTrackIndex = mVideoTrackIndex = -1;
                }else{
                    if (VERBOSE)
                        Log.i(TAG, String.format("muxer is failed to be stoped."));
                }
            }
        }
    }
}
