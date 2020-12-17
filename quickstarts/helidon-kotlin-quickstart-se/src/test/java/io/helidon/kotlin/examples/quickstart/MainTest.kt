/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
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
package io.helidon.kotlin.examples.quickstart

import io.helidon.kotlin.examples.quickstart.se.startServer
import io.helidon.media.jsonp.JsonpSupport
import io.helidon.webclient.WebClient
import io.helidon.webclient.WebClientResponse
import io.helidon.webserver.WebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import javax.json.Json
import javax.json.JsonObject

class MainTest {
    companion object {
        private lateinit var webServer: WebServer
        private lateinit var webClient: WebClient
        private var JSON_BUILDER = Json.createBuilderFactory(emptyMap<String, Any>())
        private var TEST_JSON_OBJECT: JsonObject? = null

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
                    .baseUri("http://localhost:" + webServer
                        .port())
                    .addMediaSupport(JsonpSupport.create())
                    .build()
        }

        @AfterAll
        @JvmStatic
        @Throws(Exception::class)
        fun stopServer() {
            webServer.shutdown()
                    .toCompletableFuture()[10, TimeUnit.SECONDS]
        }

        init {
            TEST_JSON_OBJECT = JSON_BUILDER.createObjectBuilder()
                    .add("greeting", "Hola")
                    .build()
        }
    }

    @Test
    @Throws(Exception::class)
    fun testHelloWorld() {
        webClient.get()
                .path("/greet")
                .request(JsonObject::class.java)
                .thenAccept { jsonObject: JsonObject -> Assertions.assertEquals("Hello World!", jsonObject.getString("message")) }
                .toCompletableFuture()
                .get()
        webClient.get()
                .path("/greet/Joe")
                .request(JsonObject::class.java)
                .thenAccept { jsonObject: JsonObject -> Assertions.assertEquals("Hello Joe!", jsonObject.getString("message")) }
                .toCompletableFuture()
                .get()
        webClient.put()
                .path("/greet/greeting")
                .submit(TEST_JSON_OBJECT)
                .thenAccept { response: WebClientResponse -> Assertions.assertEquals(204, response.status().code()) }
                .thenCompose {
                    webClient.get()
                            .path("/greet/Joe")
                            .request(JsonObject::class.java)
                }
                .thenAccept { jsonObject: JsonObject -> Assertions.assertEquals("Hola Joe!", jsonObject.getString("message")) }
                .toCompletableFuture()
                .get()
        webClient.get()
                .path("/health")
                .request()
                .thenAccept { response: WebClientResponse -> Assertions.assertEquals(200, response.status().code()) }
                .toCompletableFuture()
                .get()
        webClient.get()
                .path("/metrics")
                .request()
                .thenAccept { response: WebClientResponse -> Assertions.assertEquals(200, response.status().code()) }
                .toCompletableFuture()
                .get()
    }
}