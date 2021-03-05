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
package io.helidon.kotlin.examples.dbclient.pokemons

import dbClient
import healthSupport
import io.helidon.config.Config
import io.helidon.config.ConfigSources
import io.helidon.dbclient.DbClient
import io.helidon.dbclient.health.DbClientHealthCheck
import io.helidon.health.HealthSupport
import io.helidon.kotlin.examples.dbclient.pokemons.InitializeDb.Companion.init
import io.helidon.media.jsonb.JsonbSupport
import io.helidon.media.jsonp.JsonpSupport
import io.helidon.metrics.MetricsSupport
import io.helidon.tracing.TracerBuilder
import io.helidon.webserver.Routing
import io.helidon.webserver.WebServer
import routing
import webServer
import java.io.IOException
import java.util.logging.LogManager

/** MongoDB configuration. Default configuration file `appliaction.yaml` contains JDBC configuration.  */
private const val MONGO_CFG = "mongo.yaml"

/** Whether MongoDB support is selected.  */
var isMongo = false
    private set

/**
 * Application main entry point.
 *
 * @param args Command line arguments. Run with MongoDB support when 1st argument is mongo, run with JDBC support otherwise.
 * @throws java.io.IOException if there are problems reading logging properties
 */
fun main(args: Array<String>) {
    isMongo = if (args != null && args.isNotEmpty() && args[0] != null && "mongo" == args[0].toLowerCase()) {
        println("MongoDB database selected")
        true
    } else {
        println("JDBC database selected")
        false
    }
    startServer()
}

/**
 * Start the server.
 *
 * @return the created [io.helidon.webserver.WebServer] instance
 * @throws java.io.IOException if there are problems reading logging properties
 */
@Throws(IOException::class)
fun startServer(): WebServer {

    // load logging configuration
    LogManager.getLogManager().readConfiguration({}::class.java.getResourceAsStream("/logging.properties"))

    // By default this will pick up application.yaml from the classpath
    val config = if (isMongo) Config.create(ConfigSources.classpath(MONGO_CFG)) else Config.create()

    // Prepare routing for the server
    val routing = createRouting(config)
    val server = webServer {
        routing(routing)
        addMediaSupport(JsonpSupport.create())
        addMediaSupport(JsonbSupport.create())
        config(config["server"])
        tracer(TracerBuilder.create(config["tracing"]).build())
    }

    // Start the server and print some info.
    server.start()
        .thenAccept { ws: WebServer -> println("WEB server is up! http://localhost:" + ws.port() + "/") }

    // Server threads are not daemon. NO need to block. Just react.
    server.whenShutdown()
        .thenRun { println("WEB server is DOWN. Good bye!") }
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
    val dbClient = dbClient {
        config(dbConfig)
    }
    val health = healthSupport {
        addLiveness(DbClientHealthCheck.create(dbClient))
    }

    // Initialize database schema
    init(dbClient)
    return routing {
        register(health) // Health at "/health"
        register(MetricsSupport.create()) // Metrics at "/metrics"
        register("/db", PokemonService(dbClient))
    }
}
