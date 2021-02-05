/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates.
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
package io.helidon.kotlin.examples.openapi

import io.helidon.config.Config
import io.helidon.health.HealthSupport
import io.helidon.health.checks.HealthChecks
import io.helidon.media.jsonp.JsonpSupport
import io.helidon.metrics.MetricsSupport
import io.helidon.openapi.OpenAPISupport
import io.helidon.webserver.Routing
import io.helidon.webserver.WebServer
import java.io.IOException
import java.util.logging.LogManager

/**
 * Simple Hello World rest application.
 */
class Main

fun main() {
    startServer()
}

/**
 * Start the server.
 * @return the created [WebServer] instance
 * @throws IOException if there are problems reading logging properties
 */

fun startServer(): WebServer {

    // load logging configuration
    LogManager.getLogManager().readConfiguration(
        Main::class.java.getResourceAsStream("/logging.properties")
    )

    // By default this will pick up application.yaml from the classpath
    val config = Config.create()

    // Get webserver config from the "server" section of application.yaml and register JSON support
    val server = WebServer.builder(createRouting(config))
        .config(config["server"])
        .addMediaSupport(JsonpSupport.create())
        .build()

    // Try to start the server. If successful, print some info and arrange to
    // print a message at shutdown. If unsuccessful, print the exception.
    server.start()
        .thenAccept { ws: WebServer ->
            println(
                "WEB server is up! http://localhost:" + ws.port() + "/greet"
            )
            ws.whenShutdown().thenRun { println("WEB server is DOWN. Good bye!") }
        }
        .exceptionally { t: Throwable ->
            System.err.println("Startup failed: " + t.message)
            t.printStackTrace(System.err)
            null
        }

    // Server threads are not daemon. No need to block. Just react.
    return server
}

/**
 * Creates new [Routing].
 *
 * @return routing configured with a health check, and a service
 * @param config configuration of this server
 */
private fun createRouting(config: Config): Routing {
    val metrics = MetricsSupport.create()
    val greetService = GreetService(config)
    val health = HealthSupport.builder()
        .addLiveness(*HealthChecks.healthChecks()) // Adds a convenient set of checks
        .build()
    return Routing.builder()
        .register(OpenAPISupport.create(config[OpenAPISupport.Builder.CONFIG_KEY]))
        .register(health) // Health at "/health"
        .register(metrics) // Metrics at "/metrics"
        .register("/greet", greetService)
        .build()
}
