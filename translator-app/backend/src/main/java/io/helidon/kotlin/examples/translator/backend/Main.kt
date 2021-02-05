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
package io.helidon.kotlin.examples.translator.backend

import io.helidon.config.Config
import io.helidon.config.ConfigSources
import io.helidon.tracing.TracerBuilder
import io.helidon.webserver.Routing
import io.helidon.webserver.WebServer
import java.util.concurrent.CompletionStage
import java.util.logging.LogManager

/**
 * Translator application backend main class.
 */
class Main

fun startBackendServer(): CompletionStage<WebServer?> {
    // configure logging in order to not have the standard JVM defaults
    LogManager.getLogManager().readConfiguration(Main::class.java.getResourceAsStream("/logging.properties"))
    val config = Config.builder()
        .sources(ConfigSources.environmentVariables())
        .build()
    val webServer = WebServer.builder(
        Routing.builder()
            .register(TranslatorBackendService())
    )
        .port(9080)
        .tracer(
            TracerBuilder.create(config["tracing"])
                .serviceName("helidon-webserver-translator-backend")
                .build()
        )
        .build()
    return webServer.start()
        .thenApply { ws: WebServer ->
            println(
                "WEB server is up! http://localhost:" + ws.port()
            )
            ws.whenShutdown().thenRun { println("WEB server is DOWN. Good bye!") }
            ws
        }.exceptionally { t: Throwable ->
            System.err.println("Startup failed: " + t.message)
            t.printStackTrace(System.err)
            null
        }
}

/**
 * The main method of Translator backend.
 *
 */

fun main() {
    startBackendServer()
}
