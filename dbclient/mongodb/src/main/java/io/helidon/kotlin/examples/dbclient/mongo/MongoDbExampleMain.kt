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
package io.helidon.kotlin.examples.dbclient.mongo

import io.helidon.config.Config
import io.helidon.dbclient.DbClient
import io.helidon.dbclient.DbStatementType
import io.helidon.dbclient.health.DbClientHealthCheck
import io.helidon.dbclient.metrics.DbClientMetrics
import io.helidon.dbclient.tracing.DbClientTracing
import io.helidon.health.HealthSupport
import io.helidon.media.jsonb.JsonbSupport
import io.helidon.media.jsonp.JsonpSupport
import io.helidon.metrics.MetricsSupport
import io.helidon.tracing.TracerBuilder
import io.helidon.webserver.Routing
import io.helidon.webserver.WebServer
import java.io.IOException
import java.util.logging.LogManager

class MongoDbExampleMain

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
        MongoDbExampleMain::class.java.getResourceAsStream("/logging.properties")
    )

    // By default this will pick up application.yaml from the classpath
    val config = Config.create()
    val server = WebServer.builder(createRouting(config))
        .config(config["server"])
        .tracer(TracerBuilder.create("mongo-db").build())
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
    val dbClient = DbClient.builder(dbConfig) // add an interceptor to named statement(s)
        .addService(
            DbClientMetrics.counter().statementNames("select-all", "select-one")
        ) // add an interceptor to statement type(s)
        .addService(
            DbClientMetrics.timer()
                .statementTypes(DbStatementType.DELETE, DbStatementType.UPDATE, DbStatementType.INSERT)
        ) // add an interceptor to all statements
        .addService(DbClientTracing.create())
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

private fun noConfigError(key: String): IllegalStateException {
    return IllegalStateException(
        "Attempting to create a Pokemon service with no configuration"
                + ", config key: " + key
    )
}
