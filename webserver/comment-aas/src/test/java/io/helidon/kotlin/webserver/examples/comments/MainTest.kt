/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.webserver.examples.comments

import io.helidon.common.http.Http
import io.helidon.common.http.MediaType
import io.helidon.webserver.testsupport.MediaPublisher
import io.helidon.webserver.testsupport.TestClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Tests [Main] class.
 */
class MainTest {
    @Test
    @Throws(Exception::class)
    fun argot() {
        val response = TestClient.create(createRouting(true))
                .path("/comments/one")
                .post(MediaPublisher.create(MediaType.TEXT_PLAIN, "Spring framework is the BEST!"))
        Assertions.assertEquals(Http.Status.NOT_ACCEPTABLE_406, response.status())
    }

    @Test
    @Throws(Exception::class)
    fun anonymousDisabled() {
        val response = TestClient.create(createRouting(false))
                .path("/comment/one")
                .get()
        Assertions.assertEquals(Http.Status.FORBIDDEN_403, response.status())
    }
}