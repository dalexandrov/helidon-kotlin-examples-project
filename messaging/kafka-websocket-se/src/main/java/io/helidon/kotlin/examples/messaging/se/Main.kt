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
 *
 */
package io.helidon.kotlin.examples.messaging.se

import io.helidon.config.Config
import io.helidon.webserver.Routing
import io.helidon.webserver.StaticContentSupport
import io.helidon.webserver.WebServer
import io.helidon.webserver.tyrus.TyrusSupport
import java.io.IOException
import java.util.logging.LogManager
import javax.websocket.server.ServerEndpointConfig

/**
 * The application main class.
 */
object Main {
    /**
     * Application main entry point.
     *
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
     *
     * @return the created [WebServer] instance
     * @throws IOException if there are problems reading logging properties
     */
    @Throws(IOException::class)
    fun startServer(): WebServer {
        // load logging configuration
        setupLogging()

        // By default this will pick up application.yaml from the classpath
        val config = Config.create()
        val sendingService = SendingService(config)
        val server = WebServer.builder(createRouting(sendingService))
            .config(config["server"])
            .build()
        server.start()
            .thenAccept { ws: WebServer ->
                println(
                    "WEB server is up! http://localhost:" + ws.port()
                )
                ws.whenShutdown().thenRun {

                    // Stop messaging properly
                    sendingService.shutdown()
                    println("WEB server is DOWN. Good bye!")
                }
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
     * @param config configuration of this server
     * @return routing configured with JSON support, a health check, and a service
     */
    private fun createRouting(sendingService: SendingService): Routing {
        return Routing.builder() // register static content support (on "/")
            .register(
                StaticContentSupport.builder("/WEB").welcomeFileName("index.html")
            ) // register rest endpoint for sending to Kafka
            .register(
                "/rest/messages",
                sendingService
            ) // register WebSocket endpoint to push messages coming from Kafka to client
            .register(
                "/ws",
                TyrusSupport.builder().register(
                    ServerEndpointConfig.Builder.create(
                        WebSocketEndpoint::class.java, "/messages"
                    )
                        .build()
                )
                    .build()
            )
            .build()
    }

    /**
     * Configure logging from logging.properties file.
     */
    @Throws(IOException::class)
    private fun setupLogging() {
        Main::class.java.getResourceAsStream("/logging.properties")
            .use { `is` -> LogManager.getLogManager().readConfiguration(`is`) }
    }
}