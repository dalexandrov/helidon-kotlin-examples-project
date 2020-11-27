/*
 * Copyright (c) 2017, 2018 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.webserver.examples.tutorial

import io.helidon.common.http.Http
import io.helidon.common.http.MediaType
import io.helidon.webserver.Routing
import io.helidon.webserver.testsupport.MediaPublisher
import io.helidon.webserver.testsupport.TestClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/**
 * Tests [CommentService].
 */
class CommentServiceTest {
    @Test
    @Throws(Exception::class)
    fun addAndGetComments() {
        val service = CommentService()
        Assertions.assertEquals(0, service.getComments("one").size)
        Assertions.assertEquals(0, service.getComments("two").size)
        service.addComment("one", null, "aaa")
        Assertions.assertEquals(1, service.getComments("one").size)
        Assertions.assertEquals(0, service.getComments("two").size)
        service.addComment("one", null, "bbb")
        Assertions.assertEquals(2, service.getComments("one").size)
        Assertions.assertEquals(0, service.getComments("two").size)
        service.addComment("two", null, "bbb")
        Assertions.assertEquals(2, service.getComments("one").size)
        Assertions.assertEquals(1, service.getComments("two").size)
    }

    @Test
    @Throws(Exception::class)
    fun testRouting() {
        val routing = Routing.builder()
                .register(CommentService())
                .build()
        var response = TestClient.create(routing)
                .path("one")
                .get()
        Assertions.assertEquals(Http.Status.OK_200, response.status())

        // Add first comment
        response = TestClient.create(routing)
                .path("one")
                .post(MediaPublisher.create(MediaType.TEXT_PLAIN, "aaa"))
        Assertions.assertEquals(Http.Status.OK_200, response.status())
        response = TestClient.create(routing)
                .path("one")
                .get()
        Assertions.assertEquals(Http.Status.OK_200, response.status())
        var data = response.asBytes().toCompletableFuture().get()
        Assertions.assertEquals("anonymous: aaa\n", String(data!!, StandardCharsets.UTF_8))

        // Add second comment
        response = TestClient.create(routing)
                .path("one")
                .post(MediaPublisher.create(MediaType.TEXT_PLAIN, "bbb"))
        Assertions.assertEquals(Http.Status.OK_200, response.status())
        response = TestClient.create(routing)
                .path("one")
                .get()
        Assertions.assertEquals(Http.Status.OK_200, response.status())
        data = response.asBytes().toCompletableFuture().get()
        //FIXME: rethink this test
        //Assertions.assertEquals("anonymous: aaa\nanonymous: bbb\n", String(data, StandardCharsets.UTF_8).trim())
    }
}