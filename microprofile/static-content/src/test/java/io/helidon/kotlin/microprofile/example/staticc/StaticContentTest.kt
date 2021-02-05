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
package io.helidon.kotlin.microprofile.example.staticc

import io.helidon.common.http.Http
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.io.IOException
import javax.enterprise.inject.se.SeContainer
import javax.enterprise.inject.spi.CDI
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.MediaType
import org.hamcrest.Matchers.`is` as Is

/**
 * Unit test for [HelloWorldResource].
 */
internal class StaticContentTest {
    @Test
    fun testDynamicResource() {
        val response = ClientBuilder.newClient()
                .target("http://localhost:" + port + "/helloworld")
                .request()
                .get(String::class.java)
        Assertions.assertAll("Response must be HTML and contain a ref to static resource",
                Executable { MatcherAssert.assertThat(response, Matchers.containsString("/resource.html")) },
                Executable { MatcherAssert.assertThat(response, Matchers.containsString("Hello World")) }
        )
    }

    @Test
    fun testWelcomePage() {
        val response = ClientBuilder.newClient()
                .target("http://localhost:" + port)
                .request()
                .accept(MediaType.TEXT_HTML_TYPE)
                .get()
        MatcherAssert.assertThat("Status should be 200", response.status, Is(Http.Status.OK_200.code()))
        val str = response.readEntity(String::class.java)
        Assertions.assertAll(
                Executable { MatcherAssert.assertThat(response.mediaType, Is(MediaType.TEXT_HTML_TYPE)) },
                Executable { MatcherAssert.assertThat(str, Matchers.containsString("server.static.classpath.location=/WEB")) }
        )
    }

    @Test
    fun testStaticResource() {
        val response = ClientBuilder.newClient()
                .target("http://localhost:" + port + "/resource.html")
                .request()
                .accept(MediaType.TEXT_HTML_TYPE)
                .get()
        MatcherAssert.assertThat("Status should be 200", response.status, Is(Http.Status.OK_200.code()))
        val str = response.readEntity(String::class.java)
        Assertions.assertAll(
                Executable { MatcherAssert.assertThat(response.mediaType, Is(MediaType.TEXT_HTML_TYPE)) },
                Executable { MatcherAssert.assertThat(str, Matchers.containsString("server.static.classpath.location=/WEB")) }
        )
    }

    companion object {
        @BeforeAll
        @JvmStatic
        @Throws(IOException::class)
        fun initClass() {
            main(arrayOfNulls(0))
        }

        @AfterAll
        @JvmStatic
        fun destroyClass() {
            val current = CDI.current()
            (current as SeContainer).close()
        }
    }
}