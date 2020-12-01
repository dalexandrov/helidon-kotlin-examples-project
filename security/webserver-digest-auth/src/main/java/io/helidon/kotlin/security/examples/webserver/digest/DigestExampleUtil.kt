/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.security.examples.webserver.digest

import io.helidon.webserver.Routing
import io.helidon.webserver.WebServer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Utility for this example.
 */
internal object DigestExampleUtil {
    private const val START_TIMEOUT_SECONDS = 10
    fun startServer(routing: Routing?): WebServer {
        val server = WebServer.create(routing)
        val t = System.nanoTime()
        val cdl = CountDownLatch(1)
        server.start().thenAccept { webServer: WebServer ->
            val time = System.nanoTime() - t
            System.out.printf("Server started in %d ms ms%n", TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS))
            System.out.printf("Started server on localhost:%d%n", webServer.port())
            println()
            println("Users:")
            println("Jack/password in roles: user, admin")
            println("Jill/password in roles: user")
            println("John/password in no roles")
            println()
            println("***********************")
            println("** Endpoints:        **")
            println("***********************")
            println("No authentication:")
            System.out.printf("  http://localhost:%1\$d/public%n", webServer.port())
            println("No roles required, authenticated:")
            System.out.printf("  http://localhost:%1\$d/noRoles%n", webServer.port())
            println("User role required:")
            System.out.printf("  http://localhost:%1\$d/user%n", webServer.port())
            println("Admin role required:")
            System.out.printf("  http://localhost:%1\$d/admin%n", webServer.port())
            println("Always forbidden (uses role nobody is in), audited:")
            System.out.printf("  http://localhost:%1\$d/deny%n", webServer.port())
            println("Admin role required, authenticated, authentication optional, audited (always forbidden - challenge is not "
                    + "returned as authentication is optional):")
            System.out.printf("  http://localhost:%1\$d/noAuthn%n", webServer.port())
            println()
            cdl.countDown()
        }
        try {
            cdl.await(START_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            throw RuntimeException("Failed to start server within defined timeout: " + START_TIMEOUT_SECONDS + " seconds", e)
        }
        return server
    }
}