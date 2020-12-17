/*
 * Copyright (c) 2017, 2020 Oracle and/or its affiliates.
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
package io.helidon.kotlin.webserver.examples.comments

import io.helidon.common.http.Http
import io.helidon.config.Config
import io.helidon.webserver.*
import java.util.*
import java.util.concurrent.CompletionException

/**
 * Application java main class.
 *
 *
 *
 * The COMMENTS-As-a-Service application example demonstrates Web Server in its integration role.
 * It integrates various components including *Configuration* and *Security*.
 *
 *
 *
 * This WEB application provides possibility to store and read comment related to various topics.
 */

fun main() {
    // Load configuration
    val config = Config.create()
    val acceptAnonymousUsers = config["anonymous-enabled"].asBoolean().orElse(false)
    val server = WebServer.create(
        createRouting(acceptAnonymousUsers),
        config["webserver"]
    )

    // Start the server and print some info.
    server.start().thenAccept { ws: WebServer ->
        println(
            "WEB server is up! http://localhost:" + ws.port() + "/comments"
        )
    }
    server.whenShutdown()
        .thenRun { println("WEB server is DOWN. Good bye!") }
}

fun createRouting(acceptAnonymousUsers: Boolean): Routing {
    return Routing.builder() // Filter that translates user identity header into the contextual "user" information
        .any(Handler { req: ServerRequest, _: ServerResponse? ->
            val user = req.headers().first("user-identity")
                .or { if (acceptAnonymousUsers) Optional.of("anonymous") else Optional.empty() }
                .orElseThrow { HttpException("Anonymous access is forbidden!", Http.Status.FORBIDDEN_403) }
            req.context().register("user", user)
            req.next()
        }) // Main service logic part is registered as a separated class to "/comments" context root
        .register("/comments", CommentsService()) // Error handling for argot expressions.
        .error(CompletionException::class.java) { req: ServerRequest, _: ServerResponse?, ex: CompletionException -> req.next(ex.cause) }
        .error(ProfanityException::class.java) { _: ServerRequest?, res: ServerResponse, ex: ProfanityException ->
            res.status(Http.Status.NOT_ACCEPTABLE_406)
            res.send("Expressions like '" + ex.obfuscatedProfanity + "' are unacceptable!")
        }
        .error(HttpException::class.java) { req: ServerRequest, res: ServerResponse, ex: HttpException ->
            if (ex.status() === Http.Status.FORBIDDEN_403) {
                res.status(ex.status())
                res.send(ex.message)
            } else {
                req.next()
            }
        }
        .build()
}