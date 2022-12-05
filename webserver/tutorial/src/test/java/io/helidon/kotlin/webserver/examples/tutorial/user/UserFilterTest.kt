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
package io.helidon.kotlin.webserver.examples.tutorial.user

import io.helidon.webserver.Handler
import io.helidon.webserver.Routing
import io.helidon.webserver.ServerRequest
import io.helidon.webserver.ServerResponse
import io.helidon.webserver.testsupport.TestClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

/**
 * Tests [UserFilter].
 */
class UserFilterTest {
    @Test
    @Throws(Exception::class)
    fun filter() {
        val userReference = AtomicReference<User?>()
        val routing = Routing.builder()
            .any(UserFilter())
            .any(Handler { req: ServerRequest, res: ServerResponse ->
                userReference.set(
                    req.context()
                        .get(User::class.java)
                        .orElse(null)
                )
                res.send()
            })
            .build()
        var response = TestClient.create(routing)
            .path("/")
            .get()
        Assertions.assertEquals(User.ANONYMOUS, userReference.get())
        response = TestClient.create(routing)
            .path("/")
            .header("Cookie", "Unauthenticated-User-Alias=Foo")
            .get()
        Assertions.assertEquals("Foo", userReference.get()!!.alias)
    }
}