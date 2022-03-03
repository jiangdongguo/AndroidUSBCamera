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
 *
 * Lame for mp3
 *
 * @author Created by jiangdg on 2022/2/18
 */
#include "mp3.h"
#include "lame/lame.h"
#include "../../utils/logger.h"

static lame_global_flags *gfp = nullptr;

void lameInitInternal(int inSampleRate, int outChannel, int outSampleRate, int outBitRate, int quality) {
    if(gfp != nullptr){
        lameCloseInternal();
    }
    gfp = lame_init();
    LOGI("init lame library success!");
    lame_set_in_samplerate(gfp,inSampleRate);
    lame_set_num_channels(gfp,outChannel);
    lame_set_out_samplerate(gfp,outSampleRate);
    lame_set_brate(gfp,outBitRate);
    lame_set_quality(gfp,quality);
    lame_init_params(gfp);
    LOGI("config lame library success!");
}

int lameEncodeInternal(short* leftBuf, short* rightBuf, int sampleRate, unsigned char* mp3Buf, int len) {
    int ret = lame_encode_buffer(gfp,leftBuf,rightBuf,sampleRate,mp3Buf,len);
    if (ret < 0) {
        LOG_E("encode pcm data failed, err = %d",ret);
    }
    return ret;
}

int lameFlushInternal(unsigned char* mp3Buf, int len) {
    int ret = lame_encode_flush(gfp,mp3Buf,len);
    if (ret <= 0) {
        LOG_E("flush lame failed, err = %d", ret);
    }
    return ret;
}

void lameCloseInternal() {
    lame_close(gfp);
    gfp = nullptr;
    LOGI("close lame success!");
}