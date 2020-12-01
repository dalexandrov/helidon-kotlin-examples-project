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
package io.helidon.kotlin.security.examples.outbound

import io.helidon.security.Principal
import io.helidon.security.SecurityContext
import io.helidon.security.Subject
import io.helidon.security.integration.webserver.WebSecurity
import io.helidon.security.providers.jwt.JwtProvider
import io.helidon.webserver.*
import java.util.concurrent.CompletionStage

/**
 * Creates two services. First service invokes the second with outbound security. There are two endpoints - one that
 * does simple identity propagation and one that uses an explicit username.
 *
 * Uses basic authentication to authenticate users and JWT to propagate identity.
 *
 * The difference between this example and basic authentication example:
 *
 *  * Configuration files (this example uses ones with -jwt.yaml suffix)
 *  * Property name used in [.override] method to override username
 *
 */
object OutboundOverrideJwtExample {
    @Volatile
    private var clientPort = 0

    @Volatile
    private var servingPort = 0

    /**
     * Example that propagates identity and on one endpoint explicitly sets the username and password.
     *
     * @param args ignored
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val first = startClientService()
        val second = startServingService()
        first.toCompletableFuture().join()
        second.toCompletableFuture().join()
        println("Started services. Main endpoints:")
        println("http://localhost:$clientPort/propagate")
        println("http://localhost:$clientPort/override")
        println()
        println("Backend service started on:")
        println("http://localhost:$servingPort/hello")
    }

    private fun startServingService(): CompletionStage<Void> {
        val config = OutboundOverrideUtil.createConfig("serving-service-jwt")
        val routing = Routing.builder()
                .register(WebSecurity.create(config["security"]))["/hello", Handler { req: ServerRequest, res: ServerResponse ->
            // This is the token. It should be bearer <signed JWT base64 encoded>
            req.headers().first("Authorization")
                    .ifPresent { x: String? -> println(x) }
            res.send(req.context().get(SecurityContext::class.java).flatMap { obj: SecurityContext -> obj.user() }.map { obj: Subject -> obj.principal() }.map { obj: Principal -> obj.name }.orElse("Anonymous"))
        }].build()
        return OutboundOverrideUtil.startServer(routing, 9080, { server: WebServer -> servingPort = server.port() })
    }

    private fun startClientService(): CompletionStage<Void> {
        val config = OutboundOverrideUtil.createConfig("client-service-jwt")
        val routing = Routing.builder()
                .register(WebSecurity.create(config["security"]))["/override", Handler { req: ServerRequest, res: ServerResponse -> override(req,res) }]["/propagate", Handler { req: ServerRequest, res: ServerResponse -> propagate(req,res) }]
                .build()
        return OutboundOverrideUtil.startServer(routing, 8080, { server: WebServer -> clientPort = server.port() })
    }

    private fun override(req: ServerRequest, res: ServerResponse) {
        val context = OutboundOverrideUtil.getSecurityContext(req)
        OutboundOverrideUtil.webTarget(servingPort)
                .property(JwtProvider.EP_PROPERTY_OUTBOUND_USER, "jill")
                .request(String::class.java)
                .thenAccept { result: String -> res.send("You are: " + context.userName() + ", backend service returned: " + result) }
                .exceptionally { throwable: Throwable? -> OutboundOverrideUtil.sendError(throwable, res) }
    }

    private fun propagate(req: ServerRequest, res: ServerResponse) {
        val context = OutboundOverrideUtil.getSecurityContext(req)
        OutboundOverrideUtil.webTarget(servingPort)
                .request(String::class.java)
                .thenAccept { result: String -> res.send("You are: " + context.userName() + ", backend service returned: " + result) }
                .exceptionally { throwable: Throwable? -> OutboundOverrideUtil.sendError(throwable, res) }
    }
}