/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.kotlin.examples.webclient.standalone

import io.helidon.common.http.Http
import io.helidon.common.reactive.Single
import io.helidon.config.Config
import io.helidon.media.jsonp.JsonpSupport
import io.helidon.webclient.WebClient
import io.helidon.webclient.WebClientServiceRequest
import io.helidon.webclient.WebClientServiceResponse
import io.helidon.webclient.spi.WebClientService
import io.helidon.webserver.WebServer
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ExecutionException
import org.hamcrest.Matchers.`is` as Is

/**
 * Test for verification of WebClient example.
 */
class ClientMainTest {
    private lateinit var webClient: WebClient
    private lateinit var testFile: Path

    @BeforeEach
    @Throws(ExecutionException::class, InterruptedException::class)
    fun beforeEach() {
        testFile = Paths.get("test.txt")
        startServer()
            .thenAccept { webServer: WebServer -> createWebClient(webServer.port()) }
            .toCompletableFuture()
            .get()
    }

    @AfterEach
    @Throws(IOException::class)
    fun afterEach() {
        Files.deleteIfExists(testFile)
    }

    private fun createWebClient(port: Int, vararg services: WebClientService) {
        val config = Config.create()
        val builder = WebClient.builder()
            .baseUri("http://localhost:$port/greet")
            .config(config["client"])
            .addMediaSupport(JsonpSupport.create())
        for (service in services) {
            builder.addService(service)
        }
        webClient = builder.build()
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testPerformPutAndGetMethod() {
        performGetMethod(webClient)
            .thenAccept { MatcherAssert.assertThat(it, Is("{\"message\":\"Hello World!\"}")) }
            .thenCompose { performPutMethod(webClient) }
            .thenCompose { performGetMethod(webClient) }
            .thenAccept { MatcherAssert.assertThat(it, Is("{\"message\":\"Hola World!\"}")) }
            .toCompletableFuture()
            .get()
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testPerformRedirect() {
        createWebClient(serverPort, RedirectClientServiceTest())
        followRedirects(webClient)
            .thenAccept { MatcherAssert.assertThat(it, Is("{\"message\":\"Hello World!\"}")) }
            .toCompletableFuture()
            .get()
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun testFileDownload() {
        saveResponseToFile(webClient)
            .thenAccept { MatcherAssert.assertThat(Files.exists(testFile), Is(true)) }
            .thenAccept {
                try {
                    MatcherAssert.assertThat(Files.readString(testFile), Is("{\"message\":\"Hello World!\"}"))
                } catch (e: IOException) {
                    Assertions.fail<Any>(e)
                }
            }
            .toCompletableFuture()
            .get()
    }


    private class RedirectClientServiceTest : WebClientService {
        private val redirect = false
        override fun request(request: WebClientServiceRequest): Single<WebClientServiceRequest> {
            request.whenComplete()
                .thenAccept { response: WebClientServiceResponse ->
                    if (response.status() === Http.Status.MOVED_PERMANENTLY_301 && redirect) {
                        Assertions.fail<Any>("Received second redirect! Only one redirect expected here.")
                    } else if (response.status() === Http.Status.OK_200 && !redirect) {
                        Assertions.fail<Any>("There was status 200 without status 301 before it.")
                    }
                }
            return Single.just(request)
        }
    }
}