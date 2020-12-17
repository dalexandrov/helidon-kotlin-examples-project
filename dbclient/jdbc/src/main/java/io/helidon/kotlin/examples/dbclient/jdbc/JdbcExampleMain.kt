/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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
package io.helidon.kotlin.examples.dbclient.jdbc

import io.helidon.config.Config
import io.helidon.dbclient.DbClient
import io.helidon.dbclient.health.DbClientHealthCheck
import io.helidon.health.HealthSupport
import io.helidon.media.jsonb.JsonbSupport
import io.helidon.media.jsonp.JsonpSupport
import io.helidon.metrics.MetricsSupport
import io.helidon.tracing.TracerBuilder
import io.helidon.webserver.Routing
import io.helidon.webserver.WebServer
import java.io.IOException
import java.util.logging.LogManager

class JdbcExampleMain

/**
 * Simple Hello World rest application.
 */

fun main(args: Array<String>) {
    startServer()
}

/**
 * Start the server.
 *
 * @return the created [io.helidon.webserver.WebServer] instance
 */
fun startServer(): WebServer {

    // load logging configuration
    LogManager.getLogManager().readConfiguration(
        JdbcExampleMain::class.java.getResourceAsStream("/logging.properties")
    )

    // By default this will pick up application.yaml from the classpath
    val config = Config.create()

    // Prepare routing for the server
    val server = WebServer.builder()
        .routing(createRouting(config)) // Get webserver config from the "server" section of application.yaml
        .config(config["server"])
        .tracer(TracerBuilder.create(config["tracing"]))
        .addMediaSupport(JsonpSupport.create())
        .addMediaSupport(JsonbSupport.create())
        .build()

    // Start the server and print some info.
    server.start().thenAccept { ws: WebServer ->
        println(
            "WEB server is up! http://localhost:" + ws.port() + "/"
        )
    }

    // Server threads are not daemon. NO need to block. Just react.
    server.whenShutdown().thenRun { println("WEB server is DOWN. Good bye!") }
    return server
}

/**
 * Creates new [io.helidon.webserver.Routing].
 *
 * @param config configuration of this server
 * @return routing configured with JSON support, a health check, and a service
 */
private fun createRouting(config: Config): Routing {
    val dbConfig = config["db"]

    // Client services are added through a service loader - see mongoDB example for explicit services
    val dbClient = DbClient.builder(dbConfig)
        .build()
    val health = HealthSupport.builder()
        .addLiveness(DbClientHealthCheck.create(dbClient))
        .build()
    return Routing.builder()
        .register(health) // Health at "/health"
        .register(MetricsSupport.create()) // Metrics at "/metrics"
        .register("/db", PokemonService(dbClient))
        .build()
}
