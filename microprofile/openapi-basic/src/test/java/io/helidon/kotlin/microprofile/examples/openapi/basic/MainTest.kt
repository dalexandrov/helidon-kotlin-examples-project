/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.microprofile.examples.openapi.basic

import io.helidon.kotlin.microprofile.examples.openapi.basic.internal.SimpleAPIModelReader
import io.helidon.microprofile.server.Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import javax.enterprise.inject.se.SeContainer
import javax.enterprise.inject.spi.CDI
import javax.json.Json
import javax.json.JsonObject
import javax.json.JsonString
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType

internal class MainTest {
    @Test
    fun testHelloWorld() {
        val client = ClientBuilder.newClient()
        var jsonObject = client
                .target(getConnectionString("/greet"))
                .request()
                .get(JsonObject::class.java)
        Assertions.assertEquals("Hello World!", jsonObject.getString("message"),
                "default message")
        jsonObject = client
                .target(getConnectionString("/greet/Joe"))
                .request()
                .get(JsonObject::class.java)
        Assertions.assertEquals("Hello Joe!", jsonObject.getString("message"),
                "hello Joe message")
        val r = client
                .target(getConnectionString("/greet/greeting"))
                .request()
                .put(Entity.entity("{\"greeting\" : \"Hola\"}", MediaType.APPLICATION_JSON))
        Assertions.assertEquals(204, r.status, "PUT status code")
        jsonObject = client
                .target(getConnectionString("/greet/Jose"))
                .request()
                .get(JsonObject::class.java)
        Assertions.assertEquals("Hola Jose!", jsonObject.getString("message"),
                "hola Jose message")
        client.close()
    }

    @Test
    fun testOpenAPI() {
        val client = ClientBuilder.newClient()
        val jsonObject = client
                .target(getConnectionString("/openapi"))
                .request(MediaType.APPLICATION_JSON)
                .get(JsonObject::class.java)
        val paths = jsonObject["paths"]!!.asJsonObject()
        var jp = Json.createPointer("/" + escape(SimpleAPIModelReader.MODEL_READER_PATH) + "/get/summary")
        var js = JsonString::class.java.cast(jp.getValue(paths))
        Assertions.assertEquals(SimpleAPIModelReader.SUMMARY, js.string, "/test/newpath GET summary did not match")
        jp = Json.createPointer("/" + escape(SimpleAPIModelReader.DOOMED_PATH))
        Assertions.assertFalse(jp.containsValue(paths), "/test/doomed should not appear but does")
        jp = Json.createPointer("/" + escape("/greet") + "/get/summary")
        js = JsonString::class.java.cast(jp.getValue(paths))
        Assertions.assertEquals("Returns a generic greeting", js.string, "/greet GET summary did not match")
        client.close()
    }

    private fun getConnectionString(path: String): String {
        return "http://localhost:" + server.port() + path
    }

    private fun escape(path: String): String {
        return path.replace("/", "~1")
    }

    companion object {
        private lateinit var server: Server
        @BeforeAll
        @JvmStatic
        @Throws(Exception::class)
        fun startTheServer() {
            server = startServer()
        }

        @AfterAll
        @JvmStatic
        fun destroyClass() {
            val current = CDI.current()
            (current as SeContainer).close()
        }

        /**
         * Start the server.
         * @return the created [Server] instance
         */
        private fun startServer(): Server {
            // Server will automatically pick up configuration from
            // microprofile-config.properties
            // and Application classes annotated as @ApplicationScoped
            return Server.create().start()
        }
    }
}