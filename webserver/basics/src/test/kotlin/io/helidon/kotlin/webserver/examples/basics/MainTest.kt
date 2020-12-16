/*
 * Copyright (c) 2017, 2020 Oracle and/or its affiliates.
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
package io.helidon.kotlin.webserver.examples.basics

import io.helidon.common.http.Http
import io.helidon.common.http.MediaType
import io.helidon.media.common.MediaContext
import io.helidon.webserver.Routing
import io.helidon.webserver.testsupport.MediaPublisher
import io.helidon.webserver.testsupport.TestClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.function.Consumer

class MainTest {
    @Test
    @Throws(Exception::class)
    fun firstRouting() {
        // POST
        var response = createClient {  }.path("/post-endpoint").post()
        Assertions.assertEquals(201, response.status().code())
        // GET
        response = createClient { obj: Main -> obj.firstRouting() }.path("/get-endpoint").get()
        Assertions.assertEquals(204, response.status().code())
        Assertions.assertEquals("Hello World!", response.asString().get())
    }

    @Test
    @Throws(Exception::class)
    fun routingAsFilter() {
        // POST
        var response = createClient { obj: Main -> obj.routingAsFilter() }.path("/post-endpoint").post()
        Assertions.assertEquals(201, response.status().code())
        // GET
        response = createClient { obj: Main -> obj.routingAsFilter() }.path("/get-endpoint").get()
        Assertions.assertEquals(204, response.status().code())
    }

    @Test
    @Throws(Exception::class)
    fun parametersAndHeaders() {
        val response = createClient { obj: Main -> obj.parametersAndHeaders() }.path("/context/aaa")
                .queryParameter("bar", "bbb")
                .header("foo", "ccc")
                .get()
        Assertions.assertEquals(200, response.status().code())
        val s = response.asString().get()
        Assertions.assertTrue(s.contains("id: aaa"))
        Assertions.assertTrue(s.contains("bar: bbb"))
        Assertions.assertTrue(s.contains("foo: ccc"))
    }

    @Test
    @Throws(Exception::class)
    fun organiseCode() {
        // List
        var response = createClient { obj: Main -> obj.organiseCode() }.path("/catalog-context-path").get()
        Assertions.assertEquals(200, response.status().code())
        Assertions.assertEquals("1, 2, 3, 4, 5", response.asString().get())
        // Get by id
        response = createClient { obj: Main -> obj.organiseCode() }.path("/catalog-context-path/aaa").get()
        Assertions.assertEquals(200, response.status().code())
        Assertions.assertEquals("Item: aaa", response.asString().get())
    }

    @Test
    @Throws(Exception::class)
    fun readContentEntity() {
        // foo
        var response = createClient { obj: Main -> obj.readContentEntity() }.path("/foo")
                .post(MediaPublisher.create(MediaType.TEXT_PLAIN, "aaa"))
        Assertions.assertEquals(200, response.status().code())
        Assertions.assertEquals("aaa", response.asString().get())
        // bar
        response = createClient { obj: Main -> obj.readContentEntity() }.path("/bar")
                .post(MediaPublisher.create(MediaType.TEXT_PLAIN, "aaa"))
        Assertions.assertEquals(200, response.status().code())
        Assertions.assertEquals("aaa", response.asString().get())
    }

    @Test
    @Throws(Exception::class)
    fun mediaReader() {
        var response = createClient { obj: Main -> obj.mediaReader() }
                .path("/create-record")
                .post(MediaPublisher.create(MediaType.parse("application/name"), "John Smith"))
        Assertions.assertEquals(201, response.status().code())
        Assertions.assertEquals("John Smith", response.asString().get())
        // Unsupported Content-Type
        response = createClient { obj: Main -> obj.mediaReader() }
                .path("/create-record")
                .post(MediaPublisher.create(MediaType.TEXT_PLAIN, "John Smith"))
        Assertions.assertEquals(500, response.status().code())
    }

    @Test
    @Throws(Exception::class)
    fun supports() {
        // Jersey
        var response = createClient { obj: Main -> obj.supports() }.path("/api/hw").get()
        Assertions.assertEquals(200, response.status().code())
        Assertions.assertEquals("Hello world!", response.asString().get())
        // Static content
        response = createClient { obj: Main -> obj.supports() }.path("/index.html").get()
        Assertions.assertEquals(200, response.status().code())
        Assertions.assertEquals(MediaType.TEXT_HTML.toString(), response.headers().first(Http.Header.CONTENT_TYPE).orElse(null))
        // JSON
        response = createClient { obj: Main -> obj.supports() }.path("/hello/Europe").get()
        Assertions.assertEquals(200, response.status().code())
        Assertions.assertEquals("{\"message\":\"Hello Europe\"}", response.asString().get())
    }

    @Test
    @Throws(Exception::class)
    fun errorHandling() {
        // Valid
        var response = createClient { obj: Main -> obj.errorHandling() }
                .path("/compute")
                .post(MediaPublisher.create(MediaType.TEXT_PLAIN, "2"))
        Assertions.assertEquals(200, response.status().code())
        Assertions.assertEquals("100 / 2 = 50", response.asString().get())
        // Zero
        response = createClient { obj: Main -> obj.errorHandling() }
                .path("/compute")
                .post(MediaPublisher.create(MediaType.TEXT_PLAIN, "0"))
        Assertions.assertEquals(412, response.status().code())
        // NaN
        response = createClient { obj: Main -> obj.errorHandling() }
                .path("/compute")
                .post(MediaPublisher.create(MediaType.TEXT_PLAIN, "aaa"))
        Assertions.assertEquals(400, response.status().code())
    }

    private fun createClient(callTestedMethod: Consumer<Main>): TestClient {
        val tm = TMain()
        callTestedMethod.accept(tm)
        Assertions.assertNotNull(tm.routing)
        return TestClient.create(tm.routing, tm.mediaContext)
    }

    internal class TMain : Main() {
        internal var routing: Routing? = null
        internal var mediaContext: MediaContext? = null
        override fun startServer(routing: Routing?, mediaContext: MediaContext?) {
            this.routing = routing
            this.mediaContext = mediaContext
        }
    }
}