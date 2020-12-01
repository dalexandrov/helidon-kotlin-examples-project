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
package io.helidon.kotlin.security.examples.webserver.digest

import io.helidon.kotlin.security.examples.webserver.digest.DigestExampleBuilderMain.main
import io.helidon.webserver.WebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import java.io.IOException

/**
 * Unit test for [DigestExampleBuilderMain].
 */
class DigestExampleBuilderTest : DigestExampleTest() {
    public override fun getServerBase(): String {
        return "http://localhost:" + server.port()
    }

    companion object {
        private lateinit var server: WebServer
        @BeforeAll
        @JvmStatic
        @Throws(IOException::class)
        fun startServer() {
            // start the test
            main(emptyArray())
            server = DigestExampleBuilderMain.server
        }

        @AfterAll
        @JvmStatic
        @Throws(InterruptedException::class)
        fun stopServer() {
            stopServer(server)
        }
    }
}