package com.serenegiant.usb.encoder.biz;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**将PCM编码为AAC
 *
 * Created by jianddongguo on 2017/7/21.
 */

public class AACEncodeConsumer extends Thread{
    private static final boolean DEBUG = false;
    private static final String TAG = "TMPU";
    private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final long TIMES_OUT = 1000;
    private static final int SAMPLE_RATE = 8000;     // 采样率
    private static final int BIT_RATE = 16000;       // 比特率
    private static final int BUFFER_SIZE = 1920;     // 最小缓存
    private int outChannel = 1;
    private int bitRateForLame = 32;
    private int qaulityDegree = 7;
    private int bufferSizeInBytes;

    private AudioRecord mAudioRecord; // 音频采集
    private MediaCodec mAudioEncoder;   // 音频编码
    private OnAACEncodeResultListener listener;
    private  int mSamplingRateIndex = 0;//ADTS
    private boolean isEncoderStart = false;
    private boolean isRecMp3 = false;
    private boolean isExit = false;
    private long prevPresentationTimes = 0;
    private WeakReference<Mp4MediaMuxer> mMuxerRef;
    private MediaFormat newFormat;

    private static final int[] AUDIO_SOURCES = new int[] {
            MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.CAMCORDER,
    };
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

    private FileOutputStream fops;

    // 编码流结果回调接口
    public interface OnAACEncodeResultListener{
        void onEncodeResult(byte[] data, int offset,
                            int length, long timestamp);
    }

    public AACEncodeConsumer(){
        for (int i=0;i < AUDIO_SAMPLING_RATES.length; i++) {
            if (AUDIO_SAMPLING_RATES[i] == SAMPLE_RATE) {
                mSamplingRateIndex = i;
                break;
            }
        }
    }

    public void setOnAACEncodeResultListener(OnAACEncodeResultListener listener){
        this.listener = listener;
    }

    public void exit(){
        isExit = true;
    }

    public synchronized void setTmpuMuxer(Mp4MediaMuxer mMuxer){
        this.mMuxerRef =  new WeakReference<>(mMuxer);
        Mp4MediaMuxer muxer = mMuxerRef.get();
        if (muxer != null && newFormat != null) {
            muxer.addTrack(newFormat, false);
        }
    }

    @Override
    public void run() {
        // 开启音频采集、编码
        if(! isEncoderStart){
            initAudioRecord();
            initMediaCodec();
        }
        // 初始化音频文件参数
        byte[] mp3Buffer = new byte[1024];

        // 这里有问题，当本地录制结束后，没有写入
        while(! isExit){
            byte[] audioBuffer = new byte[2048];
            // 采集音频
            int readBytes = mAudioRecord.read(audioBuffer,0,BUFFER_SIZE);

            if(DEBUG)
                Log.i(TAG,"采集音频readBytes = "+readBytes);
            // 编码音频
            if(readBytes > 0){
                encodeBytes(audioBuffer,readBytes);
            }
        }
        // 停止音频采集、编码
        stopMediaCodec();
        stopAudioRecord();
    }


    @TargetApi(21)
    private void encodeBytes(byte[] audioBuf, int readBytes) {
        ByteBuffer[] inputBuffers = mAudioEncoder.getInputBuffers();
        ByteBuffer[] outputBuffers = mAudioEncoder.getOutputBuffers();
        //返回编码器的一个输入缓存区句柄，-1表示当前没有可用的输入缓存区
        int inputBufferIndex = mAudioEncoder.dequeueInputBuffer(TIMES_OUT);
        if(inputBufferIndex >= 0){
            // 绑定一个被空的、可写的输入缓存区inputBuffer到客户端
            ByteBuffer inputBuffer  = null;
            if(!isLollipop()){
                inputBuffer = inputBuffers[inputBufferIndex];
            }else{
                inputBuffer = mAudioEncoder.getInputBuffer(inputBufferIndex);
            }
            // 向输入缓存区写入有效原始数据，并提交到编码器中进行编码处理
            if(audioBuf==null || readBytes<=0){
                mAudioEncoder.queueInputBuffer(inputBufferIndex,0,0,getPTSUs(),MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }else{
                inputBuffer.clear();
                inputBuffer.put(audioBuf);
                mAudioEncoder.queueInputBuffer(inputBufferIndex,0,readBytes,getPTSUs(),0);
            }
        }

        // 返回一个输出缓存区句柄，当为-1时表示当前没有可用的输出缓存区
        // mBufferInfo参数包含被编码好的数据，timesOut参数为超时等待的时间
        MediaCodec.BufferInfo  mBufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = -1;
        do{
            outputBufferIndex = mAudioEncoder.dequeueOutputBuffer(mBufferInfo,TIMES_OUT);
            if(outputBufferIndex == MediaCodec. INFO_TRY_AGAIN_LATER){
                if(DEBUG)
                    Log.i(TAG,"获得编码器输出缓存区超时");
            }else if(outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                // 如果API小于21，APP需要重新绑定编码器的输入缓存区；
                // 如果API大于21，则无需处理INFO_OUTPUT_BUFFERS_CHANGED
                if(!isLollipop()){
                    outputBuffers = mAudioEncoder.getOutputBuffers();
                }
            }else if(outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                // 编码器输出缓存区格式改变，通常在存储数据之前且只会改变一次
                // 这里设置混合器视频轨道，如果音频已经添加则启动混合器（保证音视频同步）
                if(DEBUG)
                    Log.i(TAG,"编码器输出缓存区格式改变，添加视频轨道到混合器");
                synchronized (AACEncodeConsumer.this) {
                    newFormat = mAudioEncoder.getOutputFormat();
                    if(mMuxerRef != null){
                        Mp4MediaMuxer muxer = mMuxerRef.get();
                        if (muxer != null) {
                            muxer.addTrack(newFormat, false);
                        }
                    }
                }
            }else{
                // 当flag属性置为BUFFER_FLAG_CODEC_CONFIG后，说明输出缓存区的数据已经被消费了
                if((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0){
                    if(DEBUG)
                        Log.i(TAG,"编码数据被消费，BufferInfo的size属性置0");
                    mBufferInfo.size = 0;
                }
                // 数据流结束标志，结束本次循环
                if((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                    if(DEBUG)
                        Log.i(TAG,"数据流结束，退出循环");
                    break;
                }
                // 获取一个只读的输出缓存区inputBuffer ，它包含被编码好的数据
                ByteBuffer mBuffer = ByteBuffer.allocate(10240);
                ByteBuffer outputBuffer = null;
                if(!isLollipop()){
                    outputBuffer  = outputBuffers[outputBufferIndex];
                }else{
                    outputBuffer  = mAudioEncoder.getOutputBuffer(outputBufferIndex);
                }
                if(mBufferInfo.size != 0){
                    // 获取输出缓存区失败，抛出异常
                    if(outputBuffer == null){
                        throw new RuntimeException("encodecOutputBuffer"+outputBufferIndex+"was null");
                    }
                    // 添加视频流到混合器
                    if(mMuxerRef != null){
                        Mp4MediaMuxer muxer = mMuxerRef.get();
                        if (muxer != null) {
                            muxer.pumpStream(outputBuffer, mBufferInfo, false);
                        }
                    }
                    // AAC流添加ADTS头，缓存到mBuffer
                    mBuffer.clear();
                    outputBuffer.get(mBuffer.array(), 7, mBufferInfo.size);
                    outputBuffer.clear();
                    mBuffer.position(7 + mBufferInfo.size);
                    addADTStoPacket(mBuffer.array(), mBufferInfo.size + 7);
                    mBuffer.flip();
                    // 将AAC回调给MainModelImpl进行push
                    if(listener != null){
                        Log.i(TAG,"----->得到aac数据流<-----");
                        listener.onEncodeResult(mBuffer.array(),0, mBufferInfo.size + 7, mBufferInfo.presentationTimeUs / 1000);
                    }
                }
                // 处理结束，释放输出缓存区资源
                mAudioEncoder.releaseOutputBuffer(outputBufferIndex,false);
            }
        }while (outputBufferIndex >= 0);
    }

    private void initAudioRecord(){
        if(DEBUG)
            Log.d(TAG,"AACEncodeConsumer-->开始采集音频");
        // 设置进程优先级
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        for (final int src: AUDIO_SOURCES) {
            try {
                mAudioRecord = new AudioRecord(src,
                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
                if (mAudioRecord != null) {
                    if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                        mAudioRecord.release();
                        mAudioRecord = null;
                    }
                }
            } catch (final Exception e) {
                mAudioRecord = null;
            }
            if (mAudioRecord != null) {
                break;
            }
        }
        mAudioRecord.startRecording();
    }

    private void initMediaCodec(){
        if(DEBUG)
            Log.d(TAG,"AACEncodeConsumer-->开始编码音频");
        MediaCodecInfo mCodecInfo = selectSupportCodec(MIME_TYPE);
        if(mCodecInfo == null){
            Log.e(TAG,"编码器不支持"+MIME_TYPE+"类型");
            return;
        }
        try{
            mAudioEncoder = MediaCodec.createByCodecName(mCodecInfo.getName());
        }catch(IOException e){
            Log.e(TAG,"创建编码器失败"+e.getMessage());
            e.printStackTrace();
        }
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, BUFFER_SIZE);
        mAudioEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mAudioEncoder.start();
        isEncoderStart = true;
    }

    private void stopAudioRecord() {
        if(DEBUG)
            Log.d(TAG,"AACEncodeConsumer-->停止采集音频");
        if(mAudioRecord != null){
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }

    private void stopMediaCodec() {
        if(DEBUG)
            Log.d(TAG,"AACEncodeConsumer-->停止编码音频");
        if(mAudioEncoder != null){
            mAudioEncoder.stop();
            mAudioEncoder.release();
            mAudioEncoder = null;
        }
        isEncoderStart = false;
    }

    // API>=21
    private boolean isLollipop(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    // API<=19
    private boolean isKITKAT(){
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT;
    }

    private long getPTSUs(){
        long result = System.nanoTime()/1000;
        if(result < prevPresentationTimes){
            result = (prevPresentationTimes  - result ) + result;
        }
        return result;
    }

    /**
     * 遍历所有编解码器，返回第一个与指定MIME类型匹配的编码器
     *  判断是否有支持指定mime类型的编码器
     * */
    private MediaCodecInfo selectSupportCodec(String mimeType){
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            // 判断是否为编码器，否则直接进入下一次循环
            if (!codecInfo.isEncoder()) {
                continue;
            }
            // 如果是编码器，判断是否支持Mime类型
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    private void addADTStoPacket(byte[] packet, int packetLen) {
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF1;
        packet[2] = (byte) (((2 - 1) << 6) + (mSamplingRateIndex << 2) + (1 >> 2));
        packet[3] = (byte) (((1 & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }


    private short[] transferByte2Short(byte[] data,int readBytes){
        // byte[] 转 short[]，数组长度缩减一半
        int shortLen = readBytes / 2;
        // 将byte[]数组装如ByteBuffer缓冲区
        ByteBuffer byteBuffer = ByteBuffer.wrap(data, 0, readBytes);
        // 将ByteBuffer转成小端并获取shortBuffer
        ShortBuffer shortBuffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        short[] shortData = new short[shortLen];
        shortBuffer.get(shortData, 0, shortLen);
        return shortData;
    }
}
