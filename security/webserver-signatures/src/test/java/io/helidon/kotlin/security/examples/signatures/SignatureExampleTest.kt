/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates.
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
package io.helidon.kotlin.security.examples.signatures

import io.helidon.security.Security
import io.helidon.security.providers.httpauth.HttpBasicAuthProvider
import io.helidon.webclient.WebClient
import io.helidon.webclient.security.WebClientSecurity
import io.helidon.webserver.WebServer
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

/**
 * Actual unit tests are shared by config and builder example.
 */
abstract class SignatureExampleTest {
    abstract val service1Port: Int
    abstract val service2Port: Int
    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testService1Hmac() {
        testProtected("http://localhost:$service1Port/service1",
                "jack",
                "password",
                java.util.Set.of("user", "admin"),
                java.util.Set.of(),
                "Service1 - HMAC signature")
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testService1Rsa() {
        testProtected("http://localhost:$service1Port/service1-rsa",
                "jack",
                "password",
                java.util.Set.of("user", "admin"),
                java.util.Set.of(),
                "Service1 - RSA signature")
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    private fun testProtected(uri: String,
                              username: String,
                              password: String,
                              expectedRoles: Set<String>,
                              invalidRoles: Set<String>,
                              service: String) {
        client.get()
                .uri(uri)
                .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_USER, username)
                .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_PASSWORD, password)
                .request(String::class.java)
                .thenAccept { it: String ->
                    // check login
                    MatcherAssert.assertThat(it, CoreMatchers.containsString("id='$username'"))
                    // check roles
                    expectedRoles.forEach(Consumer { role: String -> MatcherAssert.assertThat(it, CoreMatchers.containsString(":$role")) })
                    invalidRoles.forEach(Consumer { role: String -> MatcherAssert.assertThat(it, CoreMatchers.not(CoreMatchers.containsString(":$role"))) })
                    MatcherAssert.assertThat(it, CoreMatchers.containsString("id='$service'"))
                }
                .toCompletableFuture()
                .get()
    }

    companion object {
        private lateinit var client: WebClient
        @BeforeAll
        @JvmStatic
        fun classInit() {
            val security = Security.builder()
                    .addProvider(HttpBasicAuthProvider.builder().build())
                    .build()
            client = WebClient.builder()
                    .addService(WebClientSecurity.create(security))
                    .build()
        }

        @JvmStatic
        @Throws(InterruptedException::class)
        fun stopServer(server: WebServer) {
            val cdl = CountDownLatch(1)
            val t = System.nanoTime()
            server.shutdown().thenAccept { webServer: WebServer? ->
                val time = System.nanoTime() - t
                println("Server shutdown in " + TimeUnit.NANOSECONDS.toMillis(time) + " ms")
                cdl.countDown()
            }
            check(cdl.await(5, TimeUnit.SECONDS)) { "Failed to shutdown server within 5 seconds" }
        }
    }
}