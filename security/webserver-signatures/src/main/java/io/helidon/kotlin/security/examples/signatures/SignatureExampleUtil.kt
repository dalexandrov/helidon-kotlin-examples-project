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
package io.helidon.kotlin.security.examples.signatures


import io.helidon.common.http.Http
import io.helidon.common.http.MediaType
import io.helidon.security.SecurityContext
import io.helidon.webclient.security.WebClientSecurity
import io.helidon.webserver.Routing
import io.helidon.webserver.ServerRequest
import io.helidon.webserver.ServerResponse
import io.helidon.webserver.WebServer
import single
import webClient
import webServer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Common code for both examples (builder and config based).
 */
internal object SignatureExampleUtil {

    @JvmStatic
    private val CLIENT = webClient {
        addService(WebClientSecurity.create())
    }

    private const val START_TIMEOUT_SECONDS = 10

    /**
     * Start a web server.
     *
     * @param routing routing to configre
     * @return started web server instance
     */
    fun startServer(routing: Routing?, port: Int): WebServer {
        val server = webServer {
            routing(routing)
            port(port)
        }
        val t = System.nanoTime()
        val cdl = CountDownLatch(1)
        server.start().thenAccept { webServer: WebServer ->
            val time = System.nanoTime() - t
            System.out.printf("Server started in %d ms ms%n", TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS))
            System.out.printf("Started server on localhost:%d%n", webServer.port())
            println()
            cdl.countDown()
        }.exceptionally { throwable: Throwable? -> throw RuntimeException("Failed to start server", throwable) }
        try {
        cdl.await(START_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            throw RuntimeException("Failed to start server within defined timeout: " + START_TIMEOUT_SECONDS + " seconds")
        }
        return server
    }

    fun processService1Request(req: ServerRequest, res: ServerResponse, path: String, svc2port: Int) {
        val securityContext = req.context().get(
            SecurityContext::class.java
        )
        res.headers().contentType(MediaType.TEXT_PLAIN.withCharset("UTF-8"))
        securityContext.ifPresentOrElse({
            CLIENT.get()
                .uri("http://localhost:$svc2port$path")
                .request()
                .thenAccept {
                    if (it.status() == Http.Status.OK_200) {
                        it.content()
                            .single<String>()
                            .thenAccept { res.send(it) }
                            .exceptionally {
                                res.send("Getting server response failed!")
                                null
                            }
                    } else {
                        res.send("Request failed, status: " + it.status())
                    }
                }
        }) { res.send("Security context is null") }
    }
}