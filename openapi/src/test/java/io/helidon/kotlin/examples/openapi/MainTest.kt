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
package io.helidon.kotlin.examples.openapi

import io.helidon.common.http.MediaType
import io.helidon.kotlin.examples.openapi.Main.startServer
import io.helidon.kotlin.examples.openapi.internal.SimpleAPIModelReader
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
import javax.json.JsonString

class MainTest {
    companion object {
        private lateinit var webServer: WebServer
        private lateinit var webClient: WebClient
        private val JSON_BF = Json.createBuilderFactory(emptyMap<String, Any>())
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

        private fun escape(path: String): String {
            return path.replace("/", "~1")
        }

        init {
            TEST_JSON_OBJECT = JSON_BF.createObjectBuilder()
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
                .thenAccept { jsonObject: JsonObject -> Assertions.assertEquals("Hello World!", jsonObject.getString("greeting")) }
                .toCompletableFuture()
                .get()
        webClient.get()
                .path("/greet/Joe")
                .request(JsonObject::class.java)
                .thenAccept { jsonObject: JsonObject -> Assertions.assertEquals("Hello Joe!", jsonObject.getString("greeting")) }
                .toCompletableFuture()
                .get()
        webClient.put()
                .path("/greet/greeting")
                .submit(TEST_JSON_OBJECT)
                .thenAccept { response: WebClientResponse -> Assertions.assertEquals(204, response.status().code()) }
                .thenCompose { nothing: Void? ->
                    webClient.get()
                            .path("/greet/Joe")
                            .request(JsonObject::class.java)
                }
                .thenAccept { jsonObject: JsonObject -> Assertions.assertEquals("Hola Joe!", jsonObject.getString("greeting")) }
                .toCompletableFuture()
                .get()
        webClient.get()
                .path("/health")
                .request()
                .thenAccept { response: WebClientResponse ->
                    Assertions.assertEquals(200, response.status().code())
                    response.close()
                }
                .toCompletableFuture()
                .get()
        webClient.get()
                .path("/metrics")
                .request()
                .thenAccept { response: WebClientResponse ->
                    Assertions.assertEquals(200, response.status().code())
                    response.close()
                }
                .toCompletableFuture()
                .get()
    }

    @Test
    @Throws(Exception::class)
    fun testOpenAPI() {
        /*
         * If you change the OpenAPI endpoing path in application.yaml, then
         * change the following path also.
         */
        val jsonObject = webClient.get()
                .accept(MediaType.APPLICATION_JSON)
                .path("/openapi")
                .request(JsonObject::class.java)
                .toCompletableFuture()
                .get()
        val paths = jsonObject.getJsonObject("paths")
        var jp = Json.createPointer("/" + escape("/greet/greeting") + "/put/summary")
        var js = JsonString::class.java.cast(jp.getValue(paths))
        Assertions.assertEquals("Set the greeting prefix", js.string, "/greet/greeting.put.summary not as expected")
        jp = Json.createPointer("/" + escape(SimpleAPIModelReader.MODEL_READER_PATH)
                + "/get/summary")
        js = JsonString::class.java.cast(jp.getValue(paths))
        Assertions.assertEquals(SimpleAPIModelReader.SUMMARY, js.string,
                "summary added by model reader does not match")
        jp = Json.createPointer("/" + escape(SimpleAPIModelReader.DOOMED_PATH))
        Assertions.assertFalse(jp.containsValue(paths), "/test/doomed should not appear but does")
    }
}