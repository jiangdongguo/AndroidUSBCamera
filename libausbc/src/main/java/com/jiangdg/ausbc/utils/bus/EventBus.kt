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
package com.jiangdg.ausbc.utils.bus

import androidx.annotation.MainThread
import androidx.lifecycle.*
import java.util.concurrent.ConcurrentHashMap

/** Event bus with using [LiveData]
 *
 * @author Created by jiangdg on 2022/3/15
 */
object EventBus {
    private val mLiveDataMap = ConcurrentHashMap<String, LiveData<*>>()

    /** Register the event bus
     *
     * key represents the event bus name
     * T represents the event type, generic
     *
     * @param key bus name
     * @return [BusLiveData], data type can any object
     */
    fun <T> with(key: String): BusLiveData<T> {
        var liveData = mLiveDataMap[key] as? BusLiveData<T>
        if (liveData == null) {
            liveData = BusLiveData<T>(key).apply {
                mLiveDataMap[key] = this
            }
        }
        return liveData
    }

    /** Customize LiveData
     *
     * By managing the version of LiveData by yourself, it is convenient for subsequent
     * synchronization of the version field of the Observer
     */
    class BusLiveData<T>(private val busName: String): MutableLiveData<T>() {
        internal var mVersion = 0

        /**
         * Send message to event bus,only used in UI thread
         *
         * @param message event, can be any object
         */
        @MainThread
        fun sendMessage(message: T) {
            ++mVersion
            value = message
        }

        /**
         * Send message to event bus, be used in any thread
         *
         * @param message event, can be any object
         */
        fun postMessage(message: T) {
            ++mVersion
            postValue(message)
        }

        override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
            // Listen for the destruction event of the host, and check whether there are other observers in LiveData
            // If the LiveData is not removed actively
            owner.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        if (mLiveDataMap[busName]?.hasObservers() == false) {
                            mLiveDataMap.remove(busName)
                        }
                    }
                }
            })
            // Repackage the observer
            // use a proxy observer, which will only dispatch the latest events to the observer
            super.observe(owner, ProxyObserver(this, observer))
        }
    }

    /** Proxy Observer
     *
     * Control whether to distribute events by managing the version field of the Observer yourself
     */
    internal class ProxyObserver<T>(
        private val liveData: BusLiveData<T>,
        private val observer: Observer<in T>
    ): Observer<T> {
        // Initialize the version of Observer to be consistent with that of LiveData
        private var mLastVersion = liveData.mVersion

        // Only when LiveData has the latest data, the onChanged dispatch event of the Observer is called
        // Among them, when judging the condition of new data: LiveData.version > Observer.version
        override fun onChanged(data: T) {
            if (mLastVersion >= liveData.mVersion) {
                return
            }
            mLastVersion = liveData.mVersion
            observer.onChanged(data)
        }
    }
}