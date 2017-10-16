/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.serenegiant.usb.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public abstract class MediaEncoder implements Runnable {
	private static final boolean DEBUG = true;	// TODO set false on release
	private static final String TAG = "MediaEncoder";
	public static final int TYPE_AUDIO = 0;		// 音频数据
	public static final int TYPE_VIDEO = 1;		// 视频数据

	protected static final int TIMEOUT_USEC = 10000;	// 10[msec]
	protected static final int MSG_FRAME_AVAILABLE = 1;
	protected static final int MSG_STOP_RECORDING = 9;

	public interface MediaEncoderListener {
		void onPrepared(MediaEncoder encoder);
		void onStopped(MediaEncoder encoder);
		// 音频或视频流，type=0为音频，type=1为视频
		void onEncodeResult(byte[] data, int offset,
							int length, long timestamp, int type);
	}

	protected final Object mSync = new Object();
	/**
	 * Flag that indicate this encoder is capturing now.
	 */
    protected volatile boolean mIsCapturing;
	/**
	 * Flag that indicate the frame data will be available soon.
	 */
	private int mRequestDrain;
    /**
     * Flag to request stop capturing
     */
    protected volatile boolean mRequestStop;
    /**
     * Flag that indicate encoder received EOS(End Of Stream)
     */
    protected boolean mIsEOS;
    /**
     * Flag the indicate the muxer is running
     */
    protected boolean mMuxerStarted;
    /**
     * Track Number
     */
    protected int mTrackIndex;
    /**
     * MediaCodec instance for encoding
     */
    protected MediaCodec mMediaCodec;				// API >= 16(Android4.1.2)
    /**
     * Weak refarence of MediaMuxerWarapper instance
     */
    protected final WeakReference<MediaMuxerWrapper> mWeakMuxer;
    /**
     * BufferInfo instance for dequeuing
     */
    private MediaCodec.BufferInfo mBufferInfo;		// API >= 16(Android4.1.2)

    protected final MediaEncoderListener mListener;

	/**
	 * There are 13 supported frequencies by ADTS.
	 **/
	public static final int[] AUDIO_SAMPLING_RATES = { 96000, // 0
			88200, // 1
			64000, // 2
			48000, // 3
			44100, // 4
			32000, // 5
			24000, // 6
			22050, // 7
			16000, // 8
			12000, // 9
			11025, // 10
			8000, // 11
			7350, // 12
			-1, // 13
			-1, // 14
			-1, // 15
	};

    public MediaEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener) {
    	if (listener == null) throw new NullPointerException("MediaEncoderListener is null");
    	if (muxer == null) throw new NullPointerException("MediaMuxerWrapper is null");
		mWeakMuxer = new WeakReference<MediaMuxerWrapper>(muxer);
		muxer.addEncoder(this);
		mListener = listener;
        synchronized (mSync) {
            // create BufferInfo here for effectiveness(to reduce GC)
            mBufferInfo = new MediaCodec.BufferInfo();
            // wait for starting thread
            new Thread(this, getClass().getSimpleName()).start();
            try {
            	mSync.wait();
            } catch (final InterruptedException e) {
            }
        }
	}

    public String getOutputPath() {
    	final MediaMuxerWrapper muxer = mWeakMuxer.get();
    	return muxer != null ? muxer.getOutputPath() : null;
    }

    /**
     * the method to indicate frame data is soon available or already available
     * @return return true if encoder is ready to encod.
     */
    public boolean frameAvailableSoon() {
//    	if (DEBUG) Log.v(TAG, "frameAvailableSoon");
        synchronized (mSync) {
            if (!mIsCapturing || mRequestStop) {
                return false;
            }
            mRequestDrain++;
            mSync.notifyAll();
        }
        return true;
    }

    /**
     * encoding loop on private thread
     */
	@Override
	public void run() {
//		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        synchronized (mSync) {
            mRequestStop = false;
    		mRequestDrain = 0;
            mSync.notify();
        }
        final boolean isRunning = true;
        boolean localRequestStop;
        boolean localRequestDrain;
        while (isRunning) {
        	synchronized (mSync) {
        		localRequestStop = mRequestStop;
        		localRequestDrain = (mRequestDrain > 0);
        		if (localRequestDrain)
        			mRequestDrain--;
        	}
	        if (localRequestStop) {
	           	drain();
	           	// request stop recording
	           	signalEndOfInputStream();
	           	// process output data again for EOS signale
	           	drain();
	           	// release all related objects
	           	release();
	           	break;
	        }
	        if (localRequestDrain) {
	        	drain();
	        } else {
	        	synchronized (mSync) {
		        	try {
						mSync.wait();
					} catch (final InterruptedException e) {
						break;
					}
	        	}
        	}
        } // end of while
		if (DEBUG) Log.d(TAG, "Encoder thread exiting");
        synchronized (mSync) {
        	mRequestStop = true;
            mIsCapturing = false;
        }
	}

	/*
    * prepareing method for each sub class
    * this method should be implemented in sub class, so set this as abstract method
    * @throws IOException
    */
   /*package*/ abstract void prepare() throws IOException;

	/*package*/ void startRecording() {
   	if (DEBUG) Log.v(TAG, "startRecording");
		synchronized (mSync) {
			mIsCapturing = true;
			mRequestStop = false;
			mSync.notifyAll();
		}
	}

   /**
    * the method to request stop encoding
    */
	/*package*/ void stopRecording() {
		if (DEBUG) Log.v(TAG, "stopRecording");
		synchronized (mSync) {
			if (!mIsCapturing || mRequestStop) {
				return;
			}
			mRequestStop = true;	// for rejecting newer frame
			mSync.notifyAll();
	        // We can not know when the encoding and writing finish.
	        // so we return immediately after request to avoid delay of caller thread
		}
	}

//********************************************************************************
//********************************************************************************
    /**
     * Release all releated objects
     */
    protected void release() {
		if (DEBUG) Log.d(TAG, "release:");
		try {
			mListener.onStopped(this);
		} catch (final Exception e) {
			Log.e(TAG, "failed onStopped", e);
		}
		mIsCapturing = false;
        if (mMediaCodec != null) {
			try {
	            mMediaCodec.stop();
	            mMediaCodec.release();
	            mMediaCodec = null;
			} catch (final Exception e) {
				Log.e(TAG, "failed releasing MediaCodec", e);
			}
        }
        if (mMuxerStarted) {
       		final MediaMuxerWrapper muxer = mWeakMuxer.get();
       		if (muxer != null) {
       			try {
           			muxer.stop();
    			} catch (final Exception e) {
    				Log.e(TAG, "failed stopping muxer", e);
    			}
       		}
        }
        mBufferInfo = null;
    }

    protected void signalEndOfInputStream() {
		if (DEBUG) Log.d(TAG, "sending EOS to encoder");
        // signalEndOfInputStream is only avairable for video encoding with surface
        // and equivalent sending a empty buffer with BUFFER_FLAG_END_OF_STREAM flag.
//		mMediaCodec.signalEndOfInputStream();	// API >= 18
        encode((byte[])null, 0, getPTSUs());
	}

    /**
     * Method to set byte array to the MediaCodec encoder
     * @param buffer
     * @param length　length of byte array, zero means EOS.
     * @param presentationTimeUs
     */
    @SuppressWarnings("deprecation")
	protected void encode(final byte[] buffer, final int length, final long presentationTimeUs) {
//    	if (DEBUG) Log.v(TAG, "encode:buffer=" + buffer);
    	if (!mIsCapturing) return;
    	int ix = 0, sz;
        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        while (mIsCapturing && ix < length) {
	        final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
	        if (inputBufferIndex >= 0) {
	            final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
	            inputBuffer.clear();
	            sz = inputBuffer.remaining();
	            sz = (ix + sz < length) ? sz : length - ix;
	            if (sz > 0 && (buffer != null)) {
	            	inputBuffer.put(buffer, ix, sz);
	            }
	            ix += sz;
//	            if (DEBUG) Log.v(TAG, "encode:queueInputBuffer");
	            if (length <= 0) {
	            	// send EOS
	            	mIsEOS = true;
	            	if (DEBUG) Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
	            	mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
	            		presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
		            break;
	            } else {
	            	mMediaCodec.queueInputBuffer(inputBufferIndex, 0, sz,
	            		presentationTimeUs, 0);
	            }
	        } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
	        	// wait for MediaCodec encoder is ready to encode
	        	// nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
	        	// will wait for maximum TIMEOUT_USEC(10msec) on each call
	        }
        }
    }

    /**
     * Method to set ByteBuffer to the MediaCodec encoder
     * @param buffer null means EOS
     * @param presentationTimeUs
     */
    @SuppressWarnings("deprecation")
	protected void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
//    	if (DEBUG) Log.v(TAG, "encode:buffer=" + buffer);
    	if (!mIsCapturing) return;
    	int ix = 0, sz;
        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        while (mIsCapturing && ix < length) {
	        final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
	        if (inputBufferIndex >= 0) {
	            final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
	            inputBuffer.clear();
	            sz = inputBuffer.remaining();
	            sz = (ix + sz < length) ? sz : length - ix;
	            if (sz > 0 && (buffer != null)) {
					buffer.position(ix + sz);
					buffer.flip();
	            	inputBuffer.put(buffer);
	            }
	            ix += sz;
//	            if (DEBUG) Log.v(TAG, "encode:queueInputBuffer");
	            if (length <= 0) {
	            	// send EOS
	            	mIsEOS = true;
	            	if (DEBUG) Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
	            	mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
	            		presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
		            break;
	            } else {
	            	mMediaCodec.queueInputBuffer(inputBufferIndex, 0, sz,
	            		presentationTimeUs, 0);
	            }
	        } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
	        	// wait for MediaCodec encoder is ready to encode
	        	// nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
	        	// will wait for maximum TIMEOUT_USEC(10msec) on each call
	        }
        }
    }

    /**
     * drain encoded data and write them to muxer
     */
    @SuppressWarnings("deprecation")
	protected void drain() {
    	if (mMediaCodec == null) return;
        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        int encoderStatus, count = 0;
        final MediaMuxerWrapper muxer = mWeakMuxer.get();
        if (muxer == null) {
//        	throw new NullPointerException("muxer is unexpectedly null");
        	Log.w(TAG, "muxer is unexpectedly null");
        	return;
        }
		byte[] mPpsSps = new byte[0];
		byte[] h264 = new byte[640 * 480];
		ByteBuffer mBuffer = ByteBuffer.allocate(10240);

LOOP:	while (mIsCapturing) {
			// get encoded data with maximum timeout duration of TIMEOUT_USEC(=10[msec])
            encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait 5 counts(=TIMEOUT_USEC x 5 = 50msec) until data/EOS come
                if (!mIsEOS) {
                	if (++count > 5)
                		break LOOP;		// out of while
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            	if (DEBUG) Log.v(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                // this shoud not come when encoding
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            	if (DEBUG) Log.v(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
            	// this status indicate the output format of codec is changed
                // this should come only once before actual encoded data
            	// but this status never come on Android4.3 or less
            	// and in that case, you should treat when MediaCodec.BUFFER_FLAG_CODEC_CONFIG come.
                if (mMuxerStarted) {	// second time request is error
                    throw new RuntimeException("format changed twice");
                }
				// get output format from codec and pass them to muxer
				// getOutputFormat should be called after INFO_OUTPUT_FORMAT_CHANGED otherwise crash.
                final MediaFormat format = mMediaCodec.getOutputFormat(); // API >= 16
               	mTrackIndex = muxer.addTrack(format);
               	mMuxerStarted = true;
               	if (!muxer.start()) {
               		// we should wait until muxer is ready
               		synchronized (muxer) {
	               		while (!muxer.isStarted())
						try {
							muxer.wait(100);
						} catch (final InterruptedException e) {
							break LOOP;
						}
               		}
               	}
            } else if (encoderStatus < 0) {
            	// unexpected status
            	if (DEBUG) Log.w(TAG, "drain:unexpected result from encoder#dequeueOutputBuffer: " + encoderStatus);
            } else {
                final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                	// this never should come...may be a MediaCodec internal error
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                	// You shoud set output format to muxer here when you target Android4.3 or less
                	// but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                	// therefor we should expand and prepare output format from buffer data.
                	// This sample is for API>=18(>=Android 4.3), just ignore this flag here
					if (DEBUG) Log.d(TAG, "drain:BUFFER_FLAG_CODEC_CONFIG");
					mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                	// encoded data is ready, clear waiting counter
            		count = 0;
                    if (!mMuxerStarted) {
                    	// muxer is not ready...this will prrograming failure.
                        throw new RuntimeException("drain:muxer hasn't started");
                    }
                    // write encoded data to muxer(need to adjust presentationTimeUs.
                   	mBufferInfo.presentationTimeUs = getPTSUs();
                   	muxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
					prevOutputPTSUs = mBufferInfo.presentationTimeUs;

					//  推流，获取h.264数据流
//					if(mListener != null){
//						byte[] temp = new byte[mBufferInfo.size];
//							encodedData.get(temp);
//						mListener.onEncodeResult(temp, 0,mBufferInfo.size, mBufferInfo.presentationTimeUs / 1000,TYPE_VIDEO);
//					}
					// 根据mBufferInfo.size来判断音视频
					// > 1000，视频；< 1000，音频
					if(mBufferInfo.size > 1000){
						boolean sync = false;
						if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {// sps
							sync = (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0;
							if (!sync) {
								byte[] temp = new byte[mBufferInfo.size];
								encodedData.get(temp);
								mPpsSps = temp;
								mMediaCodec.releaseOutputBuffer(encoderStatus, false);
								continue;
							} else {
								mPpsSps = new byte[0];
							}
						}
						sync |= (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0;
						int len = mPpsSps.length + mBufferInfo.size;
						if (len > h264.length) {
							h264 = new byte[len];
						}
						if (sync) {
							System.arraycopy(mPpsSps, 0, h264, 0, mPpsSps.length);
							encodedData.get(h264, mPpsSps.length, mBufferInfo.size);

							if(mListener != null){
								mListener.onEncodeResult(h264, 0,mPpsSps.length + mBufferInfo.size, mBufferInfo.presentationTimeUs / 1000,TYPE_VIDEO);
							}
						} else {
							encodedData.get(h264, 0, mBufferInfo.size);
							if(mListener != null){
								mListener.onEncodeResult(h264, 0,mBufferInfo.size, mBufferInfo.presentationTimeUs / 1000,TYPE_VIDEO);
							}
						}
					} else {
						mBuffer.clear();
						encodedData.get(mBuffer.array(), 7, mBufferInfo.size);
						encodedData.clear();
						mBuffer.position(7 + mBufferInfo.size);
						addADTStoPacket(mBuffer.array(), mBufferInfo.size + 7);
						mBuffer.flip();
						if(mListener != null){
							mListener.onEncodeResult(mBuffer.array(),0, mBufferInfo.size + 7, mBufferInfo.presentationTimeUs / 1000,TYPE_AUDIO);
						}
					}
                }
                // return buffer to encoder
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                	// when EOS come.
               		mMuxerStarted = mIsCapturing = false;
                    break;      // out of while
                }
            }
        }
    }

	private void addADTStoPacket(byte[] packet, int packetLen) {
		packet[0] = (byte) 0xFF;
		packet[1] = (byte) 0xF1;
		packet[2] = (byte) (((2 - 1) << 6) + (getSamplingRateIndex() << 2) + (1 >> 2));
		packet[3] = (byte) (((1 & 3) << 6) + (packetLen >> 11));
		packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
		packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
		packet[6] = (byte) 0xFC;
	}

	private int getSamplingRateIndex(){
		int mSamplingRateIndex = -1;
		for (int i=0;i < AUDIO_SAMPLING_RATES.length; i++) {
			if (AUDIO_SAMPLING_RATES[i] == MediaAudioEncoder.SAMPLE_RATE) {
				mSamplingRateIndex = i;
				break;
			}
		}
		return mSamplingRateIndex;
	}

    /**
     * previous presentationTimeUs for writing
     */
	private long prevOutputPTSUs = 0;
	/**
	 * get next encoding presentationTimeUs
	 * @return
	 */
    protected long getPTSUs() {
		long result = System.nanoTime() / 1000L;
		// presentationTimeUs should be monotonic
		// otherwise muxer fail to write
		if (result < prevOutputPTSUs)
			result = (prevOutputPTSUs - result) + result;
		return result;
    }

}
