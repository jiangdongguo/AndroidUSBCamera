package com.jiangdg.natives

/** pcm to mp3
 *
 * @author Created by jiangdg on 2022/3/2
 */
object LameMp3 {

    init {
        System.loadLibrary("nativelib")
    }

    /** 初始化lame库，配置相关信息
     *
     * @param inSampleRate pcm格式音频采样率
     * @param outChannel pcm格式音频通道数量
     * @param outSampleRate mp3格式音频采样率
     * @param outBitRate mp3格式音频比特率
     * @param quality mp3格式音频质量，0~9，最慢最差~最快最好
     */
    external fun lameInit(
        inSampleRate: Int,
        outChannel: Int,
        outSampleRate: Int,
        outBitRate: Int,
        quality: Int
    )

    /** 编码pcm成mp3格式
     *
     * @param leftBuf  左pcm数据
     * @param rightBuf 右pcm数据，如果是单声道，则一致
     * @param sampleRate 读入的pcm字节大小
     * @param mp3Buf 存放mp3数据缓存
     * @return 编码数据字节长度
     */
    external fun lameEncode(
        leftBuf: ShortArray?,
        rightBuf: ShortArray?,
        sampleRate: Int,
        mp3Buf: ByteArray?
    )

    /** 保存mp3音频流到文件
     *
     * @param mp3buf mp3数据流
     * @return 数据流长度rty
     */
    external fun lameFlush(mp3buf: ByteArray?)

    /**
     * 释放lame库资源
     */
    external fun lameClose()
}