/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.kotlin.microprofile.examples.cors

import io.helidon.common.http.Headers
import io.helidon.common.http.MediaType
import io.helidon.config.Config
import io.helidon.media.jsonp.JsonpSupport
import io.helidon.microprofile.server.Server
import io.helidon.webclient.WebClient
import io.helidon.webclient.WebClientRequestBuilder
import io.helidon.webclient.WebClientResponse
import io.helidon.webserver.cors.CrossOriginConfig
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import java.util.concurrent.ExecutionException
import javax.json.Json
import javax.json.JsonObject

@TestMethodOrder(OrderAnnotation::class)
class TestCORS {
    @Order(1) // Make sure this runs before the greeting message changes so responses are deterministic.
    @Test
    @Throws(Exception::class)
    fun testHelloWorld() {
        var r = getResponse("/greet")
        MatcherAssert.assertThat("HTTP response1", r.status().code(), Matchers.`is`(200))
        MatcherAssert.assertThat("default message", fromPayload(r), Matchers.`is`("Hello World!"))
        r = getResponse("/greet/Joe")
        MatcherAssert.assertThat("HTTP response2", r.status().code(), Matchers.`is`(200))
        MatcherAssert.assertThat("Hello Joe message", fromPayload(r), Matchers.`is`("Hello Joe!"))
        r = putResponse("/greet/greeting", "Hola")
        MatcherAssert.assertThat("HTTP response3", r.status().code(), Matchers.`is`(204))
        r = getResponse("/greet/Jose")
        MatcherAssert.assertThat("HTTP response4", r.status().code(), Matchers.`is`(200))
        MatcherAssert.assertThat("Hola Jose message", fromPayload(r), Matchers.`is`("Hola Jose!"))
        r = getResponse("/health")
        MatcherAssert.assertThat("HTTP response health", r.status().code(), Matchers.`is`(200))
        r = getResponse("/metrics")
        MatcherAssert.assertThat("HTTP response metrics", r.status().code(), Matchers.`is`(200))
    }

    @Order(10) // Run after the non-CORS tests (so the greeting is Hola) but before the CORS test that changes the greeting again.
    @Test
    @Throws(Exception::class)
    fun testAnonymousGreetWithCors() {
        val builder = client.get()
        var headers: Headers = builder.headers()
        headers.add("Origin", "http://foo.com")
        headers.add("Host", "here.com")
        val r = getResponse("/greet", builder)
        MatcherAssert.assertThat("HTTP response", r.status().code(), Matchers.`is`(200))
        val payload = fromPayload(r)
        MatcherAssert.assertThat("HTTP response payload", payload, Matchers.`is`("Hola World!"))
        headers = r.headers()
        val allowOrigin = headers.value(CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN)
        MatcherAssert.assertThat("Expected CORS header " + CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN + " is absent",
                allowOrigin.isPresent, Matchers.`is`(true))
        MatcherAssert.assertThat("CORS header " + CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN, allowOrigin.get(), Matchers.`is`("*"))
    }

    @Order(11) // Run after the non-CORS tests but before other CORS tests.
    @Test
    @Throws(Exception::class)
    fun testGreetingChangeWithCors() {

        // Send the pre-flight request and check the response.
        var builder = client.method("OPTIONS")
        var headers: Headers = builder.headers()
        headers.add("Origin", "http://foo.com")
        headers.add("Host", "here.com")
        headers.add("Access-Control-Request-Method", "PUT")
        var r = builder.path("/greet/greeting")
                .submit()
                .toCompletableFuture()
                .get()
        MatcherAssert.assertThat("pre-flight status", r.status().code(), Matchers.`is`(200))
        val preflightResponseHeaders: Headers = r.headers()
        val allowMethods = preflightResponseHeaders.values(CrossOriginConfig.ACCESS_CONTROL_ALLOW_METHODS)
        MatcherAssert.assertThat("pre-flight response check for " + CrossOriginConfig.ACCESS_CONTROL_ALLOW_METHODS,
                allowMethods, Matchers.`is`(Matchers.not(Matchers.empty())))
        MatcherAssert.assertThat("Header " + CrossOriginConfig.ACCESS_CONTROL_ALLOW_METHODS, allowMethods, Matchers.contains("PUT"))
        var allowOrigins = preflightResponseHeaders.values(CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN)
        MatcherAssert.assertThat("pre-flight response check for " + CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN,
                allowOrigins, Matchers.`is`(Matchers.not(Matchers.empty())))
        MatcherAssert.assertThat("Header " + CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN, allowOrigins, Matchers.contains("http://foo.com"))

        // Send the follow-up request.
        builder = client.put()
        headers = builder.headers()
        headers.add("Origin", "http://foo.com")
        headers.add("Host", "here.com")
        headers.addAll(preflightResponseHeaders)
        r = putResponse("/greet/greeting", "Cheers", builder)
        MatcherAssert.assertThat("HTTP response3", r.status().code(), Matchers.`is`(204))
        headers = r.headers()
        allowOrigins = headers.values(CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN)
        MatcherAssert.assertThat("Expected CORS header " + CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN,
                allowOrigins, Matchers.`is`(Matchers.not(Matchers.empty())))
        MatcherAssert.assertThat("Header " + CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN, allowOrigins, Matchers.contains("http://foo.com"))
    }

    @Order(12) // Run after CORS test changes greeting to Cheers.
    @Test
    @Throws(Exception::class)
    fun testNamedGreetWithCors() {
        val builder = client.get()
        var headers: Headers = builder.headers()
        headers.add("Origin", "http://foo.com")
        headers.add("Host", "here.com")
        val r = getResponse("/greet/Maria", builder)
        MatcherAssert.assertThat("HTTP response", r.status().code(), Matchers.`is`(200))
        MatcherAssert.assertThat(fromPayload(r), Matchers.containsString("Cheers Maria"))
        headers = r.headers()
        val allowOrigin = headers.value(CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN)
        MatcherAssert.assertThat("Expected CORS header " + CrossOriginConfig.ACCESS_CONTROL_ALLOW_ORIGIN + " presence check",
                allowOrigin.isPresent, Matchers.`is`(true))
        MatcherAssert.assertThat(allowOrigin.get(), Matchers.`is`("*"))
    }

    @Order(100) // After all other tests so we can rely on deterministic greetings.
    @Test
    @Throws(Exception::class)
    fun testGreetingChangeWithCorsAndOtherOrigin() {
        val builder = client.put()
        val headers: Headers = builder.headers()
        headers.add("Origin", "http://other.com")
        headers.add("Host", "here.com")
        val r = putResponse("/greet/greeting", "Ahoy", builder)
        // Result depends on whether we are using overrides or not.
        val isOverriding = Config.create()["cors"].exists()
        MatcherAssert.assertThat("HTTP response3", r.status().code(), Matchers.`is`(if (isOverriding) 204 else 403))
    }

    companion object {
        private const val JSON_MESSAGE_RESPONSE_LABEL = "message"
        private const val JSON_NEW_GREETING_LABEL = "greeting"
        private val JSON_BF = Json.createBuilderFactory(emptyMap<String, Any>())
        private val JSONP_SUPPORT = JsonpSupport.create()
        private lateinit var client: WebClient
        private lateinit var server: Server
        @BeforeAll
        @JvmStatic
        fun init() {
            val serverConfig = Config.create()["server"]
            val serverBuilder = Server.builder()
            serverConfig.ifExists { config: Config? -> serverBuilder.config(config) }
            server = serverBuilder
                    .port(-1) // override the port for testing
                    .build()
                    .start()
            client = WebClient.builder()
                    .baseUri("http://localhost:" + server.port())
                    .addMediaSupport(JSONP_SUPPORT)
                    .build()
        }

        @AfterAll
        @JvmStatic
        fun cleanup() {
            if (server != null) {
                server.stop()
            }
        }

        @Throws(ExecutionException::class, InterruptedException::class)
        private fun getResponse(path: String): WebClientResponse {
            return getResponse(path, client.get())
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
        private fun fromPayload(response: WebClientResponse): String {
            val json = response
                    .content()
                    .`as`(JsonObject::class.java)
                    .toCompletableFuture()
                    .get()
            return json.getString(JSON_MESSAGE_RESPONSE_LABEL)
        }

        private fun toPayload(message: String): JsonObject {
            val builder = JSON_BF.createObjectBuilder()
            return builder.add(JSON_NEW_GREETING_LABEL, message)
                    .build()
        }

        @Throws(ExecutionException::class, InterruptedException::class)
        private fun putResponse(path: String, message: String, builder: WebClientRequestBuilder = client.put()): WebClientResponse {
            return builder
                    .accept(MediaType.APPLICATION_JSON)
                    .path(path)
                    .submit(toPayload(message))
                    .toCompletableFuture()
                    .get()
        }
    }
}