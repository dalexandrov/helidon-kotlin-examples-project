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
package io.helidon.kotlin.webserver.examples.staticcontent

import io.helidon.common.http.Http
import io.helidon.media.jsonp.JsonpSupport
import io.helidon.webserver.*

/**
 * Application demonstrates combination of the static content with a simple REST API. It counts accesses and display it
 * on the WEB page.
 */


private fun createRouting(): Routing {
    return Routing.builder()
        .any("/", Handler { _: ServerRequest?, res: ServerResponse ->
            // showing the capability to run on any path, and redirecting from root
            res.status(Http.Status.MOVED_PERMANENTLY_301)
            res.headers().put(Http.Header.LOCATION, "/ui")
            res.send()
        })
        .register("/ui", CounterService())
        .register(
            "/ui", StaticContentSupport.builder("WEB")
                .welcomeFileName("index.html")
                .build()
        )
        .build()
}


fun main() {
    val server = WebServer.builder(createRouting())
        .port(8080)
        .addMediaSupport(JsonpSupport.create())
        .build()

    // Start the server and print some info.
    server.start().thenAccept { ws: WebServer -> println("WEB server is up! http://localhost:" + ws.port()) }

    // Server threads are not demon. NO need to block. Just react.
    server.whenShutdown()
        .thenRun { println("WEB server is DOWN. Good bye!") }
}
