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
package io.helidon.kotlin.security.examples.google

import io.helidon.common.Builder
import io.helidon.webserver.Routing
import io.helidon.webserver.WebServer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Google login example utilities.
 */
object GoogleUtil {
    // do not change this constant, unless you modify configuration
    // of Google application redirect URI
    const val PORT = 8080
    private const val START_TIMEOUT_SECONDS = 10
    fun startIt(port: Int, routing: Builder<out Routing>): WebServer {
        val server = WebServer.builder(routing)
                .port(port)
                .build()
        val t = System.nanoTime()
        val cdl = CountDownLatch(1)
        server.start().thenAccept { webServer: WebServer ->
            val time = System.nanoTime() - t
            System.out.printf("Server started in %d ms ms%n", TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS))
            System.out.printf("Started server on localhost:%d%n", webServer.port())
            System.out.printf("You can access this example at http://localhost:%d/index.html%n", webServer.port())
            println()
            println()
            println("Check application.yaml in case you are behind a proxy to configure it")
            cdl.countDown()
        }
        try {
            cdl.await(START_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            throw RuntimeException("Failed to start server within defined timeout: " + START_TIMEOUT_SECONDS + " seconds")
        }
        return server
    }
}