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
package io.helidon.kotlin.examples.quickstart

import io.helidon.microprofile.server.Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import javax.enterprise.inject.se.SeContainer
import javax.enterprise.inject.spi.CDI
import javax.json.JsonObject
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType

internal class MainTest {
    @Test
    fun testHelloWorld() {
        val client = ClientBuilder.newClient()
        var jsonObject = client
                .target(serverUrl)
                .path("greet")
                .request()
                .get(JsonObject::class.java)
        Assertions.assertEquals("Hello World!", jsonObject.getString("message"),
                "default message")
        jsonObject = client
                .target(serverUrl)
                .path("greet/Joe")
                .request()
                .get(JsonObject::class.java)
        Assertions.assertEquals("Hello Joe!", jsonObject.getString("message"),
                "hello Joe message")
        var r = client
                .target(serverUrl)
                .path("greet/greeting")
                .request()
                .put(Entity.entity("{\"greeting\" : \"Hola\"}", MediaType.APPLICATION_JSON))
        Assertions.assertEquals(204, r.status, "PUT status code")
        jsonObject = client
                .target(serverUrl)
                .path("greet/Jose")
                .request()
                .get(JsonObject::class.java)
        Assertions.assertEquals("Hola Jose!", jsonObject.getString("message"),
                "hola Jose message")
        r = client
                .target(serverUrl)
                .path("metrics")
                .request()
                .get()
        Assertions.assertEquals(200, r.status, "GET metrics status code")
        r = client
                .target(serverUrl)
                .path("health")
                .request()
                .get()
        Assertions.assertEquals(200, r.status, "GET health status code")
    }

    companion object {
        private var server: Server? = null
        private var serverUrl: String? = null

        @BeforeAll
        @JvmStatic
        @Throws(Exception::class)
        fun startTheServer() {
            server = Server.create().start()
            serverUrl = "http://localhost:" + server!!.port()
        }

        @AfterAll
        @JvmStatic
        fun destroyClass() {
            val current = CDI.current()
            (current as SeContainer).close()
        }
    }
}