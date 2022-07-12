package com.serenegiant.media;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2018 saki t_saki@serenegiant.com
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
*/

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public abstract class MediaReaper implements Runnable {
	private static final boolean DEBUG = false;	// FIXME 実働時はfalseにすること
	private static final String TAG = MediaReaper.class.getSimpleName();

	public static final int REAPER_VIDEO = 0;
	public static final int REAPER_AUDIO = 1;

	@IntDef({REAPER_VIDEO,
		REAPER_AUDIO,
	})
	@Retention(RetentionPolicy.SOURCE)
	@interface ReaperType {}

	public static final int TIMEOUT_USEC = 10000;	// 10ミリ秒

	public interface ReaperListener {
		public void writeSampleData(@NonNull final MediaReaper reaper,
			final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo);
		public void onOutputFormatChanged(@NonNull final MediaReaper reaper,
			@NonNull final MediaFormat format);
		public void onStop(@NonNull final MediaReaper reaper);
		public void onError(@NonNull final MediaReaper reaper, final Exception e);
	}

	public static class VideoReaper extends MediaReaper {
		public static final String MIME_AVC = "video/avc";
		private final int mWidth;
		private final int mHeight;
		public VideoReaper(final MediaCodec encoder, @NonNull final ReaperListener listener,
			final int width, final int height) {
			
			super(REAPER_VIDEO, encoder, listener);
			if (DEBUG) Log.v(TAG, "VideoReaper#コンストラクタ");
			mWidth = width;
			mHeight = height;
		}

		@Override
		protected MediaFormat createOutputFormat(final byte[] csd, final int size,
			final int ix0, final int ix1, final int ix2) {
			
			if (DEBUG) Log.v(TAG, "VideoReaper#createOutputFormat");
			final MediaFormat outFormat;
			if (ix0 >= 0) {
				outFormat = MediaFormat.createVideoFormat(MIME_AVC, mWidth, mHeight);
				final ByteBuffer csd0 = ByteBuffer.allocateDirect(ix1 - ix0)
					.order(ByteOrder.nativeOrder());
				csd0.put(csd, ix0, ix1 - ix0);
				csd0.flip();
				outFormat.setByteBuffer("csd-0", csd0);
				if (ix1 > ix0) {
					final int sz = (ix2 > ix1) ? (ix2 - ix1) : (size - ix1);
					final ByteBuffer csd1 = ByteBuffer.allocateDirect(sz)
						.order(ByteOrder.nativeOrder());
					csd1.put(csd, ix1, sz);
					csd1.flip();
					outFormat.setByteBuffer("csd-1", csd1);
				}
			} else {
				throw new RuntimeException("unexpected csd data came.");
			}
			return outFormat;
		}
	}

	public static class AudioReaper extends MediaReaper {
		private static final String MIME_TYPE = "audio/mp4a-latm";
		
		private final int mSampleRate;
		private final int mChannelCount;
		
		public AudioReaper(final MediaCodec encoder, @NonNull final ReaperListener listener,
			final int sampleRate, final int channelCount) {

			super(REAPER_AUDIO, encoder, listener);
			mSampleRate = sampleRate;
			mChannelCount = channelCount;
		}
		
		@Override
		protected MediaFormat createOutputFormat(final byte[] csd, final int size,
			final int ix0, final int ix1, final int ix2) {

			MediaFormat outFormat;
	        if (ix0 >= 0) {
	//        	Log.w(TAG, "csd may be wrong, it may be for video");
	        }
	        // audioの時はSTART_MARKが無いので全体をコピーして渡す
	        outFormat = MediaFormat.createAudioFormat(MIME_TYPE, mSampleRate, mChannelCount);
	        final ByteBuffer csd0 = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
	        csd0.put(csd, 0, size);
	        csd0.flip();
	        outFormat.setByteBuffer("csd-0", csd0);
	        return outFormat;
		}
	}
	
	private final Object mSync = new Object();
	private final WeakReference<MediaCodec> mWeakEncoder;
	private final ReaperListener mListener;
	@ReaperType
	private final int mReaperType;
	/**
	 * エンコード用バッファ
	 */
	private MediaCodec.BufferInfo mBufferInfo;		// API >= 16(Android4.1.2)
	private volatile boolean mIsRunning;
	private volatile boolean mRecorderStarted;
	private boolean mRequestStop;
	private int mRequestDrain;
	private volatile boolean mIsEOS;


	public MediaReaper(@ReaperType final int reaperType,
		final MediaCodec encoder, @NonNull final ReaperListener listener) {

		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mWeakEncoder = new WeakReference<MediaCodec>(encoder);
		mListener = listener;
		mReaperType = reaperType;
		mBufferInfo = new MediaCodec.BufferInfo();
		synchronized (mSync) {
			// Reaperスレッドを生成
			new Thread(this, getClass().getSimpleName()).start();
			try {
				mSync.wait();	// エンコーダースレッド起床待ち
			} catch (final InterruptedException e) {
			}
		}
	}

	public void release() {
		if (DEBUG) Log.v(TAG, "release:");
		if (mIsRunning && !mRequestStop) {
			mRequestStop = true;
//			final MediaCodec encoder = mWeakEncoder.get();
//			if (encoder != null) {
//				try {
//					encoder.release();
//				} catch (final Exception e) {
//					Log.w(TAG, e);
//				}
//			}
		}
		synchronized (mSync) {
			mSync.notifyAll();
		}
	}

	public void frameAvailableSoon() {
//		if (DEBUG) Log.v(TAG, "frameAvailableSoon:");
        synchronized (mSync) {
            if (!mIsRunning || mRequestStop) {
                return;
            }
            mRequestDrain++;
            mSync.notifyAll();
        }
    }

	@ReaperType
	public int reaperType() {
		return mReaperType;
	}
	
	@Override
	public void run() {
		android.os.Process.setThreadPriority(
			android.os.Process.THREAD_PRIORITY_DISPLAY); // THREAD_PRIORITY_URGENT_AUDIO
        synchronized (mSync) {
			mIsRunning = true;
            mRequestStop = false;
    		mRequestDrain = 0;
			mSync.notify();	// 起床通知
        }
        boolean localRequestStop;
        boolean localRequestDrain;
    	for ( ; mIsRunning ; ) {
        	synchronized (mSync) {
        		localRequestStop = mRequestStop;
        		localRequestDrain = (mRequestDrain > 0);
        		if (localRequestDrain)
        			mRequestDrain--;
        	}
        	try {
				if (localRequestStop) {
					drain();
					mIsEOS = true;
					release();
					break;
				}
				if (localRequestDrain) {
					drain();
				} else {
					synchronized (mSync) {
						try {
							mSync.wait(50);
						} catch (final InterruptedException e) {
							break;
						}
					}
				}
			} catch (final IllegalStateException e) {
				break;
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
    	} // end of while
        synchronized (mSync) {
        	mRequestStop = true;
            mIsRunning = false;
        }
	}

	private final void drain() {
		final MediaCodec encoder = mWeakEncoder.get();
    	if (encoder == null) return;
    	ByteBuffer[] encoderOutputBuffers;
    	try {
    		encoderOutputBuffers = encoder.getOutputBuffers();
    	} catch (final IllegalStateException e) {
//    		Log.w(TAG, "drain:", e);
    		return;
    	}
        int encoderStatus, count = 0;
LOOP:	for ( ; mIsRunning ; ) {
            encoderStatus = encoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);	// wait for max TIMEOUT_USEC(=10msec)
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // 出力するデータが無い時は最大でTIMEOUT_USEC x 5 = 50msec経過するかEOSが来るまでループする
                if (!mIsEOS) {
                	if (++count > 5)
                		break LOOP;		// out of while
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
//            	if (DEBUG) Log.v(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                // エンコード時にはこれは来ないはず
                encoderOutputBuffers = encoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//            	if (DEBUG) Log.v(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
            	// コーデックからの出力フォーマットが変更された時
                // エンコード済みバッファの受け取る前にだけ１回来るはず。
            	// ただし、Android4.3未満だとINFO_OUTPUT_FORMAT_CHANGEDは来ないので
            	// 代わりにflags & MediaCodec.BUFFER_FLAG_CODEC_CONFIGの時に処理しないとだめ
                if (mRecorderStarted) {	// ２回目が来た時はエラー
                    throw new RuntimeException("format changed twice");
                }
				// コーデックからの出力フォーマットを取得してnative側へ引き渡す
				// getOutputFormatはINFO_OUTPUT_FORMAT_CHANGEDが来た後でないと呼んじゃダメ(クラッシュする)
                final MediaFormat format = encoder.getOutputFormat(); // API >= 16
                if (!callOnFormatChanged(format))
                	break LOOP;
            } else if (encoderStatus >= 0) {
                final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                	// 出力バッファインデックスが来てるのに出力バッファを取得できない・・・無いはずやねんけど
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
//					if (DEBUG) Log.d(TAG, "drain:BUFFER_FLAG_CODEC_CONFIG");
                	// Android4.3未満をターゲットにするならここで処理しないと駄目
                    if (!mRecorderStarted) {	// 1回目に来た時だけ処理する
                    	// csd-0とcsd-1が同時に来ているはずなので分離してセットする
                        final byte[] tmp = new byte[mBufferInfo.size];
                        encodedData.position(0);
                        encodedData.get(tmp, mBufferInfo.offset, mBufferInfo.size);
                        encodedData.position(0);
                        final int ix0 = MediaCodecHelper.findStartMarker(tmp, 0);
                        final int ix1 = MediaCodecHelper.findStartMarker(tmp, ix0 + 2);
						final int ix2 = MediaCodecHelper.findStartMarker(tmp, ix1 + 2);
//						if (DEBUG) Log.i(TAG, "ix0=" + ix0 + ",ix1=" + ix1);
                        final MediaFormat outFormat
                        	= createOutputFormat(tmp, mBufferInfo.size, ix0, ix1, ix2);
                        if (!callOnFormatChanged(outFormat))
                        	break LOOP;
                    }
					mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                	// エンコード済みバッファにデータが入っている時・・・待機カウンタをクリア
            		count = 0;
                    if (!mRecorderStarted) {
                    	// でも出力可能になっていない時
                    	// =INFO_OUTPUT_FORMAT_CHANGED/BUFFER_FLAG_CODEC_CONFIGをまだ受け取ってない時
                        throw new RuntimeException("drain:muxer hasn't started");
                    }
                    // ファイルに出力(presentationTimeUsを調整)
                    try {
	                   	mBufferInfo.presentationTimeUs
	                   		= getNextOutputPTSUs(mBufferInfo.presentationTimeUs);
	                   	mListener.writeSampleData(MediaReaper.this, encodedData, mBufferInfo);
                    } catch (final TimeoutException e) {
//						if (DEBUG) Log.v(TAG, "最大録画時間を超えた", e);
						callOnError(e);
                    } catch (final Exception e) {
//						if (DEBUG) Log.w(TAG, e);
						callOnError(e);
                    }
                }
                // 出力済みのバッファをエンコーダーに返す
                encoder.releaseOutputBuffer(encoderStatus, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                	// ストリーム終了指示が来た時
                	callOnStop();
					break LOOP;
                }
            }
        }	// for ( ; mIsRunning ; )
//		if (DEBUG) Log.v(TAG, "drain:finished");
    }

	protected abstract MediaFormat createOutputFormat(final byte[] csd, final int size,
		final int ix0, final int ix1, final int ix2);

	private boolean callOnFormatChanged(final MediaFormat format) {
		try {
			mListener.onOutputFormatChanged(this, format);
			mRecorderStarted = true;
			return true;
		} catch (final Exception e) {
			callOnError(e);
		}
		return false;
	}

	private void callOnStop() {
		try {
			mListener.onStop(this);
		} catch (final Exception e) {
			callOnError(e);
		}
	}

	private void callOnError(final Exception e) {
		try {
			mListener.onError(this, e);
		} catch (final Exception e1) {
			Log.w(TAG, e1);
		}
	}

	/**
	 * 前回出力時のpresentationTimeUs
	 */
	private long prevOutputPTSUs = -1;

	/**
	 * Muxerの今回の書き込み用のpresentationTimeUs値を取得
	 * @return
	 */
	protected long getNextOutputPTSUs(long presentationTimeUs) {
		if (presentationTimeUs <= prevOutputPTSUs) {
			presentationTimeUs = prevOutputPTSUs + 9643;
		}
		prevOutputPTSUs = presentationTimeUs;
		return presentationTimeUs;
	}

}
