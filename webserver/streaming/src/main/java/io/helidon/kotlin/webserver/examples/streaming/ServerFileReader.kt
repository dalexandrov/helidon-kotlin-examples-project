/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.webserver.examples.streaming

import io.helidon.common.http.DataChunk
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.Flow
import java.util.logging.Logger

/**
 * Class ServerFileReader. Reads a file using NIO and produces data chunks for a
 * `Subscriber` to process.
 */
class ServerFileReader internal constructor(private val path: Path) : Flow.Publisher<DataChunk?> {
    override fun subscribe(s: Flow.Subscriber<in DataChunk?>) {
        val buffer = ByteBuffer.allocate(BUFFER_SIZE)
        val channel: FileChannel = try {
            FileChannel.open(path, StandardOpenOption.READ)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        s.onSubscribe(object : Flow.Subscription {
            override fun request(inn: Long) {
                var n = inn
                try {
                    while (n > 0) {
                        val bytes = channel.read(buffer)
                        if (bytes < 0) {
                            s.onComplete()
                            channel.close()
                            return
                        }
                        if (bytes > 0) {
                            LOGGER.info(buffer.toString())
                            buffer.flip()
                            s.onNext(DataChunk.create(buffer))
                            n--
                        }
                        buffer.rewind()
                    }
                } catch (e: IOException) {
                    s.onError(e)
                }
            }

            override fun cancel() {
                try {
                    channel.close()
                } catch (e: IOException) {
                    LOGGER.info(e.message)
                }
            }
        })
    }

    companion object {
        private val LOGGER = Logger.getLogger(ServerFileReader::class.java.name)
        const val BUFFER_SIZE = 4096
    }
}