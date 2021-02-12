/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.kotlin.examples.webclient.standalone

import io.helidon.config.Config
import io.helidon.media.jsonp.JsonpSupport
import io.helidon.metrics.MetricsSupport
import io.helidon.webserver.Routing
import io.helidon.webserver.WebServer
import java.util.concurrent.CompletionStage

/**
 * The application main .
 */

var serverPort = 8080
    private set

/**
 * WebServer starting method.
 *
 * @param args starting arguments
 */

fun main() {
    startServer()
}

/**
 * Start the server.
 *
 * @return the created [WebServer] instance
 */

fun startServer(): CompletionStage<WebServer> {
    // By default this will pick up application.yaml from the classpath
    val config = Config.create()
    val server = WebServer.builder(createRouting(config))
        .config(config["server"])
        .addMediaSupport(JsonpSupport.create())
        .build()

    // Try to start the server. If successful, print some info and arrange to
    // print a message at shutdown. If unsuccessful, print the exception.
    val start: CompletionStage<WebServer> = server.start()
    start.thenAccept { ws: WebServer ->
        serverPort = ws.port()
        println("WEB server is up! http://localhost:" + ws.port() + "/greet")
        ws.whenShutdown().thenRun { println("WEB server is DOWN. Good bye!") }
    }.exceptionally { t: Throwable ->
        System.err.println("Startup failed: " + t.message)
        t.printStackTrace(System.err)
        null
    }

    // Server threads are not daemon. No need to block. Just react.
    return start
}

/**
 * Creates new [Routing].
 *
 * @param config configuration of this server
 * @return routing configured with JSON support, a health check, and a service
 */
private fun createRouting(config: Config): Routing {
    val metrics = MetricsSupport.create()
    val greetService = GreetService(config)
    return Routing.builder()
        .register(metrics)
        .register("/greet", greetService)
        .build()
}