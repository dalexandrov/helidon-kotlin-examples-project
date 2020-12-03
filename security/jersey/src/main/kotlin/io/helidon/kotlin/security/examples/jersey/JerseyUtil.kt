/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
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
package io.helidon.kotlin.security.examples.jersey

import io.helidon.webserver.Routing
import io.helidon.webserver.WebServer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier

/**
 * Utility for this example.
 */
internal object JerseyUtil {
    private const val START_TIMEOUT_SECONDS = 10
    fun startIt(routing: Supplier<out Routing?>?, port: Int): WebServer {
        val server = WebServer.builder(routing)
                .port(port)
                .build()
        val t = System.nanoTime()
        val cdl = CountDownLatch(1)
        val throwableRef = AtomicReference<Throwable>()
        server.start().whenComplete { webServer: WebServer, throwable: Throwable? ->
            if (null != throwable) {
                System.err.println("Failed to start server")
                throwableRef.set(throwable)
            } else {
                val time = System.nanoTime() - t
                System.out.printf("Server started in %d ms%n", TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS))
                System.out.printf("Started server on localhost:%d%n", webServer.port())
                println()
                println("Users:")
                println("jack/password in roles: user, admin")
                println("jill/password in roles: user")
                println("john/password in no roles")
                println()
                println("***********************")
                println("** Endpoints:        **")
                println("***********************")
                println("Unprotected:")
                System.out.printf("  http://localhost:%1\$d/rest%n", server.port())
                println("Protected:")
                System.out.printf("  http://localhost:%1\$d/rest/protected%n", server.port())
                println("Identity propagation:")
                System.out.printf("  http://localhost:%1\$d/rest/outbound%n", server.port())
            }
            cdl.countDown()
        }
        try {
            if (cdl.await(START_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)) {
                val thrown = throwableRef.get()
                if (null != thrown) {
                    throw RuntimeException("Failed to start server", thrown)
                }
            } else {
                throw RuntimeException("Failed to start server, timed out")
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Failed to start server within defined timeout: $START_TIMEOUT_SECONDS seconds")
        }
        return server
    }
}