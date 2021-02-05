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

import io.helidon.common.configurable.Resource
import io.helidon.common.http.Http
import io.helidon.common.pki.KeyConfig
import io.helidon.webserver.*


/**
 * Start the example.
 * This will start Helidon [WebServer] which is configured by the [WebServer.Builder].
 * There will be two sockets running:
 *
 *
 *  * `8080` - without TLS protection
 *  * `443` - with TLS protection
 *
 *
 * Both of the ports mentioned above are default ports for this example and can be changed by updating
 * values in this method.
 *
 */
fun main() {
    startServer(8080, 443)
}

fun startServer(unsecured: Int, secured: Int): WebServer {
    val socketConf = SocketConfiguration.builder()
        .name("secured")
        .port(secured)
        .tls(tlsConfig())
        .build()
    val webServer = WebServer.builder()
        .port(unsecured)
        .routing(createPlainRouting())
        .addSocket(socketConf, createMtlsRouting())
        .build()
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

private fun tlsConfig(): WebServerTls {
    val keyConfig = KeyConfig.keystoreBuilder()
        .trustStore()
        .keystore(Resource.create("server.p12"))
        .keystorePassphrase("password")
        .build()
    return WebServerTls.builder()
        .clientAuth(ClientAuthentication.REQUIRE)
        .trust(keyConfig)
        .privateKey(keyConfig)
        .build()
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
