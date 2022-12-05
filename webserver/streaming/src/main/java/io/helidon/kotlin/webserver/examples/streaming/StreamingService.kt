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

import io.helidon.webserver.*
import java.net.URISyntaxException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.logging.Logger

/**
 * StreamingService class. Uses a `Subscriber<RequestChunk>` and a
 * `Publisher<ResponseChunk>` for uploading and downloading files.
 */
class StreamingService internal constructor() : Service {
    private var filePath: Path? = null
    override fun update(routingRules: Routing.Rules) {
        routingRules["/download", Handler { _: ServerRequest, response: ServerResponse -> download(response) }]
                .post("/upload", Handler { request: ServerRequest, response: ServerResponse -> upload(request, response) })
    }

    private fun upload(request: ServerRequest, response: ServerResponse) {
        LOGGER.info("Entering upload ... " + Thread.currentThread())
        request.content().subscribe(ServerFileWriter(response))
        LOGGER.info("Exiting upload ...")
    }

    private fun download(response: ServerResponse) {
        LOGGER.info("Entering download ..." + Thread.currentThread())
        val length = filePath!!.toFile().length()
        response.headers().add("Content-Length", length.toString())
        response.send(ServerFileReader(filePath!!))
        LOGGER.info("Exiting download ...")
    }

    companion object {
        private val LOGGER = Logger.getLogger(StreamingService::class.java.name)
    }

    init {
        filePath = try {
            Paths.get(javaClass.getResource(
                LARGE_FILE_RESOURCE).toURI())
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }
}