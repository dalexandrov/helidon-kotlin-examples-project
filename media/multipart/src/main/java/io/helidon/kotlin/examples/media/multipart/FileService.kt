/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.kotlin.examples.media.multipart

import io.helidon.common.http.DataChunk
import io.helidon.common.http.Http
import io.helidon.common.http.MediaType
import io.helidon.common.reactive.Multi
import io.helidon.media.multipart.ContentDisposition
import io.helidon.media.multipart.ReadableBodyPart
import io.helidon.media.multipart.ReadableMultiPart
import io.helidon.webserver.*
import single
import to
import java.io.IOException
import java.nio.channels.ByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Map
import java.util.stream.Stream
import javax.json.Json
import javax.json.JsonBuilderFactory

/**
 * File service.
 */
class FileService internal constructor() : Service {
    private val jsonFactory: JsonBuilderFactory = Json.createBuilderFactory(Map.of<String, Any?>())
    private val storage: Path = createStorage()
    override fun update(rules: Routing.Rules) {
        rules["/", Handler { _: ServerRequest, res: ServerResponse -> list(res) }]["/{fname}", Handler { req: ServerRequest, res: ServerResponse -> download(req, res) }]
                .post("/", Handler { req: ServerRequest, res: ServerResponse -> upload(req, res) })
    }

    private fun list(res: ServerResponse) {
        val arrayBuilder = jsonFactory.createArrayBuilder()
        listFiles(storage).forEach { s: String? -> arrayBuilder.add(s) }
        res.send(jsonFactory.createObjectBuilder().add("files", arrayBuilder).build())
    }

    private fun download(req: ServerRequest, res: ServerResponse) {
        val filePath = storage.resolve(req.path().param("fname"))
        if (filePath.parent != storage) {
            res.status(Http.Status.BAD_REQUEST_400).send("Invalid file name")
            return
        }
        if (!Files.exists(filePath)) {
            res.status(Http.Status.NOT_FOUND_404).send()
            return
        }
        if (!Files.isRegularFile(filePath)) {
            res.status(Http.Status.BAD_REQUEST_400).send("Not a file")
            return
        }
        val headers = res.headers()
        headers.contentType(MediaType.APPLICATION_OCTET_STREAM)
        headers.put(Http.Header.CONTENT_DISPOSITION, ContentDisposition.builder()
                .filename(filePath.fileName.toString())
                .build()
                .toString())
        res.send(filePath)
    }

    private fun upload(req: ServerRequest, res: ServerResponse) {
        if (req.queryParams().first("stream").isPresent) {
            streamUpload(req, res)
        } else {
            bufferedUpload(req, res)
        }
    }

    private fun bufferedUpload(req: ServerRequest, res: ServerResponse) {
        req.content().single<ReadableMultiPart>().thenAccept { multiPart: ReadableMultiPart ->
            for (part in multiPart.fields("file[]")) {
                writeBytes(storage, part.filename(), part.to<ByteArray>())
            }
            res.status(Http.Status.MOVED_PERMANENTLY_301)
            res.headers().put(Http.Header.LOCATION, "/ui")
            res.send()
        }
    }

    private fun streamUpload(req: ServerRequest, res: ServerResponse) {
        req.content().asStream(ReadableBodyPart::class.java)
                .onError { throwable: Throwable? -> res.send(throwable) }
                .onComplete {
                    res.status(Http.Status.MOVED_PERMANENTLY_301)
                    res.headers().put(Http.Header.LOCATION, "/ui")
                    res.send()
                }.forEach { part: ReadableBodyPart ->
                    if ("file[]" == part.name()) {
                        val channel = newByteChannel(storage, part.filename())
                        Multi.create(part.content())
                                .forEach { chunk: DataChunk -> writeChunk(channel, chunk) }
                                .thenAccept { closeChannel(channel) }
                    }
                }
    }

    private fun closeChannel(channel: ByteChannel) {
        try {
            channel.close()
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    companion object {
        private fun createStorage(): Path {
            return try {
                Files.createTempDirectory("fileupload")
            } catch (ex: IOException) {
                throw RuntimeException(ex)
            }
        }

        private fun listFiles(storage: Path): Stream<String> {
            return try {
                Files.walk(storage)
                        .filter { path: Path -> Files.isRegularFile(path) }
                        .map { other: Path -> storage.relativize(other) }
                        .map { obj: Path -> obj.toString() }
            } catch (ex: IOException) {
                throw RuntimeException(ex)
            }
        }

        private fun writeBytes(storage: Path, fname: String, bytes: ByteArray) {
            try {
                Files.write(storage.resolve(fname), bytes,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING)
            } catch (ex: IOException) {
                throw RuntimeException(ex)
            }
        }

        private fun writeChunk(channel: ByteChannel, chunk: DataChunk) {
            try {
                for (byteBuffer in chunk.data()) {
                    channel.write(byteBuffer)
                }
            } catch (ex: IOException) {
                throw RuntimeException(ex)
            } finally {
                chunk.release()
            }
        }

        private fun newByteChannel(storage: Path, fname: String): ByteChannel {
            return try {
                Files.newByteChannel(storage.resolve(fname),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING)
            } catch (ex: IOException) {
                throw RuntimeException(ex)
            }
        }
    }

    /**
     * Create a new file upload service instance.
     */
    init {
        println("Storage: $storage")
    }
}