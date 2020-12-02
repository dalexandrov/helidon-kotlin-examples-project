/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.security.examples.jersey

import io.helidon.kotlin.security.examples.jersey.JerseyBuilderMain.httpServer
import io.helidon.kotlin.security.examples.jersey.JerseyBuilderMain.main
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

/**
 * Test of hello world example.
 */
class JerseyBuilderMainTest : JerseyMainTest() {
    override val port: Int
        protected get() = httpServer.port()

    companion object {
        @BeforeAll
        @JvmStatic
        @Throws(Throwable::class)
        fun initClass() {
            main(null)
        }

        @AfterAll
        @JvmStatic
        @Throws(InterruptedException::class)
        fun destroyClass() {
            stopServer(httpServer)
        }
    }
}