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
import io.helidon.webserver.testsupport.TestClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests [Main].
 */
class MainTest {
    @Test
    @Throws(Exception::class)
    fun testShutDown() {
        val response = TestClient.create(createRouting())
                .path("/mgmt/shutdown")
                .post()
        Assertions.assertEquals(Http.Status.OK_200, response.status())
        val latch = CountDownLatch(1)
        val webServer = response.webServer()
        webServer
                .whenShutdown()
                .thenRun { latch.countDown() }
        Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS))
    }
}