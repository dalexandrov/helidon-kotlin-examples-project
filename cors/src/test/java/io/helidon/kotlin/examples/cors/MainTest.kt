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
package io.helidon.kotlin.examples.cors

import io.helidon.common.http.Headers
import io.helidon.common.http.MediaType
import io.helidon.config.Config
import io.helidon.kotlin.examples.cors.GreetingMessage.Companion.fromRest
import io.helidon.media.jsonp.JsonpSupport
import io.helidon.webclient.WebClient
import io.helidon.webclient.WebClientRequestBuilder
import io.helidon.webclient.WebClientResponse
import io.helidon.webserver.WebServer
import io.helidon.webserver.cors.CrossOriginConfig
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import single
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import javax.json.JsonObject

@TestMethodOrder(OrderAnnotation::class)
class MainTest {
    @Order(1) // Make sure this runs before the greeting message changes so responses are deterministic.
    @Test
    @Throws(Exception::class)
    fun testHelloWorld() {
        var r = getResponse("/greet")
        Assertions.assertEquals(200, r.status().code(), "HTTP response1")
        Assertions.assertEquals(
            "Hello World!", fromPayload(r).message,
            "default message"
        )
        r = getResponse("/greet/Joe")
        Assertions.assertEquals(200, r.status().code(), "HTTP response2")
        Assertions.assertEquals(
            "Hello Joe!", fromPayload(r).message,
            "hello Joe message"
        )
        r = putResponse("/greet/greeting", GreetingMessage("Hola"))
        Assertions.assertEquals(204, r.status().code(), "HTTP response3")
        r = getResponse("/greet/Jose")
        Assertions.assertEquals(200, r.status().code(), "HTTP response4")
        Assertions.assertEquals(
            "Hola Jose!", fromPayload(r).message,
            "hola Jose message"
        )
        r = getResponse("/health")
        Assertions.assertEquals(200, r.status().code(), "HTTP response2")
        r = getResponse("/metrics")
        Assertions.assertEquals(200, r.status().code(), "HTTP response2")
    }

    @Order(10) // Run after the non-CORS tests (so the greeting is Hola) but before the CORS test that changes the greeting again.
    @Test
    @Throws(Exception::class)
    fun testAnonymousGreetWithCors() {
        val builder = webClient.get()
        var headers: Headers = builder.headers()
        headers.add("Origin", "http://foo.com")
        headers.add("Host", "here.com")
        val r = getResponse("/greet", builder)
        Assertions.assertEquals(200, r.status().code(), "HTTP response")
        val payload = fromPayload(r).message
        Assertions.assertTrue(payload.contains("Hola World"), "HTTP response payload was $payload")
        headers = r.headers()
        val allowOrigin = headers.value(CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN)
        Assertions.assertTrue(
            allowOrigin.isPresent,
            "Expected CORS header " + CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN + " is absent"
        )
        Assertions.assertEquals(allowOrigin.get(), "*")
    }

    @Order(11) // Run after the non-CORS tests but before other CORS tests.
    @Test
    @Throws(Exception::class)
    fun testGreetingChangeWithCors() {

        // Send the pre-flight request and check the response.
        var builder = webClient.options()
        var headers: Headers = builder.headers()
        headers.add("Origin", "http://foo.com")
        headers.add("Host", "here.com")
        headers.add("Access-Control-Request-Method", "PUT")
        var r = builder.path("/greet/greeting")
            .submit()
            .toCompletableFuture()
            .get()
        val preflightResponseHeaders: Headers = r.headers()
        val allowMethods = preflightResponseHeaders.values(CrossOriginConfig.ACCESS_CONTROL_ALLOW_METHODS)
        Assertions.assertFalse(
            allowMethods.isEmpty(),
            "pre-flight response does not include " + CrossOriginConfig.ACCESS_CONTROL_ALLOW_METHODS
        )
        Assertions.assertTrue(allowMethods.contains("PUT"))
        var allowOrigins = preflightResponseHeaders.values(CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN)
        Assertions.assertFalse(
            allowOrigins.isEmpty(),
            "pre-flight response does not include " + CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN
        )
        Assertions.assertTrue(
            allowOrigins.contains("http://foo.com"), "Header " + CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN
                    + " should contain '*' but does not; " + allowOrigins
        )

        // Send the follow-up request.
        builder = webClient.put()
        headers = builder.headers()
        headers.add("Origin", "http://foo.com")
        headers.add("Host", "here.com")
        headers.addAll(preflightResponseHeaders)
        r = putResponse("/greet/greeting", GreetingMessage("Cheers"), builder)
        Assertions.assertEquals(204, r.status().code(), "HTTP response3")
        headers = r.headers()
        allowOrigins = headers.values(CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN)
        Assertions.assertFalse(
            allowOrigins.isEmpty(),
            "Expected CORS header " + CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN + " has no value(s)"
        )
        Assertions.assertTrue(
            allowOrigins.contains("http://foo.com"), "Header " + CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN
                    + " should contain '*' but does not; " + allowOrigins
        )
    }

    @Order(12) // Run after CORS test changes greeting to Cheers.
    @Test
    @Throws(Exception::class)
    fun testNamedGreetWithCors() {
        val builder = webClient.get()
        var headers: Headers = builder.headers()
        headers.add("Origin", "http://foo.com")
        headers.add("Host", "here.com")
        val r = getResponse("/greet/Maria", builder)
        Assertions.assertEquals(200, r.status().code(), "HTTP response")
        Assertions.assertTrue(fromPayload(r).message.contains("Cheers Maria"))
        headers = r.headers()
        val allowOrigin = headers.value(CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN)
        Assertions.assertTrue(
            allowOrigin.isPresent,
            "Expected CORS header " + CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN + " is absent"
        )
        Assertions.assertEquals(allowOrigin.get(), "*")
    }

    @Order(100) // After all other tests so we can rely on deterministic greetings.
    @Test
    @Throws(Exception::class)
    fun testGreetingChangeWithCorsAndOtherOrigin() {
        val builder = webClient.put()
        val headers: Headers = builder.headers()
        headers.add("Origin", "http://other.com")
        headers.add("Host", "here.com")
        val r = putResponse("/greet/greeting", GreetingMessage("Ahoy"), builder)
        // Result depends on whether we are using overrides or not.
        val isOverriding = Config.create()["cors"].exists()
        Assertions.assertEquals(if (isOverriding) 204 else 403, r.status().code(), "HTTP response3")
    }

    companion object {
        private lateinit var webServer: WebServer
        private lateinit var webClient: WebClient

        @BeforeAll
        @JvmStatic
        @Throws(Exception::class)
        fun start() {
            // the port is only available if the server started already!
            // so we need to wait
            webServer = startServer()
                .start()
                .toCompletableFuture()
                .get()
            WebClient.builder()
                .baseUri("http://localhost:" + webServer.port())
                .addMediaSupport(JsonpSupport.create())
                .build().also { webClient = it }
            val timeout: Long = 2000 // 2 seconds should be enough to start the server
            val now = System.currentTimeMillis()
            while (!webServer.isRunning) {
                Thread.sleep(100)
                if (System.currentTimeMillis() - now > timeout) {
                    Assertions.fail<Any>("Failed to start webserver")
                }
            }
        }

        @AfterAll
        @JvmStatic
        @Throws(Exception::class)
        fun stop() {
            webServer.shutdown()
                .toCompletableFuture()[10, TimeUnit.SECONDS]
        }

        @Throws(ExecutionException::class, InterruptedException::class)
        private fun getResponse(path: String): WebClientResponse {
            return getResponse(path, webClient.get())
        }

        @Throws(ExecutionException::class, InterruptedException::class)
        private fun getResponse(path: String, builder: WebClientRequestBuilder): WebClientResponse {
            return builder
                .accept(MediaType.APPLICATION_JSON)
                .path(path)
                .submit()
                .toCompletableFuture()
                .get()
        }

        @Throws(ExecutionException::class, InterruptedException::class)
        private fun putResponse(
            path: String,
            payload: GreetingMessage,
            builder: WebClientRequestBuilder = webClient.put()
        ): WebClientResponse {
            return builder
                .accept(MediaType.APPLICATION_JSON)
                .path(path)
                .submit(payload.forRest())
                .toCompletableFuture()
                .get()
        }

        @Throws(ExecutionException::class, InterruptedException::class)
        private fun fromPayload(response: WebClientResponse): GreetingMessage {
            val json = response
                .content()
                .single<JsonObject>()
                .toCompletableFuture()
                .get()
            return fromRest(json)
        }
    }
}