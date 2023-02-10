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
package com.jiangdg.ausbc.utils

import java.util.concurrent.*
import java.util.concurrent.locks.AbstractQueuedSynchronizer

/** Sync read tool
 *
 * @author Created by jiangdg on 2021/12/21
 */
open class SettableFuture<V> : Future<V> {

    private val sync: Sync<V> = Sync()

    // 设置结果，解除线程同步等待。
    fun set(value: V?): Boolean {
        return sync.set(value)
    }

    override fun isDone(): Boolean {
        return sync.isDone()
    }

    // 获取异步结果，如果结果还没有计算出来，
    // 则进入同步等待状态。
    override fun get(): V? {
        return sync.get()
    }

    // 获取异步结果，如果结果还没有计算出来，
    // 则进入同步等待状态，一段时间之后超时。
    override fun get(timeout: Long, unit: TimeUnit): V? {
        return sync[unit.toNanos(timeout)]
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        return sync.cancel(mayInterruptIfRunning)
    }

    override fun isCancelled(): Boolean {
        return sync.isCancelled()
    }

    private class Sync<V> : AbstractQueuedSynchronizer() {
        private var value: V? = null
        private var exception: Throwable? = null

        fun isDone(): Boolean {
            return state and (COMPLETED or CANCELLED or INTERRUPTED) != 0
        }

        fun isCancelled(): Boolean {
            return state and (CANCELLED or INTERRUPTED) != 0
        }

        override fun tryAcquireShared(ignored: Int): Int {
            return if (isDone()) {
                1
            } else {
                -1
            }
        }

        override fun tryReleaseShared(finalState: Int): Boolean {
            state = finalState
            return true
        }

        operator fun get(nanos: Long): V? {
            if (!tryAcquireSharedNanos(-1, nanos)) {
                throw TimeoutException("Timeout waiting for task.")
            }
            return getValue()
        }

        fun get(): V? {
            acquireSharedInterruptibly(-1)
            return getValue()
        }

        private fun getValue(): V? {
            when (state) {
                COMPLETED -> return if (exception != null) {
                    throw ExecutionException(exception)
                } else {
                    value
                }
                CANCELLED, INTERRUPTED -> throw cancellationExceptionWithCause(
                    "Task was cancelled.",
                    exception
                )
                else -> throw IllegalStateException("Error, synchronizer in invalid state: $state")
            }
        }

        private fun cancellationExceptionWithCause(
            message: String?,
            cause: Throwable?
        ): CancellationException {
            val exception = CancellationException(message)
            exception.initCause(cause)
            return exception
        }

        fun wasInterrupted(): Boolean {
            return state == INTERRUPTED
        }

        fun set(v: V?): Boolean {
            return complete(v, null, COMPLETED)
        }

        fun setException(t: Throwable): Boolean {
            return complete(null, t, COMPLETED)
        }

        fun cancel(interrupt: Boolean): Boolean {
            return complete(null, null, if (interrupt) INTERRUPTED else CANCELLED)
        }

        private fun complete(v: V?, t: Throwable?, finalState: Int): Boolean {
            val doCompletion = compareAndSetState(RUNNING, COMPLETING)
            if (doCompletion) {
                this.value = v
                this.exception = if (finalState and (CANCELLED or INTERRUPTED) != 0) {
                    CancellationException("Future.cancel() was called.")
                } else {
                    t
                }
                releaseShared(finalState)
            } else if (state == COMPLETING) {
                acquireShared(-1)
            }
            return doCompletion
        }

        companion object {
            private const val RUNNING: Int = 0
            private const val COMPLETING: Int = 1
            private const val COMPLETED: Int = 2
            private const val CANCELLED: Int = 4
            private const val INTERRUPTED: Int = 8
        }
    }

}