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
package io.helidon.kotlin.security.examples.google

import io.helidon.common.http.Http
import io.helidon.config.testing.OptionalMatcher
import io.helidon.webclient.WebClient
import io.helidon.webserver.WebServer
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * Google login common unit tests.
 */
abstract class GoogleMainTest {
    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testEndpoint() {
        client.get()
            .uri("http://localhost:" + port() + "/rest/profile")
            .request()
            .thenAccept {
                MatcherAssert.assertThat(it.status(), Is(Http.Status.UNAUTHORIZED_401))
                MatcherAssert.assertThat(
                    it.headers().first(Http.Header.WWW_AUTHENTICATE),
                    OptionalMatcher.value(Is("Bearer realm=\"helidon\",scope=\"openid profile email\""))
                )
            }
            .toCompletableFuture()
            .get()
    }

    abstract fun port(): Int

    companion object {
        @JvmStatic
        private lateinit var client: WebClient

        @BeforeAll
        @JvmStatic
        fun classInit() {
            client = WebClient.create()
        }

        @JvmStatic
        @Throws(InterruptedException::class)
        fun stopServer(server: WebServer?) {
            val cdl = CountDownLatch(1)
            val t = System.nanoTime()
            if (null == server) {
                return
            }
            server.shutdown().thenAccept {
                val time = System.nanoTime() - t
                println("Server shutdown in " + TimeUnit.NANOSECONDS.toMillis(time) + " ms")
                cdl.countDown()
            }
            check(cdl.await(5, TimeUnit.SECONDS)) { "Failed to shutdown server within 5 seconds" }
        }
    }
}