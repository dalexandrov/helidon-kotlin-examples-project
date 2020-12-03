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
package io.helidon.kotlin.examples.integrations.neo4j.se

import io.helidon.common.LogConfig
import io.helidon.config.Config
import io.helidon.kotlin.examples.integrations.neo4j.se.domain.MovieRepository
import io.helidon.health.HealthSupport
import io.helidon.health.checks.HealthChecks
import io.helidon.integrations.neo4j.Neo4j
import io.helidon.integrations.neo4j.health.Neo4jHealthCheck
import io.helidon.integrations.neo4j.metrics.Neo4jMetricsSupport
import io.helidon.media.jsonb.JsonbSupport
import io.helidon.media.jsonp.JsonpSupport
import io.helidon.metrics.MetricsSupport
import io.helidon.webserver.Routing
import io.helidon.webserver.WebServer
import java.io.IOException
import java.util.logging.LogManager

/**
 * The application main class.
 */
object Main {
    /**
     * Application main entry point.
     * @param args command line arguments.
     * @throws IOException if there are problems reading logging properties
     */
    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        startServer()
    }

    /**
     * Start the server.
     * @return the created [WebServer] instance
     * @throws IOException if there are problems reading logging properties
     */
    @JvmStatic
    @Throws(IOException::class)
    fun startServer(): WebServer {
        // load logging configuration
        LogConfig.configureRuntime()

        // By default this will pick up application.yaml from the classpath
        val config = Config.create()
        val server = WebServer.builder(createRouting(config))
                .config(config["server"])
                .addMediaSupport(JsonpSupport.create())
                .addMediaSupport(JsonbSupport.create())
                .build()

        // Try to start the server. If successful, print some info and arrange to
        // print a message at shutdown. If unsuccessful, print the exception.
        server.start()
                .thenAccept { ws: WebServer ->
                    println(
                            "WEB server is up! http://localhost:" + ws.port() + "/api/movies")
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
     * @return routing configured with JSON support, a health check, and a service
     * @param config configuration of this server
     */
    private fun createRouting(config: Config): Routing {
        val metrics = MetricsSupport.create()
        val neo4j = Neo4j.create(config["neo4j"])

        // registers all metrics
        Neo4jMetricsSupport.builder()
                .driver(neo4j.driver())
                .build()
                .initialize()
        val healthCheck = Neo4jHealthCheck.create(neo4j.driver())
        val neo4jDriver = neo4j.driver()
        val movieService = MovieService(MovieRepository(neo4jDriver))
        val health = HealthSupport.builder()
                .addLiveness(*HealthChecks.healthChecks()) // Adds a convenient set of checks
                .addReadiness(healthCheck)
                .build()
        return Routing.builder()
                .register(health) // Health at "/health"
                .register(metrics) // Metrics at "/metrics"
                .register(movieService)
                .build()
    }

    /**
     * Configure logging from logging.properties file.
     */
    @Throws(IOException::class)
    private fun setupLogging() {
        Main::class.java.getResourceAsStream("/logging.properties").use { `is` -> LogManager.getLogManager().readConfiguration(`is`) }
    }
}