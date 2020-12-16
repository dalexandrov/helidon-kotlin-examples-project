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
package io.helidon.kotlin.microprofile.example.staticc

import io.helidon.microprofile.server.Server
import java.util.concurrent.TimeUnit

/**
 * Main class to start the application.
 * See resources/META-INF/microprofile-config.properties.
 */

var port = 0
    private set

/**
 * Run this example.
 *
 * @param args command line arguments (ignored)
 */
fun main(args: Array<String?>) {
    var now = System.nanoTime()

    // everything is configured through application.yaml
    val server = Server.create()
    now = System.nanoTime() - now
    println("Create server: " + TimeUnit.MILLISECONDS.convert(now, TimeUnit.NANOSECONDS))
    now = System.nanoTime()
    server.start()
    port = server.port()
    now = System.nanoTime() - now
    println("Start server: " + TimeUnit.MILLISECONDS.convert(now, TimeUnit.NANOSECONDS))
    println("Endpoint available at http://localhost:" + port + "/helloworld")
}
