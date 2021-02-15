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
package io.helidon.kotlin.webserver.examples.opentracing

import asSingle
import io.helidon.config.Config
import io.helidon.config.ConfigSources
import io.helidon.tracing.TracerBuilder
import io.helidon.webserver.*
import webServer
import routing as routingBuilder
import java.util.logging.LogManager

/**
 * The ZipkinExampleMain is an app that leverages a use of Open Tracing and sends
 * the collected data to Zipkin.
 *
 * @see io.helidon.tracing.TracerBuilder
 *
 * @see io.helidon.tracing.zipkin.ZipkinTracerBuilder
 */
class Main

fun main() {

    // configure logging in order to not have the standard JVM defaults
    LogManager.getLogManager().readConfiguration(Main::class.java.getResourceAsStream("/logging.properties"))
    val config = Config.builder()
        .sources(ConfigSources.environmentVariables())
        .build()
    val webServer = webServer {
        routing(routingBuilder {
            any(Handler { req: ServerRequest, _: ServerResponse? ->
                println("Received another request.")
                req.next()
            })
            get("/test", Handler { _: ServerRequest?, res: ServerResponse -> res.send("Hello World!") })
            post("/hello", Handler { req: ServerRequest, res: ServerResponse ->
                req.content()
                    .asSingle(String::class.java)
                    .thenAccept { s: String -> res.send("Hello: $s") }
                    .exceptionally { t: Throwable? ->
                        req.next(t)
                        null
                    }
            })
        })
            port(8080)
            tracer(
                TracerBuilder.create(config["tracing"])
                    .serviceName("demo-first")
                    .registerGlobal(true)
                    .build()
            )
    }
    webServer.start()
        .whenComplete { server: WebServer, _: Throwable -> println("Started at http://localhost:" + server.port()) }
}