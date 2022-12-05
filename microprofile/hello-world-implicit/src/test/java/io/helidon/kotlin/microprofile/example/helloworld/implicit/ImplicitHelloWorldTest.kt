/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
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
package io.helidon.kotlin.microprofile.example.helloworld.implicit

import io.helidon.microprofile.server.Server
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import javax.enterprise.inject.se.SeContainer
import javax.enterprise.inject.spi.CDI
import javax.json.JsonObject
import javax.ws.rs.client.ClientBuilder
import org.hamcrest.Matchers.`is` as Is

/**
 * Unit test for [HelloWorldResource].
 */
internal class ImplicitHelloWorldTest {
    @Test
    fun testJsonResource() {
        val jsonObject = ClientBuilder.newClient()
                .target("http://localhost:" + server.port() + "/helloworld/unit")
                .request()
                .get(JsonObject::class.java)
        Assertions.assertAll("JSON fields must match expected injection values",
                Executable { MatcherAssert.assertThat("Name from request", jsonObject.getString("name"), Is("unit")) },
                Executable { MatcherAssert.assertThat("Request id from CDI provider", jsonObject.getInt("requestId"),
                    Is(1)
                ) },
                Executable { MatcherAssert.assertThat("App name from config", jsonObject.getString("appName"),
                    Is("Hello World Application")
                ) },
                Executable { MatcherAssert.assertThat("Logger name", jsonObject.getString("logger"),
                    Is(HelloWorldResource::class.java.name)
                ) }
        )
    }

    companion object {
        private lateinit var server: Server
        @BeforeAll
        @JvmStatic
        fun initClass() {
            server = Server.create().start()
        }

        @AfterAll
        @JvmStatic
        fun destroyClass() {
            val current = CDI.current()
            (current as SeContainer).close()
        }
    }
}