/*
 * Copyright (c) 2017, 2019 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.examples.translator

import io.helidon.kotlin.examples.translator.backend.startBackendServer
import io.helidon.kotlin.examples.translator.frontend.startFrontendServer
import io.helidon.webserver.WebServer
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.Response
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * The TranslatorTest.
 */
class TranslatorTest {
    @Test
    @Throws(Exception::class)
    fun testCzech() {
        val response: Response = target.queryParam("q", "cloud")
                .queryParam("lang", "czech")
                .request()
                .get()
        MatcherAssert.assertThat("Unexpected response! Status code: " + response.status,
                response.readEntity(String::class.java), Is("oblak\n")
        )
    }

    @Test
    @Throws(Exception::class)
    fun testItalian() {
        val response: Response = target.queryParam("q", "cloud")
                .queryParam("lang", "italian")
                .request()
                .get()
        MatcherAssert.assertThat("Unexpected response! Status code: " + response.status,
                response.readEntity(String::class.java), Is("nube\n")
        )
    }

    @Test
    @Throws(Exception::class)
    fun testFrench() {
        val response: Response = target.queryParam("q", "cloud")
                .queryParam("lang", "french")
                .request()
                .get()
        MatcherAssert.assertThat("Unexpected response! Status code: " + response.status,
                response.readEntity(String::class.java), Is("nuage\n")
        )
    }

    companion object {
        @JvmStatic
        private lateinit var webServerFrontend: WebServer
        @JvmStatic
        private lateinit var webServerBackend: WebServer
        @JvmStatic
        private lateinit var client: Client
        @JvmStatic
        private lateinit var target: WebTarget
        @BeforeAll
        @JvmStatic
        @Throws(Exception::class)
        fun setUp() {
            webServerBackend = startBackendServer().toCompletableFuture().get(10, TimeUnit.SECONDS)!!
            webServerFrontend = startFrontendServer().toCompletableFuture().get(10, TimeUnit.SECONDS)!!
            client = ClientBuilder.newClient()
            target = client.target("http://localhost:" + webServerFrontend.port())
        }

        @AfterAll
        @JvmStatic
        @Throws(Exception::class)
        fun tearDown() {
            webServerFrontend.shutdown().toCompletableFuture().get(10, TimeUnit.SECONDS)
            webServerBackend.shutdown().toCompletableFuture().get(10, TimeUnit.SECONDS)
            client.close()
        }
    }
}