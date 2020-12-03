/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.security.examples.jersey

import io.helidon.webserver.WebServer
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.Response

/**
 * Common unit tests for builder, config and programmatic security.
 */
abstract class JerseyMainTest {
    @Test
    fun testUnprotected() {
        val response = client.target(baseUri())
                .request()
                .get()
        MatcherAssert.assertThat(response.status, CoreMatchers.`is`(200))
        MatcherAssert.assertThat(response.readEntity(String::class.java), CoreMatchers.containsString("<ANONYMOUS>"))
    }

    @Test
    fun testProtectedOk() {
        testProtected(baseUri() + "/protected",
                "jack",
                "password",
                java.util.Set.of("user", "admin"),
                java.util.Set.of())
        testProtected(baseUri() + "/protected",
                "jill",
                "password",
                java.util.Set.of("user"),
                java.util.Set.of("admin"))
    }

    @Test
    fun testWrongPwd() {
        // here we call the endpoint
        val response = callProtected(baseUri() + "/protected", "jack", "somePassword")
        MatcherAssert.assertThat(response.status, CoreMatchers.`is`(401))
    }

    @Test
    fun testDenied() {
        testProtectedDenied(baseUri() + "/protected", "john", "password")
    }

    @Test
    fun testOutboundOk() {
        testProtected(baseUri() + "/outbound",
                "jill",
                "password",
                java.util.Set.of("user"),
                java.util.Set.of("admin"))
    }

    protected abstract val port: Int
    private fun baseUri(): String {
        return "http://localhost:$port/rest"
    }

    private fun callProtected(uri: String, username: String, password: String): Response {
        // here we call the endpoint
        return authFeatureClient.target(uri)
                .request()
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_USERNAME, username)
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_PASSWORD, password)
                .get()
    }

    private fun testProtectedDenied(uri: String,
                                    username: String,
                                    password: String) {
        val response = callProtected(uri, username, password)
        MatcherAssert.assertThat(response.status, CoreMatchers.`is`(403))
    }

    private fun testProtected(uri: String,
                              username: String,
                              password: String,
                              expectedRoles: Set<String>,
                              invalidRoles: Set<String>) {
        val response = callProtected(uri, username, password)
        val entity = response.readEntity(String::class.java)
        MatcherAssert.assertThat(response.status, CoreMatchers.`is`(200))

        // check login
        MatcherAssert.assertThat(entity, CoreMatchers.containsString("id='$username'"))
        // check roles
        expectedRoles.forEach(Consumer { role: String -> MatcherAssert.assertThat(entity, CoreMatchers.containsString(":$role")) })
        invalidRoles.forEach(Consumer { role: String -> MatcherAssert.assertThat(entity, CoreMatchers.not(CoreMatchers.containsString(":$role"))) })
    }

    companion object {
        @JvmStatic
        private lateinit var client: Client

        @JvmStatic
        private lateinit var authFeatureClient: Client

        @BeforeAll
        @JvmStatic
        fun classInit() {
            client = ClientBuilder.newClient()
            authFeatureClient = ClientBuilder.newClient()
                    .register(HttpAuthenticationFeature.basicBuilder().nonPreemptive().build())
        }

        @AfterAll
        @JvmStatic
        fun classDestroy() {
            client.close()
            authFeatureClient.close()
        }

        @JvmStatic
        @Throws(InterruptedException::class)
        fun stopServer(server: WebServer?) {
            if (null == server) {
                return
            }
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