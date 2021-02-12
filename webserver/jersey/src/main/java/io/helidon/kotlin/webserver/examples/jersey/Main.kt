/*
 * Copyright (c) 2017, 2021 Oracle and/or its affiliates.
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
package io.helidon.kotlin.webserver.examples.jersey

import io.helidon.webserver.WebServer
import io.helidon.webserver.jersey.JerseySupport
import org.glassfish.jersey.server.ResourceConfig
import webServer
import routing as setRouting
import java.util.concurrent.CompletionStage
import java.util.logging.LogManager

/**
 * The WebServer Jersey Main example class.
 *
 * @see .main
 * @see .startServer
 */
class Main

fun main() {
    // configure logging in order to not have the standard JVM defaults
    LogManager.getLogManager().readConfiguration(Main::class.java.getResourceAsStream("/logging.properties"))

    // start the server on port 8080
    startServer(8080)
}

/**
 * Start the WebServer based on the provided configuration. When running from
 * a test, pass to have a dynamically allocated port
 * the server listens on.
 *
 * @param port port to start server on
 * @return a completion stage indicating that the server has started and is ready to
 * accept http requests
 */
fun startServer(port: Int): CompletionStage<WebServer> {
    val webServer = webServer {
        routing(setRouting {  // register a Jersey Application at the '/jersey' context path
            register(
                "/jersey",
                JerseySupport.create(ResourceConfig(HelloWorld::class.java))
            )
        })
        port(port)
    }
    return webServer.start()
        .whenComplete { server: WebServer, _: Throwable? ->
            println("Jersey WebServer started.")
            println("To stop the application, hit CTRL+C")
            println(
                "Try the hello world resource at: http://localhost:" + server
                    .port() + "/jersey/hello"
            )
        }
}
