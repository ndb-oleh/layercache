/*
 * Copyright 2017 Appmattus Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appmattus.layercache

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.core.Is.isA
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CountDownLatch

class DeferredOnCancelShould {

    @Test
    fun `not call onCancel on success`() {
        runBlocking {
            // given we have a job that successfully returns a value
            val latch = CountDownLatch(1)
            val job = GlobalScope.async {
                "value"
            }

            // when we attach a listener and wait for the result
            job.onCancel { latch.countDown() }
            job.completeAndWait(250)

            // then onCancel is not called
            Assert.assertEquals(1, latch.count)
        }
    }

    @Test
    fun `call onCancel with exception on cancellation`() {
        runBlocking {
            // given we have a job that does nothing
            val latch = CountDownLatch(1)
            val job = GlobalScope.async {
                delay(500)
            }

            // when we attach a listener and wait for the result
            job.onCancel {
                if (it is CancellationException) {
                    latch.countDown()
                }
            }
            job.cancel()
            job.completeAndWait(250)

            // then onCancel is called supplying the exception
            Assert.assertEquals(0, latch.count)
        }
    }

    @Test
    fun `call onFailure with exception on failure`() {
        runBlocking {
            // given we have a job that throws an exception
            val latch = CountDownLatch(1)
            val job = GlobalScope.async {
                throw TestException()
            }

            // when we attach a listener and wait for the result
            job.onCancel {
                if (isA(TestException::class.java).matches(it)) {
                    latch.countDown()
                }
            }
            job.completeAndWait(250)

            // then onFailure is called supplying the exception
            Assert.assertEquals(0, latch.count)
        }
    }

    private suspend fun Deferred<*>.completeAndWait(time: Long) {
        join()
        // yield to allow parallel jobs time to complete
        delay(time)
    }
}
