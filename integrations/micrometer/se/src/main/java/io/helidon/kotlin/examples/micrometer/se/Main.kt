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
package io.helidon.kotlin.examples.micrometer.se

import io.helidon.common.LogConfig
import io.helidon.config.Config
import io.helidon.integrations.micrometer.MicrometerSupport
import io.helidon.media.jsonp.JsonpSupport
import io.helidon.webserver.Routing
import io.helidon.webserver.WebServer
import io.micrometer.core.instrument.Timer
import java.io.IOException
import java.lang.Runnable

/**
 * Simple Hello World rest application.
 */
/**
 * Application main entry point.
 * @param args command line arguments.
 */
fun main(args: Array<String>) {
    startServer()
}

/**
 * Start the server.
 * @return the created [WebServer] instance
 * @throws IOException if there are problems reading logging properties
 */
fun startServer(): WebServer {

    // load logging configuration
    LogConfig.configureRuntime()

    // By default this will pick up application.yaml from the classpath
    val config = Config.create()

    // Get webserver config from the "server" section of application.yaml
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
 * @return routing configured with JSON support, Micrometer metrics, and the greeting service
 * @param config configuration of this server
 */
private fun createRouting(config: Config): Routing {
    val micrometerSupport = MicrometerSupport.create()
    val personalizedGetCounter = micrometerSupport.registry()
        .counter("personalizedGets")
    val getTimer = Timer.builder("allGets")
        .publishPercentileHistogram()
        .register(micrometerSupport.registry())
    val greetService = GreetService(config, getTimer, personalizedGetCounter)
    return Routing.builder()
        .register(micrometerSupport) // Micrometer support at "/micrometer"
        .register("/greet", greetService)
        .build()
}