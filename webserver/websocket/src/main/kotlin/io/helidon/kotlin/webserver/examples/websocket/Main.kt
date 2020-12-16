/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.webserver.examples.websocket

import io.helidon.kotlin.webserver.examples.websocket.MessageBoardEndpoint.UppercaseEncoder
import io.helidon.webserver.Routing
import io.helidon.webserver.StaticContentSupport
import io.helidon.webserver.WebServer
import io.helidon.webserver.tyrus.TyrusSupport
import java.util.concurrent.CompletableFuture
import javax.websocket.Encoder
import javax.websocket.server.ServerEndpointConfig

/**
 * Application demonstrates combination of websocket and REST.
 */


fun createRouting(): Routing {
    val encoders: List<Class<out Encoder?>> = listOf(UppercaseEncoder::class.java)
    return Routing.builder()
        .register("/rest", MessageQueueService())
        .register(
            "/websocket",
            TyrusSupport.builder().register(
                ServerEndpointConfig.Builder.create(MessageBoardEndpoint::class.java, "/board")
                    .encoders(encoders).build()
            )
                .build()
        )
        .register("/web", StaticContentSupport.builder("/WEB").build())
        .build()
}


fun startWebServer(): WebServer {
    val server = WebServer.builder(createRouting())
        .port(8080)
        .build()

    // Start webserver
    val started = CompletableFuture<Void?>()
    server.start().thenAccept { ws: WebServer ->
        println("WEB server is up! http://localhost:" + ws.port())
        started.complete(null)
    }

    // Wait for webserver to start before returning
    try {
        started.toCompletableFuture().get()
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
    return server
}

/**
 * A java main class.
 *
 */
fun main() {
    val server = startWebServer()

    // Server threads are not demon. NO need to block. Just react.
    server.whenShutdown()
        .thenRun { println("WEB server is DOWN. Good bye!") }
}
