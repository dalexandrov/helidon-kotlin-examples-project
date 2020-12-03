/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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
package io.helidon.kotlin.service.employee

import io.helidon.common.http.Http
import io.helidon.common.http.MediaType
import io.helidon.kotlin.service.employee.Main.startServer
import io.helidon.webclient.WebClient
import io.helidon.webclient.WebClientResponse
import io.helidon.webserver.WebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class MainTest {
    @Test
    @Throws(Exception::class)
    fun testHelloWorld() {
        webClient.get()
                .path("/employees")
                .request()
                .thenAccept { response: WebClientResponse ->
                    response.close()
                    Assertions.assertEquals(Http.Status.OK_200, response.status(), "HTTP response2")
                }
                .toCompletableFuture()
                .get()
        webClient.get()
                .path("/health")
                .request()
                .thenAccept { response: WebClientResponse ->
                    response.close()
                    Assertions.assertEquals(Http.Status.OK_200, response.status(), "HTTP response2")
                }
                .toCompletableFuture()
                .get()
        webClient.get()
                .path("/metrics")
                .request()
                .thenAccept { response: WebClientResponse ->
                    response.close()
                    Assertions.assertEquals(Http.Status.OK_200, response.status(), "HTTP response2")
                }
                .toCompletableFuture()
                .get()
    }

    companion object {
        private lateinit var webServer: WebServer
        private lateinit var webClient: WebClient
        @BeforeAll
        @JvmStatic
        @Throws(Exception::class)
        fun startTheServer() {
            webServer = startServer()
            val timeout: Long = 2000 // 2 seconds should be enough to start the server
            val now = System.currentTimeMillis()
            while (!webServer.isRunning) {
                Thread.sleep(100)
                if (System.currentTimeMillis() - now > timeout) {
                    Assertions.fail<Any>("Failed to start webserver")
                }
            }
            webClient = WebClient.builder()
                    .baseUri("http://localhost:" + webServer.port())
                    .addHeader(Http.Header.ACCEPT, MediaType.APPLICATION_JSON.toString())
                    .build()
        }

        @AfterAll
        @JvmStatic
        @Throws(Exception::class)
        fun stopServer() {
            webServer.shutdown()
                    .toCompletableFuture()[10, TimeUnit.SECONDS]
        }
    }
}