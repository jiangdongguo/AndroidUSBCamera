/*
 * Copyright 2017-2023 Jiangdg
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
package com.jiangdg.ausbc.pusher.aliyun

import android.content.Context
import com.jiangdg.ausbc.pusher.IPusher
import com.jiangdg.ausbc.pusher.callback.IStateCallback
import com.jiangdg.ausbc.pusher.config.AusbcConfig

/** Your self pusher engine
 *
 * @author Created by jiangdg on 2023/1/29
 */
class AliyunPusher: IPusher {
    override fun init(context: Context?, ausbcConfig: AusbcConfig?, callback: IStateCallback?) {
        TODO("Not yet implemented")
    }

    override fun start(url: String?) {
        TODO("Not yet implemented")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

    override fun resume() {
        TODO("Not yet implemented")
    }

    override fun reconnect() {
        TODO("Not yet implemented")
    }

    override fun reconnectUrl(url: String?) {
        TODO("Not yet implemented")
    }

    override fun pushStream(type: Int, data: ByteArray?, size: Int, pts: Long) {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }

    override fun isPushing(): Boolean {
        TODO("Not yet implemented")
    }
}