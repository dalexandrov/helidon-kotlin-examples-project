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
package io.helidon.kotlin.webserver.examples.mtls

import io.helidon.common.http.Http
import io.helidon.config.Config
import io.helidon.webserver.*
import webServer


/**
 * Start the example.
 * This will start Helidon [WebServer] which is configured by the configuration.
 * There will be two sockets running:
 *
 *
 *  * `8080` - without TLS protection
 *  * `443` - with TLS protection
 *
 *
 * Both of the ports mentioned above are default ports for this example and can be changed via configuration file.
 *
 */
fun main() {
    val config = Config.create()
    startServer(config["server"])
}

fun startServer(config: Config?): WebServer {
    val webServer = webServer {
        routing(createPlainRouting())
        config(config)
        addNamedRouting("secured", createMtlsRouting())
    }
    webServer.start()
        .thenAccept { ws: WebServer ->
            println("WebServer is up!")
            println("Unsecured: http://localhost:" + ws.port() + "/")
            println("Secured: https://localhost:" + ws.port("secured") + "/")
            ws.whenShutdown().thenRun { println("WEB server is DOWN. Good bye!") }
        }
        .exceptionally { t: Throwable ->
            System.err.println("Startup failed: " + t.message)
            t.printStackTrace(System.err)
            null
        }
    return webServer
}

private fun createPlainRouting(): Routing {
    return Routing.builder()["/", Handler { _: ServerRequest?, res: ServerResponse -> res.send("Hello world unsecured!") }]
        .build()
}

private fun createMtlsRouting(): Routing {
    return Routing.builder()["/", Handler { req: ServerRequest, res: ServerResponse ->
        val cn = req.headers().first(Http.Header.X_HELIDON_CN).orElse("Unknown CN")
        res.send("Hello $cn!")
    }]
        .build()
}
