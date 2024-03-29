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
package io.helidon.kotlin.webserver.examples.streaming

import io.helidon.webserver.Routing
import io.helidon.webserver.WebServer
import routing
import webServer


const val LARGE_FILE_RESOURCE = "/large-file.bin"

/**
 * Creates new [Routing].
 *
 * @return the new instance
 */
private fun createRouting(): Routing {
    return routing {
        register(StreamingService())
    }
}

/**
 * A java main class.
 *
 */
fun main() {
    val server = webServer {
        routing(createRouting())
        port(8080)
    }
    server.start().thenAccept { ws: WebServer -> println("Steaming service is up at http://localhost:" + ws.port()) }
    server.whenShutdown().thenRun { println("Streaming service is down") }
}