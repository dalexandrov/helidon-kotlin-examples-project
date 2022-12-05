/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
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
package io.helidon.security.examples.idcs

import io.helidon.webserver.Routing
import io.helidon.webserver.WebServer
import webServer
import java.net.UnknownHostException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

/**
 * IDCS login example utilities.
 */
object IdcsUtil {
    // do not change this constant, unless you modify configuration
    // of IDCS application redirect URI
    private const val PORT = 7987
    private const val START_TIMEOUT_SECONDS = 10

    @Throws(UnknownHostException::class)
    fun startIt(routeSetup: Routing): WebServer {
        return webServer {
            routing(routeSetup)
            port(PORT)
            bindAddress("localhost")
        }
    }

    fun start(webServer: WebServer): WebServer {
        val t = System.nanoTime()
        val cdl = CountDownLatch(1)
        webServer.start()
            .thenAccept { whenStarted(it, t) }
            .thenRun { cdl.countDown() }
        try {
            cdl.await(START_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            throw RuntimeException("Failed to start server within defined timeout: $START_TIMEOUT_SECONDS seconds", e)
        }
        return webServer
    }

    private fun whenStarted(webServer: WebServer, startNanoTime: Long) {
        val time = System.nanoTime() - startNanoTime
        System.out.printf("Server started in %d ms%n", TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS))
        System.out.printf("Started server on localhost:%d%n", webServer.port())
        System.out.printf("You can access this example at http://localhost:%d/rest/profile%n", webServer.port())
        println()
        println()
        println("Check application.yaml in case you are behind a proxy to configure it")
    }
}