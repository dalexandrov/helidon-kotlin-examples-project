/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.kotlin.security.examples.webserver.basic

import io.helidon.webserver.WebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

/**
 * Unit test for [BasicExampleBuilderMain].
 */
class BasicExampleBuilderTest : BasicExampleTest() {

    companion object {
        private lateinit var server: WebServer

        @BeforeAll
        @JvmStatic
        fun startServer() {
            // start the test
            server = BasicExampleBuilderMain.startServer()
        }

        @AfterAll
        @JvmStatic
        fun stopServer() {
            stopServer(server)
        }
    }

    override val serverBase: String
        get() = "http://localhost:" + server.port()
}