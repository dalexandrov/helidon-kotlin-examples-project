/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.webserver.examples.tutorial

import io.helidon.common.http.DataChunk
import io.helidon.common.reactive.Multi
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.Flow
import java.util.function.Function

/**
 * All 'x' must be upper case.
 *
 *
 * This is a naive implementation.
 */
class UpperXFilter : Function<Flow.Publisher<DataChunk>?, Flow.Publisher<DataChunk>> {
    override fun apply(publisher: Flow.Publisher<DataChunk>?): Flow.Publisher<DataChunk> {
        return Multi.create(publisher).map { responseChunk: DataChunk? ->
            if (responseChunk == null) {
                return@map null
            }
            try {
                val originalData = responseChunk.data()
                val processedData = arrayOfNulls<ByteBuffer>(originalData.size)
                for (i in originalData.indices) {
                    // Naive but works for demo
                    val buff = ByteArray(originalData[i].remaining())
                    originalData[i][buff]
                    for (j in buff.indices) {
                        if (buff[j] == LOWER_X) {
                            buff[j] = UPPER_X
                        }
                    }
                    processedData[i] = ByteBuffer.wrap(buff)
                }
                return@map DataChunk.create(responseChunk.flush(), *processedData)
            } finally {
                responseChunk.release()
            }
        }
    }

    companion object {
        private val CHARSET = StandardCharsets.US_ASCII
        private val LOWER_X = "x".toByteArray(CHARSET)[0]
        private val UPPER_X = "X".toByteArray(CHARSET)[0]
    }
}