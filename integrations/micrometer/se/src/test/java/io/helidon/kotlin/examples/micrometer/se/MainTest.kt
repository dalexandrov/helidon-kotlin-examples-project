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
 *
 */
package io.helidon.kotlin.examples.micrometer.se

import io.helidon.media.jsonp.JsonpSupport
import io.helidon.webclient.WebClient
import io.helidon.webclient.WebClientResponse
import io.helidon.webserver.WebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import javax.json.Json
import javax.json.JsonObject

class MainTest {
    companion object {
        private lateinit var webServer: WebServer
        private lateinit var webClient: WebClient
        private val JSON_BF = Json.createBuilderFactory(emptyMap<String, Any>())
        private var TEST_JSON_OBJECT: JsonObject = JSON_BF.createObjectBuilder()
            .add("greeting", "Hola")
            .build()

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

    }

    @Test
    @Throws(Exception::class)
    fun testHelloWorld() {
        webClient.get()
            .path("/greet")
            .request(JsonObject::class.java)
            .thenAccept { jsonObject: JsonObject ->
                Assertions.assertEquals(
                    "Hello World!",
                    jsonObject.getString("greeting")
                )
            }
            .toCompletableFuture()
            .get()
        webClient.get()
            .path("/greet/Joe")
            .request(JsonObject::class.java)
            .thenAccept { jsonObject: JsonObject ->
                Assertions.assertEquals(
                    "Hello Joe!",
                    jsonObject.getString("greeting")
                )
            }
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
            .thenAccept { jsonObject: JsonObject ->
                Assertions.assertEquals(
                    "Hola Joe!",
                    jsonObject.getString("greeting")
                )
            }
            .toCompletableFuture()
            .get()
        webClient.get()
            .path("/micrometer")
            .request()
            .thenAccept { response: WebClientResponse ->
                Assertions.assertEquals(
                    200, response.status()
                        .code()
                )
                try {
                    val output = response.content()
                        .`as`(String::class.java)
                        .get()
                    Assertions.assertTrue(
                        output.contains("get_seconds_count 2.0"),
                        "Unable to find expected all-gets timer count 2.0"
                    ) // 2 gets; the put is not counted
                    Assertions.assertTrue(
                        output.contains("get_seconds_sum"),
                        "Unable to find expected all-gets timer sum"
                    )
                    Assertions.assertTrue(
                        output.contains("personalizedGets_total 1.0"),
                        "Unable to find expected counter result 1.0"
                    )
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                } catch (e: ExecutionException) {
                    throw RuntimeException(e)
                }
                response.close()
            }
    }
}