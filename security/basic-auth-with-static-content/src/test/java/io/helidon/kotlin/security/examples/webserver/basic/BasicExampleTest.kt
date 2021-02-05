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
 */
package io.helidon.kotlin.security.examples.webserver.basic

import asSingle
import io.helidon.common.http.Http
import io.helidon.security.Security
import io.helidon.security.providers.httpauth.HttpBasicAuthProvider
import io.helidon.webclient.WebClient
import io.helidon.webclient.WebClientResponse
import io.helidon.webclient.security.WebClientSecurity
import io.helidon.webserver.WebServer
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is` as Is
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

/**
 * Abstract class with tests for this example (used by programmatic and config based tests).
 */
abstract class BasicExampleTest {
    abstract val serverBase: String

    //now for the tests
    @Test
    fun testPublic() {
        //Must be accessible without authentication
        val response = client.get()
            .uri("$serverBase/public")
            .request()
            .await(10, TimeUnit.SECONDS)
        MatcherAssert.assertThat(response.status(), Is(Http.Status.OK_200))
        val entity = response.content().asSingle(String::class.java).await(10, TimeUnit.SECONDS)
        MatcherAssert.assertThat(entity, CoreMatchers.containsString("<ANONYMOUS>"))
    }

    @Test
    fun testNoRoles() {
        val url = "$serverBase/noRoles"
        testNotAuthorized(url)

        //Must be accessible with authentication - to everybody
        testProtected(url, "jack", "password", mutableSetOf("admin", "user"), mutableSetOf())
        testProtected(url, "jill", "password", mutableSetOf("user"), mutableSetOf("admin"))
        testProtected(url, "john", "password", mutableSetOf(), mutableSetOf("admin", "user"))
    }

    @Test
    fun testUserRole() {
        val url = "$serverBase/user"
        testNotAuthorized(url)

        //Jack and Jill allowed (user role)
        testProtected(url, "jack", "password", mutableSetOf("admin", "user"), mutableSetOf())
        testProtected(url, "jill", "password", mutableSetOf("user"), mutableSetOf("admin"))
        testProtectedDenied(url, "john", "password")
    }

    @Test
    fun testAdminRole() {
        val url = "$serverBase/admin"
        testNotAuthorized(url)

        //Only jack is allowed - admin role...
        testProtected(url, "jack", "password", mutableSetOf("admin", "user"), mutableSetOf())
        testProtectedDenied(url, "jill", "password")
        testProtectedDenied(url, "john", "password")
    }

    @Test
    fun testDenyRole() {
        val url = "$serverBase/deny"
        testNotAuthorized(url)

        // nobody has the correct role
        testProtectedDenied(url, "jack", "password")
        testProtectedDenied(url, "jill", "password")
        testProtectedDenied(url, "john", "password")
    }

    //Must NOT be accessible without authentication
    @get:Test
    val noAuthn: Unit
        // authentication is optional, so we are not challenged, only forbidden, as the role can never be there...
        get() {
            val url = "$serverBase/noAuthn"
            //Must NOT be accessible without authentication
            val response = client.get().uri(url).request().await(5, TimeUnit.SECONDS)

            // authentication is optional, so we are not challenged, only forbidden, as the role can never be there...
            MatcherAssert.assertThat(response.status(), Is(Http.Status.FORBIDDEN_403))
        }

    private fun testNotAuthorized(uri: String) {
        //Must NOT be accessible without authentication
        val response = client.get().uri(uri).request().await(5, TimeUnit.SECONDS)
        MatcherAssert.assertThat(response.status(), Is(Http.Status.UNAUTHORIZED_401))
        val header = response.headers().first("WWW-Authenticate").get()
        MatcherAssert.assertThat(header.toLowerCase(), CoreMatchers.containsString("basic"))
        MatcherAssert.assertThat(header, CoreMatchers.containsString("helidon"))
    }

    private fun callProtected(uri: String, username: String, password: String): WebClientResponse {
        // here we call the endpoint
        return client.get()
            .uri(uri)
            .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_USER, username)
            .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_PASSWORD, password)
            .request()
            .await(5, TimeUnit.SECONDS)
    }

    private fun testProtectedDenied(
        uri: String,
        username: String,
        password: String
    ) {
        val response = callProtected(uri, username, password)
        MatcherAssert.assertThat(response.status(), Is(Http.Status.FORBIDDEN_403))
    }

    private fun testProtected(
        uri: String,
        username: String,
        password: String,
        expectedRoles: Set<String>,
        invalidRoles: Set<String>
    ) {
        val response = callProtected(uri, username, password)
        val entity = response.content().asSingle(String::class.java).await(5, TimeUnit.SECONDS)
        MatcherAssert.assertThat(response.status(), Is(Http.Status.OK_200))

        // check login
        MatcherAssert.assertThat(entity, CoreMatchers.containsString("id='$username'"))
        // check roles
        expectedRoles.forEach(Consumer { role: String ->
            MatcherAssert.assertThat(
                entity,
                CoreMatchers.containsString(":$role")
            )
        })
        invalidRoles.forEach(Consumer { role: String ->
            MatcherAssert.assertThat(
                entity,
                CoreMatchers.not(CoreMatchers.containsString(":$role"))
            )
        })
    }

    companion object {
        @JvmStatic
        private lateinit var client: WebClient

        @BeforeAll
        @JvmStatic
        fun classInit() {
            val security = Security.builder()
                .addProvider(
                    HttpBasicAuthProvider.builder()
                        .build()
                )
                .build()
            val securityService = WebClientSecurity.create(security)
            client = WebClient.builder()
                .addService(securityService)
                .build()
        }

        @JvmStatic
        fun stopServer(server: WebServer) {
            val t = System.nanoTime()
            server.shutdown().await(5, TimeUnit.SECONDS)
            val time = System.nanoTime() - t
            println("Server shutdown in " + TimeUnit.NANOSECONDS.toMillis(time) + " ms")
        }
    }
}