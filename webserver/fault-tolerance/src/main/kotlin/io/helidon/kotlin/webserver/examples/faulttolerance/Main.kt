/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.kotlin.webserver.examples.faulttolerance

import io.helidon.common.LogConfig
import io.helidon.common.http.Http
import io.helidon.common.reactive.Single
import io.helidon.faulttolerance.BulkheadException
import io.helidon.faulttolerance.CircuitBreakerOpenException
import io.helidon.webserver.Routing
import io.helidon.webserver.ServerRequest
import io.helidon.webserver.ServerResponse
import io.helidon.webserver.WebServer
import java.util.concurrent.TimeoutException

/**
 * Main class of Fault tolerance example.
 */

fun main() {
    LogConfig.configureRuntime()
    startServer(8079).thenRun {}
}

fun startServer(port: Int): Single<WebServer> {
    return WebServer.builder()
        .routing(routing())
        .port(port)
        .build()
        .start()
        .peek { server: WebServer ->
            val url = "http://localhost:" + server.port()
            println("Server started on $url")
        }
}

private fun routing(): Routing {
    return Routing.builder()
        .register("/ft", FtService())
        .error(
            BulkheadException::class.java
        ) { _: ServerRequest?, res: ServerResponse, _: BulkheadException? ->
            res.status(Http.Status.SERVICE_UNAVAILABLE_503).send("bulkhead")
        }
        .error(
            CircuitBreakerOpenException::class.java
        ) { _: ServerRequest?, res: ServerResponse, _: CircuitBreakerOpenException? ->
            res.status(Http.Status.SERVICE_UNAVAILABLE_503).send("circuit breaker")
        }
        .error(
            TimeoutException::class.java
        ) { _: ServerRequest?, res: ServerResponse, _: TimeoutException? ->
            res.status(Http.Status.REQUEST_TIMEOUT_408).send("timeout")
        }
        .error(
            Throwable::class.java
        ) { _: ServerRequest?, res: ServerResponse, ex: Throwable ->
            res.status(Http.Status.INTERNAL_SERVER_ERROR_500)
                .send(ex.javaClass.name + ": " + ex.message)
        }
        .build()
}