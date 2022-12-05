/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.kotlin.webserver.examples.tls

import io.helidon.common.configurable.Resource
import io.helidon.config.Config
import io.helidon.webserver.*
import keystoreBuilder
import webServer
import webServerTls
import routing
import java.io.IOException
import java.util.concurrent.CompletionStage
import java.util.logging.LogManager

/**
 * Main class of TLS example.

 * Start the example.
 * This will start two Helidon WebServers, both protected by TLS - one configured from config, one using a builder.
 * Port of the servers will be configured from config, to be able to switch to an ephemeral port for tests.

 */
class Main

fun main() {
    setupLogging()
    val config = Config.create()
    startConfigBasedServer(config["config-based"])
        .thenAccept { ws: WebServer -> println("Started config based WebServer on http://localhost:" + ws.port()) }
    startBuilderBasedServer(config["builder-based"])
        .thenAccept { ws: WebServer -> println("Started builder based WebServer on http://localhost:" + ws.port()) }
}

fun startBuilderBasedServer(config: Config?): CompletionStage<WebServer> {
    return webServer {
        config(config)
        routing(routing()) // now let's configure TLS
        tls(
            webServerTls {
                privateKey(
                    keystoreBuilder {
                        keystore(Resource.create("certificate.p12"))
                        keystorePassphrase("helidon")
                    }
                )
            }
        )
    }
        .start()
}

fun startConfigBasedServer(config: Config?): CompletionStage<WebServer> {
    return webServer {
        config(config)
        routing(routing())
    }
        .start()
}

private fun routing(): Routing {
    return routing {
        get("/", Handler { _: ServerRequest?, res: ServerResponse -> res.send("Hello!") })
    }
}

/**
 * Configure logging from logging.properties file.
 */
@Throws(IOException::class)
private fun setupLogging() {
    Main::class.java.getResourceAsStream("/logging.properties").use(LogManager.getLogManager()::readConfiguration)
}