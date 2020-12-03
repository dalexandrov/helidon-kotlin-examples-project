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
package io.helidon.kotlin.examples.health.basics

import io.helidon.health.HealthSupport
import io.helidon.health.checks.HealthChecks
import io.helidon.webserver.*
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse

/**
 * Main class of health check integration example.
 */
object Main {
    /**
     * Start the example. Prints endpoints to standard output.
     *
     * @param args not used
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val health = HealthSupport.builder()
                .addLiveness(*HealthChecks.healthChecks())
                .addReadiness(HealthCheck {
                    HealthCheckResponse.named("exampleHealthCheck")
                            .up()
                            .withData("time", System.currentTimeMillis())
                            .build()
                })
                .build()
        val routing = Routing.builder()
                .register(health)["/hello", Handler { _: ServerRequest?, res: ServerResponse -> res.send("Hello World!") }]
                .build()
        val ws = WebServer.create(routing)
        ws.start()
                .thenApply<Any?> { webServer: WebServer ->
                    val endpoint = "http://localhost:" + webServer.port()
                    println("Hello World started on $endpoint/hello")
                    println("Health checks available on $endpoint/health")
                    null
                }
    }
}