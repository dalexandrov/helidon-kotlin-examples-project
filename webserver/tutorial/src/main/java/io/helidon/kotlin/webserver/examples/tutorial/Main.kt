/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates.
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
package io.helidon.kotlin.webserver.examples.tutorial

import io.helidon.common.http.MediaType
import io.helidon.kotlin.webserver.examples.tutorial.user.UserFilter
import io.helidon.webserver.*
import routing
import webServer

/**
 * Application java main class.
 *
 *
 * The TUTORIAL application demonstrates various WebServer use cases together and in its complexity.
 *
 * It also serves web server tutorial articles composed from life examples.
 */

fun createRouting(): Routing {
    val upperXFilter = UpperXFilter()
    return routing {
        any(UserFilter())
        any(Handler { req: ServerRequest, res: ServerResponse ->
            res.registerFilter(upperXFilter)
            req.next()
        })
        register("/article", CommentService())
        post("/mgmt/shutdown", Handler { req: ServerRequest, res: ServerResponse ->
            res.headers().contentType(MediaType.TEXT_PLAIN.withCharset("UTF-8"))
            res.send("Shutting down TUTORIAL server. Good bye!\n")
            // Use reactive API nature to stop the server AFTER the response was sent.
            res.whenSent().thenRun { req.webServer().shutdown() }
        })
    }
}

/**
 * A java main class.
 *
 */
fun main(args: Array<String>) {
    // Create a web server instance
    var port = 8080
    if (args.isNotEmpty()) {
        port = try {
            args[0].toInt()
        } catch (nfe: NumberFormatException) {
            0
        }
    }
    val server = webServer {
        routing(createRouting())
        port(port)
    }

    // Start the server and print some info.
    server.start().thenAccept { ws: WebServer ->
        println("TUTORIAL server is up! http://localhost:" + ws.port())
        println("Call POST on 'http://localhost:" + ws.port() + "/mgmt/shutdown' to STOP the server!")
    }

    // Server threads are not demon. NO need to block. Just react.
    server.whenShutdown()
        .thenRun { println("TUTORIAL server is DOWN. Good bye!") }
}
