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
 */
package io.helidon.kotlin.webserver.examples.faulttolerance

import io.helidon.common.http.Http
import io.helidon.kotlin.webserver.examples.faulttolerance.Main.startServer
import io.helidon.webclient.WebClient
import io.helidon.webserver.WebServer
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

internal class MainTest {
    @Test
    fun testAsync() {
        val response = client.get()
                .path("/async")
                .request(String::class.java)
                .await(5, TimeUnit.SECONDS)
        MatcherAssert.assertThat(response, CoreMatchers.`is`("blocked for 100 millis"))
    }

    @Test
    @Throws(InterruptedException::class)
    fun testBulkhead() {
        // bulkhead is configured for limit of 1 and queue of 1, so third
        // request should fail
        client.get()
                .path("/bulkhead/10000")
                .request()
        client.get()
                .path("/bulkhead/10000")
                .request()

        // I want to make sure the above is connected
        Thread.sleep(300)
        val third = client.get()
                .path("/bulkhead/10000")
                .request()
                .await(1, TimeUnit.SECONDS)

        // registered an error handler in Main
        MatcherAssert.assertThat(third.status(), CoreMatchers.`is`(Http.Status.SERVICE_UNAVAILABLE_503))
        MatcherAssert.assertThat(third.content().asSingle(String::class.java).await(1, TimeUnit.SECONDS), CoreMatchers.`is`("bulkhead"))
    }

    @Test
    fun testCircuitBreaker() {
        var response = client.get()
                .path("/circuitBreaker/true")
                .request(String::class.java)
                .await(1, TimeUnit.SECONDS)
        MatcherAssert.assertThat(response, CoreMatchers.`is`("blocked for 100 millis"))

        // error ratio is 20% within 10 request
        client.get()
                .path("/circuitBreaker/false")
                .request()
                .await(1, TimeUnit.SECONDS)

        // should work after first
        response = client.get()
                .path("/circuitBreaker/true")
                .request(String::class.java)
                .await(1, TimeUnit.SECONDS)
        MatcherAssert.assertThat(response, CoreMatchers.`is`("blocked for 100 millis"))

        // should open after second
        client.get()
                .path("/circuitBreaker/false")
                .request()
                .await(1, TimeUnit.SECONDS)
        val clientResponse = client.get()
                .path("/circuitBreaker/true")
                .request()
                .await(1, TimeUnit.SECONDS)
        response = clientResponse.content().asSingle(String::class.java).await(1, TimeUnit.SECONDS)

        // registered an error handler in Main
        MatcherAssert.assertThat(clientResponse.status(), CoreMatchers.`is`(Http.Status.SERVICE_UNAVAILABLE_503))
        MatcherAssert.assertThat(response, CoreMatchers.`is`("circuit breaker"))
    }

    @Test
    fun testFallback() {
        var response = client.get()
                .path("/fallback/true")
                .request(String::class.java)
                .await(1, TimeUnit.SECONDS)
        MatcherAssert.assertThat(response, CoreMatchers.`is`("blocked for 100 millis"))
        response = client.get()
                .path("/fallback/false")
                .request(String::class.java)
                .await(1, TimeUnit.SECONDS)
        MatcherAssert.assertThat(response, CoreMatchers.`is`("Failed back because of reactive failure"))
    }

    @Test
    fun testRetry() {
        var response = client.get()
                .path("/retry/1")
                .request(String::class.java)
                .await(1, TimeUnit.SECONDS)
        MatcherAssert.assertThat(response, CoreMatchers.`is`("calls/failures: 1/0"))
        response = client.get()
                .path("/retry/2")
                .request(String::class.java)
                .await(1, TimeUnit.SECONDS)
        MatcherAssert.assertThat(response, CoreMatchers.`is`("calls/failures: 2/1"))
        response = client.get()
                .path("/retry/3")
                .request(String::class.java)
                .await(1, TimeUnit.SECONDS)
        MatcherAssert.assertThat(response, CoreMatchers.`is`("calls/failures: 3/2"))
        val clientResponse = client.get()
                .path("/retry/4")
                .request()
                .await(1, TimeUnit.SECONDS)
        response = clientResponse.content().asSingle(String::class.java).await(1, TimeUnit.SECONDS)
        // no error handler specified
        MatcherAssert.assertThat(clientResponse.status(), CoreMatchers.`is`(Http.Status.INTERNAL_SERVER_ERROR_500))
        MatcherAssert.assertThat(response, CoreMatchers.`is`("java.lang.RuntimeException: reactive failure"))
    }

    @Test
    fun testTimeout() {
        var response = client.get()
                .path("/timeout/50")
                .request(String::class.java)
                .await(1, TimeUnit.SECONDS)
        MatcherAssert.assertThat(response, CoreMatchers.`is`("Slept for 50 ms"))
        val clientResponse = client.get()
                .path("/timeout/105")
                .request()
                .await(1, TimeUnit.SECONDS)
        response = clientResponse.content().asSingle(String::class.java).await(1, TimeUnit.SECONDS)
        // error handler specified in Main
        MatcherAssert.assertThat(clientResponse.status(), CoreMatchers.`is`(Http.Status.REQUEST_TIMEOUT_408))
        MatcherAssert.assertThat(response, CoreMatchers.`is`("timeout"))
    }

    companion object {
        private lateinit var server: WebServer
        private lateinit var client: WebClient
        @BeforeAll
        @JvmStatic
        @Throws(ExecutionException::class, InterruptedException::class)
        fun initClass() {
            server = startServer(0)
                    .await(10, TimeUnit.SECONDS)
            client = WebClient.builder()
                    .baseUri("http://localhost:" + server.port() + "/ft")
                    .build()
        }

        @AfterAll
        @JvmStatic
        fun destroyClass() {
            server.shutdown()
                    .await(5, TimeUnit.SECONDS)
        }
    }
}