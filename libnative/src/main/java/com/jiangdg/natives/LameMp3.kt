package com.jiangdg.natives

/** pcm to mp3
 *
 * @author Created by jiangdg on 2022/3/2
 */
object LameMp3 {

    init {
        System.loadLibrary("nativelib")
    }

    /** Initialize the lame library and configure related information
     *
     * @param inSampleRate pcm format audio sample rate
     * @param outChannel number of audio channels in pcm format
     * @param outSampleRate mp3 format audio sample rate
     * @param outBitRate mp3 format audio bit rate
     * @param quality mp3 format audio quality, 0~9, slowest and worst~fastest and best
     */
    external fun lameInit(
        inSampleRate: Int,
        outChannel: Int,
        outSampleRate: Int,
        outBitRate: Int,
        quality: Int
    )

    /** Encode pcm into mp3 format
     *
     * @param leftBuf left pcm data
     * @param rightBuf right pcm data, if it is mono, it is the same
     * @param sampleRate read in pcm byte size
     * @param mp3Buf store mp3 data buffer
     * @return encoded data byte length
     */
    external fun lameEncode(
        leftBuf: ShortArray?,
        rightBuf: ShortArray?,
        sampleRate: Int,
        mp3Buf: ByteArray?
    ):Int

    /** save mp3 audio stream to file
     *
     * @param mp3buf mp3 data stream
     * @return data stream length rty
     */
    external fun lameFlush(mp3buf: ByteArray?): Int

    /**
     * Release lame library resources
     */
    external fun lameClose()
}