/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
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
import io.helidon.webserver.ServerResponse
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.concurrent.Flow
import java.util.logging.Logger

/**
 * Class ServerFileWriter. Process data chunks from a `Producer` and
 * writes them to a temporary file using NIO. For simplicity, this `Subscriber` requests an unbounded number of chunks on its subscription.
 */
class ServerFileWriter internal constructor(private val response: ServerResponse) : Flow.Subscriber<DataChunk> {
    private var channel: FileChannel? = null
    override fun onSubscribe(subscription: Flow.Subscription) {
        subscription.request(Long.MAX_VALUE)
    }

    override fun onNext(chunk: DataChunk) {
        try {
            channel!!.write(chunk.data())
            LOGGER.info(chunk.data().toString() + " " + Thread.currentThread())
            chunk.release()
        } catch (e: IOException) {
            LOGGER.info(e.message)
        }
    }

    override fun onError(throwable: Throwable) {
        throwable.printStackTrace()
    }

    override fun onComplete() {
        try {
            channel!!.close()
            response.send("DONE")
        } catch (e: IOException) {
            LOGGER.info(e.message)
        }
    }

    companion object {
        private val LOGGER = Logger.getLogger(ServerFileWriter::class.java.name)
    }

    init {
        channel = try {
            val tempFilePath = Files.createTempFile("large-file", ".tmp")
            FileChannel.open(tempFilePath, StandardOpenOption.WRITE)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}